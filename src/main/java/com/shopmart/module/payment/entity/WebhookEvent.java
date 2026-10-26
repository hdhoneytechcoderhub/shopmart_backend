package com.shopmart.module.payment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Replay-attack protection for gateway webhooks: each verified webhook's (provider, eventId)
 * pair is recorded here before processing. A duplicate delivery of the same event (gateways
 * commonly retry on a non-2xx or timeout) is detected via the unique constraint and skipped
 * rather than re-applied.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "webhook_events", uniqueConstraints = {
        @UniqueConstraint(name = "uk_webhook_provider_event", columnNames = {"provider", "event_id"})
})
public class WebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String provider;

    /** Gateway-supplied event id when present, else a SHA-256 hash of the raw payload. */
    @Column(name = "event_id", nullable = false, length = 128)
    private String eventId;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt = Instant.now();
}
