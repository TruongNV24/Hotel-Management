package com.cnj42.hotel.service;

import com.cnj42.hotel.dao.ServiceDAO;
import com.cnj42.hotel.dao.ServiceUsageDAO;
import com.cnj42.hotel.dao.UserDAO;
import com.cnj42.hotel.model.Service;
import com.cnj42.hotel.model.ServiceUsageRecord;
import com.cnj42.hotel.model.User;
import com.cnj42.hotel.utils.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class ServiceUsageService {
    private final ServiceUsageDAO usageDAO = new ServiceUsageDAO();
    private final ServiceDAO serviceDAO = new ServiceDAO();
    private final AuditLogService auditLogService = new AuditLogService();

    public List<ServiceUsageRecord> findByStay(int stayId) {
        try {
            return usageDAO.findByStay(stayId);
        } catch (SQLException e) {
            System.err.println("Lỗi tải dịch vụ đã sử dụng: " + e.getMessage());
            return List.of();
        }
    }

    public int create(int stayId, int serviceId, int quantity, String note, Integer actorUserId) {
        if (!PermissionService.canRecordServiceUsage(findActor(actorUserId)) || quantity <= 0) return -1;
        try {
            if (!isActiveStay(stayId)) return -1;
            Service service = serviceDAO.findById(serviceId);
            if (service == null || !"ACTIVE".equalsIgnoreCase(service.getStatus()) || service.getPrice() <= 0) return -1;
            int usageId = usageDAO.create(stayId, serviceId, quantity, service.getPrice(), trimToNull(note), actorUserId);
            if (usageId > 0) {
                auditLogService.logEvent("CREATE_SERVICE_USAGE", "SERVICE", "SERVICE_USAGE", usageId, findActor(actorUserId),
                        "Added service " + service.getServiceName() + " x" + quantity + " to stay " + stayId, "127.0.0.1", "SUCCESS");
            }
            return usageId;
        } catch (SQLException e) {
            auditLogService.logEvent("CREATE_SERVICE_USAGE_FAILED", "SERVICE", "SERVICE_USAGE", null, findActor(actorUserId),
                    "Failed to add service usage to stay " + stayId + ": " + e.getMessage(), "127.0.0.1", "FAILED");
            return -1;
        }
    }

    public boolean update(int usageId, int quantity, String note, Integer actorUserId) {
        User actor = findActor(actorUserId);
        if (!PermissionService.canRecordServiceUsage(actor) || usageId <= 0 || quantity <= 0) return false;
        try {
            ServiceUsageRecord usage = findUsage(usageId);
            if (usage == null || !isActiveStay(usage.getStayId()) || usageDAO.isStayInPaidInvoice(usage.getStayId())) return false;
            boolean updated = usageDAO.update(usageId, quantity, trimToNull(note));
            if (updated) {
                auditLogService.logEvent("UPDATE_SERVICE_USAGE", "SERVICE", "SERVICE_USAGE", usageId, actor,
                        "Updated service usage quantity to " + quantity, "127.0.0.1", "SUCCESS");
            }
            return updated;
        } catch (SQLException e) {
            auditLogService.logEvent("UPDATE_SERVICE_USAGE_FAILED", "SERVICE", "SERVICE_USAGE", usageId, actor,
                    "Failed to update service usage: " + e.getMessage(), "127.0.0.1", "FAILED");
            return false;
        }
    }

    public boolean delete(int usageId, Integer actorUserId) {
        User actor = findActor(actorUserId);
        if (!PermissionService.canRecordServiceUsage(actor) || usageId <= 0) return false;
        try {
            ServiceUsageRecord usage = findUsage(usageId);
            if (usage == null || !isActiveStay(usage.getStayId()) || usageDAO.isStayInPaidInvoice(usage.getStayId())) return false;
            boolean deleted = usageDAO.delete(usageId);
            if (deleted) {
                auditLogService.logEvent("DELETE_SERVICE_USAGE", "SERVICE", "SERVICE_USAGE", usageId, actor,
                        "Deleted service usage from stay " + usage.getStayId(), "127.0.0.1", "SUCCESS");
            }
            return deleted;
        } catch (SQLException e) {
            auditLogService.logEvent("DELETE_SERVICE_USAGE_FAILED", "SERVICE", "SERVICE_USAGE", usageId, actor,
                    "Failed to delete service usage: " + e.getMessage(), "127.0.0.1", "FAILED");
            return false;
        }
    }

    public double totalForStay(int stayId) {
        String sql = "SELECT COALESCE(SUM(total_amount), 0) FROM service_usages WHERE stay_id = ?";
        try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, stayId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0;
            }
        } catch (SQLException e) {
            return 0;
        }
    }

    public static double calculateTotal(int quantity, double unitPrice) {
        if (quantity <= 0 || !Double.isFinite(unitPrice) || unitPrice <= 0) {
            return 0;
        }
        return quantity * unitPrice;
    }

    private ServiceUsageRecord findUsage(int usageId) throws SQLException {
        String sql = "SELECT su.usage_id, su.stay_id, su.service_id, s.service_name, su.quantity, su.unit_price, su.total_amount, su.used_at, su.note, su.created_by " +
                "FROM service_usages su JOIN services s ON s.service_id = su.service_id WHERE su.usage_id = ?";
        try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, usageId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) return null;
                ServiceUsageRecord usage = new ServiceUsageRecord();
                usage.setUsageId(rs.getInt("usage_id"));
                usage.setStayId(rs.getInt("stay_id"));
                usage.setServiceId(rs.getInt("service_id"));
                usage.setServiceName(rs.getString("service_name"));
                usage.setQuantity(rs.getInt("quantity"));
                usage.setUnitPrice(rs.getDouble("unit_price"));
                usage.setTotalAmount(rs.getDouble("total_amount"));
                return usage;
            }
        }
    }

    private boolean isActiveStay(int stayId) throws SQLException {
        try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(
                "SELECT status FROM stays WHERE stay_id = ?")) {
            statement.setInt(1, stayId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() && "CHECKED_IN".equalsIgnoreCase(rs.getString(1));
            }
        }
    }

    private User findActor(Integer actorUserId) {
        if (actorUserId == null) return null;
        try {
            return new UserDAO().findById(actorUserId);
        } catch (SQLException e) {
            return null;
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
