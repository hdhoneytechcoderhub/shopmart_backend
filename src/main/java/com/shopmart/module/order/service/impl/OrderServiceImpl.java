package com.shopmart.module.order.service.impl;

import com.shopmart.common.dto.PageResponse;
import com.shopmart.common.exception.BadRequestException;
import com.shopmart.common.exception.ResourceNotFoundException;
import com.shopmart.module.audit.service.AuditService;
import com.shopmart.module.cart.entity.Cart;
import com.shopmart.module.cart.entity.CartItem;
import com.shopmart.module.cart.repository.CartRepository;
import com.shopmart.module.coupon.dto.CouponApplication;
import com.shopmart.module.coupon.service.CouponService;
import com.shopmart.module.order.dto.*;
import com.shopmart.module.order.entity.Order;
import com.shopmart.module.order.entity.OrderItem;
import com.shopmart.module.order.entity.OrderStatus;
import com.shopmart.module.order.entity.OrderStatusHistory;
import com.shopmart.module.order.entity.PaymentStatus;
import com.shopmart.module.order.mapper.OrderMapper;
import com.shopmart.module.order.repository.OrderRepository;
import com.shopmart.module.order.repository.OrderStatusHistoryRepository;
import com.shopmart.module.order.service.OrderService;
import com.shopmart.module.product.entity.Product;
import com.shopmart.module.product.repository.ProductRepository;
import com.shopmart.module.user.entity.User;
import com.shopmart.module.user.repository.UserRepository;
import com.shopmart.module.warehouse.service.InventoryAllocator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final com.shopmart.config.MetricsConfig.AppMetrics appMetrics;
    private final CouponService couponService;
    private final InventoryAllocator inventoryAllocator;
    private final OrderStatusHistoryRepository statusHistoryRepository;
    private final AuditService auditService;

    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("499");
    private static final BigDecimal FLAT_SHIPPING_FEE = new BigDecimal("49");
    // RAZORPAY is the gateway used by the storefront; COD is retained for existing
    // administrative flows but is not offered in the checkout UI.
    private static final Set<String> PAYMENT_METHODS = Set.of("RAZORPAY", "COD", "UPI", "CARD", "NETBANKING", "WALLET");
    private static final DateTimeFormatter ORDER_NO_FMT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC);

    @Override
    @Transactional
    public OrderResponse create(Long userId, CreateOrderRequest request, String idempotencyKey) {
        String key = normalizeKey(idempotencyKey);
        if (key != null) {
            // A repeated submit (double click / network retry after the order was saved)
            // returns the same pending order instead of creating a second one.
            var existing = orderRepository.findByUserIdAndClientRequestId(userId, key);
            if (existing.isPresent()) {
                log.info("[ORDER] idempotent replay for user={} key={} order={}", userId, key, existing.get().getId());
                return OrderMapper.toResponse(existing.get());
            }
        }

        String method = request.paymentMethod() == null ? "" : request.paymentMethod().toUpperCase();
        if (!PAYMENT_METHODS.contains(method)) {
            throw new BadRequestException("Unsupported payment method: " + request.paymentMethod());
        }

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Your cart is empty"));
        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Your cart is empty");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Order order = new Order();
        order.setUserId(userId);
        order.setOrderNumber(generateOrderNumber());
        order.setPaymentMethod(method);
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setCustomerEmail(user.getEmail());
        order.setClientRequestId(key);
        applyShippingAddress(order, request.shippingAddress(), user);

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem ci : cart.getItems()) {
            Product product = productRepository.findById(ci.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", ci.getProductId()));
            if (product.getStock() < ci.getQuantity()) {
                throw new BadRequestException("Insufficient stock for " + product.getName());
            }

            BigDecimal lineTotal = ci.getUnitPrice().multiply(BigDecimal.valueOf(ci.getQuantity()));
            OrderItem oi = new OrderItem();
            oi.setProductId(product.getId());
            oi.setProductName(product.getName());
            oi.setThumbnail(product.getImages().isEmpty() ? null : product.getImages().get(0).getUrl());
            oi.setQuantity(ci.getQuantity());
            oi.setUnitPrice(ci.getUnitPrice());
            oi.setLineTotal(lineTotal);
            oi.setVendorId(product.getVendorId());
            order.addItem(oi);

            subtotal = subtotal.add(lineTotal);

            // Reserve inventory while the order is pending payment. It is released again
            // if the order is cancelled before payment.
            product.setStock(product.getStock() - ci.getQuantity());
            productRepository.save(product);

            inventoryAllocator.allocate(product.getId(), ci.getQuantity());
        }

        BigDecimal shippingFee = subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0
                ? BigDecimal.ZERO : FLAT_SHIPPING_FEE;

        BigDecimal discount = BigDecimal.ZERO;
        CouponApplication couponApplication = null;
        String couponCode = request.couponCode();
        if (couponCode != null && !couponCode.isBlank()) {
            couponApplication = couponService.validate(couponCode, userId, subtotal);
            discount = couponApplication.discount();
        }

        BigDecimal total = subtotal.add(shippingFee).subtract(discount);
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO;
        }

        order.setSubtotal(subtotal);
        order.setShippingFee(shippingFee);
        order.setDiscount(discount);
        order.setTotal(total);

        Order saved = orderRepository.save(order);

        appMetrics.incrementOrderCreated();

        // IMPORTANT: the cart is intentionally NOT cleared here. It is cleared only after
        // Razorpay confirms the payment (PaymentServiceImpl.confirmPaid). That keeps the
        // cart intact when the customer cancels, fails, or closes the payment popup.

        if (couponApplication != null) {
            couponService.markRedeemed(couponApplication.couponId(), userId,
                    saved.getId(), couponApplication.discount());
        }

        // No "order placed successfully" email/notification here — the customer has not paid.
        // Customer + admin emails are sent by the payment confirmation path.

        return OrderMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderSummary> getOrders(Long userId, Pageable pageable) {
        Page<OrderSummary> page = orderRepository.findByUserId(userId, pageable).map(OrderMapper::toSummary);
        return PageResponse.from(page);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderDetails(Long userId, Long orderId) {
        return OrderMapper.toResponse(findOwned(userId, orderId));
    }

    @Override
    @Transactional
    public OrderResponse cancel(Long userId, Long orderId) {
        Order order = findOwned(userId, orderId);
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new BadRequestException("This order can no longer be cancelled");
        }
        releaseStock(order);
        order.setStatus(OrderStatus.CANCELLED);
        // A captured payment stays PAID until a real refund is processed — cancelling an
        // order must never make the payment record look refunded when no refund happened.
        return OrderMapper.toResponse(orderRepository.save(order));
    }

    @Override
    @Transactional(readOnly = true)
    public TrackingResponse track(Long userId, Long orderId) {
        Order order = findOwned(userId, orderId);
        return buildTracking(order);
    }

    @Override
    @Transactional
    public OrderResponse updateStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        boolean fulfillmentStep = status == OrderStatus.PROCESSING || status == OrderStatus.PACKED
                || status == OrderStatus.SHIPPED || status == OrderStatus.DELIVERED;
        boolean isCod = "COD".equalsIgnoreCase(order.getPaymentMethod());
        if (fulfillmentStep && order.getPaymentStatus() != PaymentStatus.PAID && !isCod) {
            // Admin fulfilment actions must never stand in for a captured payment.
            throw new BadRequestException("Payment has not been captured for this order");
        }
        if (status == OrderStatus.PENDING) {
            throw new BadRequestException("An order cannot be moved back to PENDING_PAYMENT");
        }

        if (status == OrderStatus.CANCELLED && order.getStatus() != OrderStatus.CANCELLED) {
            releaseStock(order);
        }

        order.setStatus(status);
        if (status == OrderStatus.DELIVERED && order.getPaymentStatus() == PaymentStatus.PENDING && isCod) {
            // COD is the only case where a fulfilment step legitimately collects the money.
            order.setPaymentStatus(PaymentStatus.PAID);
        }

        Order updated = orderRepository.save(order);
        OrderStatusHistory h = new OrderStatusHistory();
        h.setOrderId(updated.getId());
        h.setStatus(status);
        h.setNote("Updated by admin");
        statusHistoryRepository.save(h);
        auditService.log(null, "ORDER_STATUS_UPDATED", "Order", updated.getId(), status.name());
        return OrderMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public OrderResponse setTracking(Long orderId, String trackingNumber, String courierPartner) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        order.setTrackingNumber(trackingNumber);
        order.setCourierPartner(courierPartner);
        Order saved = orderRepository.save(order);
        auditService.log(null, "ORDER_TRACKING_SET", "Order", saved.getId(), courierPartner);
        return OrderMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<StatusHistoryResponse> statusHistory(Long userId, Long orderId) {
        findOwned(userId, orderId);
        return statusHistoryRepository.findByOrderIdOrderByCreatedAtAsc(orderId).stream()
                .map(h -> new StatusHistoryResponse(h.getStatus().name(), h.getNote(), h.getCreatedAt()))
                .toList();
    }

    // ---- helpers ----

    private String normalizeKey(String key) {
        if (key == null || key.isBlank()) return null;
        String trimmed = key.trim();
        return trimmed.length() > 100 ? trimmed.substring(0, 100) : trimmed;
    }

    private Order findOwned(Long userId, Long orderId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
    }

    private void releaseStock(Order order) {
        for (OrderItem item : order.getItems()) {
            productRepository.findById(item.getProductId()).ifPresent(p -> {
                p.setStock(p.getStock() + item.getQuantity());
                productRepository.save(p);
            });
        }
    }

    private String generateOrderNumber() {
        return "ORD-" + ORDER_NO_FMT.format(Instant.now()) + "-"
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private void applyShippingAddress(Order order, ShippingAddressDto a, User user) {
        order.setShipName(a.name());
        order.setShipPhone(a.phone());
        String email = a.email() != null && !a.email().isBlank() ? a.email() : user.getEmail();
        order.setShipEmail(email);
        order.setShipLine1(a.line1());
        order.setShipLine2(a.line2());
        order.setShipCity(a.city());
        order.setShipState(a.state());
        order.setShipPostalCode(a.postalCode());
        order.setShipCountry(a.country() != null && !a.country().isBlank() ? a.country() : "India");
    }

    private TrackingResponse buildTracking(Order order) {
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.RETURNED) {
            List<TrackingResponse.Step> steps = new ArrayList<>();
            steps.add(new TrackingResponse.Step("PENDING", "Order Placed", true, order.getCreatedAt()));
            steps.add(new TrackingResponse.Step(order.getStatus().name(),
                    order.getStatus() == OrderStatus.CANCELLED ? "Cancelled" : "Returned",
                    true, order.getUpdatedAt()));
            return new TrackingResponse(order.getOrderNumber(), order.getStatus().name(), steps);
        }

        OrderStatus[] path = {
                OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.PROCESSING,
                OrderStatus.SHIPPED, OrderStatus.DELIVERED
        };
        String[] labels = {"Order Placed", "Confirmed", "Processing", "Shipped", "Delivered"};
        int currentIdx = indexOf(path, order.getStatus());

        List<TrackingResponse.Step> steps = new ArrayList<>();
        for (int i = 0; i < path.length; i++) {
            boolean reached = i <= currentIdx;
            Instant at = i == 0 ? order.getCreatedAt()
                    : (i == currentIdx ? order.getUpdatedAt() : null);
            steps.add(new TrackingResponse.Step(path[i].name(), labels[i], reached, at));
        }
        return new TrackingResponse(order.getOrderNumber(), order.getStatus().name(), steps);
    }

    private int indexOf(OrderStatus[] path, OrderStatus status) {
        if (status == OrderStatus.PACKED) {
            for (int i = 0; i < path.length; i++) {
                if (path[i] == OrderStatus.PROCESSING) return i;
            }
        }
        for (int i = 0; i < path.length; i++) {
            if (path[i] == status) return i;
        }
        return 0;
    }
}
