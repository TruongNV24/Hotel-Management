package com.cnj42.hotel.dao;

import com.cnj42.hotel.model.LookupOption;
import com.cnj42.hotel.utils.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ReferenceDataDAO {

    public List<LookupOption> findOptions(String type) throws SQLException {
        String sql = switch (type) {
            case "GUEST" -> "SELECT guest_id, CONCAT(full_name, ' - ', phone) AS label FROM guests ORDER BY full_name";
            case "RESERVATION" -> "SELECT r.reservation_id, CONCAT(r.reservation_code, ' - phòng ', rm.room_number, ' - ', g.full_name) AS label " +
                    "FROM reservations r JOIN guests g ON g.guest_id = r.guest_id JOIN rooms rm ON rm.room_id = r.room_id ORDER BY r.created_at DESC";
            case "STAY" -> "SELECT s.stay_id, CONCAT('Lưu trú #', s.stay_id, ' - phòng ', rm.room_number, ' - ', g.full_name) AS label " +
                    "FROM stays s JOIN reservations r ON r.reservation_id = s.reservation_id JOIN guests g ON g.guest_id = r.guest_id " +
                    "JOIN rooms rm ON rm.room_id = s.room_id ORDER BY s.stay_id DESC";
            case "INVOICE" -> "SELECT i.invoice_id, CONCAT(i.invoice_code, ' - ', i.status, ' - ', g.full_name) AS label " +
                    "FROM invoices i JOIN stays s ON s.stay_id = i.stay_id JOIN reservations r ON r.reservation_id = s.reservation_id " +
                    "JOIN guests g ON g.guest_id = r.guest_id ORDER BY i.issued_at DESC";
            case "ACTIVE_USER" -> "SELECT user_id, CONCAT(full_name, ' (', username, ')') AS label FROM users WHERE status = 'ACTIVE' ORDER BY full_name";
            default -> throw new IllegalArgumentException("Unsupported reference data type: " + type);
        };

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<LookupOption> options = new ArrayList<>();
            while (resultSet.next()) options.add(new LookupOption(resultSet.getInt(1), resultSet.getString("label")));
            return options;
        }
    }
}