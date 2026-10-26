package com.shopmart.module.wallet.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record WalletTxnResponse(Long id, String type, BigDecimal amount,
                                BigDecimal balanceAfter, String reason, Instant createdAt) {}
