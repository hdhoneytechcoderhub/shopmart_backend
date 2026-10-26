package com.shopmart.module.servicedesk.dto;

import java.math.BigDecimal;

public record ServiceReportResponse(
        long totalBookings,
        long requested,
        long assigned,
        long inProgress,
        long completed,
        long cancelled,
        BigDecimal completedRevenue,
        Double averageRating
) {}
