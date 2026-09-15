package com.cnj42.hotel.service;

import com.cnj42.hotel.utils.DBConnection;

import java.sql.*;

public class InvoiceService {

    /**
     * Tạo hóa đơn cho một lần lưu trú
     * Tính toán tiền phòng dựa trên ngày check-in/check-out và giá loại phòng
     * Thu thập các dịch vụ sử dụng và lập chi tiết hóa đơn
     */
    public int createInvoiceForStay(int stayId, Integer createdBy, double discountAmount) {
        String sqlFetch = "SELECT s.stay_id, s.actual_check_in, s.actual_check_out, s.room_id, r.room_number, rt.price_per_night " +
            "FROM stays s JOIN rooms r ON s.room_id = r.room_id JOIN room_types rt ON r.room_type_id = rt.room_type_id WHERE s.stay_id = ?";

        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sqlFetch)) {
            ps.setInt(1, stayId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return -1;
                Timestamp in = rs.getTimestamp("actual_check_in");
                Timestamp out = rs.getTimestamp("actual_check_out");
                double price = rs.getDouble("price_per_night");
                String roomNumber = rs.getString("room_number");

                if (in == null || out == null) return -1;
                long millis = out.getTime() - in.getTime();
                long days = Math.max(1, (int) Math.ceil(millis / (1000.0 * 60 * 60 * 24)));
                double roomAmount = days * price;

                // Tính tiền dịch vụ
                double serviceAmount = 0.0;
                String usagesSql = "SELECT s.service_name, su.quantity, su.unit_price, (su.quantity * su.unit_price) AS amount " +
                    "FROM service_usages su JOIN services s ON su.service_id = s.service_id WHERE su.stay_id = ?";
                java.util.List<java.util.Map<String,Object>> usages = new java.util.ArrayList<>();
                try (PreparedStatement psu = conn.prepareStatement(usagesSql)) {
                    psu.setInt(1, stayId);
                    try (ResultSet rsu = psu.executeQuery()) {
                        while (rsu.next()) {
                            String sname = rsu.getString("service_name");
                            int qty = rsu.getInt("quantity");
                            double up = rsu.getDouble("unit_price");
                            double amt = rsu.getDouble("amount");
                            serviceAmount += amt;
                            java.util.Map<String,Object> item = new java.util.HashMap<>();
                            item.put("name", sname);
                            item.put("qty", qty);
                            item.put("up", up);
                            item.put("amt", amt);
                            usages.add(item);
                        }
                    }
                }

                double tax = 0.0; // Có thể thêm logic tính thuế sau
                double total = roomAmount + serviceAmount - discountAmount + tax;

                String insertInvoice = "INSERT INTO invoices (invoice_code, stay_id, room_amount, service_amount, discount_amount, tax_amount, total_amount, status, created_by) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, 'UNPAID', ?)";
                String code = "INV" + System.currentTimeMillis();
                
                try (PreparedStatement ins = conn.prepareStatement(insertInvoice, Statement.RETURN_GENERATED_KEYS)) {
                    ins.setString(1, code);
                    ins.setInt(2, stayId);
                    ins.setDouble(3, roomAmount);
                    ins.setDouble(4, serviceAmount);
                    ins.setDouble(5, discountAmount);
                    ins.setDouble(6, tax);
                    ins.setDouble(7, total);
                    if (createdBy != null) ins.setInt(8, createdBy); else ins.setNull(8, Types.INTEGER);
                    ins.executeUpdate();
                    
                    try (ResultSet gk = ins.getGeneratedKeys()) {
                        if (gk.next()) {
                            int invoiceId = gk.getInt(1);
                            
                            // Thêm chi tiết hóa đơn: tiền phòng
                            String insDetail = "INSERT INTO invoice_details (invoice_id, item_type, description, quantity, unit_price, amount) VALUES (?, 'ROOM', ?, ?, ?, ?)";
                            try (PreparedStatement pd = conn.prepareStatement(insDetail)) {
                                pd.setInt(1, invoiceId);
                                pd.setString(2, "Room " + roomNumber);
                                pd.setInt(3, (int) days);
                                pd.setDouble(4, price);
                                pd.setDouble(5, roomAmount);
                                pd.executeUpdate();
                            }
                            
                            // Thêm chi tiết hóa đơn: dịch vụ
                            String insSrv = "INSERT INTO invoice_details (invoice_id, item_type, description, quantity, unit_price, amount) VALUES (?, 'SERVICE', ?, ?, ?, ?)";
                            try (PreparedStatement pd2 = conn.prepareStatement(insSrv)) {
                                for (java.util.Map<String,Object> it : usages) {
                                    pd2.setInt(1, invoiceId);
                                    pd2.setString(2, (String) it.get("name"));
                                    pd2.setInt(3, (Integer) it.get("qty"));
                                    pd2.setDouble(4, (Double) it.get("up"));
                                    pd2.setDouble(5, (Double) it.get("amt"));
                                    pd2.executeUpdate();
                                }
                            }
                            return invoiceId;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi tạo hóa đơn: " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Áp dụng mã giảm giá cho hóa đơn
     */
    public boolean applyVoucher(int invoiceId, double discountAmount) {
        String sql = "UPDATE invoices SET discount_amount = discount_amount + ?, " +
                     "total_amount = (room_amount + service_amount - (discount_amount + ?) + tax_amount) " +
                     "WHERE invoice_id = ?";
        try (Connection conn = DBConnection.getConnection(); 
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, discountAmount);
            ps.setDouble(2, discountAmount);
            ps.setInt(3, invoiceId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi áp dụng mã giảm giá: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Hủy hóa đơn (chỉ hủy nếu chưa thanh toán)
     */
    public boolean cancelInvoice(int invoiceId) {
        String sql = "UPDATE invoices SET status = 'CANCELLED' WHERE invoice_id = ? AND status != 'PAID'";
        try (Connection conn = DBConnection.getConnection(); 
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, invoiceId);
            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi hủy hóa đơn: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Xác nhận thanh toán hóa đơn
     * Tạo bản ghi payment và cập nhật trạng thái hóa đơn
     */
    public boolean confirmPayment(int invoiceId, double amount, String method, Integer receivedBy) {
        String insertPayment = "INSERT INTO payments (invoice_id, amount, payment_method, note, received_by) VALUES (?, ?, ?, ?, ?)";
        String updateInvoicePaid = "UPDATE invoices SET status = 'PAID' WHERE invoice_id = ?";
        String updateInvoicePartial = "UPDATE invoices SET status = 'PARTIALLY_PAID' WHERE invoice_id = ?";
        Connection conn = null;
        
        try {
            conn = DBConnection.getConnection();
            try (PreparedStatement p1 = conn.prepareStatement(insertPayment)) {
                conn.setAutoCommit(false);
                
                // Thêm bản ghi thanh toán
                p1.setInt(1, invoiceId);
                p1.setDouble(2, amount);
                p1.setString(3, method);
                p1.setString(4, "Thanh toán từ hệ thống");
                if (receivedBy != null) p1.setInt(5, receivedBy); else p1.setNull(5, Types.INTEGER);
                p1.executeUpdate();
                
                // Kiểm tra tổng tiền đã thanh toán
                String sumSql = "SELECT COALESCE(SUM(amount), 0) as paid FROM payments WHERE invoice_id = ?";
                double paid = 0.0;
                try (PreparedStatement ps = conn.prepareStatement(sumSql)) {
                    ps.setInt(1, invoiceId);
                    try (ResultSet rs = ps.executeQuery()) { 
                        if (rs.next()) paid = rs.getDouble("paid"); 
                    }
                }
                
                // Lấy tổng tiền hóa đơn
                double total = 0.0;
                try (PreparedStatement ps2 = conn.prepareStatement("SELECT total_amount FROM invoices WHERE invoice_id = ?")) {
                    ps2.setInt(1, invoiceId);
                    try (ResultSet rs2 = ps2.executeQuery()) { 
                        if (rs2.next()) total = rs2.getDouble(1); 
                    }
                }
                
                // Cập nhật trạng thái hóa đơn
                if (paid >= total) {
                    try (PreparedStatement upd = conn.prepareStatement(updateInvoicePaid)) { 
                        upd.setInt(1, invoiceId); 
                        upd.executeUpdate(); 
                    }
                } else {
                    try (PreparedStatement upd = conn.prepareStatement(updateInvoicePartial)) { 
                        upd.setInt(1, invoiceId); 
                        upd.executeUpdate(); 
                    }
                }
                
                conn.commit();
                return true;
            }
        } catch (SQLException e) {
            if (conn != null) {
                try { 
                    conn.rollback(); 
                } catch (SQLException rollbackEx) { 
                    System.err.println("Lỗi rollback thanh toán: " + rollbackEx.getMessage()); 
                }
            }
            System.err.println("Lỗi xác nhận thanh toán: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Lấy thông tin chi tiết của hóa đơn
     */
    public java.util.Map<String, Object> getInvoiceDetails(int invoiceId) {
        String sql = "SELECT * FROM invoices WHERE invoice_id = ?";
        try (Connection conn = DBConnection.getConnection(); 
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    java.util.Map<String, Object> invoice = new java.util.HashMap<>();
                    invoice.put("invoice_id", rs.getInt("invoice_id"));
                    invoice.put("invoice_code", rs.getString("invoice_code"));
                    invoice.put("stay_id", rs.getInt("stay_id"));
                    invoice.put("room_amount", rs.getDouble("room_amount"));
                    invoice.put("service_amount", rs.getDouble("service_amount"));
                    invoice.put("discount_amount", rs.getDouble("discount_amount"));
                    invoice.put("tax_amount", rs.getDouble("tax_amount"));
                    invoice.put("total_amount", rs.getDouble("total_amount"));
                    invoice.put("status", rs.getString("status"));
                    invoice.put("issued_at", rs.getTimestamp("issued_at"));
                    return invoice;
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi lấy chi tiết hóa đơn: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Lấy danh sách chi tiết hóa đơn
     */
    public java.util.List<java.util.Map<String, Object>> getInvoiceLineItems(int invoiceId) {
        String sql = "SELECT * FROM invoice_details WHERE invoice_id = ?";
        java.util.List<java.util.Map<String, Object>> items = new java.util.ArrayList<>();
        
        try (Connection conn = DBConnection.getConnection(); 
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    java.util.Map<String, Object> item = new java.util.HashMap<>();
                    item.put("detail_id", rs.getInt("detail_id"));
                    item.put("item_type", rs.getString("item_type"));
                    item.put("description", rs.getString("description"));
                    item.put("quantity", rs.getInt("quantity"));
                    item.put("unit_price", rs.getDouble("unit_price"));
                    item.put("amount", rs.getDouble("amount"));
                    items.add(item);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi lấy chi tiết từng dòng hóa đơn: " + e.getMessage());
            e.printStackTrace();
        }
        return items;
    }
}

