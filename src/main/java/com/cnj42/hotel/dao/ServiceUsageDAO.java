package com.cnj42.hotel.dao;

import com.cnj42.hotel.model.ServiceUsageRecord;
import com.cnj42.hotel.utils.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceUsageDAO {

    public int create(int stayId, int serviceId, int quantity, double unitPrice, String note, Integer createdBy) throws SQLException {
        String sql = "INSERT INTO service_usages (stay_id, service_id, quantity, unit_price, total_amount, used_at, note, created_by) VALUES (?, ?, ?, ?, ?, NOW(), ?, ?)";
        try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, stayId);
            statement.setInt(2, serviceId);
            statement.setInt(3, quantity);
            statement.setDouble(4, unitPrice);
            statement.setDouble(5, quantity * unitPrice);
            statement.setString(6, note);
            if (createdBy == null) statement.setNull(7, Types.INTEGER); else statement.setInt(7, createdBy);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    public List<ServiceUsageRecord> findByStay(int stayId) throws SQLException {
        String sql = "SELECT su.usage_id, su.stay_id, su.service_id, s.service_name, su.quantity, su.unit_price, su.total_amount, su.used_at, su.note, su.created_by " +
                "FROM service_usages su JOIN services s ON s.service_id = su.service_id WHERE su.stay_id = ? ORDER BY su.used_at DESC, su.usage_id DESC";
        try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, stayId);
            try (ResultSet rs = statement.executeQuery()) {
                List<ServiceUsageRecord> records = new ArrayList<>();
                while (rs.next()) records.add(mapRow(rs));
                return records;
            }
        }
    }

    public boolean update(int usageId, int quantity, String note) throws SQLException {
        String sql = "UPDATE service_usages SET quantity = ?, total_amount = quantity * unit_price, note = ? WHERE usage_id = ?";
        try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, quantity);
            statement.setString(2, note);
            statement.setInt(3, usageId);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean delete(int usageId) throws SQLException {
        try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM service_usages WHERE usage_id = ?")) {
            statement.setInt(1, usageId);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean isStayInPaidInvoice(int stayId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM invoices WHERE stay_id = ? AND status = 'PAID'";
        try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, stayId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private ServiceUsageRecord mapRow(ResultSet rs) throws SQLException {
        ServiceUsageRecord record = new ServiceUsageRecord();
        record.setUsageId(rs.getInt("usage_id"));
        record.setStayId(rs.getInt("stay_id"));
        record.setServiceId(rs.getInt("service_id"));
        record.setServiceName(rs.getString("service_name"));
        record.setQuantity(rs.getInt("quantity"));
        record.setUnitPrice(rs.getDouble("unit_price"));
        record.setTotalAmount(rs.getDouble("total_amount"));
        Timestamp usedAt = rs.getTimestamp("used_at");
        if (usedAt != null) record.setUsedAt(usedAt.toLocalDateTime());
        record.setNote(rs.getString("note"));
        int createdBy = rs.getInt("created_by");
        record.setCreatedBy(rs.wasNull() ? null : createdBy);
        return record;
    }
}
