package com.shopmart.module.analytics.advanced.service;

import com.shopmart.module.analytics.advanced.dto.*;

public interface AdvancedAnalyticsService {
    RevenueForecastResponse revenueForecast(int historyMonths, int forecastMonths);
    CohortResponse customerCohorts(int cohortMonths, int trackMonths);
    RetentionResponse retentionAnalysis(int cohortMonths, int trackMonths);
    GrowthTrendResponse growthTrend(int months);
    KpiResponse kpis();
}
