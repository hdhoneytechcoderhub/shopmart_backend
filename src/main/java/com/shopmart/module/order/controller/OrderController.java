package com.shopmart.module.order.controller;

import com.shopmart.common.dto.ApiResponse;
import com.shopmart.module.order.dto.TrackingUpdateRequest;
import com.shopmart.module.order.dto.StatusHistoryResponse;
import com.shopmart.common.dto.PageResponse;
import com.shopmart.module.order.dto.CreateOrderRequest;
import com.shopmart.module.order.dto.OrderResponse;
import com.shopmart.module.order.dto.OrderSummary;
import com.shopmart.module.order.dto.TrackingResponse;
import com.shopmart.module.order.entity.OrderStatus;
import com.shopmart.module.order.service.OrderService;
import com.shopmart.security.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "Orders")
public class OrderController {

    private final OrderService service;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> create(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse order = service.create(SecurityUtils.currentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Order placed", order));
    }

    @GetMapping
    public ApiResponse<PageResponse<OrderSummary>> getOrders(
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return ApiResponse.ok(service.getOrders(SecurityUtils.currentUserId(), pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> getDetails(@PathVariable Long id) {
        return ApiResponse.ok(service.getOrderDetails(SecurityUtils.currentUserId(), id));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<OrderResponse> cancel(@PathVariable Long id) {
        return ApiResponse.ok("Order cancelled", service.cancel(SecurityUtils.currentUserId(), id));
    }

    @GetMapping("/{id}/track")
    public ApiResponse<TrackingResponse> track(@PathVariable Long id) {
        return ApiResponse.ok(service.track(SecurityUtils.currentUserId(), id));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<OrderResponse> updateStatus(@PathVariable Long id, @RequestParam OrderStatus status) {
        return ApiResponse.ok("Order status updated", service.updateStatus(id, status));
    }

    @PostMapping("/{id}/tracking")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<com.shopmart.module.order.dto.OrderResponse> setTracking(
            @PathVariable Long id, @RequestBody TrackingUpdateRequest req) {
        return ApiResponse.ok("Tracking updated",
                service.setTracking(id, req.trackingNumber(), req.courierPartner()));
    }

    @GetMapping("/{id}/history")
    public ApiResponse<java.util.List<StatusHistoryResponse>> history(@PathVariable Long id) {
        return ApiResponse.ok(service.statusHistory(id));
    }
}
