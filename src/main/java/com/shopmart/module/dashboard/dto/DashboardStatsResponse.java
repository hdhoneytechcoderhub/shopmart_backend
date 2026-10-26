package com.shopmart.module.dashboard.dto;

import java.math.BigDecimal;

public record DashboardStatsResponse(
        long totalAdmins,
        long totalProducts,
        long totalCategories,
        long totalMachines,
        long totalServices,
        long totalOrders,
        long totalCustomers,
        BigDecimal revenue
) {}
