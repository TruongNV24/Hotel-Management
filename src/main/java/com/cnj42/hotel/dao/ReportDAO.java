package com.cnj42.hotel.dao;

import com.cnj42.hotel.model.ReportData;
import com.cnj42.hotel.utils.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportDAO {

    private static final String SUMMARY_SQL = "SELECT COALESCE(SUM(CASE WHEN status <> 'CANCELLED' THEN total_amount ELSE 0 END), 0), COUNT(CASE WHEN status <> 'CANCELLED' THEN 1 END), "
            + "COALESCE(SUM(CASE WHEN status = 'PAID' THEN total_amount ELSE 0 END), 0) "
            + "FROM invoices WHERE YEAR(issued_at) = ?";
    private static final String RESERVATION_SQL = "SELECT status, COUNT(*) FROM reservations GROUP BY status ORDER BY status";
    private static final String ROOM_STATUS_SQL = "SELECT status, COUNT(*) FROM rooms GROUP BY status ORDER BY status";
        private static final String DETAIL_SQL = "SELECT i.invoice_code, i.issued_at, s.actual_check_in, s.actual_check_out, "
            + "g.full_name, r.room_number, d.item_type, d.description, d.quantity, d.unit_price, d.amount, "
            + "i.total_amount, COALESCE(p.paid_amount, 0), p.payment_date, i.status "
            + "FROM invoices i JOIN stays s ON s.stay_id = i.stay_id "
            + "JOIN reservations res ON res.reservation_id = s.reservation_id "
            + "JOIN guests g ON g.guest_id = res.guest_id JOIN rooms r ON r.room_id = s.room_id "
            + "LEFT JOIN invoice_details d ON d.invoice_id = i.invoice_id "
            + "LEFT JOIN (SELECT invoice_id, SUM(amount) AS paid_amount, MAX(payment_date) AS payment_date "
            + "FROM payments GROUP BY invoice_id) p "
            + "ON p.invoice_id = i.invoice_id WHERE YEAR(i.issued_at) = ?";

    public ReportData getReportData(int year, Integer month) throws SQLException {
        ReportData report = new ReportData();
        report.setYear(year);
        report.setMonth(month);
        try (Connection connection = DBConnection.getConnection()) {
            loadSummary(connection, report, year, month);
            loadRevenueRows(connection, report, year, month);
            loadReservationRows(connection, report);
            loadRoomStatusRows(connection, report);
            loadInvoiceDetails(connection, report, year, month);
            loadServiceRows(connection, report, year, month);
            loadIncidentRows(connection, report, year, month);
            loadMaintenanceRows(connection, report, year, month);
            loadAuditRows(connection, report, year, month);
        }
        return report;
    }

    private void loadSummary(Connection connection, ReportData report, int year, Integer month) throws SQLException {
        String sql = SUMMARY_SQL + (month == null ? "" : " AND MONTH(issued_at) = ?");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, year);
            if (month != null) statement.setInt(2, month);
            try (ResultSet result = statement.executeQuery()) {
            if (result.next()) {
                report.setTotalRevenue(result.getBigDecimal(1));
                report.setInvoiceCount(result.getInt(2));
                report.setPaidRevenue(result.getBigDecimal(3));
            }
            }
        }
    }

    private void loadRevenueRows(Connection connection, ReportData report, int year, Integer month) throws SQLException {
        String group = month == null ? "MONTH(issued_at)" : "DAY(issued_at)";
        String sql = "SELECT " + group + ", COUNT(*), COALESCE(SUM(total_amount), 0) FROM invoices "
                + "WHERE YEAR(issued_at) = ?" + (month == null ? "" : " AND MONTH(issued_at) = ?")
            + " AND status <> 'CANCELLED'"
                + " GROUP BY " + group + " ORDER BY 1";
        Map<Integer, RevenueValue> revenueByPeriod = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, year);
            if (month != null) statement.setInt(2, month);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    revenueByPeriod.put(result.getInt(1), new RevenueValue(result.getInt(2), result.getBigDecimal(3)));
                }
            }
            }

        int lastPeriod = month == null ? 12 : YearMonth.of(year, month).lengthOfMonth();
        for (int period = 1; period <= lastPeriod; period++) {
            RevenueValue value = revenueByPeriod.getOrDefault(period, new RevenueValue(0, BigDecimal.ZERO));
            report.getRevenueRows().add(new ReportData.RevenueRow(
                    String.valueOf(period), value.invoiceCount, value.revenue));
        }
    }

    private void loadReservationRows(Connection connection, ReportData report) throws SQLException {
        List<ReservationCount> rows = new ArrayList<>();
        int total = 0;
        try (PreparedStatement statement = connection.prepareStatement(RESERVATION_SQL);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                String status = result.getString(1);
                int count = result.getInt(2);
                rows.add(new ReservationCount(status, count));
                total += count;
                if ("COMPLETED".equals(status)) {
                    report.setCompletedReservations(count);
                }
            }
        }
        for (ReservationCount row : rows) {
            double percentage = total == 0 ? 0 : row.count * 100.0 / total;
            report.getReservationStatusRows().add(
                    new ReportData.ReservationStatusRow(row.status, row.count, percentage));
        }
    }

    private void loadRoomStatusRows(Connection connection, ReportData report) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(ROOM_STATUS_SQL);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                report.getRoomStatusRows().add(new ReportData.RoomStatusRow(result.getString(1), result.getInt(2)));
            }
        }
    }

    private void loadInvoiceDetails(Connection connection, ReportData report, int year, Integer month) throws SQLException {
        String sql = DETAIL_SQL + " AND i.status <> 'CANCELLED'" + (month == null ? "" : " AND MONTH(i.issued_at) = ?")
                + " ORDER BY i.issued_at, i.invoice_id, d.invoice_detail_id";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, year);
            if (month != null) statement.setInt(2, month);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                        java.sql.Timestamp paymentDate = result.getTimestamp(14);
                        report.getInvoiceDetailRows().add(new ReportData.InvoiceDetailRow(
                            result.getString(1), String.valueOf(result.getTimestamp(2)),
                            String.valueOf(result.getTimestamp(3)), String.valueOf(result.getTimestamp(4)),
                            result.getString(5), result.getString(6), result.getString(7), result.getString(8),
                            result.getInt(9), result.getBigDecimal(10), result.getBigDecimal(11),
                            result.getBigDecimal(12), result.getBigDecimal(13),
                            paymentDate == null ? "" : paymentDate.toString(), result.getString(15)));
                }
            }
        }
    }

    private void loadServiceRows(Connection connection, ReportData report, int year, Integer month) throws SQLException {
        String sql = "SELECT s.service_name, COUNT(*), COALESCE(SUM(su.total_amount), 0) "
                + "FROM service_usages su JOIN services s ON s.service_id = su.service_id "
                + "WHERE YEAR(su.used_at) = ?" + (month == null ? "" : " AND MONTH(su.used_at) = ?")
                + " GROUP BY s.service_id, s.service_name ORDER BY SUM(su.total_amount) DESC";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, year);
            if (month != null) statement.setInt(2, month);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    report.getServiceRows().add(new ReportData.ServiceRow(result.getString(1), result.getInt(2), result.getBigDecimal(3)));
                    report.setServiceUsageCount(report.getServiceUsageCount() + result.getInt(2));
                    report.setServiceRevenue(report.getServiceRevenue().add(result.getBigDecimal(3)));
                }
            }
        }
    }

    private void loadIncidentRows(Connection connection, ReportData report, int year, Integer month) throws SQLException {
        String sql = "SELECT status, COUNT(*) FROM incidents WHERE YEAR(reported_at) = ?"
                + (month == null ? "" : " AND MONTH(reported_at) = ?") + " GROUP BY status ORDER BY status";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, year);
            if (month != null) statement.setInt(2, month);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    report.getIncidentRows().add(new ReportData.CountRow(result.getString(1), result.getInt(2)));
                    report.setIncidentCount(report.getIncidentCount() + result.getInt(2));
                }
            }
        }
    }

    private void loadMaintenanceRows(Connection connection, ReportData report, int year, Integer month) throws SQLException {
        String sql = "SELECT status, COUNT(*) FROM maintenance_requests WHERE YEAR(created_at) = ?"
                + (month == null ? "" : " AND MONTH(created_at) = ?") + " GROUP BY status ORDER BY status";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, year);
            if (month != null) statement.setInt(2, month);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    report.getMaintenanceRows().add(new ReportData.CountRow(result.getString(1), result.getInt(2)));
                    report.setMaintenanceCount(report.getMaintenanceCount() + result.getInt(2));
                }
            }
        }

        String durationSql = "SELECT COALESCE(SUM(duration_minutes), 0), COALESCE(AVG(duration_minutes), 0) "
                + "FROM maintenance_requests WHERE status = 'COMPLETED' AND YEAR(created_at) = ?"
                + (month == null ? "" : " AND MONTH(created_at) = ?");
        try (PreparedStatement statement = connection.prepareStatement(durationSql)) {
            statement.setInt(1, year);
            if (month != null) statement.setInt(2, month);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    report.setTotalMaintenanceMinutes(result.getInt(1));
                    report.setAverageMaintenanceMinutes(result.getDouble(2));
                }
            }
        }

        String overdueSql = "SELECT COUNT(*) FROM maintenance_requests WHERE status = 'IN_PROGRESS' "
                + "AND expected_end_at IS NOT NULL AND CURRENT_TIMESTAMP > expected_end_at";
        try (PreparedStatement statement = connection.prepareStatement(overdueSql);
             ResultSet result = statement.executeQuery()) {
            if (result.next()) report.setOverdueMaintenanceCount(result.getInt(1));
        }
    }

    private void loadAuditRows(Connection connection, ReportData report, int year, Integer month) throws SQLException {
        String sql = "SELECT action, COUNT(*) FROM audit_logs WHERE YEAR(created_at) = ?"
                + (month == null ? "" : " AND MONTH(created_at) = ?") + " GROUP BY action ORDER BY COUNT(*) DESC, action";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, year);
            if (month != null) statement.setInt(2, month);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) report.getAuditRows().add(new ReportData.CountRow(result.getString(1), result.getInt(2)));
            }
        }
    }

    private static class ReservationCount {
        private final String status;
        private final int count;

        private ReservationCount(String status, int count) {
            this.status = status;
            this.count = count;
        }
    }

    private static class RevenueValue {
        private final int invoiceCount;
        private final BigDecimal revenue;

        private RevenueValue(int invoiceCount, BigDecimal revenue) {
            this.invoiceCount = invoiceCount;
            this.revenue = revenue;
        }
    }
}