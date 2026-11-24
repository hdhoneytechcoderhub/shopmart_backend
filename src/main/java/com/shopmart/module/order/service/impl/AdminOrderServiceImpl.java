package com.shopmart.module.order.service.impl;

import com.shopmart.common.dto.PageResponse;
import com.shopmart.common.exception.ResourceNotFoundException;
import com.shopmart.module.order.dto.*;
import com.shopmart.module.order.entity.Order;
import com.shopmart.module.order.entity.OrderStatus;
import com.shopmart.module.order.mapper.OrderMapper;
import com.shopmart.module.order.repository.OrderRepository;
import com.shopmart.module.order.repository.OrderStatusHistoryRepository;
import com.shopmart.module.order.service.AdminOrderService;
import com.shopmart.module.user.entity.User;
import com.shopmart.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminOrderServiceImpl implements AdminOrderService {

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminOrderSummary> search(OrderStatus status, String query, Pageable pageable) {
        String q = (query == null || query.isBlank())
                ? null
                : "%" + query.trim().toLowerCase() + "%";
        Page<Order> page = orderRepository.adminSearch(status, q, pageable);
        return PageResponse.from(page.map(this::toSummary));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminOrderSummary> recent(int limit) {
        int n = Math.max(1, Math.min(limit, 50));
        Page<Order> page = orderRepository.adminSearch(null, null,
                PageRequest.of(0, n, Sort.by(Sort.Direction.DESC, "createdAt")));
        return page.getContent().stream().map(this::toSummary).toList();
    }

    /**
     * Maps an order to the admin table row. Customer name/email/phone come from the account when
     * it still exists, falling back to the shipping snapshot captured at checkout.
     */
    private AdminOrderSummary toSummary(Order o) {
        User u = userRepository.findById(o.getUserId()).orElse(null);
        int count = o.getItems().stream().mapToInt(i -> i.getQuantity()).sum();
        return new AdminOrderSummary(
                o.getId(),
                o.getOrderNumber(),
                u != null ? u.getName() : o.getShipName(),
                o.getCustomerEmail() != null ? o.getCustomerEmail() : (u != null ? u.getEmail() : null),
                u != null && u.getPhone() != null ? u.getPhone() : o.getShipPhone(),
                o.getStatus().name(),
                o.getPaymentStatus().name(),
                o.getPaymentMethod(),
                o.getTotal(),
                count,
                o.getRazorpayOrderId(),
                o.getRazorpayPaymentId(),
                o.getCreatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AdminOrderDetail detail(Long orderId) {
        Order o = orderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        User u = userRepository.findById(o.getUserId()).orElse(null);
        ShippingAddressDto addr = new ShippingAddressDto(o.getShipName(), o.getShipPhone(),
                o.getShipEmail(), o.getShipLine1(), o.getShipLine2(), o.getShipCity(), o.getShipState(),
                o.getShipPostalCode(), o.getShipCountry());
        List<StatusHistoryResponse> history = statusHistoryRepository
                .findByOrderIdOrderByCreatedAtAsc(orderId).stream()
                .map(h -> new StatusHistoryResponse(h.getStatus().name(), h.getNote(), h.getCreatedAt()))
                .toList();
        return new AdminOrderDetail(
                o.getId(), o.getOrderNumber(), o.getStatus().name(), o.getPaymentStatus().name(),
                o.getPaymentMethod(), o.getSubtotal(), o.getShippingFee(), o.getDiscount(), o.getTotal(),
                u != null ? u.getName() : o.getShipName(),
                o.getCustomerEmail() != null ? o.getCustomerEmail() : (u != null ? u.getEmail() : null),
                u != null && u.getPhone() != null ? u.getPhone() : o.getShipPhone(),
                addr,
                o.getItems().stream().map(OrderMapper::toItem).toList(),
                o.getRazorpayOrderId(), o.getRazorpayPaymentId(), o.getPaidAt(), o.getCreatedAt(),
                history
        );
    }
}
