package com.shopmart.module.referral.service.impl;

import com.shopmart.common.exception.BadRequestException;
import com.shopmart.module.referral.dto.ReferralCodeResponse;
import com.shopmart.module.referral.dto.ReferralResponse;
import com.shopmart.module.referral.entity.Referral;
import com.shopmart.module.referral.entity.ReferralCode;
import com.shopmart.module.referral.repository.ReferralCodeRepository;
import com.shopmart.module.referral.repository.ReferralRepository;
import com.shopmart.module.referral.service.ReferralService;
import com.shopmart.module.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReferralServiceImpl implements ReferralService {

    private final ReferralCodeRepository codeRepository;
    private final ReferralRepository referralRepository;
    private final WalletService walletService;

    @Value("${app.referral.reward:50}")
    private BigDecimal rewardAmount;

    @Override @Transactional
    public ReferralCodeResponse myCode(Long userId) {
        ReferralCode rc = codeRepository.findByUserId(userId).orElseGet(() -> {
            ReferralCode c = new ReferralCode();
            c.setUserId(userId);
            c.setCode(generateUniqueCode());
            return codeRepository.save(c);
        });
        return new ReferralCodeResponse(rc.getCode());
    }

    @Override @Transactional
    public ReferralResponse apply(Long newUserId, String code) {
        if (referralRepository.existsByReferredUserId(newUserId))
            throw new BadRequestException("Referral already applied for this account");
        ReferralCode rc = codeRepository.findByCode(code.trim().toUpperCase())
                .orElseThrow(() -> new BadRequestException("Invalid referral code"));
        if (rc.getUserId().equals(newUserId))
            throw new BadRequestException("You cannot use your own referral code");

        Referral r = new Referral();
        r.setReferrerId(rc.getUserId());
        r.setReferredUserId(newUserId);
        r.setRewardAmount(rewardAmount);
        r = referralRepository.save(r);

        // Reward the referrer's wallet.
        walletService.credit(rc.getUserId(), rewardAmount, "Referral reward");

        return new ReferralResponse(r.getId(), r.getReferredUserId(), r.getRewardAmount(), r.getCreatedAt());
    }

    @Override @Transactional(readOnly = true)
    public List<ReferralResponse> myReferrals(Long userId) {
        return referralRepository.findByReferrerIdOrderByCreatedAtDesc(userId).stream()
                .map(r -> new ReferralResponse(r.getId(), r.getReferredUserId(), r.getRewardAmount(), r.getCreatedAt()))
                .toList();
    }

    private String generateUniqueCode() {
        String code;
        do {
            code = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        } while (codeRepository.existsByCode(code));
        return code;
    }
}
