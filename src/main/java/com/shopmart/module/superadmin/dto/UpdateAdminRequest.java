package com.shopmart.module.superadmin.dto;

import jakarta.validation.constraints.Size;

public record UpdateAdminRequest(
        String name,
        String phone,
        Boolean enabled,
        @Size(min = 8, message = "Password must be at least 8 characters") String password
) {}
