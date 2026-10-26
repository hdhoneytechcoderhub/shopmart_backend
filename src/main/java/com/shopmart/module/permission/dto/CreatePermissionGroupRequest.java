package com.shopmart.module.permission.dto;

import jakarta.validation.constraints.NotBlank;

public record CreatePermissionGroupRequest(
        @NotBlank String name,
        String description
) {}
