package com.cnj42.hotel.ui;

import com.cnj42.hotel.service.ReservationService;
import com.cnj42.hotel.utils.DBConnection;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ReservationDialog extends JDialog {

    private final ReservationService reservationService = new ReservationService();
    private Integer reservationId;
    private Integer createdBy;

    private JComboBox<Item> guestBox;
    private JComboBox<Item> roomBox;
    private JTextField checkInField;
    private JTextField checkOutField;
    private JTextField guestsField;
    private JTextField noteField;
    private boolean saved = false;

    public ReservationDialog(Window owner, Integer reservationId, Integer createdBy) {
        super(owner);
        this.reservationId = reservationId;
        this.createdBy = createdBy;
        setTitle(reservationId == null ? "Tạo đặt phòng" : "Sửa đặt phòng");
        setModal(true);
        initUI();
        if (reservationId != null) loadReservation();
        pack();
        setLocationRelativeTo(owner);
    }

    private void initUI() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6,6,6,6);
        c.fill = GridBagConstraints.HORIZONTAL;

        guestBox = new JComboBox<>(); loadGuests();
        JButton addGuest = new JButton("+ Khách mới");
        addGuest.addActionListener(e -> {
            GuestDialog gd = new GuestDialog(SwingUtilities.getWindowAncestor(this));
            gd.setVisible(true);
            if (gd.isSaved()) {
                loadGuests();
                // select newly created
                for (int i=0;i<guestBox.getItemCount();i++) if (guestBox.getItemAt(i).id == gd.getCreatedId()) { guestBox.setSelectedIndex(i); break; }
            }
        });
        roomBox = new JComboBox<>(); loadRooms();
        checkInField = new JTextField(10); checkOutField = new JTextField(10);
        guestsField = new JTextField("1",5);
        noteField = new JTextField(20);

        c.gridx=0; c.gridy=0; p.add(new JLabel("Khách:"), c);
        c.gridx=1; p.add(guestBox, c);
        c.gridx=2; p.add(addGuest, c);
        c.gridx=0; c.gridy=1; p.add(new JLabel("Phòng:"), c);
        c.gridx=1; p.add(roomBox, c);
        c.gridx=0; c.gridy=2; p.add(new JLabel("Nhận (yyyy-mm-dd):"), c);
        c.gridx=1; p.add(checkInField, c);
        c.gridx=0; c.gridy=3; p.add(new JLabel("Trả (yyyy-mm-dd):"), c);
        c.gridx=1; p.add(checkOutField, c);
        c.gridx=0; c.gridy=4; p.add(new JLabel("Số khách:"), c);
        c.gridx=1; p.add(guestsField, c);
        c.gridx=0; c.gridy=5; p.add(new JLabel("Ghi chú:"), c);
        c.gridx=1; p.add(noteField, c);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton save = new JButton("Lưu");
        JButton cancel = new JButton("Hủy");
        btns.add(save); btns.add(cancel);

        save.addActionListener(e -> onSave());
        cancel.addActionListener(e -> { saved=false; dispose(); });

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(p, BorderLayout.CENTER);
        getContentPane().add(btns, BorderLayout.SOUTH);
    }

    private void loadGuests() {
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT guest_id, full_name FROM guests ORDER BY full_name"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) guestBox.addItem(new Item(rs.getInt(1), rs.getString(2)));
        } catch (SQLException e) { System.err.println("Lỗi load guests: " + e.getMessage()); }
    }

    private void loadRooms() {
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT room_id, room_number FROM rooms WHERE status = 'AVAILABLE' ORDER BY room_number"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) roomBox.addItem(new Item(rs.getInt(1), rs.getString(2)));
        } catch (SQLException e) { System.err.println("Lỗi load rooms: " + e.getMessage()); }
    }

    private void loadReservation() {
        var r = reservationService.getReservationById(reservationId);
        if (r == null) return;
        // select guest
        for (int i=0;i<guestBox.getItemCount();i++) if (guestBox.getItemAt(i).id == r.getGuestId()) guestBox.setSelectedIndex(i);
        // select room (may not be in list)
        boolean found=false;
        for (int i=0;i<roomBox.getItemCount();i++) if (roomBox.getItemAt(i).id == r.getRoomId()) { roomBox.setSelectedIndex(i); found=true; break; }
        if (!found) roomBox.addItem(new Item(r.getRoomId(), r.getRoomNumber())); roomBox.setSelectedIndex(roomBox.getItemCount()-1);
        checkInField.setText(r.getCheckInDate());
        checkOutField.setText(r.getCheckOutDate());
        guestsField.setText(String.valueOf(r.getNumberOfGuests()));
        noteField.setText(r.getNote());
    }

    private void onSave() {
        try {
            Item guest = (Item) guestBox.getSelectedItem();
            Item room = (Item) roomBox.getSelectedItem();
            String in = checkInField.getText().trim();
            String out = checkOutField.getText().trim();
            int num = Integer.parseInt(guestsField.getText().trim());
            String note = noteField.getText().trim();
            if (guest == null || room == null) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn khách và phòng.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (num <= 0) {
                JOptionPane.showMessageDialog(this, "Số khách phải lớn hơn 0.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (in.isEmpty() || out.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập ngày nhận phòng và trả phòng.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!reservationService.isRoomAvailable(room.id, in, out, reservationId)) {
                JOptionPane.showMessageDialog(this, "Phòng này đã có đặt phòng hoặc lưu trú trong khoảng thời gian đã chọn.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (reservationId == null) {
                int id = reservationService.createReservation(guest.id, room.id, in, out, num, note, createdBy);
                if (id > 0) { saved=true; dispose(); return; }
            } else {
                boolean ok = reservationService.updateReservation(reservationId, guest.id, room.id, in, out, num, note);
                if (ok) { saved=true; dispose(); return; }
            }
            JOptionPane.showMessageDialog(this, "Lỗi khi lưu. Kiểm tra dữ liệu.", "Lỗi", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Dữ liệu không hợp lệ: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isSaved() { return saved; }

    private static class Item {
        final int id; final String label;
        Item(int id, String label) { this.id=id; this.label=label; }
        @Override public String toString() { return label + " (" + id + ")"; }
    }
}
