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
    private final AuditLogService auditLogService = new AuditLogService();

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
            boolean ok = stayDAO.checkInReservation(reservationId, roomId, currentUserId);
            if (ok) {
                auditLogService.logEvent("STAY_CHECKIN_SUCCESS", "STAY", "STAY", reservationId, null, "Checked in reservation " + reservationId + " into room " + roomId, "127.0.0.1", "SUCCESS");
            } else {
                auditLogService.logEvent("STAY_CHECKIN_FAILED", "STAY", "STAY", reservationId, null, "Failed to check in reservation " + reservationId + " into room " + roomId, "127.0.0.1", "FAILED");
            }
            return ok;
        } catch (SQLException e) {
            System.err.println("Lỗi check-in lưu trú: " + e.getMessage());
            auditLogService.logEvent("STAY_CHECKIN_FAILED", "STAY", "STAY", reservationId, null, "Exception checking in reservation " + reservationId + ": " + e.getMessage(), "127.0.0.1", "FAILED");
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
            boolean ok = stayDAO.checkoutAndCreateInvoice(stayId, reservationId, roomId, paymentMethod, currentUserId);
            if (ok) {
                auditLogService.logEvent("STAY_CHECKOUT_SUCCESS", "STAY", "STAY", reservationId, null, "Checked out stay " + stayId + " and created invoice for room " + roomId, "127.0.0.1", "SUCCESS");
            } else {
                auditLogService.logEvent("STAY_CHECKOUT_FAILED", "STAY", "STAY", reservationId, null, "Failed checkout for stay " + stayId + " room " + roomId, "127.0.0.1", "FAILED");
            }
            return ok;
        } catch (SQLException e) {
            System.err.println("Lỗi checkout lưu trú: " + e.getMessage());
            auditLogService.logEvent("STAY_CHECKOUT_FAILED", "STAY", "STAY", reservationId, null, "Exception during checkout for stay " + stayId + ": " + e.getMessage(), "127.0.0.1", "FAILED");
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
