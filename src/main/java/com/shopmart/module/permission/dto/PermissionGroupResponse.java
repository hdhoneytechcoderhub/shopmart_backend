package com.shopmart.module.permission.dto;

import java.util.List;

public record PermissionGroupResponse(
        Long id,
        String name,
        String description,
        List<PermissionResponse> permissions
) {}
