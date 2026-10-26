package com.shopmart.module.payment.gateway;

import java.math.BigDecimal;

/**
 * Abstraction over an external payment provider (Razorpay, Stripe, ...).
 * The default {@link StubPaymentGateway} simulates the provider so the flow is
 * end-to-end testable without live credentials. Provide a real implementation
 * (marked @Primary or via @ConditionalOnProperty) in Phase 2.
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
     * rather than checked generically. The default is fail-closed: any gateway that doesn't
     * override this rejects all webhooks rather than silently accepting unverified ones.
     */
    default boolean verifyWebhookSignature(String payload, String signature) {
        return false;
    }

    /** Outcome of asking the gateway directly what a transaction's current state is. */
    enum GatewayStatus { SUCCESS, FAILED, PENDING, UNKNOWN }

    /**
     * Reconciliation hook: ask the gateway directly for a transaction's current status,
     * rather than relying solely on the checkout callback or webhook (either of which can
     * be missed — e.g. the customer closes the tab before the callback fires, or a webhook
     * delivery fails). Default UNKNOWN for gateways that don't implement it, so the
     * reconciliation job simply skips those payments rather than guessing.
     */
    default GatewayStatus checkStatus(String gatewayRef, String transactionId) {
        return GatewayStatus.UNKNOWN;
    }
}
