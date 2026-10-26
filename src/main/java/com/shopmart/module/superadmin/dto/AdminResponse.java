package com.shopmart.module.superadmin.dto;

import java.time.Instant;
import java.util.Set;

public record AdminResponse(
        Long id, String name, String email, String phone,
        boolean enabled, Set<String> roles, Instant createdAt
) {}
