package com.cnj42.hotel.service;

import com.cnj42.hotel.dao.AuditLogDAO;
import com.cnj42.hotel.model.AuditLog;
import com.cnj42.hotel.model.User;

import java.util.List;

public class AuditLogService {

    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    public boolean logEvent(String action, String module, String entityType, Integer entityId,
                            User actor, String details, String ipAddress, String status) {
        if (action == null || action.isBlank()) {
            return false;
        }

        AuditLog log = new AuditLog();
        log.setAction(normalizeAction(action));
        log.setModule(module == null ? "SYSTEM" : module);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        if (actor != null) {
            log.setActorUserId(actor.getUserId());
            log.setActorUsername(actor.getUsername());
            log.setActorRole(actor.getNormalizedRole());
        }
        log.setDetails(details);
        log.setIpAddress(ipAddress);
        log.setStatus(status == null ? "SUCCESS" : status.toUpperCase());

        try {
            return auditLogDAO.insert(log);
        } catch (Exception e) {
            System.err.println("Lỗi ghi audit log: " + e.getMessage());
            return false;
        }
    }

    public List<AuditLog> getRecentLogs(int limit) {
        try {
            return auditLogDAO.getRecentLogs(limit);
        } catch (Exception e) {
            System.err.println("Lỗi lấy audit log: " + e.getMessage());
            return List.of();
        }
    }

    public List<AuditLog> searchLogs(String keyword, String action, String module, String status) {
        try {
            return auditLogDAO.searchLogs(keyword, action, module, status);
        } catch (Exception e) {
            System.err.println("Lỗi tìm audit log: " + e.getMessage());
            return List.of();
        }
    }

    public static String normalizeAction(String action) {
        if (action == null) {
            return "SYSTEM_ACTION";
        }

        String normalized = action.trim().replace(' ', '_').replace('-', '_').toUpperCase();
        if (normalized.isBlank()) {
            return "SYSTEM_ACTION";
        }
        return normalized;
    }
}
