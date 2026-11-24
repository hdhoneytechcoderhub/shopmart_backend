package com.shopmart.module.order.entity;

import com.shopmart.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "orders", uniqueConstraints = @UniqueConstraint(columnNames = "order_number"))
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false)
    private String orderNumber;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "shipping_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    // Shipping address snapshot
    private String shipName;
    private String shipPhone;
    @Column(name = "ship_email")
    private String shipEmail;
    private String shipLine1;
    private String shipLine2;
    private String shipCity;
    private String shipState;
    private String shipPostalCode;
    private String shipCountry;

    /** Customer account email captured when the order was created (email snapshot). */
    @Column(name = "customer_email")
    private String customerEmail;

    private String trackingNumber;

    private String courierPartner;

    // ---- Razorpay payment references (populated only after backend verification) ----
    @Column(name = "razorpay_order_id")
    private String razorpayOrderId;

    @Column(name = "razorpay_payment_id")
    private String razorpayPaymentId;

    @Column(name = "paid_at")
    private Instant paidAt;

    /** Set once the customer confirmation email has been accepted by the mail provider. */
    @Column(name = "confirmation_email_sent_at")
    private Instant confirmationEmailSentAt;

    /** Set once the admin "new paid order" email has been accepted by the mail provider. */
    @Column(name = "admin_email_sent_at")
    private Instant adminEmailSentAt;

    /** Client-supplied idempotency key so repeated create-order clicks reuse the same order. */
    @Column(name = "client_request_id", length = 100)
    private String clientRequestId;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    public void addItem(OrderItem item) {
        item.setOrder(this);
        this.items.add(item);
    }
}
