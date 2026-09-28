package com.cnj42.hotel.service;

import com.cnj42.hotel.model.User;
import com.cnj42.hotel.model.UserRole;

public final class PermissionService {

    private PermissionService() {
    }

    public static boolean canManageUsers(User user) {
        return UserRole.isAdmin(user);
    }

    public static boolean canCreateManager(User user) {
        return UserRole.isAdmin(user);
    }

    public static boolean canAccessHotelOperations(User user) {
        return UserRole.isManager(user) || UserRole.isEmployee(user);
    }

    public static boolean canAccessRoomManagement(User user) {
        return UserRole.isManager(user);
    }

    public static boolean canAccessReports(User user) {
        return UserRole.isAdmin(user) || UserRole.isManager(user);
    }

    public static boolean canAccessEmployeeOperations(User user) {
        return UserRole.isManager(user) || UserRole.isEmployee(user);
    }

    public static boolean canViewAuditLogs(User user) {
        return UserRole.isAdmin(user) || UserRole.isManager(user);
    }

    public static boolean canManageServices(User user) {
        return UserRole.isAdmin(user) || UserRole.isManager(user);
    }

    public static boolean canRecordServiceUsage(User user) {
        return UserRole.isAdmin(user) || UserRole.isManager(user) || UserRole.isEmployee(user);
    }

    public static boolean canManageIncidents(User user) {
        return UserRole.isAdmin(user) || UserRole.isManager(user);
    }

    public static boolean canRecordIncident(User user) {
        return UserRole.isAdmin(user) || UserRole.isManager(user) || UserRole.isEmployee(user);
    }

    public static boolean canManageMaintenance(User user) {
        return UserRole.isAdmin(user) || UserRole.isManager(user);
    }

    public static boolean canReportMaintenance(User user) {
        return UserRole.isAdmin(user) || UserRole.isManager(user) || UserRole.isEmployee(user);
    }

    public static boolean canEditUserRole(User actor, User targetUser) {
        if (!canManageUsers(actor)) {
            return false;
        }

        if (targetUser == null) {
            return false;
        }

        String targetRole = UserRole.normalize(targetUser.getRole());
        return !"ADMIN".equals(targetRole);
    }
}
