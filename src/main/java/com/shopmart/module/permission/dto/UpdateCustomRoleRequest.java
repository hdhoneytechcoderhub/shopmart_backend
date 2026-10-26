package com.shopmart.module.permission.dto;

public record UpdateCustomRoleRequest(
        String name,
        String description,
        Boolean active
) {}
