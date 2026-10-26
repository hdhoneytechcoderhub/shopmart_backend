package com.shopmart.module.referral.dto;

import jakarta.validation.constraints.NotBlank;

public record ApplyReferralRequest(@NotBlank(message = "code is required") String code) {}
