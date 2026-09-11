package com.cnj42.hotel.ui;

import com.cnj42.hotel.model.User;
import com.cnj42.hotel.service.UserService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Objects;

public class AccountManagerPanel extends JPanel {

    private final UserService userService = new UserService();
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JComboBox<String> statusFilter = new JComboBox<>(new String[]{"Tất cả", "ACTIVE", "INACTIVE"});
    private final JTextField searchField = new JTextField();

    public AccountManagerPanel() {
        setLayout(new BorderLayout(0, 12));
        setBorder(new EmptyBorder(12, 12, 12, 12));

        // Header + controls (search + filter + add)
        JPanel top = new JPanel(new BorderLayout(12, 0));
        top.setOpaque(false);

        JLabel title = new JLabel("Quản lý tài khoản");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        top.add(title, BorderLayout.WEST);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        controls.setOpaque(false);

        JTextField search = new JTextField();
        search.setPreferredSize(new Dimension(220, 34));
        search.addActionListener(e -> refreshUsers(search.getText().trim(), statusFilter.getSelectedItem().toString()));
        controls.add(search);

        JComboBox<String> statusFilter = new JComboBox<>(new String[]{"Tất cả", "ACTIVE", "INACTIVE"});
        statusFilter.setPreferredSize(new Dimension(140, 34));
        statusFilter.addActionListener(e -> refreshUsers(search.getText().trim(), statusFilter.getSelectedItem().toString()));
        controls.add(statusFilter);

        JButton addBtn = new JButton("Thêm tài khoản");
        addBtn.addActionListener(e -> openUserDialog(null));
        controls.add(addBtn);

        top.add(controls, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new Object[]{"ID", "Username", "Họ và tên", "Quyền", "Phone", "Email", "Trạng thái", "Hành động"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return column == 7; }
        };

        table = new JTable(tableModel);
        table.setRowHeight(36);
        table.getColumnModel().getColumn(0).setPreferredWidth(40);
        table.getColumnModel().getColumn(7).setPreferredWidth(160);
        // Use editor to make buttons clickable
        table.getColumnModel().getColumn(7).setCellRenderer((tbl, value, isSelected, hasFocus, row, col) -> {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4));
            p.setOpaque(true);
            p.setBackground(isSelected ? tbl.getSelectionBackground() : Color.WHITE);
            JButton edit = new JButton("Sửa");
            JButton del = new JButton("Xóa");
            p.add(edit); p.add(del);
            return p;
        });

        table.getColumnModel().getColumn(7).setCellEditor(new UserActionEditor());

        add(new JScrollPane(table), BorderLayout.CENTER);

        refreshUsers();
    }

    private User loadUserFromRow(int row) {
        User u = new User();
        u.setUserId(Integer.parseInt(tableModel.getValueAt(row, 0).toString()));
        u.setUsername(String.valueOf(tableModel.getValueAt(row, 1)));
        u.setFullName(String.valueOf(tableModel.getValueAt(row, 2)));
        u.setRole(String.valueOf(tableModel.getValueAt(row, 3)));
        u.setPhone(String.valueOf(tableModel.getValueAt(row, 4)));
        u.setEmail(String.valueOf(tableModel.getValueAt(row, 5)));
        u.setStatus(String.valueOf(tableModel.getValueAt(row, 6)));
        return u;
    }

    private void refreshUsers() {
        refreshUsers("", "Tất cả");
    }

    private void refreshUsers(String keyword, String status) {
        tableModel.setRowCount(0);
        List<User> users = userService.getAllUsers();
        for (User u : users) {
            if (keyword != null && !keyword.isBlank()) {
                String k = keyword.toLowerCase();
                if (!(String.valueOf(u.getUsername()).toLowerCase().contains(k) || String.valueOf(u.getFullName()).toLowerCase().contains(k))) continue;
            }
            if (status != null && !"Tất cả".equals(status) && !status.equals(u.getStatus())) continue;
            tableModel.addRow(new Object[]{u.getUserId(), u.getUsername(), u.getFullName(), u.getRole(), u.getPhone(), u.getEmail(), u.getStatus(), u});
        }
    }

    // Table cell editor for action buttons
    private class UserActionEditor extends AbstractCellEditor implements TableCellEditor {
        private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4));
        private final JButton editBtn = new JButton("Sửa");
        private final JButton delBtn = new JButton("Xóa");
        private int editingRow = -1;

        UserActionEditor() {
            panel.add(editBtn);
            panel.add(delBtn);
            editBtn.addActionListener(e -> {
                stopCellEditing();
                openUserDialog(loadUserFromRow(editingRow));
            });
            delBtn.addActionListener(e -> {
                stopCellEditing();
                int userId = Integer.parseInt(table.getValueAt(editingRow, 0).toString());
                deleteUser(userId);
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            this.editingRow = row;
            panel.setBackground(table.getSelectionBackground());
            return panel;
        }

        @Override
        public Object getCellEditorValue() { return null; }
    }

    private void openUserDialog(User user) {
        JTextField username = new JTextField();
        JPasswordField password = new JPasswordField();
        JTextField fullname = new JTextField();
        JComboBox<String> role = new JComboBox<>(new String[]{"ADMIN", "MANAGER", "RECEPTIONIST"});
        JTextField phone = new JTextField();
        JTextField email = new JTextField();
        JComboBox<String> status = new JComboBox<>(new String[]{"ACTIVE", "INACTIVE"});

        if (user != null) {
            username.setText(user.getUsername());
            password.setText(user.getPassword());
            fullname.setText(user.getFullName());
            role.setSelectedItem(user.getRole());
            phone.setText(user.getPhone());
            email.setText(user.getEmail());
            status.setSelectedItem(user.getStatus());
        }

        JPanel panel = new JPanel(new GridLayout(0, 1, 6, 6));
        panel.add(new JLabel("Username")); panel.add(username);
        panel.add(new JLabel("Password")); panel.add(password);
        panel.add(new JLabel("Họ và tên")); panel.add(fullname);
        panel.add(new JLabel("Quyền")); panel.add(role);
        panel.add(new JLabel("Phone")); panel.add(phone);
        panel.add(new JLabel("Email")); panel.add(email);
        panel.add(new JLabel("Trạng thái")); panel.add(status);

        int result = JOptionPane.showConfirmDialog(this, panel, user == null ? "Thêm tài khoản" : "Sửa tài khoản", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        if (username.getText().trim().isEmpty() || password.getPassword().length == 0) {
            JOptionPane.showMessageDialog(this, "Username và password không được để trống", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        User u = user == null ? new User() : user;
        u.setUsername(username.getText().trim());
        u.setPassword(new String(password.getPassword()));
        u.setFullName(fullname.getText().trim());
        u.setRole(role.getSelectedItem().toString());
        u.setPhone(phone.getText().trim());
        u.setEmail(email.getText().trim());
        u.setStatus(status.getSelectedItem().toString());

        boolean ok = user == null ? userService.createUser(u) : userService.updateUser(u);
        if (ok) {
            refreshUsers();
        } else {
            JOptionPane.showMessageDialog(this, "Thao tác thất bại", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteUser(int userId) {
        int r = JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn xóa tài khoản này?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (r != JOptionPane.YES_OPTION) return;
        if (userService.deleteUser(userId)) refreshUsers(); else JOptionPane.showMessageDialog(this, "Không thể xóa tài khoản", "Lỗi", JOptionPane.ERROR_MESSAGE);
    }
}
