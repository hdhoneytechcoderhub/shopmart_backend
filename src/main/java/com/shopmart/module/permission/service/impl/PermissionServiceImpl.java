package com.shopmart.module.permission.service.impl;

import com.shopmart.common.exception.ConflictException;
import com.shopmart.common.exception.ResourceNotFoundException;
import com.shopmart.module.audit.service.AuditService;
import com.shopmart.module.permission.dto.*;
import com.shopmart.module.permission.entity.CustomRole;
import com.shopmart.module.permission.entity.Permission;
import com.shopmart.module.permission.entity.PermissionGroup;
import com.shopmart.module.permission.repository.CustomRoleRepository;
import com.shopmart.module.permission.repository.PermissionGroupRepository;
import com.shopmart.module.permission.repository.PermissionRepository;
import com.shopmart.module.permission.service.PermissionService;
import com.shopmart.module.user.entity.User;
import com.shopmart.module.user.repository.UserRepository;
import com.shopmart.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;
    private final PermissionGroupRepository permissionGroupRepository;
    private final CustomRoleRepository customRoleRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    // ---- permissions ----

    @Override
    @Transactional
    public PermissionResponse createPermission(CreatePermissionRequest req) {
        String code = req.code().trim().toUpperCase().replace(' ', '_');
        if (permissionRepository.existsByCode(code)) {
            throw new ConflictException("A permission with code '" + code + "' already exists");
        }
        Permission p = new Permission();
        p.setCode(code);
        p.setName(req.name());
        p.setDescription(req.description());
        p.setModule(req.module());
        p.setScreenKey(req.screenKey());
        if (req.groupId() != null) {
            p.setGroup(permissionGroupRepository.findById(req.groupId())
                    .orElseThrow(() -> new ResourceNotFoundException("PermissionGroup", "id", req.groupId())));
        }
        p = permissionRepository.save(p);
        auditService.log(currentUserIdOrNull(), "PERMISSION_CREATED", "Permission", p.getId(), code);
        return toResponse(p);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionResponse> listPermissions() {
        return permissionRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public void deletePermission(Long id) {
        Permission p = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", "id", id));
        permissionRepository.delete(p);
        auditService.log(currentUserIdOrNull(), "PERMISSION_DELETED", "Permission", id, p.getCode());
    }

    // ---- permission groups ----

    @Override
    @Transactional
    public PermissionGroupResponse createGroup(CreatePermissionGroupRequest req) {
        if (permissionGroupRepository.existsByName(req.name())) {
            throw new ConflictException("A permission group named '" + req.name() + "' already exists");
        }
        PermissionGroup g = new PermissionGroup();
        g.setName(req.name());
        g.setDescription(req.description());
        g = permissionGroupRepository.save(g);
        auditService.log(currentUserIdOrNull(), "PERMISSION_GROUP_CREATED", "PermissionGroup", g.getId(), g.getName());
        return toGroupResponse(g);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionGroupResponse> listGroups() {
        return permissionGroupRepository.findAll().stream().map(this::toGroupResponse).toList();
    }

    @Override
    @Transactional
    public void deleteGroup(Long id) {
        PermissionGroup g = permissionGroupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PermissionGroup", "id", id));
        permissionGroupRepository.delete(g);
        auditService.log(currentUserIdOrNull(), "PERMISSION_GROUP_DELETED", "PermissionGroup", id, g.getName());
    }

    // ---- custom (dynamic) roles ----

    @Override
    @Transactional
    public CustomRoleResponse createRole(CreateCustomRoleRequest req) {
        if (customRoleRepository.existsByName(req.name())) {
            throw new ConflictException("A role named '" + req.name() + "' already exists");
        }
        CustomRole role = new CustomRole();
        role.setName(req.name());
        role.setDescription(req.description());
        if (req.permissionIds() != null && !req.permissionIds().isEmpty()) {
            role.setPermissions(new HashSet<>(permissionRepository.findAllById(req.permissionIds())));
        }
        role = customRoleRepository.save(role);
        auditService.log(currentUserIdOrNull(), "CUSTOM_ROLE_CREATED", "CustomRole", role.getId(), role.getName());
        return toRoleResponse(role);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomRoleResponse> listRoles() {
        return customRoleRepository.findAll().stream().map(this::toRoleResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CustomRoleResponse getRole(Long id) {
        return toRoleResponse(findRole(id));
    }

    @Override
    @Transactional
    public CustomRoleResponse updateRole(Long id, UpdateCustomRoleRequest req) {
        CustomRole role = findRole(id);
        if (req.name() != null && !req.name().isBlank()) role.setName(req.name());
        if (req.description() != null) role.setDescription(req.description());
        if (req.active() != null) role.setActive(req.active());
        role = customRoleRepository.save(role);
        auditService.log(currentUserIdOrNull(), "CUSTOM_ROLE_UPDATED", "CustomRole", role.getId(), role.getName());
        return toRoleResponse(role);
    }

    @Override
    @Transactional
    public void deleteRole(Long id) {
        CustomRole role = findRole(id);
        customRoleRepository.delete(role);
        auditService.log(currentUserIdOrNull(), "CUSTOM_ROLE_DELETED", "CustomRole", id, role.getName());
    }

    @Override
    @Transactional
    public CustomRoleResponse assignPermissionsToRole(Long roleId, Set<Long> permissionIds) {
        CustomRole role = findRole(roleId);
        Set<Permission> permissions = permissionIds == null || permissionIds.isEmpty()
                ? new HashSet<>()
                : new HashSet<>(permissionRepository.findAllById(permissionIds));
        role.setPermissions(permissions);
        role = customRoleRepository.save(role);
        auditService.log(currentUserIdOrNull(), "CUSTOM_ROLE_PERMISSIONS_UPDATED", "CustomRole",
                role.getId(), permissions.size() + " permission(s)");
        return toRoleResponse(role);
    }

    // ---- role <-> user assignment ----

    @Override
    @Transactional
    public void assignRoleToUser(Long roleId, Long userId) {
        CustomRole role = findRole(roleId);
        User user = findUser(userId);
        user.getCustomRoles().add(role);
        userRepository.save(user);
        auditService.log(currentUserIdOrNull(), "CUSTOM_ROLE_ASSIGNED", "User", userId,
                role.getName() + " -> " + user.getEmail());
    }

    @Override
    @Transactional
    public void removeRoleFromUser(Long roleId, Long userId) {
        CustomRole role = findRole(roleId);
        User user = findUser(userId);
        user.getCustomRoles().remove(role);
        userRepository.save(user);
        auditService.log(currentUserIdOrNull(), "CUSTOM_ROLE_REMOVED", "User", userId,
                role.getName() + " -x- " + user.getEmail());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomRoleResponse> rolesForUser(Long userId) {
        return findUser(userId).getCustomRoles().stream().map(this::toRoleResponse).toList();
    }

    // ---- effective access ----

    @Override
    @Transactional(readOnly = true)
    public MyPermissionsResponse myPermissions(Long userId) {
        User user = findUser(userId);
        Set<String> baseRoles = user.getRoles().stream().map(Enum::name).collect(Collectors.toSet());
        Set<Permission> effective = user.getCustomRoles().stream()
                .filter(CustomRole::isActive)
                .flatMap(r -> r.getPermissions().stream())
                .collect(Collectors.toSet());
        Set<String> codes = effective.stream().map(Permission::getCode).collect(Collectors.toSet());
        Set<String> screens = effective.stream()
                .map(Permission::getScreenKey)
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.toSet());
        return new MyPermissionsResponse(baseRoles, codes, screens);
    }

    // ---- helpers ----

    private CustomRole findRole(Long id) {
        return customRoleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CustomRole", "id", id));
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    private Long currentUserIdOrNull() {
        return SecurityUtils.currentUserIdOrNull();
    }

    private PermissionResponse toResponse(Permission p) {
        return new PermissionResponse(p.getId(), p.getCode(), p.getName(), p.getDescription(),
                p.getModule(), p.getScreenKey(),
                p.getGroup() != null ? p.getGroup().getId() : null,
                p.getGroup() != null ? p.getGroup().getName() : null);
    }

    private PermissionGroupResponse toGroupResponse(PermissionGroup g) {
        List<PermissionResponse> perms = permissionRepository.findByGroupId(g.getId()).stream()
                .map(this::toResponse).toList();
        return new PermissionGroupResponse(g.getId(), g.getName(), g.getDescription(), perms);
    }

    private CustomRoleResponse toRoleResponse(CustomRole r) {
        Set<String> codes = r.getPermissions().stream().map(Permission::getCode).collect(Collectors.toSet());
        return new CustomRoleResponse(r.getId(), r.getName(), r.getDescription(), r.isActive(), codes);
    }
}
