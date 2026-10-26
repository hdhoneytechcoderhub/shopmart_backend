package com.shopmart.module.permission.entity;

import com.shopmart.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A named bucket of related permissions (e.g. "Product Management", "Order Management"),
 * used to organise the permission matrix in the admin UI.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "permission_groups",
        uniqueConstraints = @UniqueConstraint(name = "uk_permission_group_name", columnNames = "name"))
public class PermissionGroup extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;
}
