package com.shopmart.module.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Returned when a payment is initiated. The client uses {@code razorpayOrderId}
 * to launch the checkout widget with {@code keyId}, and must use the
 * backend-supplied {@code amountMinor} (paise) — never a client-calculated amount.
 */
public record PaymentIntentResponse(
        Long paymentId,
        Long orderId,
        String provider,
        @JsonProperty("razorpayOrderId") String gatewayRef,
        BigDecimal amount,
        long amountMinor,
        String currency,
        String status,
        boolean requiresGatewayCheckout,
        String keyId
) {}
