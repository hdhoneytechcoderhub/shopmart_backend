package com.shopmart.module.analytics.advanced.dto;

import java.util.List;

/**
 * "forecast" here is a simple linear trend projected from historical monthly revenue
 * (ordinary least squares over the trailing window) — not a machine-learning model. It's
 * a defensible baseline, not a claim of statistical sophistication; swap in a proper
 * forecasting library/service if you need seasonality-aware predictions.
 */
public record RevenueForecastResponse(
        List<MonthValue> historical,
        List<MonthValue> forecast,
        String method
) {}
