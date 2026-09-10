package com.cnj42.hotel.service;

import com.cnj42.hotel.dao.RoomDAO;
import com.cnj42.hotel.model.Room;
import com.cnj42.hotel.model.RoomType;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RoomService {

    private final RoomDAO roomDAO;

    public RoomService() {
        this.roomDAO = new RoomDAO();
    }

    public List<Room> getAllRooms() {
        try {
            return roomDAO.getAllRoomsWithType();
        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy danh sách phòng: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Room> searchRooms(String keyword, String status) {
        try {
            return roomDAO.searchRooms(keyword, status);
        } catch (SQLException e) {
            System.err.println("Lỗi khi tìm kiếm phòng: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public Room getRoomById(int roomId) {
        try {
            return roomDAO.getRoomById(roomId);
        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy thông tin phòng: " + e.getMessage());
            return null;
        }
    }

    public boolean createRoom(Room room) {
        try {
            return roomDAO.createRoom(room);
        } catch (SQLException e) {
            System.err.println("Lỗi khi tạo phòng: " + e.getMessage());
            return false;
        }
    }

    public boolean updateRoom(Room room) {
        try {
            return roomDAO.updateRoom(room);
        } catch (SQLException e) {
            System.err.println("Lỗi khi cập nhật phòng: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteRoom(int roomId) {
        try {
            return roomDAO.deleteRoom(roomId);
        } catch (SQLException e) {
            System.err.println("Lỗi khi xóa phòng: " + e.getMessage());
            return false;
        }
    }

    public List<RoomType> getAllRoomTypes() {
        try {
            return roomDAO.getAllRoomTypes();
        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy loại phòng: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}
