package com.shopmart.module.user.controller;

import com.shopmart.common.dto.ApiResponse;
import com.shopmart.module.user.dto.AddressRequest;
import com.shopmart.module.user.dto.AddressResponse;
import com.shopmart.module.user.service.AddressService;
import com.shopmart.security.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/addresses")
@RequiredArgsConstructor
@Tag(name = "Addresses")
public class AddressController {

    private final AddressService service;

    @GetMapping
    public ApiResponse<List<AddressResponse>> list() {
        return ApiResponse.ok(service.list(SecurityUtils.currentUserId()));
    }

    @PostMapping
    public ApiResponse<AddressResponse> create(@Valid @RequestBody AddressRequest req) {
        return ApiResponse.ok("Address added", service.create(SecurityUtils.currentUserId(), req));
    }

    @PutMapping("/{id}")
    public ApiResponse<AddressResponse> update(@PathVariable Long id, @Valid @RequestBody AddressRequest req) {
        return ApiResponse.ok("Address updated", service.update(SecurityUtils.currentUserId(), id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(SecurityUtils.currentUserId(), id);
        return ApiResponse.message("Address deleted");
    }
}
