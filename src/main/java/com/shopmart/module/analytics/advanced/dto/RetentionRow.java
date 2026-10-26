package com.shopmart.module.analytics.advanced.dto;

import java.util.List;

/** Same shape as CohortRow but expressed as a retention percentage rather than a raw count. */
public record RetentionRow(String cohortMonth, long cohortSize, List<Double> retentionPercentByMonthOffset) {}
