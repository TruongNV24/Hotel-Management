package com.cnj42.hotel.ui;

import com.cnj42.hotel.model.User;
import com.cnj42.hotel.model.UserRole;
import com.cnj42.hotel.service.PermissionService;
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

    private static final Color BACKGROUND = new Color(244, 248, 248);
    private static final Color PRIMARY = new Color(24, 119, 135);
    private static final Color TEXT_DARK = new Color(30, 53, 61);
    private static final Color TEXT_MUTED = new Color(105, 127, 132);
    private static final Color BORDER = new Color(225, 233, 235);
    private static final Color GREEN = new Color(45, 160, 108);
    private static final Color RED = new Color(214, 82, 88);

    private final UserService userService = new UserService();
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JComboBox<String> statusFilter = new JComboBox<>(new String[]{"Tất cả", "ACTIVE", "INACTIVE"});
    private final JTextField searchField = new JTextField();
    private final User currentUser;
    private final JLabel totalCount = new JLabel("0");
    private final JLabel activeCount = new JLabel("0");
    private final JLabel inactiveCount = new JLabel("0");

    public AccountManagerPanel() {
        this(null);
    }

    public AccountManagerPanel(User currentUser) {
        this.currentUser = currentUser;
        setLayout(new BorderLayout(0, 16));
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(8, 14, 18, 14));

        // Header + controls (search + filter + add)
        JPanel top = new JPanel(new BorderLayout(0, 14));
        top.setOpaque(false);

        top.add(buildStatsPanel(), BorderLayout.NORTH);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        controls.setBackground(Color.WHITE);
        controls.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1, true),
            new EmptyBorder(10, 12, 10, 12)
        ));

        searchField.setPreferredSize(new Dimension(260, 36));
        searchField.putClientProperty("JTextField.placeholderText", "Tìm theo username hoặc họ tên");
        searchField.addActionListener(e -> refreshUsers(searchField.getText().trim(), statusFilter.getSelectedItem().toString()));
        controls.add(new JLabel("Tìm kiếm"));
        controls.add(searchField);

        statusFilter.setPreferredSize(new Dimension(140, 36));
        statusFilter.addActionListener(e -> refreshUsers(searchField.getText().trim(), statusFilter.getSelectedItem().toString()));
        controls.add(new JLabel("Trạng thái"));
        controls.add(statusFilter);

        JButton addBtn = new JButton("Thêm tài khoản");
        addBtn.setEnabled(PermissionService.canManageUsers(currentUser));
        addBtn.addActionListener(e -> {
            if (!PermissionService.canManageUsers(currentUser)) {
                JOptionPane.showMessageDialog(this, "Bạn không có quyền tạo Manager.", "Không đủ quyền", JOptionPane.WARNING_MESSAGE);
                return;
            }
            openUserDialog(null);
        });
        styleButton(addBtn, PRIMARY, Color.WHITE);
        controls.add(addBtn);

        top.add(controls, BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new Object[]{"ID", "Username", "Họ và tên", "Quyền", "Phone", "Email", "Trạng thái", "Hành động"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return column == 7; }
        };

        table = new JTable(tableModel);
        table.setRowHeight(36);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setForeground(TEXT_DARK);
        table.setBackground(Color.WHITE);
        table.setSelectionBackground(new Color(225, 243, 245));
        table.setSelectionForeground(TEXT_DARK);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(239, 243, 244));
        table.setAutoCreateRowSorter(true);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        table.getTableHeader().setForeground(TEXT_MUTED);
        table.getTableHeader().setBackground(new Color(247, 250, 250));
        table.getTableHeader().setPreferredSize(new Dimension(0, 38));
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
        int active = 0;
        int inactive = 0;
        for (User u : users) {
            if ("ACTIVE".equalsIgnoreCase(u.getStatus())) active++; else inactive++;
            if (keyword != null && !keyword.isBlank()) {
                String k = keyword.toLowerCase();
                if (!(String.valueOf(u.getUsername()).toLowerCase().contains(k) || String.valueOf(u.getFullName()).toLowerCase().contains(k))) continue;
            }
            if (status != null && !"Tất cả".equals(status) && !status.equals(u.getStatus())) continue;
            tableModel.addRow(new Object[]{u.getUserId(), u.getUsername(), u.getFullName(), u.getRole(), u.getPhone(), u.getEmail(), u.getStatus(), u});
        }
        totalCount.setText(String.valueOf(users.size()));
        activeCount.setText(String.valueOf(active));
        inactiveCount.setText(String.valueOf(inactive));
    }

    private JPanel buildStatsPanel() {
        JPanel stats = new JPanel(new GridLayout(1, 3, 12, 0));
        stats.setOpaque(false);
        stats.add(metricCard("TỔNG TÀI KHOẢN", totalCount, "Tất cả người dùng", PRIMARY));
        stats.add(metricCard("ĐANG HOẠT ĐỘNG", activeCount, "Có thể đăng nhập", GREEN));
        stats.add(metricCard("NGỪNG HOẠT ĐỘNG", inactiveCount, "Đang bị khóa", RED));
        return stats;
    }

    private JPanel metricCard(String title, JLabel value, String helper, Color accent) {
        JPanel card = new JPanel(new BorderLayout(12, 0));
        card.setBackground(Color.WHITE);
        card.setBorder(new EmptyBorder(14, 16, 14, 16));
        JPanel marker = new JPanel();
        marker.setBackground(accent);
        marker.setPreferredSize(new Dimension(5, 46));
        card.add(marker, BorderLayout.WEST);
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        titleLabel.setForeground(TEXT_MUTED);
        value.setFont(new Font("Segoe UI", Font.BOLD, 26));
        value.setForeground(accent);
        JLabel helperLabel = new JLabel(helper);
        helperLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        helperLabel.setForeground(TEXT_MUTED);
        text.add(titleLabel); text.add(Box.createVerticalStrut(2)); text.add(value); text.add(helperLabel);
        card.add(text, BorderLayout.CENTER);
        return card;
    }

    private void styleButton(JButton button, Color background, Color foreground) {
        button.setBackground(background);
        button.setForeground(foreground);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(8, 13, 8, 13));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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
                if (!PermissionService.canManageUsers(currentUser)) {
                    JOptionPane.showMessageDialog(AccountManagerPanel.this, "Bạn không có quyền quản lý tài khoản.", "Không đủ quyền", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                openUserDialog(loadUserFromRow(editingRow));
            });
            delBtn.addActionListener(e -> {
                stopCellEditing();
                if (!PermissionService.canManageUsers(currentUser)) {
                    JOptionPane.showMessageDialog(AccountManagerPanel.this, "Bạn không có quyền xóa tài khoản.", "Không đủ quyền", JOptionPane.WARNING_MESSAGE);
                    return;
                }
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
        JComboBox<String> role = new JComboBox<>(new String[]{"ADMIN", "MANAGER", "EMPLOYEE"});
        JTextField phone = new JTextField();
        JTextField email = new JTextField();
        JComboBox<String> status = new JComboBox<>(new String[]{"ACTIVE", "INACTIVE"});

        if (user != null) {
            username.setText(user.getUsername());
            password.setText("");
            fullname.setText(user.getFullName());
            role.setSelectedItem(UserRole.normalize(user.getRole()));
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
        if (password.getPassword() != null && password.getPassword().length > 0) {
            u.setPassword(new String(password.getPassword()));
        }
        u.setFullName(fullname.getText().trim());
        u.setRole(role.getSelectedItem().toString());
        u.setPhone(phone.getText().trim());
        u.setEmail(email.getText().trim());
        u.setStatus(status.getSelectedItem().toString());

        if (user == null && !PermissionService.canCreateManager(currentUser)) {
            JOptionPane.showMessageDialog(this, "Chỉ Admin mới có quyền tạo Manager.", "Không đủ quyền", JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean ok = user == null ? userService.createUser(u, currentUser) : userService.updateUser(u, currentUser);
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
