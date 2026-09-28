package com.cnj42.hotel;

import com.cnj42.hotel.model.User;
import com.cnj42.hotel.service.PermissionService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DashboardReportTest {
    @Test
    void reportAccessIsRestrictedToManagerOnly() {
        User admin = user("ADMIN");
        User manager = user("MANAGER");
        User employee = user("EMPLOYEE");

        assertFalse(PermissionService.canAccessReports(admin));
        assertTrue(PermissionService.canAccessReports(manager));
        assertFalse(PermissionService.canAccessReports(employee));
    }

    private User user(String role) {
        User user = new User();
        user.setRole(role);
        user.setStatus("ACTIVE");
        return user;
    }
}
