package com.shopmart.module.customer.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record CustomerInsightsResponse(
        Long customerId,
        String name,
        String email,
        CustomerSegment segment,
        Instant registeredAt,
        long totalOrders,
        long paidOrders,
        BigDecimal totalSpent,
        BigDecimal averageOrderValue,
        long reviewCount,
        long activeSessionCount,
        Instant lastOrderAt,
        Instant lastLoginAt
) {}
