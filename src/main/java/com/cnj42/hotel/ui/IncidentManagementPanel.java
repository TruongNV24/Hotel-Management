package com.cnj42.hotel.ui;

import com.cnj42.hotel.model.Incident;
import com.cnj42.hotel.model.User;
import com.cnj42.hotel.service.IncidentService;
import com.cnj42.hotel.service.PermissionService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class IncidentManagementPanel extends JPanel {
    private static final Color BACKGROUND = new Color(246, 247, 251);
    private static final Color PRIMARY = new Color(105, 78, 210);
    private final User currentUser;
    private final IncidentService incidentService = new IncidentService();
    private final JTextField searchField = new JTextField(18);
    private final JComboBox<String> typeFilter = new JComboBox<>(new String[]{"ALL", Incident.PAYMENT, Incident.NO_CHECKOUT, Incident.PROPERTY_DAMAGE, Incident.GUEST_COMPLAINT, Incident.BILLING_DISPUTE, Incident.SERVICE_ISSUE, Incident.OTHER});
    private final JComboBox<String> priorityFilter = new JComboBox<>(new String[]{"ALL", Incident.LOW, Incident.MEDIUM, Incident.HIGH, Incident.CRITICAL});
    private final JComboBox<String> statusFilter = new JComboBox<>(new String[]{"ALL", Incident.OPEN, Incident.IN_PROGRESS, Incident.RESOLVED, Incident.CLOSED, Incident.CANCELLED});
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public IncidentManagementPanel(User currentUser) {
        this.currentUser = currentUser;
        setLayout(new BorderLayout(10, 10));
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(18, 18, 18, 18));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.setOpaque(false);
        toolbar.add(new JLabel("Loại:")); toolbar.add(typeFilter);
        toolbar.add(new JLabel("Ưu tiên:")); toolbar.add(priorityFilter);
        toolbar.add(new JLabel("Trạng thái:")); toolbar.add(statusFilter);
        toolbar.add(new JLabel("Tìm:")); toolbar.add(searchField);
        JButton searchButton = new JButton("Tìm kiếm");
        JButton resetButton = new JButton("Đặt lại");
        JButton addButton = new JButton("Tạo incident");
        JButton editButton = new JButton("Cập nhật");
        JButton assignButton = new JButton("Phân công");
        JButton startButton = new JButton("Bắt đầu xử lý");
        JButton resolveButton = new JButton("Resolved");
        JButton closeButton = new JButton("Đóng");
        JButton cancelButton = new JButton("Hủy incident");
        toolbar.add(searchButton); toolbar.add(resetButton); toolbar.add(addButton);
        toolbar.add(editButton); toolbar.add(assignButton); toolbar.add(startButton);
        toolbar.add(resolveButton); toolbar.add(closeButton); toolbar.add(cancelButton);
        add(toolbar, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new Object[]{"ID", "Loại", "Tiêu đề", "Ưu tiên", "Trạng thái", "Khách", "Người xử lý", "Thời gian"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(tableModel);
        table.setRowHeight(30);
        table.setAutoCreateRowSorter(true);
        add(new JScrollPane(table), BorderLayout.CENTER);

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
        for (Incident incident : incidents) {
            tableModel.addRow(new Object[]{incident.getIncidentId(), incident.getIncidentType(), incident.getTitle(), incident.getPriority(), incident.getStatus(),
                    incident.getGuestName() == null ? "-" : incident.getGuestName(), incident.getAssignedToName() == null ? "-" : incident.getAssignedToName(),
                    incident.getReportedAt() == null ? "-" : incident.getReportedAt().format(dateFormatter)});
        }
    }

    private void updateButtonState(JButton edit, JButton assign, JButton start, JButton resolve, JButton close, JButton cancel) {
        Incident incident = selectedIncident();
        boolean exists = incident != null;
        boolean manager = PermissionService.canManageIncidents(currentUser);
        edit.setEnabled(exists);
        assign.setEnabled(exists && manager);
        start.setEnabled(exists && manager && Incident.OPEN.equals(incident.getStatus()));
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
        JTextField titleField = new JTextField(existing == null ? "" : existing.getTitle());
        JTextArea descriptionArea = new JTextArea(existing == null ? "" : nullToEmpty(existing.getDescription()), 4, 28);
        JTextArea resolutionArea = new JTextArea(existing == null ? "" : nullToEmpty(existing.getResolution()), 4, 28);
        JTextField guestField = new JTextField(idText(existing == null ? null : existing.getGuestId()));
        JTextField reservationField = new JTextField(idText(existing == null ? null : existing.getReservationId()));
        JTextField stayField = new JTextField(idText(existing == null ? null : existing.getStayId()));
        JTextField invoiceField = new JTextField(idText(existing == null ? null : existing.getInvoiceId()));
        if (existing != null) { typeBox.setSelectedItem(existing.getIncidentType()); priorityBox.setSelectedItem(existing.getPriority()); statusBox.setSelectedItem(existing.getStatus()); }
        if (existing != null && !PermissionService.canManageIncidents(currentUser)) statusBox.setEnabled(false);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints(); c.insets = new Insets(4, 4, 4, 4); c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1;
        addField(form, c, 0, "Loại:", typeBox); addField(form, c, 1, "Ưu tiên:", priorityBox); addField(form, c, 2, "Trạng thái:", statusBox); addField(form, c, 3, "Tiêu đề:", titleField);
        addField(form, c, 4, "Guest ID:", guestField); addField(form, c, 5, "Reservation ID:", reservationField); addField(form, c, 6, "Stay ID:", stayField); addField(form, c, 7, "Invoice ID:", invoiceField);
        addArea(form, c, 8, "Mô tả:", descriptionArea); addArea(form, c, 9, "Hướng xử lý:", resolutionArea);
        int result = JOptionPane.showConfirmDialog(this, new JScrollPane(form), existing == null ? "Tạo incident" : "Cập nhật incident", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        Incident incident = existing == null ? new Incident() : existing;
        incident.setIncidentType((String) typeBox.getSelectedItem()); incident.setPriority((String) priorityBox.getSelectedItem()); incident.setStatus((String) statusBox.getSelectedItem());
        incident.setTitle(titleField.getText()); incident.setDescription(descriptionArea.getText()); incident.setResolution(resolutionArea.getText());
        incident.setGuestId(parseId(guestField.getText())); incident.setReservationId(parseId(reservationField.getText())); incident.setStayId(parseId(stayField.getText())); incident.setInvoiceId(parseId(invoiceField.getText()));
        boolean success = existing == null ? incidentService.create(incident, currentUser) > 0 : incidentService.update(incident, currentUser);
        if (!success) JOptionPane.showMessageDialog(this, "Không thể lưu incident. Kiểm tra dữ liệu, quyền hoặc trạng thái.", "Lỗi", JOptionPane.ERROR_MESSAGE);
        loadIncidents();
    }

    private void assignSelected() {
        Incident incident = selectedIncident(); if (incident == null) return;
        String value = JOptionPane.showInputDialog(this, "Nhập User ID xử lý (để trống để bỏ phân công):", idText(incident.getAssignedTo()));
        if (value == null) return;
        Integer id = parseId(value);
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

    private void addField(JPanel panel, GridBagConstraints c, int row, String label, JComponent field) { c.gridy = row; c.gridx = 0; c.weightx = 0; panel.add(new JLabel(label), c); c.gridx = 1; c.weightx = 1; panel.add(field, c); }
    private void addArea(JPanel panel, GridBagConstraints c, int row, String label, JTextArea area) { area.setLineWrap(true); area.setWrapStyleWord(true); addField(panel, c, row, label, new JScrollPane(area)); }
    private String selected(JComboBox<String> combo) { return (String) combo.getSelectedItem(); }
    private String idText(Integer id) { return id == null ? "" : String.valueOf(id); }
    private Integer parseId(String value) { try { return value == null || value.isBlank() ? null : Integer.valueOf(value.trim()); } catch (NumberFormatException e) { return -1; } }
    private String nullToEmpty(String value) { return value == null ? "" : value; }
}
