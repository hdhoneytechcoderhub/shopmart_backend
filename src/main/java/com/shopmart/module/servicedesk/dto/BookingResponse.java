package com.shopmart.module.servicedesk.dto;

import java.time.Instant;

public record BookingResponse(
        Long id, Long userId, Long serviceItemId, Long engineerId,
        String status, Instant scheduledAt, String address, String phone,
        String notes, String completionNotes, Integer rating, String ratingComment,
        Instant createdAt
) {}
