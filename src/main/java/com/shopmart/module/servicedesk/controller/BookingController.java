package com.shopmart.module.servicedesk.controller;

import com.shopmart.common.dto.ApiResponse;
import com.shopmart.module.servicedesk.dto.BookingRequest;
import com.shopmart.module.servicedesk.dto.BookingResponse;
import com.shopmart.module.servicedesk.dto.RatingRequest;
import com.shopmart.module.servicedesk.service.ServiceDeskService;
import com.shopmart.security.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@Tag(name = "Service Bookings")
public class BookingController {

    private final ServiceDeskService service;

    @PostMapping
    public ApiResponse<BookingResponse> book(@Valid @RequestBody BookingRequest req) {
        return ApiResponse.ok("Booking created", service.book(SecurityUtils.currentUserId(), req));
    }

    @GetMapping("/my")
    public ApiResponse<List<BookingResponse>> myBookings() {
        return ApiResponse.ok(service.myBookings(SecurityUtils.currentUserId()));
    }

    @GetMapping("/{id}")
    public ApiResponse<BookingResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(service.getBooking(id));
    }

    @GetMapping("/{id}/track")
    public ApiResponse<BookingResponse> track(@PathVariable Long id) {
        return ApiResponse.ok(service.track(id));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<BookingResponse> cancel(@PathVariable Long id) {
        return ApiResponse.ok("Booking cancelled", service.cancel(SecurityUtils.currentUserId(), id));
    }

    @PostMapping("/{id}/rating")
    public ApiResponse<BookingResponse> rate(@PathVariable Long id,
                                             @jakarta.validation.Valid @RequestBody RatingRequest req) {
        return ApiResponse.ok("Thanks for your rating",
                service.rate(SecurityUtils.currentUserId(), id, req.rating(), req.comment()));
    }
}
