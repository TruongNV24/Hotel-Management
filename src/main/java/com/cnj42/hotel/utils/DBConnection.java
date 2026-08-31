package com.cnj42.hotel.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static final String URL =
            "jdbc:mysql://localhost:3306/hotel_management"
            + "?useSSL=false"
            + "&serverTimezone=Asia/Ho_Chi_Minh"
            + "&characterEncoding=UTF-8";

    private static final String USER = "root";
    private static final String PASSWORD = "";

    private DBConnection() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}