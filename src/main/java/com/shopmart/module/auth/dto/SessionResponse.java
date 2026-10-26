package com.shopmart.module.auth.dto;

import java.time.Instant;

public record SessionResponse(
        Long id,
        String deviceInfo,
        String ipAddress,
        Instant createdAt,
        Instant lastUsedAt,
        Instant expiresAt,
        boolean current
) {}
