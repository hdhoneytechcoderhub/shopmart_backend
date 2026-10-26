package com.shopmart.module.servicedesk.service;

import com.shopmart.module.servicedesk.dto.*;

import java.util.List;

public interface ServiceDeskService {
    // categories
    List<ServiceCategoryResponse> listActiveCategories();
    List<ServiceCategoryResponse> listAllCategories();
    ServiceCategoryResponse createCategory(ServiceCategoryRequest req);
    ServiceCategoryResponse updateCategory(Long id, ServiceCategoryRequest req);
    void deleteCategory(Long id);

    // services
    List<ServiceItemResponse> listActiveServices(Long categoryId);
    ServiceItemResponse getService(Long id);
    ServiceItemResponse createService(ServiceItemRequest req);
    ServiceItemResponse updateService(Long id, ServiceItemRequest req);
    void deleteService(Long id);

    // bookings - customer
    BookingResponse book(Long userId, BookingRequest req);
    List<BookingResponse> myBookings(Long userId);
    BookingResponse getBooking(Long id);
    BookingResponse track(Long id);
    BookingResponse cancel(Long userId, Long id);

    // bookings - admin
    List<BookingResponse> allBookings(String status);
    BookingResponse assignEngineer(Long bookingId, Long engineerId);

    // bookings - engineer
    List<BookingResponse> engineerBookings(Long engineerId, String status);
    BookingResponse updateStatus(Long engineerId, Long bookingId, String status);
    BookingResponse complete(Long engineerId, Long bookingId, String completionNotes);

    // rating + report
    BookingResponse rate(Long userId, Long bookingId, Integer rating, String comment);
    ServiceReportResponse report();
}
