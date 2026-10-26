package com.shopmart.module.product.entity;

import com.shopmart.common.entity.BaseEntity;
import com.shopmart.module.brand.entity.Brand;
import com.shopmart.module.category.entity.Category;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "products", uniqueConstraints = @UniqueConstraint(columnNames = "slug"),
        indexes = {@Index(name = "idx_product_status", columnList = "status")})
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String slug;

    @Column(length = 5000)
    private String description;

    private String sku;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(precision = 12, scale = 2)
    private BigDecimal salePrice;

    @Column(nullable = false)
    private int stock = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    /** Owning vendor (null for first-party / platform-owned products). */
    @Column(name = "vendor_id")
    private Long vendorId;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status = ProductStatus.DRAFT;

    @Column(nullable = false)
    private boolean featured = false;

    @Column(precision = 3, scale = 2)
    private BigDecimal ratingAverage = BigDecimal.ZERO;

    @Column(nullable = false)
    private int ratingCount = 0;

    private String metaTitle;
    private String metaDescription;

    /** SEO keyword list (comma-separated free text — matches metaTitle/metaDescription's
     *  plain-string convention rather than a normalized collection table). */
    @Column(length = 500)
    private String metaKeywords;

    /** Per-product override for the low-stock alert threshold. Null = use the platform-wide
     *  default (see AnalyticsServiceImpl.LOW_STOCK_THRESHOLD / the `threshold` query param on
     *  GET /admin/analytics/low-stock) — most products don't need a custom value. */
    @Column(name = "low_stock_threshold")
    private Integer lowStockThreshold;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<ProductImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductVariant> variants = new ArrayList<>();

    public void addImage(ProductImage image) {
        image.setProduct(this);
        this.images.add(image);
    }

    public void addVariant(ProductVariant variant) {
        variant.setProduct(this);
        this.variants.add(variant);
    }
}
