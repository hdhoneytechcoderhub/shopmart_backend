package com.shopmart.module.permission.service;

import com.shopmart.module.permission.dto.*;

import java.util.List;
import java.util.Set;

public interface PermissionService {

    // ---- permissions ----
    PermissionResponse createPermission(CreatePermissionRequest request);
    List<PermissionResponse> listPermissions();
    void deletePermission(Long id);

    // ---- permission groups ----
    PermissionGroupResponse createGroup(CreatePermissionGroupRequest request);
    List<PermissionGroupResponse> listGroups();
    void deleteGroup(Long id);

    // ---- custom (dynamic) roles ----
    CustomRoleResponse createRole(CreateCustomRoleRequest request);
    List<CustomRoleResponse> listRoles();
    CustomRoleResponse getRole(Long id);
    CustomRoleResponse updateRole(Long id, UpdateCustomRoleRequest request);
    void deleteRole(Long id);
    CustomRoleResponse assignPermissionsToRole(Long roleId, Set<Long> permissionIds);

    // ---- role <-> user assignment ----
    void assignRoleToUser(Long roleId, Long userId);
    void removeRoleFromUser(Long roleId, Long userId);
    List<CustomRoleResponse> rolesForUser(Long userId);

    // ---- effective access, for screen-level checks ----
    MyPermissionsResponse myPermissions(Long userId);
}
