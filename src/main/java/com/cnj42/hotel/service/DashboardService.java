package com.cnj42.hotel.service;

import com.cnj42.hotel.dao.DashboardDAO;
import com.cnj42.hotel.model.DashboardData;

import java.sql.SQLException;

public class DashboardService {

    private final DashboardDAO dashboardDAO;

    public DashboardService() {
        this.dashboardDAO = new DashboardDAO();
    }

    public DashboardData getDashboardData() {
        try {
            return dashboardDAO.getDashboardData();
        } catch (SQLException e) {
            System.err.println("Lỗi khi tải dữ liệu dashboard: " + e.getMessage());
            return new DashboardData();
        }
    }
}
