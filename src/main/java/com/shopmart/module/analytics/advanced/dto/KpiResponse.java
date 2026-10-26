package com.shopmart.module.analytics.advanced.dto;

import java.math.BigDecimal;

public record KpiResponse(
        BigDecimal totalRevenue,
        long totalOrders,
        long totalCustomers,
        BigDecimal averageOrderValue,
        double repeatCustomerRatePct,
        Double lastMonthRevenueGrowthPct,
        Double lastMonthOrderGrowthPct
) {}
