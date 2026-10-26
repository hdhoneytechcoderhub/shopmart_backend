package com.shopmart.module.servicedesk.dto;

import jakarta.validation.constraints.NotBlank;

public record ServiceCategoryRequest(
        @NotBlank(message = "Name is required") String name,
        String description,
        String imageUrl,
        Boolean active
) {}
