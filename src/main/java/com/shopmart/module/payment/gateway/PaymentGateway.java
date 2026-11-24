package com.shopmart.module.payment.gateway;

import java.math.BigDecimal;

/**
 * Abstraction over an external payment provider (Razorpay, Stripe, ...).
 * The active provider is selected with app.payments.provider. The storefront
 * requires a real gateway; there is no "assume success" fallback.
 */
public interface PaymentGateway {

    String provider();

    /** Create an order/intent on the provider side; returns the provider reference. */
    String createGatewayOrder(Long orderId, BigDecimal amount, String currency);

    /** Verify the signature returned by the provider after checkout. */
    boolean verifySignature(String gatewayRef, String transactionId, String signature);

    /**
     * Client-facing public key/id the frontend needs to open the checkout widget
     * (e.g. Razorpay's key_id). Null for gateways that don't need one client-side.
     */
    default String publicKey() {
        return null;
    }

    /** Refund a captured payment. Returns a gateway refund id, or null if not performed. */
    default String refund(String transactionId, java.math.BigDecimal amount, String currency) {
        return null;
    }

    /**
     * Verify an inbound async webhook/callback notification from this provider.
     * Each gateway uses its own signature scheme, so this MUST be implemented per-gateway
     * rather than checked generically. The default is fail-closed.
     */
    default boolean verifyWebhookSignature(String payload, String signature) {
        return false;
    }

    /** Outcome of asking the gateway directly what a transaction's current state is. */
    enum GatewayStatus { SUCCESS, AUTHORIZED, FAILED, PENDING, UNKNOWN }

    /** Authoritative view of a provider payment/order, fetched server-to-server. */
    record GatewayPayment(
            String gatewayOrderId,
            String paymentId,
            GatewayStatus status,
            BigDecimal amount,
            String currency,
            String method,
            String failureReason
    ) {}

    /**
     * Ask the gateway for the authoritative payment state. Implementations must return
     * null / UNKNOWN when the state cannot be confirmed; callers fail closed.
     * When transactionId is supplied it is checked directly, otherwise the order's
     * payment list is inspected for a captured payment.
     */
    default GatewayPayment fetchPayment(String gatewayRef, String transactionId) {
        return null;
    }

    /**
     * Capture an authorised (not yet captured) payment. Returns true when the gateway
     * accepted the capture request. Callers re-fetch the payment before confirming.
     */
    default boolean capturePayment(String paymentId, BigDecimal amount, String currency) {
        return false;
    }

    /**
     * Reconciliation hook: ask the gateway directly for a transaction's current status.
     * Default UNKNOWN for gateways that don't implement it.
     */
    default GatewayStatus checkStatus(String gatewayRef, String transactionId) {
        return GatewayStatus.UNKNOWN;
    }
}
