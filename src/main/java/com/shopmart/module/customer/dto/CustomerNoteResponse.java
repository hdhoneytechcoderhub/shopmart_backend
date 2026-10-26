package com.shopmart.module.customer.dto;

import java.time.Instant;

public record CustomerNoteResponse(
        Long id,
        Long customerId,
        Long authorId,
        String authorName,
        String note,
        Instant createdAt
) {}
