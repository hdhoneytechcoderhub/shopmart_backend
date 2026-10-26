package com.shopmart.module.auth.entity;

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
@Table(name = "refresh_tokens", indexes = @Index(name = "idx_refresh_token", columnList = "token"))
public class RefreshToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 512)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked = false;

    /** User-Agent captured at issue time, e.g. "Chrome 126 on Windows". */
    @Column(name = "device_info", length = 255)
    private String deviceInfo;

    /** Client IP captured at issue time (or last refresh). */
    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    /** Updated every time this session is used to refresh an access token. */
    @Column(name = "last_used_at")
    private Instant lastUsedAt;
}
