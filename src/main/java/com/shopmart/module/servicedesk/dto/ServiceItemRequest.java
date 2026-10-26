package com.shopmart.module.servicedesk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record ServiceItemRequest(
        @NotBlank(message = "Name is required") String name,
        String description,
        @NotNull @PositiveOrZero(message = "Price must be >= 0") BigDecimal price,
        Integer durationMinutes,
        Long categoryId,
        String imageUrl,
        Boolean active
) {}
