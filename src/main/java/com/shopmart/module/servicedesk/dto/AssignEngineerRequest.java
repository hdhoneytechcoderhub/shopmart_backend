package com.shopmart.module.servicedesk.dto;

import jakarta.validation.constraints.NotNull;

public record AssignEngineerRequest(
        @NotNull(message = "engineerId is required") Long engineerId
) {}
