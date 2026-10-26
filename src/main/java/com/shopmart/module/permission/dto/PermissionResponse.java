package com.shopmart.module.permission.dto;

public record PermissionResponse(
        Long id,
        String code,
        String name,
        String description,
        String module,
        String screenKey,
        Long groupId,
        String groupName
) {}
