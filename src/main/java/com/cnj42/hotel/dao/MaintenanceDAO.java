package com.cnj42.hotel.dao;

import com.cnj42.hotel.model.Maintenance;
import com.cnj42.hotel.utils.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MaintenanceDAO {
    private static final String SELECT = "SELECT m.*, r.room_number, " +
            "CONCAT(ru.full_name, ' (', ru.username, ')') AS reported_by_name, " +
            "CONCAT(au.full_name, ' (', au.username, ')') AS assigned_to_name " +
            "FROM maintenance_requests m JOIN rooms r ON r.room_id = m.room_id " +
            "LEFT JOIN users ru ON ru.user_id = m.reported_by " +
            "LEFT JOIN users au ON au.user_id = m.assigned_to ";

    public int create(Maintenance maintenance) throws SQLException {
        String sql = "INSERT INTO maintenance_requests (room_id, title, description, maintenance_type, priority, status, reported_by, assigned_to, expected_end_at, resolution, notes) " +
                "VALUES (?, ?, ?, ?, ?, 'OPEN', ?, ?, ?, ?, ?)";
        try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, maintenance.getRoomId());
            statement.setString(2, maintenance.getTitle());
            statement.setString(3, maintenance.getDescription());
            statement.setString(4, maintenance.getMaintenanceType());
            statement.setString(5, maintenance.getPriority());
            statement.setInt(6, maintenance.getReportedBy());
            setNullableInt(statement, 7, maintenance.getAssignedTo());
            setNullableTimestamp(statement, 8, maintenance.getExpectedEndAt());
            statement.setString(9, maintenance.getResolution());
            statement.setString(10, maintenance.getNotes());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    public Maintenance findById(int maintenanceId) throws SQLException {
        try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(SELECT + " WHERE m.maintenance_id = ?")) {
            statement.setInt(1, maintenanceId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public List<Maintenance> search(Integer roomId, String type, String priority, String status, String keyword) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT).append(" WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (roomId != null) { sql.append(" AND m.room_id = ?"); params.add(roomId); }
        if (type != null && !type.isBlank() && !"ALL".equalsIgnoreCase(type)) { sql.append(" AND m.maintenance_type = ?"); params.add(type); }
        if (priority != null && !priority.isBlank() && !"ALL".equalsIgnoreCase(priority)) { sql.append(" AND m.priority = ?"); params.add(priority); }
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) { sql.append(" AND m.status = ?"); params.add(status); }
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (m.title LIKE ? OR m.description LIKE ? OR r.room_number LIKE ?)");
            String value = "%" + keyword.trim() + "%";
            params.add(value); params.add(value); params.add(value);
        }
        sql.append(" ORDER BY FIELD(m.priority, 'CRITICAL', 'HIGH', 'MEDIUM', 'LOW'), m.created_at DESC");
        try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) statement.setObject(i + 1, params.get(i));
            try (ResultSet rs = statement.executeQuery()) {
                List<Maintenance> records = new ArrayList<>();
                while (rs.next()) records.add(mapRow(rs));
                return records;
            }
        }
    }

    public boolean update(Maintenance maintenance) throws SQLException {
        String sql = "UPDATE maintenance_requests SET title = ?, description = ?, maintenance_type = ?, priority = ?, assigned_to = ?, expected_end_at = ?, resolution = ?, notes = ?, updated_at = CURRENT_TIMESTAMP WHERE maintenance_id = ?";
        try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, maintenance.getTitle());
            statement.setString(2, maintenance.getDescription());
            statement.setString(3, maintenance.getMaintenanceType());
            statement.setString(4, maintenance.getPriority());
            setNullableInt(statement, 5, maintenance.getAssignedTo());
            setNullableTimestamp(statement, 6, maintenance.getExpectedEndAt());
            statement.setString(7, maintenance.getResolution());
            statement.setString(8, maintenance.getNotes());
            statement.setInt(9, maintenance.getMaintenanceId());
            return statement.executeUpdate() > 0;
        }
    }

    public boolean start(int maintenanceId) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            int roomId;
            try (PreparedStatement statement = connection.prepareStatement("SELECT room_id, status, expected_end_at FROM maintenance_requests WHERE maintenance_id = ? FOR UPDATE")) {
                statement.setInt(1, maintenanceId);
                try (ResultSet rs = statement.executeQuery()) {
                    if (!rs.next() || !Maintenance.OPEN.equals(rs.getString("status"))) { connection.rollback(); return false; }
                    roomId = rs.getInt("room_id");
                    Timestamp expectedEndAt = rs.getTimestamp("expected_end_at");
                    if (expectedEndAt != null && expectedEndAt.before(new Timestamp(System.currentTimeMillis()))) { connection.rollback(); return false; }
                }
            }
            String roomStatus;
            try (PreparedStatement statement = connection.prepareStatement("SELECT status FROM rooms WHERE room_id = ? FOR UPDATE")) {
                statement.setInt(1, roomId);
                try (ResultSet rs = statement.executeQuery()) {
                    if (!rs.next()) { connection.rollback(); return false; }
                    roomStatus = rs.getString(1);
                }
            }
            if (!"AVAILABLE".equals(roomStatus) && !"CLEANING".equals(roomStatus)) { connection.rollback(); return false; }
            if (hasActiveReservationOrStay(connection, roomId) || hasActiveMaintenance(connection, roomId, maintenanceId)) { connection.rollback(); return false; }
            try (PreparedStatement statement = connection.prepareStatement("UPDATE maintenance_requests SET status = 'IN_PROGRESS', started_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE maintenance_id = ? AND status = 'OPEN'")) {
                statement.setInt(1, maintenanceId);
                if (statement.executeUpdate() == 0) { connection.rollback(); return false; }
            }
            try (PreparedStatement statement = connection.prepareStatement("UPDATE rooms SET status = 'MAINTENANCE' WHERE room_id = ?")) {
                statement.setInt(1, roomId);
                if (statement.executeUpdate() == 0) { connection.rollback(); return false; }
            }
            connection.commit();
            return true;
        }
    }

    public boolean complete(int maintenanceId, String resolution) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            int roomId;
            Timestamp startedAt;
            try (PreparedStatement statement = connection.prepareStatement("SELECT room_id, started_at, status FROM maintenance_requests WHERE maintenance_id = ? FOR UPDATE")) {
                statement.setInt(1, maintenanceId);
                try (ResultSet rs = statement.executeQuery()) {
                    if (!rs.next() || !Maintenance.IN_PROGRESS.equals(rs.getString("status")) || rs.getTimestamp("started_at") == null) { connection.rollback(); return false; }
                    roomId = rs.getInt("room_id");
                    startedAt = rs.getTimestamp("started_at");
                }
            }
            String roomStatus;
            try (PreparedStatement statement = connection.prepareStatement("SELECT status FROM rooms WHERE room_id = ? FOR UPDATE")) {
                statement.setInt(1, roomId);
                try (ResultSet rs = statement.executeQuery()) {
                    if (!rs.next()) { connection.rollback(); return false; }
                    roomStatus = rs.getString(1);
                }
            }
            if (!"MAINTENANCE".equals(roomStatus)) { connection.rollback(); return false; }
            try (PreparedStatement statement = connection.prepareStatement("UPDATE maintenance_requests SET status = 'COMPLETED', completed_at = CURRENT_TIMESTAMP, duration_minutes = TIMESTAMPDIFF(MINUTE, started_at, CURRENT_TIMESTAMP), resolution = ?, updated_at = CURRENT_TIMESTAMP WHERE maintenance_id = ? AND status = 'IN_PROGRESS'")) {
                statement.setString(1, resolution); statement.setInt(2, maintenanceId);
                if (statement.executeUpdate() == 0) { connection.rollback(); return false; }
            }
            updateRoomAfterMaintenance(connection, roomId);
            connection.commit();
            return true;
        }
    }

    public boolean cancel(int maintenanceId) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            int roomId; String status;
            try (PreparedStatement statement = connection.prepareStatement("SELECT room_id, status FROM maintenance_requests WHERE maintenance_id = ? FOR UPDATE")) {
                statement.setInt(1, maintenanceId);
                try (ResultSet rs = statement.executeQuery()) {
                    if (!rs.next()) { connection.rollback(); return false; }
                    roomId = rs.getInt("room_id"); status = rs.getString("status");
                }
            }
            if (!Maintenance.OPEN.equals(status) && !Maintenance.IN_PROGRESS.equals(status)) { connection.rollback(); return false; }
            try (PreparedStatement statement = connection.prepareStatement("UPDATE maintenance_requests SET status = 'CANCELLED', updated_at = CURRENT_TIMESTAMP WHERE maintenance_id = ?")) {
                statement.setInt(1, maintenanceId); statement.executeUpdate();
            }
            if (Maintenance.IN_PROGRESS.equals(status)) updateRoomAfterMaintenance(connection, roomId);
            connection.commit();
            return true;
        }
    }

    public boolean roomExists(int roomId) throws SQLException { return exists("rooms", "room_id", roomId); }
    public boolean userExists(int userId) throws SQLException { return exists("users", "user_id", userId); }

    private boolean hasActiveMaintenance(Connection connection, int roomId, int excludedId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM maintenance_requests WHERE room_id = ? AND status = 'IN_PROGRESS' AND maintenance_id <> ? LIMIT 1")) {
            statement.setInt(1, roomId); statement.setInt(2, excludedId);
            try (ResultSet rs = statement.executeQuery()) { return rs.next(); }
        }
    }

    private boolean hasActiveReservationOrStay(Connection connection, int roomId) throws SQLException {
        String sql = "SELECT 1 FROM reservations WHERE room_id = ? AND status IN ('PENDING', 'CONFIRMED', 'CHECKED_IN') LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, roomId);
            try (ResultSet rs = statement.executeQuery()) { return rs.next(); }
        }
    }

    private void updateRoomAfterMaintenance(Connection connection, int roomId) throws SQLException {
        String nextStatus;
        if (exists(connection, "stays", "room_id", roomId, "status", "CHECKED_IN")) nextStatus = "OCCUPIED";
        else if (existsReservation(connection, roomId)) nextStatus = "RESERVED";
        else nextStatus = "AVAILABLE";
        try (PreparedStatement statement = connection.prepareStatement("UPDATE rooms SET status = ? WHERE room_id = ? AND status = 'MAINTENANCE'")) {
            statement.setString(1, nextStatus); statement.setInt(2, roomId); statement.executeUpdate();
        }
    }

    private boolean existsReservation(Connection connection, int roomId) throws SQLException {
        return exists(connection, "reservations", "room_id", roomId, "status", "PENDING", "CONFIRMED");
    }

    private boolean exists(String table, String column, int id) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) { return exists(connection, table, column, id); }
    }

    private boolean exists(Connection connection, String table, String column, int id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM " + table + " WHERE " + column + " = ? LIMIT 1")) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) { return rs.next(); }
        }
    }

    private boolean exists(Connection connection, String table, String column, int id, String secondColumn, String... values) throws SQLException {
        String placeholders = String.join(",", java.util.Collections.nCopies(values.length, "?"));
        try (PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM " + table + " WHERE " + column + " = ? AND " + secondColumn + " IN (" + placeholders + ") LIMIT 1")) {
            statement.setInt(1, id);
            for (int i = 0; i < values.length; i++) statement.setString(i + 2, values[i]);
            try (ResultSet rs = statement.executeQuery()) { return rs.next(); }
        }
    }

    private Maintenance mapRow(ResultSet rs) throws SQLException {
        Maintenance record = new Maintenance();
        record.setMaintenanceId(rs.getInt("maintenance_id")); record.setRoomId(rs.getInt("room_id")); record.setRoomNumber(rs.getString("room_number"));
        record.setTitle(rs.getString("title")); record.setDescription(rs.getString("description")); record.setMaintenanceType(rs.getString("maintenance_type"));
        record.setPriority(rs.getString("priority")); record.setStatus(rs.getString("status")); record.setReportedBy(nullableInt(rs, "reported_by")); record.setAssignedTo(nullableInt(rs, "assigned_to"));
        record.setReportedByName(rs.getString("reported_by_name")); record.setAssignedToName(rs.getString("assigned_to_name"));
        record.setStartedAt(toLocal(rs.getTimestamp("started_at"))); record.setExpectedEndAt(toLocal(rs.getTimestamp("expected_end_at"))); record.setCompletedAt(toLocal(rs.getTimestamp("completed_at")));
        record.setDurationMinutes(nullableInt(rs, "duration_minutes")); record.setResolution(rs.getString("resolution")); record.setNotes(rs.getString("notes"));
        record.setCreatedAt(toLocal(rs.getTimestamp("created_at"))); record.setUpdatedAt(toLocal(rs.getTimestamp("updated_at")));
        return record;
    }

    private Integer nullableInt(ResultSet rs, String column) throws SQLException { int value = rs.getInt(column); return rs.wasNull() ? null : value; }
    private LocalDateTime toLocal(Timestamp value) { return value == null ? null : value.toLocalDateTime(); }
    private void setNullableInt(PreparedStatement statement, int index, Integer value) throws SQLException { if (value == null) statement.setNull(index, Types.INTEGER); else statement.setInt(index, value); }
    private void setNullableTimestamp(PreparedStatement statement, int index, LocalDateTime value) throws SQLException { if (value == null) statement.setNull(index, Types.TIMESTAMP); else statement.setTimestamp(index, Timestamp.valueOf(value)); }
}
