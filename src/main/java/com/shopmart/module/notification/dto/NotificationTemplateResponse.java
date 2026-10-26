package com.shopmart.module.notification.dto;

import com.shopmart.module.notification.entity.NotificationChannel;

import java.time.Instant;

public record NotificationTemplateResponse(
        Long id, String code, NotificationChannel channel, String subject, String body,
        String description, boolean active, Instant createdAt, Instant updatedAt
) {}
