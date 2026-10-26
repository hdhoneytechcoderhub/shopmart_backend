package com.shopmart.module.report.export;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

/**
 * Generic tabular exporter shared by every "export as CSV/Excel/PDF" endpoint, so each report
 * only needs to produce a List<Map<String,Object>> of rows instead of three separate rendering
 * implementations per report type.
 */
@Service
public class ReportExportServiceImpl implements ReportExportService {

    @Override
    public byte[] toCsv(List<String> headers, List<Map<String, Object>> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", headers.stream().map(this::csvEscape).toList())).append("\n");
        for (Map<String, Object> row : rows) {
            sb.append(String.join(",", headers.stream()
                    .map(h -> csvEscape(String.valueOf(row.getOrDefault(h, "")))).toList())).append("\n");
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private String csvEscape(String value) {
        if (value == null) return "";
        boolean needsQuoting = value.contains(",") || value.contains("\"") || value.contains("\n");
        String escaped = value.replace("\"", "\"\"");
        return needsQuoting ? "\"" + escaped + "\"" : escaped;
    }

    @Override
    public byte[] toExcel(String sheetName, List<String> headers, List<Map<String, Object>> rows) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName);
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                headerRow.createCell(i).setCellValue(headers.get(i));
            }
            int rowIdx = 1;
            for (Map<String, Object> row : rows) {
                Row excelRow = sheet.createRow(rowIdx++);
                for (int i = 0; i < headers.size(); i++) {
                    Cell cell = excelRow.createCell(i);
                    Object value = row.get(headers.get(i));
                    if (value instanceof Number n) {
                        cell.setCellValue(n.doubleValue());
                    } else {
                        cell.setCellValue(value != null ? value.toString() : "");
                    }
                }
            }
            for (int i = 0; i < headers.size(); i++) sheet.autoSizeColumn(i);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Excel export failed", e);
        }
    }

    @Override
    public byte[] toPdf(String title, List<String> headers, List<Map<String, Object>> rows) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
            Paragraph titlePara = new Paragraph(title, titleFont);
            titlePara.setAlignment(Element.ALIGN_CENTER);
            titlePara.setSpacingAfter(12);
            document.add(titlePara);

            PdfPTable table = new PdfPTable(headers.size());
            table.setWidthPercentage(100);
            Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD);
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Paragraph(h, headerFont));
                cell.setBackgroundColor(new java.awt.Color(230, 230, 230));
                table.addCell(cell);
            }
            Font bodyFont = new Font(Font.HELVETICA, 9);
            for (Map<String, Object> row : rows) {
                for (String h : headers) {
                    Object value = row.get(h);
                    table.addCell(new Paragraph(value != null ? value.toString() : "", bodyFont));
                }
            }
            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("PDF export failed", e);
        }
    }
}
