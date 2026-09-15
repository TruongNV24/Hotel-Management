package com.cnj42.hotel.ui;

import com.cnj42.hotel.service.InvoiceService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.cnj42.hotel.utils.DBConnection;

public class PaymentManagerPanel extends JPanel {

    private static final Color PRIMARY = new Color(105, 78, 210);
    private static final Color BACKGROUND = new Color(246, 247, 251);
    private static final Color CARD = Color.WHITE;
    private static final Color BORDER = new Color(230, 234, 241);
    private static final Color TEXT_DARK = new Color(35, 40, 52);
    private static final Color TEXT_MUTED = new Color(120, 125, 140);
    private static final Color GREEN = new Color(45, 166, 108);
    private static final Color ORANGE = new Color(225, 143, 55);
    private static final Color RED = new Color(210, 82, 82);

    private final DefaultTableModel tableModel;
    private final JTable table;
    private final InvoiceService invoiceService = new InvoiceService();
    private final Integer currentUserId;
    private final JLabel invoiceCountLabel = new JLabel("0");
    private final JLabel paidCountLabel = new JLabel("0");
    private final JLabel unpaidCountLabel = new JLabel("0");
    private final JLabel revenueLabel = new JLabel("0 VND");
    private final JTextField searchField = new JTextField();
    private final JComboBox<String> statusFilter = new JComboBox<>(new String[]{"Tất cả", "PAID", "UNPAID", "CANCELLED"});

    public PaymentManagerPanel() { this(null); }

    public PaymentManagerPanel(Integer currentUserId) {
        this.currentUserId = currentUserId;
        setLayout(new BorderLayout(0, 16));
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(14, 16, 18, 16));

        JPanel north = new JPanel(new BorderLayout(0, 14));
        north.setOpaque(false);
        north.add(buildHeader(), BorderLayout.NORTH);
        north.add(buildStats(), BorderLayout.CENTER);
        add(north, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
            new Object[]{"ID","Mã","Stay ID","Phòng","Tiền phòng","Tiền dịch vụ","Giảm","Thuế","Tổng","Trạng thái","Hành động"}, 
            0
        ) {
            @Override public boolean isCellEditable(int row, int col) { return col == 10; }
        };

        table = new JTable(tableModel);
        table.setRowHeight(48);
        table.setFillsViewportHeight(true);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 6));
        table.setSelectionBackground(new Color(238, 240, 255));
        table.setSelectionForeground(TEXT_DARK);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        table.getTableHeader().setBackground(Color.WHITE);
        table.getTableHeader().setForeground(TEXT_DARK);
        table.getTableHeader().setReorderingAllowed(false);
        table.setDefaultRenderer(Object.class, new InvoiceCellRenderer());
        table.getColumnModel().getColumn(10).setPreferredWidth(150);
        table.getColumnModel().getColumn(10).setMinWidth(140);
        table.getColumnModel().getColumn(10).setMaxWidth(170);
        table.getColumnModel().getColumn(0).setPreferredWidth(45);
        table.getColumnModel().getColumn(1).setPreferredWidth(145);
        table.getColumnModel().getColumn(2).setPreferredWidth(70);
        table.getColumnModel().getColumn(3).setPreferredWidth(70);
        table.getColumnModel().getColumn(8).setPreferredWidth(120);
        table.getColumnModel().getColumn(10).setCellRenderer((tbl, value, isSel, hasFocus, row, col) -> {
            return createActionPanel(isSel ? tbl.getSelectionBackground() : CARD);
        });

        table.getColumnModel().getColumn(10).setCellEditor(new PaymentActionEditor());

        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setBackground(CARD);
        content.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(12, 12, 12, 12)
        ));
        content.add(buildToolbar(), BorderLayout.NORTH);
        content.add(new JScrollPane(table), BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);

        refreshInvoices();
    }

    private JPanel buildHeader() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Quản lý thanh toán");
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setForeground(TEXT_DARK);
        JLabel subtitle = new JLabel("Theo dõi hóa đơn và lịch sử thanh toán từ các lượt checkout");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(TEXT_MUTED);
        panel.add(title);
        panel.add(Box.createVerticalStrut(4));
        panel.add(subtitle);
        return panel;
    }

    private JPanel buildStats() {
        JPanel panel = new JPanel(new GridLayout(1, 4, 14, 0));
        panel.setOpaque(false);
        panel.add(createStatCard("Tổng hóa đơn", invoiceCountLabel, "Đã phát sinh", PRIMARY));
        panel.add(createStatCard("Đã thanh toán", paidCountLabel, "Trạng thái PAID", GREEN));
        panel.add(createStatCard("Chưa thanh toán", unpaidCountLabel, "Cần theo dõi", ORANGE));
        panel.add(createStatCard("Doanh thu", revenueLabel, "Tổng hóa đơn đã trả", RED));
        return panel;
    }

    private JPanel createStatCard(String title, JLabel value, String helper, Color accent) {
        JPanel card = new JPanel(new BorderLayout(10, 4));
        card.setBackground(CARD);
        card.setBorder(BorderFactory.createCompoundBorder(new LineBorder(BORDER, 1, true), new EmptyBorder(13, 15, 13, 15)));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        titleLabel.setForeground(TEXT_MUTED);
        value.setFont(new Font("Segoe UI", Font.BOLD, 21));
        value.setForeground(accent);
        JLabel helperLabel = new JLabel(helper);
        helperLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        helperLabel.setForeground(TEXT_MUTED);
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(titleLabel);
        text.add(Box.createVerticalStrut(3));
        text.add(value);
        text.add(Box.createVerticalStrut(2));
        text.add(helperLabel);
        card.add(text, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout(10, 0));
        toolbar.setOpaque(false);
        searchField.setPreferredSize(new Dimension(280, 34));
        searchField.setBorder(BorderFactory.createCompoundBorder(new LineBorder(BORDER, 1, true), new EmptyBorder(0, 10, 0, 10)));
        searchField.putClientProperty("JTextField.placeholderText", "Tìm mã hóa đơn hoặc phòng");
        searchField.addActionListener(e -> refreshInvoices());
        toolbar.add(searchField, BorderLayout.WEST);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        controls.setOpaque(false);
        statusFilter.setPreferredSize(new Dimension(125, 34));
        statusFilter.addActionListener(e -> refreshInvoices());
        JButton refresh = createActionButton("Làm mới", PRIMARY);
        refresh.addActionListener(e -> refreshInvoices());
        controls.add(new JLabel("Trạng thái"));
        controls.add(statusFilter);
        controls.add(refresh);
        toolbar.add(controls, BorderLayout.EAST);
        return toolbar;
    }

    private JButton createActionButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setBackground(color);
        button.setForeground(Color.BLACK);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(7, 10, 7, 10));
        button.setFont(new Font("Segoe UI", Font.BOLD, 11));
        return button;
    }

    private JPanel createActionPanel(Color background) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 2));
        panel.setOpaque(true);
        panel.setBackground(background);
        panel.add(createActionButton("Hủy", RED));
        panel.add(createActionButton("In hóa đơn", PRIMARY));
        return panel;
    }

    private void refreshInvoices() {
        tableModel.setRowCount(0);
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim();
        String selectedStatus = (String) statusFilter.getSelectedItem();
        String sql = "SELECT i.invoice_id, i.invoice_code, i.stay_id, r.room_number, i.room_amount, i.service_amount, " +
                     "i.discount_amount, i.tax_amount, i.total_amount, i.status " +
                     "FROM invoices i " +
                     "JOIN stays s ON i.stay_id = s.stay_id " +
                 "JOIN rooms r ON s.room_id = r.room_id " +
                 "WHERE (? = '' OR i.invoice_code LIKE ? OR r.room_number LIKE ?) " +
                 "AND (? = 'Tất cả' OR i.status = ?) " +
                     "ORDER BY i.issued_at DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             PreparedStatement statsPs = conn.prepareStatement(
                 "SELECT COUNT(*) AS invoice_count, " +
                 "SUM(CASE WHEN status = 'PAID' THEN 1 ELSE 0 END) AS paid_count, " +
                 "SUM(CASE WHEN status = 'UNPAID' THEN 1 ELSE 0 END) AS unpaid_count, " +
                 "COALESCE(SUM(CASE WHEN status = 'PAID' THEN total_amount ELSE 0 END), 0) AS revenue " +
                 "FROM invoices")) {
            String like = "%" + keyword + "%";
            ps.setString(1, keyword);
            ps.setString(2, like);
            ps.setString(3, like);
            ps.setString(4, selectedStatus == null ? "Tất cả" : selectedStatus);
            ps.setString(5, selectedStatus == null ? "Tất cả" : selectedStatus);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tableModel.addRow(new Object[]{
                        rs.getInt("invoice_id"),
                        rs.getString("invoice_code"),
                        rs.getInt("stay_id"),
                        rs.getString("room_number"),
                        rs.getDouble("room_amount"),
                        rs.getDouble("service_amount"),
                        rs.getDouble("discount_amount"),
                        rs.getDouble("tax_amount"),
                        rs.getDouble("total_amount"),
                        rs.getString("status"),
                        rs.getInt("invoice_id")
                    });
                }
            }
            try (ResultSet stats = statsPs.executeQuery()) {
                if (stats.next()) {
                    invoiceCountLabel.setText(String.valueOf(stats.getInt("invoice_count")));
                    paidCountLabel.setText(String.valueOf(stats.getInt("paid_count")));
                    unpaidCountLabel.setText(String.valueOf(stats.getInt("unpaid_count")));
                    revenueLabel.setText(formatMoney(stats.getDouble("revenue")));
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi lấy invoices: " + e.getMessage());
        }
    }

    private String formatMoney(double amount) {
        return String.format("%,.0f VND", amount);
    }

    private static class InvoiceCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                        boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setBorder(new EmptyBorder(0, 8, 0, 8));
            label.setForeground(TEXT_DARK);
            label.setBackground(isSelected ? table.getSelectionBackground() : CARD);
            label.setFont(new Font("Segoe UI", Font.PLAIN, 12));

            if (column >= 4 && column <= 8 && value instanceof Number) {
                label.setText(String.format("%,.0f VND", ((Number) value).doubleValue()));
                label.setHorizontalAlignment(SwingConstants.RIGHT);
            } else if (column == 9 && value != null) {
                String status = value.toString();
                label.setText(status);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setFont(new Font("Segoe UI", Font.BOLD, 11));
                if (!isSelected) {
                    if ("PAID".equals(status)) {
                        label.setForeground(GREEN);
                    } else if ("UNPAID".equals(status)) {
                        label.setForeground(ORANGE);
                    } else {
                        label.setForeground(RED);
                    }
                }
            } else {
                label.setHorizontalAlignment(SwingConstants.LEFT);
            }
            return label;
        }
    }

    private void createInvoiceForStay() {
        String sql = "SELECT s.stay_id, r.room_number FROM stays s " +
                     "LEFT JOIN invoices i ON s.stay_id = i.stay_id " +
                     "JOIN rooms r ON s.room_id = r.room_id " +
                     "WHERE i.stay_id IS NULL AND s.status = 'CHECKED_IN'";
        try (Connection conn = DBConnection.getConnection(); 
             PreparedStatement ps = conn.prepareStatement(sql); 
             ResultSet rs = ps.executeQuery()) {
            java.util.List<Item> items = new java.util.ArrayList<>();
            while (rs.next()) items.add(new Item(rs.getInt(1), rs.getString(2)));
            if (items.isEmpty()) { 
                JOptionPane.showMessageDialog(this, "Không có stays cần lập hóa đơn.", "Thông báo", JOptionPane.INFORMATION_MESSAGE); 
                return; 
            }
            Item sel = (Item) JOptionPane.showInputDialog(this, "Chọn stay để lập hóa đơn", "Tạo hóa đơn", JOptionPane.PLAIN_MESSAGE, null, items.toArray(), items.get(0));
            if (sel != null) {
                int invoiceId = invoiceService.createInvoiceForStay(sel.id, currentUserId, 0.0);
                if (invoiceId > 0) { 
                    JOptionPane.showMessageDialog(this, "Đã tạo hóa đơn ID=" + invoiceId); 
                    refreshInvoices(); 
                }
                else JOptionPane.showMessageDialog(this, "Tạo hóa đơn thất bại", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException e) {
            System.err.println("Lỗi fetch stays for invoice: " + e.getMessage());
        }
    }

    private static class Item {
        final int id; 
        final String label; 
        Item(int id, String label) { 
            this.id = id; 
            this.label = label; 
        } 
        @Override public String toString() { 
            return label + " (ID: " + id + ")"; 
        }
    }

    private class PaymentActionEditor extends AbstractCellEditor implements javax.swing.table.TableCellEditor {
        private final JPanel panel = createActionPanel(table.getSelectionBackground());
        private final JButton cancel = (JButton) panel.getComponent(0);
        private final JButton print = (JButton) panel.getComponent(1);
        private int row;

        PaymentActionEditor() {
            cancel.addActionListener(e -> {
                stopCellEditing();
                Object idObj = table.getValueAt(row, 0);
                if (idObj == null) return;
                int invoiceId = Integer.parseInt(idObj.toString());
                cancelInvoiceDialog(invoiceId);
            });
            
            print.addActionListener(e -> {
                stopCellEditing();
                Object idObj = table.getValueAt(row, 0);
                if (idObj == null) return;
                int invoiceId = Integer.parseInt(idObj.toString());
                showInvoicePreview(invoiceId);
            });
        }

        @Override 
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) { 
            this.row = row; 
            panel.setBackground(table.getSelectionBackground()); 
            return panel; 
        }
        
        @Override 
        public Object getCellEditorValue() { 
            return null; 
        }
    }

    private void applyDiscountCode(int invoiceId) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Áp dụng mã giảm giá", true);
        dialog.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
        dialog.setSize(450, 150);
        dialog.setLocationRelativeTo(this);

        JLabel discountLabel = new JLabel("Nhập % giảm giá (0-100):");
        JTextField discountField = new JTextField(10);
        JButton applyBtn = new JButton("Áp dụng");
        JButton cancelBtn = new JButton("Hủy");

        applyBtn.addActionListener(e -> {
            try {
                double percentage = Double.parseDouble(discountField.getText());
                if (percentage < 0 || percentage > 100) {
                    JOptionPane.showMessageDialog(dialog, "Phần trăm phải từ 0 đến 100", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Lấy tổng tiền hóa đơn
                String invSql = "SELECT total_amount FROM invoices WHERE invoice_id = ?";
                try (Connection conn = DBConnection.getConnection(); 
                     PreparedStatement ps = conn.prepareStatement(invSql)) {
                    ps.setInt(1, invoiceId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            double totalAmount = rs.getDouble("total_amount");
                            double discountAmount = totalAmount * percentage / 100;

                            // Áp dụng giảm giá
                            if (invoiceService.applyVoucher(invoiceId, discountAmount)) {
                                JOptionPane.showMessageDialog(dialog, 
                                    String.format("Áp dụng thành công!\nGiảm: %.0f%% = %.0f VND", percentage, discountAmount), 
                                    "Thành công", 
                                    JOptionPane.INFORMATION_MESSAGE);
                                refreshInvoices();
                                dialog.dispose();
                            } else {
                                JOptionPane.showMessageDialog(dialog, "Lỗi khi áp dụng giảm giá", "Lỗi", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Vui lòng nhập số hợp lệ", "Lỗi", JOptionPane.ERROR_MESSAGE);
            } catch (SQLException ex) {
                System.err.println("Lỗi: " + ex.getMessage());
                JOptionPane.showMessageDialog(dialog, "Lỗi hệ thống: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelBtn.addActionListener(e -> dialog.dispose());

        dialog.add(discountLabel);
        dialog.add(discountField);
        dialog.add(applyBtn);
        dialog.add(cancelBtn);

        dialog.setVisible(true);
    }

    private void confirmPaymentDialog(int invoiceId) {
        // Lấy thông tin hóa đơn
        String sql = "SELECT total_amount, status FROM invoices WHERE invoice_id = ?";
        try (Connection conn = DBConnection.getConnection(); 
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double totalAmount = rs.getDouble("total_amount");
                    String status = rs.getString("status");

                    if ("PAID".equals(status)) {
                        JOptionPane.showMessageDialog(this, "Hóa đơn này đã được thanh toán rồi!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }

                    JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Xác nhận thanh toán", true);
                    dialog.setLayout(new GridLayout(5, 2, 10, 10));
                    dialog.setSize(400, 220);
                    dialog.setLocationRelativeTo(this);

                    JLabel totalLabel = new JLabel("Tổng tiền:");
                    JLabel totalValue = new JLabel(String.format("%.0f VND", totalAmount));
                    totalValue.setFont(new Font("Segoe UI", Font.BOLD, 14));
                    totalValue.setForeground(new Color(192, 0, 0));

                    JLabel amountLabel = new JLabel("Nhập tiền thanh toán:");
                    JTextField amountField = new JTextField(String.format("%.0f", totalAmount));

                    JLabel methodLabel = new JLabel("Phương thức thanh toán:");
                    String[] methods = {"CASH", "CARD", "BANK_TRANSFER"};
                    JComboBox<String> methodBox = new JComboBox<>(methods);

                    JButton confirmBtn = new JButton("Xác nhận thanh toán");
                    JButton cancelBtn = new JButton("Hủy");

                    confirmBtn.addActionListener(e -> {
                        try {
                            double amount = Double.parseDouble(amountField.getText());
                            String method = (String) methodBox.getSelectedItem();

                            if (amount <= 0) {
                                JOptionPane.showMessageDialog(dialog, "Số tiền phải lớn hơn 0", "Lỗi", JOptionPane.ERROR_MESSAGE);
                                return;
                            }

                            if (invoiceService.confirmPayment(invoiceId, amount, method, currentUserId)) {
                                String message = String.format("Xác nhận thanh toán thành công!\nĐã nhận: %.0f VND\nPhương thức: %s", amount, method);
                                if (amount < totalAmount) {
                                    message += String.format("\nCòn thiếu: %.0f VND", totalAmount - amount);
                                }
                                JOptionPane.showMessageDialog(dialog, message, "Thành công", JOptionPane.INFORMATION_MESSAGE);
                                refreshInvoices();
                                dialog.dispose();
                            } else {
                                JOptionPane.showMessageDialog(dialog, "Lỗi khi xác nhận thanh toán", "Lỗi", JOptionPane.ERROR_MESSAGE);
                            }
                        } catch (NumberFormatException ex) {
                            JOptionPane.showMessageDialog(dialog, "Số tiền không hợp lệ", "Lỗi", JOptionPane.ERROR_MESSAGE);
                        }
                    });

                    cancelBtn.addActionListener(e -> dialog.dispose());

                    dialog.add(totalLabel);
                    dialog.add(totalValue);
                    dialog.add(amountLabel);
                    dialog.add(amountField);
                    dialog.add(methodLabel);
                    dialog.add(methodBox);
                    dialog.add(confirmBtn);
                    dialog.add(cancelBtn);

                    dialog.setVisible(true);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi lấy thông tin hóa đơn: " + e.getMessage());
        }
    }

    private void cancelInvoiceDialog(int invoiceId) {
        String sql = "SELECT status, invoice_code FROM invoices WHERE invoice_id = ?";
        try (Connection conn = DBConnection.getConnection(); 
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String status = rs.getString("status");
                    String invoiceCode = rs.getString("invoice_code");
                    
                    if ("PAID".equals(status)) {
                        JOptionPane.showMessageDialog(this, "Không thể hủy hóa đơn đã thanh toán!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    if ("CANCELLED".equals(status)) {
                        JOptionPane.showMessageDialog(this, "Hóa đơn này đã được hủy rồi!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }

                    int result = JOptionPane.showConfirmDialog(this, 
                        "Bạn có chắc chắn muốn hủy hóa đơn " + invoiceCode + " không?\nHành động này không thể hoàn tác!", 
                        "Xác nhận hủy hóa đơn", 
                        JOptionPane.YES_NO_OPTION, 
                        JOptionPane.WARNING_MESSAGE);

                    if (result == JOptionPane.YES_OPTION) {
                        if (invoiceService.cancelInvoice(invoiceId)) {
                            JOptionPane.showMessageDialog(this, "Đã hủy hóa đơn " + invoiceCode + " thành công", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                            refreshInvoices();
                        } else {
                            JOptionPane.showMessageDialog(this, "Lỗi khi hủy hóa đơn", "Lỗi", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi kiểm tra trạng thái hóa đơn: " + e.getMessage());
        }
    }

    private void showInvoicePreview(int invoiceId) {
        String sql = "SELECT i.invoice_code, i.room_amount, i.service_amount, i.discount_amount, i.tax_amount, i.total_amount, " +
                     "i.status, i.issued_at, g.full_name AS guest_name, r.room_number " +
                     "FROM invoices i " +
                     "JOIN stays s ON i.stay_id = s.stay_id " +
                     "JOIN reservations res ON s.reservation_id = res.reservation_id " +
                     "JOIN guests g ON res.guest_id = g.guest_id " +
                     "JOIN rooms r ON s.room_id = r.room_id " +
                     "WHERE i.invoice_id = ?";
        try (Connection conn = DBConnection.getConnection(); 
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    StringBuilder html = new StringBuilder();
                    html.append("<html><head><meta charset='UTF-8'></head>");
                    html.append("<body style='font-family: Arial; margin: 0; padding: 10px;'>");
                    html.append("<div style='border: 2px solid #333; padding: 20px; max-width: 550px; margin: auto; background-color: #fff;'>");
                    
                    // Header
                    html.append("<center>");
                    html.append("<h2 style='margin: 0 0 5px 0; color: #000;'>===== HÓA ĐƠN THANH TOÁN =====</h2>");
                    html.append("<p style='margin: 0; font-size: 11px; color: #666;'>Khách sạn Quản Lý - Hệ Thống POS</p>");
                    html.append("</center>");
                    html.append("<hr style='border: none; border-top: 1px dashed #999; margin: 10px 0;'>");
                    
                    // Invoice Info
                    html.append("<table style='width: 100%; font-size: 13px; border-collapse: collapse;'>");
                    html.append("<tr>");
                    html.append("<td style='width: 50%;'><strong>Mã HĐ:</strong> ").append(rs.getString("invoice_code")).append("</td>");
                    html.append("<td style='width: 50%; text-align: right;'><strong>Ngày:</strong> ").append(rs.getTimestamp("issued_at")).append("</td>");
                    html.append("</tr>");
                    html.append("<tr>");
                    html.append("<td><strong>Khách hàng:</strong> ").append(rs.getString("guest_name")).append("</td>");
                    html.append("<td style='text-align: right;'><strong>Phòng:</strong> ").append(rs.getString("room_number")).append("</td>");
                    html.append("</tr>");
                    html.append("</table>");
                    html.append("<hr style='border: none; border-top: 1px dashed #999; margin: 10px 0;'>");
                    
                    // Items
                    html.append("<table style='width: 100%; font-size: 12px; border-collapse: collapse;'>");
                    html.append("<tr style='border-bottom: 1px solid #ccc;'>");
                    html.append("<th style='text-align: left; padding: 3px 0;'>Mô tả</th>");
                    html.append("<th style='text-align: right; padding: 3px 0; width: 80px;'>Số tiền</th>");
                    html.append("</tr>");
                    
                    double roomAmount = rs.getDouble("room_amount");
                    double serviceAmount = rs.getDouble("service_amount");
                    
                    html.append("<tr>");
                    html.append("<td style='padding: 3px 0;'>Tiền phòng</td>");
                    html.append("<td style='text-align: right; padding: 3px 0;'>").append(String.format("%.0f", roomAmount)).append(" VND</td>");
                    html.append("</tr>");
                    
                    if (serviceAmount > 0) {
                        html.append("<tr>");
                        html.append("<td style='padding: 3px 0;'>Tiền dịch vụ</td>");
                        html.append("<td style='text-align: right; padding: 3px 0;'>").append(String.format("%.0f", serviceAmount)).append(" VND</td>");
                        html.append("</tr>");
                    }
                    
                    double discountAmount = rs.getDouble("discount_amount");
                    if (discountAmount > 0) {
                        html.append("<tr style='color: green;'>");
                        html.append("<td style='padding: 3px 0;'>Giảm giá</td>");
                        html.append("<td style='text-align: right; padding: 3px 0;'>-").append(String.format("%.0f", discountAmount)).append(" VND</td>");
                        html.append("</tr>");
                    }
                    
                    double taxAmount = rs.getDouble("tax_amount");
                    if (taxAmount > 0) {
                        html.append("<tr>");
                        html.append("<td style='padding: 3px 0;'>Thuế VAT (10%)</td>");
                        html.append("<td style='text-align: right; padding: 3px 0;'>").append(String.format("%.0f", taxAmount)).append(" VND</td>");
                        html.append("</tr>");
                    }
                    
                    html.append("</table>");
                    html.append("<hr style='border: none; border-top: 2px solid #333; margin: 10px 0;'>");
                    
                    // Total
                    double totalAmount = rs.getDouble("total_amount");
                    html.append("<table style='width: 100%; font-size: 14px; font-weight: bold; border-collapse: collapse;'>");
                    html.append("<tr>");
                    html.append("<td style='padding: 5px 0;'>TỔNG CỘNG:</td>");
                    html.append("<td style='text-align: right; padding: 5px 0; color: #c00;'>").append(String.format("%.0f", totalAmount)).append(" VND</td>");
                    html.append("</tr>");
                    html.append("</table>");
                    html.append("<hr style='border: none; border-top: 1px dashed #999; margin: 10px 0;'>");
                    
                    // Status
                    String status = rs.getString("status");
                    String statusText;
                    String statusColor;
                    if ("PAID".equals(status)) {
                        statusText = "✓ ĐÃ THANH TOÁN";
                        statusColor = "green";
                    } else if ("PARTIALLY_PAID".equals(status)) {
                        statusText = "◐ THANH TOÁN MỘT PHẦN";
                        statusColor = "orange";
                    } else if ("CANCELLED".equals(status)) {
                        statusText = "✗ ĐÃ HỦY";
                        statusColor = "red";
                    } else {
                        statusText = "○ CHƯA THANH TOÁN";
                        statusColor = "blue";
                    }
                    html.append("<center style='margin: 10px 0; font-weight: bold; color: ").append(statusColor).append(";'>");
                    html.append(statusText);
                    html.append("</center>");
                    html.append("<hr style='border: none; border-top: 1px dashed #999; margin: 10px 0;'>");
                    
                    // Footer
                    html.append("<center style='margin-top: 15px; font-size: 11px; color: #999;'>");
                    html.append("<p style='margin: 3px 0;'>Cảm ơn bạn đã sử dụng dịch vụ</p>");
                    html.append("<p style='margin: 3px 0;'>Vui lòng bảo quản hóa đơn để kiểm chứng</p>");
                    html.append("</center>");
                    
                    html.append("</div>");
                    html.append("</body></html>");

                    JEditorPane pane = new JEditorPane("text/html", html.toString());
                    pane.setEditable(false);
                    pane.setBackground(Color.WHITE);
                    
                    JScrollPane scroll = new JScrollPane(pane);
                    scroll.setPreferredSize(new Dimension(600, 500));
                    
                    JOptionPane.showMessageDialog(this, scroll, "HÓA ĐƠN - " + rs.getString("invoice_code"), JOptionPane.INFORMATION_MESSAGE);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi tạo preview: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Lỗi khi tải hóa đơn: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
}
