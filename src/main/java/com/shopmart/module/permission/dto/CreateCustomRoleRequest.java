package com.shopmart.module.permission.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Set;

public record CreateCustomRoleRequest(
        @NotBlank String name,
        String description,
        Set<Long> permissionIds
) {}
