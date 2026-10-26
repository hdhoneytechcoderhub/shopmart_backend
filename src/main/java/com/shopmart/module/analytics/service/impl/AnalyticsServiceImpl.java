package com.shopmart.module.analytics.service.impl;

import com.shopmart.module.analytics.dto.*;
import com.shopmart.module.analytics.service.AnalyticsService;
import com.shopmart.module.order.entity.Order;
import com.shopmart.module.order.entity.OrderStatus;
import com.shopmart.module.order.dto.OrderSummary;
import com.shopmart.module.order.mapper.OrderMapper;
import com.shopmart.module.order.repository.OrderItemRepository;
import com.shopmart.module.order.repository.OrderRepository;
import com.shopmart.module.product.repository.ProductRepository;
import com.shopmart.module.product.entity.Product;
import com.shopmart.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    private static final int LOW_STOCK_THRESHOLD = 5;

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse dashboard() {
        BigDecimal revenue = orderRepository.totalRevenue();
        long totalOrders = orderRepository.count();
        long pending = orderRepository.countByStatus(OrderStatus.PENDING);
        long customers = userRepository.count();
        long products = productRepository.count();
        long lowStock = productRepository.countByStockLessThanEqual(LOW_STOCK_THRESHOLD);
        return new DashboardResponse(
                revenue != null ? revenue : BigDecimal.ZERO,
                totalOrders, pending, customers, products, lowStock);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalesPoint> salesOverTime(int days) {
        int span = Math.max(1, Math.min(days, 365));
        Instant since = Instant.now().minus(span, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
        List<Order> orders = orderRepository.findByCreatedAtGreaterThanEqual(since);

        // Pre-seed every day in range with zero so the series has no gaps
        Map<LocalDate, BigDecimal> revenueByDay = new TreeMap<>();
        Map<LocalDate, Long> countByDay = new TreeMap<>();
        LocalDate start = LocalDate.ofInstant(since, ZoneOffset.UTC);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        for (LocalDate d = start; !d.isAfter(today); d = d.plusDays(1)) {
            revenueByDay.put(d, BigDecimal.ZERO);
            countByDay.put(d, 0L);
        }

        for (Order o : orders) {
            if (o.getStatus() == OrderStatus.CANCELLED || o.getCreatedAt() == null) continue;
            LocalDate day = LocalDate.ofInstant(o.getCreatedAt(), ZoneOffset.UTC);
            revenueByDay.merge(day, o.getTotal(), BigDecimal::add);
            countByDay.merge(day, 1L, Long::sum);
        }

        List<SalesPoint> series = new ArrayList<>();
        for (Map.Entry<LocalDate, BigDecimal> e : revenueByDay.entrySet()) {
            series.add(new SalesPoint(e.getKey(), e.getValue(), countByDay.getOrDefault(e.getKey(), 0L)));
        }
        return series;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopProduct> topProducts(int limit) {
        int n = Math.max(1, Math.min(limit, 50));
        List<Object[]> rows = orderItemRepository.topProducts(PageRequest.of(0, n));
        List<TopProduct> result = new ArrayList<>();
        for (Object[] row : rows) {
            Long productId = (Long) row[0];
            String name = (String) row[1];
            long units = ((Number) row[2]).longValue();
            BigDecimal revenue = (BigDecimal) row[3];
            result.add(new TopProduct(productId, name, units, revenue));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StatusCount> orderStatusBreakdown() {
        List<Object[]> rows = orderRepository.countGroupedByStatus();
        List<StatusCount> result = new ArrayList<>();
        for (Object[] row : rows) {
            OrderStatus status = (OrderStatus) row[0];
            long count = ((Number) row[1]).longValue();
            result.add(new StatusCount(status.name(), count));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LowStockProduct> lowStock(int threshold) {
        int t = threshold > 0 ? threshold : LOW_STOCK_THRESHOLD;
        // Candidate set: products under the global threshold, plus any product with a custom
        // override (which might sit above the global threshold but below its own — a product
        // with lowStockThreshold=20 and stock=15 wouldn't be caught by the global-only query).
        java.util.Map<Long, Product> candidates = new java.util.LinkedHashMap<>();
        productRepository.findByStockLessThanEqualOrderByStockAsc(t).forEach(p -> candidates.put(p.getId(), p));
        productRepository.findByLowStockThresholdIsNotNull().forEach(p -> candidates.put(p.getId(), p));

        return candidates.values().stream()
                .filter(p -> p.getStock() <= (p.getLowStockThreshold() != null ? p.getLowStockThreshold() : t))
                .sorted(java.util.Comparator.comparingInt(Product::getStock))
                .map(p -> new LowStockProduct(p.getId(), p.getName(), p.getStock()))
                .toList();
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public CustomerAnalyticsResponse customerAnalytics() {
        long totalCustomers = userRepository.count();
        long withOrders = orderRepository.countDistinctCustomers();
        long repeat = orderRepository.repeatCustomerIds().size();
        long orderCount = orderRepository.count();
        java.math.BigDecimal revenue = orderRepository.totalRevenue();
        if (revenue == null) revenue = java.math.BigDecimal.ZERO;
        java.math.BigDecimal aov = orderCount == 0 ? java.math.BigDecimal.ZERO
                : revenue.divide(java.math.BigDecimal.valueOf(orderCount), 2, java.math.RoundingMode.HALF_UP);
        java.math.BigDecimal repeatRate = withOrders == 0 ? java.math.BigDecimal.ZERO
                : java.math.BigDecimal.valueOf(repeat).multiply(java.math.BigDecimal.valueOf(100))
                .divide(java.math.BigDecimal.valueOf(withOrders), 2, java.math.RoundingMode.HALF_UP);
        long new30 = userRepository.countByCreatedAtAfter(
                java.time.Instant.now().minus(30, java.time.temporal.ChronoUnit.DAYS));
        return new CustomerAnalyticsResponse(totalCustomers, withOrders, repeat, repeatRate, aov, new30);
    }

    @Override
    @Transactional(readOnly = true)
    public RealTimeStatsResponse realTimeStats() {
        Instant startOfToday = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<Order> todayOrders = orderRepository.findByCreatedAtGreaterThanEqual(startOfToday);
        BigDecimal todayRevenue = sumRevenue(todayOrders);

        long activeOrders = orderRepository.countByStatusIn(List.of(
                OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.PACKED, OrderStatus.SHIPPED));
        long pendingOrders = orderRepository.countByStatus(OrderStatus.PENDING);
        long newCustomersToday = userRepository.countByCreatedAtAfter(startOfToday);

        List<OrderSummary> recentOrders = orderRepository.findTop10ByOrderByCreatedAtDesc().stream()
                .map(OrderMapper::toSummary)
                .toList();

        return new RealTimeStatsResponse(
                Instant.now(), todayRevenue, todayOrders.size(),
                newCustomersToday, activeOrders, pendingOrders, recentOrders);
    }

    @Override
    @Transactional(readOnly = true)
    public RevenueAnalyticsResponse revenueAnalytics(String period, int periods) {
        String p = (period == null || period.isBlank()) ? "daily" : period.toLowerCase();
        int bucketDays = switch (p) {
            case "weekly" -> 7;
            case "monthly" -> 30;
            default -> 1;
        };
        int n = periods > 0 ? Math.min(periods, 100) : (p.equals("daily") ? 30 : 12);
        int windowDays = bucketDays * n;

        Instant now = Instant.now();
        Instant currentFrom = now.minus(windowDays, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
        Instant previousFrom = currentFrom.minus(windowDays, ChronoUnit.DAYS);
        Instant previousTo = currentFrom;

        List<Order> currentOrders = orderRepository.findByCreatedAtBetween(currentFrom, now);
        List<Order> previousOrders = orderRepository.findByCreatedAtBetween(previousFrom, previousTo);

        BigDecimal currentRevenue = sumRevenue(currentOrders);
        BigDecimal previousRevenue = sumRevenue(previousOrders);
        BigDecimal growthPct;
        if (previousRevenue.compareTo(BigDecimal.ZERO) == 0) {
            growthPct = currentRevenue.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        } else {
            growthPct = currentRevenue.subtract(previousRevenue)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(previousRevenue, 2, RoundingMode.HALF_UP);
        }

        LocalDate windowStart = LocalDate.ofInstant(currentFrom, ZoneOffset.UTC);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Map<LocalDate, BigDecimal> revenueByBucket = new TreeMap<>();
        Map<LocalDate, Long> countByBucket = new TreeMap<>();
        for (LocalDate b = windowStart; !b.isAfter(today); b = b.plusDays(bucketDays)) {
            revenueByBucket.put(b, BigDecimal.ZERO);
            countByBucket.put(b, 0L);
        }
        for (Order o : currentOrders) {
            if (o.getStatus() == OrderStatus.CANCELLED || o.getCreatedAt() == null) continue;
            LocalDate day = LocalDate.ofInstant(o.getCreatedAt(), ZoneOffset.UTC);
            long daysFromStart = ChronoUnit.DAYS.between(windowStart, day);
            long bucketIndex = daysFromStart / bucketDays;
            LocalDate bucketStart = windowStart.plusDays(bucketIndex * bucketDays);
            revenueByBucket.merge(bucketStart, o.getTotal(), BigDecimal::add);
            countByBucket.merge(bucketStart, 1L, Long::sum);
        }

        List<SalesPoint> series = new ArrayList<>();
        for (Map.Entry<LocalDate, BigDecimal> e : revenueByBucket.entrySet()) {
            series.add(new SalesPoint(e.getKey(), e.getValue(), countByBucket.getOrDefault(e.getKey(), 0L)));
        }

        return new RevenueAnalyticsResponse(p, currentRevenue, previousRevenue, growthPct, series);
    }

    private BigDecimal sumRevenue(List<Order> orders) {
        return orders.stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .map(Order::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
