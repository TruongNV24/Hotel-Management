package com.cnj42.hotel.ui;

import com.cnj42.hotel.service.AuditLogService;
import com.cnj42.hotel.model.User;
import com.cnj42.hotel.utils.DBConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class GuestManagementPanel extends JPanel {

    private static final Color BACKGROUND = new Color(246, 249, 249);
    private static final Color PRIMARY = new Color(24, 119, 135);
    private static final Color TEXT_DARK = new Color(30, 53, 61);
    private static final Color TEXT_MUTED = new Color(105, 127, 132);
    private static final Color BORDER = new Color(225, 233, 235);

    private final User currentUser;
    private final AuditLogService auditLogService = new AuditLogService();
    private final JTextField searchField = new JTextField();
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JLabel totalLabel = new JLabel("0");

    public GuestManagementPanel(User currentUser) {
        this.currentUser = currentUser;
        setLayout(new BorderLayout(0, 16));
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(8, 14, 18, 14));

        JPanel top = new JPanel(new BorderLayout(0, 14));
        top.setOpaque(false);
        top.add(buildSummary(), BorderLayout.NORTH);
        top.add(buildToolbar(), BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
                new Object[]{"ID", "HỌ TÊN", "SỐ ĐIỆN THOẠI", "EMAIL", "CCCD / HỘ CHIẾU", "QUỐC TỊCH", "NGÀY TẠO"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setRowHeight(38);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setForeground(TEXT_DARK);
        table.setBackground(Color.WHITE);
        table.setSelectionBackground(new Color(225, 243, 245));
        table.setSelectionForeground(TEXT_DARK);
        table.setGridColor(new Color(239, 243, 244));
        table.setShowVerticalLines(false);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        table.getTableHeader().setForeground(TEXT_MUTED);
        table.getTableHeader().setBackground(new Color(247, 250, 250));
        table.getTableHeader().setPreferredSize(new Dimension(0, 38));
        table.setAutoCreateRowSorter(true);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER, 1, true));
        scrollPane.getViewport().setBackground(Color.WHITE);
        add(scrollPane, BorderLayout.CENTER);

        loadGuests();
    }

    private JPanel buildSummary() {
        JPanel summary = new JPanel(new BorderLayout());
        summary.setBackground(Color.WHITE);
        summary.setBorder(new EmptyBorder(14, 18, 14, 18));

        JPanel marker = new JPanel();
        marker.setBackground(PRIMARY);
        marker.setPreferredSize(new Dimension(5, 48));
        summary.add(marker, BorderLayout.WEST);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(0, 14, 0, 0));
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("KHÁCH HÀNG");
        title.setFont(new Font("Segoe UI", Font.BOLD, 10));
        title.setForeground(TEXT_MUTED);
        totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        totalLabel.setForeground(PRIMARY);
        JLabel helper = new JLabel("Danh sách khách hàng trong hệ thống");
        helper.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        helper.setForeground(TEXT_MUTED);

        content.add(title);
        content.add(Box.createVerticalStrut(2));
        content.add(totalLabel);
        content.add(helper);
        summary.add(content, BorderLayout.CENTER);
        return summary;
    }

    private JPanel buildToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout(10, 0));
        toolbar.setBackground(Color.WHITE);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                new EmptyBorder(10, 12, 10, 12)
        ));

        searchField.setPreferredSize(new Dimension(320, 36));
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                new EmptyBorder(0, 10, 0, 10)
        ));
        searchField.putClientProperty("JTextField.placeholderText", "Tìm theo tên, số điện thoại, CCCD...");
        searchField.addActionListener(event -> loadGuests());
        toolbar.add(searchField, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton searchButton = createButton("Tìm kiếm", PRIMARY, Color.WHITE);
        JButton resetButton = createButton("Đặt lại", new Color(239, 246, 247), PRIMARY);
        JButton addButton = createButton("+ Thêm khách", PRIMARY, Color.WHITE);
        JButton editButton = createButton("Sửa", new Color(239, 246, 247), PRIMARY);
        JButton deleteButton = createButton("Xóa", new Color(255, 235, 236), new Color(214, 82, 88));

        searchButton.addActionListener(event -> loadGuests());
        resetButton.addActionListener(event -> { searchField.setText(""); loadGuests(); });
        addButton.addActionListener(event -> openGuestEditor(null));
        editButton.addActionListener(event -> editSelected());
        deleteButton.addActionListener(event -> deleteSelected());

        actions.add(searchButton);
        actions.add(resetButton);
        actions.add(addButton);
        actions.add(editButton);
        actions.add(deleteButton);
        toolbar.add(actions, BorderLayout.EAST);
        return toolbar;
    }

    private JButton createButton(String text, Color background, Color foreground) {
        JButton button = new JButton(text);
        button.setBackground(background);
        button.setForeground(foreground);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setFont(new Font("Segoe UI", Font.BOLD, 11));
        button.setBorder(new EmptyBorder(8, 13, 8, 13));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private void loadGuests() {
        tableModel.setRowCount(0);
        String keyword = searchField.getText().trim();
        String sql = "SELECT guest_id, full_name, phone, email, id_card, nationality, created_at "
                + "FROM guests WHERE full_name LIKE ? OR phone LIKE ? OR id_card LIKE ? "
                + "ORDER BY created_at DESC";
        String query = "%" + keyword + "%";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, query);
            statement.setString(2, query);
            statement.setString(3, query);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    tableModel.addRow(new Object[]{
                            resultSet.getInt("guest_id"),
                            resultSet.getString("full_name"),
                            resultSet.getString("phone"),
                            valueOrDash(resultSet.getString("email")),
                            resultSet.getString("id_card"),
                            valueOrDash(resultSet.getString("nationality")),
                            resultSet.getTimestamp("created_at")
                    });
                }
            }
            totalLabel.setText(String.valueOf(tableModel.getRowCount()));
        } catch (SQLException exception) {
            showError("Không thể tải danh sách khách hàng: " + exception.getMessage());
        }
    }

    private void openGuestEditor(Integer guestId) {
        JTextField nameField = new JTextField();
        JTextField phoneField = new JTextField();
        JTextField idCardField = new JTextField();
        JTextField emailField = new JTextField();
        JTextField addressField = new JTextField();
        JTextField nationalityField = new JTextField("Vietnam");

        if (guestId != null) {
            String sql = "SELECT full_name, phone, id_card, email, address, nationality FROM guests WHERE guest_id = ?";
            try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, guestId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        nameField.setText(resultSet.getString("full_name"));
                        phoneField.setText(resultSet.getString("phone"));
                        idCardField.setText(resultSet.getString("id_card"));
                        emailField.setText(valueOrEmpty(resultSet.getString("email")));
                        addressField.setText(valueOrEmpty(resultSet.getString("address")));
                        nationalityField.setText(valueOrEmpty(resultSet.getString("nationality")));
                    }
                }
            } catch (SQLException exception) {
                showError("Không thể tải thông tin khách hàng: " + exception.getMessage());
                return;
            }
        }

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 5, 5, 5);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        addField(form, constraints, 0, "Họ tên *", nameField);
        addField(form, constraints, 1, "Số điện thoại *", phoneField);
        addField(form, constraints, 2, "CCCD / Hộ chiếu *", idCardField);
        addField(form, constraints, 3, "Email", emailField);
        addField(form, constraints, 4, "Địa chỉ", addressField);
        addField(form, constraints, 5, "Quốc tịch", nationalityField);

        int result = JOptionPane.showConfirmDialog(this, form,
                guestId == null ? "Thêm khách hàng" : "Cập nhật khách hàng",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;
        if (nameField.getText().trim().isEmpty() || phoneField.getText().trim().isEmpty() || idCardField.getText().trim().isEmpty()) {
            showError("Vui lòng nhập họ tên, số điện thoại và CCCD / hộ chiếu.");
            return;
        }

        boolean saved = guestId == null
                ? insertGuest(nameField, phoneField, idCardField, emailField, addressField, nationalityField)
                : updateGuest(guestId, nameField, phoneField, idCardField, emailField, addressField, nationalityField);
        if (saved) loadGuests();
    }

    private boolean insertGuest(JTextField name, JTextField phone, JTextField idCard, JTextField email, JTextField address, JTextField nationality) {
        String sql = "INSERT INTO guests (full_name, phone, id_card, email, address, nationality) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            fillGuestStatement(statement, name, phone, idCard, email, address, nationality);
            statement.executeUpdate();
            int id = -1;
            try (ResultSet keys = statement.getGeneratedKeys()) { if (keys.next()) id = keys.getInt(1); }
            auditLogService.logEvent("GUEST_CREATED", "GUEST", "GUEST", id > 0 ? id : null, currentUser,
                    "Created guest " + name.getText().trim(), "127.0.0.1", "SUCCESS");
            return true;
        } catch (SQLException exception) {
            auditLogService.logEvent("GUEST_CREATE_FAILED", "GUEST", "GUEST", null, currentUser,
                    "Failed to create guest: " + exception.getMessage(), "127.0.0.1", "FAILED");
            showError("Không thể thêm khách hàng: " + exception.getMessage());
            return false;
        }
    }

    private boolean updateGuest(int guestId, JTextField name, JTextField phone, JTextField idCard, JTextField email, JTextField address, JTextField nationality) {
        String sql = "UPDATE guests SET full_name = ?, phone = ?, id_card = ?, email = ?, address = ?, nationality = ? WHERE guest_id = ?";
        try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            fillGuestStatement(statement, name, phone, idCard, email, address, nationality);
            statement.setInt(7, guestId);
            statement.executeUpdate();
            auditLogService.logEvent("GUEST_UPDATED", "GUEST", "GUEST", guestId, currentUser,
                    "Updated guest " + name.getText().trim(), "127.0.0.1", "SUCCESS");
            return true;
        } catch (SQLException exception) {
            auditLogService.logEvent("GUEST_UPDATE_FAILED", "GUEST", "GUEST", guestId, currentUser,
                    "Failed to update guest: " + exception.getMessage(), "127.0.0.1", "FAILED");
            showError("Không thể cập nhật khách hàng: " + exception.getMessage());
            return false;
        }
    }

    private void fillGuestStatement(PreparedStatement statement, JTextField name, JTextField phone, JTextField idCard, JTextField email, JTextField address, JTextField nationality) throws SQLException {
        statement.setString(1, name.getText().trim());
        statement.setString(2, phone.getText().trim());
        statement.setString(3, idCard.getText().trim());
        statement.setString(4, emptyToNull(email.getText()));
        statement.setString(5, emptyToNull(address.getText()));
        statement.setString(6, emptyToNull(nationality.getText()));
    }

    private void editSelected() {
        Integer id = selectedGuestId();
        if (id != null) openGuestEditor(id);
    }

    private void deleteSelected() {
        Integer id = selectedGuestId();
        if (id == null) return;
        int result = JOptionPane.showConfirmDialog(this, "Xóa khách hàng này?", "Xác nhận", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (result != JOptionPane.YES_OPTION) return;
        try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement("DELETE FROM guests WHERE guest_id = ?")) {
            statement.setInt(1, id);
            statement.executeUpdate();
            auditLogService.logEvent("GUEST_DELETED", "GUEST", "GUEST", id, currentUser,
                    "Deleted guest " + id, "127.0.0.1", "SUCCESS");
            loadGuests();
        } catch (SQLException exception) {
            showError("Không thể xóa khách hàng. Có thể khách đã có đặt phòng hoặc lưu trú.");
        }
    }

    private Integer selectedGuestId() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return null;
        return (Integer) tableModel.getValueAt(table.convertRowIndexToModel(viewRow), 0);
    }

    private void addField(JPanel panel, GridBagConstraints constraints, int row, String label, JComponent field) {
        constraints.gridy = row;
        constraints.gridx = 0;
        constraints.weightx = 0;
        panel.add(new JLabel(label), constraints);
        constraints.gridx = 1;
        constraints.weightx = 1;
        field.setPreferredSize(new Dimension(260, 30));
        panel.add(field, constraints);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    private String valueOrDash(String value) { return value == null || value.isBlank() ? "-" : value; }
    private String valueOrEmpty(String value) { return value == null ? "" : value; }
    private String emptyToNull(String value) { return value == null || value.trim().isEmpty() ? null : value.trim(); }
}
