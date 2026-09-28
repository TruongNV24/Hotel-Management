package com.cnj42.hotel.utils;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBConnection {

    private static final Properties PROPERTIES = loadProperties();
    private static final String URL = firstNonBlank(
            System.getenv("DB_URL"),
            PROPERTIES.getProperty("db.url"),
            "jdbc:mysql://localhost:3306/hotel_management?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8"
    );
    private static final String USER = firstNonBlank(
            System.getenv("DB_USERNAME"),
            System.getenv("DB_USER"),
            PROPERTIES.getProperty("db.user"),
            "root"
    );
    private static final String PASSWORD = firstNonBlank(
            System.getenv("DB_PASSWORD"),
            PROPERTIES.getProperty("db.password"),
            ""
    );

    private DBConnection() {
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream input = DBConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException ignored) {
            // Ignore missing configuration file and fallback to env vars / defaults.
        }
        return properties;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}