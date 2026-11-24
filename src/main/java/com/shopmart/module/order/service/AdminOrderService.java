package com.shopmart.module.order.service;

import com.shopmart.common.dto.PageResponse;
import com.shopmart.module.order.dto.AdminOrderDetail;
import com.shopmart.module.order.dto.AdminOrderSummary;
import com.shopmart.module.order.entity.OrderStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AdminOrderService {
    PageResponse<AdminOrderSummary> search(OrderStatus status, String query, Pageable pageable);
    List<AdminOrderSummary> recent(int limit);
    AdminOrderDetail detail(Long orderId);
}
