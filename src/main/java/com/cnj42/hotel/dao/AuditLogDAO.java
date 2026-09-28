package com.cnj42.hotel.dao;

import com.cnj42.hotel.model.AuditLog;
import com.cnj42.hotel.utils.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class AuditLogDAO {

    public boolean insert(AuditLog log) throws SQLException {
        String sql = "INSERT INTO audit_logs (action, module, entity_type, entity_id, actor_user_id, actor_username, actor_role, details, ip_address, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, log.getAction());
            statement.setString(2, log.getModule());
            statement.setString(3, log.getEntityType());
            if (log.getEntityId() != null) {
                statement.setInt(4, log.getEntityId());
            } else {
                statement.setNull(4, java.sql.Types.INTEGER);
            }
            if (log.getActorUserId() != null) {
                statement.setInt(5, log.getActorUserId());
            } else {
                statement.setNull(5, java.sql.Types.INTEGER);
            }
            statement.setString(6, log.getActorUsername());
            statement.setString(7, log.getActorRole());
            statement.setString(8, log.getDetails());
            statement.setString(9, log.getIpAddress());
            statement.setString(10, log.getStatus());
            return statement.executeUpdate() > 0;
        }
    }

    public List<AuditLog> getRecentLogs(int limit) throws SQLException {
        String sql = "SELECT * FROM audit_logs ORDER BY created_at DESC LIMIT ?";
        List<AuditLog> logs = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapRow(rs));
                }
            }
        }

        return logs;
    }

    public List<AuditLog> searchLogs(String keyword, String action, String module, String status) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM audit_logs WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (actor_username LIKE ? OR details LIKE ? OR entity_type LIKE ?)");
            String q = "%" + keyword.trim() + "%";
            params.add(q);
            params.add(q);
            params.add(q);
        }
        if (action != null && !action.isBlank()) {
            sql.append(" AND action = ?");
            params.add(action.trim());
        }
        if (module != null && !module.isBlank()) {
            sql.append(" AND module = ?");
            params.add(module.trim());
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            params.add(status.trim());
        }

        sql.append(" ORDER BY created_at DESC LIMIT 200");

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                statement.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = statement.executeQuery()) {
                List<AuditLog> logs = new ArrayList<>();
                while (rs.next()) {
                    logs.add(mapRow(rs));
                }
                return logs;
            }
        }
    }

    private AuditLog mapRow(ResultSet rs) throws SQLException {
        AuditLog log = new AuditLog();
        log.setAuditLogId(rs.getInt("audit_log_id"));
        log.setAction(rs.getString("action"));
        log.setModule(rs.getString("module"));
        log.setEntityType(rs.getString("entity_type"));
        int entityId = rs.getInt("entity_id");
        log.setEntityId(rs.wasNull() ? null : entityId);
        int actorId = rs.getInt("actor_user_id");
        log.setActorUserId(rs.wasNull() ? null : actorId);
        log.setActorUsername(rs.getString("actor_username"));
        log.setActorRole(rs.getString("actor_role"));
        log.setDetails(rs.getString("details"));
        log.setIpAddress(rs.getString("ip_address"));
        log.setStatus(rs.getString("status"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            log.setCreatedAt(createdAt.toLocalDateTime());
        }
        return log;
    }
}
