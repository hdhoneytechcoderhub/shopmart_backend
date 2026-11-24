package com.shopmart.module.order.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AdminOrderSummary(
        Long id,
        String orderNumber,
        String customerName,
        String customerEmail,
        String customerPhone,
        String status,
        String paymentStatus,
        String paymentMethod,
        BigDecimal total,
        int itemCount,
        String razorpayOrderId,
        String razorpayPaymentId,
        Instant createdAt
) {}
