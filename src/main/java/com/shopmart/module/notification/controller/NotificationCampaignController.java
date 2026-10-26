package com.shopmart.module.notification.controller;

import com.shopmart.common.dto.ApiResponse;
import com.shopmart.common.dto.PageResponse;
import com.shopmart.module.notification.dto.CampaignRequest;
import com.shopmart.module.notification.dto.CampaignResponse;
import com.shopmart.module.notification.dto.CampaignStatsResponse;
import com.shopmart.module.notification.entity.CampaignStatus;
import com.shopmart.module.notification.service.NotificationCampaignService;
import com.shopmart.security.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/campaigns")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
@Tag(name = "Notification Campaigns")
public class NotificationCampaignController {

    private final NotificationCampaignService service;

    @PostMapping
    public ApiResponse<CampaignResponse> create(@Valid @RequestBody CampaignRequest request) {
        return ApiResponse.ok("Campaign created", service.create(request, SecurityUtils.currentUserId()));
    }

    @PutMapping("/{id}")
    public ApiResponse<CampaignResponse> update(@PathVariable Long id, @Valid @RequestBody CampaignRequest request) {
        return ApiResponse.ok("Campaign updated", service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.message("Campaign deleted");
    }

    @GetMapping("/{id}")
    public ApiResponse<CampaignResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(service.get(id));
    }

    @GetMapping
    public ApiResponse<PageResponse<CampaignResponse>> list(@RequestParam(required = false) CampaignStatus status,
                                                             @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(service.list(status, pageable));
    }

    @PostMapping("/{id}/send")
    public ApiResponse<CampaignResponse> sendNow(@PathVariable Long id) {
        return ApiResponse.ok("Campaign sent", service.sendNow(id));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<CampaignResponse> cancel(@PathVariable Long id) {
        return ApiResponse.ok("Campaign cancelled", service.cancel(id));
    }

    @GetMapping("/stats")
    public ApiResponse<CampaignStatsResponse> stats() {
        return ApiResponse.ok(service.stats());
    }
}
