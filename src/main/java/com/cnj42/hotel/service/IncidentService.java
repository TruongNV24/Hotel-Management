package com.cnj42.hotel.service;

import com.cnj42.hotel.dao.IncidentDAO;
import com.cnj42.hotel.dao.UserDAO;
import com.cnj42.hotel.model.Incident;
import com.cnj42.hotel.model.User;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public class IncidentService {
    private static final Set<String> TYPES = Set.of(Incident.PAYMENT, Incident.NO_CHECKOUT, Incident.PROPERTY_DAMAGE,
            Incident.GUEST_COMPLAINT, Incident.BILLING_DISPUTE, Incident.SERVICE_ISSUE, Incident.OTHER);
    private static final Set<String> PRIORITIES = Set.of(Incident.LOW, Incident.MEDIUM, Incident.HIGH, Incident.CRITICAL);
    private static final Set<String> STATUSES = Set.of(Incident.OPEN, Incident.IN_PROGRESS, Incident.RESOLVED, Incident.CLOSED, Incident.CANCELLED);
    private final IncidentDAO incidentDAO = new IncidentDAO();
    private final AuditLogService auditLogService = new AuditLogService();

    public List<Incident> search(String keyword, String type, String priority, String status) {
        try {
            return incidentDAO.search(keyword, normalize(type), normalize(priority), normalize(status));
        } catch (SQLException e) {
            System.err.println("Lỗi tải incident: " + e.getMessage());
            return List.of();
        }
    }

    public Incident findById(int incidentId) {
        try { return incidentDAO.findById(incidentId); }
        catch (SQLException e) { return null; }
    }

    public boolean canCreate(User actor) {
        return actor != null && "ACTIVE".equalsIgnoreCase(actor.getStatus());
    }

    public boolean canManage(User actor) {
        return PermissionService.canManageIncidents(actor);
    }

    public int create(Incident incident, User actor) {
        if (!canCreate(actor) || !validForCreate(incident)) return -1;
        incident.setReportedBy(actor.getUserId());
        incident.setStatus(Incident.OPEN);
        incident.setIncidentType(incident.getIncidentType().trim().toUpperCase());
        incident.setPriority(incident.getPriority().trim().toUpperCase());
        incident.setTitle(incident.getTitle().trim());
        incident.setDescription(trimToNull(incident.getDescription()));
        incident.setResolution(trimToNull(incident.getResolution()));
        try {
            validateRelations(incident);
            int id = incidentDAO.create(incident);
            if (id > 0) auditLogService.logEvent("CREATE_INCIDENT", "INCIDENT", "INCIDENT", id, actor, "Created incident: " + incident.getTitle(), "127.0.0.1", "SUCCESS");
            return id;
        } catch (SQLException | IllegalArgumentException e) {
            auditLogService.logEvent("CREATE_INCIDENT_FAILED", "INCIDENT", "INCIDENT", null, actor, "Failed to create incident: " + e.getMessage(), "127.0.0.1", "FAILED");
            return -1;
        }
    }

    public boolean update(Incident incident, User actor) {
        if (incident == null || incident.getIncidentId() <= 0) return false;
        Incident current = findById(incident.getIncidentId());
        if (current == null || !canUpdate(current, actor) || !validForUpdate(incident)) return false;
        try {
            validateRelations(incident);
            if (!canTransition(current.getStatus(), incident.getStatus())) return false;
            if (Incident.RESOLVED.equals(incident.getStatus()) && (incident.getResolution() == null || incident.getResolution().isBlank())) return false;
            if (Incident.RESOLVED.equals(incident.getStatus()) && current.getResolvedAt() == null) incident.setResolvedAt(LocalDateTime.now());
            if (Incident.CLOSED.equals(incident.getStatus()) && current.getClosedAt() == null) incident.setClosedAt(LocalDateTime.now());
            boolean updated = incidentDAO.update(incident);
            if (updated) {
                String action = Incident.CLOSED.equals(incident.getStatus()) ? "CLOSE_INCIDENT" :
                        Incident.RESOLVED.equals(incident.getStatus()) ? "RESOLVE_INCIDENT" :
                        !equals(current.getStatus(), incident.getStatus()) ? "CHANGE_INCIDENT_STATUS" :
                        !equals(current.getAssignedTo(), incident.getAssignedTo()) ? "ASSIGN_INCIDENT" : "UPDATE_INCIDENT";
                auditLogService.logEvent(action, "INCIDENT", "INCIDENT", incident.getIncidentId(), actor, "Updated incident " + incident.getIncidentId(), "127.0.0.1", "SUCCESS");
            }
            return updated;
        } catch (SQLException | IllegalArgumentException e) {
            auditLogService.logEvent("UPDATE_INCIDENT_FAILED", "INCIDENT", "INCIDENT", incident.getIncidentId(), actor, "Failed to update incident: " + e.getMessage(), "127.0.0.1", "FAILED");
            return false;
        }
    }

    public boolean assign(int incidentId, Integer assignedTo, User actor) {
        if (!canManage(actor)) return false;
        Incident incident = findById(incidentId);
        if (incident == null) return false;
        if (assignedTo != null) {
            try { if (!incidentDAO.exists("users", assignedTo)) return false; }
            catch (SQLException e) { return false; }
        }
        incident.setAssignedTo(assignedTo);
        return update(incident, actor);
    }

    public boolean transition(int incidentId, String status, String resolution, User actor) {
        Incident incident = findById(incidentId);
        if (incident == null) return false;
        incident.setStatus(status);
        incident.setResolution(resolution);
        return update(incident, actor);
    }

    public boolean isValidType(String type) { return type != null && TYPES.contains(type.trim().toUpperCase()); }
    public boolean isValidPriority(String priority) { return priority != null && PRIORITIES.contains(priority.trim().toUpperCase()); }
    public boolean isValidStatus(String status) { return status != null && STATUSES.contains(status.trim().toUpperCase()); }
    public boolean canTransition(String from, String to) {
        if (equals(from, to)) return true;
        return (Incident.OPEN.equals(from) && (Incident.IN_PROGRESS.equals(to) || Incident.CANCELLED.equals(to)))
                || (Incident.IN_PROGRESS.equals(from) && (Incident.RESOLVED.equals(to) || Incident.CANCELLED.equals(to)))
                || (Incident.RESOLVED.equals(from) && Incident.CLOSED.equals(to));
    }

    private boolean validForCreate(Incident incident) {
        return incident != null && incident.getTitle() != null && !incident.getTitle().isBlank()
                && isValidType(incident.getIncidentType()) && isValidPriority(incident.getPriority());
    }

    private boolean validForUpdate(Incident incident) {
        return incident.getTitle() != null && !incident.getTitle().isBlank()
                && isValidType(incident.getIncidentType()) && isValidPriority(incident.getPriority())
                && isValidStatus(incident.getStatus());
    }

    private boolean canUpdate(Incident incident, User actor) {
        if (actor == null) return false;
        if (PermissionService.canManageIncidents(actor)) return true;
        return PermissionService.canRecordIncident(actor)
                && (actor.getUserId() == safeId(incident.getReportedBy()) || actor.getUserId() == safeId(incident.getAssignedTo()));
    }

    private void validateRelations(Incident incident) throws SQLException {
        validateRelation("guests", incident.getGuestId());
        validateRelation("reservations", incident.getReservationId());
        validateRelation("stays", incident.getStayId());
        validateRelation("invoices", incident.getInvoiceId());
        if (incident.getAssignedTo() != null) validateRelation("users", incident.getAssignedTo());
    }

    private void validateRelation(String table, Integer id) throws SQLException {
        if (id != null && id <= 0) throw new IllegalArgumentException("Invalid relation ID");
        if (id != null && !incidentDAO.exists(table, id)) throw new IllegalArgumentException("Related record not found: " + table);
    }

    private User findActor(Integer userId) {
        try { return userId == null ? null : new UserDAO().findById(userId); }
        catch (SQLException e) { return null; }
    }

    private String normalize(String value) { return value == null ? null : value.trim().toUpperCase(); }
    private String trimToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private int safeId(Integer value) { return value == null ? -1 : value; }
    private boolean equals(Object first, Object second) { return first == null ? second == null : first.equals(second); }
}
