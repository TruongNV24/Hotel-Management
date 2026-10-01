package com.cnj42.hotel.ui;

import com.cnj42.hotel.model.Service;
import com.cnj42.hotel.model.User;
import com.cnj42.hotel.service.PermissionService;
import com.cnj42.hotel.service.ServiceService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class ServiceManagementPanel extends JPanel {
    private static final Color PRIMARY = new Color(24, 119, 135);
    private static final Color BACKGROUND = new Color(246, 247, 251);
    private static final Color TEXT_DARK = new Color(30, 53, 61);
    private static final Color TEXT_MUTED = new Color(105, 127, 132);
    private static final Color BORDER = new Color(225, 233, 235);
    private static final Color GREEN = new Color(45, 160, 108);
    private static final Color RED = new Color(214, 82, 88);
    private final User currentUser;
    private final ServiceService serviceService = new ServiceService();
    private final JTextField searchField = new JTextField(20);
    private final JComboBox<String> statusFilter = new JComboBox<>(new String[]{"ALL", "ACTIVE", "INACTIVE"});
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final NumberFormat currency = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
    private final JLabel totalCount = new JLabel("0");
    private final JLabel activeCount = new JLabel("0");
    private final JLabel inactiveCount = new JLabel("0");
    private final JLabel resultCount = new JLabel("0");

    public ServiceManagementPanel(User currentUser) {
        this.currentUser = currentUser;
        setLayout(new BorderLayout(0, 16));
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(8, 14, 18, 14));

        if (!PermissionService.canManageServices(currentUser)) {
            JLabel denied = new JLabel("Bạn không có quyền quản lý danh mục dịch vụ.", SwingConstants.CENTER);
            denied.setForeground(new Color(180, 0, 0));
            denied.setFont(new Font("Segoe UI", Font.BOLD, 18));
            add(denied, BorderLayout.CENTER);
            tableModel = null;
            table = null;
            return;
        }

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
        JButton searchButton = new JButton("Tìm kiếm");
        JButton refreshButton = new JButton("Làm mới");
        styleButton(searchButton, PRIMARY, Color.WHITE);
        styleButton(refreshButton, new Color(239, 246, 247), PRIMARY);
        addFilterLabel(filterRow, "Từ khóa");
        filterRow.add(searchField);
        addFilterLabel(filterRow, "Trạng thái");
        filterRow.add(statusFilter);
        filterRow.add(searchButton);
        filterRow.add(refreshButton);

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        actionRow.setOpaque(false);
        JButton addButton = new JButton("Thêm dịch vụ");
        JButton editButton = new JButton("Sửa");
        JButton toggleButton = new JButton("Bật/Tắt");
        styleButton(addButton, PRIMARY, Color.WHITE);
        styleButton(editButton, new Color(239, 246, 247), PRIMARY);
        styleButton(toggleButton, new Color(255, 244, 229), new Color(224, 142, 57));
        actionRow.add(addButton);
        actionRow.add(editButton);
        actionRow.add(toggleButton);
        toolbar.add(filterRow);
        toolbar.add(actionRow);
        top.add(toolbar, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new Object[]{"ID", "Tên dịch vụ", "Đơn vị", "Mô tả", "Đơn giá", "Trạng thái"}, 0) {
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
        table.setFillsViewportHeight(true);
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createLineBorder(BORDER, 1, true));
        tableScroll.getViewport().setBackground(Color.WHITE);
        add(tableScroll, BorderLayout.CENTER);

        searchButton.addActionListener(e -> loadServices());
        refreshButton.addActionListener(e -> {
            searchField.setText("");
            statusFilter.setSelectedItem("ALL");
            loadServices();
        });
        addButton.addActionListener(e -> openEditor(null));
        editButton.addActionListener(e -> editSelected());
        toggleButton.addActionListener(e -> toggleSelected());
        searchField.addActionListener(e -> loadServices());
        loadServices();
    }

    private void loadServices() {
        if (tableModel == null) return;
        tableModel.setRowCount(0);
        String status = (String) statusFilter.getSelectedItem();
        List<Service> services = serviceService.search(searchField.getText(), status);
        int active = 0;
        int inactive = 0;
        for (Service service : services) {
            if ("ACTIVE".equalsIgnoreCase(service.getStatus())) active++; else inactive++;
            tableModel.addRow(new Object[]{service.getServiceId(), service.getServiceName(), service.getUnit(),
                    service.getDescription() == null ? "-" : service.getDescription(), currency.format(service.getPrice()), service.getStatus()});
        }
        resultCount.setText(String.valueOf(services.size()));
        activeCount.setText(String.valueOf(active));
        inactiveCount.setText(String.valueOf(inactive));
        totalCount.setText(String.valueOf(serviceService.search(null, "ALL").size()));
    }

    private JPanel buildStatsPanel() {
        JPanel stats = new JPanel(new GridLayout(1, 4, 12, 0));
        stats.setOpaque(false);
        stats.add(createMetricCard("TỔNG DỊCH VỤ", totalCount, "Tất cả danh mục", PRIMARY));
        stats.add(createMetricCard("ĐANG HOẠT ĐỘNG", activeCount, "Có thể sử dụng", GREEN));
        stats.add(createMetricCard("NGỪNG HOẠT ĐỘNG", inactiveCount, "Tạm ẩn khỏi hệ thống", RED));
        stats.add(createMetricCard("KẾT QUẢ HIỆN TẠI", resultCount, "Theo bộ lọc", PRIMARY));
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

    private void editSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn dịch vụ.", "Thiếu dữ liệu", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int serviceId = (Integer) tableModel.getValueAt(table.convertRowIndexToModel(row), 0);
        Service service = serviceService.search(null, "ALL").stream().filter(item -> item.getServiceId() == serviceId).findFirst().orElse(null);
        if (service != null) openEditor(service);
    }

    private void toggleSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn dịch vụ.", "Thiếu dữ liệu", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = table.convertRowIndexToModel(row);
        int id = (Integer) tableModel.getValueAt(modelRow, 0);
        String status = String.valueOf(tableModel.getValueAt(modelRow, 5));
        if (serviceService.setActive(id, !"ACTIVE".equalsIgnoreCase(status), currentUser)) loadServices();
    }

    private void openEditor(Service existing) {
        JTextField nameField = new JTextField(existing == null ? "" : existing.getServiceName());
        JTextField unitField = new JTextField(existing == null ? "Unit" : existing.getUnit());
        JTextField priceField = new JTextField(existing == null ? "" : String.valueOf(existing.getPrice()));
        JTextField descriptionField = new JTextField(existing == null || existing.getDescription() == null ? "" : existing.getDescription());
        JComboBox<String> statusBox = new JComboBox<>(new String[]{"ACTIVE", "INACTIVE"});
        if (existing != null) statusBox.setSelectedItem(existing.getStatus());

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Tên dịch vụ:")); form.add(nameField);
        form.add(new JLabel("Đơn vị:")); form.add(unitField);
        form.add(new JLabel("Đơn giá:")); form.add(priceField);
        form.add(new JLabel("Mô tả:")); form.add(descriptionField);
        form.add(new JLabel("Trạng thái:")); form.add(statusBox);

        int result = JOptionPane.showConfirmDialog(this, form, existing == null ? "Thêm dịch vụ" : "Sửa dịch vụ",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        double price;
        try {
            price = Double.parseDouble(priceField.getText().trim());
        } catch (NumberFormatException exception) {
            JOptionPane.showMessageDialog(this, "Đơn giá phải là số hợp lệ.", "Lỗi dữ liệu", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Service service = existing == null ? new Service() : existing;
        service.setServiceName(nameField.getText());
        service.setUnit(unitField.getText());
        service.setPrice(price);
        service.setDescription(descriptionField.getText());
        service.setStatus((String) statusBox.getSelectedItem());
        boolean saved = existing == null ? serviceService.create(service, currentUser) : serviceService.update(service, currentUser);
        if (!saved) {
            JOptionPane.showMessageDialog(this, "Không thể lưu dịch vụ. Kiểm tra tên, giá hoặc quyền truy cập.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        loadServices();
    }
}
