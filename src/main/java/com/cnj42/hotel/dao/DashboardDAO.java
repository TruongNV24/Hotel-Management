package com.cnj42.hotel.dao;

import com.cnj42.hotel.model.DashboardData;
import com.cnj42.hotel.utils.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DashboardDAO {

    public DashboardData getDashboardData() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            DashboardData data = new DashboardData();

            data.setTotalRooms(executeCount(conn, "SELECT COUNT(*) FROM rooms"));
            data.setAvailableRooms(executeCount(conn, "SELECT COUNT(*) FROM rooms WHERE status = 'AVAILABLE'"));
            data.setOccupiedRooms(executeCount(conn, "SELECT COUNT(*) FROM rooms WHERE status = 'OCCUPIED'"));
            data.setActiveReservations(executeCount(conn, "SELECT COUNT(*) FROM reservations WHERE status IN ('PENDING', 'CONFIRMED')"));
            data.setRoomStatusSummary(getRoomStatusSummary(conn));
            data.setRevenueTrend(getRevenueTrend(conn));
            data.setMonthlyRevenue(getMonthlyRevenue(conn));
            data.setPreviousMonthRevenue(getPreviousMonthRevenue(conn));

            return data;
        }
    }

    private int executeCount(Connection conn, String sql) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private int[] getRoomStatusSummary(Connection conn) throws SQLException {
        String sql = "SELECT status, COUNT(*) AS total FROM rooms GROUP BY status";
        int[] values = new int[]{0, 0, 0, 0, 0};

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String status = rs.getString("status");
                int count = rs.getInt("total");

                switch (status == null ? "" : status.toUpperCase()) {
                    case "AVAILABLE" -> values[0] = count;
                    case "OCCUPIED" -> values[1] = count;
                    case "BOOKED" -> values[2] = count;
                    case "RESERVED" -> values[2] = count;
                    case "MAINTENANCE" -> values[3] = count;
                    case "CLEANING" -> values[4] = count;
                    default -> {
                    }
                }
            }
        }

        return values;
    }

    private int[] getRevenueTrend(Connection conn) throws SQLException {
        String sql = "SELECT DATE_FORMAT(issued_at, '%Y-%m') AS month_key, COALESCE(SUM(total_amount), 0) AS total " +
                "FROM invoices " +
                "WHERE issued_at >= DATE_SUB(CURRENT_DATE, INTERVAL 5 MONTH) " +
                "GROUP BY DATE_FORMAT(issued_at, '%Y-%m') " +
                "ORDER BY month_key ASC";

        int[] values = new int[6];
        Map<String, Integer> monthMap = new HashMap<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                monthMap.put(rs.getString("month_key"), rs.getInt("total"));
            }
        }

        Calendar cal = Calendar.getInstance();
        String[] labels = new String[6];
        for (int i = 5; i >= 0; i--) {
            cal.add(Calendar.MONTH, -1);
            labels[5 - i] = String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1);
        }

        for (int i = 0; i < labels.length; i++) {
            values[i] = monthMap.getOrDefault(labels[i], 0);
        }

        return values;
    }

    private long getMonthlyRevenue(Connection conn) throws SQLException {
        String sql = "SELECT COALESCE(SUM(total_amount), 0) FROM invoices " +
                "WHERE MONTH(issued_at) = MONTH(CURRENT_DATE) AND YEAR(issued_at) = YEAR(CURRENT_DATE)";

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }

    private long getPreviousMonthRevenue(Connection conn) throws SQLException {
        String sql = "SELECT COALESCE(SUM(total_amount), 0) FROM invoices " +
                "WHERE MONTH(issued_at) = MONTH(DATE_SUB(CURRENT_DATE, INTERVAL 1 MONTH)) " +
                "AND YEAR(issued_at) = YEAR(DATE_SUB(CURRENT_DATE, INTERVAL 1 MONTH))";

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }
}
