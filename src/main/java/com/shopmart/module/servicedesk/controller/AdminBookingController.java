package com.shopmart.module.servicedesk.controller;

import com.shopmart.common.dto.ApiResponse;
import com.shopmart.module.servicedesk.dto.AssignEngineerRequest;
import com.shopmart.module.servicedesk.dto.BookingResponse;
import com.shopmart.module.servicedesk.service.ServiceDeskService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/bookings")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
@Tag(name = "Admin - Service Bookings")
public class AdminBookingController {

    private final ServiceDeskService service;

    @GetMapping
    public ApiResponse<List<BookingResponse>> all(@RequestParam(required = false) String status) {
        return ApiResponse.ok(service.allBookings(status));
    }

    @PostMapping("/{id}/assign")
    public ApiResponse<BookingResponse> assign(@PathVariable Long id,
                                               @Valid @RequestBody AssignEngineerRequest req) {
        return ApiResponse.ok("Engineer assigned", service.assignEngineer(id, req.engineerId()));
    }
}
