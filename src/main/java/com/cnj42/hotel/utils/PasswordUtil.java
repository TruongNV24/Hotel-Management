package com.cnj42.hotel.utils;

import org.mindrot.jbcrypt.BCrypt;

public final class PasswordUtil {

    private PasswordUtil() {
    }

    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isBlank()) {
            throw new IllegalArgumentException("Password cannot be blank");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
    }

    public static boolean matches(String plainPassword, String storedPassword) {
        if (plainPassword == null || storedPassword == null) {
            return false;
        }

        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
            return BCrypt.checkpw(plainPassword, storedPassword);
        }

        return plainPassword.equals(storedPassword);
    }

    public static boolean isHashedPassword(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return false;
        }
        return candidate.startsWith("$2a$") || candidate.startsWith("$2b$") || candidate.startsWith("$2y$");
    }
}
