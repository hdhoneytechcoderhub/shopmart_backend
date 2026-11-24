package com.shopmart.module.payment.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopmart.common.dto.PageResponse;
import com.shopmart.common.exception.BadRequestException;
import com.shopmart.common.exception.ResourceNotFoundException;
import com.shopmart.module.cart.repository.CartRepository;
import com.shopmart.module.loyalty.service.LoyaltyService;
import com.shopmart.module.notification.entity.NotificationType;
import com.shopmart.module.notification.service.UserNotificationService;
import com.shopmart.module.order.entity.Order;
import com.shopmart.module.order.entity.OrderStatus;
import com.shopmart.module.order.entity.PaymentStatus;
import com.shopmart.module.order.event.OrderPaidEvent;
import com.shopmart.module.order.repository.OrderRepository;
import com.shopmart.module.payment.dto.*;
import com.shopmart.module.payment.entity.Payment;
import com.shopmart.module.payment.entity.Payment.PaymentState;
import com.shopmart.module.payment.entity.PaymentTransaction;
import com.shopmart.module.payment.entity.WebhookEvent;
import com.shopmart.module.payment.gateway.PaymentGateway;
import com.shopmart.module.payment.gateway.PaymentGateway.GatewayPayment;
import com.shopmart.module.payment.gateway.PaymentGateway.GatewayStatus;
import com.shopmart.module.payment.mapper.PaymentMapper;
import com.shopmart.module.payment.repository.PaymentRepository;
import com.shopmart.module.payment.repository.PaymentTransactionRepository;
import com.shopmart.module.payment.repository.WebhookEventRepository;
import com.shopmart.module.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private static final String CURRENCY = "INR";

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentGateway gateway;
    private final PaymentTransactionRepository txnRepository;
    private final WebhookEventRepository webhookEventRepository;
    private final ObjectMapper objectMapper;
    private final com.shopmart.config.MetricsConfig.AppMetrics appMetrics;
    private final CartRepository cartRepository;
    private final UserNotificationService userNotificationService;
    private final LoyaltyService loyaltyService;
    private final ApplicationEventPublisher eventPublisher;

    // ------------------------------------------------------------------
    // Initiate: create (or reuse) the Razorpay order. The amount is always
    // the server-side order total, never a client-supplied value.
    // ------------------------------------------------------------------
    @Override
    @Transactional
    public PaymentIntentResponse initiate(Long userId, CreatePaymentRequest request) {
        Order order = ownedOrderForUpdate(userId, request.orderId());

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("This order has been cancelled");
        }
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BadRequestException("This order is already paid");
        }
        if (order.getTotal() == null || order.getTotal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("This order has no payable amount");
        }

        String method = order.getPaymentMethod() == null ? "RAZORPAY" : order.getPaymentMethod().toUpperCase();
        if ("COD".equals(method)) {
            return initiateCod(order);
        }
        order.setPaymentMethod("RAZORPAY");

        List<Payment> payments = paymentRepository.findByOrderIdOrderByCreatedAtDesc(order.getId());
        Payment latest = payments.isEmpty() ? null : payments.get(0);
        if (latest != null && latest.getStatus() == PaymentState.REFUNDED) {
            throw new BadRequestException("This order's payment was refunded");
        }
        if (latest != null) {
            PaymentIntentResponse reused = settleOrReusePreviousAttempt(order, latest);
            if (reused != null) return reused;
        }

        String gatewayRef = gateway.createGatewayOrder(order.getId(), order.getTotal(), CURRENCY);
        String keyId = gateway.publicKey();
        if (keyId == null || keyId.isBlank()) {
            throw new BadRequestException("Razorpay public key is not configured");
        }

        Payment payment = new Payment();
        payment.setOrderId(order.getId());
        payment.setMethod("RAZORPAY");
        payment.setAmount(order.getTotal());
        payment.setProvider(gateway.provider());
        payment.setGatewayRef(gatewayRef);
        payment.setStatus(PaymentState.PENDING);
        paymentRepository.save(payment);
        recordTxn(order.getId(), payment.getId(), gatewayRef, null, order.getTotal(),
                "CREATED", gateway.provider(), "Razorpay order created");

        // A new attempt starts a fresh PENDING state; the order itself stays PENDING_PAYMENT.
        order.setPaymentStatus(PaymentStatus.PENDING);
        orderRepository.save(order);

        log.info("[PAYMENT] initiated order={} razorpayOrder={} amount={}", order.getId(), gatewayRef, order.getTotal());
        return intent(payment, order);
    }

    private PaymentIntentResponse initiateCod(Order order) {
        Payment payment = new Payment();
        payment.setOrderId(order.getId());
        payment.setMethod("COD");
        payment.setAmount(order.getTotal());
        payment.setProvider("none");
        payment.setStatus(PaymentState.PENDING);
        paymentRepository.save(payment);

        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);
        return new PaymentIntentResponse(payment.getId(), order.getId(), "none", null,
                order.getTotal(), 0, CURRENCY, payment.getStatus().name(), false, null);
    }

    /**
     * Before creating another Razorpay order, ask Razorpay what happened to the previous
     * one. This blocks a second charge while an earlier attempt is still pending/unknown,
     * and reuses the same Razorpay order for a clean retry.
     */
    private PaymentIntentResponse settleOrReusePreviousAttempt(Order order, Payment latest) {
        if (latest.getGatewayRef() == null || latest.getGatewayRef().isBlank()) return null;

        GatewayPayment gp = gateway.fetchPayment(latest.getGatewayRef(), null);
        if (gp == null || gp.status() == GatewayStatus.UNKNOWN) {
            throw new BadRequestException(
                    "We could not confirm your previous payment attempt yet. Please wait a moment and retry; you have not been charged twice.");
        }

        switch (gp.status()) {
            case SUCCESS -> {
                confirmPaid(order, latest, gp, "initiate-sync");
                throw new BadRequestException("This order has already been paid");
            }
            case AUTHORIZED -> {
                GatewayPayment captured = captureAuthorized(latest, gp);
                if (captured != null) {
                    confirmPaid(order, latest, captured, "initiate-capture");
                    throw new BadRequestException("This order has already been paid");
                }
                throw new BadRequestException("Your previous payment is authorised but not captured. Please contact support.");
            }
            case PENDING, FAILED -> {
                // Razorpay keeps the order open after a failed/cancelled attempt, so the
                // same order id can be reused. Reset the local attempt to PENDING.
                if (latest.getStatus() != PaymentState.PENDING) {
                    latest.setStatus(PaymentState.PENDING);
                    latest.setFailureReason(null);
                    paymentRepository.save(latest);
                    recordTxn(order.getId(), latest.getId(), latest.getGatewayRef(), null,
                            latest.getAmount(), "RETRY_INITIATED", latest.getProvider(), null);
                }
                order.setPaymentStatus(PaymentStatus.PENDING);
                orderRepository.save(order);
                log.info("[PAYMENT] reusing Razorpay order {} for retry on order {}", latest.getGatewayRef(), order.getId());
                return intent(latest, order);
            }
            default -> throw new BadRequestException("Your previous payment could not be verified. Please contact support.");
        }
    }

    private PaymentIntentResponse intent(Payment payment, Order order) {
        return new PaymentIntentResponse(payment.getId(), order.getId(), payment.getProvider(),
                payment.getGatewayRef(), payment.getAmount(), toMinor(payment.getAmount()), CURRENCY,
                payment.getStatus().name(), true, gateway.publicKey());
    }

    private long toMinor(BigDecimal amount) {
        return amount == null ? 0 : amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    // ------------------------------------------------------------------
    // Verify: signature + authoritative Razorpay payment state. The order
    // is only marked PAID after both checks pass.
    // ------------------------------------------------------------------
    @Override
    @Transactional
    public PaymentResponse verify(Long userId, VerifyPaymentRequest request) {
        Order order = ownedOrderForUpdate(userId, request.orderId());
        Payment payment = paymentRepository.findByGatewayRef(request.gatewayRef())
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "gatewayRef", request.gatewayRef()));

        if (!payment.getOrderId().equals(order.getId())) {
            throw new BadRequestException("Payment does not belong to this order");
        }

        // Idempotent replay: the same verified callback can be delivered more than once.
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            if (payment.getStatus() == PaymentState.SUCCESS) {
                log.info("[PAYMENT] duplicate verify for already-paid order {} - no-op", order.getId());
                return PaymentMapper.toResponse(payment);
            }
            throw new BadRequestException("This order is already paid");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("This order has been cancelled");
        }

        boolean signatureValid = gateway.verifySignature(
                request.gatewayRef(), request.transactionId(), request.signature());
        if (!signatureValid) {
            failPayment(order, payment, "Signature verification failed", "FAILED");
            appMetrics.incrementPaymentResult(payment.getProvider(), "failed");
            throw new BadRequestException("Payment verification failed");
        }

        GatewayPayment gp = gateway.fetchPayment(request.gatewayRef(), request.transactionId());
        if (gp == null || gp.status() == GatewayStatus.UNKNOWN) {
            // Do not confirm and do not mark failed: the gateway has not told us the final
            // state. The webhook/reconciliation job will settle it.
            throw new BadRequestException("Payment could not be confirmed with Razorpay yet. Please wait and check your orders.");
        }

        if (gp.status() == GatewayStatus.AUTHORIZED) {
            gp = captureAuthorized(payment, gp);
            if (gp == null) {
                throw new BadRequestException("Payment was authorised but capture failed. Please contact support.");
            }
        }

        if (gp.status() != GatewayStatus.SUCCESS) {
            if (gp.status() == GatewayStatus.FAILED) {
                failPayment(order, payment, gp.failureReason() != null ? gp.failureReason() : "Payment failed", "FAILED");
                appMetrics.incrementPaymentResult(payment.getProvider(), "failed");
                throw new BadRequestException("Payment failed");
            }
            throw new BadRequestException("Payment is not captured yet. Please wait and retry.");
        }

        assertMatchesOrder(order, payment, gp);
        confirmPaid(order, payment, gp, "checkout-verify");
        return PaymentMapper.toResponse(payment);
    }

    /** Capture an authorised payment, then re-read the authoritative state. */
    private GatewayPayment captureAuthorized(Payment payment, GatewayPayment gp) {
        if (gp.paymentId() == null || gp.paymentId().isBlank()) return null;
        if (!gateway.capturePayment(gp.paymentId(), payment.getAmount(), CURRENCY)) return null;
        GatewayPayment refreshed = gateway.fetchPayment(payment.getGatewayRef(), gp.paymentId());
        return refreshed != null && refreshed.status() == GatewayStatus.SUCCESS ? refreshed : null;
    }

    private void assertMatchesOrder(Order order, Payment payment, GatewayPayment gp) {
        if (gp.gatewayOrderId() != null && !gp.gatewayOrderId().equals(payment.getGatewayRef())) {
            throw new BadRequestException("Gateway order does not match this payment");
        }
        if (gp.amount() != null && gp.amount().compareTo(order.getTotal()) != 0) {
            throw new BadRequestException("Paid amount does not match the order total");
        }
        if (gp.amount() != null && gp.amount().compareTo(payment.getAmount()) != 0) {
            throw new BadRequestException("Paid amount does not match the payment record");
        }
        if (gp.currency() != null && !CURRENCY.equalsIgnoreCase(gp.currency())) {
            throw new BadRequestException("Unsupported payment currency");
        }
    }

    /**
     * Single point that marks an order PAID + CONFIRMED, clears the cart, awards loyalty,
     * publishes the email event and records the transaction. Safe to call repeatedly.
     */
    private void confirmPaid(Order order, Payment payment, GatewayPayment gp, String source) {
        boolean alreadyPaid = order.getPaymentStatus() == PaymentStatus.PAID
                && payment.getStatus() == PaymentState.SUCCESS;
        if (alreadyPaid) return;

        if (gp != null) assertMatchesOrder(order, payment, gp);

        if (order.getStatus() == OrderStatus.CANCELLED) {
            // Money was captured for an order cancelled before confirmation. Record the
            // payment accurately; leave the order cancelled for an admin refund.
            payment.setStatus(PaymentState.SUCCESS);
            if (gp != null && gp.paymentId() != null) payment.setTransactionId(gp.paymentId());
            payment.setFailureReason(null);
            paymentRepository.save(payment);
            order.setPaymentStatus(PaymentStatus.PAID);
            order.setRazorpayOrderId(payment.getGatewayRef());
            order.setRazorpayPaymentId(payment.getTransactionId());
            orderRepository.save(order);
            recordTxn(order.getId(), payment.getId(), payment.getGatewayRef(), payment.getTransactionId(),
                    payment.getAmount(), "CAPTURED", payment.getProvider(), "captured after cancellation - refund required");
            log.error("[PAYMENT] order {} was cancelled but payment {} captured; refund required",
                    order.getId(), payment.getTransactionId());
            return;
        }

        payment.setStatus(PaymentState.SUCCESS);
        if (gp != null && gp.paymentId() != null) payment.setTransactionId(gp.paymentId());
        payment.setFailureReason(null);
        paymentRepository.save(payment);

        order.setPaymentStatus(PaymentStatus.PAID);
        if (order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.CONFIRMED);
        }
        order.setRazorpayOrderId(payment.getGatewayRef());
        order.setRazorpayPaymentId(payment.getTransactionId());
        if (order.getPaidAt() == null) order.setPaidAt(java.time.Instant.now());
        orderRepository.save(order);

        // Purchased items leave the cart only now that payment is confirmed.
        cartRepository.findByUserId(order.getUserId()).ifPresent(cart -> {
            if (!cart.getItems().isEmpty()) {
                cart.getItems().clear();
                cartRepository.save(cart);
            }
        });

        try {
            userNotificationService.notify(order.getUserId(), NotificationType.ORDER,
                    "Payment successful",
                    "Your order " + order.getOrderNumber() + " has been confirmed.",
                    "/dashboard/orders/" + order.getId());
        } catch (Exception e) {
            log.warn("[PAYMENT] in-app notification failed for order {}: {}", order.getId(), e.getMessage());
        }
        try {
            loyaltyService.earnForOrder(order.getUserId(), order.getId(), order.getTotal());
        } catch (Exception e) {
            log.warn("[PAYMENT] loyalty award failed for order {}: {}", order.getId(), e.getMessage());
        }

        recordTxn(order.getId(), payment.getId(), payment.getGatewayRef(), payment.getTransactionId(),
                payment.getAmount(), "CAPTURED", payment.getProvider(), source);
        appMetrics.incrementPaymentResult(payment.getProvider(), "success");

        // Emails are sent AFTER_COMMIT by OrderPaidEmailListener; the flags on the order
        // make repeated webhook deliveries idempotent.
        eventPublisher.publishEvent(new OrderPaidEvent(order.getId()));

        log.info("[PAYMENT] order {} confirmed PAID source={} razorpayPayment={}",
                order.getId(), source, payment.getTransactionId());
    }

    private void failPayment(Order order, Payment payment, String reason, String txnStatus) {
        if (payment.getStatus() == PaymentState.SUCCESS) {
            log.warn("[PAYMENT] refusing to mark already-successful payment {} as failed", payment.getId());
            return;
        }
        boolean changed = payment.getStatus() != PaymentState.FAILED
                || !reason.equals(payment.getFailureReason());
        payment.setStatus(PaymentState.FAILED);
        payment.setFailureReason(reason.length() > 250 ? reason.substring(0, 250) : reason);
        paymentRepository.save(payment);

        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            order.setPaymentStatus(PaymentStatus.FAILED);
            orderRepository.save(order);
        }
        if (changed) {
            recordTxn(order.getId(), payment.getId(), payment.getGatewayRef(), payment.getTransactionId(),
                    payment.getAmount(), txnStatus, payment.getProvider(), reason);
        }
    }

    // ------------------------------------------------------------------
    // Status sync / retry safety
    // ------------------------------------------------------------------
    @Override
    @Transactional
    public OrderPaymentStatusResponse syncStatus(Long userId, Long orderId) {
        Order order = ownedOrderForUpdate(userId, orderId);
        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            Payment latest = paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(orderId).orElse(null);
            if (latest != null && latest.getStatus() != PaymentState.REFUNDED) {
                GatewayPayment gp = gateway.fetchPayment(latest.getGatewayRef(), null);
                if (gp != null) {
                    if (gp.status() == GatewayStatus.SUCCESS) {
                        assertMatchesOrder(order, latest, gp);
                        confirmPaid(order, latest, gp, "status-sync");
                    } else if (gp.status() == GatewayStatus.AUTHORIZED) {
                        GatewayPayment captured = captureAuthorized(latest, gp);
                        if (captured != null) confirmPaid(order, latest, captured, "status-sync-capture");
                    } else if (gp.status() == GatewayStatus.FAILED) {
                        failPayment(order, latest, "Razorpay reports payment failed", "FAILED");
                    }
                }
            }
        }
        return statusOf(order);
    }

    private OrderPaymentStatusResponse statusOf(Order order) {
        return new OrderPaymentStatusResponse(order.getId(), order.getOrderNumber(),
                order.getStatus().name(), order.getPaymentStatus().name(),
                order.getRazorpayOrderId(), order.getRazorpayPaymentId(), order.getTotal(), order.getPaidAt());
    }

    @Override
    @Transactional
    public PaymentResponse markFailed(Long userId, Long orderId, String reason) {
        String r = (reason == null || reason.isBlank()) ? "PAYMENT_FAILED" : reason.trim().toUpperCase();
        Order order = ownedOrderForUpdate(userId, orderId);
        Payment payment = paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> new BadRequestException("No payment found for this order"));

        // Never downgrade a captured payment because a stale popup was dismissed.
        if (order.getPaymentStatus() == PaymentStatus.PAID || payment.getStatus() == PaymentState.SUCCESS) {
            return PaymentMapper.toResponse(payment);
        }

        PaymentState target = "PAYMENT_CANCELLED".equals(r) ? PaymentState.CANCELLED : PaymentState.FAILED;
        if (payment.getStatus() == target && r.equals(payment.getFailureReason())) {
            return PaymentMapper.toResponse(payment);
        }
        payment.setStatus(target);
        payment.setFailureReason(r);
        paymentRepository.save(payment);

        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            order.setPaymentStatus("PAYMENT_CANCELLED".equals(r) ? PaymentStatus.CANCELLED : PaymentStatus.FAILED);
            orderRepository.save(order);
        }
        String txnStatus = switch (r) {
            case "PAYMENT_CANCELLED" -> "CANCELLED";
            case "PAYMENT_TIMEOUT" -> "TIMEOUT";
            default -> "FAILED";
        };
        recordTxn(orderId, payment.getId(), payment.getGatewayRef(), payment.getTransactionId(),
                payment.getAmount(), txnStatus, payment.getProvider(), r);
        appMetrics.incrementPaymentResult(payment.getProvider(), "failed");
        return PaymentMapper.toResponse(payment);
    }

    // ------------------------------------------------------------------
    // Webhooks: verified, deduplicated, and idempotent
    // ------------------------------------------------------------------
    @Override
    @Transactional
    public void handleWebhook(String payload, String eventIdHeader, String razorpaySignature,
                              String phonepeSignature, String stripeSignature) {
        log.info("[PAYMENT][webhook] received len={} provider={}",
                payload != null ? payload.length() : 0, gateway.provider());
        if (payload == null || payload.isBlank()) {
            throw new BadRequestException("Empty webhook payload");
        }

        String signature = switch (gateway.provider()) {
            case "razorpay" -> razorpaySignature;
            case "phonepe" -> phonepeSignature;
            case "stripe" -> stripeSignature;
            default -> razorpaySignature != null ? razorpaySignature
                    : (phonepeSignature != null ? phonepeSignature : stripeSignature);
        };
        if (!gateway.verifyWebhookSignature(payload, signature)) {
            log.warn("[PAYMENT][webhook] signature verification FAILED for provider={} - rejecting", gateway.provider());
            appMetrics.incrementWebhookResult(gateway.provider(), "rejected");
            throw new BadRequestException("Invalid webhook signature");
        }
        appMetrics.incrementWebhookResult(gateway.provider(), "verified");

        JsonNode root;
        try {
            root = objectMapper.readTree(payload);
        } catch (Exception e) {
            throw new BadRequestException("Invalid webhook JSON");
        }

        String eventId = eventIdHeader;
        if (eventId == null || eventId.isBlank()) eventId = root.path("id").asText("");
        if (eventId.isBlank()) eventId = "sha256:" + sha256Hex(payload);

        if (webhookEventRepository.existsByProviderAndEventId(gateway.provider(), eventId)) {
            log.info("[PAYMENT][webhook] duplicate delivery provider={} eventId={} - ignored",
                    gateway.provider(), eventId);
            return;
        }

        String eventType = root.path("event").asText("");
        log.info("[PAYMENT][webhook] event={} eventId={}", eventType, eventId);
        switch (eventType) {
            case "payment.captured" -> onCaptured(root);
            case "order.paid" -> onOrderPaid(root);
            case "payment.failed" -> onFailed(root);
            case "refund.processed" -> onRefundProcessed(root);
            default -> log.info("[PAYMENT][webhook] unhandled event {}", eventType);
        }

        // Recorded only after successful processing, so a transient failure can be retried
        // by Razorpay instead of being silently swallowed as a duplicate.
        try {
            WebhookEvent event = new WebhookEvent();
            event.setProvider(gateway.provider());
            event.setEventId(eventId);
            webhookEventRepository.save(event);
        } catch (DataIntegrityViolationException e) {
            log.info("[PAYMENT][webhook] concurrent duplicate eventId={} - ignored", eventId);
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
        JsonNode entity = root.path("payload").path("payment").path("entity");
        String gatewayRef = entity.path("order_id").asText(null);
        if (gatewayRef == null || gatewayRef.isBlank()) {
            log.warn("[PAYMENT][webhook] payment.captured without order_id - ignored");
            return;
        }
        Payment payment = paymentRepository.findByGatewayRef(gatewayRef).orElse(null);
        if (payment == null) {
            log.warn("[PAYMENT][webhook] unknown razorpay order {} - ignored", gatewayRef);
            return;
        }
        if (payment.getStatus() == PaymentState.SUCCESS) {
            log.info("[PAYMENT][webhook] order {} already settled - no-op", payment.getOrderId());
            return;
        }

        Order order = orderRepository.findByIdForUpdate(payment.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", payment.getOrderId()));

        boolean captured = entity.path("captured").asBoolean(false)
                || "captured".equals(entity.path("status").asText(""));
        if (!captured) {
            log.warn("[PAYMENT][webhook] payment.captured entity is not captured (status={}) - ignored",
                    entity.path("status").asText());
            return;
        }

        long minor = entity.path("amount").asLong(0);
        BigDecimal amount = minor > 0 ? BigDecimal.valueOf(minor).movePointLeft(2) : null;
        String currency = entity.path("currency").asText(null);
        GatewayPayment gp = new GatewayPayment(gatewayRef, entity.path("id").asText(null),
                GatewayStatus.SUCCESS, amount, currency, entity.path("method").asText(null), null);
        confirmPaid(order, payment, gp, "webhook-payment.captured");
    }

    private void onOrderPaid(JsonNode root) {
        JsonNode entity = root.path("payload").path("order").path("entity");
        String gatewayRef = entity.path("id").asText(null);
        if (gatewayRef == null || gatewayRef.isBlank()) return;
        Payment payment = paymentRepository.findByGatewayRef(gatewayRef).orElse(null);
        if (payment == null) return;
        if (payment.getStatus() == PaymentState.SUCCESS) return;
        Order order = orderRepository.findByIdForUpdate(payment.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", payment.getOrderId()));
        GatewayPayment gp = gateway.fetchPayment(gatewayRef, null);
        if (gp == null || gp.status() != GatewayStatus.SUCCESS) {
            log.warn("[PAYMENT][webhook] order.paid but payment not verifiable for {} - ignored", gatewayRef);
            return;
        }
        confirmPaid(order, payment, gp, "webhook-order.paid");
    }

    private void onFailed(JsonNode root) {
        JsonNode entity = root.path("payload").path("payment").path("entity");
        String gatewayRef = entity.path("order_id").asText(null);
        if (gatewayRef == null) return;
        Payment payment = paymentRepository.findByGatewayRef(gatewayRef).orElse(null);
        if (payment == null || payment.getStatus() == PaymentState.SUCCESS) return;
        Order order = orderRepository.findByIdForUpdate(payment.getOrderId()).orElse(null);
        if (order == null) return;
        failPayment(order, payment, "PAYMENT_FAILED (webhook)", "FAILED");
    }

    private void onRefundProcessed(JsonNode root) {
        String gatewayRef = root.path("payload").path("refund").path("entity").path("order_id").asText(null);
        if (gatewayRef == null) return;
        paymentRepository.findByGatewayRef(gatewayRef).ifPresent(payment -> {
            payment.setStatus(PaymentState.REFUNDED);
            paymentRepository.save(payment);
            orderRepository.findByIdForUpdate(payment.getOrderId()).ifPresent(order -> {
                order.setPaymentStatus(PaymentStatus.REFUNDED);
                orderRepository.save(order);
            });
            recordTxn(payment.getOrderId(), payment.getId(), gatewayRef, null,
                    payment.getAmount(), "REFUNDED", payment.getProvider(), "via webhook");
        });
    }

    // ------------------------------------------------------------------
    // Refund / reporting
    // ------------------------------------------------------------------
    @Override
    @Transactional
    public PaymentResponse refundOrder(Long orderId, RefundRequest request) {
        log.info("Refund requested for order {} amount={} idempotencyKey={}",
                orderId, request != null ? request.amount() : null, request != null ? request.idempotencyKey() : null);

        String idempotencyKey = request != null ? request.idempotencyKey() : null;
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = txnRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                log.info("[PAYMENT][refund] idempotent replay for key={}", idempotencyKey);
                Payment payment = paymentRepository.findById(existing.get().getPaymentId())
                        .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", existing.get().getPaymentId()));
                return PaymentMapper.toResponse(payment);
            }
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        Payment payment = paymentRepository.findByOrderId(orderId).stream()
                .filter(p -> p.getStatus() == PaymentState.SUCCESS
                        || p.getStatus() == PaymentState.PARTIALLY_REFUNDED)
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
        if (refundId == null) {
            throw new BadRequestException("Refund could not be processed by the gateway");
        }

        BigDecimal newRefundedTotal = alreadyRefunded.add(refundAmount);
        payment.setRefundedAmount(newRefundedTotal);
        boolean isFullRefund = newRefundedTotal.compareTo(payment.getAmount()) >= 0;
        payment.setStatus(isFullRefund ? PaymentState.REFUNDED : PaymentState.PARTIALLY_REFUNDED);
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
        return PaymentMapper.toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> allPayments(Pageable pageable) {
        return PageResponse.from(paymentRepository.findAll(pageable).map(PaymentMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> history(Long userId, Long orderId) {
        ownedOrder(userId, orderId);
        return paymentRepository.findByOrderId(orderId).stream().map(PaymentMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentTransaction> transactions(Long userId, Long orderId) {
        ownedOrder(userId, orderId);
        return txnRepository.findByOrderIdOrderByCreatedAtAsc(orderId);
    }

    private Order ownedOrder(Long userId, Long orderId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
    }

    private Order ownedOrderForUpdate(Long userId, Long orderId) {
        return orderRepository.findByIdAndUserIdForUpdate(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
    }

    private PaymentTransaction recordTxn(Long orderId, Long paymentId, String gatewayOrderId, String transactionId,
                                         BigDecimal amount, String status, String provider, String note) {
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
