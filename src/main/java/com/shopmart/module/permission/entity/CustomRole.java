package com.shopmart.module.permission.entity;

import com.shopmart.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * A dynamic, admin-defined role (distinct from the built-in {@code Role} enum). Custom roles
 * carry a set of fine-grained {@link Permission}s and can be assigned to any number of users,
 * additively on top of their base ROLE_* — so this layers on top of the existing coarse-grained
 * security model rather than replacing it.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "custom_roles",
        uniqueConstraints = @UniqueConstraint(name = "uk_custom_role_name", columnNames = "name"))
public class CustomRole extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private boolean active = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "custom_role_permissions",
            joinColumns = @JoinColumn(name = "custom_role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id"))
    private Set<Permission> permissions = new HashSet<>();
}
