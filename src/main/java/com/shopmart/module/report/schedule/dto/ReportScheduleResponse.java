package com.shopmart.module.report.schedule.dto;

import com.shopmart.module.report.schedule.entity.ReportFrequency;
import com.shopmart.module.report.schedule.entity.ReportType;

import java.time.Instant;

public record ReportScheduleResponse(
        Long id, String name, ReportType reportType, ReportFrequency frequency,
        String recipientEmails, boolean enabled, Instant nextRunAt, Instant lastRunAt
) {}
