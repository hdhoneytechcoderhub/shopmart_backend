package com.shopmart.module.notification.dto;

import com.shopmart.module.notification.entity.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NotificationTemplateRequest(
        @NotBlank String code,
        @NotNull NotificationChannel channel,
        String subject,
        @NotBlank String body,
        String description,
        Boolean active
) {}
