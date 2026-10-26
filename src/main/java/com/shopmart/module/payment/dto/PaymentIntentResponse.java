package com.shopmart.module.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Returned when a payment is initiated. For gateway methods, the client uses
 * {@code gatewayRef} (serialized as "razorpayOrderId") to launch the checkout widget,
 * together with {@code keyId} (the gateway's public/client key).
 * For COD, {@code requiresGatewayCheckout} is false and the order is already confirmed.
 */
public record PaymentIntentResponse(
        Long paymentId,
        Long orderId,
        String provider,
        @JsonProperty("razorpayOrderId") String gatewayRef,
        BigDecimal amount,
        String currency,
        String status,
        boolean requiresGatewayCheckout,
        String keyId
) {}
