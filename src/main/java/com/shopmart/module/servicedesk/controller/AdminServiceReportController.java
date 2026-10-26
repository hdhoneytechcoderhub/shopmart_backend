package com.shopmart.module.servicedesk.controller;

import com.shopmart.common.dto.ApiResponse;
import com.shopmart.module.servicedesk.dto.ServiceReportResponse;
import com.shopmart.module.servicedesk.service.ServiceDeskService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
@Tag(name = "Admin - Service Report")
public class AdminServiceReportController {

    private final ServiceDeskService service;

    @GetMapping("/services")
    public ApiResponse<ServiceReportResponse> serviceReport() {
        return ApiResponse.ok(service.report());
    }
}
