package com.shopmart.module.referral.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ReferralResponse(Long id, Long referredUserId, BigDecimal rewardAmount, Instant createdAt) {}
