package com.shopmart.module.wallet.dto;

import java.math.BigDecimal;

public record WalletResponse(Long userId, BigDecimal balance) {}
