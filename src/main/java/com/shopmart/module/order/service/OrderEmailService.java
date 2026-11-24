package com.shopmart.module.order.service;

import com.shopmart.common.notification.EmailSender;
import com.shopmart.module.order.entity.Order;
import com.shopmart.module.order.entity.OrderItem;
import com.shopmart.module.order.repository.OrderRepository;
import com.shopmart.module.user.entity.User;
import com.shopmart.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Builds and sends the two post-payment emails:
 *   1. customer order confirmation
 *   2. admin "new paid order" notification
 *
 * Runs in its own transaction AFTER the payment transaction has committed, so an email
 * failure can never roll back a confirmed order. Send timestamps make repeated webhook
 * deliveries / retries idempotent.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEmailService {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH)
                    .withZone(ZoneId.of("Asia/Kolkata"));

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final EmailSender emailSender;

    @Value("${app.orders.admin-email:admin@shopmart.local}")
    private String adminEmail;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendForPaidOrder(Long orderId) {
        Order order = orderRepository.findWithItemsById(orderId).orElse(null);
        if (order == null) {
            log.warn("[ORDER_EMAIL] order {} not found", orderId);
            return;
        }
        if (!"PAID".equals(order.getPaymentStatus().name())) {
            log.info("[ORDER_EMAIL] order {} is not PAID; skipping emails", orderId);
            return;
        }

        User user = userRepository.findById(order.getUserId()).orElse(null);
        String customerEmail = firstNonBlank(order.getCustomerEmail(), order.getShipEmail(),
                user != null ? user.getEmail() : null);
        String customerName = firstNonBlank(order.getShipName(), user != null ? user.getName() : null, "Customer");

        boolean changed = false;
        if (order.getConfirmationEmailSentAt() == null && customerEmail != null) {
            try {
                emailSender.send(customerEmail,
                        "ShopMart – Order Confirmation #" + order.getOrderNumber(),
                        customerBody(order, customerName));
                order.setConfirmationEmailSentAt(Instant.now());
                changed = true;
                log.info("[ORDER_EMAIL] customer confirmation sent for order {}", order.getOrderNumber());
            } catch (Exception e) {
                log.error("[ORDER_EMAIL] customer confirmation failed for order {}: {}",
                        order.getOrderNumber(), e.getMessage());
            }
        }

        if (order.getAdminEmailSentAt() == null && adminEmail != null && !adminEmail.isBlank()) {
            try {
                emailSender.send(adminEmail,
                        "ShopMart – New Paid Order Received #" + order.getOrderNumber(),
                        adminBody(order, customerName, customerEmail, firstNonBlank(order.getShipPhone(),
                                user != null ? user.getPhone() : null, "—")));
                order.setAdminEmailSentAt(Instant.now());
                changed = true;
                log.info("[ORDER_EMAIL] admin notification sent for order {}", order.getOrderNumber());
            } catch (Exception e) {
                log.error("[ORDER_EMAIL] admin notification failed for order {}: {}",
                        order.getOrderNumber(), e.getMessage());
            }
        }

        if (changed) orderRepository.save(order);
    }

    private String customerBody(Order o, String customerName) {
        StringBuilder b = new StringBuilder();
        b.append("Dear ").append(customerName).append(",\n\n");
        b.append("Thank you for shopping with ShopMart!\n\n");
        b.append("Your payment has been received successfully, and your order has been confirmed.\n\n");
        b.append("Order Details\n");
        b.append("-------------\n");
        b.append("Order ID: ").append(o.getOrderNumber()).append('\n');
        b.append("Order Date: ").append(fmt(o.getPaidAt() != null ? o.getPaidAt() : o.getCreatedAt())).append('\n');
        b.append("Product Details:\n").append(productLines(o));
        b.append("Total Amount Paid: ").append(money(o.getTotal())).append('\n');
        b.append("Payment Method: Razorpay\n");
        b.append("Payment Status: Successful\n");
        b.append("Order Status: ").append(statusLabel(o.getStatus().name())).append('\n');
        b.append("Delivery Address:\n").append(addressLines(o)).append('\n');
        b.append("Thank you for choosing ShopMart.\n\n");
        b.append("Best regards,\nTeam ShopMart\n");
        return b.toString();
    }

    private String adminBody(Order o, String customerName, String customerEmail, String customerPhone) {
        StringBuilder b = new StringBuilder();
        b.append("Hello Admin,\n\n");
        b.append("A new order has been successfully paid and confirmed on ShopMart.\n\n");
        b.append("Order Information\n");
        b.append("-----------------\n");
        b.append("Order ID: ").append(o.getOrderNumber()).append('\n');
        b.append("Customer Name: ").append(customerName).append('\n');
        b.append("Customer Email: ").append(customerEmail != null ? customerEmail : "—").append('\n');
        b.append("Customer Mobile: ").append(customerPhone).append('\n');
        b.append("Products:\n").append(productLines(o));
        b.append("Quantity: ").append(o.getItems().stream().mapToInt(OrderItem::getQuantity).sum()).append('\n');
        b.append("Total Payment Received: ").append(money(o.getTotal())).append('\n');
        b.append("Payment Method: Razorpay\n");
        b.append("Razorpay Payment ID: ").append(o.getRazorpayPaymentId() != null ? o.getRazorpayPaymentId() : "—").append('\n');
        b.append("Payment Status: Successful\n");
        b.append("Delivery Address:\n").append(addressLines(o)).append('\n');
        b.append("Order Date: ").append(fmt(o.getPaidAt() != null ? o.getPaidAt() : o.getCreatedAt())).append("\n\n");
        b.append("Please log in to the ShopMart Admin Dashboard to process this order.\n");
        return b.toString();
    }

    private String productLines(Order o) {
        StringBuilder b = new StringBuilder();
        for (OrderItem i : o.getItems()) {
            b.append("- ").append(i.getProductName())
                    .append(" | Qty: ").append(i.getQuantity())
                    .append(" | Price: ").append(money(i.getUnitPrice()))
                    .append(" | Line total: ").append(money(i.getLineTotal()))
                    .append('\n');
        }
        b.append("Shipping Charges: ").append(money(o.getShippingFee())).append('\n');
        if (o.getDiscount() != null && o.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            b.append("Discount: -").append(money(o.getDiscount())).append('\n');
        }
        return b.toString();
    }

    private String addressLines(Order o) {
        StringBuilder b = new StringBuilder();
        b.append(o.getShipName() != null ? o.getShipName() : "—").append('\n');
        if (o.getShipLine1() != null) b.append(o.getShipLine1()).append('\n');
        if (o.getShipLine2() != null && !o.getShipLine2().isBlank()) b.append(o.getShipLine2()).append('\n');
        b.append(String.join(", ",
                nz(o.getShipCity()), nz(o.getShipState()), nz(o.getShipPostalCode()))).append('\n');
        b.append(nz(o.getShipCountry())).append('\n');
        if (o.getShipPhone() != null) b.append("Mobile: ").append(o.getShipPhone()).append('\n');
        if (o.getShipEmail() != null) b.append("Email: ").append(o.getShipEmail()).append('\n');
        return b.toString();
    }

    private String fmt(Instant instant) {
        return instant == null ? "—" : DATE_FMT.format(instant);
    }

    private String money(BigDecimal amount) {
        return "₹" + (amount == null ? "0.00" : amount.setScale(2).toPlainString());
    }

    private String statusLabel(String status) {
        if (status == null) return "Confirmed";
        return switch (status) {
            case "PENDING" -> "Pending payment";
            case "CONFIRMED" -> "Confirmed";
            case "PROCESSING", "PACKED" -> "Processing";
            case "SHIPPED" -> "Shipped";
            case "DELIVERED" -> "Delivered";
            case "CANCELLED" -> "Cancelled";
            default -> status;
        };
    }

    private String nz(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }

    private String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }
}
