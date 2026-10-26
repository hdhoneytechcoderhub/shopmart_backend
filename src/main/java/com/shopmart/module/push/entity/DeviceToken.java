//package com.shopmart.module.push.entity;
//
//import com.shopmart.common.entity.BaseEntity;
//import jakarta.persistence.*;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.Setter;
//
//import java.time.Instant;
//
//@Getter
//@Setter
//@NoArgsConstructor
//@Entity
//@Table(name = "push_device_tokens", uniqueConstraints = {
//        @UniqueConstraint(name = "uk_push_device_tokens_token", columnNames = "token")
//})
//public class DeviceToken extends BaseEntity {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(name = "user_id", nullable = false)
//    private Long userId;
//
//    /** FCM registration token. Unique across all users — re-registering the same device token
//     *  under a different account (e.g. logout/login) updates ownership rather than duplicating. */
//    @Column(nullable = false, length = 500)
//    private String token;
//
//    @Enumerated(EnumType.STRING)
//    @Column(nullable = false, length = 10)
//    private DevicePlatform platform;
//
//    @Column(name = "last_seen_at", nullable = false)
//    private Instant lastSeenAt = Instant.now();
//
//    @Column(nullable = false)
//    private boolean active = true;
//}
package com.shopmart.module.push.entity;

import com.shopmart.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity(name = "PushDeviceToken")
@Table(
        name = "push_device_tokens",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_push_device_tokens_token",
                        columnNames = "token"
                )
        }
)
public class DeviceToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * FCM registration token.
     * Unique across all users.
     */
    @Column(nullable = false, length = 500)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DevicePlatform platform;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt = Instant.now();

    @Column(nullable = false)
    private boolean active = true;
}