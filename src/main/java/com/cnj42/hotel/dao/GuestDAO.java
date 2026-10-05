package com.cnj42.hotel.dao;

import com.cnj42.hotel.model.Guest;
import com.cnj42.hotel.utils.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class GuestDAO {

    public List<Guest> search(String keyword) throws SQLException {
        String sql = "SELECT guest_id, full_name, phone, email, id_card, address, nationality, created_at " +
                "FROM guests WHERE full_name LIKE ? OR phone LIKE ? OR id_card LIKE ? ORDER BY created_at DESC";
        String pattern = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, pattern);
            statement.setString(2, pattern);
            statement.setString(3, pattern);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Guest> guests = new ArrayList<>();
                while (resultSet.next()) guests.add(mapGuest(resultSet));
                return guests;
            }
        }
    }

    public List<Guest> findAll() throws SQLException {
        String sql = "SELECT guest_id, full_name FROM guests ORDER BY full_name";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<Guest> guests = new ArrayList<>();
            while (resultSet.next()) {
                Guest guest = new Guest();
                guest.setGuestId(resultSet.getInt("guest_id"));
                guest.setFullName(resultSet.getString("full_name"));
                guests.add(guest);
            }
            return guests;
        }
    }

    public Guest findById(int guestId) throws SQLException {
        String sql = "SELECT guest_id, full_name, phone, email, id_card, address, nationality, created_at " +
                "FROM guests WHERE guest_id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, guestId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapGuest(resultSet) : null;
            }
        }
    }

    public int create(Guest guest) throws SQLException {
        String sql = "INSERT INTO guests (full_name, phone, id_card, email, address, nationality) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindGuest(statement, guest);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    public boolean update(Guest guest) throws SQLException {
        String sql = "UPDATE guests SET full_name = ?, phone = ?, id_card = ?, email = ?, address = ?, nationality = ? WHERE guest_id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindGuest(statement, guest);
            statement.setInt(7, guest.getGuestId());
            return statement.executeUpdate() > 0;
        }
    }

    public boolean delete(int guestId) throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM guests WHERE guest_id = ?")) {
            statement.setInt(1, guestId);
            return statement.executeUpdate() > 0;
        }
    }

    private Guest mapGuest(ResultSet resultSet) throws SQLException {
        Guest guest = new Guest();
        guest.setGuestId(resultSet.getInt("guest_id"));
        guest.setFullName(resultSet.getString("full_name"));
        guest.setPhone(resultSet.getString("phone"));
        guest.setEmail(resultSet.getString("email"));
        guest.setIdCard(resultSet.getString("id_card"));
        guest.setAddress(resultSet.getString("address"));
        guest.setNationality(resultSet.getString("nationality"));
        guest.setCreatedAt(resultSet.getTimestamp("created_at"));
        return guest;
    }

    private void bindGuest(PreparedStatement statement, Guest guest) throws SQLException {
        statement.setString(1, guest.getFullName());
        statement.setString(2, guest.getPhone());
        statement.setString(3, guest.getIdCard());
        statement.setString(4, guest.getEmail());
        statement.setString(5, guest.getAddress());
        statement.setString(6, guest.getNationality());
    }
}