package com.shopmart.module.servicedesk.dto;

import java.math.BigDecimal;

public record ServiceItemResponse(
        Long id, String name, String description, BigDecimal price,
        Integer durationMinutes, Long categoryId, String imageUrl, boolean active
) {}
