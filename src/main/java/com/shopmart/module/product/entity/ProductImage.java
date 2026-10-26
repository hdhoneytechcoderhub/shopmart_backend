package com.shopmart.module.product.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "product_images")
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private String url;

    /** Set only for images uploaded via FileStorageService (Phase 5 integration) — null for
     *  images whose URL was pasted in directly (e.g. an external CDN link). Needed because
     *  deleting a stored file requires the provider-specific key, not the public URL. */
    private String storageKey;

    private String alt;

    @Column(nullable = false)
    private int position = 0;
}
