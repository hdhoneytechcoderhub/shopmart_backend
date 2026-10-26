package com.shopmart.module.report.schedule.service.impl;

import com.shopmart.common.dto.PageResponse;
import com.shopmart.common.exception.ResourceNotFoundException;
import com.shopmart.common.notification.EmailSender;
import com.shopmart.module.analytics.advanced.dto.GrowthTrendResponse;
import com.shopmart.module.analytics.advanced.dto.KpiResponse;
import com.shopmart.module.analytics.advanced.service.AdvancedAnalyticsService;
import com.shopmart.module.report.export.ReportExportService;
import com.shopmart.module.report.schedule.dto.ReportScheduleRequest;
import com.shopmart.module.report.schedule.dto.ReportScheduleResponse;
import com.shopmart.module.report.schedule.entity.ReportFrequency;
import com.shopmart.module.report.schedule.entity.ReportSchedule;
import com.shopmart.module.report.schedule.entity.ReportType;
import com.shopmart.module.report.schedule.repository.ReportScheduleRepository;
import com.shopmart.module.report.schedule.service.ReportScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportScheduleServiceImpl implements ReportScheduleService {

    private final ReportScheduleRepository repository;
    private final AdvancedAnalyticsService analyticsService;
    private final ReportExportService exportService;
    private final EmailSender emailSender;

    @Override
    @Transactional
    public ReportScheduleResponse create(ReportScheduleRequest request, Long adminId) {
        ReportSchedule s = new ReportSchedule();
        apply(s, request);
        s.setCreatedBy(adminId);
        s.setNextRunAt(nextRunFrom(Instant.now(), s.getFrequency()));
        return toResponse(repository.save(s));
    }

    @Override
    @Transactional
    public ReportScheduleResponse update(Long id, ReportScheduleRequest request) {
        ReportSchedule s = find(id);
        apply(s, request);
        // Recompute next run relative to now if the frequency changed; keep existing progress otherwise.
        s.setNextRunAt(nextRunFrom(Instant.now(), s.getFrequency()));
        return toResponse(repository.save(s));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        repository.delete(find(id));
    }

    @Override
    @Transactional(readOnly = true)
    public ReportScheduleResponse get(Long id) {
        return toResponse(find(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReportScheduleResponse> list(Pageable pageable) {
        Page<ReportSchedule> page = repository.findAll(pageable);
        return PageResponse.from(page.map(this::toResponse));
    }

    @Override
    @Transactional
    public ReportScheduleResponse runNow(Long id) {
        ReportSchedule s = find(id);
        runOne(s);
        return toResponse(s);
    }

    @Override
    @Transactional
    public void processDueSchedules() {
        List<ReportSchedule> due = repository.findByEnabledTrueAndNextRunAtLessThanEqual(Instant.now());
        for (ReportSchedule s : due) {
            try {
                runOne(s);
            } catch (Exception e) {
                log.error("[REPORT-SCHEDULE] failed to run schedule {}: {}", s.getId(), e.getMessage());
            }
        }
    }

    private void runOne(ReportSchedule s) {
        String subject = "[ShopMart] " + s.getName() + " report";
        String body = renderReport(s.getReportType());
        for (String email : s.getRecipientEmails().split(",")) {
            String trimmed = email.trim();
            if (!trimmed.isEmpty()) emailSender.send(trimmed, subject, body);
        }
        s.setLastRunAt(Instant.now());
        s.setNextRunAt(nextRunFrom(Instant.now(), s.getFrequency()));
        repository.save(s);
        log.info("[REPORT-SCHEDULE] sent schedule={} type={} recipients={}", s.getId(), s.getReportType(), s.getRecipientEmails());
    }

    /** Renders the report as a plain-text CSV body embedded directly in the email — the existing
     *  EmailSender abstraction (send(to, subject, body)) has no attachment support, and adding one
     *  would mean changing SmtpEmailSender's contract for every other caller too. A CSV table in
     *  the body is genuinely usable (copy-paste into a spreadsheet) without that wider change. */
    private String renderReport(ReportType type) {
        List<String> headers;
        List<Map<String, Object>> rows;
        if (type == ReportType.KPI_SUMMARY) {
            KpiResponse k = analyticsService.kpis();
            headers = List.of("Metric", "Value");
            rows = List.of(
                    metricRow("Total Revenue", k.totalRevenue()),
                    metricRow("Total Orders", k.totalOrders()),
                    metricRow("Total Customers", k.totalCustomers()),
                    metricRow("Average Order Value", k.averageOrderValue()),
                    metricRow("Repeat Customer Rate %", k.repeatCustomerRatePct())
            );
        } else {
            GrowthTrendResponse trend = analyticsService.growthTrend(6);
            headers = List.of("Month", "Revenue", "Revenue Growth %", "Orders", "New Customers");
            rows = trend.points().stream().map(p -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("Month", p.month());
                m.put("Revenue", p.revenue());
                m.put("Revenue Growth %", p.revenueGrowthPct());
                m.put("Orders", p.orders());
                m.put("New Customers", p.newCustomers());
                return m;
            }).toList();
        }
        return new String(exportService.toCsv(headers, rows), java.nio.charset.StandardCharsets.UTF_8);
    }

    private Map<String, Object> metricRow(String metric, Object value) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("Metric", metric);
        m.put("Value", value);
        return m;
    }

    private Instant nextRunFrom(Instant from, ReportFrequency frequency) {
        return switch (frequency) {
            case DAILY -> from.plus(1, java.time.temporal.ChronoUnit.DAYS);
            case WEEKLY -> from.plus(7, java.time.temporal.ChronoUnit.DAYS);
            case MONTHLY -> from.plus(30, java.time.temporal.ChronoUnit.DAYS);
        };
    }

    private void apply(ReportSchedule s, ReportScheduleRequest request) {
        s.setName(request.name());
        s.setReportType(request.reportType());
        s.setFrequency(request.frequency());
        s.setRecipientEmails(request.recipientEmails());
        s.setEnabled(request.enabled() == null || request.enabled());
    }

    private ReportSchedule find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ReportSchedule", "id", id));
    }

    private ReportScheduleResponse toResponse(ReportSchedule s) {
        return new ReportScheduleResponse(s.getId(), s.getName(), s.getReportType(), s.getFrequency(),
                s.getRecipientEmails(), s.isEnabled(), s.getNextRunAt(), s.getLastRunAt());
    }
}
