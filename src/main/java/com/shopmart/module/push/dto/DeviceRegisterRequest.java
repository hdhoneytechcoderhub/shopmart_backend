package com.shopmart.module.push.dto;

import com.shopmart.module.push.entity.DevicePlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DeviceRegisterRequest(@NotBlank String token, @NotNull DevicePlatform platform) {}
