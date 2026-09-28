package com.cnj42.hotel;

import com.cnj42.hotel.model.User;
import com.cnj42.hotel.model.UserRole;
import com.cnj42.hotel.service.PermissionService;
import com.cnj42.hotel.utils.PasswordUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthPermissionTest {

    @Test
    void receptionistIsNormalizedAsEmployee() {
        assertEquals("EMPLOYEE", UserRole.normalize("RECEPTIONIST"));
        assertEquals("EMPLOYEE", UserRole.normalize("receptionist"));
        assertTrue(UserRole.isEmployee("RECEPTIONIST"));
        assertTrue(UserRole.isEmployee("EMPLOYEE"));
    }

    @Test
    void passwordHashingMatchesPlainText() {
        String plainPassword = "Secret123!";
        String hashedPassword = PasswordUtil.hashPassword(plainPassword);

        assertNotNull(hashedPassword);
        assertNotEquals(plainPassword, hashedPassword);
        assertTrue(PasswordUtil.matches(plainPassword, hashedPassword));
        assertFalse(PasswordUtil.matches("wrong-password", hashedPassword));
    }

    @Test
    void permissionsEnforceRoleRules() {
        User admin = new User();
        admin.setRole("ADMIN");

        User manager = new User();
        manager.setRole("MANAGER");

        User employee = new User();
        employee.setRole("RECEPTIONIST");

        assertTrue(PermissionService.canManageUsers(admin));
        assertFalse(PermissionService.canManageUsers(manager));
        assertFalse(PermissionService.canManageUsers(employee));

        assertTrue(PermissionService.canCreateManager(admin));
        assertFalse(PermissionService.canCreateManager(manager));
        assertFalse(PermissionService.canCreateManager(employee));

        assertTrue(PermissionService.canViewAuditLogs(admin));
        assertFalse(PermissionService.canViewAuditLogs(manager));
        assertFalse(PermissionService.canViewAuditLogs(employee));

        assertFalse(PermissionService.canAccessReports(admin));
        assertTrue(PermissionService.canAccessReports(manager));
        assertFalse(PermissionService.canAccessReports(employee));

        assertFalse(PermissionService.canAccessHotelOperations(admin));
        assertTrue(PermissionService.canAccessHotelOperations(manager));
        assertTrue(PermissionService.canAccessHotelOperations(employee));

        assertFalse(PermissionService.canManageServices(admin));
        assertTrue(PermissionService.canManageServices(manager));
        assertFalse(PermissionService.canManageServices(employee));

        assertFalse(PermissionService.canManageIncidents(admin));
        assertTrue(PermissionService.canManageIncidents(manager));
        assertFalse(PermissionService.canManageIncidents(employee));
    }
}
