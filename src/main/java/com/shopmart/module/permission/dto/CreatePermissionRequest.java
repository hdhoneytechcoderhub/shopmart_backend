package com.shopmart.module.permission.dto;

import jakarta.validation.constraints.NotBlank;

public record CreatePermissionRequest(
        @NotBlank String code,
        @NotBlank String name,
        String description,
        String module,
        String screenKey,
        Long groupId
) {}
