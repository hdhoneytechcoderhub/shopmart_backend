package com.shopmart.module.report.schedule.repository;

import com.shopmart.module.report.schedule.entity.ReportSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface ReportScheduleRepository extends JpaRepository<ReportSchedule, Long> {
    List<ReportSchedule> findByEnabledTrueAndNextRunAtLessThanEqual(Instant now);
}
