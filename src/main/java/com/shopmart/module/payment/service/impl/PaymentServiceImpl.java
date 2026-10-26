package com.shopmart.module.payment.service.impl;

import com.shopmart.common.exception.BadRequestException;
import com.shopmart.common.exception.ResourceNotFoundException;
import com.shopmart.module.order.entity.Order;
import com.shopmart.module.order.entity.OrderStatus;
import com.shopmart.module.order.entity.PaymentStatus;
import com.shopmart.module.order.repository.OrderRepository;
import com.shopmart.module.payment.dto.*;
import com.shopmart.module.payment.entity.Payment;
import com.shopmart.module.payment.entity.Payment.PaymentState;
import com.shopmart.module.payment.gateway.PaymentGateway;
import com.shopmart.module.payment.mapper.PaymentMapper;
import com.shopmart.module.payment.repository.PaymentRepository;
import com.shopmart.module.payment.repository.PaymentTransactionRepository;
import com.shopmart.module.payment.repository.WebhookEventRepository;
import com.shopmart.module.payment.entity.WebhookEvent;
import com.shopmart.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import com.shopmart.module.payment.entity.PaymentTransaction;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.shopmart.module.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class PaymentServiceImpl implements PaymentService {

    private static final String CURRENCY = "INR";

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentGateway gateway;
    private final PaymentTransactionRepository txnRepository;
    private final WebhookEventRepository webhookEventRepository;
    private final ObjectMapper objectMapper;
    private final com.shopmart.config.MetricsConfig.AppMetrics appMetrics;

    @Override
    @Transactional
    public PaymentIntentResponse initiate(Long userId, CreatePaymentRequest request) {
        log.info("Creating payment intent for order {}", request.orderId());
        Order order = ownedOrder(userId, request.orderId());
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BadRequestException("This order is already paid");
        }

        String method = order.getPaymentMethod();
        Payment payment = new Payment();
        payment.setOrderId(order.getId());
        payment.setMethod(method);
        payment.setAmount(order.getTotal());

        // Cash on delivery: no gateway round-trip. Confirm the order immediately.
        if ("COD".equals(method)) {
            payment.setProvider("none");
            payment.setStatus(PaymentState.PENDING);
            paymentRepository.save(payment);

            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);

            return new PaymentIntentResponse(payment.getId(), order.getId(), "none", null,
                    order.getTotal(), CURRENCY, payment.getStatus().name(), false, null);
        }

        // Gateway-backed methods: create a provider order and hand the ref to the client.
        String gatewayRef = gateway.createGatewayOrder(order.getId(), order.getTotal(), CURRENCY);
        payment.setProvider(gateway.provider());
        payment.setGatewayRef(gatewayRef);
        payment.setStatus(PaymentState.PENDING);
        paymentRepository.save(payment);
        recordTxn(order.getId(), payment.getId(), gatewayRef, null, order.getTotal(),
                "CREATED", gateway.provider(), null);

        return new PaymentIntentResponse(payment.getId(), order.getId(), gateway.provider(),
                gatewayRef, order.getTotal(), CURRENCY, payment.getStatus().name(), true, gateway.publicKey());
    }

    @Override
    @Transactional
    public PaymentResponse verify(Long userId, VerifyPaymentRequest request) {
        log.info("Payment Verification Started for order {}", request.orderId());
        Order order = ownedOrder(userId, request.orderId());
        Payment payment = paymentRepository.findByGatewayRef(request.gatewayRef())
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "gatewayRef", request.gatewayRef()));

        if (!payment.getOrderId().equals(order.getId())) {
            throw new BadRequestException("Payment does not belong to this order");
        }

        boolean valid = gateway.verifySignature(request.gatewayRef(), request.transactionId(), request.signature());
        if (!valid) {
            payment.setStatus(PaymentState.FAILED);
            payment.setFailureReason("Signature verification failed");
            paymentRepository.save(payment);
            order.setPaymentStatus(PaymentStatus.FAILED);
            orderRepository.save(order);
            recordTxn(order.getId(), payment.getId(), request.gatewayRef(), request.transactionId(),
                    payment.getAmount(), "FAILED", payment.getProvider(), "Signature verification failed");
            appMetrics.incrementPaymentResult(payment.getProvider(), "failed");
            throw new BadRequestException("Payment verification failed");
        }

        payment.setStatus(PaymentState.SUCCESS);
        payment.setTransactionId(request.transactionId());
        paymentRepository.save(payment);
        appMetrics.incrementPaymentResult(payment.getProvider(), "success");

        log.info("Payment Verified Successfully order={}", order.getId());
        recordTxn(order.getId(), payment.getId(), request.gatewayRef(), request.transactionId(),
                payment.getAmount(), "CAPTURED", payment.getProvider(), null);
        order.setPaymentStatus(PaymentStatus.PAID);
        if (order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.CONFIRMED);
        }
        orderRepository.save(order);

        return PaymentMapper.toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> history(Long userId, Long orderId) {
        ownedOrder(userId, orderId);
        return paymentRepository.findByOrderId(orderId).stream().map(PaymentMapper::toResponse).toList();
    }

    private Order ownedOrder(Long userId, Long orderId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void handleWebhook(String payload, String razorpaySignature, String phonepeSignature, String stripeSignature) {
        log.info("[PAYMENT][webhook] received len={} provider={}",
                payload != null ? payload.length() : 0, gateway.provider());
        if (payload == null || payload.isBlank()) {
            log.warn("[PAYMENT][webhook] empty payload - ignoring");
            return;
        }
        // Route to the signature header matching the currently active gateway. Only one
        // PaymentGateway bean is active at a time (selected via app.payments.provider), so
        // whichever provider is configured also determines which header carries the signature.
        String signature = switch (gateway.provider()) {
            case "razorpay" -> razorpaySignature;
            case "phonepe" -> phonepeSignature;
            case "stripe" -> stripeSignature;
            default -> razorpaySignature != null ? razorpaySignature
                    : (phonepeSignature != null ? phonepeSignature : stripeSignature);
        };
        // Fail-closed: verification is delegated entirely to the active gateway's own
        // verifyWebhookSignature(). The interface default rejects (false) unless a gateway
        // explicitly implements it, so an unconfigured secret can never fall through unverified.
        if (!gateway.verifyWebhookSignature(payload, signature)) {
            log.warn("[PAYMENT][webhook] signature verification FAILED for provider={} - rejecting", gateway.provider());
            appMetrics.incrementWebhookResult(gateway.provider(), "rejected");
            return;
        }
        appMetrics.incrementWebhookResult(gateway.provider(), "verified");
        log.info("[PAYMENT][webhook] signature verified for provider={}", gateway.provider());
        try {
            JsonNode root = objectMapper.readTree(payload);
            String eventId = root.path("id").asText("");
            if (eventId.isBlank()) {
                // Not every gateway includes a stable event id at the payload root (PhonePe callbacks
                // don't). Fall back to a content hash so an identical redelivery is still deduped —
                // this won't catch a gateway resending with a different timestamp wrapper, but that's
                // a gap in the gateway's own retry semantics, not something we can fix from here.
                eventId = "sha256:" + sha256Hex(payload);
            }
            if (webhookEventRepository.existsByProviderAndEventId(gateway.provider(), eventId)) {
                log.warn("[PAYMENT][webhook] duplicate delivery provider={} eventId={} - ignoring (replay protection)",
                        gateway.provider(), eventId);
                return;
            }
            WebhookEvent event = new WebhookEvent();
            event.setProvider(gateway.provider());
            event.setEventId(eventId);
            webhookEventRepository.save(event);

            String eventType = root.path("event").asText("");
            log.info("[PAYMENT][webhook] event={}", eventType);
            switch (eventType) {
                case "payment.captured" -> onCaptured(root);
                case "payment.failed"   -> onFailed(root);
                case "refund.processed" -> onRefundProcessed(root);
                default -> log.info("[PAYMENT][webhook] unhandled event {}", eventType);
            }
        } catch (Exception e) {
            log.error("[PAYMENT][webhook] processing error", e);
        }
    }

    private String sha256Hex(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("hash failed", e);
        }
    }

    private void onCaptured(JsonNode root) {
        String gatewayRef = root.path("payload").path("payment").path("entity").path("order_id").asText(null);
        if (gatewayRef == null) return;
        paymentRepository.findByGatewayRef(gatewayRef).ifPresent(payment -> {
            if (payment.getStatus() != PaymentState.SUCCESS) {
                payment.setStatus(PaymentState.SUCCESS);
                paymentRepository.save(payment);
                orderRepository.findById(payment.getOrderId()).ifPresent(order -> {
                    order.setPaymentStatus(PaymentStatus.PAID);
                    if (order.getStatus() == OrderStatus.PENDING) order.setStatus(OrderStatus.CONFIRMED);
                    orderRepository.save(order);
                });
                recordTxn(payment.getOrderId(), payment.getId(), gatewayRef, payment.getTransactionId(),
                        payment.getAmount(), "CAPTURED", payment.getProvider(), "via webhook");
                log.info("[PAYMENT][webhook] order {} marked PAID via webhook", payment.getOrderId());
            }
        });
    }

    private void onFailed(JsonNode root) {
        String gatewayRef = root.path("payload").path("payment").path("entity").path("order_id").asText(null);
        if (gatewayRef == null) return;
        paymentRepository.findByGatewayRef(gatewayRef).ifPresent(payment -> {
            payment.setStatus(PaymentState.FAILED);
            payment.setFailureReason("PAYMENT_FAILED (webhook)");
            paymentRepository.save(payment);
            orderRepository.findById(payment.getOrderId()).ifPresent(order -> {
                order.setPaymentStatus(PaymentStatus.FAILED);
                orderRepository.save(order);
            });
            recordTxn(payment.getOrderId(), payment.getId(), gatewayRef, null,
                    payment.getAmount(), "FAILED", payment.getProvider(), "via webhook");
        });
    }

    private void onRefundProcessed(JsonNode root) {
        String gatewayRef = root.path("payload").path("refund").path("entity").path("order_id").asText(null);
        if (gatewayRef == null) return;
        paymentRepository.findByGatewayRef(gatewayRef).ifPresent(payment -> {
            payment.setStatus(PaymentState.REFUNDED);
            paymentRepository.save(payment);
            orderRepository.findById(payment.getOrderId()).ifPresent(order -> {
                order.setPaymentStatus(PaymentStatus.REFUNDED);
                orderRepository.save(order);
            });
            recordTxn(payment.getOrderId(), payment.getId(), gatewayRef, null,
                    payment.getAmount(), "REFUNDED", payment.getProvider(), "via webhook");
        });
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public PaymentResponse refundOrder(Long orderId, RefundRequest request) {
        log.info("Refund requested for order {} amount={} idempotencyKey={}",
                orderId, request != null ? request.amount() : null, request != null ? request.idempotencyKey() : null);

        // Idempotency: if this exact key was already used, return the transaction it produced
        // instead of processing a second refund (double-click / client retry protection).
        String idempotencyKey = request != null ? request.idempotencyKey() : null;
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = txnRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                log.info("[PAYMENT][refund] idempotent replay for key={} - returning existing result", idempotencyKey);
                Payment payment = paymentRepository.findById(existing.get().getPaymentId())
                        .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", existing.get().getPaymentId()));
                return PaymentMapper.toResponse(payment);
            }
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        Payment payment = paymentRepository.findByOrderId(orderId).stream()
                .filter(p -> p.getStatus() == Payment.PaymentState.SUCCESS
                        || p.getStatus() == Payment.PaymentState.PARTIALLY_REFUNDED)
                .findFirst()
                .orElseThrow(() -> new BadRequestException("No refundable payment for this order"));

        BigDecimal alreadyRefunded = payment.getRefundedAmount() == null ? BigDecimal.ZERO : payment.getRefundedAmount();
        BigDecimal remaining = payment.getAmount().subtract(alreadyRefunded);
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("This payment has already been fully refunded");
        }

        BigDecimal refundAmount = (request != null && request.amount() != null) ? request.amount() : remaining;
        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Refund amount must be positive");
        }
        if (refundAmount.compareTo(remaining) > 0) {
            throw new BadRequestException("Refund amount exceeds the remaining refundable balance of " + remaining);
        }

        String refundId = gateway.refund(payment.getTransactionId(), refundAmount, CURRENCY);

        BigDecimal newRefundedTotal = alreadyRefunded.add(refundAmount);
        payment.setRefundedAmount(newRefundedTotal);
        boolean isFullRefund = newRefundedTotal.compareTo(payment.getAmount()) >= 0;
        payment.setStatus(isFullRefund ? Payment.PaymentState.REFUNDED : Payment.PaymentState.PARTIALLY_REFUNDED);
        paymentRepository.save(payment);

        order.setPaymentStatus(isFullRefund ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED);
        orderRepository.save(order);

        PaymentTransaction txn = recordTxn(order.getId(), payment.getId(), payment.getGatewayRef(), payment.getTransactionId(),
                refundAmount, isFullRefund ? "REFUNDED" : "PARTIALLY_REFUNDED", payment.getProvider(),
                "refundId=" + refundId + (isFullRefund ? "" : " (partial)"));
        if (idempotencyKey != null && !idempotencyKey.isBlank() && txn != null) {
            txn.setIdempotencyKey(idempotencyKey);
            txnRepository.save(txn);
        }
        appMetrics.incrementPaymentResult(payment.getProvider(), isFullRefund ? "refunded" : "partially_refunded");
        log.info("Refund completed for order {} refundId={} amount={} fullyRefunded={}",
                orderId, refundId, refundAmount, isFullRefund);
        return PaymentMapper.toResponse(payment);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public PaymentResponse markFailed(Long userId, Long orderId, String reason) {
        // reason: PAYMENT_FAILED | PAYMENT_CANCELLED | PAYMENT_TIMEOUT
        String r = (reason == null || reason.isBlank()) ? "PAYMENT_FAILED" : reason.trim().toUpperCase();
        log.info("Marking payment failed order={} reason={}", orderId, r);
        Order order = ownedOrder(userId, orderId);
        Payment payment = paymentRepository.findByOrderId(orderId).stream()
                .reduce((a, b) -> b)  // latest
                .orElseThrow(() -> new BadRequestException("No payment found for this order"));
        payment.setStatus(PaymentState.FAILED);
        payment.setFailureReason(r);
        paymentRepository.save(payment);
        order.setPaymentStatus(PaymentStatus.FAILED);
        orderRepository.save(order);
        String txnStatus = switch (r) {
            case "PAYMENT_CANCELLED" -> "CANCELLED";
            case "PAYMENT_TIMEOUT" -> "TIMEOUT";
            default -> "FAILED";
        };
        recordTxn(orderId, payment.getId(), payment.getGatewayRef(), payment.getTransactionId(),
                payment.getAmount(), txnStatus, payment.getProvider(), r);
        return PaymentMapper.toResponse(payment);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public PageResponse<PaymentResponse> allPayments(Pageable pageable) {
        return PageResponse.from(paymentRepository.findAll(pageable).map(PaymentMapper::toResponse));
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public java.util.List<PaymentTransaction> transactions(Long orderId) {
        return txnRepository.findByOrderIdOrderByCreatedAtAsc(orderId);
    }

    private PaymentTransaction recordTxn(Long orderId, Long paymentId, String gatewayOrderId, String transactionId,
                           java.math.BigDecimal amount, String status, String provider, String note) {
        try {
            PaymentTransaction t = new PaymentTransaction();
            t.setOrderId(orderId);
            t.setPaymentId(paymentId);
            t.setGatewayOrderId(gatewayOrderId);
            t.setTransactionId(transactionId);
            t.setAmount(amount);
            t.setStatus(status);
            t.setProvider(provider);
            t.setNote(note);
            return txnRepository.save(t);
        } catch (Exception e) {
            log.warn("Failed to record payment transaction ({}): {}", status, e.getMessage());
            return null;
        }
    }
}
