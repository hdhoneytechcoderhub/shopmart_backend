package com.shopmart.module.referral.repository;

import com.shopmart.module.referral.entity.ReferralCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReferralCodeRepository extends JpaRepository<ReferralCode, Long> {
    Optional<ReferralCode> findByUserId(Long userId);
    Optional<ReferralCode> findByCode(String code);
    boolean existsByCode(String code);
}
