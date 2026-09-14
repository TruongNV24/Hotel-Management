package com.cnj42.hotel.service;

import com.cnj42.hotel.model.Reservation;
import com.cnj42.hotel.utils.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReservationService {

    public List<Reservation> listReservations(String keyword, String status) {
        List<Reservation> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT r.reservation_id, r.reservation_code, g.full_name AS guest_name, r.room_id, rm.room_number, " +
                        "r.check_in_date, r.check_out_date, r.status " +
                        "FROM reservations r LEFT JOIN guests g ON r.guest_id = g.guest_id " +
                        "LEFT JOIN rooms rm ON r.room_id = rm.room_id WHERE 1=1"
        );

        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (r.reservation_code LIKE ? OR g.full_name LIKE ? OR rm.room_number LIKE ?)");
        }
        if (status != null && !status.isBlank() && !"Tất cả".equals(status)) {
            sql.append(" AND r.status = ?");
        }
        sql.append(" ORDER BY r.reservation_id DESC");

        try (Connection conn = DBConnection.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            if (keyword != null && !keyword.isBlank()) {
                String k = "%" + keyword + "%";
                stmt.setString(idx++, k);
                stmt.setString(idx++, k);
                stmt.setString(idx++, k);
            }
            if (status != null && !status.isBlank() && !"Tất cả".equals(status)) {
                stmt.setString(idx++, status);
            }

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

    public boolean isRoomAvailable(int roomId, String checkInDate, String checkOutDate, Integer excludeReservationId) {
        if (roomId <= 0 || checkInDate == null || checkOutDate == null) {
            return false;
        }

        try {
            LocalDate start = LocalDate.parse(checkInDate);
            LocalDate end = LocalDate.parse(checkOutDate);
            if (!end.isAfter(start)) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }

        String sql = "SELECT 1 FROM reservations " +
                "WHERE room_id = ? " +
                "AND status IN ('PENDING', 'CONFIRMED', 'CHECKED_IN') " +
                "AND (? IS NULL OR reservation_id <> ?) " +
                "AND check_in_date < ? AND check_out_date > ? LIMIT 1";

        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            if (excludeReservationId != null) {
                ps.setInt(2, excludeReservationId);
                ps.setInt(3, excludeReservationId);
            } else {
                ps.setNull(2, java.sql.Types.INTEGER);
                ps.setNull(3, java.sql.Types.INTEGER);
            }
            ps.setDate(4, java.sql.Date.valueOf(checkOutDate));
            ps.setDate(5, java.sql.Date.valueOf(checkInDate));
            try (ResultSet rs = ps.executeQuery()) {
                return !rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Lỗi kiểm tra phòng trống: " + e.getMessage());
            return false;
        }
    }

    private void updateRoomStatus(Connection conn, int roomId, String status) throws SQLException {
        String sql = "UPDATE rooms SET status = ? WHERE room_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, roomId);
            ps.executeUpdate();
        }
    }

    private void refreshRoomStatus(Connection conn, int roomId, Integer excludeReservationId) throws SQLException {
        String sql = "SELECT CASE " +
                "WHEN EXISTS (SELECT 1 FROM reservations WHERE room_id = ? AND status = 'CHECKED_IN' " +
                "AND (? IS NULL OR reservation_id <> ?)) THEN 'OCCUPIED' " +
                "WHEN EXISTS (SELECT 1 FROM reservations WHERE room_id = ? AND status IN ('PENDING', 'CONFIRMED') " +
                "AND (? IS NULL OR reservation_id <> ?)) THEN 'RESERVED' " +
                "ELSE 'AVAILABLE' END AS effective_status";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            ps.setInt(idx++, roomId);
            if (excludeReservationId != null) {
                ps.setInt(idx++, excludeReservationId);
                ps.setInt(idx++, excludeReservationId);
            } else {
                ps.setNull(idx++, java.sql.Types.INTEGER);
                ps.setNull(idx++, java.sql.Types.INTEGER);
            }
            ps.setInt(idx++, roomId);
            if (excludeReservationId != null) {
                ps.setInt(idx++, excludeReservationId);
                ps.setInt(idx++, excludeReservationId);
            } else {
                ps.setNull(idx++, java.sql.Types.INTEGER);
                ps.setNull(idx++, java.sql.Types.INTEGER);
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    updateRoomStatus(conn, roomId, rs.getString("effective_status"));
                } else {
                    updateRoomStatus(conn, roomId, "AVAILABLE");
                }
            }
        }
    }

    public boolean cancelReservation(int reservationId, int roomId) {
        String fetchStatus = "SELECT status FROM reservations WHERE reservation_id = ?";
        String updateReservation = "UPDATE reservations SET status = 'CANCELLED' WHERE reservation_id = ? AND status IN ('PENDING', 'CONFIRMED')";
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            String currentStatus = null;
            try (PreparedStatement ps = conn.prepareStatement(fetchStatus)) {
                ps.setInt(1, reservationId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        currentStatus = rs.getString(1);
                    }
                }
            }

            if (currentStatus == null || "CANCELLED".equals(currentStatus) || "COMPLETED".equals(currentStatus) || "CHECKED_IN".equals(currentStatus)) {
                conn.rollback();
                return false;
            }

            try (PreparedStatement ps = conn.prepareStatement(updateReservation)) {
                ps.setInt(1, reservationId);
                if (ps.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }
            }

            refreshRoomStatus(conn, roomId, reservationId);
            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
            }
            System.err.println("Lỗi khi hủy reservation: " + e.getMessage());
            return false;
        }
    }

    public boolean checkIn(int reservationId, int roomId) {
        return checkIn(reservationId, roomId, null);
    }

    public boolean checkIn(int reservationId, int roomId, Integer userId) {
        String fetchReservation = "SELECT room_id, status, check_in_date, check_out_date FROM reservations WHERE reservation_id = ?";
        String updateReservation = "UPDATE reservations SET status = 'CHECKED_IN' WHERE reservation_id = ? AND status IN ('PENDING', 'CONFIRMED')";
        String insertStay = "INSERT INTO stays (reservation_id, room_id, actual_check_in, status, check_in_by) " +
                "VALUES (?, ?, NOW(), 'CHECKED_IN', ?)";
        String countStay = "SELECT COUNT(*) FROM stays WHERE reservation_id = ?";
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            int currentRoomId = -1;
            String status = null;
            String checkInDate = null;
            String checkOutDate = null;
            try (PreparedStatement ps = conn.prepareStatement(fetchReservation)) {
                ps.setInt(1, reservationId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return false;
                    }
                    currentRoomId = rs.getInt("room_id");
                    status = rs.getString("status");
                    checkInDate = rs.getString("check_in_date");
                    checkOutDate = rs.getString("check_out_date");
                }
            }

            if (roomId != currentRoomId) {
                conn.rollback();
                return false;
            }

            if (status == null || (!"PENDING".equals(status) && !"CONFIRMED".equals(status))) {
                conn.rollback();
                return false;
            }

            if (!isRoomAvailable(roomId, checkInDate, checkOutDate, reservationId)) {
                conn.rollback();
                return false;
            }

            try (PreparedStatement ps = conn.prepareStatement(updateReservation)) {
                ps.setInt(1, reservationId);
                if (ps.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }
            }

            boolean stayExists = false;
            try (PreparedStatement ps = conn.prepareStatement(countStay)) {
                ps.setInt(1, reservationId);
                try (ResultSet rs = ps.executeQuery()) {
                    stayExists = rs.next() && rs.getInt(1) > 0;
                }
            }
            if (!stayExists) {
                try (PreparedStatement ps = conn.prepareStatement(insertStay)) {
                    ps.setInt(1, reservationId);
                    ps.setInt(2, roomId);
                    if (userId != null) ps.setInt(3, userId); else ps.setNull(3, java.sql.Types.INTEGER);
                    ps.executeUpdate();
                }
            }

            updateRoomStatus(conn, roomId, "OCCUPIED");
            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
            }
            System.err.println("Lỗi khi check-in: " + e.getMessage());
            return false;
        }
    }

    public boolean checkOut(int reservationId, int roomId) {
        return checkOut(reservationId, roomId, null);
    }

    public boolean checkOut(int reservationId, int roomId, Integer userId) {
        String fetchStatus = "SELECT status, room_id FROM reservations WHERE reservation_id = ?";
        String updateReservation = "UPDATE reservations SET status = 'COMPLETED' WHERE reservation_id = ? AND status = 'CHECKED_IN'";
        String updateStay = "UPDATE stays SET actual_check_out = NOW(), status = 'CHECKED_OUT', check_out_by = ? WHERE reservation_id = ? AND status = 'CHECKED_IN'";
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            Integer currentRoomId = null;
            String status = null;
            try (PreparedStatement ps = conn.prepareStatement(fetchStatus)) {
                ps.setInt(1, reservationId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        currentRoomId = rs.getInt("room_id");
                        status = rs.getString("status");
                    }
                }
            }

            if (currentRoomId == null || roomId != currentRoomId || !"CHECKED_IN".equals(status)) {
                conn.rollback();
                return false;
            }

            try (PreparedStatement ps = conn.prepareStatement(updateReservation)) {
                ps.setInt(1, reservationId);
                if (ps.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(updateStay)) {
                if (userId != null) ps.setInt(1, userId); else ps.setNull(1, java.sql.Types.INTEGER);
                ps.setInt(2, reservationId);
                ps.executeUpdate();
            }

            updateRoomStatus(conn, roomId, "CLEANING");
            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
            }
            System.err.println("Lỗi khi check-out: " + e.getMessage());
            return false;
        }
    }

    public int createReservation(int guestId, int roomId, String checkInDate, String checkOutDate, int numGuests, String note, Integer createdBy) {
        if (!isRoomAvailable(roomId, checkInDate, checkOutDate, null)) {
            return -1;
        }

        String insert = "INSERT INTO reservations (reservation_code, guest_id, room_id, check_in_date, check_out_date, number_of_guests, status, note, created_by) " +
                "VALUES (?, ?, ?, ?, ?, ?, 'PENDING', ?, ?)";
        String code = "RES" + System.currentTimeMillis();
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(insert, PreparedStatement.RETURN_GENERATED_KEYS)) {
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
                        updateRoomStatus(conn, roomId, "RESERVED");
                        conn.commit();
                        return id;
                    }
                }
            }
            conn.rollback();
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
            }
            System.err.println("Lỗi khi tạo reservation: " + e.getMessage());
        }
        return -1;
    }

    public int walkInCheckIn(int guestId, int roomId, int numGuests, String note, Integer createdBy) {
        String insert = "INSERT INTO reservations (reservation_code, guest_id, room_id, check_in_date, check_out_date, number_of_guests, status, note, created_by) " +
                "VALUES (?, ?, ?, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 1 DAY), ?, 'CHECKED_IN', ?, ?)";
        String insertStay = "INSERT INTO stays (reservation_id, room_id, actual_check_in, status, check_in_by) VALUES (?, ?, NOW(), 'CHECKED_IN', ?)";
        String updateRoom = "UPDATE rooms SET status = 'OCCUPIED' WHERE room_id = ?";
        String code = "RESW" + System.currentTimeMillis();
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(insert, PreparedStatement.RETURN_GENERATED_KEYS);
                 PreparedStatement p2 = conn.prepareStatement(insertStay);
                 PreparedStatement p3 = conn.prepareStatement(updateRoom)) {
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
            }
            conn.rollback();
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
            }
            System.err.println("Lỗi walk-in check-in: " + e.getMessage());
        }
        return -1;
    }

    public boolean updateReservation(int reservationId, int guestId, int roomId, String checkInDate, String checkOutDate, int numGuests, String note) {
        String fetchOld = "SELECT room_id, status FROM reservations WHERE reservation_id = ?";
        String update = "UPDATE reservations SET guest_id = ?, room_id = ?, check_in_date = ?, check_out_date = ?, number_of_guests = ?, note = ? WHERE reservation_id = ? AND status IN ('PENDING', 'CONFIRMED')";
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            int oldRoom = -1;
            try (PreparedStatement f = conn.prepareStatement(fetchOld)) {
                f.setInt(1, reservationId);
                try (ResultSet rs = f.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return false;
                    }
                    oldRoom = rs.getInt("room_id");
                    String status = rs.getString("status");
                    if (!"PENDING".equals(status) && !"CONFIRMED".equals(status)) {
                        conn.rollback();
                        return false;
                    }
                }
            }

            if (!isRoomAvailable(roomId, checkInDate, checkOutDate, reservationId)) {
                conn.rollback();
                return false;
            }

            try (PreparedStatement u = conn.prepareStatement(update)) {
                u.setInt(1, guestId);
                u.setInt(2, roomId);
                u.setDate(3, java.sql.Date.valueOf(checkInDate));
                u.setDate(4, java.sql.Date.valueOf(checkOutDate));
                u.setInt(5, numGuests);
                u.setString(6, note);
                u.setInt(7, reservationId);
                if (u.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }
            }

            if (oldRoom != roomId) {
                refreshRoomStatus(conn, oldRoom, reservationId);
            }
            refreshRoomStatus(conn, roomId, reservationId);
            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
            }
            System.err.println("Lỗi khi cập nhật reservation: " + e.getMessage());
            return false;
        }
    }

    public Reservation getReservationById(int reservationId) {
        String sql = "SELECT r.*, g.full_name FROM reservations r JOIN guests g ON r.guest_id = g.guest_id WHERE r.reservation_id = ?";
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

    public Integer getLatestStayIdForReservation(int reservationId) {
        String sql = "SELECT stay_id FROM stays WHERE reservation_id = ? ORDER BY stay_id DESC LIMIT 1";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Lỗi lấy stay id: " + e.getMessage());
        }
        return null;
    }
}
