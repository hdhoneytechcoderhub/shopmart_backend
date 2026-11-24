package com.shopmart.module.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderSummary(
        Long id,
        String orderNumber,
        String status,
        String paymentStatus,
        String paymentMethod,
        BigDecimal total,
        int itemCount,
        List<OrderItemResponse> items,
        String razorpayPaymentId,
        Instant placedAt
) {}
