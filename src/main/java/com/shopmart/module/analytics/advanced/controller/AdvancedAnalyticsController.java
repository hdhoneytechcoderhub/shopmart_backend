package com.shopmart.module.analytics.advanced.controller;

import com.shopmart.common.dto.ApiResponse;
import com.shopmart.module.analytics.advanced.dto.*;
import com.shopmart.module.analytics.advanced.service.AdvancedAnalyticsService;
import com.shopmart.module.report.export.ReportExportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/analytics/advanced")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Advanced Analytics")
public class AdvancedAnalyticsController {

    private final AdvancedAnalyticsService service;
    private final ReportExportService exportService;

    @GetMapping("/revenue-forecast")
    public ApiResponse<RevenueForecastResponse> revenueForecast(
            @RequestParam(defaultValue = "6") int historyMonths,
            @RequestParam(defaultValue = "3") int forecastMonths) {
        return ApiResponse.ok(service.revenueForecast(historyMonths, forecastMonths));
    }

    @GetMapping("/cohorts")
    public ApiResponse<CohortResponse> cohorts(
            @RequestParam(defaultValue = "6") int cohortMonths,
            @RequestParam(defaultValue = "3") int trackMonths) {
        return ApiResponse.ok(service.customerCohorts(cohortMonths, trackMonths));
    }

    @GetMapping("/retention")
    public ApiResponse<RetentionResponse> retention(
            @RequestParam(defaultValue = "6") int cohortMonths,
            @RequestParam(defaultValue = "3") int trackMonths) {
        return ApiResponse.ok(service.retentionAnalysis(cohortMonths, trackMonths));
    }

    @GetMapping("/growth-trend")
    public ApiResponse<GrowthTrendResponse> growthTrend(@RequestParam(defaultValue = "6") int months) {
        return ApiResponse.ok(service.growthTrend(months));
    }

    @GetMapping("/kpis")
    public ApiResponse<KpiResponse> kpis() {
        return ApiResponse.ok(service.kpis());
    }

    // ---------------- Export (CSV / Excel / PDF) ----------------
    // report = growth-trend | kpis   (the two genuinely tabular reports; cohort/retention are
    // matrices better consumed as JSON by the frontend chart than as a flat export table)

    @GetMapping("/export/{format}")
    public ResponseEntity<byte[]> export(@PathVariable String format,
                                         @RequestParam(defaultValue = "growth-trend") String report,
                                         @RequestParam(defaultValue = "6") int months) {
        List<String> headers;
        List<Map<String, Object>> rows;
        String filenameBase;

        if ("kpis".equalsIgnoreCase(report)) {
            KpiResponse k = service.kpis();
            headers = List.of("Metric", "Value");
            rows = List.of(
                    row("Metric", "Total Revenue", "Value", k.totalRevenue()),
                    row("Metric", "Total Orders", "Value", k.totalOrders()),
                    row("Metric", "Total Customers", "Value", k.totalCustomers()),
                    row("Metric", "Average Order Value", "Value", k.averageOrderValue()),
                    row("Metric", "Repeat Customer Rate %", "Value", k.repeatCustomerRatePct()),
                    row("Metric", "Last Month Revenue Growth %", "Value", k.lastMonthRevenueGrowthPct()),
                    row("Metric", "Last Month Order Growth %", "Value", k.lastMonthOrderGrowthPct())
            );
            filenameBase = "kpi-report";
        } else {
            GrowthTrendResponse trend = service.growthTrend(months);
            headers = List.of("Month", "Revenue", "Revenue Growth %", "Orders", "Order Growth %", "New Customers", "Customer Growth %");
            rows = trend.points().stream().map(p -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("Month", p.month());
                m.put("Revenue", p.revenue());
                m.put("Revenue Growth %", p.revenueGrowthPct());
                m.put("Orders", p.orders());
                m.put("Order Growth %", p.orderGrowthPct());
                m.put("New Customers", p.newCustomers());
                m.put("Customer Growth %", p.customerGrowthPct());
                return m;
            }).toList();
            filenameBase = "growth-trend";
        }

        byte[] content;
        MediaType mediaType;
        String extension;
        switch (format.toLowerCase()) {
            case "csv" -> {
                content = exportService.toCsv(headers, rows);
                mediaType = MediaType.parseMediaType("text/csv");
                extension = "csv";
            }
            case "excel", "xlsx" -> {
                content = exportService.toExcel(filenameBase, headers, rows);
                mediaType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                extension = "xlsx";
            }
            case "pdf" -> {
                content = exportService.toPdf(filenameBase.replace('-', ' '), headers, rows);
                mediaType = MediaType.APPLICATION_PDF;
                extension = "pdf";
            }
            default -> throw new com.shopmart.common.exception.BadRequestException(
                    "Unsupported export format: " + format + " (use csv, excel, or pdf)");
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filenameBase + "." + extension + "\"")
                .contentType(mediaType)
                .body(content);
    }

    private Map<String, Object> row(String k1, Object v1, String k2, Object v2) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put(k1, v1);
        m.put(k2, v2);
        return m;
    }
}
