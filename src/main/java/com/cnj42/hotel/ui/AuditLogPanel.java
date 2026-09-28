package com.cnj42.hotel.ui;

import com.cnj42.hotel.model.AuditLog;
import com.cnj42.hotel.service.AuditLogService;
import com.cnj42.hotel.service.PermissionService;
import com.cnj42.hotel.model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AuditLogPanel extends JPanel {

    private final User currentUser;
    private final AuditLogService auditLogService = new AuditLogService();
    private JTable logTable;
    private DefaultTableModel tableModel;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public AuditLogPanel(User currentUser) {
        this.currentUser = currentUser;

        if (currentUser == null || !PermissionService.canViewAuditLogs(currentUser)) {
            setLayout(new BorderLayout());
            JLabel message = new JLabel("Bạn không có quyền xem nhật ký hệ thống.", SwingConstants.CENTER);
            message.setForeground(new Color(180, 0, 0));
            message.setFont(new Font("Segoe UI", Font.BOLD, 18));
            add(message, BorderLayout.CENTER);
            return;
        }

        setBackground(new Color(246, 248, 251));
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.setOpaque(false);

        JTextField searchField = new JTextField(20);
        searchField.setToolTipText("Tìm theo người dùng, chi tiết, module");
        JButton refreshBtn = new JButton("Làm mới");
        JButton filterBtn = new JButton("Lọc");

        toolbar.add(new JLabel("Tìm kiếm:"));
        toolbar.add(searchField);
        toolbar.add(refreshBtn);
        toolbar.add(filterBtn);
        add(toolbar, BorderLayout.NORTH);

        String[] columns = {"ID", "Thời gian", "Hành động", "Module", "Người thực hiện", "Vai trò", "Trạng thái", "Chi tiết"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        logTable = new JTable(tableModel);
        logTable.setRowHeight(28);
        logTable.setFillsViewportHeight(true);
        logTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        logTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        logTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        logTable.getColumnModel().getColumn(2).setPreferredWidth(140);
        logTable.getColumnModel().getColumn(3).setPreferredWidth(120);
        logTable.getColumnModel().getColumn(4).setPreferredWidth(160);
        logTable.getColumnModel().getColumn(5).setPreferredWidth(100);
        logTable.getColumnModel().getColumn(6).setPreferredWidth(100);
        logTable.getColumnModel().getColumn(7).setPreferredWidth(500);

        JScrollPane scrollPane = new JScrollPane(logTable);
        add(scrollPane, BorderLayout.CENTER);

        refreshBtn.addActionListener(e -> loadLogs(searchField.getText(), null, null, null));
        filterBtn.addActionListener(e -> loadLogs(searchField.getText(), null, null, null));

        loadLogs(null, null, null, null);
    }

    private void loadLogs(String keyword, String action, String module, String status) {
        tableModel.setRowCount(0);
        List<AuditLog> logs = auditLogService.searchLogs(keyword, action, module, status);
        for (AuditLog log : logs) {
            tableModel.addRow(new Object[] {
                    log.getAuditLogId(),
                    log.getCreatedAt() != null ? log.getCreatedAt().format(formatter) : "-",
                    log.getAction(),
                    log.getModule(),
                    log.getActorUsername() == null ? "SYSTEM" : log.getActorUsername(),
                    log.getActorRole() == null ? "-" : log.getActorRole(),
                    log.getStatus(),
                    log.getDetails() == null ? "-" : log.getDetails()
            });
        }
    }
}
