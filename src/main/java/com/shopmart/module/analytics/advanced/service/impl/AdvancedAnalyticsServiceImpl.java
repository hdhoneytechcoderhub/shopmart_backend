package com.shopmart.module.analytics.advanced.service.impl;

import com.shopmart.module.analytics.advanced.dto.*;
import com.shopmart.module.analytics.advanced.service.AdvancedAnalyticsService;
import com.shopmart.module.order.repository.OrderRepository;
import com.shopmart.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.*;

/**
 * All computations here run in-memory over orders/users fetched for the requested window.
 * That's the right tradeoff at small/medium data volumes (simple, obviously-correct, no new
 * infrastructure) and the wrong one once order volume gets large — at that point this should
 * move to SQL-side aggregation (date_trunc + group by) or a scheduled job that materializes
 * these tables, rather than pulling raw rows into the JVM on every request.
 */
@Service
@RequiredArgsConstructor
public class AdvancedAnalyticsServiceImpl implements AdvancedAnalyticsService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    // ---------------- Revenue Forecast ----------------

    @Override
    @Transactional(readOnly = true)
    public RevenueForecastResponse revenueForecast(int historyMonths, int forecastMonths) {
        int history = historyMonths > 0 ? historyMonths : 6;
        int forecast = forecastMonths > 0 ? forecastMonths : 3;

        List<MonthValue> historical = monthlyRevenue(history);

        // Ordinary least squares over x = 0..n-1 (month index), y = revenue.
        int n = historical.size();
        double[] xs = new double[n];
        double[] ys = new double[n];
        for (int i = 0; i < n; i++) {
            xs[i] = i;
            ys[i] = historical.get(i).value().doubleValue();
        }
        double meanX = Arrays.stream(xs).average().orElse(0);
        double meanY = Arrays.stream(ys).average().orElse(0);
        double num = 0, den = 0;
        for (int i = 0; i < n; i++) {
            num += (xs[i] - meanX) * (ys[i] - meanY);
            den += (xs[i] - meanX) * (xs[i] - meanX);
        }
        double slope = den == 0 ? 0 : num / den;
        double intercept = meanY - slope * meanX;

        List<MonthValue> forecastPoints = new ArrayList<>();
        YearMonth lastMonth = n > 0 ? YearMonth.parse(historical.get(n - 1).month()) : YearMonth.now(ZoneOffset.UTC);
        for (int i = 1; i <= forecast; i++) {
            double predicted = intercept + slope * (n - 1 + i);
            BigDecimal value = BigDecimal.valueOf(Math.max(0, predicted)).setScale(2, RoundingMode.HALF_UP);
            forecastPoints.add(new MonthValue(lastMonth.plusMonths(i).toString(), value));
        }

        return new RevenueForecastResponse(historical, forecastPoints,
                "linear-trend (ordinary least squares over trailing " + history + " months)");
    }

    private List<MonthValue> monthlyRevenue(int months) {
        Instant from = YearMonth.now(ZoneOffset.UTC).minusMonths(months - 1L).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        Map<YearMonth, BigDecimal> byMonth = new TreeMap<>();
        for (int i = 0; i < months; i++) {
            byMonth.put(YearMonth.now(ZoneOffset.UTC).minusMonths(months - 1L - i), BigDecimal.ZERO);
        }
        // SQL-side aggregation (see OrderRepository.monthlyOrderAggregates) instead of pulling
        // every order row into the JVM — see the class javadoc for when this still isn't enough.
        for (OrderRepository.MonthlyOrderAggregate row : orderRepository.monthlyOrderAggregates(from)) {
            YearMonth ym = YearMonth.from(row.getBucketMonth().toInstant().atZone(ZoneOffset.UTC));
            byMonth.merge(ym, row.getRevenue(), (existing, ignored) -> row.getRevenue());
        }
        List<MonthValue> result = new ArrayList<>();
        for (var e : byMonth.entrySet()) {
            result.add(new MonthValue(e.getKey().toString(), e.getValue().setScale(2, RoundingMode.HALF_UP)));
        }
        return result;
    }

    // ---------------- Cohorts / Retention ----------------

    @Override
    @Transactional(readOnly = true)
    public CohortResponse customerCohorts(int cohortMonths, int trackMonths) {
        int cMonths = cohortMonths > 0 ? cohortMonths : 6;
        int track = trackMonths > 0 ? trackMonths : 3;
        YearMonth earliestCohort = YearMonth.now(ZoneOffset.UTC).minusMonths(cMonths - 1L);
        Instant cohortFrom = earliestCohort.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        // Cohort sizes: reuses the same "new customers per month" definition already used by
        // growthTrend(), computed in Postgres — see UserRepository.monthlyNewCustomers().
        Map<YearMonth, Long> cohortSizes = new TreeMap<>();
        for (var row : userRepository.monthlyNewCustomers(cohortFrom)) {
            cohortSizes.put(YearMonth.from(row.getBucketMonth().toInstant().atZone(ZoneOffset.UTC)), row.getNewCustomers());
        }

        // Retention counts: one Postgres query for every (cohort, offset) pair at once —
        // see OrderRepository.cohortRetention() — instead of an N-cohort loop of in-memory filters.
        Map<YearMonth, long[]> retainedByCohort = new TreeMap<>();
        for (OrderRepository.CohortRetentionRow row : orderRepository.cohortRetention(cohortFrom, track - 1)) {
            YearMonth cohort = YearMonth.from(row.getCohortMonth().toInstant().atZone(ZoneOffset.UTC));
            int offset = row.getMonthOffset();
            if (offset < 0 || offset >= track) continue; // defensive; SQL HAVING already filters this
            retainedByCohort.computeIfAbsent(cohort, k -> new long[track])[offset] = row.getRetainedCount();
        }

        List<CohortRow> rows = new ArrayList<>();
        for (var entry : cohortSizes.entrySet()) {
            YearMonth cohortMonth = entry.getKey();
            long size = entry.getValue();
            long[] retained = retainedByCohort.getOrDefault(cohortMonth, new long[track]);
            List<Long> retainedList = new ArrayList<>();
            for (long v : retained) retainedList.add(v);
            rows.add(new CohortRow(cohortMonth.toString(), size, retainedList));
        }
        return new CohortResponse(rows, track);
    }

    @Override
    @Transactional(readOnly = true)
    public RetentionResponse retentionAnalysis(int cohortMonths, int trackMonths) {
        CohortResponse cohorts = customerCohorts(cohortMonths, trackMonths);
        List<RetentionRow> rows = cohorts.cohorts().stream().map(c -> {
            List<Double> pct = c.retainedByMonthOffset().stream()
                    .map(count -> c.cohortSize() == 0 ? 0.0
                            : Math.round((count * 10000.0 / c.cohortSize())) / 100.0)
                    .toList();
            return new RetentionRow(c.cohortMonth(), c.cohortSize(), pct);
        }).toList();
        return new RetentionResponse(rows, cohorts.monthsTracked());
    }

    // ---------------- Growth Trend ----------------

    @Override
    @Transactional(readOnly = true)
    public GrowthTrendResponse growthTrend(int months) {
        int m = months > 0 ? months : 6;
        Instant from = YearMonth.now(ZoneOffset.UTC).minusMonths(m - 1L).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        Map<YearMonth, BigDecimal> revenueByMonth = new TreeMap<>();
        Map<YearMonth, Long> ordersByMonth = new TreeMap<>();
        Map<YearMonth, Long> newCustomersByMonth = new TreeMap<>();
        for (int i = 0; i < m; i++) {
            YearMonth ym = YearMonth.now(ZoneOffset.UTC).minusMonths(m - 1L - i);
            revenueByMonth.put(ym, BigDecimal.ZERO);
            ordersByMonth.put(ym, 0L);
            newCustomersByMonth.put(ym, 0L);
        }
        // Both aggregates now computed in Postgres (GROUP BY date_trunc) — see
        // OrderRepository.monthlyOrderAggregates / UserRepository.monthlyNewCustomers —
        // instead of loading every order and every customer into the JVM.
        for (OrderRepository.MonthlyOrderAggregate row : orderRepository.monthlyOrderAggregates(from)) {
            YearMonth ym = YearMonth.from(row.getBucketMonth().toInstant().atZone(ZoneOffset.UTC));
            if (revenueByMonth.containsKey(ym)) {
                revenueByMonth.put(ym, row.getRevenue());
                ordersByMonth.put(ym, row.getOrderCount());
            }
        }
        for (var row : userRepository.monthlyNewCustomers(from)) {
            YearMonth ym = YearMonth.from(row.getBucketMonth().toInstant().atZone(ZoneOffset.UTC));
            if (newCustomersByMonth.containsKey(ym)) newCustomersByMonth.put(ym, row.getNewCustomers());
        }

        List<YearMonth> ordered = new ArrayList<>(revenueByMonth.keySet());
        List<GrowthPoint> points = new ArrayList<>();
        for (int i = 0; i < ordered.size(); i++) {
            YearMonth ym = ordered.get(i);
            BigDecimal revenue = revenueByMonth.get(ym);
            long orderCount = ordersByMonth.get(ym);
            long newCustomers = newCustomersByMonth.get(ym);
            Double revenueGrowth = null, orderGrowth = null, customerGrowth = null;
            if (i > 0) {
                YearMonth prev = ordered.get(i - 1);
                revenueGrowth = pctChange(revenueByMonth.get(prev), revenue);
                orderGrowth = pctChange(BigDecimal.valueOf(ordersByMonth.get(prev)), BigDecimal.valueOf(orderCount));
                customerGrowth = pctChange(BigDecimal.valueOf(newCustomersByMonth.get(prev)), BigDecimal.valueOf(newCustomers));
            }
            points.add(new GrowthPoint(ym.toString(), revenue.setScale(2, RoundingMode.HALF_UP), revenueGrowth,
                    orderCount, orderGrowth, newCustomers, customerGrowth));
        }
        return new GrowthTrendResponse(points);
    }

    private Double pctChange(BigDecimal prev, BigDecimal current) {
        if (prev == null || prev.compareTo(BigDecimal.ZERO) == 0) return null;
        return current.subtract(prev).divide(prev, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).doubleValue();
    }

    // ---------------- KPI Aggregation ----------------

    @Override
    @Transactional(readOnly = true)
    public KpiResponse kpis() {
        BigDecimal totalRevenue = orderRepository.totalRevenue();
        long totalOrders = orderRepository.count();
        long totalCustomers = orderRepository.countDistinctCustomers();
        long repeatCustomers = orderRepository.repeatCustomerIds().size();
        double repeatRatePct = totalCustomers == 0 ? 0
                : Math.round((repeatCustomers * 10000.0 / totalCustomers)) / 100.0;
        BigDecimal aov = totalOrders == 0 ? BigDecimal.ZERO
                : totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP);

        GrowthTrendResponse trend = growthTrend(2);
        Double lastRevenueGrowth = trend.points().isEmpty() ? null : trend.points().get(trend.points().size() - 1).revenueGrowthPct();
        Double lastOrderGrowth = trend.points().isEmpty() ? null : trend.points().get(trend.points().size() - 1).orderGrowthPct();

        return new KpiResponse(totalRevenue, totalOrders, totalCustomers, aov, repeatRatePct,
                lastRevenueGrowth, lastOrderGrowth);
    }
}
