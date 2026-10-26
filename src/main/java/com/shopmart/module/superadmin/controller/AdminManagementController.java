package com.shopmart.module.superadmin.controller;

import com.shopmart.common.dto.ApiResponse;
import com.shopmart.common.dto.PageResponse;
import com.shopmart.module.superadmin.dto.AdminCreatedResponse;
import com.shopmart.module.superadmin.dto.AdminResponse;
import com.shopmart.module.superadmin.dto.CreateAdminRequest;
import com.shopmart.module.superadmin.dto.UpdateAdminRequest;
import com.shopmart.module.superadmin.service.SuperAdminService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Super Admin: full Admin management CRUD. */
@RestController
@RequestMapping("/super-admin/admins")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Super Admin - Admins")
public class AdminManagementController {

    private final SuperAdminService service;

    @PostMapping
    public ApiResponse<AdminCreatedResponse> create(@Valid @RequestBody CreateAdminRequest req) {
        return ApiResponse.ok("Admin created", service.createAdmin(req));
    }

    @GetMapping
    public ApiResponse<PageResponse<AdminResponse>> list(@PageableDefault(size = 10) Pageable pageable) {
        return ApiResponse.ok(service.listAdmins(pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(service.getAdmin(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<AdminResponse> update(@PathVariable Long id,
                                             @Valid @RequestBody UpdateAdminRequest req) {
        return ApiResponse.ok("Admin updated", service.updateAdmin(id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.deleteAdmin(id);
        return ApiResponse.message("Admin deleted");
    }
}
