package com.shopmart.module.machine.dto;

import java.util.List;

public record MachineResponse(
        Long id, String name, String modelNumber, String brand,
        String description, List<String> images
) {}
