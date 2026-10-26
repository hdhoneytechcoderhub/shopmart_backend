package com.shopmart.module.machine.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record MachineRequest(
        @NotBlank(message = "Name is required") String name,
        String modelNumber,
        String brand,
        String description,
        List<String> images
) {}
