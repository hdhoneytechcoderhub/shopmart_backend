package com.shopmart.module.wallet.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record WalletOpRequest(
        @NotNull @Positive BigDecimal amount,
        String reason
) {}
