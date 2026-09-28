package com.cnj42.hotel;

import com.cnj42.hotel.model.Maintenance;
import com.cnj42.hotel.model.User;
import com.cnj42.hotel.service.MaintenanceService;
import com.cnj42.hotel.service.PermissionService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class MaintenanceManagementTest {
    private final MaintenanceService maintenanceService = new MaintenanceService();

    @Test
    void validatesMaintenanceTypePriorityAndTitle() {
        Maintenance maintenance = new Maintenance();
        maintenance.setRoomId(305);
        maintenance.setTitle("Air conditioner repair");
        maintenance.setMaintenanceType(Maintenance.CORRECTIVE);
        maintenance.setPriority(Maintenance.HIGH);
        assertTrue(maintenanceService.isValid(maintenance));

        maintenance.setTitle(" ");
        assertFalse(maintenanceService.isValid(maintenance));
        assertFalse(maintenanceService.isValidType("UNKNOWN"));
        assertFalse(maintenanceService.isValidPriority("URGENT"));
    }

    @Test
    void calculatesActualDurationFromTimestamps() {
        LocalDateTime started = LocalDateTime.of(2026, 9, 10, 8, 0);
        LocalDateTime completed = LocalDateTime.of(2026, 9, 10, 15, 30);
        assertEquals(450, MaintenanceService.calculateDurationMinutes(started, completed));
        assertEquals(-1, MaintenanceService.calculateDurationMinutes(completed, started));
    }

    @Test
    void identifiesOverdueInProgressMaintenance() {
        Maintenance maintenance = new Maintenance();
        maintenance.setStatus(Maintenance.IN_PROGRESS);
        maintenance.setExpectedEndAt(LocalDateTime.of(2026, 9, 10, 10, 0));
        assertTrue(maintenance.isOverdue(LocalDateTime.of(2026, 9, 10, 10, 1)));
        maintenance.setStatus(Maintenance.COMPLETED);
        assertFalse(maintenance.isOverdue(LocalDateTime.of(2026, 9, 10, 10, 1)));
    }

    @Test
    void enforcesMaintenanceStateTransitions() {
        assertTrue(maintenanceService.canTransition(Maintenance.OPEN, Maintenance.IN_PROGRESS));
        assertTrue(maintenanceService.canTransition(Maintenance.IN_PROGRESS, Maintenance.COMPLETED));
        assertTrue(maintenanceService.canTransition(Maintenance.OPEN, Maintenance.CANCELLED));
        assertFalse(maintenanceService.canTransition(Maintenance.OPEN, Maintenance.COMPLETED));
        assertFalse(maintenanceService.canTransition(Maintenance.COMPLETED, Maintenance.IN_PROGRESS));
    }

    @Test
    void permissionsAllowReportingButRestrictLifecycleManagement() {
        User admin = user("ADMIN");
        User manager = user("MANAGER");
        User employee = user("EMPLOYEE");
        assertTrue(PermissionService.canManageMaintenance(admin));
        assertTrue(PermissionService.canManageMaintenance(manager));
        assertFalse(PermissionService.canManageMaintenance(employee));
        assertTrue(PermissionService.canReportMaintenance(employee));
    }

    private User user(String role) {
        User user = new User();
        user.setRole(role);
        return user;
    }
}
