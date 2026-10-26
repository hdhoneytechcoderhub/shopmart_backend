package com.shopmart.module.notification.dto;

import com.shopmart.module.notification.entity.CampaignStatus;
import com.shopmart.module.notification.entity.CampaignTargetType;
import com.shopmart.module.notification.entity.NotificationChannel;

import java.time.Instant;

public record CampaignResponse(
        Long id, String name, NotificationChannel channel, CampaignTargetType targetType,
        String targetRole, String targetSegment, String title, String message, String link,
        CampaignStatus status, Instant scheduledAt, Instant sentAt, int recipientCount,
        String failureReason, Instant createdAt
) {}
