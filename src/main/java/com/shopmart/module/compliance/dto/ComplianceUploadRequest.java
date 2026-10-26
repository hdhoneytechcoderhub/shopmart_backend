package com.shopmart.module.compliance.dto;

import jakarta.validation.constraints.NotBlank;

public record ComplianceUploadRequest(
        @NotBlank(message = "Title is required") String title,
        @NotBlank(message = "documentUrl is required") String documentUrl
) {}
