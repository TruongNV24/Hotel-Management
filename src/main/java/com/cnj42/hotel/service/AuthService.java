package com.cnj42.hotel.service;

import com.cnj42.hotel.dao.UserDAO;
import com.cnj42.hotel.model.User;
import com.cnj42.hotel.model.UserRole;
import com.cnj42.hotel.utils.PasswordUtil;

import java.sql.SQLException;

public class AuthService {

    private final UserDAO userDAO;
    private final AuditLogService auditLogService = new AuditLogService();

    public AuthService() {
        this.userDAO = new UserDAO();
    }

    public User login(String username, String password) throws SQLException {
        if (username == null || username.isBlank()) {
            return null;
        }

        if (password == null || password.isBlank()) {
            return null;
        }

        User user = userDAO.findByUsername(username.trim());
        if (user == null) {
            auditLogService.logEvent("LOGIN_FAILED", "AUTH", "USER", null, null, "Username not found: " + username, "127.0.0.1", "FAILED");
            return null;
        }

        String normalizedRole = UserRole.normalize(user.getRole());
        user.setRole(normalizedRole);

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            auditLogService.logEvent("LOGIN_FAILED", "AUTH", "USER", user.getUserId(), user, "User account inactive", "127.0.0.1", "FAILED");
            return null;
        }

        String storedPassword = user.getPassword();
        if (storedPassword == null || storedPassword.isBlank()) {
            return null;
        }

        if (!PasswordUtil.matches(password, storedPassword)) {
            if (password.equals(storedPassword)) {
                user.setPassword(PasswordUtil.hashPassword(password));
                userDAO.updatePasswordHash(user.getUserId(), user.getPassword());
                auditLogService.logEvent("PASSWORD_MIGRATED", "AUTH", "USER", user.getUserId(), user, "Legacy plaintext password migrated to BCrypt", "127.0.0.1", "SUCCESS");
                auditLogService.logEvent("LOGIN_SUCCESS", "AUTH", "USER", user.getUserId(), user, "Legacy password login succeeded", "127.0.0.1", "SUCCESS");
                return user;
            }
            auditLogService.logEvent("LOGIN_FAILED", "AUTH", "USER", user.getUserId(), user, "Invalid credentials", "127.0.0.1", "FAILED");
            return null;
        }

        auditLogService.logEvent("LOGIN_SUCCESS", "AUTH", "USER", user.getUserId(), user, "Successful authentication", "127.0.0.1", "SUCCESS");
        return user;
    }
}