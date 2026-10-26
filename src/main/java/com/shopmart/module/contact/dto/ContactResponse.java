package com.shopmart.module.contact.dto;

import java.time.Instant;

public record ContactResponse(
        Long id, String name, String email, String phone,
        String subject, String message, boolean handled, Instant createdAt
) {}
