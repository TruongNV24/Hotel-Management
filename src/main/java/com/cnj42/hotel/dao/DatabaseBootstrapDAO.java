package com.cnj42.hotel.dao;

import com.cnj42.hotel.utils.DBConnection;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

public class DatabaseBootstrapDAO {

    public void ensureReady() {
        try (Connection connection = DBConnection.getConnection()) {
            connection.setCatalog("hotel_management");
            boolean hasUsers = tableExists(connection, "users");
            boolean hasRooms = tableExists(connection, "rooms");
            boolean hasReservations = tableExists(connection, "reservations");
            if (!hasUsers || !hasRooms || !hasReservations) {
                bootstrapFromSqlFile();
                return;
            }

            ensureRoomImageColumn(connection);
            ensureRoomAmenitiesColumn(connection);
            ensureCleaningMaintenanceType(connection);
            if (countRows(connection, "users") == 0 || countRows(connection, "rooms") == 0
                    || countRows(connection, "reservations") == 0) {
                bootstrapFromSqlFile();
            }
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("Unknown database")) {
                bootstrapFromSqlFile();
                return;
            }
            System.err.println("Database bootstrap check failed: " + e.getMessage());
        }
    }

    private void ensureRoomImageColumn(Connection connection) {
        ensureJsonColumn(connection, "image_path", "AFTER status");
    }

    private void ensureRoomAmenitiesColumn(Connection connection) {
        ensureJsonColumn(connection, "amenities", "AFTER image_path");
    }

    private void ensureJsonColumn(Connection connection, String column, String placement) {
        String sql = "SELECT data_type FROM information_schema.columns " +
                "WHERE table_schema = DATABASE() AND table_name = 'rooms' AND column_name = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, column);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    executeDdl(connection, "ALTER TABLE rooms ADD COLUMN " + column + " JSON " + placement,
                            "failed to add " + column + " column");
                } else if (!isJsonColumnType(connection, resultSet.getString("data_type"))) {
                    executeDdl(connection, "ALTER TABLE rooms MODIFY COLUMN " + column + " JSON",
                            "failed to modify " + column + " column to JSON");
                }
            }
        } catch (SQLException e) {
            System.err.println("Warning: unable to inspect/alter " + column + " column: " + e.getMessage());
        }
    }

    private void ensureCleaningMaintenanceType(Connection connection) {
        try {
            if (!tableExists(connection, "maintenance_requests")) return;
            String sql = "SELECT constraint_name, check_clause FROM information_schema.check_constraints " +
                    "WHERE constraint_schema = DATABASE() AND table_name = 'maintenance_requests' " +
                    "AND check_clause LIKE '%maintenance_type%'";
            String constraintName = null;
            String checkClause = null;
            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    constraintName = resultSet.getString("constraint_name");
                    checkClause = resultSet.getString("check_clause");
                }
            }
            if (checkClause != null && checkClause.toUpperCase(Locale.ROOT).contains("CLEANING")) return;

            try (Statement statement = connection.createStatement()) {
                if (constraintName != null) {
                    String databaseProduct = connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
                    String safeName = constraintName.replace("`", "");
                    String drop = databaseProduct.contains("maria")
                            ? "ALTER TABLE maintenance_requests DROP CONSTRAINT `" + safeName + "`"
                            : "ALTER TABLE maintenance_requests DROP CHECK `" + safeName + "`";
                    statement.executeUpdate(drop);
                }
                statement.executeUpdate("ALTER TABLE maintenance_requests ADD CONSTRAINT chk_maintenance_type " +
                        "CHECK (maintenance_type IN ('PREVENTIVE', 'CORRECTIVE', 'EMERGENCY', 'INSPECTION', 'CLEANING', 'OTHER'))");
            }
        } catch (SQLException e) {
            System.err.println("Warning: unable to enable CLEANING maintenance type: " + e.getMessage());
        }
    }

    private boolean isJsonColumnType(Connection connection, String dataType) throws SQLException {
        if ("json".equalsIgnoreCase(dataType)) return true;
        return "longtext".equalsIgnoreCase(dataType)
                && connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT).contains("maria");
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    private int countRows(Connection connection, String tableName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM " + tableName);
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        }
    }

    private void bootstrapFromSqlFile() {
        Path sqlPath = Paths.get("database", "hotel_management.sql");
        if (!Files.exists(sqlPath)) {
            System.err.println("SQL bootstrap file not found: " + sqlPath.toAbsolutePath());
            return;
        }
        try {
            String sql = Files.readString(sqlPath, StandardCharsets.UTF_8);
            String cleaned = sql.replaceAll("(?s)/\\*.*?\\*/", "")
                    .replaceAll("--.*", "")
                    .replaceAll("(?m)^\\s*USE\\s+.*;?\\s*$", "");
            String[] statements = cleaned.split(";");
            try (Connection connection = DBConnection.getConnection();
                 Statement statement = connection.createStatement()) {
                statement.execute("CREATE DATABASE IF NOT EXISTS hotel_management");
                statement.execute("USE hotel_management");
                for (String part : statements) {
                    String trimmed = part.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("CREATE DATABASE")
                            || trimmed.startsWith("DROP DATABASE") || trimmed.startsWith("USE ")) continue;
                    if (trimmed.startsWith("INSERT") || trimmed.startsWith("CREATE TABLE")
                            || trimmed.startsWith("CREATE VIEW") || trimmed.startsWith("ALTER")
                            || trimmed.startsWith("CREATE INDEX") || trimmed.startsWith("INSERT INTO")) {
                        statement.execute(trimmed);
                    }
                }
            }
        } catch (IOException | SQLException e) {
            System.err.println("Failed to bootstrap database from SQL file: " + e.getMessage());
        }
    }

    private void executeDdl(Connection connection, String sql, String warning) {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            System.err.println("Warning: " + warning + ": " + e.getMessage());
        }
    }
}