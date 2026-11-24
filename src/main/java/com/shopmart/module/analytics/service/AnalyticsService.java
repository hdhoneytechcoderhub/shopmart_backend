package com.shopmart.module.analytics.service;

import com.shopmart.module.analytics.dto.*;

import java.util.List;

public interface AnalyticsService {
    DashboardResponse dashboard();
    List<SalesPoint> salesOverTime(int days);
    /** Calendar-month revenue/order series, zero-filled up to and including the current month. */
    List<SalesPoint> salesMonthly(int months);
    /** New customers per calendar month, zero-filled for months with no sign-ups. */
    List<CustomerGrowthPoint> customerGrowth(int months);
    List<TopProduct> topProducts(int limit);
    List<StatusCount> orderStatusBreakdown();
    List<LowStockProduct> lowStock(int threshold);
    CustomerAnalyticsResponse customerAnalytics();

    RealTimeStatsResponse realTimeStats();

    RevenueAnalyticsResponse revenueAnalytics(String period, int periods);
}
