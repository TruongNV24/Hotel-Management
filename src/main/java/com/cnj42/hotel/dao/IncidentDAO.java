package com.cnj42.hotel.dao;

import com.cnj42.hotel.model.Incident;
import com.cnj42.hotel.utils.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class IncidentDAO {
    private static final String SELECT = "SELECT i.*, g.full_name AS guest_name, " +
            "CONCAT(ru.full_name, ' (', ru.username, ')') AS reported_by_name, " +
            "CONCAT(au.full_name, ' (', au.username, ')') AS assigned_to_name " +
            "FROM incidents i LEFT JOIN guests g ON g.guest_id = i.guest_id " +
            "LEFT JOIN users ru ON ru.user_id = i.reported_by " +
            "LEFT JOIN users au ON au.user_id = i.assigned_to ";

    public int create(Incident incident) throws SQLException {
        String sql = "INSERT INTO incidents (incident_type, title, description, priority, status, guest_id, reservation_id, stay_id, invoice_id, reported_by, assigned_to, reported_at, resolution) " +
                "VALUES (?, ?, ?, ?, 'OPEN', ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?)";
        try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindCreate(statement, incident);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    public Incident findById(int incidentId) throws SQLException {
        try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(SELECT + " WHERE i.incident_id = ?")) {
            statement.setInt(1, incidentId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public List<Incident> search(String keyword, String type, String priority, String status) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT).append(" WHERE 1=1");
        List<String> params = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (i.title LIKE ? OR i.description LIKE ?)");
            String value = "%" + keyword.trim() + "%";
            params.add(value);
            params.add(value);
        }
        if (type != null && !type.isBlank() && !"ALL".equalsIgnoreCase(type)) {
            sql.append(" AND i.incident_type = ?");
            params.add(type);
        }
        if (priority != null && !priority.isBlank() && !"ALL".equalsIgnoreCase(priority)) {
            sql.append(" AND i.priority = ?");
            params.add(priority);
        }
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            sql.append(" AND i.status = ?");
            params.add(status);
        }
        sql.append(" ORDER BY FIELD(i.priority, 'CRITICAL', 'HIGH', 'MEDIUM', 'LOW'), i.reported_at DESC");

        try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) statement.setString(i + 1, params.get(i));
            try (ResultSet rs = statement.executeQuery()) {
                List<Incident> incidents = new ArrayList<>();
                while (rs.next()) incidents.add(mapRow(rs));
                return incidents;
            }
        }
    }

    public boolean update(Incident incident) throws SQLException {
        String sql = "UPDATE incidents SET incident_type = ?, title = ?, description = ?, priority = ?, guest_id = ?, reservation_id = ?, stay_id = ?, invoice_id = ?, assigned_to = ?, resolution = ?, status = ?, resolved_at = ?, closed_at = ?, updated_at = CURRENT_TIMESTAMP WHERE incident_id = ?";
        try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, incident.getIncidentType());
            statement.setString(2, incident.getTitle());
            statement.setString(3, incident.getDescription());
            statement.setString(4, incident.getPriority());
            setNullableInt(statement, 5, incident.getGuestId());
            setNullableInt(statement, 6, incident.getReservationId());
            setNullableInt(statement, 7, incident.getStayId());
            setNullableInt(statement, 8, incident.getInvoiceId());
            setNullableInt(statement, 9, incident.getAssignedTo());
            statement.setString(10, incident.getResolution());
            statement.setString(11, incident.getStatus());
            setNullableTimestamp(statement, 12, incident.getResolvedAt());
            setNullableTimestamp(statement, 13, incident.getClosedAt());
            statement.setInt(14, incident.getIncidentId());
            return statement.executeUpdate() > 0;
        }
    }

    public boolean exists(String table, int id) throws SQLException {
        if (!List.of("guests", "reservations", "stays", "invoices", "users").contains(table)) {
            throw new IllegalArgumentException("Unsupported incident relation");
        }
        String key = table.substring(0, table.length() - 1) + "_id";
        if ("users".equals(table)) key = "user_id";
        try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM " + table + " WHERE " + key + " = ?")) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void bindCreate(PreparedStatement statement, Incident incident) throws SQLException {
        statement.setString(1, incident.getIncidentType());
        statement.setString(2, incident.getTitle());
        statement.setString(3, incident.getDescription());
        statement.setString(4, incident.getPriority());
        setNullableInt(statement, 5, incident.getGuestId());
        setNullableInt(statement, 6, incident.getReservationId());
        setNullableInt(statement, 7, incident.getStayId());
        setNullableInt(statement, 8, incident.getInvoiceId());
        setNullableInt(statement, 9, incident.getReportedBy());
        setNullableInt(statement, 10, incident.getAssignedTo());
        statement.setString(11, incident.getResolution());
    }

    private Incident mapRow(ResultSet rs) throws SQLException {
        Incident incident = new Incident();
        incident.setIncidentId(rs.getInt("incident_id"));
        incident.setIncidentType(rs.getString("incident_type"));
        incident.setTitle(rs.getString("title"));
        incident.setDescription(rs.getString("description"));
        incident.setPriority(rs.getString("priority"));
        incident.setStatus(rs.getString("status"));
        incident.setGuestId(nullableInt(rs, "guest_id"));
        incident.setReservationId(nullableInt(rs, "reservation_id"));
        incident.setStayId(nullableInt(rs, "stay_id"));
        incident.setInvoiceId(nullableInt(rs, "invoice_id"));
        incident.setReportedBy(nullableInt(rs, "reported_by"));
        incident.setAssignedTo(nullableInt(rs, "assigned_to"));
        incident.setGuestName(rs.getString("guest_name"));
        incident.setReportedByName(rs.getString("reported_by_name"));
        incident.setAssignedToName(rs.getString("assigned_to_name"));
        incident.setResolution(rs.getString("resolution"));
        incident.setReportedAt(toLocalDateTime(rs.getTimestamp("reported_at")));
        incident.setResolvedAt(toLocalDateTime(rs.getTimestamp("resolved_at")));
        incident.setClosedAt(toLocalDateTime(rs.getTimestamp("closed_at")));
        incident.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        incident.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        return incident;
    }

    private Integer nullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    private void setNullableInt(PreparedStatement statement, int index, Integer value) throws SQLException {
        if (value == null) statement.setNull(index, Types.INTEGER); else statement.setInt(index, value);
    }

    private void setNullableTimestamp(PreparedStatement statement, int index, java.time.LocalDateTime value) throws SQLException {
        if (value == null) statement.setNull(index, Types.TIMESTAMP); else statement.setTimestamp(index, Timestamp.valueOf(value));
    }
}
