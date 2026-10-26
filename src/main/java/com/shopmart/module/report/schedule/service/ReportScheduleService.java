package com.shopmart.module.report.schedule.service;

import com.shopmart.common.dto.PageResponse;
import com.shopmart.module.report.schedule.dto.ReportScheduleRequest;
import com.shopmart.module.report.schedule.dto.ReportScheduleResponse;
import org.springframework.data.domain.Pageable;

public interface ReportScheduleService {
    ReportScheduleResponse create(ReportScheduleRequest request, Long adminId);
    ReportScheduleResponse update(Long id, ReportScheduleRequest request);
    void delete(Long id);
    ReportScheduleResponse get(Long id);
    PageResponse<ReportScheduleResponse> list(Pageable pageable);
    ReportScheduleResponse runNow(Long id);

    /** Called by the scheduler for every due schedule. */
    void processDueSchedules();
}
