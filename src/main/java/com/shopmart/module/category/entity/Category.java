package com.shopmart.module.category.entity;

import com.shopmart.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "categories", uniqueConstraints = @UniqueConstraint(columnNames = "slug"))
public class Category extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String slug;

    @Column(length = 1000)
    private String description;

    private String bannerUrl;
    private String metaTitle;
    private String metaDescription;

    /** Self-referencing hierarchy, stored as a plain FK id (not a managed @ManyToOne) —
     *  consistent with how Product.vendorId is modeled elsewhere in this codebase, and avoids
     *  lazy-loading/circular-serialization complexity for what's normally a shallow (1-2 level)
     *  category tree. Null = top-level category. */
    @Column(name = "parent_id")
    private Long parentId;

    /** Display ordering within the same parent (siblings). Lower sorts first. */
    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(nullable = false)
    private boolean active = true;
}
