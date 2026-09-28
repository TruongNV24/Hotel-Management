package com.cnj42.hotel.model;

public enum UserRole {
    ADMIN,
    MANAGER,
    EMPLOYEE;

    public static String normalize(String role) {
        if (role == null) {
            return "EMPLOYEE";
        }

        String normalized = role.trim().toUpperCase();
        if ("RECEPTIONIST".equals(normalized)) {
            return "EMPLOYEE";
        }
        if ("EMPLOYEE".equals(normalized)) {
            return "EMPLOYEE";
        }
        if ("MANAGER".equals(normalized)) {
            return "MANAGER";
        }
        if ("ADMIN".equals(normalized)) {
            return "ADMIN";
        }
        return "EMPLOYEE";
    }

    public static String toDbRole(String role) {
        String normalized = normalize(role);
        if ("EMPLOYEE".equals(normalized)) {
            return "RECEPTIONIST";
        }
        return normalized;
    }

    public static boolean isAdmin(User user) {
        return user != null && "ADMIN".equals(normalize(user.getRole()));
    }

    public static boolean isManager(User user) {
        return user != null && "MANAGER".equals(normalize(user.getRole()));
    }

    public static boolean isEmployee(User user) {
        return user != null && "EMPLOYEE".equals(normalize(user.getRole()));
    }

    public static boolean isEmployee(String role) {
        return "EMPLOYEE".equals(normalize(role));
    }
}
