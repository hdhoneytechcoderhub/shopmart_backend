package com.shopmart.module.settings.controller;

import com.shopmart.common.dto.ApiResponse;
import com.shopmart.module.settings.dto.SettingRequest;
import com.shopmart.module.settings.dto.SettingResponse;
import com.shopmart.module.settings.service.SettingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Settings")
public class SettingController {

    private final SettingService service;

    // Public: storefront reads public settings (site name, support email, etc.)
    @GetMapping("/settings/public")
    public ApiResponse<List<SettingResponse>> publicSettings() {
        return ApiResponse.ok(service.listPublic());
    }

    // Admin/Super Admin: full settings management
    @GetMapping("/admin/settings")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<List<SettingResponse>> all() {
        return ApiResponse.ok(service.listAll());
    }

    @PutMapping("/admin/settings")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<SettingResponse> upsert(@Valid @RequestBody SettingRequest req) {
        return ApiResponse.ok("Setting saved", service.upsert(req));
    }

    @DeleteMapping("/admin/settings/{key}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<Void> delete(@PathVariable String key) {
        service.delete(key);
        return ApiResponse.message("Setting deleted");
    }
}
