package com.shopmart.module.payment.entity;

import com.shopmart.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "payments")
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private String method;          // RAZORPAY, UPI, CARD, NETBANKING, WALLET, COD

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentState status = PaymentState.CREATED;

    private String provider;        // razorpay, stripe, none (COD)

    @Column(name = "gateway_ref")
    private String gatewayRef;      // Razorpay order id

    @Column(name = "transaction_id")
    private String transactionId;   // Razorpay payment id on success

    private String failureReason;

    /** Cumulative amount refunded so far (supports partial refunds). Zero until any refund is issued. */
    @Column(name = "refunded_amount", precision = 12, scale = 2)
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    public enum PaymentState {
        CREATED, PENDING, SUCCESS, FAILED, CANCELLED, REFUNDED, PARTIALLY_REFUNDED
    }
}
