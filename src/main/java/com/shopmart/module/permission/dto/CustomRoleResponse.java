package com.shopmart.module.permission.dto;

import java.util.Set;

public record CustomRoleResponse(
        Long id,
        String name,
        String description,
        boolean active,
        Set<String> permissionCodes
) {}
