package com.shopmart.module.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AdminOrderDetail(
        Long id,
        String orderNumber,
        String status,
        String paymentStatus,
        String paymentMethod,
        BigDecimal subtotal,
        BigDecimal shippingFee,
        BigDecimal discount,
        BigDecimal total,
        String customerName,
        String customerEmail,
        String customerPhone,
        ShippingAddressDto shippingAddress,
        List<OrderItemResponse> items,
        String razorpayOrderId,
        String razorpayPaymentId,
        Instant paidAt,
        Instant createdAt,
        List<StatusHistoryResponse> history
) {}
