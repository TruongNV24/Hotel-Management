package com.cnj42.hotel.service;

import com.cnj42.hotel.dao.ServiceDAO;
import com.cnj42.hotel.model.Service;
import com.cnj42.hotel.model.User;

import java.sql.SQLException;
import java.util.List;

public class ServiceService {
    private final ServiceDAO serviceDAO = new ServiceDAO();
    private final AuditLogService auditLogService = new AuditLogService();

    public List<Service> search(String keyword, String status) {
        try {
            return serviceDAO.search(keyword, status);
        } catch (SQLException e) {
            System.err.println("Lỗi tải danh sách dịch vụ: " + e.getMessage());
            return List.of();
        }
    }

    public boolean canManage(User actor) {
        return PermissionService.canManageServices(actor);
    }

    public boolean create(Service service, User actor) {
        if (!canManage(actor) || !isValid(service)) return false;
        try {
            service.setServiceName(service.getServiceName().trim());
            service.setUnit(normalizeUnit(service.getUnit()));
            service.setDescription(trimToNull(service.getDescription()));
            service.setStatus(normalizeStatus(service.getStatus()));
            if (serviceDAO.existsByName(service.getServiceName(), null)) return false;
            boolean created = serviceDAO.create(service);
            if (created) {
                auditLogService.logEvent("CREATE_SERVICE", "SERVICE", "SERVICE", service.getServiceId(), actor,
                        "Created service: " + service.getServiceName(), "127.0.0.1", "SUCCESS");
            }
            return created;
        } catch (SQLException e) {
            auditLogService.logEvent("CREATE_SERVICE_FAILED", "SERVICE", "SERVICE", null, actor,
                    "Failed to create service: " + safeName(service) + ": " + e.getMessage(), "127.0.0.1", "FAILED");
            return false;
        }
    }

    public boolean update(Service service, User actor) {
        if (!canManage(actor) || !isValid(service) || service.getServiceId() <= 0) return false;
        try {
            service.setServiceName(service.getServiceName().trim());
            service.setUnit(normalizeUnit(service.getUnit()));
            service.setDescription(trimToNull(service.getDescription()));
            service.setStatus(normalizeStatus(service.getStatus()));
            if (serviceDAO.existsByName(service.getServiceName(), service.getServiceId())) return false;
            boolean updated = serviceDAO.update(service);
            if (updated) {
                auditLogService.logEvent("UPDATE_SERVICE", "SERVICE", "SERVICE", service.getServiceId(), actor,
                        "Updated service: " + service.getServiceName() + ", price=" + service.getPrice(), "127.0.0.1", "SUCCESS");
            }
            return updated;
        } catch (SQLException e) {
            auditLogService.logEvent("UPDATE_SERVICE_FAILED", "SERVICE", "SERVICE", service.getServiceId(), actor,
                    "Failed to update service: " + safeName(service) + ": " + e.getMessage(), "127.0.0.1", "FAILED");
            return false;
        }
    }

    public boolean setActive(int serviceId, boolean active, User actor) {
        if (!canManage(actor) || serviceId <= 0) return false;
        try {
            boolean updated = serviceDAO.setStatus(serviceId, active ? "ACTIVE" : "INACTIVE");
            if (updated) {
                auditLogService.logEvent(active ? "ENABLE_SERVICE" : "DISABLE_SERVICE", "SERVICE", "SERVICE", serviceId, actor,
                        (active ? "Enabled" : "Disabled") + " service id " + serviceId, "127.0.0.1", "SUCCESS");
            }
            return updated;
        } catch (SQLException e) {
            auditLogService.logEvent(active ? "ENABLE_SERVICE_FAILED" : "DISABLE_SERVICE_FAILED", "SERVICE", "SERVICE", serviceId, actor,
                    "Failed to change service status: " + e.getMessage(), "127.0.0.1", "FAILED");
            return false;
        }
    }

    public boolean isValid(Service service) {
        return service != null
                && service.getServiceName() != null
                && !service.getServiceName().trim().isEmpty()
                && service.getPrice() > 0
                && Double.isFinite(service.getPrice());
    }

    private String normalizeStatus(String status) {
        return "INACTIVE".equalsIgnoreCase(status) ? "INACTIVE" : "ACTIVE";
    }

    private String normalizeUnit(String unit) {
        return unit == null || unit.isBlank() ? "Unit" : unit.trim();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String safeName(Service service) {
        return service == null || service.getServiceName() == null ? "unknown" : service.getServiceName();
    }
}
