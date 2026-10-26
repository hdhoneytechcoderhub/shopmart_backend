package com.shopmart.module.push.dto;

import com.shopmart.module.push.entity.DevicePlatform;

import java.time.Instant;

public record DeviceTokenResponse(Long id, DevicePlatform platform, Instant lastSeenAt, Instant registeredAt) {}
