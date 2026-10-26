package com.shopmart.module.compliance.dto;

import java.time.Instant;

public record ComplianceResponse(
        Long id, Long uploadedBy, String title, String documentUrl,
        String status, String reason, String certificateUrl, Instant createdAt
) {}
