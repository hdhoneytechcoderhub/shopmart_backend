package com.shopmart.module.report.schedule.controller;

import com.shopmart.common.dto.ApiResponse;
import com.shopmart.common.dto.PageResponse;
import com.shopmart.module.report.schedule.dto.ReportScheduleRequest;
import com.shopmart.module.report.schedule.dto.ReportScheduleResponse;
import com.shopmart.module.report.schedule.service.ReportScheduleService;
import com.shopmart.security.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/report-schedules")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Scheduled Reports")
public class ReportScheduleController {

    private final ReportScheduleService service;

    @PostMapping
    public ApiResponse<ReportScheduleResponse> create(@Valid @RequestBody ReportScheduleRequest request) {
        return ApiResponse.ok("Schedule created", service.create(request, SecurityUtils.currentUserId()));
    }

    @PutMapping("/{id}")
    public ApiResponse<ReportScheduleResponse> update(@PathVariable Long id, @Valid @RequestBody ReportScheduleRequest request) {
        return ApiResponse.ok("Schedule updated", service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.message("Schedule deleted");
    }

    @GetMapping("/{id}")
    public ApiResponse<ReportScheduleResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(service.get(id));
    }

    @GetMapping
    public ApiResponse<PageResponse<ReportScheduleResponse>> list(@PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(service.list(pageable));
    }

    @PostMapping("/{id}/run-now")
    public ApiResponse<ReportScheduleResponse> runNow(@PathVariable Long id) {
        return ApiResponse.ok("Report sent", service.runNow(id));
    }
}
