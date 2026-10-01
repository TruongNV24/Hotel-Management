package com.cnj42.hotel.service;

import com.cnj42.hotel.dao.MaintenanceDAO;
import com.cnj42.hotel.model.Maintenance;
import com.cnj42.hotel.model.User;

import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public class MaintenanceService {
    private static final Set<String> TYPES = Set.of(Maintenance.PREVENTIVE, Maintenance.CORRECTIVE, Maintenance.EMERGENCY, Maintenance.INSPECTION, Maintenance.CLEANING, Maintenance.OTHER);
    private static final Set<String> PRIORITIES = Set.of(Maintenance.LOW, Maintenance.MEDIUM, Maintenance.HIGH, Maintenance.CRITICAL);
    private final MaintenanceDAO maintenanceDAO = new MaintenanceDAO();
    private final AuditLogService auditLogService = new AuditLogService();

    public List<Maintenance> search(Integer roomId, String type, String priority, String status, String keyword) {
        try {
            return maintenanceDAO.search(roomId, normalize(type), normalize(priority), normalize(status), keyword);
        } catch (SQLException e) {
            System.err.println("Lỗi tải danh sách bảo trì: " + e.getMessage());
            return List.of();
        }
    }

    public Maintenance findById(int maintenanceId) {
        try { return maintenanceDAO.findById(maintenanceId); }
        catch (SQLException e) { return null; }
    }

    public boolean canReport(User actor) {
        return actor != null && PermissionService.canReportMaintenance(actor);
    }

    public boolean canManage(User actor) {
        return actor != null && PermissionService.canManageMaintenance(actor);
    }

    public int create(Maintenance maintenance, User actor) {
        if (!canReport(actor) || !isValid(maintenance)) return -1;
        maintenance.setReportedBy(actor.getUserId());
        maintenance.setMaintenanceType(normalize(maintenance.getMaintenanceType()));
        maintenance.setPriority(normalize(maintenance.getPriority()));
        maintenance.setTitle(maintenance.getTitle().trim());
        maintenance.setDescription(trimToNull(maintenance.getDescription()));
        maintenance.setResolution(trimToNull(maintenance.getResolution()));
        maintenance.setNotes(trimToNull(maintenance.getNotes()));
        try {
            if (!maintenanceDAO.roomExists(maintenance.getRoomId())) return -1;
            validateAssignedUser(maintenance.getAssignedTo());
            validateExpectedTime(maintenance.getStartedAt(), maintenance.getExpectedEndAt());
            int id = maintenanceDAO.create(maintenance);
            if (id > 0) auditLogService.logEvent("CREATE_MAINTENANCE", "MAINTENANCE", "MAINTENANCE", id, actor,
                    "Created maintenance for room " + maintenance.getRoomId() + ": " + maintenance.getTitle(), "127.0.0.1", "SUCCESS");
            return id;
        } catch (SQLException | IllegalArgumentException e) {
            auditLogService.logEvent("CREATE_MAINTENANCE_FAILED", "MAINTENANCE", "MAINTENANCE", null, actor,
                    "Failed to create maintenance: " + e.getMessage(), "127.0.0.1", "FAILED");
            return -1;
        }
    }

    public boolean update(Maintenance maintenance, User actor) {
        if (maintenance == null || maintenance.getMaintenanceId() <= 0) return false;
        Maintenance current = findById(maintenance.getMaintenanceId());
        if (current == null || !canEdit(current, actor) || !isValid(maintenance)) return false;
        try {
            validateAssignedUser(maintenance.getAssignedTo());
            validateExpectedTime(current.getStartedAt(), maintenance.getExpectedEndAt());
            boolean updated = maintenanceDAO.update(maintenance);
                if (updated) {
                String action = !same(current.getAssignedTo(), maintenance.getAssignedTo()) ? "ASSIGN_MAINTENANCE" :
                    !same(current.getPriority(), maintenance.getPriority()) ? "CHANGE_MAINTENANCE_PRIORITY" : "UPDATE_MAINTENANCE";
                auditLogService.logEvent(action, "MAINTENANCE", "MAINTENANCE", maintenance.getMaintenanceId(), actor,
                    "Updated maintenance " + maintenance.getMaintenanceId(), "127.0.0.1", "SUCCESS");
                }
            return updated;
        } catch (SQLException | IllegalArgumentException e) {
            auditLogService.logEvent("UPDATE_MAINTENANCE_FAILED", "MAINTENANCE", "MAINTENANCE", maintenance.getMaintenanceId(), actor,
                    "Failed to update maintenance: " + e.getMessage(), "127.0.0.1", "FAILED");
            return false;
        }
    }

    public boolean assign(int maintenanceId, Integer assignedTo, User actor) {
        if (!canManage(actor)) return false;
        Maintenance maintenance = findById(maintenanceId);
        if (maintenance == null) return false;
        maintenance.setAssignedTo(assignedTo);
        return update(maintenance, actor);
    }

    public boolean start(int maintenanceId, User actor) {
        if (!canManage(actor)) return false;
        try {
            boolean started = maintenanceDAO.start(maintenanceId);
            if (started) auditLogService.logEvent("START_MAINTENANCE", "MAINTENANCE", "MAINTENANCE", maintenanceId, actor,
                    "Started maintenance " + maintenanceId, "127.0.0.1", "SUCCESS");
            return started;
        } catch (SQLException e) {
            auditLogService.logEvent("START_MAINTENANCE_FAILED", "MAINTENANCE", "MAINTENANCE", maintenanceId, actor,
                    "Failed to start maintenance: " + e.getMessage(), "127.0.0.1", "FAILED");
            return false;
        }
    }

    public boolean complete(int maintenanceId, String resolution, User actor) {
        if (!canManage(actor) || resolution == null || resolution.isBlank()) return false;
        try {
            Maintenance current = maintenanceDAO.findById(maintenanceId);
            if (current == null || !Maintenance.IN_PROGRESS.equals(current.getStatus())) return false;
            boolean completed = maintenanceDAO.complete(maintenanceId, resolution.trim());
            if (completed) auditLogService.logEvent("COMPLETE_MAINTENANCE", "MAINTENANCE", "MAINTENANCE", maintenanceId, actor,
                    "Completed maintenance " + maintenanceId + ": " + resolution.trim(), "127.0.0.1", "SUCCESS");
            return completed;
        } catch (SQLException e) {
            auditLogService.logEvent("COMPLETE_MAINTENANCE_FAILED", "MAINTENANCE", "MAINTENANCE", maintenanceId, actor,
                    "Failed to complete maintenance: " + e.getMessage(), "127.0.0.1", "FAILED");
            return false;
        }
    }

    public boolean cancel(int maintenanceId, User actor) {
        if (!canManage(actor)) return false;
        try {
            boolean cancelled = maintenanceDAO.cancel(maintenanceId);
            if (cancelled) auditLogService.logEvent("CANCEL_MAINTENANCE", "MAINTENANCE", "MAINTENANCE", maintenanceId, actor,
                    "Cancelled maintenance " + maintenanceId, "127.0.0.1", "SUCCESS");
            return cancelled;
        } catch (SQLException e) {
            auditLogService.logEvent("CANCEL_MAINTENANCE_FAILED", "MAINTENANCE", "MAINTENANCE", maintenanceId, actor,
                    "Failed to cancel maintenance: " + e.getMessage(), "127.0.0.1", "FAILED");
            return false;
        }
    }

    public boolean isValid(Maintenance maintenance) {
        return maintenance != null && maintenance.getRoomId() > 0 && maintenance.getTitle() != null && !maintenance.getTitle().isBlank()
                && isValidType(maintenance.getMaintenanceType()) && isValidPriority(maintenance.getPriority());
    }

    public boolean isValidType(String value) { return value != null && TYPES.contains(value.trim().toUpperCase()); }
    public boolean isValidPriority(String value) { return value != null && PRIORITIES.contains(value.trim().toUpperCase()); }

    public boolean canTransition(String from, String to) {
        if (from == null || to == null) return false;
        if (from.equals(to)) return true;
        return (Maintenance.OPEN.equals(from) && (Maintenance.IN_PROGRESS.equals(to) || Maintenance.CANCELLED.equals(to)))
                || (Maintenance.IN_PROGRESS.equals(from) && (Maintenance.COMPLETED.equals(to) || Maintenance.CANCELLED.equals(to)));
    }

    public static int calculateDurationMinutes(LocalDateTime startedAt, LocalDateTime completedAt) {
        if (startedAt == null || completedAt == null || completedAt.isBefore(startedAt)) return -1;
        return Math.toIntExact(Duration.between(startedAt, completedAt).toMinutes());
    }

    private boolean canEdit(Maintenance maintenance, User actor) {
        if (!canReport(actor)) return false;
        if (canManage(actor)) return true;
        return actor.getUserId() == safeId(maintenance.getReportedBy()) || actor.getUserId() == safeId(maintenance.getAssignedTo());
    }

    private void validateAssignedUser(Integer userId) throws SQLException {
        if (userId != null && (userId <= 0 || !maintenanceDAO.userExists(userId))) throw new IllegalArgumentException("Assigned user not found");
    }

    private void validateExpectedTime(LocalDateTime startedAt, LocalDateTime expectedEndAt) {
        if (startedAt != null && expectedEndAt != null && expectedEndAt.isBefore(startedAt)) throw new IllegalArgumentException("Expected end precedes start");
    }

    private String normalize(String value) { return value == null ? null : value.trim().toUpperCase(); }
    private String trimToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private int safeId(Integer value) { return value == null ? -1 : value; }
    private boolean same(Object first, Object second) { return first == null ? second == null : first.equals(second); }
}
