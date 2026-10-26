package com.shopmart.module.analytics.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Revenue trend for a bucketed window (daily / weekly / monthly), plus a
 * comparison against the immediately preceding window of equal length.
 */
public record RevenueAnalyticsResponse(
        String period,
        BigDecimal currentPeriodRevenue,
        BigDecimal previousPeriodRevenue,
        BigDecimal growthPct,
        List<SalesPoint> series
) {}
