package com.shopmart.module.usermgmt.controller;

import com.shopmart.common.dto.ApiResponse;
import com.shopmart.common.dto.PageResponse;
import com.shopmart.module.superadmin.dto.AdminCreatedResponse;
import com.shopmart.module.superadmin.dto.AdminResponse;
import com.shopmart.module.superadmin.dto.CreateAdminRequest;
import com.shopmart.module.user.entity.Role;
import com.shopmart.module.usermgmt.service.UserManagementService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Super Admin: manage engineers and customers. */
@RestController
@RequestMapping("/super-admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Super Admin - Users")
public class UserManagementController {

    private final UserManagementService service;

    // ----- Engineers (full CRUD) -----
    @PostMapping("/engineers")
    public ApiResponse<AdminCreatedResponse> createEngineer(@Valid @RequestBody CreateAdminRequest req) {
        return ApiResponse.ok("Engineer created", service.createUser(req, Role.ROLE_ENGINEER));
    }

    @GetMapping("/engineers")
    public ApiResponse<PageResponse<AdminResponse>> engineers(@PageableDefault(size = 10) Pageable pageable) {
        return ApiResponse.ok(service.listByRole(Role.ROLE_ENGINEER, pageable));
    }

    // ----- Customers (management: list, view, enable/disable) -----
    @GetMapping("/customers")
    public ApiResponse<PageResponse<AdminResponse>> customers(@PageableDefault(size = 10) Pageable pageable) {
        return ApiResponse.ok(service.listByRole(Role.ROLE_CUSTOMER, pageable));
    }

    // ----- Shared user ops -----
    @GetMapping("/users/{id}")
    public ApiResponse<AdminResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(service.getUser(id));
    }

    @PutMapping("/users/{id}/status")
    public ApiResponse<AdminResponse> setStatus(@PathVariable Long id, @RequestParam boolean enabled) {
        return ApiResponse.ok("User status updated", service.setEnabled(id, enabled));
    }

    @DeleteMapping("/users/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.softDelete(id);
        return ApiResponse.message("User deleted");
    }
}
