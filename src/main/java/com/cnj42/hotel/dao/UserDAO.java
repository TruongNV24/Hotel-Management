package com.cnj42.hotel.dao;

import com.cnj42.hotel.model.User;
import com.cnj42.hotel.utils.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {

    private static final String LOGIN_SQL = """
            SELECT user_id,
                   username,
                   password,
                   full_name,
                   role,
                   phone,
                   email,
                   status
            FROM users
            WHERE username = ?
            LIMIT 1
            """;

    public User findByUsername(String username) {

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(LOGIN_SQL)) {

            statement.setString(1, username);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return mapUser(resultSet);
                }
            }

        } catch (SQLException e) {
            System.err.println("Lỗi khi tìm user: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    private User mapUser(ResultSet resultSet) throws SQLException {

        User user = new User();

        user.setUserId(resultSet.getInt("user_id"));
        user.setUsername(resultSet.getString("username"));
        user.setPassword(resultSet.getString("password"));
        user.setFullName(resultSet.getString("full_name"));
        user.setRole(resultSet.getString("role"));
        user.setPhone(resultSet.getString("phone"));
        user.setEmail(resultSet.getString("email"));
        user.setStatus(resultSet.getString("status"));

        return user;
    }
}