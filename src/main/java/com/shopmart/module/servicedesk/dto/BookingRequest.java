package com.shopmart.module.servicedesk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record BookingRequest(
        @NotNull(message = "serviceItemId is required") Long serviceItemId,
        Instant scheduledAt,
        @NotBlank(message = "Address is required") String address,
        String phone,
        String notes
) {}
