package com.shopmart.module.permission.controller;

import com.shopmart.common.dto.ApiResponse;
import com.shopmart.module.permission.dto.*;
import com.shopmart.module.permission.service.PermissionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Super-admin-only management of the fine-grained permission matrix: permissions, permission
 * groups, dynamic (custom) roles, and role<->user assignment. Existing @PreAuthorize("hasRole(...)")
 * checks elsewhere in the app are untouched; new/updated endpoints can additionally gate on
 * individual permission codes via @PreAuthorize("hasAuthority('SOME_CODE')").
 */
@RestController
@RequestMapping("/super-admin/permissions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Permission Matrix")
public class PermissionController {

    private final PermissionService service;

    // ---- permissions ----

    @PostMapping
    public ApiResponse<PermissionResponse> createPermission(@Valid @RequestBody CreatePermissionRequest request) {
        return ApiResponse.ok("Permission created", service.createPermission(request));
    }

    @GetMapping
    public ApiResponse<List<PermissionResponse>> listPermissions() {
        return ApiResponse.ok(service.listPermissions());
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deletePermission(@PathVariable Long id) {
        service.deletePermission(id);
        return ApiResponse.message("Permission deleted");
    }

    // ---- permission groups ----

    @PostMapping("/groups")
    public ApiResponse<PermissionGroupResponse> createGroup(@Valid @RequestBody CreatePermissionGroupRequest request) {
        return ApiResponse.ok("Permission group created", service.createGroup(request));
    }

    @GetMapping("/groups")
    public ApiResponse<List<PermissionGroupResponse>> listGroups() {
        return ApiResponse.ok(service.listGroups());
    }

    @DeleteMapping("/groups/{id}")
    public ApiResponse<Void> deleteGroup(@PathVariable Long id) {
        service.deleteGroup(id);
        return ApiResponse.message("Permission group deleted");
    }

    // ---- dynamic roles ----

    @PostMapping("/roles")
    public ApiResponse<CustomRoleResponse> createRole(@Valid @RequestBody CreateCustomRoleRequest request) {
        return ApiResponse.ok("Role created", service.createRole(request));
    }

    @GetMapping("/roles")
    public ApiResponse<List<CustomRoleResponse>> listRoles() {
        return ApiResponse.ok(service.listRoles());
    }

    @GetMapping("/roles/{id}")
    public ApiResponse<CustomRoleResponse> getRole(@PathVariable Long id) {
        return ApiResponse.ok(service.getRole(id));
    }

    @PutMapping("/roles/{id}")
    public ApiResponse<CustomRoleResponse> updateRole(@PathVariable Long id,
                                                       @RequestBody UpdateCustomRoleRequest request) {
        return ApiResponse.ok("Role updated", service.updateRole(id, request));
    }

    @DeleteMapping("/roles/{id}")
    public ApiResponse<Void> deleteRole(@PathVariable Long id) {
        service.deleteRole(id);
        return ApiResponse.message("Role deleted");
    }

    @PutMapping("/roles/{id}/permissions")
    public ApiResponse<CustomRoleResponse> assignPermissions(@PathVariable Long id,
                                                              @Valid @RequestBody AssignPermissionsRequest request) {
        return ApiResponse.ok("Permissions updated", service.assignPermissionsToRole(id, request.permissionIds()));
    }

    // ---- role <-> user assignment ----

    @PostMapping("/roles/{roleId}/users/{userId}")
    public ApiResponse<Void> assignRoleToUser(@PathVariable Long roleId, @PathVariable Long userId) {
        service.assignRoleToUser(roleId, userId);
        return ApiResponse.message("Role assigned");
    }

    @DeleteMapping("/roles/{roleId}/users/{userId}")
    public ApiResponse<Void> removeRoleFromUser(@PathVariable Long roleId, @PathVariable Long userId) {
        service.removeRoleFromUser(roleId, userId);
        return ApiResponse.message("Role removed");
    }

    @GetMapping("/users/{userId}/roles")
    public ApiResponse<List<CustomRoleResponse>> rolesForUser(@PathVariable Long userId) {
        return ApiResponse.ok(service.rolesForUser(userId));
    }
}
