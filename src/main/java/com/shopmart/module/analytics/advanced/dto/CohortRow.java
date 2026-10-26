package com.shopmart.module.analytics.advanced.dto;

import java.util.List;

/**
 * retainedByMonthOffset[0] = customers from this cohort who ordered in their registration
 * month; [1] = who ordered again exactly 1 calendar month later; etc.
 */
public record CohortRow(String cohortMonth, long cohortSize, List<Long> retainedByMonthOffset) {}
