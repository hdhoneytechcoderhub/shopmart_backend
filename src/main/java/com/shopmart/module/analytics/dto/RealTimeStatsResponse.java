package com.shopmart.module.analytics.dto;

import com.shopmart.module.order.dto.OrderSummary;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record RealTimeStatsResponse(
        Instant serverTime,
        BigDecimal todayRevenue,
        long todayOrders,
        long todayNewCustomers,
        long activeOrders,
        long pendingOrders,
        List<OrderSummary> recentOrders
) {}
