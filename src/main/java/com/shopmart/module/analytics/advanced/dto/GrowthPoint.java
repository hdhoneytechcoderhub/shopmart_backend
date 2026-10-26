package com.shopmart.module.analytics.advanced.dto;

import java.math.BigDecimal;

public record GrowthPoint(
        String month, BigDecimal revenue, Double revenueGrowthPct,
        long orders, Double orderGrowthPct,
        long newCustomers, Double customerGrowthPct
) {}
