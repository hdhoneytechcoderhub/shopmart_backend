package com.shopmart.module.machine.controller;

import com.shopmart.common.dto.ApiResponse;
import com.shopmart.common.dto.PageResponse;
import com.shopmart.module.machine.dto.MachineRequest;
import com.shopmart.module.machine.dto.MachineResponse;
import com.shopmart.module.machine.service.MachineService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/machines")
@RequiredArgsConstructor
@Tag(name = "Machines")
public class MachineController {

    private final MachineService service;

    // Public browsing (paged, excludes soft-deleted)
    @GetMapping
    public ApiResponse<PageResponse<MachineResponse>> list(@PageableDefault(size = 10) Pageable pageable) {
        return ApiResponse.ok(service.list(pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<MachineResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<MachineResponse> create(@Valid @RequestBody MachineRequest req) {
        return ApiResponse.ok("Machine created", service.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<MachineResponse> update(@PathVariable Long id, @Valid @RequestBody MachineRequest req) {
        return ApiResponse.ok("Machine updated", service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.message("Machine deleted");
    }
}
