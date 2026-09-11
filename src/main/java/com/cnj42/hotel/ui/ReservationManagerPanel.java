package com.cnj42.hotel.ui;

import com.cnj42.hotel.model.Reservation;
import com.cnj42.hotel.service.ReservationService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class ReservationManagerPanel extends JPanel {

    private final ReservationService reservationService = new ReservationService();
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final Integer currentUserId;

    public ReservationManagerPanel() {
        this(null);
    }

    public ReservationManagerPanel(Integer currentUserId) {
        this.currentUserId = currentUserId;
        final com.cnj42.hotel.service.InvoiceService invoiceService = new com.cnj42.hotel.service.InvoiceService();
        setLayout(new BorderLayout(0, 12));
        setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel top = new JPanel(new BorderLayout(12, 0));
        top.setOpaque(false);
        JLabel title = new JLabel("Quản lý lưu trú");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        top.add(title, BorderLayout.WEST);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        controls.setOpaque(false);
        JTextField search = new JTextField(); search.setPreferredSize(new Dimension(240, 34));
        JComboBox<String> statusFilter = new JComboBox<>(new String[]{"Tất cả", "PENDING", "CONFIRMED", "CHECKED_IN", "COMPLETED", "CANCELLED"});
        JButton addBtn = new JButton("Tạo đặt phòng");
        addBtn.addActionListener(e -> createReservationDialog());
        JButton walkIn = new JButton("Khách vãng lai - Check-in");
        walkIn.addActionListener(e -> doWalkIn());
        controls.add(search); controls.add(statusFilter); controls.add(addBtn);
        controls.add(walkIn);
        top.add(controls, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new Object[]{"ID","Mã","Khách","Phòng","Nhận","Trả","Trạng thái","Hành động"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return column == 7; }
        };

        table = new JTable(tableModel);
        table.setRowHeight(40);
        table.getColumnModel().getColumn(7).setCellRenderer((tbl,value,isSel,hasFocus,row,col) -> {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER,6,6));
            JButton edit = new JButton("Sửa"); JButton cancel = new JButton("Hủy"); JButton checkin = new JButton("Check-in"); JButton checkout = new JButton("Check-out");
            // determine enabled state from reservation value
            if (value instanceof com.cnj42.hotel.model.Reservation) {
                com.cnj42.hotel.model.Reservation rr = (com.cnj42.hotel.model.Reservation) value;
                String st = rr.getStatus();
                edit.setEnabled(!"CANCELLED".equals(st) && !"COMPLETED".equals(st));
                cancel.setEnabled(!"CANCELLED".equals(st) && !"COMPLETED".equals(st));
                checkin.setEnabled("CONFIRMED".equals(st));
                checkout.setEnabled("CHECKED_IN".equals(st));
            }
            p.add(edit); p.add(cancel); p.add(checkin); p.add(checkout);
            p.setBackground(isSel?tbl.getSelectionBackground():Color.WHITE);
            return p;
        });

        table.getColumnModel().getColumn(7).setCellEditor(new ReservationActionEditor());

        add(new JScrollPane(table), BorderLayout.CENTER);

        refreshReservations();
    }

    private void refreshReservations() {
        tableModel.setRowCount(0);
        List<Reservation> list = reservationService.listReservations(null, null);
        for (Reservation r : list) {
            tableModel.addRow(new Object[]{r.getReservationId(), r.getReservationCode(), r.getGuestName(), r.getRoomNumber(), r.getCheckInDate(), r.getCheckOutDate(), r.getStatus(), r});
        }
    }

    private void createReservationDialog() {
        ReservationDialog dlg = new ReservationDialog(SwingUtilities.getWindowAncestor(this), null, currentUserId);
        dlg.setVisible(true);
        if (dlg.isSaved()) refreshReservations();
    }

    private void doWalkIn() {
        // simple dialog: choose or create guest, choose room, number, note
        JDialog d = new JDialog(SwingUtilities.getWindowAncestor(this), "Khách vãng lai - Check-in", Dialog.ModalityType.APPLICATION_MODAL);
        JPanel p = new JPanel(new GridBagLayout()); GridBagConstraints c = new GridBagConstraints(); c.insets = new Insets(6,6,6,6); c.fill = GridBagConstraints.HORIZONTAL;
        class Item { int id; String label; Item(int id, String label){this.id=id;this.label=label;} @Override public String toString(){return label + " ("+id+")";} }
        JComboBox<Item> guestBox = new JComboBox<>();
        try (java.sql.Connection conn = com.cnj42.hotel.utils.DBConnection.getConnection(); java.sql.PreparedStatement ps = conn.prepareStatement("SELECT guest_id, full_name FROM guests ORDER BY full_name"); java.sql.ResultSet rs = ps.executeQuery()) {
            while (rs.next()) guestBox.addItem(new Item(rs.getInt(1), rs.getString(2)));
        } catch (java.sql.SQLException ex) { System.err.println("Lỗi load guests: " + ex.getMessage()); }
        JButton addG = new JButton("+ Khách mới"); addG.addActionListener(e -> { GuestDialog gd = new GuestDialog(d); gd.setVisible(true); if (gd.isSaved()) { guestBox.removeAllItems(); try (java.sql.Connection conn = com.cnj42.hotel.utils.DBConnection.getConnection(); java.sql.PreparedStatement ps = conn.prepareStatement("SELECT guest_id, full_name FROM guests ORDER BY full_name"); java.sql.ResultSet rs = ps.executeQuery()) { while (rs.next()) guestBox.addItem(new Item(rs.getInt(1), rs.getString(2))); } catch (java.sql.SQLException ex) { } } });
        JComboBox<Item> roomBox = new JComboBox<>();
        try (java.sql.Connection conn = com.cnj42.hotel.utils.DBConnection.getConnection(); java.sql.PreparedStatement ps = conn.prepareStatement("SELECT room_id, room_number FROM rooms WHERE status = 'AVAILABLE' ORDER BY room_number"); java.sql.ResultSet rs = ps.executeQuery()) {
            while (rs.next()) roomBox.addItem(new Item(rs.getInt(1), rs.getString(2)));
        } catch (java.sql.SQLException ex) { System.err.println("Lỗi load rooms: " + ex.getMessage()); }
        JTextField numField = new JTextField("1",6);
        JTextField noteField = new JTextField(20);
        c.gridx=0; c.gridy=0; p.add(new JLabel("Khách:"), c); c.gridx=1; p.add(guestBox, c); c.gridx=2; p.add(addG, c);
        c.gridx=0; c.gridy=1; p.add(new JLabel("Phòng:"), c); c.gridx=1; p.add(roomBox, c);
        c.gridx=0; c.gridy=2; p.add(new JLabel("Số khách:"), c); c.gridx=1; p.add(numField, c);
        c.gridx=0; c.gridy=3; p.add(new JLabel("Ghi chú:"), c); c.gridx=1; p.add(noteField, c);
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT)); JButton ok = new JButton("Check-in"); JButton cancel = new JButton("Hủy"); btns.add(ok); btns.add(cancel);
            ok.addActionListener(e -> {
            try {
                    Item g = (Item) guestBox.getSelectedItem(); Item r = (Item) roomBox.getSelectedItem(); int num = Integer.parseInt(numField.getText().trim()); String note = noteField.getText().trim();
                    if (g == null || r == null) { JOptionPane.showMessageDialog(d, "Chọn khách và phòng"); return; }
                    int rid = reservationService.walkInCheckIn(g.id, r.id, num, note, currentUserId);
                    if (rid > 0) { JOptionPane.showMessageDialog(d, "Khách đã nhận phòng. Reservation ID=" + rid); d.dispose(); refreshReservations(); }
                    else JOptionPane.showMessageDialog(d, "Check-in thất bại");
            } catch (Exception ex) { JOptionPane.showMessageDialog(d, "Dữ liệu không hợp lệ"); }
        });
        cancel.addActionListener(e -> d.dispose());
        d.getContentPane().setLayout(new BorderLayout()); d.getContentPane().add(p, BorderLayout.CENTER); d.getContentPane().add(btns, BorderLayout.SOUTH); d.pack(); d.setLocationRelativeTo(this); d.setVisible(true);
    }

    private class ReservationActionEditor extends AbstractCellEditor implements javax.swing.table.TableCellEditor {
        private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER,6,6));
        private final JButton edit = new JButton("Sửa");
        private final JButton cancel = new JButton("Hủy");
        private final JButton checkin = new JButton("Check-in");
        private final JButton checkout = new JButton("Check-out");
        private int row;

        ReservationActionEditor() {
            panel.add(edit); panel.add(cancel); panel.add(checkin); panel.add(checkout);
            edit.addActionListener(e -> { stopCellEditing(); int id = Integer.parseInt(table.getValueAt(row,0).toString()); ReservationDialog dlg = new ReservationDialog(SwingUtilities.getWindowAncestor(panel), id, currentUserId); dlg.setVisible(true); if (dlg.isSaved()) refreshReservations(); });
            cancel.addActionListener(e -> { stopCellEditing(); int id = Integer.parseInt(table.getValueAt(row,0).toString()); com.cnj42.hotel.model.Reservation rr = (com.cnj42.hotel.model.Reservation)table.getValueAt(row,7); int roomId = rr.getRoomId(); if (reservationService.cancelReservation(id, roomId)) refreshReservations(); });
            checkin.addActionListener(e -> { stopCellEditing(); int id = Integer.parseInt(table.getValueAt(row,0).toString()); com.cnj42.hotel.model.Reservation rr = (com.cnj42.hotel.model.Reservation)table.getValueAt(row,7); int roomId = rr.getRoomId(); boolean ok = (currentUserId!=null) ? reservationService.checkIn(id, roomId, currentUserId) : reservationService.checkIn(id, roomId); if (ok) refreshReservations(); });
            checkout.addActionListener(e -> { stopCellEditing(); int id = Integer.parseInt(table.getValueAt(row,0).toString()); com.cnj42.hotel.model.Reservation rr = (com.cnj42.hotel.model.Reservation)table.getValueAt(row,7); int roomId = rr.getRoomId(); boolean ok = (currentUserId!=null) ? reservationService.checkOut(id, roomId, currentUserId) : reservationService.checkOut(id, roomId); if (ok) {
                    refreshReservations();
                    Integer stayId = reservationService.getLatestStayIdForReservation(id);
                    if (stayId != null) {
                        int choice = JOptionPane.showConfirmDialog(panel, "Khách trả phòng. Bạn muốn lập hóa đơn ngay?", "Check-out", JOptionPane.YES_NO_OPTION);
                        if (choice == JOptionPane.YES_OPTION) {
                            com.cnj42.hotel.service.InvoiceService inv = new com.cnj42.hotel.service.InvoiceService();
                            int invId = inv.createInvoiceForStay(stayId, currentUserId, 0.0);
                            if (invId > 0) JOptionPane.showMessageDialog(panel, "Đã tạo hóa đơn ID=" + invId);
                            else JOptionPane.showMessageDialog(panel, "Tạo hóa đơn thất bại", "Lỗi", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                } });
        }

        @Override public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) { this.row = row; panel.setBackground(table.getSelectionBackground()); return panel; }
        @Override public Object getCellEditorValue() { return null; }
    }

    // customize renderer so buttons can be enabled/disabled based on status when showing
    @Override public void addNotify() {
        super.addNotify();
        // ensure reservations refreshed when panel shown
        SwingUtilities.invokeLater(this::refreshReservations);
    }
}
