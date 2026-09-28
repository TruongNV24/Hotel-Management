package com.cnj42.hotel.dao;

import com.cnj42.hotel.model.Service;
import com.cnj42.hotel.utils.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceDAO {

    public List<Service> search(String keyword, String status) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT service_id, service_name, unit, description, price, status, created_at, updated_at FROM services WHERE 1=1");
        List<String> params = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (service_name LIKE ? OR description LIKE ?)");
            String value = "%" + keyword.trim() + "%";
            params.add(value);
            params.add(value);
        }
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            sql.append(" AND status = ?");
            params.add(status.trim().toUpperCase());
        }
        sql.append(" ORDER BY service_name");

        try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                statement.setString(i + 1, params.get(i));
            }
            try (ResultSet rs = statement.executeQuery()) {
                List<Service> services = new ArrayList<>();
                while (rs.next()) {
                    services.add(mapRow(rs));
                }
                return services;
            }
        }
    }

    public Service findById(int serviceId) throws SQLException {
        try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(
                "SELECT service_id, service_name, unit, description, price, status, created_at, updated_at FROM services WHERE service_id = ?")) {
            statement.setInt(1, serviceId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public boolean existsByName(String name, Integer exceptId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM services WHERE LOWER(service_name) = LOWER(?) AND (? IS NULL OR service_id <> ?)";
        try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            if (exceptId == null) {
                statement.setNull(2, Types.INTEGER);
                statement.setNull(3, Types.INTEGER);
            } else {
                statement.setInt(2, exceptId);
                statement.setInt(3, exceptId);
            }
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public boolean create(Service service) throws SQLException {
        String sql = "INSERT INTO services (service_name, unit, price, status, description) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindService(statement, service);
            if (statement.executeUpdate() == 0) return false;
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) service.setServiceId(keys.getInt(1));
            }
            return true;
        }
    }

    public boolean update(Service service) throws SQLException {
        String sql = "UPDATE services SET service_name = ?, unit = ?, price = ?, status = ?, description = ?, updated_at = CURRENT_TIMESTAMP WHERE service_id = ?";
        try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            bindService(statement, service);
            statement.setInt(6, service.getServiceId());
            return statement.executeUpdate() > 0;
        }
    }

    public boolean hasUsage(int serviceId) throws SQLException {
        try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM service_usages WHERE service_id = ?")) {
            statement.setInt(1, serviceId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public boolean setStatus(int serviceId, String status) throws SQLException {
        try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(
                "UPDATE services SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE service_id = ?")) {
            statement.setString(1, status);
            statement.setInt(2, serviceId);
            return statement.executeUpdate() > 0;
        }
    }

    private void bindService(PreparedStatement statement, Service service) throws SQLException {
        statement.setString(1, service.getServiceName());
        statement.setString(2, service.getUnit());
        statement.setDouble(3, service.getPrice());
        statement.setString(4, service.getStatus());
        statement.setString(5, service.getDescription());
    }

    private Service mapRow(ResultSet rs) throws SQLException {
        Service service = new Service();
        service.setServiceId(rs.getInt("service_id"));
        service.setServiceName(rs.getString("service_name"));
        service.setUnit(rs.getString("unit"));
        service.setDescription(rs.getString("description"));
        service.setPrice(rs.getDouble("price"));
        service.setStatus(rs.getString("status"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (createdAt != null) service.setCreatedAt(createdAt.toLocalDateTime());
        if (updatedAt != null) service.setUpdatedAt(updatedAt.toLocalDateTime());
        return service;
    }
}
