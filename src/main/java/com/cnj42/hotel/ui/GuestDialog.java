package com.cnj42.hotel.ui;

import com.cnj42.hotel.model.Guest;
import com.cnj42.hotel.service.GuestService;

import javax.swing.*;
import java.awt.*;

public class GuestDialog extends JDialog {
    private boolean saved = false;
    private int createdId = -1;
    private final GuestService guestService = new GuestService();

    private JTextField nameField;
    private JTextField phoneField;
    private JTextField idCardField;

    public GuestDialog(Window owner) {
        super(owner, "Thêm khách", ModalityType.APPLICATION_MODAL);
        initUI();
        pack();
        setLocationRelativeTo(owner);
    }

    private void initUI() {
        JPanel p = new JPanel(new GridLayout(0,2,8,8));
        p.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        p.add(new JLabel("Họ tên:")); nameField = new JTextField(20); p.add(nameField);
        p.add(new JLabel("SĐT:")); phoneField = new JTextField(20); p.add(phoneField);
        p.add(new JLabel("CMND/CCCD:")); idCardField = new JTextField(20); p.add(idCardField);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton save = new JButton("Lưu"); JButton cancel = new JButton("Hủy");
        btns.add(save); btns.add(cancel);
        save.addActionListener(e -> onSave());
        cancel.addActionListener(e -> { saved=false; dispose(); });

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(p, BorderLayout.CENTER);
        getContentPane().add(btns, BorderLayout.SOUTH);
    }

    private void onSave() {
        String name = nameField.getText().trim();
        String phone = phoneField.getText().trim();
        String idCard = idCardField.getText().trim();
        if (name.isEmpty() || phone.isEmpty() || idCard.isEmpty()) { JOptionPane.showMessageDialog(this, "Vui lòng nhập đủ thông tin"); return; }
        Guest guest = new Guest();
        guest.setFullName(name);
        guest.setPhone(phone);
        guest.setIdCard(idCard);
        createdId = guestService.create(guest, null);
        saved = createdId > 0;
        if (saved) {
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Lỗi lưu khách.");
        }
    }

    public boolean isSaved() { return saved; }
    public int getCreatedId() { return createdId; }
}
