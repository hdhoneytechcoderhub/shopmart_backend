package com.shopmart.module.wallet.service.impl;

import com.shopmart.common.exception.BadRequestException;
import com.shopmart.module.wallet.dto.WalletResponse;
import com.shopmart.module.wallet.dto.WalletTxnResponse;
import com.shopmart.module.wallet.entity.Wallet;
import com.shopmart.module.wallet.entity.WalletTransaction;
import com.shopmart.module.wallet.repository.WalletRepository;
import com.shopmart.module.wallet.repository.WalletTransactionRepository;
import com.shopmart.module.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository txnRepository;

    @Override @Transactional
    public WalletResponse balance(Long userId) {
        Wallet w = getOrCreate(userId);
        return new WalletResponse(userId, w.getBalance());
    }

    @Override @Transactional(readOnly = true)
    public List<WalletTxnResponse> history(Long userId) {
        Wallet w = walletRepository.findByUserId(userId).orElse(null);
        if (w == null) return List.of();
        return txnRepository.findByWalletIdOrderByCreatedAtDesc(w.getId()).stream()
                .map(t -> new WalletTxnResponse(t.getId(), t.getType(), t.getAmount(),
                        t.getBalanceAfter(), t.getReason(), t.getCreatedAt()))
                .toList();
    }

    @Override @Transactional
    public WalletResponse credit(Long userId, BigDecimal amount, String reason) {
        Wallet w = getOrCreate(userId);
        w.setBalance(w.getBalance().add(amount));
        walletRepository.save(w);
        record(w, "CREDIT", amount, reason);
        return new WalletResponse(userId, w.getBalance());
    }

    @Override @Transactional
    public WalletResponse debit(Long userId, BigDecimal amount, String reason) {
        Wallet w = getOrCreate(userId);
        if (w.getBalance().compareTo(amount) < 0)
            throw new BadRequestException("Insufficient wallet balance");
        w.setBalance(w.getBalance().subtract(amount));
        walletRepository.save(w);
        record(w, "DEBIT", amount, reason);
        return new WalletResponse(userId, w.getBalance());
    }

    private Wallet getOrCreate(Long userId) {
        return walletRepository.findByUserId(userId).orElseGet(() -> {
            Wallet w = new Wallet();
            w.setUserId(userId);
            w.setBalance(BigDecimal.ZERO);
            return walletRepository.save(w);
        });
    }

    private void record(Wallet w, String type, BigDecimal amount, String reason) {
        WalletTransaction t = new WalletTransaction();
        t.setWalletId(w.getId());
        t.setType(type);
        t.setAmount(amount);
        t.setBalanceAfter(w.getBalance());
        t.setReason(reason);
        txnRepository.save(t);
    }
}
