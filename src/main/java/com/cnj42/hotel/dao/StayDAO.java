package com.cnj42.hotel.dao;

import com.cnj42.hotel.model.CheckoutSummary;
import com.cnj42.hotel.model.ServiceUsage;
import com.cnj42.hotel.model.Stay;
import com.cnj42.hotel.model.StayDetail;
import com.cnj42.hotel.utils.DBConnection;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StayDAO {

    public Map<String, Integer> getStayStats() throws SQLException {
        String sql = "SELECT " +
                "SUM(CASE WHEN res.status IN ('PENDING', 'CONFIRMED') THEN 1 ELSE 0 END) AS waiting_checkin, " +
                "SUM(CASE WHEN res.status = 'CHECKED_IN' OR (s.status = 'CHECKED_IN' AND s.actual_check_out IS NULL) THEN 1 ELSE 0 END) AS in_house, " +
                "SUM(CASE WHEN res.status = 'CHECKED_IN' AND s.actual_check_out IS NULL AND res.check_out_date <= CURDATE() THEN 1 ELSE 0 END) AS waiting_checkout, " +
                "SUM(CASE WHEN res.status = 'COMPLETED' OR s.status = 'CHECKED_OUT' THEN 1 ELSE 0 END) AS checked_out " +
                "FROM reservations res LEFT JOIN stays s ON s.reservation_id = res.reservation_id";

        Map<String, Integer> result = new HashMap<>();
        result.put("waiting_checkin", 0);
        result.put("in_house", 0);
        result.put("waiting_checkout", 0);
        result.put("checked_out", 0);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                result.put("waiting_checkin", rs.getInt("waiting_checkin"));
                result.put("in_house", rs.getInt("in_house"));
                result.put("waiting_checkout", rs.getInt("waiting_checkout"));
                result.put("checked_out", rs.getInt("checked_out"));
            }
        }
        return result;
    }

    public List<Stay> searchStays(String keyword, String statusCode, LocalDate fromDate, LocalDate toDate) throws SQLException {
        List<Stay> stays = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT res.reservation_id, res.reservation_code, g.full_name AS guest_name, g.phone, " +
                        "r.room_number, r.room_id, s.stay_id, res.check_in_date, res.check_out_date, " +
                        "s.actual_check_in, s.actual_check_out, res.number_of_guests, " +
                        "CASE " +
                        "WHEN s.stay_id IS NULL AND res.status IN ('PENDING', 'CONFIRMED') THEN 'WAITING_CHECK_IN' " +
                        "WHEN s.status = 'CHECKED_IN' AND s.actual_check_out IS NULL AND res.check_out_date <= CURDATE() THEN 'CHECKOUT_PENDING' " +
                        "WHEN s.status = 'CHECKED_IN' AND s.actual_check_out IS NULL THEN 'IN_HOUSE' " +
                        "WHEN s.status = 'CHECKED_OUT' OR res.status = 'COMPLETED' THEN 'CHECKED_OUT' " +
                        "ELSE 'WAITING_CHECK_IN' END AS status_code " +
                        "FROM reservations res " +
                        "LEFT JOIN guests g ON g.guest_id = res.guest_id " +
                        "LEFT JOIN rooms r ON r.room_id = res.room_id " +
                        "LEFT JOIN stays s ON s.reservation_id = res.reservation_id " +
                        "WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append("AND (g.full_name LIKE ? OR g.phone LIKE ? OR r.room_number LIKE ? OR res.reservation_code LIKE ?) ");
            String pattern = "%" + keyword.trim() + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }

        if (statusCode != null && !statusCode.isBlank()) {
            sql.append("AND CASE " +
                    "WHEN s.stay_id IS NULL AND res.status IN ('PENDING', 'CONFIRMED') THEN 'WAITING_CHECK_IN' " +
                    "WHEN s.status = 'CHECKED_IN' AND s.actual_check_out IS NULL AND res.check_out_date <= CURDATE() THEN 'CHECKOUT_PENDING' " +
                    "WHEN s.status = 'CHECKED_IN' AND s.actual_check_out IS NULL THEN 'IN_HOUSE' " +
                    "WHEN s.status = 'CHECKED_OUT' OR res.status = 'COMPLETED' THEN 'CHECKED_OUT' " +
                    "ELSE 'WAITING_CHECK_IN' END = ? ");
            params.add(statusCode);
        }

        if (fromDate != null) {
            sql.append("AND COALESCE(s.actual_check_in, res.check_in_date) >= ? ");
            params.add(Date.valueOf(fromDate));
        }
        if (toDate != null) {
            sql.append("AND COALESCE(s.actual_check_in, res.check_in_date) <= ? ");
            params.add(Date.valueOf(toDate));
        }

        sql.append("ORDER BY COALESCE(s.actual_check_in, res.check_in_date) DESC, res.reservation_id DESC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int index = 1;
            for (Object param : params) {
                ps.setObject(index++, param);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Stay stay = new Stay();
                    stay.setReservationId(rs.getInt("reservation_id"));
                    stay.setReservationCode(rs.getString("reservation_code"));
                    stay.setGuestName(rs.getString("guest_name"));
                    stay.setPhone(rs.getString("phone"));
                    stay.setRoomNumber(rs.getString("room_number"));
                    stay.setRoomId(rs.getInt("room_id"));
                    if (rs.getObject("stay_id") != null) {
                        stay.setStayId(rs.getInt("stay_id"));
                    }
                    Date checkIn = rs.getDate("check_in_date");
                    Date expectedCheckout = rs.getDate("check_out_date");
                    if (checkIn != null) {
                        stay.setCheckInDate(checkIn.toLocalDate());
                    }
                    if (expectedCheckout != null) {
                        stay.setExpectedCheckOutDate(expectedCheckout.toLocalDate());
                    }
                    Timestamp actualCheckIn = rs.getTimestamp("actual_check_in");
                    if (actualCheckIn != null) {
                        stay.setActualCheckIn(actualCheckIn.toLocalDateTime());
                    }
                    Timestamp actualCheckOut = rs.getTimestamp("actual_check_out");
                    if (actualCheckOut != null) {
                        stay.setActualCheckOut(actualCheckOut.toLocalDateTime());
                    }
                    stay.setNumberOfGuests(rs.getInt("number_of_guests"));
                    stay.setStatusCode(rs.getString("status_code"));
                    stays.add(stay);
                }
            }
        }
        return stays;
    }

    public boolean checkInReservation(int reservationId, int roomId, Integer currentUserId) throws SQLException {
        String fetch = "SELECT room_id, status FROM reservations WHERE reservation_id = ?";
        String updateReservation = "UPDATE reservations SET status = 'CHECKED_IN' WHERE reservation_id = ? AND status IN ('PENDING', 'CONFIRMED')";
        String countStay = "SELECT COUNT(*) FROM stays WHERE reservation_id = ?";
        String insertStay = "INSERT INTO stays (reservation_id, room_id, actual_check_in, status, check_in_by) VALUES (?, ?, NOW(), 'CHECKED_IN', ?)";
        String updateStay = "UPDATE stays SET room_id = ?, actual_check_in = NOW(), status = 'CHECKED_IN', check_in_by = ? WHERE reservation_id = ?";
        String updateRoom = "UPDATE rooms SET status = 'OCCUPIED' WHERE room_id = ?";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            int existingRoomId = -1;
            String reservationStatus = null;
            try (PreparedStatement ps = conn.prepareStatement(fetch)) {
                ps.setInt(1, reservationId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return false;
                    }
                    existingRoomId = rs.getInt("room_id");
                    reservationStatus = rs.getString("status");
                }
            }

            if (existingRoomId != roomId || reservationStatus == null || (!"PENDING".equals(reservationStatus) && !"CONFIRMED".equals(reservationStatus))) {
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

            int stayCount = 0;
            try (PreparedStatement ps = conn.prepareStatement(countStay)) {
                ps.setInt(1, reservationId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        stayCount = rs.getInt(1);
                    }
                }
            }

            if (stayCount == 0) {
                try (PreparedStatement ps = conn.prepareStatement(insertStay)) {
                    ps.setInt(1, reservationId);
                    ps.setInt(2, roomId);
                    if (currentUserId != null) {
                        ps.setInt(3, currentUserId);
                    } else {
                        ps.setNull(3, Types.INTEGER);
                    }
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement(updateStay)) {
                    ps.setInt(1, roomId);
                    if (currentUserId != null) {
                        ps.setInt(2, currentUserId);
                    } else {
                        ps.setNull(2, Types.INTEGER);
                    }
                    ps.setInt(3, reservationId);
                    ps.executeUpdate();
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(updateRoom)) {
                ps.setInt(1, roomId);
                ps.executeUpdate();
            }

            conn.commit();
            return true;
        }
    }

    public CheckoutSummary getCheckoutSummary(int stayId, int roomId) throws SQLException {
        String roomSql = "SELECT rt.price_per_night FROM rooms r JOIN room_types rt ON rt.room_type_id = r.room_type_id WHERE r.room_id = ?";
        String serviceSql = "SELECT COALESCE(SUM(su.quantity * su.unit_price), 0) AS service_amount FROM service_usages su WHERE su.stay_id = ?";

        double roomAmount = 0;
        double serviceAmount = 0;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement roomPs = conn.prepareStatement(roomSql)) {
            roomPs.setInt(1, roomId);
            try (ResultSet rs = roomPs.executeQuery()) {
                if (rs.next()) {
                    roomAmount = rs.getDouble("price_per_night");
                }
            }

            try (PreparedStatement servicePs = conn.prepareStatement(serviceSql)) {
                servicePs.setInt(1, stayId);
                try (ResultSet rs = servicePs.executeQuery()) {
                    if (rs.next()) {
                        serviceAmount = rs.getDouble("service_amount");
                    }
                }
            }
        }

        return new CheckoutSummary(roomAmount, serviceAmount);
    }

    public boolean checkoutAndCreateInvoice(int stayId, int reservationId, int roomId, String paymentMethod, Integer currentUserId) throws SQLException {
        String checkExistingInvoice = "SELECT COUNT(*) FROM invoices WHERE stay_id = ?";
        String checkStayStatus = "SELECT status FROM stays WHERE stay_id = ?";
        String invoiceInsert = "INSERT INTO invoices (invoice_code, stay_id, room_amount, service_amount, discount_amount, tax_amount, total_amount, status, created_by) VALUES (?, ?, ?, ?, 0, 0, ?, 'UNPAID', ?)";
        String roomDetailInsert = "INSERT INTO invoice_details (invoice_id, item_type, description, quantity, unit_price, amount) VALUES (?, 'ROOM', ?, ?, ?, ?)";
        String serviceDetailInsert = "INSERT INTO invoice_details (invoice_id, item_type, description, quantity, unit_price, amount) VALUES (?, 'SERVICE', ?, ?, ?, ?)";
        String paymentInsert = "INSERT INTO payments (invoice_id, amount, payment_method, payment_date, note, received_by) VALUES (?, ?, ?, NOW(), ?, ?)";
        String stayUpdate = "UPDATE stays SET status = 'CHECKED_OUT', actual_check_out = NOW(), check_out_by = ? WHERE stay_id = ? AND status <> 'CHECKED_OUT'";
        String reservationUpdate = "UPDATE reservations SET status = 'COMPLETED' WHERE reservation_id = ? AND status <> 'COMPLETED'";
        String roomUpdate = "UPDATE rooms SET status = 'CLEANING' WHERE room_id = ?";
        String serviceUsageSql = "SELECT s.service_name, su.quantity, su.unit_price, (su.quantity * su.unit_price) AS total_amount FROM service_usages su JOIN services s ON s.service_id = su.service_id WHERE su.stay_id = ?";

        CheckoutSummary summary = getCheckoutSummary(stayId, roomId);

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement stayStatusPs = conn.prepareStatement(checkStayStatus)) {
                stayStatusPs.setInt(1, stayId);
                try (ResultSet rs = stayStatusPs.executeQuery()) {
                    if (rs.next() && "CHECKED_OUT".equalsIgnoreCase(rs.getString("status"))) {
                        conn.rollback();
                        return false;
                    }
                }
            }

            try (PreparedStatement existingPs = conn.prepareStatement(checkExistingInvoice)) {
                existingPs.setInt(1, stayId);
                try (ResultSet rs = existingPs.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        conn.rollback();
                        return false;
                    }
                }
            }

            int invoiceId;
            try (PreparedStatement invoicePs = conn.prepareStatement(invoiceInsert, Statement.RETURN_GENERATED_KEYS)) {
                invoicePs.setString(1, "INV" + System.currentTimeMillis());
                invoicePs.setInt(2, stayId);
                invoicePs.setDouble(3, summary.getRoomAmount());
                invoicePs.setDouble(4, summary.getServiceAmount());
                invoicePs.setDouble(5, summary.getTotalAmount());
                if (currentUserId != null) {
                    invoicePs.setInt(6, currentUserId);
                } else {
                    invoicePs.setNull(6, Types.INTEGER);
                }
                invoicePs.executeUpdate();

                try (ResultSet keys = invoicePs.getGeneratedKeys()) {
                    if (!keys.next()) {
                        conn.rollback();
                        return false;
                    }
                    invoiceId = keys.getInt(1);
                }
            }

            try (PreparedStatement roomDetailPs = conn.prepareStatement(roomDetailInsert)) {
                roomDetailPs.setInt(1, invoiceId);
                roomDetailPs.setString(2, "Phòng " + roomId);
                roomDetailPs.setInt(3, 1);
                roomDetailPs.setDouble(4, summary.getRoomAmount());
                roomDetailPs.setDouble(5, summary.getRoomAmount());
                roomDetailPs.executeUpdate();
            }

            try (PreparedStatement serviceDetailPs = conn.prepareStatement(serviceDetailInsert);
                 PreparedStatement usagePs = conn.prepareStatement(serviceUsageSql)) {
                usagePs.setInt(1, stayId);
                try (ResultSet usageRs = usagePs.executeQuery()) {
                    while (usageRs.next()) {
                        serviceDetailPs.setInt(1, invoiceId);
                        serviceDetailPs.setString(2, usageRs.getString("service_name"));
                        serviceDetailPs.setInt(3, usageRs.getInt("quantity"));
                        serviceDetailPs.setDouble(4, usageRs.getDouble("unit_price"));
                        serviceDetailPs.setDouble(5, usageRs.getDouble("total_amount"));
                        serviceDetailPs.executeUpdate();
                    }
                }
            }

            try (PreparedStatement paymentPs = conn.prepareStatement(paymentInsert)) {
                paymentPs.setInt(1, invoiceId);
                paymentPs.setDouble(2, summary.getTotalAmount());
                paymentPs.setString(3, paymentMethod == null ? "CASH" : paymentMethod);
                paymentPs.setString(4, "Thanh toán khi check-out");
                if (currentUserId != null) {
                    paymentPs.setInt(5, currentUserId);
                } else {
                    paymentPs.setNull(5, Types.INTEGER);
                }
                paymentPs.executeUpdate();
            }

            try (PreparedStatement stayPs = conn.prepareStatement(stayUpdate)) {
                if (currentUserId != null) {
                    stayPs.setInt(1, currentUserId);
                } else {
                    stayPs.setNull(1, Types.INTEGER);
                }
                stayPs.setInt(2, stayId);
                stayPs.executeUpdate();
            }

            try (PreparedStatement reservationPs = conn.prepareStatement(reservationUpdate)) {
                reservationPs.setInt(1, reservationId);
                reservationPs.executeUpdate();
            }

            try (PreparedStatement roomPs = conn.prepareStatement(roomUpdate)) {
                roomPs.setInt(1, roomId);
                roomPs.executeUpdate();
            }

            conn.commit();
            return true;
        }
    }

    public StayDetail getStayDetail(int reservationId) throws SQLException {
        String detailSql = "SELECT res.reservation_id, res.reservation_code, g.full_name, g.phone, g.email, " +
                "r.room_number, rt.type_name, rt.price_per_night, s.actual_check_in, res.check_out_date, s.actual_check_out, " +
                "res.number_of_guests, CASE WHEN s.stay_id IS NULL AND res.status IN ('PENDING', 'CONFIRMED') THEN 'WAITING_CHECK_IN' " +
                "WHEN s.status = 'CHECKED_IN' AND s.actual_check_out IS NULL AND res.check_out_date <= CURDATE() THEN 'CHECKOUT_PENDING' " +
                "WHEN s.status = 'CHECKED_IN' AND s.actual_check_out IS NULL THEN 'IN_HOUSE' " +
                "WHEN s.status = 'CHECKED_OUT' OR res.status = 'COMPLETED' THEN 'CHECKED_OUT' " +
                "ELSE 'WAITING_CHECK_IN' END AS status_code " +
                "FROM reservations res " +
                "JOIN guests g ON g.guest_id = res.guest_id " +
                "JOIN rooms r ON r.room_id = res.room_id " +
                "JOIN room_types rt ON rt.room_type_id = r.room_type_id " +
                "LEFT JOIN stays s ON s.reservation_id = res.reservation_id " +
                "WHERE res.reservation_id = ?";

        String serviceSql = "SELECT ser.service_name, su.quantity, su.unit_price, (su.quantity * su.unit_price) AS total_amount " +
                "FROM service_usages su JOIN services ser ON ser.service_id = su.service_id WHERE su.stay_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement detailPs = conn.prepareStatement(detailSql)) {
            detailPs.setInt(1, reservationId);
            try (ResultSet detailRs = detailPs.executeQuery()) {
                if (!detailRs.next()) {
                    return null;
                }

                StayDetail detail = new StayDetail();
                detail.setReservationCode(detailRs.getString("reservation_code"));
                detail.setGuestName(detailRs.getString("full_name"));
                detail.setPhone(detailRs.getString("phone"));
                detail.setEmail(detailRs.getString("email"));
                detail.setRoomNumber(detailRs.getString("room_number"));
                detail.setRoomType(detailRs.getString("type_name"));
                detail.setRoomPrice(detailRs.getDouble("price_per_night"));
                Timestamp actualCheckIn = detailRs.getTimestamp("actual_check_in");
                if (actualCheckIn != null) {
                    detail.setActualCheckIn(actualCheckIn.toLocalDateTime());
                }
                Date expectedCheckout = detailRs.getDate("check_out_date");
                if (expectedCheckout != null) {
                    detail.setExpectedCheckOut(expectedCheckout.toLocalDate());
                }
                Timestamp actualCheckOut = detailRs.getTimestamp("actual_check_out");
                if (actualCheckOut != null) {
                    detail.setActualCheckOut(actualCheckOut.toLocalDateTime());
                }
                detail.setNumberOfGuests(detailRs.getInt("number_of_guests"));
                detail.setStatusCode(detailRs.getString("status_code"));

                Integer stayId = null;
                try (PreparedStatement stayPs = conn.prepareStatement("SELECT stay_id FROM stays WHERE reservation_id = ?")) {
                    stayPs.setInt(1, reservationId);
                    try (ResultSet stayRs = stayPs.executeQuery()) {
                        if (stayRs.next()) {
                            stayId = stayRs.getInt("stay_id");
                        }
                    }
                }

                if (stayId != null) {
                    try (PreparedStatement servicePs = conn.prepareStatement(serviceSql)) {
                        servicePs.setInt(1, stayId);
                        try (ResultSet serviceRs = servicePs.executeQuery()) {
                            while (serviceRs.next()) {
                                detail.getServiceUsages().add(new ServiceUsage(
                                        serviceRs.getString("service_name"),
                                        serviceRs.getInt("quantity"),
                                        serviceRs.getDouble("unit_price"),
                                        serviceRs.getDouble("total_amount")
                                ));
                            }
                        }
                    }
                }

                return detail;
            }
        }
    }
}
