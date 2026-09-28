package com.cnj42.hotel;

import com.cnj42.hotel.model.Incident;
import com.cnj42.hotel.model.User;
import com.cnj42.hotel.service.IncidentService;
import com.cnj42.hotel.service.PermissionService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IncidentManagementTest {
    private final IncidentService incidentService = new IncidentService();

    @Test
    void acceptsRequiredIncidentTypesAndPriorities() {
        assertTrue(incidentService.isValidType(Incident.PAYMENT));
        assertTrue(incidentService.isValidType(Incident.NO_CHECKOUT));
        assertTrue(incidentService.isValidType(Incident.PROPERTY_DAMAGE));
        assertTrue(incidentService.isValidType(Incident.BILLING_DISPUTE));
        assertFalse(incidentService.isValidType("UNKNOWN"));
        assertTrue(incidentService.isValidPriority(Incident.CRITICAL));
        assertFalse(incidentService.isValidPriority("URGENT"));
    }

    @Test
    void allowsOnlyDefinedIncidentStateTransitions() {
        assertTrue(incidentService.canTransition(Incident.OPEN, Incident.IN_PROGRESS));
        assertTrue(incidentService.canTransition(Incident.IN_PROGRESS, Incident.RESOLVED));
        assertTrue(incidentService.canTransition(Incident.RESOLVED, Incident.CLOSED));
        assertTrue(incidentService.canTransition(Incident.OPEN, Incident.CANCELLED));
        assertFalse(incidentService.canTransition(Incident.CLOSED, Incident.OPEN));
        assertFalse(incidentService.canTransition(Incident.RESOLVED, Incident.IN_PROGRESS));
    }

    @Test
    void permissionsMatchIncidentRoles() {
        User admin = user("ADMIN");
        User manager = user("MANAGER");
        User employee = user("EMPLOYEE");

        assertTrue(PermissionService.canManageIncidents(admin));
        assertTrue(PermissionService.canManageIncidents(manager));
        assertFalse(PermissionService.canManageIncidents(employee));
        assertTrue(PermissionService.canRecordIncident(employee));
    }

    @Test
    void activeUserCanCreateButInactiveUserCannot() {
        User active = user("EMPLOYEE");
        active.setStatus("ACTIVE");
        User inactive = user("EMPLOYEE");
        inactive.setStatus("INACTIVE");

        assertTrue(incidentService.canCreate(active));
        assertFalse(incidentService.canCreate(inactive));
    }

    private User user(String role) {
        User user = new User();
        user.setUserId(10);
        user.setRole(role);
        return user;
    }
}
