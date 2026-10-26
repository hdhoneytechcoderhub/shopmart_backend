package com.shopmart.module.payment.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Accepts both our canonical field names and the raw field names Razorpay's
 * checkout.js callback hands back to the frontend (razorpay_order_id, etc.),
 * so the frontend can forward the checkout.js response with no remapping.
 */
public record VerifyPaymentRequest(
        @NotNull Long orderId,
        @NotBlank @JsonAlias("razorpay_order_id") String gatewayRef,
        @NotBlank @JsonAlias("razorpay_payment_id") String transactionId,
        @NotBlank @JsonAlias("razorpay_signature") String signature
) {}
