package com.shopmart.module.settings.dto;

import jakarta.validation.constraints.NotBlank;

public record SettingRequest(
        @NotBlank(message = "key is required") String key,
        String value,
        Boolean isPublic
) {}
