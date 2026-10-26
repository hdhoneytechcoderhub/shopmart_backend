package com.shopmart.module.audit.dto;

import java.time.Instant;

public record AuditLogResponse(
        Long id, Long userId, String action, String entityType,
        Long entityId, String detail, Instant createdAt
) {}
