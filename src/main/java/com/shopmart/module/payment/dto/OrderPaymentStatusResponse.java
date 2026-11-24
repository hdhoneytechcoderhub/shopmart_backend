package com.shopmart.module.payment.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderPaymentStatusResponse(
        Long orderId,
        String orderNumber,
        String orderStatus,
        String paymentStatus,
        String razorpayOrderId,
        String razorpayPaymentId,
        BigDecimal total,
        Instant paidAt
) {}
