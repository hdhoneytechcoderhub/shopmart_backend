package com.shopmart.module.wallet.controller;

import com.shopmart.common.dto.ApiResponse;
import com.shopmart.module.wallet.dto.WalletOpRequest;
import com.shopmart.module.wallet.dto.WalletResponse;
import com.shopmart.module.wallet.dto.WalletTxnResponse;
import com.shopmart.module.wallet.service.WalletService;
import com.shopmart.security.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/wallet")
@RequiredArgsConstructor
@Tag(name = "Wallet")
public class WalletController {

    private final WalletService service;

    @GetMapping
    public ApiResponse<WalletResponse> myWallet() {
        return ApiResponse.ok(service.balance(SecurityUtils.currentUserId()));
    }

    @GetMapping("/transactions")
    public ApiResponse<List<WalletTxnResponse>> myTransactions() {
        return ApiResponse.ok(service.history(SecurityUtils.currentUserId()));
    }

    // Admin can credit/debit any user's wallet (refunds, rewards, adjustments)
    @PostMapping("/{userId}/credit")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<WalletResponse> credit(@PathVariable Long userId, @Valid @RequestBody WalletOpRequest req) {
        return ApiResponse.ok("Wallet credited", service.credit(userId, req.amount(), req.reason()));
    }

    @PostMapping("/{userId}/debit")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<WalletResponse> debit(@PathVariable Long userId, @Valid @RequestBody WalletOpRequest req) {
        return ApiResponse.ok("Wallet debited", service.debit(userId, req.amount(), req.reason()));
    }
}
