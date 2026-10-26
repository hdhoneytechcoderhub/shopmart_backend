package com.shopmart.module.notification.dto;

import com.shopmart.module.notification.entity.CampaignTargetType;
import com.shopmart.module.notification.entity.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CampaignRequest(
        @NotBlank String name,
        NotificationChannel channel,
        @NotNull CampaignTargetType targetType,
        String targetRole,      // required when targetType = ROLE (e.g. "ROLE_CUSTOMER")
        String targetSegment,   // required when targetType = SEGMENT (e.g. "VIP")
        Long templateId,        // optional: overrides title/message with the template's content at send time
        @NotBlank String title,
        @NotBlank String message,
        String link,
        Instant scheduledAt     // null = available to send immediately
) {}
