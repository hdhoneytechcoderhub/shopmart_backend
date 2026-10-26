package com.shopmart.module.report.export;

import java.util.List;
import java.util.Map;

public interface ReportExportService {
    byte[] toCsv(List<String> headers, List<Map<String, Object>> rows);
    byte[] toExcel(String sheetName, List<String> headers, List<Map<String, Object>> rows);
    byte[] toPdf(String title, List<String> headers, List<Map<String, Object>> rows);
}
