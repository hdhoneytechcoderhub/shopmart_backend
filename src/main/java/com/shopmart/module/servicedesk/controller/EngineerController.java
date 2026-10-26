package com.shopmart.module.servicedesk.controller;

import com.shopmart.common.dto.ApiResponse;
import com.shopmart.module.servicedesk.dto.BookingResponse;
import com.shopmart.module.servicedesk.dto.BookingStatusUpdateRequest;
import com.shopmart.module.servicedesk.dto.CompleteBookingRequest;
import com.shopmart.module.servicedesk.service.ServiceDeskService;
import com.shopmart.security.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/engineer")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ENGINEER','ADMIN','SUPER_ADMIN')")
@Tag(name = "Engineer")
public class EngineerController {

    private final ServiceDeskService service;

    @GetMapping("/bookings")
    public ApiResponse<List<BookingResponse>> myAssigned(@RequestParam(required = false) String status) {
        return ApiResponse.ok(service.engineerBookings(SecurityUtils.currentUserId(), status));
    }

    @PatchMapping("/bookings/{id}/status")
    public ApiResponse<BookingResponse> updateStatus(@PathVariable Long id,
                                                     @Valid @RequestBody BookingStatusUpdateRequest req) {
        return ApiResponse.ok("Status updated", service.updateStatus(SecurityUtils.currentUserId(), id, req.status()));
    }

    @PostMapping("/bookings/{id}/complete")
    public ApiResponse<BookingResponse> complete(@PathVariable Long id,
                                                 @RequestBody CompleteBookingRequest req) {
        return ApiResponse.ok("Service completed",
                service.complete(SecurityUtils.currentUserId(), id, req.completionNotes()));
    }
}
