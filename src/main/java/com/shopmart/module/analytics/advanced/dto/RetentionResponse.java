package com.shopmart.module.analytics.advanced.dto;

import java.util.List;

public record RetentionResponse(List<RetentionRow> cohorts, int monthsTracked) {}
