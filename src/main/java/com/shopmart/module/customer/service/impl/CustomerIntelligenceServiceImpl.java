package com.shopmart.module.customer.service.impl;

import com.shopmart.common.dto.PageResponse;
import com.shopmart.common.exception.BadRequestException;
import com.shopmart.common.exception.ResourceNotFoundException;
import com.shopmart.module.auth.entity.RefreshToken;
import com.shopmart.module.auth.repository.RefreshTokenRepository;
import com.shopmart.module.customer.dto.*;
import com.shopmart.module.customer.entity.CustomerNote;
import com.shopmart.module.customer.repository.CustomerNoteRepository;
import com.shopmart.module.customer.service.CustomerIntelligenceService;
import com.shopmart.module.order.entity.Order;
import com.shopmart.module.order.entity.PaymentStatus;
import com.shopmart.module.order.repository.OrderRepository;
import com.shopmart.module.review.entity.Review;
import com.shopmart.module.review.repository.ReviewRepository;
import com.shopmart.module.user.entity.User;
import com.shopmart.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class CustomerIntelligenceServiceImpl implements CustomerIntelligenceService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ReviewRepository reviewRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CustomerNoteRepository noteRepository;

    @Value("${app.customer.segments.vip-min-spend:50000}")
    private BigDecimal vipMinSpend;
    @Value("${app.customer.segments.high-value-min-spend:20000}")
    private BigDecimal highValueMinSpend;
    @Value("${app.customer.segments.new-customer-days:30}")
    private int newCustomerDays;
    @Value("${app.customer.segments.inactive-days:90}")
    private int inactiveDays;

    // ---------------- Insights ----------------

    @Override
    @Transactional(readOnly = true)
    public CustomerInsightsResponse insights(Long customerId) {
        User user = findUser(customerId);
        BigDecimal totalSpent = orderRepository.totalSpentByUser(customerId);
        long totalOrders = orderRepository.findByUserIdOrderByCreatedAtDesc(customerId).size();
        long paidOrders = orderRepository.countByUserIdAndPaymentStatus(customerId, PaymentStatus.PAID);
        BigDecimal avgOrderValue = paidOrders > 0
                ? totalSpent.divide(BigDecimal.valueOf(paidOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        long reviewCount = reviewRepository.countByUserId(customerId);
        long activeSessions = refreshTokenRepository.countByUserIdAndRevokedFalse(customerId);
        Instant lastOrderAt = orderRepository.findFirstByUserIdOrderByCreatedAtDesc(customerId)
                .map(Order::getCreatedAt).orElse(null);
        Instant lastLoginAt = refreshTokenRepository.findTopByUserIdOrderByLastUsedAtDesc(customerId)
                .map(RefreshToken::getLastUsedAt).orElse(null);

        return new CustomerInsightsResponse(
                user.getId(), user.getName(), user.getEmail(),
                computeSegment(user, totalSpent, paidOrders, lastOrderAt, lastLoginAt),
                user.getCreatedAt(), totalOrders, paidOrders, totalSpent, avgOrderValue,
                reviewCount, activeSessions, lastOrderAt, lastLoginAt);
    }

    // ---------------- Timeline / Activity ----------------

    @Override
    @Transactional(readOnly = true)
    public List<CustomerTimelineEvent> timeline(Long customerId, int limit) {
        User user = findUser(customerId);
        Stream<CustomerTimelineEvent> registration = Stream.of(new CustomerTimelineEvent(
                TimelineEventType.REGISTRATION, "Account created",
                "Registered via " + user.getProvider(), user.getCreatedAt()));

        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(customerId);
        Stream<CustomerTimelineEvent> orderEvents = orders.stream().map(o -> new CustomerTimelineEvent(
                TimelineEventType.ORDER, "Order " + o.getOrderNumber(),
                "Status " + o.getStatus() + " · " + o.getTotal() + " INR", o.getCreatedAt()));

        // Payment events are derived from paid orders (no separate payment-timestamp column
        // exposed on Order) — uses the order's updatedAt as an approximation of when it was
        // marked PAID. For exact gateway timestamps, cross-reference PaymentTransaction by orderId.
        Stream<CustomerTimelineEvent> paymentEvents = orders.stream()
                .filter(o -> o.getPaymentStatus() == PaymentStatus.PAID)
                .map(o -> new CustomerTimelineEvent(
                        TimelineEventType.PAYMENT, "Payment received",
                        "Order " + o.getOrderNumber() + " · " + o.getTotal() + " INR",
                        o.getUpdatedAt() != null ? o.getUpdatedAt() : o.getCreatedAt()));

        List<Review> reviews = reviewRepository.findByUserIdOrderByCreatedAtDesc(customerId);
        Stream<CustomerTimelineEvent> reviewEvents = reviews.stream().map(r -> new CustomerTimelineEvent(
                TimelineEventType.REVIEW, "Left a " + r.getRating() + "\u2605 review",
                r.getTitle() != null ? r.getTitle() : "", r.getCreatedAt()));

        List<RefreshToken> sessions = refreshTokenRepository.findByUserIdOrderByCreatedAtDesc(customerId);
        Stream<CustomerTimelineEvent> loginEvents = sessions.stream().map(s -> new CustomerTimelineEvent(
                TimelineEventType.LOGIN, "Signed in",
                (s.getDeviceInfo() != null ? s.getDeviceInfo() : "Unknown device")
                        + (s.getIpAddress() != null ? " · " + s.getIpAddress() : ""), s.getCreatedAt()));

        return Stream.of(registration, orderEvents, paymentEvents, reviewEvents, loginEvents)
                .flatMap(s -> s)
                .sorted(Comparator.comparing(CustomerTimelineEvent::occurredAt).reversed())
                .limit(Math.max(1, limit))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerTimelineEvent> recentActivity(Long customerId, int limit) {
        return timeline(customerId, limit);
    }

    // ---------------- Segmentation ----------------

    @Override
    @Transactional(readOnly = true)
    public CustomerSegment segmentOf(Long customerId) {
        User user = findUser(customerId);
        BigDecimal totalSpent = orderRepository.totalSpentByUser(customerId);
        long paidOrders = orderRepository.countByUserIdAndPaymentStatus(customerId, PaymentStatus.PAID);
        Instant lastOrderAt = orderRepository.findFirstByUserIdOrderByCreatedAtDesc(customerId)
                .map(Order::getCreatedAt).orElse(null);
        Instant lastLoginAt = refreshTokenRepository.findTopByUserIdOrderByLastUsedAtDesc(customerId)
                .map(RefreshToken::getLastUsedAt).orElse(null);
        return computeSegment(user, totalSpent, paidOrders, lastOrderAt, lastLoginAt);
    }

    private CustomerSegment computeSegment(User user, BigDecimal totalSpent, long paidOrders,
                                            Instant lastOrderAt, Instant lastLoginAt) {
        if (totalSpent != null && totalSpent.compareTo(vipMinSpend) >= 0) return CustomerSegment.VIP;
        if (totalSpent != null && totalSpent.compareTo(highValueMinSpend) >= 0) return CustomerSegment.HIGH_VALUE;
        if (paidOrders > 1) return CustomerSegment.RETURNING;

        Instant now = Instant.now();
        boolean isNew = user.getCreatedAt() != null
                && ChronoUnit.DAYS.between(user.getCreatedAt(), now) <= newCustomerDays;
        if (isNew && paidOrders == 0) return CustomerSegment.NEW;

        Instant lastActivity = latest(lastOrderAt, lastLoginAt, user.getCreatedAt());
        boolean isInactive = lastActivity == null
                || ChronoUnit.DAYS.between(lastActivity, now) >= inactiveDays;
        if (!isNew && isInactive) return CustomerSegment.INACTIVE;

        return CustomerSegment.STANDARD;
    }

    private Instant latest(Instant... candidates) {
        Instant max = null;
        for (Instant c : candidates) {
            if (c != null && (max == null || c.isAfter(max))) max = c;
        }
        return max;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> customerIdsInSegment(CustomerSegment segment) {
        return userRepository.findAll().stream()
                .filter(u -> u.getRoles().contains(com.shopmart.module.user.entity.Role.ROLE_CUSTOMER))
                .filter(u -> segmentOf(u.getId()) == segment)
                .map(User::getId)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CustomerInsightsResponse> bySegment(CustomerSegment segment, Pageable pageable) {
        // See interface javadoc: computed live across all users. Acceptable at small/medium
        // scale; move to a materialized/scheduled segment table if the customer base grows large.
        List<CustomerInsightsResponse> matches = userRepository.findAll().stream()
                .filter(u -> u.getRoles().contains(com.shopmart.module.user.entity.Role.ROLE_CUSTOMER))
                .map(u -> insights(u.getId()))
                .filter(i -> i.segment() == segment)
                .sorted(Comparator.comparing(CustomerInsightsResponse::totalSpent).reversed())
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), matches.size());
        List<CustomerInsightsResponse> pageContent = start >= matches.size()
                ? List.of() : matches.subList(start, end);
        Page<CustomerInsightsResponse> page = new PageImpl<>(pageContent, pageable, matches.size());
        return PageResponse.from(page);
    }

    // ---------------- Notes ----------------

    @Override
    @Transactional
    public CustomerNoteResponse addNote(Long customerId, Long authorId, String note) {
        findUser(customerId); // 404s if the customer doesn't exist
        if (note == null || note.isBlank()) {
            throw new BadRequestException("Note text is required");
        }
        CustomerNote entity = new CustomerNote();
        entity.setCustomerId(customerId);
        entity.setAuthorId(authorId);
        entity.setNote(note.trim());
        entity = noteRepository.save(entity);
        return toNoteResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CustomerNoteResponse> listNotes(Long customerId, Pageable pageable) {
        return PageResponse.from(noteRepository.findByCustomerIdOrderByCreatedAtDesc(customerId, pageable)
                .map(this::toNoteResponse));
    }

    @Override
    @Transactional
    public void deleteNote(Long customerId, Long noteId) {
        CustomerNote note = noteRepository.findById(noteId)
                .orElseThrow(() -> new ResourceNotFoundException("CustomerNote", "id", noteId));
        if (!note.getCustomerId().equals(customerId)) {
            throw new BadRequestException("This note does not belong to the given customer");
        }
        noteRepository.delete(note);
    }

    private CustomerNoteResponse toNoteResponse(CustomerNote n) {
        String authorName = userRepository.findById(n.getAuthorId()).map(User::getName).orElse("Unknown");
        return new CustomerNoteResponse(n.getId(), n.getCustomerId(), n.getAuthorId(), authorName,
                n.getNote(), n.getCreatedAt());
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }
}
