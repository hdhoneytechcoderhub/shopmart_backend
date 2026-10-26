package com.shopmart.module.payment.service.impl;

import com.shopmart.common.alert.AlertSender;
import com.shopmart.common.alert.AlertSeverity;
import com.shopmart.config.MetricsConfig;
import com.shopmart.module.order.entity.Order;
import com.shopmart.module.order.entity.OrderStatus;
import com.shopmart.module.order.entity.PaymentStatus;
import com.shopmart.module.order.repository.OrderRepository;
import com.shopmart.module.payment.entity.Payment;
import com.shopmart.module.payment.entity.PaymentTransaction;
import com.shopmart.module.payment.gateway.PaymentGateway;
import com.shopmart.module.payment.repository.PaymentRepository;
import com.shopmart.module.payment.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Reconciliation: payments can get stuck in PENDING if the checkout callback never fires
 * (tab closed) and the webhook is also missed (delivery failure, or — before today's fix —
 * silently rejected by the fail-open bug). This job periodically asks the gateway directly
 * what actually happened, so those orders don't sit in limbo forever.
 *
 * Only reconciles payments made through the currently active gateway (the one PaymentGateway
 * bean wired in), consistent with the rest of the payment module's single-active-provider design.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentReconciliationJob {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentTransactionRepository txnRepository;
    private final PaymentGateway gateway;
    private final MetricsConfig.AppMetrics appMetrics;
    private final AlertSender alertSender;

    @Value("${app.payments.reconciliation.stale-after-minutes:30}")
    private int staleAfterMinutes;

    @Scheduled(fixedDelayString = "${app.payments.reconciliation.interval-ms:1800000}") // default 30 min
    @SchedulerLock(name = "paymentReconciliationJob", lockAtMostFor = "PT25M", lockAtLeastFor = "PT1M")
    @Transactional
    public void reconcilePendingPayments() {
        Instant cutoff = Instant.now().minus(staleAfterMinutes, ChronoUnit.MINUTES);
        List<Payment> stalePending = paymentRepository.findByStatusAndCreatedAtBefore(Payment.PaymentState.PENDING, cutoff);
        if (stalePending.isEmpty()) return;

        log.info("[RECONCILE] checking {} stale PENDING payment(s) older than {} min", stalePending.size(), staleAfterMinutes);
        for (Payment payment : stalePending) {
            // Only reconcile payments that belong to the currently active gateway; a payment
            // created under a previously-active provider can't be checked against the wrong one.
            if (!payment.getProvider().equals(gateway.provider())) continue;
            try {
                reconcileOne(payment);
            } catch (Exception e) {
                log.error("[RECONCILE] failed for payment {}: {}", payment.getId(), e.getMessage());
            }
        }
    }

    private void reconcileOne(Payment payment) {
        PaymentGateway.GatewayStatus status = gateway.checkStatus(payment.getGatewayRef(), payment.getTransactionId());
        if (status == PaymentGateway.GatewayStatus.UNKNOWN || status == PaymentGateway.GatewayStatus.PENDING) {
            return; // nothing actionable yet
        }

        Order order = orderRepository.findById(payment.getOrderId()).orElse(null);
        if (status == PaymentGateway.GatewayStatus.SUCCESS) {
            payment.setStatus(Payment.PaymentState.SUCCESS);
            paymentRepository.save(payment);
            if (order != null) {
                order.setPaymentStatus(PaymentStatus.PAID);
                if (order.getStatus() == OrderStatus.PENDING) order.setStatus(OrderStatus.CONFIRMED);
                orderRepository.save(order);
            }
            recordTxn(payment, "CAPTURED", "reconciliation: gateway reports success, local record was stale PENDING");
            appMetrics.incrementPaymentResult(payment.getProvider(), "reconciled_success");
            log.warn("[RECONCILE] payment {} (order {}) was stuck PENDING but gateway confirms SUCCESS - corrected",
                    payment.getId(), payment.getOrderId());
            alertSender.send("Payment reconciliation correction",
                    "Payment " + payment.getId() + " (order " + payment.getOrderId() + ") was stuck PENDING — " +
                    "gateway confirmed SUCCESS and a webhook/callback was likely missed. Auto-corrected.",
                    AlertSeverity.WARNING);
        } else if (status == PaymentGateway.GatewayStatus.FAILED) {
            payment.setStatus(Payment.PaymentState.FAILED);
            payment.setFailureReason("Reconciliation: gateway reports failed");
            paymentRepository.save(payment);
            if (order != null) {
                order.setPaymentStatus(PaymentStatus.FAILED);
                orderRepository.save(order);
            }
            recordTxn(payment, "FAILED", "reconciliation: gateway reports failed, local record was stale PENDING");
            appMetrics.incrementPaymentResult(payment.getProvider(), "reconciled_failed");
            log.warn("[RECONCILE] payment {} (order {}) was stuck PENDING but gateway confirms FAILED - corrected",
                    payment.getId(), payment.getOrderId());
        }
    }

    private void recordTxn(Payment payment, String status, String note) {
        PaymentTransaction t = new PaymentTransaction();
        t.setOrderId(payment.getOrderId());
        t.setPaymentId(payment.getId());
        t.setGatewayOrderId(payment.getGatewayRef());
        t.setTransactionId(payment.getTransactionId());
        t.setAmount(payment.getAmount());
        t.setStatus(status);
        t.setProvider(payment.getProvider());
        t.setNote(note);
        txnRepository.save(t);
    }
}
