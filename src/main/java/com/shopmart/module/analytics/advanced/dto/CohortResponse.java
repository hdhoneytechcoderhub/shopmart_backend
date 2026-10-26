package com.shopmart.module.analytics.advanced.dto;

import java.util.List;

public record CohortResponse(List<CohortRow> cohorts, int monthsTracked) {}
