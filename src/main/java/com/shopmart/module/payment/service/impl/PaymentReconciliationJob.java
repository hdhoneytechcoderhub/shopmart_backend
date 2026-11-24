package com.shopmart.module.payment.service.impl;

import com.shopmart.config.MetricsConfig;
import com.shopmart.module.order.entity.Order;
import com.shopmart.module.order.repository.OrderRepository;
import com.shopmart.module.payment.entity.Payment;
import com.shopmart.module.payment.gateway.PaymentGateway;
import com.shopmart.module.payment.repository.PaymentRepository;
import com.shopmart.module.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Reconciliation: payments can get stuck in PENDING if the checkout callback never fires
 * (tab closed) and the webhook is also missed. This job periodically asks the gateway
 * directly what actually happened, going through the same signature-independent
 * server-side status sync used by the checkout retry path.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentReconciliationJob {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentGateway gateway;
    private final PaymentService paymentService;
    private final MetricsConfig.AppMetrics appMetrics;

    @Value("${app.payments.reconciliation.stale-after-minutes:30}")
    private int staleAfterMinutes;

    @Scheduled(fixedDelayString = "${app.payments.reconciliation.interval-ms:1800000}")
    @SchedulerLock(name = "paymentReconciliationJob", lockAtMostFor = "PT25M", lockAtLeastFor = "PT1M")
    public void reconcilePendingPayments() {
        Instant cutoff = Instant.now().minus(staleAfterMinutes, ChronoUnit.MINUTES);
        List<Payment> stalePending = paymentRepository.findByStatusAndCreatedAtBefore(Payment.PaymentState.PENDING, cutoff);
        if (stalePending.isEmpty()) return;

        log.info("[RECONCILE] checking {} stale PENDING payment(s) older than {} min", stalePending.size(), staleAfterMinutes);
        for (Payment payment : stalePending) {
            if (!payment.getProvider().equals(gateway.provider())) continue;
            try {
                Order order = orderRepository.findById(payment.getOrderId()).orElse(null);
                if (order == null) continue;
                paymentService.syncStatus(order.getUserId(), order.getId());
                appMetrics.incrementPaymentResult(payment.getProvider(), "reconciled");
            } catch (Exception e) {
                log.error("[RECONCILE] failed for payment {}: {}", payment.getId(), e.getMessage());
            }
        }
    }
}
