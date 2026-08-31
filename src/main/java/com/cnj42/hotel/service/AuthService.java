package com.cnj42.hotel.service;

import com.cnj42.hotel.dao.UserDAO;
import com.cnj42.hotel.model.User;

public class AuthService {

    private final UserDAO userDAO;

    public AuthService() {
        this.userDAO = new UserDAO();
    }

    public User login(String username, String password) {

        if (username == null || username.isBlank()) {
            return null;
        }

        if (password == null || password.isBlank()) {
            return null;
        }

        User user = userDAO.findByUsername(username.trim());

        if (user == null) {
            return null;
        }

        if (!password.equals(user.getPassword())) {
            return null;
        }

        if (!"ACTIVE".equals(user.getStatus())) {
            return null;
        }

        return user;
    }
}