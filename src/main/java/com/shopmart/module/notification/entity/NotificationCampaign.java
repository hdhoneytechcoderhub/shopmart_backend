package com.shopmart.module.notification.entity;

import com.shopmart.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "notification_campaigns")
public class NotificationCampaign extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannel channel = NotificationChannel.IN_APP;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private CampaignTargetType targetType;

    /** Set when targetType = ROLE (e.g. ROLE_CUSTOMER). */
    private String targetRole;

    /** Set when targetType = SEGMENT (e.g. VIP, INACTIVE) — see CustomerSegment. */
    private String targetSegment;

    /** Optional: pulls subject/body from a NotificationTemplate at send time. */
    @Column(name = "template_id")
    private Long templateId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 4000)
    private String message;

    private String link;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CampaignStatus status = CampaignStatus.DRAFT;

    /** Null = send immediately when triggered; set = picked up by the scheduler at this time. */
    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "recipient_count")
    private int recipientCount;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;
}
