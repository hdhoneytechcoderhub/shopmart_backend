package com.shopmart.module.payment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter @Setter @NoArgsConstructor
@Entity
@Table(name = "payment_transactions",
        indexes = {
            @Index(name = "idx_txn_order", columnList = "order_id"),
            @Index(name = "idx_txn_payment", columnList = "payment_id")
        })
public class PaymentTransaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "gateway_order_id")
    private String gatewayOrderId;

    @Column(name = "transaction_id")
    private String transactionId;

    private BigDecimal amount;

    /** CREATED, CAPTURED, FAILED, CANCELLED, TIMEOUT, REFUNDED */
    @Column(nullable = false, length = 20)
    private String status;

    private String provider;

    @Column(length = 500)
    private String note;

    /** Client/admin-supplied key for refund idempotency; null for non-refund transaction rows. */
    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
