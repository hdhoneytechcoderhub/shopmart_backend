package com.shopmart.module.referral.entity;

import com.shopmart.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor
@Entity
@Table(name = "referrals",
        uniqueConstraints = @UniqueConstraint(columnNames = "referred_user_id"),
        indexes = @Index(name = "idx_referral_referrer", columnList = "referrer_id"))
public class Referral extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "referrer_id", nullable = false)
    private Long referrerId;

    @Column(name = "referred_user_id", nullable = false, unique = true)
    private Long referredUserId;

    @Column(name = "reward_amount")
    private BigDecimal rewardAmount = BigDecimal.ZERO;
}
