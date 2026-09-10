package com.cnj42.hotel.dao;

import com.cnj42.hotel.model.Room;
import com.cnj42.hotel.model.RoomType;
import com.cnj42.hotel.utils.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RoomDAO {

    public List<Room> getAllRoomsWithType() throws SQLException {
        String sql = "SELECT r.room_id, r.room_number, r.room_type_id, r.floor, r.status, r.image_path, r.amenities, r.note, " +
                "rt.type_name, rt.description, rt.capacity, rt.price_per_night, rt.status as type_status " +
                "FROM rooms r " +
                "JOIN room_types rt ON r.room_type_id = rt.room_type_id " +
                "ORDER BY r.room_number ASC";

        List<Room> rooms = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Room room = new Room();
                room.setRoomId(rs.getInt("room_id"));
                room.setRoomNumber(rs.getString("room_number"));
                room.setRoomTypeId(rs.getInt("room_type_id"));
                room.setFloor(rs.getInt("floor"));
                room.setStatus(rs.getString("status"));
                room.setImagePath(rs.getString("image_path"));
                room.setAmenities(rs.getString("amenities"));
                room.setNote(rs.getString("note"));

                RoomType roomType = new RoomType();
                roomType.setRoomTypeId(rs.getInt("room_type_id"));
                roomType.setTypeName(rs.getString("type_name"));
                roomType.setDescription(rs.getString("description"));
                roomType.setCapacity(rs.getInt("capacity"));
                roomType.setPricePerNight(rs.getDouble("price_per_night"));
                roomType.setStatus(rs.getString("type_status"));

                room.setRoomType(roomType);
                rooms.add(room);
            }
        }

        return rooms;
    }

    public List<Room> searchRooms(String keyword, String status) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT r.room_id, r.room_number, r.room_type_id, r.floor, r.status, r.image_path, r.amenities, r.note, " +
                "rt.type_name, rt.description, rt.capacity, rt.price_per_night, rt.status as type_status " +
                "FROM rooms r " +
                "JOIN room_types rt ON r.room_type_id = rt.room_type_id " +
                "WHERE 1=1"
        );

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (r.room_number LIKE ? OR rt.type_name LIKE ?)");
        }

        if (status != null && !status.isEmpty() && !status.equals("TẤT CẢ")) {
            sql.append(" AND r.status = ?");
        }

        sql.append(" ORDER BY r.room_number ASC");

        List<Room> rooms = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            int paramIndex = 1;

            if (keyword != null && !keyword.trim().isEmpty()) {
                String searchKey = "%" + keyword + "%";
                stmt.setString(paramIndex++, searchKey);
                stmt.setString(paramIndex++, searchKey);
            }

            if (status != null && !status.isEmpty() && !status.equals("TẤT CẢ")) {
                stmt.setString(paramIndex++, status);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Room room = new Room();
                    room.setRoomId(rs.getInt("room_id"));
                    room.setRoomNumber(rs.getString("room_number"));
                    room.setRoomTypeId(rs.getInt("room_type_id"));
                    room.setFloor(rs.getInt("floor"));
                    room.setStatus(rs.getString("status"));
                    room.setImagePath(rs.getString("image_path"));
                    room.setAmenities(rs.getString("amenities"));
                    room.setNote(rs.getString("note"));

                    RoomType roomType = new RoomType();
                    roomType.setRoomTypeId(rs.getInt("room_type_id"));
                    roomType.setTypeName(rs.getString("type_name"));
                    roomType.setDescription(rs.getString("description"));
                    roomType.setCapacity(rs.getInt("capacity"));
                    roomType.setPricePerNight(rs.getDouble("price_per_night"));
                    roomType.setStatus(rs.getString("type_status"));

                    room.setRoomType(roomType);
                    rooms.add(room);
                }
            }
        }

        return rooms;
    }

    public Room getRoomById(int roomId) throws SQLException {
        String sql = "SELECT r.room_id, r.room_number, r.room_type_id, r.floor, r.status, r.image_path, r.amenities, r.note, " +
                "rt.type_name, rt.description, rt.capacity, rt.price_per_night, rt.status as type_status " +
                "FROM rooms r " +
                "JOIN room_types rt ON r.room_type_id = rt.room_type_id " +
                "WHERE r.room_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, roomId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Room room = new Room();
                    room.setRoomId(rs.getInt("room_id"));
                    room.setRoomNumber(rs.getString("room_number"));
                    room.setRoomTypeId(rs.getInt("room_type_id"));
                    room.setFloor(rs.getInt("floor"));
                    room.setStatus(rs.getString("status"));
                    room.setImagePath(rs.getString("image_path"));
                    room.setAmenities(rs.getString("amenities"));
                    room.setNote(rs.getString("note"));

                    RoomType roomType = new RoomType();
                    roomType.setRoomTypeId(rs.getInt("room_type_id"));
                    roomType.setTypeName(rs.getString("type_name"));
                    roomType.setDescription(rs.getString("description"));
                    roomType.setCapacity(rs.getInt("capacity"));
                    roomType.setPricePerNight(rs.getDouble("price_per_night"));
                    roomType.setStatus(rs.getString("type_status"));

                    room.setRoomType(roomType);
                    return room;
                }
            }
        }

        return null;
    }

    public boolean createRoom(Room room) throws SQLException {
        String sql = "INSERT INTO rooms (room_number, room_type_id, floor, status, image_path, amenities, note) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, room.getRoomNumber());
            stmt.setInt(2, room.getRoomTypeId());
            stmt.setInt(3, room.getFloor());
            stmt.setString(4, room.getStatus() != null ? room.getStatus() : "AVAILABLE");
            stmt.setString(5, room.getImagePath());
            stmt.setString(6, room.getAmenities());
            stmt.setString(7, room.getNote());

            return stmt.executeUpdate() > 0;
        }
    }

    public boolean updateRoom(Room room) throws SQLException {
        String sql = "UPDATE rooms SET room_number = ?, room_type_id = ?, floor = ?, status = ?, image_path = ?, amenities = ?, note = ? WHERE room_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, room.getRoomNumber());
            stmt.setInt(2, room.getRoomTypeId());
            stmt.setInt(3, room.getFloor());
            stmt.setString(4, room.getStatus());
            stmt.setString(5, room.getImagePath());
            stmt.setString(6, room.getAmenities());
            stmt.setString(7, room.getNote());
            stmt.setInt(8, room.getRoomId());

            return stmt.executeUpdate() > 0;
        }
    }

    public boolean deleteRoom(int roomId) throws SQLException {
        String sql = "DELETE FROM rooms WHERE room_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, roomId);

            return stmt.executeUpdate() > 0;
        }
    }

    public List<RoomType> getAllRoomTypes() throws SQLException {
        String sql = "SELECT * FROM room_types WHERE status = 'ACTIVE' ORDER BY type_name";

        List<RoomType> roomTypes = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                RoomType roomType = new RoomType();
                roomType.setRoomTypeId(rs.getInt("room_type_id"));
                roomType.setTypeName(rs.getString("type_name"));
                roomType.setDescription(rs.getString("description"));
                roomType.setCapacity(rs.getInt("capacity"));
                roomType.setPricePerNight(rs.getDouble("price_per_night"));
                roomType.setStatus(rs.getString("status"));

                roomTypes.add(roomType);
            }
        }

        return roomTypes;
    }
}
