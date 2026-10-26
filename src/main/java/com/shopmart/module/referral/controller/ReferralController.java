package com.shopmart.module.referral.controller;

import com.shopmart.common.dto.ApiResponse;
import com.shopmart.module.referral.dto.ApplyReferralRequest;
import com.shopmart.module.referral.dto.ReferralCodeResponse;
import com.shopmart.module.referral.dto.ReferralResponse;
import com.shopmart.module.referral.service.ReferralService;
import com.shopmart.security.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/referrals")
@RequiredArgsConstructor
@Tag(name = "Referrals")
public class ReferralController {

    private final ReferralService service;

    @GetMapping("/my-code")
    public ApiResponse<ReferralCodeResponse> myCode() {
        return ApiResponse.ok(service.myCode(SecurityUtils.currentUserId()));
    }

    @PostMapping("/apply")
    public ApiResponse<ReferralResponse> apply(@Valid @RequestBody ApplyReferralRequest req) {
        return ApiResponse.ok("Referral applied", service.apply(SecurityUtils.currentUserId(), req.code()));
    }

    @GetMapping("/my")
    public ApiResponse<List<ReferralResponse>> myReferrals() {
        return ApiResponse.ok(service.myReferrals(SecurityUtils.currentUserId()));
    }
}
