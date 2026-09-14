package com.cnj42.hotel.service;

import com.cnj42.hotel.dao.StayDAO;
import com.cnj42.hotel.model.CheckoutSummary;
import com.cnj42.hotel.model.Stay;
import com.cnj42.hotel.model.StayDetail;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StayService {

    private final StayDAO stayDAO = new StayDAO();

    public Map<String, Integer> getStayStats() {
        try {
            return stayDAO.getStayStats();
        } catch (SQLException e) {
            System.err.println("Lỗi tải thống kê lưu trú: " + e.getMessage());
            Map<String, Integer> fallback = new HashMap<>();
            fallback.put("waiting_checkin", 0);
            fallback.put("in_house", 0);
            fallback.put("waiting_checkout", 0);
            fallback.put("checked_out", 0);
            return fallback;
        }
    }

    public List<Stay> searchStays(String keyword, String statusCode, LocalDate fromDate, LocalDate toDate) {
        try {
            return stayDAO.searchStays(keyword, statusCode, fromDate, toDate);
        } catch (SQLException e) {
            System.err.println("Lỗi tải danh sách lưu trú: " + e.getMessage());
            return List.of();
        }
    }

    public boolean checkInReservation(int reservationId, int roomId, Integer currentUserId) {
        try {
            return stayDAO.checkInReservation(reservationId, roomId, currentUserId);
        } catch (SQLException e) {
            System.err.println("Lỗi check-in lưu trú: " + e.getMessage());
            return false;
        }
    }

    public CheckoutSummary getCheckoutSummary(int stayId, int roomId) {
        try {
            return stayDAO.getCheckoutSummary(stayId, roomId);
        } catch (SQLException e) {
            System.err.println("Lỗi tính toán checkout: " + e.getMessage());
            return null;
        }
    }

    public boolean checkoutAndCreateInvoice(int stayId, int reservationId, int roomId, String paymentMethod, Integer currentUserId) {
        try {
            return stayDAO.checkoutAndCreateInvoice(stayId, reservationId, roomId, paymentMethod, currentUserId);
        } catch (SQLException e) {
            System.err.println("Lỗi checkout lưu trú: " + e.getMessage());
            return false;
        }
    }

    public StayDetail getStayDetail(int reservationId) {
        try {
            return stayDAO.getStayDetail(reservationId);
        } catch (SQLException e) {
            System.err.println("Lỗi lấy chi tiết lưu trú: " + e.getMessage());
            return null;
        }
    }
}
