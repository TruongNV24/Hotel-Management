package com.cnj42.hotel.service;

import com.cnj42.hotel.dao.GuestDAO;
import com.cnj42.hotel.model.Guest;
import com.cnj42.hotel.model.User;

import java.sql.SQLException;
import java.util.List;

public class GuestService {
    private final GuestDAO guestDAO = new GuestDAO();
    private final AuditLogService auditLogService = new AuditLogService();

    public List<Guest> search(String keyword) {
        try {
            return guestDAO.search(keyword);
        } catch (SQLException e) {
            System.err.println("Lỗi tải danh sách khách hàng: " + e.getMessage());
            return List.of();
        }
    }

    public List<Guest> findAll() {
        try {
            return guestDAO.findAll();
        } catch (SQLException e) {
            System.err.println("Lỗi tải khách hàng: " + e.getMessage());
            return List.of();
        }
    }

    public Guest findById(int guestId) {
        try {
            return guestDAO.findById(guestId);
        } catch (SQLException e) {
            System.err.println("Lỗi tải thông tin khách hàng: " + e.getMessage());
            return null;
        }
    }

    public int create(Guest guest, User actor) {
        try {
            int guestId = guestDAO.create(guest);
            auditLogService.logEvent(guestId > 0 ? "GUEST_CREATED" : "GUEST_CREATE_FAILED", "GUEST", "GUEST",
                    guestId > 0 ? guestId : null, actor, "Created guest " + guest.getFullName(),
                    "127.0.0.1", guestId > 0 ? "SUCCESS" : "FAILED");
            return guestId;
        } catch (SQLException e) {
            auditLogService.logEvent("GUEST_CREATE_FAILED", "GUEST", "GUEST", null, actor,
                    "Failed to create guest: " + e.getMessage(), "127.0.0.1", "FAILED");
            System.err.println("Lỗi tạo khách hàng: " + e.getMessage());
            return -1;
        }
    }

    public boolean update(Guest guest, User actor) {
        try {
            boolean updated = guestDAO.update(guest);
            auditLogService.logEvent(updated ? "GUEST_UPDATED" : "GUEST_UPDATE_FAILED", "GUEST", "GUEST",
                    guest.getGuestId(), actor, "Updated guest " + guest.getFullName(),
                    "127.0.0.1", updated ? "SUCCESS" : "FAILED");
            return updated;
        } catch (SQLException e) {
            auditLogService.logEvent("GUEST_UPDATE_FAILED", "GUEST", "GUEST", guest.getGuestId(), actor,
                    "Failed to update guest: " + e.getMessage(), "127.0.0.1", "FAILED");
            System.err.println("Lỗi cập nhật khách hàng: " + e.getMessage());
            return false;
        }
    }

    public boolean delete(int guestId, User actor) {
        try {
            boolean deleted = guestDAO.delete(guestId);
            auditLogService.logEvent(deleted ? "GUEST_DELETED" : "GUEST_DELETE_FAILED", "GUEST", "GUEST",
                    guestId, actor, "Deleted guest " + guestId, "127.0.0.1", deleted ? "SUCCESS" : "FAILED");
            return deleted;
        } catch (SQLException e) {
            auditLogService.logEvent("GUEST_DELETE_FAILED", "GUEST", "GUEST", guestId, actor,
                    "Failed to delete guest: " + e.getMessage(), "127.0.0.1", "FAILED");
            System.err.println("Lỗi xóa khách hàng: " + e.getMessage());
            return false;
        }
    }
}