package com.cnj42.hotel.service;

import com.cnj42.hotel.dao.UserDAO;
import com.cnj42.hotel.model.User;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserService {

    private final UserDAO userDAO;

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
        try {
            return userDAO.createUser(user);
        } catch (SQLException e) {
            System.err.println("Lỗi khi tạo user: " + e.getMessage());
            return false;
        }
    }

    public boolean updateUser(User user) {
        try {
            return userDAO.updateUser(user);
        } catch (SQLException e) {
            System.err.println("Lỗi khi cập nhật user: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteUser(int userId) {
        try {
            return userDAO.deleteUser(userId);
        } catch (SQLException e) {
            System.err.println("Lỗi khi xóa user: " + e.getMessage());
            return false;
        }
    }
}
