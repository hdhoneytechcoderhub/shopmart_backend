package com.shopmart.module.servicedesk.controller;

import com.shopmart.common.dto.ApiResponse;
import com.shopmart.module.servicedesk.dto.*;
import com.shopmart.module.servicedesk.service.ServiceDeskService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Service Catalog")
public class ServiceCatalogController {

    private final ServiceDeskService service;

    // ---- public ----
    @GetMapping("/service-categories")
    public ApiResponse<List<ServiceCategoryResponse>> categories() {
        return ApiResponse.ok(service.listActiveCategories());
    }

    @GetMapping("/services")
    public ApiResponse<List<ServiceItemResponse>> services(@RequestParam(required = false) Long categoryId) {
        return ApiResponse.ok(service.listActiveServices(categoryId));
    }

    @GetMapping("/services/{id}")
    public ApiResponse<ServiceItemResponse> getService(@PathVariable Long id) {
        return ApiResponse.ok(service.getService(id));
    }

    // ---- admin: category CRUD ----
    @PostMapping("/service-categories")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<ServiceCategoryResponse> createCategory(@Valid @RequestBody ServiceCategoryRequest req) {
        return ApiResponse.ok("Service category created", service.createCategory(req));
    }

    @PutMapping("/service-categories/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<ServiceCategoryResponse> updateCategory(@PathVariable Long id,
                                                               @Valid @RequestBody ServiceCategoryRequest req) {
        return ApiResponse.ok("Service category updated", service.updateCategory(id, req));
    }

    @DeleteMapping("/service-categories/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<Void> deleteCategory(@PathVariable Long id) {
        service.deleteCategory(id);
        return ApiResponse.message("Service category deleted");
    }

    // ---- admin: service CRUD ----
    @PostMapping("/services")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<ServiceItemResponse> createService(@Valid @RequestBody ServiceItemRequest req) {
        return ApiResponse.ok("Service created", service.createService(req));
    }

    @PutMapping("/services/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<ServiceItemResponse> updateService(@PathVariable Long id,
                                                          @Valid @RequestBody ServiceItemRequest req) {
        return ApiResponse.ok("Service updated", service.updateService(id, req));
    }

    @DeleteMapping("/services/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<Void> deleteService(@PathVariable Long id) {
        service.deleteService(id);
        return ApiResponse.message("Service deleted");
    }
}
