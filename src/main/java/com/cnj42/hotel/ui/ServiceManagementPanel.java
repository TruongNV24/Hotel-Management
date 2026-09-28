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
    private static final Color PRIMARY = new Color(105, 78, 210);
    private static final Color BACKGROUND = new Color(246, 247, 251);
    private final User currentUser;
    private final ServiceService serviceService = new ServiceService();
    private final JTextField searchField = new JTextField(20);
    private final JComboBox<String> statusFilter = new JComboBox<>(new String[]{"ALL", "ACTIVE", "INACTIVE"});
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final NumberFormat currency = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    public ServiceManagementPanel(User currentUser) {
        this.currentUser = currentUser;
        setLayout(new BorderLayout(12, 12));
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        if (!PermissionService.canManageServices(currentUser)) {
            JLabel denied = new JLabel("Bạn không có quyền quản lý danh mục dịch vụ.", SwingConstants.CENTER);
            denied.setForeground(new Color(180, 0, 0));
            denied.setFont(new Font("Segoe UI", Font.BOLD, 18));
            add(denied, BorderLayout.CENTER);
            tableModel = null;
            table = null;
            return;
        }

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.setOpaque(false);
        JButton searchButton = new JButton("Tìm kiếm");
        JButton refreshButton = new JButton("Làm mới");
        JButton addButton = new JButton("Thêm dịch vụ");
        JButton editButton = new JButton("Sửa");
        JButton toggleButton = new JButton("Bật/Tắt");
        toolbar.add(new JLabel("Từ khóa:"));
        toolbar.add(searchField);
        toolbar.add(new JLabel("Trạng thái:"));
        toolbar.add(statusFilter);
        toolbar.add(searchButton);
        toolbar.add(refreshButton);
        toolbar.add(addButton);
        toolbar.add(editButton);
        toolbar.add(toggleButton);
        add(toolbar, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new Object[]{"ID", "Tên dịch vụ", "Đơn vị", "Mô tả", "Đơn giá", "Trạng thái"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setRowHeight(30);
        table.setFillsViewportHeight(true);
        add(new JScrollPane(table), BorderLayout.CENTER);

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
        for (Service service : services) {
            tableModel.addRow(new Object[]{service.getServiceId(), service.getServiceName(), service.getUnit(),
                    service.getDescription() == null ? "-" : service.getDescription(), currency.format(service.getPrice()), service.getStatus()});
        }
    }

    private void editSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn dịch vụ.", "Thiếu dữ liệu", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int serviceId = (Integer) tableModel.getValueAt(row, 0);
        Service service = serviceService.search(null, "ALL").stream().filter(item -> item.getServiceId() == serviceId).findFirst().orElse(null);
        if (service != null) openEditor(service);
    }

    private void toggleSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn dịch vụ.", "Thiếu dữ liệu", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int id = (Integer) tableModel.getValueAt(row, 0);
        String status = String.valueOf(tableModel.getValueAt(row, 5));
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
