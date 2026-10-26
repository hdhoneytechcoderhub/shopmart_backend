package com.shopmart.module.category.controller;

import com.shopmart.common.dto.ApiResponse;
import com.shopmart.common.dto.PageResponse;
import com.shopmart.module.category.dto.CategoryRequest;
import com.shopmart.module.category.dto.CategoryResponse;
import com.shopmart.module.category.service.CategoryService;
import com.shopmart.module.product.dto.ProductFilter;
import com.shopmart.module.product.dto.ProductSummary;
import com.shopmart.module.product.service.ProductService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Tag(name = "Categories")
public class CategoryController {

    private final CategoryService service;
    private final ProductService productService;

    @GetMapping
    public ApiResponse<List<CategoryResponse>> getAll() {
        return ApiResponse.ok(service.getAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<CategoryResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(service.getById(id));
    }

    /** Top-level categories (no parent) — for building a category menu/tree from the root. */
    @GetMapping("/root")
    public ApiResponse<List<CategoryResponse>> getRoot() {
        return ApiResponse.ok(service.getChildren(null));
    }

    @GetMapping("/{id}/children")
    public ApiResponse<List<CategoryResponse>> getChildren(@PathVariable Long id) {
        return ApiResponse.ok(service.getChildren(id));
    }

    /** Public: active products in a category (paged). */
    @GetMapping("/{id}/products")
    public ApiResponse<PageResponse<ProductSummary>> products(
            @PathVariable Long id, @PageableDefault(size = 20) Pageable pageable) {
        ProductFilter filter = new ProductFilter(null, id, null, null, null, null);
        return ApiResponse.ok(productService.search(filter, pageable));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        return ApiResponse.ok("Category created", service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CategoryResponse> update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return ApiResponse.ok("Category updated", service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.message("Category deleted");
    }
}
