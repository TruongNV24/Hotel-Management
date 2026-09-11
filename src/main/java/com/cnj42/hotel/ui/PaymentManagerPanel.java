package com.cnj42.hotel.ui;

import com.cnj42.hotel.service.InvoiceService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

import com.cnj42.hotel.utils.DBConnection;

public class PaymentManagerPanel extends JPanel {

    private final DefaultTableModel tableModel;
    private final JTable table;
    private final InvoiceService invoiceService = new InvoiceService();
    private final Integer currentUserId;

    public PaymentManagerPanel() { this(null); }

    public PaymentManagerPanel(Integer currentUserId) {
        this.currentUserId = currentUserId;
        setLayout(new BorderLayout(0, 12));
        setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel top = new JPanel(new BorderLayout(8,0)); top.setOpaque(false);
        JLabel title = new JLabel("Quản lý thanh toán");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        top.add(title, BorderLayout.WEST);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)); controls.setOpaque(false);
        JButton createInv = new JButton("Tạo hóa đơn");
        createInv.addActionListener(e -> createInvoiceForStay());
        controls.add(createInv);
        top.add(controls, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new Object[]{"ID","Mã","Stay ID","Phòng","Tiền phòng","Tiền dịch vụ","Giảm","Thuế","Tổng","Trạng thái","Hành động"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return col == 10; }
        };

        table = new JTable(tableModel);
        table.setRowHeight(36);
        table.getColumnModel().getColumn(10).setCellRenderer((tbl, value, isSel, hasFocus, row, col) -> {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER,6,4));
            JButton apply = new JButton("Áp dụng mã"); JButton pay = new JButton("Xác nhận"); JButton cancel = new JButton("Hủy"); JButton print = new JButton("In");
            p.add(apply); p.add(pay); p.add(cancel); p.add(print);
            p.setBackground(isSel?tbl.getSelectionBackground():Color.WHITE);
            return p;
        });

        table.getColumnModel().getColumn(10).setCellEditor(new PaymentActionEditor());

        add(new JScrollPane(table), BorderLayout.CENTER);

        refreshInvoices();
    }

    private void refreshInvoices() {
        tableModel.setRowCount(0);
        String sql = "SELECT invoice_id, invoice_code, stay_id, room_amount, service_amount, discount_amount, tax_amount, total_amount, status FROM invoices ORDER BY issued_at DESC";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                tableModel.addRow(new Object[]{rs.getInt("invoice_id"), rs.getString("invoice_code"), rs.getInt("stay_id"), "-", rs.getDouble("room_amount"), rs.getDouble("service_amount"), rs.getDouble("discount_amount"), rs.getDouble("tax_amount"), rs.getDouble("total_amount"), rs.getString("status"), rs.getInt("invoice_id")});
            }
        } catch (SQLException e) {
            System.err.println("Lỗi lấy invoices: " + e.getMessage());
        }
    }

    private void createInvoiceForStay() {
        String sql = "SELECT s.stay_id, r.room_number FROM stays s LEFT JOIN invoices i ON s.stay_id = i.stay_id JOIN rooms r ON s.room_id = r.room_id WHERE i.stay_id IS NULL";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            java.util.List<Item> items = new java.util.ArrayList<>();
            while (rs.next()) items.add(new Item(rs.getInt(1), rs.getString(2)));
            if (items.isEmpty()) { JOptionPane.showMessageDialog(this, "Không có stays cần lập hóa đơn.", "Thông báo", JOptionPane.INFORMATION_MESSAGE); return; }
            Item sel = (Item) JOptionPane.showInputDialog(this, "Chọn stay", "Tạo hóa đơn", JOptionPane.PLAIN_MESSAGE, null, items.toArray(), items.get(0));
            if (sel != null) {
                int invoiceId = invoiceService.createInvoiceForStay(sel.id, currentUserId, 0.0);
                if (invoiceId > 0) { JOptionPane.showMessageDialog(this, "Đã tạo hóa đơn ID=" + invoiceId); refreshInvoices(); }
                else JOptionPane.showMessageDialog(this, "Tạo hóa đơn thất bại", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException e) {
            System.err.println("Lỗi fetch stays for invoice: " + e.getMessage());
        }
    }

    private static class Item {
        final int id; final String label; Item(int id, String label) { this.id=id; this.label=label; } @Override public String toString() { return label + " (" + id + ")"; }
    }

    private class PaymentActionEditor extends AbstractCellEditor implements javax.swing.table.TableCellEditor {
        private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER,6,4));
        private final JButton apply = new JButton("Áp dụng mã");
        private final JButton pay = new JButton("Xác nhận");
        private final JButton cancel = new JButton("Hủy");
        private final JButton print = new JButton("In");
        private int row;

        PaymentActionEditor() {
            panel.add(apply); panel.add(pay); panel.add(cancel); panel.add(print);
            apply.addActionListener(e -> { stopCellEditing(); int invoiceId = Integer.parseInt(table.getValueAt(row,10).toString()); String code = JOptionPane.showInputDialog(panel, "Mã giảm giá:"); if (code != null) { // simple demo: if code equals SAVE10 subtract 10%
                        if (code.toUpperCase(Locale.ROOT).equals("SAVE10")) {
                            double total = (double) table.getValueAt(row,8);
                            double discount = total * 0.1;
                            if (invoiceService.applyVoucher(invoiceId, discount)) refreshInvoices();
                        } else JOptionPane.showMessageDialog(panel, "Mã không hợp lệ (demo: SAVE10)"); }});
            pay.addActionListener(e -> { stopCellEditing(); int invoiceId = Integer.parseInt(table.getValueAt(row,10).toString()); double total = (double) table.getValueAt(row,8); if (invoiceService.confirmPayment(invoiceId, total, "CASH", null)) refreshInvoices(); });
            cancel.addActionListener(e -> { stopCellEditing(); int invoiceId = Integer.parseInt(table.getValueAt(row,10).toString()); if (invoiceService.cancelInvoice(invoiceId)) refreshInvoices(); });
            print.addActionListener(e -> { stopCellEditing(); int invoiceId = Integer.parseInt(table.getValueAt(row,10).toString()); showInvoicePreview(invoiceId); });
        }

        @Override public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) { this.row = row; panel.setBackground(table.getSelectionBackground()); return panel; }
        @Override public Object getCellEditorValue() { return null; }
    }

    private void showInvoicePreview(int invoiceId) {
        String sql = "SELECT invoice_code, room_amount, service_amount, discount_amount, tax_amount, total_amount, status, issued_at FROM invoices WHERE invoice_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String html = "<html><body>" +
                            "<h2>HÓA ĐƠN " + rs.getString("invoice_code") + "</h2>" +
                            "<p>Tiền phòng: " + rs.getDouble("room_amount") + "</p>" +
                            "<p>Tiền dịch vụ: " + rs.getDouble("service_amount") + "</p>" +
                            "<p>Giảm giá: " + rs.getDouble("discount_amount") + "</p>" +
                            "<p>Thuế: " + rs.getDouble("tax_amount") + "</p>" +
                            "<h3>Tổng: " + rs.getDouble("total_amount") + "</h3>" +
                            "</body></html>";
                    JEditorPane pane = new JEditorPane("text/html", html);
                    pane.setEditable(false);
                    JOptionPane.showMessageDialog(this, new JScrollPane(pane), "Phiếu thanh toán", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi tạo preview: " + e.getMessage());
        }
    }
}
