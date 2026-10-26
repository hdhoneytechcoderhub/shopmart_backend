package com.shopmart.module.customer.dto;

import java.time.Instant;

public record CustomerTimelineEvent(
        TimelineEventType type,
        String title,
        String description,
        Instant occurredAt
) {}
