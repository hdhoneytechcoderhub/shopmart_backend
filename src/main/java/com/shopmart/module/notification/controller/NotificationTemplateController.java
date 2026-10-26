package com.shopmart.module.notification.controller;

import com.shopmart.common.dto.ApiResponse;
import com.shopmart.common.dto.PageResponse;
import com.shopmart.module.notification.dto.NotificationTemplateRequest;
import com.shopmart.module.notification.dto.NotificationTemplateResponse;
import com.shopmart.module.notification.entity.NotificationChannel;
import com.shopmart.module.notification.service.NotificationTemplateService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Unified CRUD for message templates across channels (in-app, email, push, SMS) — see
 * NotificationTemplate's javadoc for why this is one endpoint set instead of three
 * near-duplicate ones for "Notification/Email/Push Templates".
 */
@RestController
@RequestMapping("/admin/notification-templates")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
@Tag(name = "Notification Templates")
public class NotificationTemplateController {

    private final NotificationTemplateService service;

    @PostMapping
    public ApiResponse<NotificationTemplateResponse> create(@Valid @RequestBody NotificationTemplateRequest request) {
        return ApiResponse.ok("Template created", service.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<NotificationTemplateResponse> update(@PathVariable Long id,
                                                             @Valid @RequestBody NotificationTemplateRequest request) {
        return ApiResponse.ok("Template updated", service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.message("Template deleted");
    }

    @GetMapping("/{id}")
    public ApiResponse<NotificationTemplateResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(service.get(id));
    }

    @GetMapping
    public ApiResponse<PageResponse<NotificationTemplateResponse>> list(
            @RequestParam(required = false) NotificationChannel channel,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(service.list(channel, pageable));
    }
}
