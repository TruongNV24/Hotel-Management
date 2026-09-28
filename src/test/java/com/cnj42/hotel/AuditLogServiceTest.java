package com.cnj42.hotel;

import com.cnj42.hotel.model.User;
import com.cnj42.hotel.service.AuditLogService;
import com.cnj42.hotel.service.PermissionService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuditLogServiceTest {

    @Test
    void canViewAuditLogs_shouldAllowAdminOnly() {
        User admin = new User();
        admin.setRole("ADMIN");

        User manager = new User();
        manager.setRole("MANAGER");

        User employee = new User();
        employee.setRole("EMPLOYEE");

        assertTrue(PermissionService.canViewAuditLogs(admin));
        assertFalse(PermissionService.canViewAuditLogs(manager));
        assertFalse(PermissionService.canViewAuditLogs(employee));
        assertFalse(PermissionService.canViewAuditLogs(null));
    }

    @Test
    void shouldNormalizeAuditActionName() {
        assertEquals("LOGIN_SUCCESS", AuditLogService.normalizeAction("login_success"));
        assertEquals("LOGIN_SUCCESS", AuditLogService.normalizeAction(" LOGIN_SUCCESS "));
        assertEquals("USER_CREATED", AuditLogService.normalizeAction("user_created"));
    }
}
