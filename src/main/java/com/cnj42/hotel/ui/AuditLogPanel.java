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

    private static final Color BACKGROUND = new Color(244, 248, 248);
    private static final Color PRIMARY = new Color(24, 119, 135);
    private static final Color TEXT_DARK = new Color(30, 53, 61);
    private static final Color TEXT_MUTED = new Color(105, 127, 132);
    private static final Color BORDER = new Color(225, 233, 235);

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

        setBackground(BACKGROUND);
        setLayout(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(8, 14, 18, 14));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.setBackground(Color.WHITE);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1, true),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        JTextField searchField = new JTextField(20);
        searchField.setToolTipText("Tìm theo người dùng, chi tiết, module");
        JButton refreshBtn = new JButton("Làm mới");
        JButton filterBtn = new JButton("Lọc");
        styleButton(refreshBtn, new Color(239, 246, 247), PRIMARY);
        styleButton(filterBtn, PRIMARY, Color.WHITE);

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
        logTable.setRowHeight(36);
        logTable.setFillsViewportHeight(true);
        logTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        logTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        logTable.setForeground(TEXT_DARK);
        logTable.setBackground(Color.WHITE);
        logTable.setSelectionBackground(new Color(225, 243, 245));
        logTable.setSelectionForeground(TEXT_DARK);
        logTable.setShowVerticalLines(false);
        logTable.setGridColor(new Color(239, 243, 244));
        logTable.setAutoCreateRowSorter(true);
        logTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        logTable.getTableHeader().setForeground(TEXT_MUTED);
        logTable.getTableHeader().setBackground(new Color(247, 250, 250));
        logTable.getTableHeader().setPreferredSize(new Dimension(0, 38));
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

    private void styleButton(JButton button, Color background, Color foreground) {
        button.setBackground(background);
        button.setForeground(foreground);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 13, 8, 13));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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
