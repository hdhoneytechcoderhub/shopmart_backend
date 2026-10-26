package com.shopmart.module.wallet.service;

import com.shopmart.module.wallet.dto.WalletResponse;
import com.shopmart.module.wallet.dto.WalletTxnResponse;

import java.math.BigDecimal;
import java.util.List;

public interface WalletService {
    WalletResponse balance(Long userId);
    List<WalletTxnResponse> history(Long userId);
    WalletResponse credit(Long userId, BigDecimal amount, String reason);
    WalletResponse debit(Long userId, BigDecimal amount, String reason);
}
