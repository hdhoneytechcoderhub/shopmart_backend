package com.shopmart.module.servicedesk.dto;

import jakarta.validation.constraints.NotBlank;

public record BookingStatusUpdateRequest(
        @NotBlank(message = "status is required") String status
) {}
