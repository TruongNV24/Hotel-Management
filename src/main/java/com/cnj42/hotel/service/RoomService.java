package com.cnj42.hotel.service;

import com.cnj42.hotel.dao.RoomDAO;
import com.cnj42.hotel.model.Room;
import com.cnj42.hotel.model.RoomType;
import com.cnj42.hotel.model.User;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RoomService {

    private final RoomDAO roomDAO;
    private final AuditLogService auditLogService = new AuditLogService();

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
        return createRoom(room, null);
    }

    public boolean createRoom(Room room, User actor) {
        try {
            boolean created = roomDAO.createRoom(room);
            auditLogService.logEvent(created ? "ROOM_CREATED" : "ROOM_CREATE_FAILED", "ROOM", "ROOM",
                    created ? room.getRoomId() : null, actor,
                    (created ? "Created" : "Failed to create") + " room " + room.getRoomNumber(),
                    "127.0.0.1", created ? "SUCCESS" : "FAILED");
            return created;
        } catch (SQLException e) {
            System.err.println("Lỗi khi tạo phòng: " + e.getMessage());
            auditLogService.logEvent("ROOM_CREATE_FAILED", "ROOM", "ROOM", null, actor,
                    "Exception creating room " + (room == null ? "" : room.getRoomNumber()) + ": " + e.getMessage(),
                    "127.0.0.1", "FAILED");
            return false;
        }
    }

    public boolean updateRoom(Room room) {
        return updateRoom(room, null);
    }

    public boolean updateRoom(Room room, User actor) {
        try {
            boolean updated = roomDAO.updateRoom(room);
            auditLogService.logEvent(updated ? "ROOM_UPDATED" : "ROOM_UPDATE_FAILED", "ROOM", "ROOM",
                    room.getRoomId(), actor,
                    (updated ? "Updated" : "Failed to update") + " room " + room.getRoomNumber(),
                    "127.0.0.1", updated ? "SUCCESS" : "FAILED");
            return updated;
        } catch (SQLException e) {
            System.err.println("Lỗi khi cập nhật phòng: " + e.getMessage());
            auditLogService.logEvent("ROOM_UPDATE_FAILED", "ROOM", "ROOM", room == null ? null : room.getRoomId(), actor,
                    "Exception updating room: " + e.getMessage(), "127.0.0.1", "FAILED");
            return false;
        }
    }

    public boolean deleteRoom(int roomId) {
        return deleteRoom(roomId, null);
    }

    public boolean deleteRoom(int roomId, User actor) {
        try {
            boolean deleted = roomDAO.deleteRoom(roomId);
            auditLogService.logEvent(deleted ? "ROOM_DELETED" : "ROOM_DELETE_FAILED", "ROOM", "ROOM", roomId, actor,
                    (deleted ? "Deleted" : "Failed to delete") + " room id " + roomId,
                    "127.0.0.1", deleted ? "SUCCESS" : "FAILED");
            return deleted;
        } catch (SQLException e) {
            System.err.println("Lỗi khi xóa phòng: " + e.getMessage());
            auditLogService.logEvent("ROOM_DELETE_FAILED", "ROOM", "ROOM", roomId, actor,
                    "Exception deleting room id " + roomId + ": " + e.getMessage(), "127.0.0.1", "FAILED");
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
