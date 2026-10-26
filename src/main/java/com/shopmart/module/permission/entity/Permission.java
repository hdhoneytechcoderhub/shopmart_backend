package com.shopmart.module.permission.entity;

import com.shopmart.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A single fine-grained permission. Its {@code code} is used verbatim as a Spring Security
 * authority (e.g. checked via {@code @PreAuthorize("hasAuthority('PRODUCT_CREATE')")}), so
 * granting/revoking it takes effect immediately without touching any controller.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "permissions",
        uniqueConstraints = @UniqueConstraint(name = "uk_permission_code", columnNames = "code"))
public class Permission extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Machine code, e.g. "PRODUCT_CREATE", "ORDER_REFUND". Used as a security authority. */
    @Column(nullable = false, length = 100)
    private String code;

    @Column(nullable = false)
    private String name;

    private String description;

    /** Logical module/area, e.g. "product", "order", "report". */
    private String module;

    /**
     * Optional screen/menu key for screen-level (frontend route/menu) gating,
     * e.g. "dashboard", "products", "orders". Null for pure API-only permissions.
     */
    @Column(name = "screen_key")
    private String screenKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private PermissionGroup group;
}
