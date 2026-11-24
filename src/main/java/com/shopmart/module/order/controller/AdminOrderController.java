package com.shopmart.module.order.controller;

import com.shopmart.common.dto.ApiResponse;
import com.shopmart.common.dto.PageResponse;
import com.shopmart.module.order.dto.AdminOrderDetail;
import com.shopmart.module.order.dto.AdminOrderSummary;
import com.shopmart.module.order.entity.OrderStatus;
import com.shopmart.module.order.service.AdminOrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
@Tag(name = "Admin Orders")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class AdminOrderController {

    private final AdminOrderService service;

    @GetMapping
    public ApiResponse<PageResponse<AdminOrderSummary>> search(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(service.search(status, q, pageable));
    }

    /**
     * Latest orders across every customer — used by the dashboard's "Recent orders" table.
     * Declared before /{id} so "recent" is not parsed as an order id.
     */
    @GetMapping("/recent")
    public ApiResponse<List<AdminOrderSummary>> recent(@RequestParam(defaultValue = "5") int size) {
        return ApiResponse.ok(service.recent(size));
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminOrderDetail> detail(@PathVariable Long id) {
        return ApiResponse.ok(service.detail(id));
    }
}
