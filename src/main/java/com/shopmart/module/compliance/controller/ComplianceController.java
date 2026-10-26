package com.shopmart.module.compliance.controller;

import com.shopmart.common.dto.ApiResponse;
import com.shopmart.module.compliance.dto.ComplianceDecisionRequest;
import com.shopmart.module.compliance.dto.ComplianceResponse;
import com.shopmart.module.compliance.dto.ComplianceUploadRequest;
import com.shopmart.module.compliance.service.ComplianceService;
import com.shopmart.security.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/compliance")
@RequiredArgsConstructor
@Tag(name = "Compliance")
public class ComplianceController {

    private final ComplianceService service;

    // Admin uploads a compliance document
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<ComplianceResponse> upload(@Valid @RequestBody ComplianceUploadRequest req) {
        return ApiResponse.ok("Compliance uploaded", service.upload(SecurityUtils.currentUserId(), req));
    }

    // Real file upload variant (Phase 5 FileStorageService integration) — takes an actual file
    // instead of requiring the caller to already have a hosted URL.
    @PostMapping(value = "/upload-file", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<ComplianceResponse> uploadFile(
            @RequestParam String title,
            @RequestParam org.springframework.web.multipart.MultipartFile file) {
        return ApiResponse.ok("Compliance document uploaded", service.uploadFile(SecurityUtils.currentUserId(), title, file));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<List<ComplianceResponse>> list(@RequestParam(required = false) String status) {
        return ApiResponse.ok(service.list(status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<ComplianceResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(service.get(id));
    }

    // Super Admin approves / rejects
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<ComplianceResponse> approve(@PathVariable Long id,
                                                   @RequestBody(required = false) ComplianceDecisionRequest req) {
        return ApiResponse.ok("Compliance approved", service.approve(id, req));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<ComplianceResponse> reject(@PathVariable Long id,
                                                  @RequestBody(required = false) ComplianceDecisionRequest req) {
        return ApiResponse.ok("Compliance rejected", service.reject(id, req));
    }

    // Certificate download (returns the certificate URL once approved)
    @GetMapping("/{id}/certificate")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<String> certificate(@PathVariable Long id) {
        return ApiResponse.ok(service.certificate(id));
    }
}
