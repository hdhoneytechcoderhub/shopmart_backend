package com.shopmart.module.push.entity;

import com.shopmart.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Phase 3 requirement: "Push Notification Tracking" — one row per (user, attempted send). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "push_delivery_logs")
public class PushDeliveryLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeliveryStatus status;

    /** How many of this user's active device tokens the message was attempted on. */
    @Column(name = "attempted_tokens")
    private int attemptedTokens;

    @Column(name = "delivered_tokens")
    private int deliveredTokens;

    @Column(length = 500)
    private String failureReason;

    public enum DeliveryStatus { SENT, PARTIAL, FAILED, NO_DEVICES }
}
