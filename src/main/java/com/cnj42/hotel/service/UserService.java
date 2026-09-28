package com.cnj42.hotel.service;

import com.cnj42.hotel.dao.UserDAO;
import com.cnj42.hotel.model.User;
import com.cnj42.hotel.model.UserRole;
import com.cnj42.hotel.utils.PasswordUtil;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserService {

    private final UserDAO userDAO;
    private final AuditLogService auditLogService = new AuditLogService();

    public UserService() {
        this.userDAO = new UserDAO();
    }

    public List<User> getAllUsers() {
        try {
            return userDAO.getAllUsers();
        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy danh sách user: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public boolean createUser(User user) {
        return createUser(user, null);
    }

    public boolean createUser(User user, User actor) {
        if (user == null) {
            return false;
        }

        if (!PermissionService.canManageUsers(actor) && actor != null) {
            return false;
        }

        if (user.getPassword() == null || user.getPassword().isBlank()) {
            return false;
        }

        String normalizedRole = UserRole.normalize(user.getRole());
        if ("ADMIN".equals(normalizedRole)) {
            return false;
        }

        user.setRole(normalizedRole);
        user.setPassword(PasswordUtil.hashPassword(user.getPassword()));

        try {
            boolean created = userDAO.createUser(user);
            if (created) {
                auditLogService.logEvent("USER_CREATED", "USER", "USER", user.getUserId(), actor, "Created user: " + user.getUsername(), "127.0.0.1", "SUCCESS");
            } else {
                auditLogService.logEvent("USER_CREATE_FAILED", "USER", "USER", null, actor, "Failed to create user: " + user.getUsername(), "127.0.0.1", "FAILED");
            }
            return created;
        } catch (SQLException e) {
            System.err.println("Lỗi khi tạo user: " + e.getMessage());
            auditLogService.logEvent("USER_CREATE_FAILED", "USER", "USER", null, actor, "Exception creating user: " + user.getUsername(), "127.0.0.1", "FAILED");
            return false;
        }
    }

    public boolean updateUser(User user) {
        return updateUser(user, null);
    }

    public boolean updateUser(User user, User actor) {
        if (user == null) {
            return false;
        }

        if (!PermissionService.canManageUsers(actor) && actor != null) {
            return false;
        }

        String normalizedRole = UserRole.normalize(user.getRole());
        user.setRole(normalizedRole);

        if (user.getPassword() != null && !user.getPassword().isBlank() && !PasswordUtil.isHashedPassword(user.getPassword())) {
            user.setPassword(PasswordUtil.hashPassword(user.getPassword()));
        }

        try {
            boolean updated = userDAO.updateUser(user);
            if (updated) {
                auditLogService.logEvent("USER_UPDATED", "USER", "USER", user.getUserId(), actor, "Updated user: " + user.getUsername(), "127.0.0.1", "SUCCESS");
            } else {
                auditLogService.logEvent("USER_UPDATE_FAILED", "USER", "USER", user.getUserId(), actor, "Failed to update user: " + user.getUsername(), "127.0.0.1", "FAILED");
            }
            return updated;
        } catch (SQLException e) {
            System.err.println("Lỗi khi cập nhật user: " + e.getMessage());
            auditLogService.logEvent("USER_UPDATE_FAILED", "USER", "USER", user.getUserId(), actor, "Exception updating user: " + user.getUsername(), "127.0.0.1", "FAILED");
            return false;
        }
    }

    public boolean deleteUser(int userId) {
        return deleteUser(userId, null);
    }

    public boolean deleteUser(int userId, User actor) {
        if (!PermissionService.canManageUsers(actor) && actor != null) {
            return false;
        }

        try {
            boolean deleted = userDAO.deleteUser(userId);
            if (deleted) {
                auditLogService.logEvent("USER_DELETED", "USER", "USER", userId, actor, "Deleted user id " + userId, "127.0.0.1", "SUCCESS");
            } else {
                auditLogService.logEvent("USER_DELETE_FAILED", "USER", "USER", userId, actor, "Failed to delete user id " + userId, "127.0.0.1", "FAILED");
            }
            return deleted;
        } catch (SQLException e) {
            System.err.println("Lỗi khi xóa user: " + e.getMessage());
            auditLogService.logEvent("USER_DELETE_FAILED", "USER", "USER", userId, actor, "Exception deleting user id " + userId, "127.0.0.1", "FAILED");
            return false;
        }
    }
}
