package com.cnj42.hotel.service;

import com.cnj42.hotel.dao.DashboardDAO;
import com.cnj42.hotel.model.DashboardData;
import com.cnj42.hotel.model.Reservation;
import com.cnj42.hotel.model.User;

import java.sql.SQLException;
import java.util.List;

public class DashboardService {

    private final DashboardDAO dashboardDAO;
    private final AuditLogService auditLogService = new AuditLogService();

    public DashboardService() {
        this.dashboardDAO = new DashboardDAO();
    }

    public DashboardData getDashboardData() {
        return getDashboardData(null);
    }

    public DashboardData getDashboardData(User actor) {
        try {
            DashboardData data = dashboardDAO.getDashboardData();
            if (actor == null || !PermissionService.canAccessReports(actor)) {
                data.setRevenueTrend(new int[]{0, 0, 0, 0, 0, 0});
                data.setMonthlyRevenue(0);
                data.setPreviousMonthRevenue(0);
            }
            auditLogService.logEvent("DASHBOARD_VIEWED", "DASHBOARD", "DASHBOARD", null, actor,
                    "Dashboard data loaded", "127.0.0.1", "SUCCESS");
            return data;
        } catch (SQLException e) {
            System.err.println("Lỗi khi tải dữ liệu dashboard: " + e.getMessage());
            auditLogService.logEvent("DASHBOARD_VIEW_FAILED", "DASHBOARD", "DASHBOARD", null, actor,
                    "Failed to load dashboard: " + e.getMessage(), "127.0.0.1", "FAILED");
            return new DashboardData();
        }
    }

    public List<Reservation> getRecentReservations() {
        try {
            return dashboardDAO.getRecentReservations();
        } catch (SQLException e) {
            System.err.println("Lỗi khi tải đặt phòng gần đây: " + e.getMessage());
            return List.of();
        }
    }
}
