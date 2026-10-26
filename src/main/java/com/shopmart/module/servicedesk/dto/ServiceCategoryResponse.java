package com.shopmart.module.servicedesk.dto;

public record ServiceCategoryResponse(
        Long id, String name, String description, String imageUrl, boolean active
) {}
