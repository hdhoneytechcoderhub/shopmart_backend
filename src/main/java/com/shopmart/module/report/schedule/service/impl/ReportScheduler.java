package com.shopmart.module.report.schedule.service.impl;

import com.shopmart.module.report.schedule.service.ReportScheduleService;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.reports.scheduled.enabled", havingValue = "true", matchIfMissing = true)
public class ReportScheduler {

    private final ReportScheduleService scheduleService;

    // Checked hourly; each schedule's own nextRunAt controls actual send timing, so an hourly
    // poll is precise enough for daily/weekly/monthly frequencies without needing cron parsing.
    @Scheduled(fixedDelay = 3600000)
    @SchedulerLock(name = "reportScheduler", lockAtMostFor = "PT30M", lockAtLeastFor = "PT1M")
    public void run() {
        scheduleService.processDueSchedules();
    }
}
