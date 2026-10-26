package com.shopmart.module.servicedesk.entity;

import com.shopmart.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter @Setter @NoArgsConstructor
@Entity
@Table(name = "service_bookings",
        indexes = {
            @Index(name = "idx_booking_user", columnList = "user_id"),
            @Index(name = "idx_booking_engineer", columnList = "engineer_id"),
            @Index(name = "idx_booking_status", columnList = "status")
        })
public class ServiceBooking extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "service_item_id", nullable = false)
    private Long serviceItemId;

    @Column(name = "engineer_id")
    private Long engineerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status = BookingStatus.REQUESTED;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(nullable = false)
    private String address;

    private String phone;

    @Column(length = 2000)
    private String notes;

    @Column(name = "completion_notes", length = 2000)
    private String completionNotes;

    /** Customer rating (1-5) after a completed service. */
    private Integer rating;

    @Column(name = "rating_comment", length = 1000)
    private String ratingComment;
}
