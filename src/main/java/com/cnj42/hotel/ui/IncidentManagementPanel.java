package com.cnj42.hotel.ui;

import com.cnj42.hotel.model.Incident;
import com.cnj42.hotel.model.User;
import com.cnj42.hotel.service.IncidentService;
import com.cnj42.hotel.service.PermissionService;
import com.cnj42.hotel.utils.DBConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class IncidentManagementPanel extends JPanel {
    private static final Color BACKGROUND = new Color(246, 247, 251);
    private static final Color PRIMARY = new Color(24, 119, 135);
    private static final Color TEXT_DARK = new Color(30, 53, 61);
    private static final Color TEXT_MUTED = new Color(105, 127, 132);
    private static final Color BORDER = new Color(225, 233, 235);
    private static final Color RED = new Color(214, 82, 88);
    private static final Color ORANGE = new Color(224, 142, 57);
    private static final Color GREEN = new Color(45, 160, 108);
    private final User currentUser;
    private final IncidentService incidentService = new IncidentService();
    private final JTextField searchField = new JTextField(18);
    private final JComboBox<String> typeFilter = new JComboBox<>(new String[]{"ALL", Incident.PAYMENT, Incident.NO_CHECKOUT, Incident.PROPERTY_DAMAGE, Incident.GUEST_COMPLAINT, Incident.BILLING_DISPUTE, Incident.SERVICE_ISSUE, Incident.OTHER});
    private final JComboBox<String> priorityFilter = new JComboBox<>(new String[]{"ALL", Incident.LOW, Incident.MEDIUM, Incident.HIGH, Incident.CRITICAL});
    private final JComboBox<String> statusFilter = new JComboBox<>(new String[]{"ALL", Incident.OPEN, Incident.IN_PROGRESS, Incident.RESOLVED, Incident.CLOSED, Incident.CANCELLED});
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final JLabel totalCount = new JLabel("0");
    private final JLabel openCount = new JLabel("0");
    private final JLabel progressCount = new JLabel("0");
    private final JLabel resolvedCount = new JLabel("0");

    public IncidentManagementPanel(User currentUser) {
        this.currentUser = currentUser;
        setLayout(new BorderLayout(0, 16));
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(8, 14, 18, 14));

        JPanel top = new JPanel(new BorderLayout(0, 14));
        top.setOpaque(false);
        top.add(buildStatsPanel(), BorderLayout.NORTH);

        JPanel toolbar = new JPanel();
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.Y_AXIS));
        toolbar.setBackground(Color.WHITE);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1, true),
            new EmptyBorder(10, 12, 10, 12)
        ));

        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filterRow.setOpaque(false);
        addFilterLabel(filterRow, "Loại sự cố"); filterRow.add(typeFilter);
        addFilterLabel(filterRow, "Ưu tiên"); filterRow.add(priorityFilter);
        addFilterLabel(filterRow, "Trạng thái"); filterRow.add(statusFilter);
        addFilterLabel(filterRow, "Tìm kiếm"); filterRow.add(searchField);
        JButton searchButton = new JButton("Tìm kiếm");
        JButton resetButton = new JButton("Đặt lại");
        styleButton(searchButton, PRIMARY, Color.WHITE);
        styleButton(resetButton, new Color(239, 246, 247), PRIMARY);
        filterRow.add(searchButton); filterRow.add(resetButton);

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        actionRow.setOpaque(false);
        JButton addButton = new JButton("Tạo incident");
        JButton editButton = new JButton("Cập nhật");
        JButton assignButton = new JButton("Phân công");
        JButton startButton = new JButton("Bắt đầu xử lý");
        JButton resolveButton = new JButton("Đã xử lý");
        JButton closeButton = new JButton("Đóng");
        JButton cancelButton = new JButton("Hủy incident");
        styleButton(addButton, PRIMARY, Color.WHITE);
        styleButton(editButton, new Color(239, 246, 247), PRIMARY);
        styleButton(assignButton, new Color(239, 246, 247), PRIMARY);
        styleButton(startButton, new Color(232, 246, 239), GREEN);
        styleButton(resolveButton, new Color(232, 246, 239), GREEN);
        styleButton(closeButton, new Color(255, 244, 229), ORANGE);
        styleButton(cancelButton, new Color(255, 235, 236), RED);
        actionRow.add(addButton); actionRow.add(editButton); actionRow.add(assignButton);
        actionRow.add(startButton); actionRow.add(resolveButton); actionRow.add(closeButton); actionRow.add(cancelButton);

        toolbar.add(filterRow);
        toolbar.add(actionRow);
        top.add(toolbar, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new Object[]{"ID", "Loại", "Tiêu đề", "Ưu tiên", "Trạng thái", "Khách", "Người xử lý", "Thời gian"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
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
        table.setShowHorizontalLines(true);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        table.getTableHeader().setForeground(TEXT_MUTED);
        table.getTableHeader().setBackground(new Color(247, 250, 250));
        table.getTableHeader().setPreferredSize(new Dimension(0, 38));
        table.setAutoCreateRowSorter(true);
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createLineBorder(BORDER, 1, true));
        tableScroll.getViewport().setBackground(Color.WHITE);
        add(tableScroll, BorderLayout.CENTER);

        searchButton.addActionListener(e -> loadIncidents());
        resetButton.addActionListener(e -> { searchField.setText(""); typeFilter.setSelectedItem("ALL"); priorityFilter.setSelectedItem("ALL"); statusFilter.setSelectedItem("ALL"); loadIncidents(); });
        addButton.addActionListener(e -> openEditor(null));
        editButton.addActionListener(e -> editSelected());
        assignButton.addActionListener(e -> assignSelected());
        startButton.addActionListener(e -> transitionSelected(Incident.IN_PROGRESS, null));
        resolveButton.addActionListener(e -> resolveSelected());
        closeButton.addActionListener(e -> transitionSelected(Incident.CLOSED, null));
        cancelButton.addActionListener(e -> transitionSelected(Incident.CANCELLED, null));
        searchField.addActionListener(e -> loadIncidents());
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) updateButtonState(editButton, assignButton, startButton, resolveButton, closeButton, cancelButton);
        });
        loadIncidents();
    }

    private void loadIncidents() {
        tableModel.setRowCount(0);
        List<Incident> incidents = incidentService.search(searchField.getText(), selected(typeFilter), selected(priorityFilter), selected(statusFilter));
        int open = 0;
        int inProgress = 0;
        int resolved = 0;
        for (Incident incident : incidents) {
            if (Incident.OPEN.equals(incident.getStatus())) open++;
            if (Incident.IN_PROGRESS.equals(incident.getStatus())) inProgress++;
            if (Incident.RESOLVED.equals(incident.getStatus()) || Incident.CLOSED.equals(incident.getStatus())) resolved++;
            tableModel.addRow(new Object[]{incident.getIncidentId(), incident.getIncidentType(), incident.getTitle(), incident.getPriority(), incident.getStatus(),
                    incident.getGuestName() == null ? "-" : incident.getGuestName(), incident.getAssignedToName() == null ? "-" : incident.getAssignedToName(),
                    incident.getReportedAt() == null ? "-" : incident.getReportedAt().format(dateFormatter)});
        }
        totalCount.setText(String.valueOf(incidents.size()));
        openCount.setText(String.valueOf(open));
        progressCount.setText(String.valueOf(inProgress));
        resolvedCount.setText(String.valueOf(resolved));
    }

    private JPanel buildStatsPanel() {
        JPanel stats = new JPanel(new GridLayout(1, 4, 12, 0));
        stats.setOpaque(false);
        stats.add(createMetricCard("TỔNG SỰ CỐ", totalCount, "Theo bộ lọc hiện tại", PRIMARY));
        stats.add(createMetricCard("ĐANG MỞ", openCount, "Cần tiếp nhận", RED));
        stats.add(createMetricCard("ĐANG XỬ LÝ", progressCount, "Đã phân công", ORANGE));
        stats.add(createMetricCard("ĐÃ XỬ LÝ", resolvedCount, "Đã giải quyết / đóng", GREEN));
        return stats;
    }

    private JPanel createMetricCard(String title, JLabel value, String helper, Color accent) {
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
        text.add(titleLabel);
        text.add(Box.createVerticalStrut(2));
        text.add(value);
        text.add(helperLabel);
        card.add(text, BorderLayout.CENTER);
        return card;
    }

    private void addFilterLabel(JPanel parent, String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 11));
        label.setForeground(TEXT_MUTED);
        parent.add(label);
    }

    private void styleButton(JButton button, Color background, Color foreground) {
        button.setBackground(background);
        button.setForeground(foreground);
        button.setFont(new Font("Segoe UI", Font.BOLD, 11));
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(8, 13, 8, 13));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void updateButtonState(JButton edit, JButton assign, JButton start, JButton resolve, JButton close, JButton cancel) {
        Incident incident = selectedIncident();
        boolean exists = incident != null;
        boolean manager = PermissionService.canManageIncidents(currentUser);
        edit.setEnabled(exists);
        assign.setEnabled(exists && manager);
        start.setEnabled(exists && manager && Incident.OPEN.equals(incident.getStatus()) && incident.getAssignedTo() != null);
        resolve.setEnabled(exists && manager && Incident.IN_PROGRESS.equals(incident.getStatus()));
        close.setEnabled(exists && manager && Incident.RESOLVED.equals(incident.getStatus()));
        cancel.setEnabled(exists && manager && (Incident.OPEN.equals(incident.getStatus()) || Incident.IN_PROGRESS.equals(incident.getStatus())));
    }

    private Incident selectedIncident() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return null;
        int row = table.convertRowIndexToModel(viewRow);
        return incidentService.findById((Integer) tableModel.getValueAt(row, 0));
    }

    private void editSelected() {
        Incident incident = selectedIncident();
        if (incident != null) openEditor(incident);
    }

    private void openEditor(Incident existing) {
        JComboBox<String> typeBox = new JComboBox<>(new String[]{Incident.PAYMENT, Incident.NO_CHECKOUT, Incident.PROPERTY_DAMAGE, Incident.GUEST_COMPLAINT, Incident.BILLING_DISPUTE, Incident.SERVICE_ISSUE, Incident.OTHER});
        JComboBox<String> priorityBox = new JComboBox<>(new String[]{Incident.LOW, Incident.MEDIUM, Incident.HIGH, Incident.CRITICAL});
        JComboBox<String> statusBox = new JComboBox<>(new String[]{Incident.OPEN, Incident.IN_PROGRESS, Incident.RESOLVED, Incident.CLOSED, Incident.CANCELLED});
        JComboBox<ReferenceOption> guestBox = createReferenceBox("SELECT guest_id, CONCAT(full_name, ' - ', phone) FROM guests ORDER BY full_name");
        JComboBox<ReferenceOption> reservationBox = createReferenceBox("SELECT r.reservation_id, CONCAT(r.reservation_code, ' - phòng ', rm.room_number, ' - ', g.full_name) "
            + "FROM reservations r JOIN guests g ON g.guest_id = r.guest_id JOIN rooms rm ON rm.room_id = r.room_id ORDER BY r.created_at DESC");
        JComboBox<ReferenceOption> stayBox = createReferenceBox("SELECT s.stay_id, CONCAT('Lưu trú #', s.stay_id, ' - phòng ', rm.room_number, ' - ', g.full_name) "
            + "FROM stays s JOIN reservations r ON r.reservation_id = s.reservation_id JOIN guests g ON g.guest_id = r.guest_id JOIN rooms rm ON rm.room_id = s.room_id ORDER BY s.stay_id DESC");
        JComboBox<ReferenceOption> invoiceBox = createReferenceBox("SELECT i.invoice_id, CONCAT(i.invoice_code, ' - ', i.status, ' - ', g.full_name) "
            + "FROM invoices i JOIN stays s ON s.stay_id = i.stay_id JOIN reservations r ON r.reservation_id = s.reservation_id JOIN guests g ON g.guest_id = r.guest_id ORDER BY i.issued_at DESC");
        JTextField titleField = new JTextField(existing == null ? "" : existing.getTitle());
        JTextArea descriptionArea = new JTextArea(existing == null ? "" : nullToEmpty(existing.getDescription()), 4, 28);
        JTextArea resolutionArea = new JTextArea(existing == null ? "" : nullToEmpty(existing.getResolution()), 4, 28);
        if (existing != null) { typeBox.setSelectedItem(existing.getIncidentType()); priorityBox.setSelectedItem(existing.getPriority()); statusBox.setSelectedItem(existing.getStatus()); }
        selectReference(guestBox, existing == null ? null : existing.getGuestId());
        selectReference(reservationBox, existing == null ? null : existing.getReservationId());
        selectReference(stayBox, existing == null ? null : existing.getStayId());
        selectReference(invoiceBox, existing == null ? null : existing.getInvoiceId());
        if (existing != null && !PermissionService.canManageIncidents(currentUser)) statusBox.setEnabled(false);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints(); c.insets = new Insets(4, 4, 4, 4); c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1;
        addField(form, c, 0, "Loại:", typeBox); addField(form, c, 1, "Ưu tiên:", priorityBox); addField(form, c, 2, "Trạng thái:", statusBox); addField(form, c, 3, "Tiêu đề:", titleField);
        addField(form, c, 4, "Khách hàng:", guestBox); addField(form, c, 5, "Đặt phòng:", reservationBox); addField(form, c, 6, "Lưu trú:", stayBox); addField(form, c, 7, "Hóa đơn:", invoiceBox);
        addArea(form, c, 8, "Mô tả:", descriptionArea); addArea(form, c, 9, "Hướng xử lý:", resolutionArea);
        int result = JOptionPane.showConfirmDialog(this, new JScrollPane(form), existing == null ? "Tạo incident" : "Cập nhật incident", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        Incident incident = existing == null ? new Incident() : existing;
        incident.setIncidentType((String) typeBox.getSelectedItem()); incident.setPriority((String) priorityBox.getSelectedItem()); incident.setStatus((String) statusBox.getSelectedItem());
        incident.setTitle(titleField.getText()); incident.setDescription(descriptionArea.getText()); incident.setResolution(resolutionArea.getText());
        incident.setGuestId(selectedReferenceId(guestBox)); incident.setReservationId(selectedReferenceId(reservationBox));
        incident.setStayId(selectedReferenceId(stayBox)); incident.setInvoiceId(selectedReferenceId(invoiceBox));
        boolean success = existing == null ? incidentService.create(incident, currentUser) > 0 : incidentService.update(incident, currentUser);
        if (!success) JOptionPane.showMessageDialog(this, "Không thể lưu incident. Kiểm tra dữ liệu, quyền hoặc trạng thái.", "Lỗi", JOptionPane.ERROR_MESSAGE);
        loadIncidents();
    }

    private void assignSelected() {
        Incident incident = selectedIncident(); if (incident == null) return;
        JComboBox<ReferenceOption> assigneeBox = createReferenceBox(
            "SELECT user_id, CONCAT(full_name, ' (', username, ')') FROM users "
                + "WHERE status = 'ACTIVE' ORDER BY full_name");
        selectReference(assigneeBox, incident.getAssignedTo());
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.add(new JLabel("Nhân viên xử lý:"), BorderLayout.NORTH);
        panel.add(assigneeBox, BorderLayout.CENTER);
        int result = JOptionPane.showConfirmDialog(this, panel, "Phân công sự cố",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;
        Integer id = selectedReferenceId(assigneeBox);
        if (!incidentService.assign(incident.getIncidentId(), id, currentUser)) JOptionPane.showMessageDialog(this, "Không thể phân công user này.", "Lỗi", JOptionPane.ERROR_MESSAGE);
        loadIncidents();
    }

    private void resolveSelected() {
        Incident incident = selectedIncident(); if (incident == null) return;
        String resolution = JOptionPane.showInputDialog(this, "Nhập hướng xử lý:", incident.getResolution() == null ? "" : incident.getResolution());
        if (resolution == null || resolution.isBlank()) return;
        transitionSelected(Incident.RESOLVED, resolution);
    }

    private void transitionSelected(String status, String resolution) {
        Incident incident = selectedIncident(); if (incident == null) return;
        if (!incidentService.transition(incident.getIncidentId(), status, resolution == null ? incident.getResolution() : resolution, currentUser)) JOptionPane.showMessageDialog(this, "Chuyển trạng thái không hợp lệ hoặc không đủ quyền.", "Lỗi", JOptionPane.ERROR_MESSAGE);
        loadIncidents();
    }

    private JComboBox<ReferenceOption> createReferenceBox(String sql) {
        JComboBox<ReferenceOption> box = new JComboBox<>();
        box.addItem(new ReferenceOption(null, "Không liên kết"));
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                box.addItem(new ReferenceOption(resultSet.getInt(1), resultSet.getString(2)));
            }
        } catch (SQLException exception) {
            System.err.println("Không thể tải danh sách liên kết sự cố: " + exception.getMessage());
        }
        box.setPreferredSize(new Dimension(280, 28));
        return box;
    }

    private void selectReference(JComboBox<ReferenceOption> box, Integer id) {
        for (int index = 0; index < box.getItemCount(); index++) {
            ReferenceOption option = box.getItemAt(index);
            if (id == null ? option.id == null : id.equals(option.id)) {
                box.setSelectedIndex(index);
                return;
            }
        }
    }

    private Integer selectedReferenceId(JComboBox<ReferenceOption> box) {
        ReferenceOption option = (ReferenceOption) box.getSelectedItem();
        return option == null ? null : option.id;
    }

    private void addField(JPanel panel, GridBagConstraints c, int row, String label, JComponent field) { c.gridy = row; c.gridx = 0; c.weightx = 0; panel.add(new JLabel(label), c); c.gridx = 1; c.weightx = 1; panel.add(field, c); }
    private void addArea(JPanel panel, GridBagConstraints c, int row, String label, JTextArea area) { area.setLineWrap(true); area.setWrapStyleWord(true); addField(panel, c, row, label, new JScrollPane(area)); }
    private String selected(JComboBox<String> combo) { return (String) combo.getSelectedItem(); }
    private String idText(Integer id) { return id == null ? "" : String.valueOf(id); }
    private Integer parseId(String value) { try { return value == null || value.isBlank() ? null : Integer.valueOf(value.trim()); } catch (NumberFormatException e) { return -1; } }
    private String nullToEmpty(String value) { return value == null ? "" : value; }

    private static class ReferenceOption {
        private final Integer id;
        private final String label;

        private ReferenceOption(Integer id, String label) {
            this.id = id;
            this.label = label == null ? "" : label;
        }

        @Override
        public String toString() {
            return id == null ? label : id + " - " + label;
        }
    }
}
