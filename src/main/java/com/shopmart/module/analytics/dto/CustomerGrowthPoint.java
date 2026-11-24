package com.shopmart.module.analytics.dto;

/**
 * One calendar month of customer sign-ups. `month` is an ISO year-month (e.g. "2026-11");
 * the admin chart parses it with `new Date(...)`.
 */
public record CustomerGrowthPoint(String month, long newCustomers) {}
