package com.shopmart.module.notification.entity;

import com.shopmart.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Reusable message template, keyed by a unique code (e.g. "ORDER_SHIPPED", "WELCOME_EMAIL").
 * One entity/CRUD covers "Notification Templates", "Email Templates", and "Push Templates" from
 * the spec — they're the same shape (subject + body bound to a channel), so this avoids three
 * near-identical CRUD modules that would drift out of sync with each other.
 * {subject}/{body} support simple {{placeholder}} substitution performed by the caller (e.g. the
 * campaign sender) — this entity just stores the template text.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "notification_templates", uniqueConstraints = {
        @UniqueConstraint(name = "uk_notification_template_code", columnNames = "code")
})
public class NotificationTemplate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannel channel;

    /** Used for EMAIL; ignored (may be null) for PUSH/IN_APP/SMS. */
    private String subject;

    @Column(nullable = false, length = 4000)
    private String body;

    @Column(nullable = false)
    private boolean active = true;

    private String description;
}
