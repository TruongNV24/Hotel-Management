package com.cnj42.hotel.service;

import com.cnj42.hotel.dao.ReportDAO;
import com.cnj42.hotel.model.ReportData;
import com.cnj42.hotel.model.User;
import com.cnj42.hotel.service.PermissionService;

import java.sql.SQLException;
import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ReportService {

    private final ReportDAO reportDAO = new ReportDAO();
    private final AuditLogService auditLogService = new AuditLogService();

    public ReportData getReportData(int year, Integer month) {
        return getReportData(year, month, null);
    }

    public ReportData getReportData(int year, Integer month, User actor) {
        if (actor == null || !PermissionService.canAccessReports(actor)) {
            ReportData denied = new ReportData();
            denied.setErrorMessage("Bạn không có quyền xem báo cáo.");
            auditLogService.logEvent("REPORT_ACCESS_DENIED", "REPORT", "REPORT", null, actor,
                    "Report access denied for " + year + (month == null ? "" : "-" + month),
                    "127.0.0.1", "FAILED");
            return denied;
        }
        try {
            ReportData report = reportDAO.getReportData(year, month);
            auditLogService.logEvent("REPORT_VIEWED", "REPORT", "REPORT", null, actor,
                    "Viewed report for " + year + (month == null ? "" : "-" + month),
                    "127.0.0.1", "SUCCESS");
            return report;
        } catch (SQLException exception) {
            ReportData report = new ReportData();
            report.setErrorMessage(exception.getMessage());
            auditLogService.logEvent("REPORT_VIEW_FAILED", "REPORT", "REPORT", null, actor,
                    "Failed to load report: " + exception.getMessage(), "127.0.0.1", "FAILED");
            return report;
        }
    }

    public void exportToExcel(ReportData report, File file) throws Exception {
        exportToExcel(report, file, null);
    }

    public void exportToExcel(ReportData report, File file, User actor) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); FileOutputStream output = new FileOutputStream(file)) {
            writeSummarySheet(workbook, report);
            if (report.getMonth() == null) {
                writeYearRevenueSheet(workbook, report);
            } else {
                writeMonthRevenueSheet(workbook, report);
            }

            Sheet rooms = workbook.createSheet("Tình trạng phòng");
            rooms.createRow(0).createCell(0).setCellValue("Trạng thái");
            rooms.getRow(0).createCell(1).setCellValue("Số phòng");
            int rowIndex = 1;
            for (ReportData.RoomStatusRow item : report.getRoomStatusRows()) {
                Row row = rooms.createRow(rowIndex++);
                row.createCell(0).setCellValue(item.getStatus());
                row.createCell(1).setCellValue(item.getCount());
            }
            String reportSheetName = report.getMonth() == null ? "Doanh thu theo tháng" : "Doanh thu theo ngày";
            workbook.setSheetOrder(reportSheetName, 0);
            for (Sheet sheet : workbook) {
                for (int column = 0; column < Math.min(sheet.getRow(0).getLastCellNum(), 15); column++) {
                    sheet.autoSizeColumn(column);
                }
            }
            workbook.write(output);
            auditLogService.logEvent("REPORT_EXPORTED", "REPORT", "REPORT", null, actor,
                    "Exported report to " + file.getName(), "127.0.0.1", "SUCCESS");
        }
    }

    private void writeSummarySheet(Workbook workbook, ReportData report) {
        Sheet summary = workbook.createSheet("Tổng quan");
        summary.createRow(0).createCell(0).setCellValue("BÁO CÁO DOANH THU");
        summary.createRow(1).createCell(0).setCellValue("Năm");
        summary.getRow(1).createCell(1).setCellValue(report.getYear());
        summary.createRow(2).createCell(0).setCellValue("Tháng");
        summary.getRow(2).createCell(1).setCellValue(report.getMonth() == null ? "Cả năm" : String.valueOf(report.getMonth()));
        summary.createRow(3).createCell(0).setCellValue("Tổng doanh thu");
        summary.getRow(3).createCell(1).setCellValue(moneyValue(report.getTotalRevenue()));
        summary.createRow(4).createCell(0).setCellValue("Đã thanh toán");
        summary.getRow(4).createCell(1).setCellValue(moneyValue(report.getPaidRevenue()));
        summary.createRow(5).createCell(0).setCellValue("Tổng số hóa đơn");
        summary.getRow(5).createCell(1).setCellValue(report.getInvoiceCount());
    }

    private void writeMonthRevenueSheet(Workbook workbook, ReportData report) {
        Sheet sheet = workbook.createSheet("Doanh thu theo ngày");
        Map<String, ReportData.RevenueRow> revenueByDay = new LinkedHashMap<>();
        for (ReportData.RevenueRow row : report.getRevenueRows()) {
            if (row.getRevenue() != null && row.getRevenue().compareTo(BigDecimal.ZERO) > 0) {
                revenueByDay.put(row.getMonth(), row);
            }
        }
        Map<String, List<ReportData.InvoiceDetailRow>> detailsByDay = detailsByDay(report.getInvoiceDetailRows());
        int rowIndex = 0;
        for (Map.Entry<String, ReportData.RevenueRow> entry : revenueByDay.entrySet()) {
            ReportData.RevenueRow dailyTotal = entry.getValue();
            Row dayTitle = sheet.createRow(rowIndex++);
            dayTitle.createCell(0).setCellValue("NGÀY " + entry.getKey());
            dayTitle.createCell(1).setCellValue("Tổng doanh thu ngày");
            dayTitle.createCell(2).setCellValue(moneyValue(dailyTotal.getRevenue()));
            Row headers = sheet.createRow(rowIndex++);
            writeDetailHeaders(headers);
            List<ReportData.InvoiceDetailRow> rows = detailsByDay.getOrDefault(entry.getKey(), new ArrayList<>());
            for (ReportData.InvoiceDetailRow detail : rows) {
                writeDetailRow(sheet.createRow(rowIndex++), detail);
            }
            rowIndex++;
        }
        Row total = sheet.createRow(rowIndex);
        total.createCell(0).setCellValue("TỔNG DOANH THU THÁNG");
        total.createCell(2).setCellValue(moneyValue(report.getTotalRevenue()));
    }

    private void writeYearRevenueSheet(Workbook workbook, ReportData report) {
        Sheet sheet = workbook.createSheet("Doanh thu theo tháng");
        Row headers = sheet.createRow(0);
        headers.createCell(0).setCellValue("Tháng có doanh thu");
        headers.createCell(1).setCellValue("Số hóa đơn");
        headers.createCell(2).setCellValue("Tổng doanh thu tháng");
        int rowIndex = 1;
        for (ReportData.RevenueRow row : report.getRevenueRows()) {
            if (row.getRevenue() != null && row.getRevenue().compareTo(BigDecimal.ZERO) > 0) {
                Row month = sheet.createRow(rowIndex++);
                month.createCell(0).setCellValue("Tháng " + row.getMonth());
                month.createCell(1).setCellValue(row.getInvoiceCount());
                month.createCell(2).setCellValue(moneyValue(row.getRevenue()));
            }
        }
        Row total = sheet.createRow(rowIndex);
        total.createCell(0).setCellValue("TỔNG DOANH THU CẢ NĂM");
        total.createCell(2).setCellValue(moneyValue(report.getTotalRevenue()));
    }

    private Map<String, List<ReportData.InvoiceDetailRow>> detailsByDay(List<ReportData.InvoiceDetailRow> details) {
        Map<String, List<ReportData.InvoiceDetailRow>> grouped = new LinkedHashMap<>();
        for (ReportData.InvoiceDetailRow detail : details) {
            String issuedAt = textValue(detail.getIssuedAt());
            String day = issuedAt.length() >= 10
                    ? String.valueOf(Integer.parseInt(issuedAt.substring(8, 10)))
                    : issuedAt;
            grouped.computeIfAbsent(day, ignored -> new ArrayList<>()).add(detail);
        }
        return grouped;
    }

    private void writeDetailHeaders(Row row) {
        String[] headers = {"Mã HĐ", "Ngày lập HĐ", "Nhận phòng", "Trả phòng", "Khách hàng", "Phòng",
                    "Loại", "Nội dung", "Số lượng", "Đơn giá", "Thành tiền", "Tổng hóa đơn", "Đã thanh toán", "Ngày thanh toán", "Trạng thái"};
        for (int i = 0; i < headers.length; i++) row.createCell(i).setCellValue(headers[i]);
    }

    private void writeDetailRow(Row row, ReportData.InvoiceDetailRow item) {
        row.createCell(0).setCellValue(textValue(item.getInvoiceCode()));
        row.createCell(1).setCellValue(textValue(item.getIssuedAt()));
        row.createCell(2).setCellValue(textValue(item.getActualCheckIn()));
        row.createCell(3).setCellValue(textValue(item.getActualCheckOut()));
        row.createCell(4).setCellValue(textValue(item.getGuestName()));
        row.createCell(5).setCellValue(textValue(item.getRoomNumber()));
        row.createCell(6).setCellValue(textValue(item.getItemType()));
        row.createCell(7).setCellValue(textValue(item.getDescription()));
        row.createCell(8).setCellValue(item.getQuantity());
        row.createCell(9).setCellValue(moneyValue(item.getUnitPrice()));
        row.createCell(10).setCellValue(moneyValue(item.getAmount()));
        row.createCell(11).setCellValue(moneyValue(item.getInvoiceTotal()));
        row.createCell(12).setCellValue(moneyValue(item.getPaidAmount()));
        row.createCell(13).setCellValue(textValue(item.getPaymentDate()));
        row.createCell(14).setCellValue(textValue(item.getStatus()));
    }

    private static double moneyValue(java.math.BigDecimal value) {
        return value == null ? 0.0 : value.doubleValue();
    }

    private static String textValue(String value) {
        return value == null ? "" : value;
    }
}