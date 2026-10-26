package com.shopmart.module.referral.repository;

import com.shopmart.module.referral.entity.Referral;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReferralRepository extends JpaRepository<Referral, Long> {
    List<Referral> findByReferrerIdOrderByCreatedAtDesc(Long referrerId);
    boolean existsByReferredUserId(Long referredUserId);
}
