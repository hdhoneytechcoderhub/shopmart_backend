package com.shopmart.module.servicedesk.entity;

import com.shopmart.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor
@Entity
@Table(name = "service_items", indexes = @Index(name = "idx_service_item_cat", columnList = "category_id"))
public class ServiceItem extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    @Column(length = 2000)
    private String description;
    @Column(nullable = false)
    private BigDecimal price = BigDecimal.ZERO;
    @Column(name = "duration_minutes")
    private Integer durationMinutes;
    @Column(name = "category_id")
    private Long categoryId;
    private String imageUrl;
    @Column(nullable = false)
    private boolean active = true;
}
