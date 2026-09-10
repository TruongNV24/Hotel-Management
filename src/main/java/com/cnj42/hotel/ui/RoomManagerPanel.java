package com.cnj42.hotel.ui;

import com.cnj42.hotel.model.Room;
import com.cnj42.hotel.model.RoomType;
import com.cnj42.hotel.service.RoomService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;

/** Room list and management page. */
public class RoomManagerPanel extends JPanel {
    private static final Color PRIMARY = new Color(85, 110, 230); // Màu tím xanh của nút
    private static final Color BACKGROUND = new Color(248, 248, 251);
    private static final Color TEXT_DARK = new Color(73, 80, 87);
    private static final Color TEXT_GRAY = new Color(135, 143, 153);
    private static final Color BORDER = new Color(239, 242, 247);
    
    // Màu cho trạng thái
    private static final Color GREEN = new Color(52, 195, 143);
    private static final Color GREEN_BG = new Color(212, 245, 233);
    private static final Color ORANGE = new Color(241, 180, 76);
    private static final Color ORANGE_BG = new Color(253, 236, 205);
    private static final Color BLUE = new Color(85, 110, 230);
    private static final Color BLUE_BG = new Color(223, 229, 252);
    private static final Color RED = new Color(244, 106, 106);
    private static final Color RED_BG = new Color(253, 225, 225);
    private static final Color GRAY_BG = new Color(233, 236, 239);

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0");

    private final RoomService roomService = new RoomService();
    private final Runnable addRoomAction;
    private final java.util.function.Consumer<Room> editRoomAction;
    private final JTextField searchField = new JTextField();
    private final JComboBox<String> typeFilter = new JComboBox<>();
    private final JComboBox<String> statusFilter = new JComboBox<>(new String[]{"Tất cả", "Trống", "Đang sử dụng", "Đặt trước", "Dọn dẹp", "Bảo trì"});
    private final DefaultTableModel tableModel;
    private final JTable roomTable;
    
    private final JLabel totalLabel = new JLabel("0");
    private final JLabel availableLabel = new JLabel("0");
    private final JLabel occupiedLabel = new JLabel("0");
    private final JLabel maintenanceLabel = new JLabel("0");
    
    private final JLabel availableSub = new JLabel("0.0% tổng số phòng");
    private final JLabel occupiedSub = new JLabel("0.0% tổng số phòng");
    private final JLabel maintenanceSub = new JLabel("0.0% tổng số phòng");

    public RoomManagerPanel() {
        this(() -> { }, room -> { });
    }

    public RoomManagerPanel(Runnable addRoomAction) {
        this(addRoomAction, room -> { });
    }

    public RoomManagerPanel(Runnable addRoomAction, java.util.function.Consumer<Room> editRoomAction) {
        this.addRoomAction = addRoomAction;
        this.editRoomAction = editRoomAction;
        setLayout(new BorderLayout(0, 20));
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(15, 20, 20, 20));

        add(createSummary(), BorderLayout.NORTH);
        tableModel = new DefaultTableModel(new Object[]{"SỐ PHÒNG", " ", "LOẠI PHÒNG", "TẦNG", "GIÁ / ĐÊM", "TRẠNG THÁI", "GHI CHÚ", "THAO TÁC"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return column == 7; }
        };
        roomTable = createTable();
        add(createRoomCard(), BorderLayout.CENTER);
        loadRoomTypes();
        refreshRooms();
    }

    private JPanel createSummary() {
        JPanel summary = new JPanel(new GridLayout(1, 4, 20, 0));
        summary.setOpaque(false);
        summary.add(statCard("TỔNG SỐ PHÒNG", "Tất cả phòng", totalLabel, null, PRIMARY, 0));
        summary.add(statCard("PHÒNG TRỐNG", "", availableLabel, availableSub, GREEN, 1));
        summary.add(statCard("ĐANG SỬ DỤNG", "", occupiedLabel, occupiedSub, ORANGE, 2));
        summary.add(statCard("BẢO TRÌ / DỌN DẸP", "", maintenanceLabel, maintenanceSub, RED, 3));
        return summary;
    }

    private JPanel statCard(String title, String defaultSub, JLabel valueLabel, JLabel subLabel, Color color, int iconType) {
        JPanel card = new JPanel(new BorderLayout(15, 0));
        card.setBackground(Color.WHITE);
        card.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Icon bên trái
        JPanel iconPanel = new JPanel(new BorderLayout());
        iconPanel.setOpaque(false);
        iconPanel.add(new JLabel(new SummaryIcon(color, iconType)), BorderLayout.NORTH);
        card.add(iconPanel, BorderLayout.WEST);

        // Chữ bên phải
        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        titleLabel.setForeground(TEXT_GRAY);
        
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        valueLabel.setForeground(color);

        JLabel subtitle = subLabel != null ? subLabel : new JLabel(defaultSub);
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        subtitle.setForeground(TEXT_GRAY);

        textPanel.add(titleLabel);
        textPanel.add(Box.createVerticalStrut(5));
        textPanel.add(valueLabel);
        textPanel.add(Box.createVerticalStrut(5));
        textPanel.add(subtitle);

        card.add(textPanel, BorderLayout.CENTER);
        
        // Bo góc cho Card
        return new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                g2.dispose();
            }
            {
                setOpaque(false);
                setBorder(new EmptyBorder(0, 0, 0, 0));
                add(card);
                card.setOpaque(false);
            }
        };
    }

    private JPanel createRoomCard() {
        JPanel card = new JPanel(new BorderLayout(0, 15));
        card.setBackground(Color.WHITE);
        card.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        filters.setOpaque(false);
        
        searchField.setPreferredSize(new Dimension(250, 36));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER, 1, true),
            new EmptyBorder(0, 10, 0, 10)
        ));
        searchField.addActionListener(e -> refreshRooms());
        
        filters.add(searchField);
        filters.add(label("Loại phòng"));
        
        typeFilter.setPreferredSize(new Dimension(150, 36));
        typeFilter.setBackground(Color.WHITE);
        filters.add(typeFilter);
        
        filters.add(label("Trạng thái"));
        statusFilter.setPreferredSize(new Dimension(150, 36));
        statusFilter.setBackground(Color.WHITE);
        filters.add(statusFilter);
        
        JButton addButton = button("Thêm phòng", PRIMARY, Color.WHITE);
        addButton.setIcon(new LineIcon("plus", Color.WHITE, 12, 12));
        addButton.setPreferredSize(new Dimension(130, 36));
        addButton.addActionListener(e -> addRoomAction.run());
        
        JPanel addWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        addWrap.setOpaque(false);
        addWrap.setPreferredSize(new Dimension(200, 36));
        addWrap.add(addButton);
        filters.add(addWrap);
        
        typeFilter.addActionListener(e -> refreshRooms());
        statusFilter.addActionListener(e -> refreshRooms());
        card.add(filters, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(roomTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);
        card.add(scroll, BorderLayout.CENTER);
        
        return new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                g2.dispose();
            }
            {
                setOpaque(false);
                add(card);
                card.setOpaque(false);
            }
        };
    }

    private JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        label.setForeground(TEXT_DARK);
        return label;
    }

    private JButton button(String text, Color background, Color foreground) {
        JButton button = new RoomButton(text, background, foreground);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private JTable createTable() {
        JTable table = new JTable(tableModel);
        table.setRowHeight(64); // Tăng chiều cao hàng để hiện ảnh to hơn
        table.setShowVerticalLines(false); // Bỏ kẻ dọc
        table.setShowHorizontalLines(true);
        table.setGridColor(BORDER);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        table.getTableHeader().setForeground(TEXT_DARK);
        table.getTableHeader().setBackground(Color.WHITE);
        table.getTableHeader().setPreferredSize(new Dimension(100, 40));
        table.getTableHeader().setBorder(new LineBorder(BORDER, 1));
        
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setForeground(TEXT_DARK);
        
        // Màu khi click chọn hàng (Xám rất nhạt thay vì tím đậm)
        table.setSelectionBackground(new Color(248, 249, 250));
        table.setSelectionForeground(TEXT_DARK);
        
        table.getColumnModel().getColumn(0).setPreferredWidth(80);
        table.getColumnModel().getColumn(1).setPreferredWidth(100); // Cột ảnh
        table.getColumnModel().getColumn(2).setPreferredWidth(140);
        table.getColumnModel().getColumn(3).setPreferredWidth(60);
        table.getColumnModel().getColumn(4).setPreferredWidth(100);
        table.getColumnModel().getColumn(5).setPreferredWidth(130);
        table.getColumnModel().getColumn(6).setPreferredWidth(160);
        table.getColumnModel().getColumn(7).setPreferredWidth(155);
        
        table.getColumnModel().getColumn(0).setCellRenderer(new RoomNumberRenderer());
        table.getColumnModel().getColumn(1).setCellRenderer(new RoomImageRenderer());
        table.getColumnModel().getColumn(5).setCellRenderer(new StatusRenderer());
        table.getColumnModel().getColumn(7).setCellRenderer(new ActionRenderer());
        table.getColumnModel().getColumn(7).setCellEditor(new ActionEditor(editRoomAction, this::deleteRoom));
        return table;
    }

    private void loadRoomTypes() {
        typeFilter.removeAllItems();
        typeFilter.addItem("Tất cả");
        for (RoomType type : roomService.getAllRoomTypes()) typeFilter.addItem(type.getTypeName());
    }

    private void refreshRooms() {
        String keyword = searchField.getText().trim();
        String selectedStatus = (String) statusFilter.getSelectedItem();
        String status = toDatabaseStatus(selectedStatus);
        List<Room> rooms = roomService.searchRooms(keyword, status);
        String selectedType = (String) typeFilter.getSelectedItem();
        tableModel.setRowCount(0);
        
        int available = 0, occupied = 0, maintenance = 0;
        
        for (Room room : rooms) {
            String typeName = room.getRoomType() == null ? "" : room.getRoomType().getTypeName();
            if (selectedType != null && !"Tất cả".equals(selectedType) && !selectedType.equals(typeName)) continue;
            
            if ("AVAILABLE".equals(room.getStatus())) available++;
            if ("OCCUPIED".equals(room.getStatus()) || "RESERVED".equals(room.getStatus())) occupied++;
            if ("MAINTENANCE".equals(room.getStatus()) || "CLEANING".equals(room.getStatus())) maintenance++;
            
            double price = room.getRoomType() == null ? 0 : room.getRoomType().getPricePerNight();
            tableModel.addRow(new Object[]{
                room.getRoomNumber() + "\n" + typeName, 
                room,
                typeName, 
                room.getFloor(), 
                MONEY.format(price) + " đ", 
                room.getStatus(), 
                room.getNote() != null ? room.getNote() : "", 
                room
            });
        }
        
        int total = tableModel.getRowCount();
        totalLabel.setText(String.valueOf(total));
        availableLabel.setText(String.valueOf(available));
        occupiedLabel.setText(String.valueOf(occupied));
        maintenanceLabel.setText(String.valueOf(maintenance));
        
        if (total > 0) {
            availableSub.setText(String.format("%.1f%% tổng số phòng", (available * 100.0) / total));
            occupiedSub.setText(String.format("%.1f%% tổng số phòng", (occupied * 100.0) / total));
            maintenanceSub.setText(String.format("%.1f%% tổng số phòng", (maintenance * 100.0) / total));
        } else {
            availableSub.setText("0.0% tổng số phòng");
            occupiedSub.setText("0.0% tổng số phòng");
            maintenanceSub.setText("0.0% tổng số phòng");
        }
    }

    private String toDatabaseStatus(String status) {
        if (status == null || "Tất cả".equals(status)) return "";
        return switch (status) {
            case "Trống" -> "AVAILABLE";
            case "Đang sử dụng" -> "OCCUPIED";
            case "Đặt trước" -> "RESERVED";
            case "Dọn dẹp" -> "CLEANING";
            case "Bảo trì" -> "MAINTENANCE";
            default -> status;
        };
    }

    private void deleteRoom(Room room) {
        int result = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn xóa phòng " + room.getRoomNumber() + "?",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (result == JOptionPane.YES_OPTION && roomService.deleteRoom(room.getRoomId())) {
            refreshRooms();
        } else if (result == JOptionPane.YES_OPTION) {
            JOptionPane.showMessageDialog(this, "Không thể xóa phòng. Phòng có thể đang được sử dụng.",
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    // --- CÁC RENDERER GIAO DIỆN MỚI ---

    private static class StatusRenderer extends DefaultTableCellRenderer {
        @Override 
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int column) {
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 18));
            panel.setOpaque(true);
            panel.setBackground(selected ? table.getSelectionBackground() : Color.WHITE);
            
            String status = value == null ? "" : value.toString();
            String text = translate(status);
            Color fgColor = color(status);
            Color bgColor = bgColor(status);

            JLabel label = new JLabel(text) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(bgColor);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20); // Pill shape
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            
            label.setFont(new Font("Segoe UI", Font.BOLD, 11));
            label.setForeground(fgColor);
            label.setBorder(new EmptyBorder(4, 12, 4, 12));
            label.setIcon(new DotIcon(fgColor));
            label.setIconTextGap(6);

            panel.add(label);
            return panel;
        }
        private static String translate(String status) { return switch (status) { case "AVAILABLE" -> "Trống"; case "OCCUPIED" -> "Đang sử dụng"; case "RESERVED" -> "Đặt trước"; case "CLEANING" -> "Dọn dẹp"; case "MAINTENANCE" -> "Bảo trì"; default -> status; }; }
        private static Color color(String status) { return switch (status) { case "AVAILABLE" -> GREEN; case "OCCUPIED" -> ORANGE; case "RESERVED" -> BLUE; case "MAINTENANCE", "CLEANING" -> RED; default -> TEXT_GRAY; }; }
        private static Color bgColor(String status) { return switch (status) { case "AVAILABLE" -> GREEN_BG; case "OCCUPIED" -> ORANGE_BG; case "RESERVED" -> BLUE_BG; case "MAINTENANCE", "CLEANING" -> RED_BG; default -> GRAY_BG; }; }
    }

    private static class ActionRenderer extends DefaultTableCellRenderer {
        @Override 
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int column) {
            return new ActionPanel(selected ? table.getSelectionBackground() : Color.WHITE, null, null);
        }
    }

    private static class ActionEditor extends AbstractCellEditor implements TableCellEditor {
        private final java.util.function.Consumer<Room> editAction;
        private final java.util.function.Consumer<Room> deleteAction;
        private final JPanel panel = new ActionPanel(Color.WHITE, null, null);

        ActionEditor(java.util.function.Consumer<Room> editAction, java.util.function.Consumer<Room> deleteAction) {
            this.editAction = editAction;
            this.deleteAction = deleteAction;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean selected, int row, int column) {
            Room room = value instanceof Room ? (Room) value : null;
            panel.removeAll();
            JButton editButton = actionButton("Sửa", ORANGE, room == null ? null : () -> editAction.accept(room));
            JButton deleteButton = actionButton("Xóa", RED, room == null ? null : () -> deleteAction.accept(room));
            panel.add(editButton);
            panel.add(deleteButton);
            panel.revalidate();
            return panel;
        }

        @Override
        public Object getCellEditorValue() { return null; }
    }

    private static class ActionPanel extends JPanel {
        ActionPanel(Color background, Runnable editAction, Runnable deleteAction) {
            super(new FlowLayout(FlowLayout.CENTER, 6, 14));
            setOpaque(true);
            setBackground(background);
            add(actionButton("Sửa", ORANGE, editAction));
            add(actionButton("Xóa", RED, deleteAction));
        }
    }

    private static JButton actionButton(String text, Color color, Runnable action) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 11));
        button.setForeground(color);
        button.setBackground(Color.WHITE);
        button.setBorder(new LineBorder(color, 1, true));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(58, 30));
        if (action != null) button.addActionListener(e -> action.run());
        return button;
    }

    private static class RoomNumberRenderer extends DefaultTableCellRenderer {
        @Override 
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, selected, focus, row, column);
            String[] parts = String.valueOf(value == null ? "" : value).split("\\n", 2);
            String number = parts.length > 0 ? parts[0] : "";
            String type = parts.length > 1 ? parts[1] : "";
            label.setText("<html><div style='margin-left: 10px;'><b style='font-size: 11px; color:#343a40;'>" + number + "</b><br><font size='3' color='#878f99'>" + type + "</font></div></html>");
            return label;
        }
    }

    private static class RoomImageRenderer extends DefaultTableCellRenderer {
        @Override 
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, selected, focus, row, column);
            Room room = value instanceof Room ? (Room) value : null;
            label.setText("");
            label.setIcon(new RoomImageIcon(room, row));
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setBackground(selected ? table.getSelectionBackground() : Color.WHITE);
            return label;
        }
    }

    // --- ICONS VÀ CÁC THÀNH PHẦN VẼ TAY ---

    private static class SummaryIcon implements Icon {
        private final Color color;
        private final int type;
        SummaryIcon(Color color, int type) { this.color = color; this.type = type; }
        @Override 
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Vẽ nền hình tròn nhạt
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 25));
            g2.fillOval(x, y, 54, 54);
            
            g2.setColor(color);
            g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            
            int cx = x + 27, cy = y + 27;
            // Vẽ icon minh họa tương tự mockup
            if (type == 0) { // All rooms
                g2.drawRect(cx - 10, cy - 10, 8, 8);
                g2.drawRect(cx + 2, cy - 10, 8, 8);
                g2.drawRect(cx - 10, cy + 2, 8, 8);
                g2.drawRect(cx + 2, cy + 2, 8, 8);
            } else if (type == 1) { // Available
                g2.drawRect(cx - 10, cy - 8, 20, 16);
                g2.drawLine(cx - 4, cy - 2, cx + 4, cy - 2);
                g2.drawLine(cx - 4, cy + 2, cx + 4, cy + 2);
            } else if (type == 2) { // Occupied
                g2.drawRoundRect(cx - 12, cy - 9, 24, 18, 4, 4);
                g2.drawLine(cx - 6, cy, cx + 6, cy);
            } else { // Maintenance
                g2.drawLine(cx - 8, cy + 8, cx + 4, cy - 4);
                g2.drawOval(cx + 2, cy - 8, 6, 6);
            }
            g2.dispose();
        }
        @Override public int getIconWidth() { return 54; }
        @Override public int getIconHeight() { return 54; }
    }

    private static class RoomImageIcon implements Icon {
        private final Room room;
        private final int variant;
        RoomImageIcon(Room room, int variant) { this.room = room; this.variant = variant; }
        
        @Override 
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Xử lý logic lấy ảnh ĐẦU TIÊN của phòng
            File selectedImage = null;
            if (room != null && room.getImagePath() != null && !room.getImagePath().isBlank()) {
                String firstImagePath = firstJsonImagePath(room.getImagePath());
                if (firstImagePath != null) selectedImage = new File(firstImagePath);
            }
            String roomNumber = room == null ? "" : room.getRoomNumber();
            File folder = new File("uploads/rooms/" + roomNumber);
            if (folder.exists() && folder.isDirectory()) {
                File[] files = folder.listFiles((dir, name) -> name.toLowerCase().matches(".*\\.(png|jpg|jpeg)"));
                if (files != null && files.length > 0) {
                    Arrays.sort(files); // Đảm bảo lấy theo thứ tự
                    selectedImage = files[0];
                }
            }
            if (selectedImage == null || !selectedImage.isFile()) {
                selectedImage = AddRoomPanel.getRoomImage(roomNumber); // Fallback cũ
            }

            int imgW = 76, imgH = 48;
            int offsetY = y + 8;
            
            if (selectedImage != null && selectedImage.isFile()) {
                Image image = new ImageIcon(selectedImage.getAbsolutePath()).getImage();
                // Clip để bo góc ảnh thật
                g2.setClip(new RoundRectangle2D.Float(x, offsetY, imgW, imgH, 8, 8));
                g2.drawImage(image, x, offsetY, imgW, imgH, null);
                g2.setClip(null);
            } else {
                // Placeholder nếu chưa có ảnh
                g2.setColor(new Color(233, 236, 239));
                g2.fillRoundRect(x, offsetY, imgW, imgH, 8, 8);
                g2.setColor(new Color(173, 181, 189));
                g2.setStroke(new BasicStroke(2f));
                g2.drawRect(x + 32, offsetY + 18, 12, 10);
                g2.drawLine(x + 34, offsetY + 18, x + 38, offsetY + 12);
                g2.drawLine(x + 38, offsetY + 12, x + 42, offsetY + 18);
            }
            g2.dispose();
        }

        private String firstJsonImagePath(String json) {
            String value = json.trim();
            if (!value.startsWith("[\"") || value.length() < 4) return value;
            StringBuilder path = new StringBuilder();
            boolean escaped = false;
            for (int i = 2; i < value.length(); i++) {
                char current = value.charAt(i);
                if (escaped) {
                    path.append(current);
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == '"') {
                    return path.toString();
                } else {
                    path.append(current);
                }
            }
            return null;
        }
        @Override public int getIconWidth() { return 80; }
        @Override public int getIconHeight() { return 64; }
    }

    private static class ActionIcon implements Icon {
        ActionIcon() { }
        @Override 
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            int btnW = 32, btnH = 32;
            int yPos = y + 16; 
            
            // --- NÚT SỬA (Màu Xanh) ---
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(x, yPos, btnW, btnH, 8, 8);
            g2.setColor(new Color(85, 110, 230, 80)); // Viền xanh nhạt
            g2.drawRoundRect(x, yPos, btnW, btnH, 8, 8);
            
            // Vẽ cây bút
            g2.setColor(new Color(85, 110, 230));
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int px = x + 9, py = yPos + 18;
            g2.drawLine(px, py, px + 3, py + 3); // tip
            g2.drawLine(px, py, px + 4, py - 4); 
            g2.drawLine(px + 4, py - 4, px + 12, py - 12); // thân trên
            g2.drawLine(px + 3, py + 3, px + 11, py - 5); // thân dưới
            g2.drawLine(px + 12, py - 12, px + 15, py - 9); // đuôi
            g2.drawLine(px + 11, py - 5, px + 15, py - 9);
            g2.drawLine(px + 3, py + 3, px + 6, py + 3); // gạch dưới

            // --- NÚT XÓA (Màu Đỏ) ---
            int x2 = x + 40;
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(x2, yPos, btnW, btnH, 8, 8);
            g2.setColor(new Color(244, 106, 106, 80)); // Viền đỏ nhạt
            g2.drawRoundRect(x2, yPos, btnW, btnH, 8, 8);
            
            // Vẽ thùng rác
            g2.setColor(new Color(244, 106, 106));
            g2.drawLine(x2 + 10, yPos + 10, x2 + 22, yPos + 10); // Nắp
            g2.drawLine(x2 + 14, yPos + 10, x2 + 14, yPos + 8); // Quai nắp
            g2.drawLine(x2 + 18, yPos + 10, x2 + 18, yPos + 8);
            g2.drawLine(x2 + 14, yPos + 8, x2 + 18, yPos + 8);
            g2.drawRect(x2 + 11, yPos + 10, 10, 12); // Thân
            g2.drawLine(x2 + 14, yPos + 13, x2 + 14, yPos + 19); // Sọc 1
            g2.drawLine(x2 + 18, yPos + 13, x2 + 18, yPos + 19); // Sọc 2

            g2.dispose();
        }
        @Override public int getIconWidth() { return 72; }
        @Override public int getIconHeight() { return 64; }
    }

    private static class RoomButton extends JButton {
        private final Color backgroundColor;
        RoomButton(String text, Color backgroundColor, Color foregroundColor) {
            super(text);
            this.backgroundColor = backgroundColor;
            setForeground(foregroundColor);
            setContentAreaFilled(false);
            setOpaque(false);
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(backgroundColor);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class LineIcon implements Icon {
        private final String type;
        private final Color color;
        private final int width;
        private final int height;
        LineIcon(String type, Color color, int width, int height) { this.type = type; this.color = color; this.width = width; this.height = height; }
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(color);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int cx = x + width / 2;
            int cy = y + height / 2;
            if ("plus".equals(type)) {
                g2.drawLine(cx, y + 2, cx, y + height - 2);
                g2.drawLine(x + 2, cy, x + width - 2, cy);
            }
            g2.dispose();
        }
        @Override public int getIconWidth() { return width; }
        @Override public int getIconHeight() { return height; }
    }

    private static class DotIcon implements Icon {
        private final Color color;
        DotIcon(Color color) { this.color = color; }
        @Override public void paintIcon(Component c, Graphics g, int x, int y) { 
            g.setColor(color); 
            g.fillOval(x, y + 4, 6, 6); 
        }
        @Override public int getIconWidth() { return 6; }
        @Override public int getIconHeight() { return 14; }
    }
}