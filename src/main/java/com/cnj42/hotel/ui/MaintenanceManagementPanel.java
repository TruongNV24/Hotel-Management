package com.cnj42.hotel.ui;

import com.cnj42.hotel.model.Maintenance;
import com.cnj42.hotel.model.User;
import com.cnj42.hotel.service.MaintenanceService;
import com.cnj42.hotel.service.PermissionService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class MaintenanceManagementPanel extends JPanel {
    private static final Color BACKGROUND = new Color(246, 247, 251);
    private static final Color PRIMARY = new Color(24, 119, 135);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final User currentUser;
    private final MaintenanceService maintenanceService = new MaintenanceService();
    private final JTextField roomFilter = new JTextField(5);
    private final JTextField searchField = new JTextField(15);
    private final JComboBox<String> typeFilter = new JComboBox<>(new String[]{"ALL", Maintenance.PREVENTIVE, Maintenance.CORRECTIVE, Maintenance.EMERGENCY, Maintenance.INSPECTION, Maintenance.OTHER});
    private final JComboBox<String> priorityFilter = new JComboBox<>(new String[]{"ALL", Maintenance.LOW, Maintenance.MEDIUM, Maintenance.HIGH, Maintenance.CRITICAL});
    private final JComboBox<String> statusFilter = new JComboBox<>(new String[]{"ALL", Maintenance.OPEN, Maintenance.IN_PROGRESS, Maintenance.COMPLETED, Maintenance.CANCELLED});
    private final DefaultTableModel tableModel;
    private final JTable table;

    public MaintenanceManagementPanel(User currentUser) {
        this.currentUser = currentUser;
        setLayout(new BorderLayout(10, 10));
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(18, 18, 18, 18));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.setOpaque(false);
        toolbar.add(new JLabel("Room:")); toolbar.add(roomFilter);
        toolbar.add(new JLabel("Loại:")); toolbar.add(typeFilter);
        toolbar.add(new JLabel("Ưu tiên:")); toolbar.add(priorityFilter);
        toolbar.add(new JLabel("Trạng thái:")); toolbar.add(statusFilter);
        toolbar.add(new JLabel("Tìm:")); toolbar.add(searchField);
        JButton searchButton = new JButton("Tìm kiếm");
        JButton resetButton = new JButton("Đặt lại");
        JButton addButton = new JButton("Tạo bảo trì");
        JButton editButton = new JButton("Cập nhật");
        JButton assignButton = new JButton("Phân công");
        JButton startButton = new JButton("Bắt đầu");
        JButton completeButton = new JButton("Hoàn thành");
        JButton cancelButton = new JButton("Hủy");
        toolbar.add(searchButton); toolbar.add(resetButton); toolbar.add(addButton); toolbar.add(editButton);
        toolbar.add(assignButton); toolbar.add(startButton); toolbar.add(completeButton); toolbar.add(cancelButton);
        add(toolbar, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new Object[]{"ID", "Room", "Loại", "Tiêu đề", "Ưu tiên", "Trạng thái", "Bắt đầu", "Dự kiến", "Hoàn thành", "Duration", "Người xử lý"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(tableModel);
        table.setRowHeight(30);
        table.setAutoCreateRowSorter(true);
        add(new JScrollPane(table), BorderLayout.CENTER);

        searchButton.addActionListener(e -> loadMaintenances());
        resetButton.addActionListener(e -> { roomFilter.setText(""); searchField.setText(""); typeFilter.setSelectedItem("ALL"); priorityFilter.setSelectedItem("ALL"); statusFilter.setSelectedItem("ALL"); loadMaintenances(); });
        addButton.addActionListener(e -> openEditor(null));
        editButton.addActionListener(e -> editSelected());
        assignButton.addActionListener(e -> assignSelected());
        startButton.addActionListener(e -> startSelected());
        completeButton.addActionListener(e -> completeSelected());
        cancelButton.addActionListener(e -> cancelSelected());
        searchField.addActionListener(e -> loadMaintenances());
        table.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) updateButtonState(editButton, assignButton, startButton, completeButton, cancelButton); });
        loadMaintenances();
    }

    private void loadMaintenances() {
        tableModel.setRowCount(0);
        Integer roomId = parseId(roomFilter.getText());
        List<Maintenance> records = maintenanceService.search(roomId, selected(typeFilter), selected(priorityFilter), selected(statusFilter), searchField.getText());
        LocalDateTime now = LocalDateTime.now();
        for (Maintenance record : records) {
            String status = record.isOverdue(now) ? "OVERDUE" : record.getStatus();
            tableModel.addRow(new Object[]{record.getMaintenanceId(), record.getRoomNumber(), record.getMaintenanceType(), record.getTitle(), record.getPriority(), status,
                    display(record.getStartedAt()), display(record.getExpectedEndAt()), display(record.getCompletedAt()), formatDuration(record.getDurationMinutes()),
                    record.getAssignedToName() == null ? "-" : record.getAssignedToName()});
        }
    }

    private void updateButtonState(JButton edit, JButton assign, JButton start, JButton complete, JButton cancel) {
        Maintenance record = selectedMaintenance();
        boolean exists = record != null;
        boolean manager = PermissionService.canManageMaintenance(currentUser);
        edit.setEnabled(exists);
        assign.setEnabled(exists && manager);
        start.setEnabled(exists && manager && Maintenance.OPEN.equals(record.getStatus()));
        complete.setEnabled(exists && manager && Maintenance.IN_PROGRESS.equals(record.getStatus()));
        cancel.setEnabled(exists && manager && (Maintenance.OPEN.equals(record.getStatus()) || Maintenance.IN_PROGRESS.equals(record.getStatus())));
    }

    private Maintenance selectedMaintenance() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return null;
        int modelRow = table.convertRowIndexToModel(viewRow);
        return maintenanceService.findById((Integer) tableModel.getValueAt(modelRow, 0));
    }

    private void editSelected() { Maintenance record = selectedMaintenance(); if (record != null) openEditor(record); }

    private void openEditor(Maintenance existing) {
        JTextField roomField = new JTextField(existing == null ? "" : String.valueOf(existing.getRoomId()));
        JTextField titleField = new JTextField(existing == null ? "" : existing.getTitle());
        JComboBox<String> typeBox = new JComboBox<>(new String[]{Maintenance.PREVENTIVE, Maintenance.CORRECTIVE, Maintenance.EMERGENCY, Maintenance.INSPECTION, Maintenance.OTHER});
        JComboBox<String> priorityBox = new JComboBox<>(new String[]{Maintenance.LOW, Maintenance.MEDIUM, Maintenance.HIGH, Maintenance.CRITICAL});
        JTextField expectedField = new JTextField(existing == null ? "" : display(existing.getExpectedEndAt()));
        JTextArea descriptionArea = new JTextArea(existing == null ? "" : empty(existing.getDescription()), 3, 26);
        JTextArea notesArea = new JTextArea(existing == null ? "" : empty(existing.getNotes()), 3, 26);
        if (existing != null) { typeBox.setSelectedItem(existing.getMaintenanceType()); priorityBox.setSelectedItem(existing.getPriority()); roomField.setEnabled(false); }

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints(); c.insets = new Insets(4, 4, 4, 4); c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1;
        addField(form, c, 0, "Room ID:", roomField); addField(form, c, 1, "Loại:", typeBox); addField(form, c, 2, "Ưu tiên:", priorityBox); addField(form, c, 3, "Tiêu đề:", titleField); addField(form, c, 4, "Dự kiến (yyyy-MM-dd HH:mm):", expectedField);
        addArea(form, c, 5, "Mô tả:", descriptionArea); addArea(form, c, 6, "Ghi chú:", notesArea);
        int result = JOptionPane.showConfirmDialog(this, new JScrollPane(form), existing == null ? "Tạo bảo trì" : "Cập nhật bảo trì", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        Integer roomId = parseId(roomField.getText());
        LocalDateTime expected = parseDate(expectedField.getText());
        if (roomId == null || roomId <= 0 || (expectedField.getText() != null && !expectedField.getText().isBlank() && expected == null)) {
            JOptionPane.showMessageDialog(this, "Room ID hoặc thời gian dự kiến không hợp lệ.", "Lỗi dữ liệu", JOptionPane.ERROR_MESSAGE);
            return;
        }
        Maintenance record = existing == null ? new Maintenance() : existing;
        record.setRoomId(roomId); record.setMaintenanceType((String) typeBox.getSelectedItem()); record.setPriority((String) priorityBox.getSelectedItem()); record.setTitle(titleField.getText()); record.setExpectedEndAt(expected);
        record.setDescription(descriptionArea.getText()); record.setNotes(notesArea.getText());
        boolean success = existing == null ? maintenanceService.create(record, currentUser) > 0 : maintenanceService.update(record, currentUser);
        if (!success) JOptionPane.showMessageDialog(this, "Không thể lưu bảo trì. Kiểm tra quyền, room hoặc dữ liệu.", "Lỗi", JOptionPane.ERROR_MESSAGE);
        loadMaintenances();
    }

    private void assignSelected() {
        Maintenance record = selectedMaintenance(); if (record == null) return;
        String value = JOptionPane.showInputDialog(this, "Nhập User ID thực hiện (để trống để bỏ phân công):", record.getAssignedTo() == null ? "" : String.valueOf(record.getAssignedTo()));
        if (value == null) return;
        if (!maintenanceService.assign(record.getMaintenanceId(), parseId(value), currentUser)) JOptionPane.showMessageDialog(this, "Không thể phân công user.", "Lỗi", JOptionPane.ERROR_MESSAGE);
        loadMaintenances();
    }

    private void startSelected() {
        Maintenance record = selectedMaintenance(); if (record == null) return;
        if (!maintenanceService.start(record.getMaintenanceId(), currentUser)) JOptionPane.showMessageDialog(this, "Không thể bắt đầu. Phòng có thể đang OCCUPIED/RESERVED hoặc đã có bảo trì khác.", "Lỗi", JOptionPane.ERROR_MESSAGE);
        loadMaintenances();
    }

    private void completeSelected() {
        Maintenance record = selectedMaintenance(); if (record == null) return;
        String resolution = JOptionPane.showInputDialog(this, "Kết quả bảo trì:", record.getResolution() == null ? "" : record.getResolution());
        if (resolution == null || resolution.isBlank()) return;
        if (!maintenanceService.complete(record.getMaintenanceId(), resolution, currentUser)) JOptionPane.showMessageDialog(this, "Không thể hoàn thành bảo trì.", "Lỗi", JOptionPane.ERROR_MESSAGE);
        loadMaintenances();
    }

    private void cancelSelected() {
        Maintenance record = selectedMaintenance(); if (record == null) return;
        if (!maintenanceService.cancel(record.getMaintenanceId(), currentUser)) JOptionPane.showMessageDialog(this, "Không thể hủy bảo trì.", "Lỗi", JOptionPane.ERROR_MESSAGE);
        loadMaintenances();
    }

    private void addField(JPanel panel, GridBagConstraints c, int row, String label, JComponent component) { c.gridy = row; c.gridx = 0; c.weightx = 0; panel.add(new JLabel(label), c); c.gridx = 1; c.weightx = 1; panel.add(component, c); }
    private void addArea(JPanel panel, GridBagConstraints c, int row, String label, JTextArea area) { area.setLineWrap(true); area.setWrapStyleWord(true); addField(panel, c, row, label, new JScrollPane(area)); }
    private String selected(JComboBox<String> combo) { return (String) combo.getSelectedItem(); }
    private Integer parseId(String text) { try { return text == null || text.isBlank() ? null : Integer.valueOf(text.trim()); } catch (NumberFormatException e) { return -1; } }
    private LocalDateTime parseDate(String text) { if (text == null || text.isBlank()) return null; try { return LocalDateTime.parse(text.trim(), FORMATTER); } catch (DateTimeParseException e) { return null; } }
    private String display(LocalDateTime value) { return value == null ? "-" : FORMATTER.format(value); }
    private String empty(String value) { return value == null ? "" : value; }
    private String formatDuration(Integer minutes) { if (minutes == null) return "-"; return (minutes / 60) + "h " + (minutes % 60) + "m"; }
}
