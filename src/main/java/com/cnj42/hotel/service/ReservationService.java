package com.cnj42.hotel.service;

import com.cnj42.hotel.model.Reservation;
import com.cnj42.hotel.utils.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ReservationService {

    public List<Reservation> listReservations(String keyword, String status) {
        List<Reservation> list = new ArrayList<>();
        String sql = "SELECT r.reservation_id, r.reservation_code, g.full_name AS guest_name, r.room_id, rm.room_number, r.check_in_date, r.check_out_date, r.status " +
                "FROM reservations r LEFT JOIN guests g ON r.guest_id = g.guest_id LEFT JOIN rooms rm ON r.room_id = rm.room_id WHERE 1=1";

        if (keyword != null && !keyword.isBlank()) {
            sql += " AND (r.reservation_code LIKE ? OR g.full_name LIKE ? OR rm.room_number LIKE ? )";
        }
        if (status != null && !status.isBlank() && !"Tất cả".equals(status)) {
            sql += " AND r.status = ?";
        }
        sql += " ORDER BY r.reservation_id DESC";

        try (Connection conn = DBConnection.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            int idx = 1;
            if (keyword != null && !keyword.isBlank()) {
                String k = "%" + keyword + "%";
                stmt.setString(idx++, k);
                stmt.setString(idx++, k);
                stmt.setString(idx++, k);
            }
            if (status != null && !status.isBlank() && !"Tất cả".equals(status)) stmt.setString(idx++, status);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Reservation r = new Reservation();
                    r.setReservationId(rs.getInt("reservation_id"));
                    r.setReservationCode(rs.getString("reservation_code"));
                    r.setGuestName(rs.getString("guest_name"));
                    r.setRoomId(rs.getInt("room_id"));
                    r.setRoomNumber(rs.getString("room_number"));
                    r.setCheckInDate(rs.getString("check_in_date"));
                    r.setCheckOutDate(rs.getString("check_out_date"));
                    r.setStatus(rs.getString("status"));
                    list.add(r);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy reservations: " + e.getMessage());
        }

        return list;
    }

    public boolean cancelReservation(int reservationId, int roomId) {
        String sql1 = "UPDATE reservations SET status = 'CANCELLED' WHERE reservation_id = ?";
        String sql2 = "UPDATE rooms SET status = 'AVAILABLE' WHERE room_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement s1 = conn.prepareStatement(sql1); PreparedStatement s2 = conn.prepareStatement(sql2)) {
            conn.setAutoCommit(false);
            s1.setInt(1, reservationId);
            s1.executeUpdate();
            s2.setInt(1, roomId);
            s2.executeUpdate();
            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Lỗi khi hủy reservation: " + e.getMessage());
            return false;
        }
    }

    public boolean checkIn(int reservationId, int roomId) {
        return checkIn(reservationId, roomId, null);
    }

    public boolean checkIn(int reservationId, int roomId, Integer userId) {
        String sql1 = "UPDATE reservations SET status = 'CHECKED_IN' WHERE reservation_id = ?";
        String sql2 = "UPDATE rooms SET status = 'OCCUPIED' WHERE room_id = ?";
        String insertStay = "INSERT INTO stays (reservation_id, room_id, actual_check_in, status, check_in_by) VALUES (?, ?, NOW(), 'CHECKED_IN', ?)";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement s1 = conn.prepareStatement(sql1); PreparedStatement s2 = conn.prepareStatement(sql2); PreparedStatement s3 = conn.prepareStatement(insertStay)) {
            conn.setAutoCommit(false);
            s1.setInt(1, reservationId);
            s1.executeUpdate();
            s2.setInt(1, roomId);
            s2.executeUpdate();
            s3.setInt(1, reservationId);
            s3.setInt(2, roomId);
            if (userId != null) s3.setInt(3, userId); else s3.setNull(3, java.sql.Types.INTEGER);
            s3.executeUpdate();
            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Lỗi khi check-in: " + e.getMessage());
            return false;
        }
    }

    public boolean checkOut(int reservationId, int roomId) {
        return checkOut(reservationId, roomId, null);
    }

    public boolean checkOut(int reservationId, int roomId, Integer userId) {
        String sql1 = "UPDATE reservations SET status = 'COMPLETED' WHERE reservation_id = ?";
        String sql2 = "UPDATE rooms SET status = 'AVAILABLE' WHERE room_id = ?";
        String updateStay = "UPDATE stays SET actual_check_out = NOW(), status = 'CHECKED_OUT', check_out_by = ? WHERE reservation_id = ? AND status = 'CHECKED_IN'";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement s1 = conn.prepareStatement(sql1); PreparedStatement s2 = conn.prepareStatement(sql2); PreparedStatement s3 = conn.prepareStatement(updateStay)) {
            conn.setAutoCommit(false);
            s1.setInt(1, reservationId);
            s1.executeUpdate();
            s2.setInt(1, roomId);
            s2.executeUpdate();
            if (userId != null) s3.setInt(1, userId); else s3.setNull(1, java.sql.Types.INTEGER);
            s3.setInt(2, reservationId);
            s3.executeUpdate();
            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Lỗi khi check-out: " + e.getMessage());
            return false;
        }
    }

    public int createReservation(int guestId, int roomId, String checkInDate, String checkOutDate, int numGuests, String note, Integer createdBy) {
        String insert = "INSERT INTO reservations (reservation_code, guest_id, room_id, check_in_date, check_out_date, number_of_guests, status, note, created_by) VALUES (?, ?, ?, ?, ?, ?, 'PENDING', ?, ?)";
        String updateRoom = "UPDATE rooms SET status = 'RESERVED' WHERE room_id = ?";
        String code = "RES" + System.currentTimeMillis();
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(insert, PreparedStatement.RETURN_GENERATED_KEYS); PreparedStatement p2 = conn.prepareStatement(updateRoom)) {
            conn.setAutoCommit(false);
            ps.setString(1, code);
            ps.setInt(2, guestId);
            ps.setInt(3, roomId);
            ps.setDate(4, java.sql.Date.valueOf(checkInDate));
            ps.setDate(5, java.sql.Date.valueOf(checkOutDate));
            ps.setInt(6, numGuests);
            ps.setString(7, note);
            if (createdBy != null) ps.setInt(8, createdBy); else ps.setNull(8, java.sql.Types.INTEGER);
            ps.executeUpdate();
            try (ResultSet gk = ps.getGeneratedKeys()) {
                if (gk.next()) {
                    int id = gk.getInt(1);
                    p2.setInt(1, roomId);
                    p2.executeUpdate();
                    conn.commit();
                    return id;
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi tạo reservation: " + e.getMessage());
        }
        return -1;
    }

    public int walkInCheckIn(int guestId, int roomId, int numGuests, String note, Integer createdBy) {
        // create reservation with CHECKED_IN and create stay immediately
        String insert = "INSERT INTO reservations (reservation_code, guest_id, room_id, check_in_date, check_out_date, number_of_guests, status, note, created_by) VALUES (?, ?, ?, CURDATE(), CURDATE(), ?, 'CHECKED_IN', ?, ?)";
        String insertStay = "INSERT INTO stays (reservation_id, room_id, actual_check_in, status, check_in_by) VALUES (?, ?, NOW(), 'CHECKED_IN', ?)";
        String updateRoom = "UPDATE rooms SET status = 'OCCUPIED' WHERE room_id = ?";
        String code = "RESW" + System.currentTimeMillis();
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(insert, PreparedStatement.RETURN_GENERATED_KEYS); PreparedStatement p2 = conn.prepareStatement(insertStay); PreparedStatement p3 = conn.prepareStatement(updateRoom)) {
            conn.setAutoCommit(false);
            ps.setString(1, code);
            ps.setInt(2, guestId);
            ps.setInt(3, roomId);
            ps.setInt(4, numGuests);
            ps.setString(5, note);
            if (createdBy != null) ps.setInt(6, createdBy); else ps.setNull(6, java.sql.Types.INTEGER);
            ps.executeUpdate();
            try (ResultSet gk = ps.getGeneratedKeys()) {
                if (gk.next()) {
                    int rid = gk.getInt(1);
                    p2.setInt(1, rid);
                    p2.setInt(2, roomId);
                    if (createdBy != null) p2.setInt(3, createdBy); else p2.setNull(3, java.sql.Types.INTEGER);
                    p2.executeUpdate();
                    p3.setInt(1, roomId);
                    p3.executeUpdate();
                    conn.commit();
                    return rid;
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi walk-in check-in: " + e.getMessage());
        }
        return -1;
    }

    public Integer getLatestStayIdForReservation(int reservationId) {
        String sql = "SELECT stay_id FROM stays WHERE reservation_id = ? ORDER BY stay_id DESC LIMIT 1";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt(1); }
        } catch (SQLException e) { System.err.println("Lỗi lấy stay id: " + e.getMessage()); }
        return null;
    }

    public boolean updateReservation(int reservationId, int guestId, int roomId, String checkInDate, String checkOutDate, int numGuests, String note) {
        String fetchOld = "SELECT room_id FROM reservations WHERE reservation_id = ?";
        String update = "UPDATE reservations SET guest_id = ?, room_id = ?, check_in_date = ?, check_out_date = ?, number_of_guests = ?, note = ? WHERE reservation_id = ?";
        String setAvailable = "UPDATE rooms SET status = 'AVAILABLE' WHERE room_id = ?";
        String setReserved = "UPDATE rooms SET status = 'RESERVED' WHERE room_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement f = conn.prepareStatement(fetchOld); PreparedStatement u = conn.prepareStatement(update); PreparedStatement sa = conn.prepareStatement(setAvailable); PreparedStatement sr = conn.prepareStatement(setReserved)) {
            conn.setAutoCommit(false);
            f.setInt(1, reservationId);
            int oldRoom = -1;
            try (ResultSet rs = f.executeQuery()) { if (rs.next()) oldRoom = rs.getInt(1); }
            u.setInt(1, guestId);
            u.setInt(2, roomId);
            u.setDate(3, java.sql.Date.valueOf(checkInDate));
            u.setDate(4, java.sql.Date.valueOf(checkOutDate));
            u.setInt(5, numGuests);
            u.setString(6, note);
            u.setInt(7, reservationId);
            u.executeUpdate();
            if (oldRoom != -1 && oldRoom != roomId) {
                sa.setInt(1, oldRoom);
                sa.executeUpdate();
            }
            sr.setInt(1, roomId);
            sr.executeUpdate();
            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Lỗi khi cập nhật reservation: " + e.getMessage());
            return false;
        }
    }

    public Reservation getReservationById(int reservationId) {
        String sql = "SELECT r.*, g.full_name, r.number_of_guests, r.note FROM reservations r JOIN guests g ON r.guest_id = g.guest_id WHERE r.reservation_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Reservation r = new Reservation();
                    r.setReservationId(rs.getInt("reservation_id"));
                    r.setReservationCode(rs.getString("reservation_code"));
                    r.setGuestId(rs.getInt("guest_id"));
                    r.setGuestName(rs.getString("full_name"));
                    r.setRoomId(rs.getInt("room_id"));
                    r.setCheckInDate(rs.getString("check_in_date"));
                    r.setCheckOutDate(rs.getString("check_out_date"));
                    r.setNumberOfGuests(rs.getInt("number_of_guests"));
                    r.setNote(rs.getString("note"));
                    r.setStatus(rs.getString("status"));
                    return r;
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi lấy reservation theo id: " + e.getMessage());
        }
        return null;
    }
}
