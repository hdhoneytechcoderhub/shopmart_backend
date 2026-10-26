package com.shopmart.module.report.schedule.dto;

import com.shopmart.module.report.schedule.entity.ReportFrequency;
import com.shopmart.module.report.schedule.entity.ReportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReportScheduleRequest(
        @NotBlank String name,
        @NotNull ReportType reportType,
        @NotNull ReportFrequency frequency,
        @NotBlank String recipientEmails,
        Boolean enabled
) {}
