package com.shopmart.module.customer.entity;

import com.shopmart.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Internal admin-only note attached to a customer (support context, escalation flags, etc). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "customer_notes", indexes = @Index(name = "idx_customer_notes_customer", columnList = "customer_id"))
public class CustomerNote extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(nullable = false, length = 2000)
    private String note;
}
