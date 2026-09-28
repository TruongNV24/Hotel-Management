package com.cnj42.hotel.service;

import com.cnj42.hotel.dao.DashboardDAO;
import com.cnj42.hotel.model.DashboardData;
import com.cnj42.hotel.model.User;

import java.sql.SQLException;

public class DashboardService {

    private final DashboardDAO dashboardDAO;

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
            return data;
        } catch (SQLException e) {
            System.err.println("Lỗi khi tải dữ liệu dashboard: " + e.getMessage());
            return new DashboardData();
        }
    }
}
