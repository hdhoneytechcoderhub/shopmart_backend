package com.shopmart.module.servicedesk.service.impl;

import com.shopmart.common.exception.BadRequestException;
import com.shopmart.common.exception.ResourceNotFoundException;
import com.shopmart.module.servicedesk.dto.*;
import com.shopmart.module.servicedesk.entity.*;
import com.shopmart.module.servicedesk.repository.*;
import com.shopmart.module.servicedesk.service.ServiceDeskService;
import com.shopmart.module.user.entity.Role;
import com.shopmart.module.user.entity.User;
import com.shopmart.module.user.repository.UserRepository;
import com.shopmart.module.audit.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceDeskServiceImpl implements ServiceDeskService {

    private final ServiceCategoryRepository categoryRepo;
    private final ServiceItemRepository itemRepo;
    private final ServiceBookingRepository bookingRepo;
    private final UserRepository userRepository;
    private final AuditService auditService;

    // ---------------- categories ----------------
    @Override @Transactional(readOnly = true)
    public List<ServiceCategoryResponse> listActiveCategories() {
        return categoryRepo.findByActiveTrue().stream().map(this::toCat).toList();
    }

    @Override @Transactional(readOnly = true)
    public List<ServiceCategoryResponse> listAllCategories() {
        return categoryRepo.findAll().stream().map(this::toCat).toList();
    }

    @Override @Transactional
    public ServiceCategoryResponse createCategory(ServiceCategoryRequest req) {
        ServiceCategory c = new ServiceCategory();
        c.setName(req.name());
        c.setDescription(req.description());
        c.setImageUrl(req.imageUrl());
        c.setActive(req.active() == null || req.active());
        return toCat(categoryRepo.save(c));
    }

    @Override @Transactional
    public ServiceCategoryResponse updateCategory(Long id, ServiceCategoryRequest req) {
        ServiceCategory c = categoryRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service category not found: " + id));
        c.setName(req.name());
        c.setDescription(req.description());
        c.setImageUrl(req.imageUrl());
        if (req.active() != null) c.setActive(req.active());
        return toCat(categoryRepo.save(c));
    }

    @Override @Transactional
    public void deleteCategory(Long id) {
        if (!categoryRepo.existsById(id))
            throw new ResourceNotFoundException("Service category not found: " + id);
        categoryRepo.deleteById(id);
    }

    // ---------------- services ----------------
    @Override @Transactional(readOnly = true)
    public List<ServiceItemResponse> listActiveServices(Long categoryId) {
        List<ServiceItem> items = (categoryId == null)
                ? itemRepo.findByActiveTrue()
                : itemRepo.findByActiveTrueAndCategoryId(categoryId);
        return items.stream().map(this::toItem).toList();
    }

    @Override @Transactional(readOnly = true)
    public ServiceItemResponse getService(Long id) {
        return toItem(findItem(id));
    }

    @Override @Transactional
    public ServiceItemResponse createService(ServiceItemRequest req) {
        ServiceItem s = new ServiceItem();
        apply(s, req);
        return toItem(itemRepo.save(s));
    }

    @Override @Transactional
    public ServiceItemResponse updateService(Long id, ServiceItemRequest req) {
        ServiceItem s = findItem(id);
        apply(s, req);
        return toItem(itemRepo.save(s));
    }

    @Override @Transactional
    public void deleteService(Long id) {
        if (!itemRepo.existsById(id))
            throw new ResourceNotFoundException("Service not found: " + id);
        itemRepo.deleteById(id);
    }

    private void apply(ServiceItem s, ServiceItemRequest req) {
        s.setName(req.name());
        s.setDescription(req.description());
        s.setPrice(req.price() == null ? BigDecimal.ZERO : req.price());
        s.setDurationMinutes(req.durationMinutes());
        s.setCategoryId(req.categoryId());
        s.setImageUrl(req.imageUrl());
        if (req.active() != null) s.setActive(req.active());
    }

    // ---------------- bookings: customer ----------------
    @Override @Transactional
    public BookingResponse book(Long userId, BookingRequest req) {
        ServiceItem item = findItem(req.serviceItemId());
        if (!item.isActive()) throw new BadRequestException("This service is not available");
        ServiceBooking b = new ServiceBooking();
        b.setUserId(userId);
        b.setServiceItemId(item.getId());
        b.setStatus(BookingStatus.REQUESTED);
        b.setScheduledAt(req.scheduledAt());
        b.setAddress(req.address());
        b.setPhone(req.phone());
        b.setNotes(req.notes());
        return toBooking(bookingRepo.save(b));
    }

    @Override @Transactional(readOnly = true)
    public List<BookingResponse> myBookings(Long userId) {
        return bookingRepo.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toBooking).toList();
    }

    @Override @Transactional(readOnly = true)
    public BookingResponse getBooking(Long id) {
        return toBooking(findBooking(id));
    }

    @Override @Transactional(readOnly = true)
    public BookingResponse track(Long id) {
        return toBooking(findBooking(id));
    }

    @Override @Transactional
    public BookingResponse cancel(Long userId, Long id) {
        ServiceBooking b = findBooking(id);
        if (!b.getUserId().equals(userId))
            throw new BadRequestException("You can only cancel your own booking");
        if (b.getStatus() == BookingStatus.COMPLETED)
            throw new BadRequestException("Completed booking cannot be cancelled");
        b.setStatus(BookingStatus.CANCELLED);
        return toBooking(bookingRepo.save(b));
    }

    // ---------------- bookings: admin ----------------
    @Override @Transactional(readOnly = true)
    public List<BookingResponse> allBookings(String status) {
        List<ServiceBooking> list = (status == null || status.isBlank())
                ? bookingRepo.findAllByOrderByCreatedAtDesc()
                : bookingRepo.findByStatusOrderByCreatedAtDesc(parseStatus(status));
        return list.stream().map(this::toBooking).toList();
    }

    @Override @Transactional
    public BookingResponse assignEngineer(Long bookingId, Long engineerId) {
        ServiceBooking b = findBooking(bookingId);
        User engineer = userRepository.findById(engineerId)
                .orElseThrow(() -> new ResourceNotFoundException("Engineer not found: " + engineerId));
        if (!engineer.getRoles().contains(Role.ROLE_ENGINEER))
            throw new BadRequestException("User " + engineerId + " is not an engineer");
        b.setEngineerId(engineerId);
        if (b.getStatus() == BookingStatus.REQUESTED) b.setStatus(BookingStatus.ASSIGNED);
        ServiceBooking saved = bookingRepo.save(b);
        auditService.log(engineerId, "BOOKING_ASSIGNED", "ServiceBooking", saved.getId(), "Engineer " + engineerId + " assigned");
        return toBooking(saved);
    }

    // ---------------- bookings: engineer ----------------
    @Override @Transactional(readOnly = true)
    public List<BookingResponse> engineerBookings(Long engineerId, String status) {
        List<ServiceBooking> list = (status == null || status.isBlank())
                ? bookingRepo.findByEngineerIdOrderByCreatedAtDesc(engineerId)
                : bookingRepo.findByEngineerIdAndStatusOrderByCreatedAtDesc(engineerId, parseStatus(status));
        return list.stream().map(this::toBooking).toList();
    }

    @Override @Transactional
    public BookingResponse updateStatus(Long engineerId, Long bookingId, String status) {
        ServiceBooking b = ownedByEngineer(engineerId, bookingId);
        b.setStatus(parseStatus(status));
        return toBooking(bookingRepo.save(b));
    }

    @Override @Transactional
    public BookingResponse complete(Long engineerId, Long bookingId, String completionNotes) {
        ServiceBooking b = ownedByEngineer(engineerId, bookingId);
        b.setStatus(BookingStatus.COMPLETED);
        b.setCompletionNotes(completionNotes);
        return toBooking(bookingRepo.save(b));
    }

    @Override
    @Transactional
    public BookingResponse rate(Long userId, Long bookingId, Integer rating, String comment) {
        ServiceBooking b = findBooking(bookingId);
        if (!b.getUserId().equals(userId))
            throw new BadRequestException("You can only rate your own booking");
        if (b.getStatus() != BookingStatus.COMPLETED)
            throw new BadRequestException("Only completed services can be rated");
        b.setRating(rating);
        b.setRatingComment(comment);
        return toBooking(bookingRepo.save(b));
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceReportResponse report() {
        java.math.BigDecimal rev = bookingRepo.completedRevenue();
        Double avg = bookingRepo.averageRating();
        return new ServiceReportResponse(
                bookingRepo.count(),
                bookingRepo.countByStatus(BookingStatus.REQUESTED),
                bookingRepo.countByStatus(BookingStatus.ASSIGNED),
                bookingRepo.countByStatus(BookingStatus.IN_PROGRESS),
                bookingRepo.countByStatus(BookingStatus.COMPLETED),
                bookingRepo.countByStatus(BookingStatus.CANCELLED),
                rev != null ? rev : java.math.BigDecimal.ZERO,
                avg
        );
    }

    // ---------------- helpers ----------------
    private ServiceBooking ownedByEngineer(Long engineerId, Long bookingId) {
        ServiceBooking b = findBooking(bookingId);
        if (b.getEngineerId() == null || !b.getEngineerId().equals(engineerId))
            throw new BadRequestException("This booking is not assigned to you");
        return b;
    }

    private ServiceItem findItem(Long id) {
        return itemRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found: " + id));
    }

    private ServiceBooking findBooking(Long id) {
        return bookingRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + id));
    }

    private BookingStatus parseStatus(String s) {
        try {
            return BookingStatus.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status: " + s);
        }
    }

    private ServiceCategoryResponse toCat(ServiceCategory c) {
        return new ServiceCategoryResponse(c.getId(), c.getName(), c.getDescription(), c.getImageUrl(), c.isActive());
    }

    private ServiceItemResponse toItem(ServiceItem s) {
        return new ServiceItemResponse(s.getId(), s.getName(), s.getDescription(), s.getPrice(),
                s.getDurationMinutes(), s.getCategoryId(), s.getImageUrl(), s.isActive());
    }

    private BookingResponse toBooking(ServiceBooking b) {
        return new BookingResponse(b.getId(), b.getUserId(), b.getServiceItemId(), b.getEngineerId(),
                b.getStatus().name(), b.getScheduledAt(), b.getAddress(), b.getPhone(),
                b.getNotes(), b.getCompletionNotes(), b.getRating(), b.getRatingComment(),
                b.getCreatedAt());
    }
}
