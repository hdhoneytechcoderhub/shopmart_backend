package com.shopmart.module.referral.service;

import com.shopmart.module.referral.dto.ReferralCodeResponse;
import com.shopmart.module.referral.dto.ReferralResponse;

import java.util.List;

public interface ReferralService {
    ReferralCodeResponse myCode(Long userId);
    ReferralResponse apply(Long newUserId, String code);
    List<ReferralResponse> myReferrals(Long userId);
}
