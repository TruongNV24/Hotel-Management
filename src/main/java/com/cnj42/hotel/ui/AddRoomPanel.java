package com.cnj42.hotel.ui;

import com.cnj42.hotel.model.Room;
import com.cnj42.hotel.model.RoomType;
import com.cnj42.hotel.service.RoomService;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * "Thêm phòng" panel — matches the reference design:
 *  - Card 1: Thông tin phòng (room info form)
 *  - Card 2: Hình ảnh phòng (drag & drop image upload with thumbnails)
 *  - Card 3: Tiện nghi phòng (amenities checkbox grid)
 *  - Footer: Hủy bỏ / Lưu phòng
 */
public class AddRoomPanel extends JPanel {

    // ---- Palette (kept consistent with RoomManagementPanel) ----
    private static final Color PRIMARY = new Color(105, 78, 210);
    private static final Color PRIMARY_LIGHT = new Color(239, 235, 255);
    private static final Color BACKGROUND = new Color(246, 247, 251);
    private static final Color TEXT_DARK = new Color(35, 40, 52);
    private static final Color TEXT_GRAY = new Color(120, 125, 140);
    private static final Color TEXT_PLACEHOLDER = new Color(160, 164, 178);
    private static final Color BORDER_GRAY = new Color(222, 225, 233);
    private static final Color WHITE = Color.WHITE;
    private static final Color RED = new Color(235, 75, 75);
    private static final Color GREEN = new Color(35, 181, 118);

    private static final Font FONT_REGULAR = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_LABEL = new Font("Segoe UI", Font.BOLD, 12);
    private static final Font FONT_SECTION = new Font("Segoe UI", Font.BOLD, 15);
    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 20);

    private final RoomService roomService;
    private final Room editingRoom;
    private final Runnable onSaved;

    // Form fields
    private JTextField roomNumberField;
    private JComboBox<RoomType> roomTypeCombo;
    private JComboBox<Integer> floorCombo;
    private JTextField priceField;
    private JComboBox<Integer> capacityCombo;
    private JComboBox<String> statusCombo;
    private JTextArea noteArea;
    private JLabel noteCounter;

    // Images
    private JPanel thumbnailsPanel;
    private final List<File> selectedImages = new ArrayList<>();
    private static final Map<String, File> ROOM_IMAGES = new ConcurrentHashMap<>();
    private static final int MAX_IMAGES = 5;

    // Amenities
    private final java.util.Map<String, JCheckBox> amenityChecks = new java.util.LinkedHashMap<>();
    private JTextField otherAmenityField;

    public AddRoomPanel() {
        this(null, null);
    }

    public AddRoomPanel(Room room, Runnable onSaved) {
        this.roomService = new RoomService();
        this.editingRoom = room;
        this.onSaved = onSaved;
        initUI();
        if (editingRoom != null) populateForm(editingRoom);
    }

    public static File getRoomImage(String roomNumber) {
        return ROOM_IMAGES.get(roomNumber);
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        add(createPageTitle(), BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(Box.createVerticalStrut(15));
        content.add(createTopRow());
        content.add(Box.createVerticalStrut(20));
        content.add(createAmenitiesCard());
        content.add(Box.createVerticalStrut(20));
        content.add(createFooterPanel());

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(BACKGROUND);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setOpaque(false);

        add(scrollPane, BorderLayout.CENTER);
    }

    // ==================== PAGE TITLE ====================

    private JPanel createPageTitle() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel(editingRoom == null ? "Thêm phòng" : "Sửa phòng");
        title.setFont(FONT_TITLE);
        title.setForeground(TEXT_DARK);

        JLabel breadcrumb = new JLabel("Dashboard  >  Quản lý phòng  >  Thêm phòng");
        breadcrumb.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        breadcrumb.setForeground(TEXT_GRAY);
        breadcrumb.setBorder(new EmptyBorder(4, 0, 0, 0));

        panel.add(title);
        panel.add(breadcrumb);
        return panel;
    }

    // ==================== TOP ROW: INFO + IMAGES ====================

    private JPanel createTopRow() {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 460));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;

        gbc.gridx = 0;
        gbc.weightx = 0.62;
        gbc.insets = new Insets(0, 0, 0, 10);
        row.add(createRoomInfoCard(), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.38;
        gbc.insets = new Insets(0, 10, 0, 0);
        row.add(createImageUploadCard(), gbc);

        return row;
    }

    // ==================== CARD: THÔNG TIN PHÒNG ====================

    private JPanel createRoomInfoCard() {
        JPanel card = card();

        JLabel header = sectionHeader("building", "Thông tin phòng");
        card.add(header);
        card.add(Box.createVerticalStrut(18));

        // Row 1: Số phòng | Loại phòng
        roomNumberField = placeholderField("Ví dụ: 101");
        roomTypeCombo = new JComboBox<>();
        roomTypeCombo.setRenderer(new DefaultComboBoxRendererWithHint("Chọn loại phòng"));
        loadRoomTypes();
        card.add(fieldRow(
                labeledField("Số phòng", true, roomNumberField),
                labeledField("Loại phòng", true, styled(roomTypeCombo))
        ));
        card.add(Box.createVerticalStrut(14));

        // Row 2: Tầng | Giá / đêm
        floorCombo = new JComboBox<>();
        for (int i = 1; i <= 20; i++) floorCombo.addItem(i);
        floorCombo.setSelectedItem(null);
        priceField = placeholderField("Ví dụ: 500000");
        card.add(fieldRow(
                labeledField("Tầng", true, styled(floorCombo)),
                labeledField("Giá / đêm (VNĐ)", true, priceField)
        ));
        card.add(Box.createVerticalStrut(14));

        // Row 3: Sức chứa | Trạng thái
        capacityCombo = new JComboBox<>();
        for (int i = 1; i <= 8; i++) capacityCombo.addItem(i);
        capacityCombo.setSelectedItem(null);
        capacityCombo.setRenderer(new DefaultComboBoxRendererWithHint("Chọn số người"));

        statusCombo = new JComboBox<>(new String[]{"Trống", "Đang ở", "Đặt trước", "Dọn dẹp", "Bảo trì"});
        statusCombo.setRenderer(new StatusComboRenderer());

        card.add(fieldRow(
                labeledField("Sức chứa (người)", true, styled(capacityCombo)),
                labeledField("Trạng thái", true, styled(statusCombo))
        ));
        card.add(Box.createVerticalStrut(14));

        // Row 4: Ghi chú
        JLabel noteLabel = fieldLabel("Ghi chú", false);
        noteArea = new JTextArea(4, 10);
        noteArea.setLineWrap(true);
        noteArea.setWrapStyleWord(true);
        noteArea.setFont(FONT_REGULAR);
        noteArea.setBorder(new EmptyBorder(8, 10, 8, 10));
        addPlaceholder(noteArea, "Nhập ghi chú (nếu có)...");

        noteCounter = new JLabel("0/255");
        noteCounter.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        noteCounter.setForeground(TEXT_GRAY);
        noteArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateCounter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateCounter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateCounter(); }
            private void updateCounter() {
                int len = noteArea.getText().length();
                noteCounter.setText(Math.min(len, 255) + "/255");
            }
        });

        JPanel noteBox = new JPanel(new BorderLayout());
        noteBox.setOpaque(false);
        noteBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        JScrollPane noteScroll = new JScrollPane(noteArea);
        noteScroll.setBorder(new LineBorder(BORDER_GRAY, 1, true));
        noteBox.add(noteScroll, BorderLayout.CENTER);

        JPanel counterRow = new JPanel(new BorderLayout());
        counterRow.setOpaque(false);
        counterRow.add(noteCounter, BorderLayout.EAST);
        noteBox.add(counterRow, BorderLayout.SOUTH);

        JPanel notePanel = new JPanel();
        notePanel.setOpaque(false);
        notePanel.setLayout(new BoxLayout(notePanel, BoxLayout.Y_AXIS));
        notePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        noteLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        notePanel.add(noteLabel);
        notePanel.add(Box.createVerticalStrut(6));
        notePanel.add(noteBox);

        card.add(notePanel);
        card.add(Box.createVerticalGlue());

        return card;
    }

    private void loadRoomTypes() {
        List<RoomType> types = roomService.getAllRoomTypes();
        roomTypeCombo.removeAllItems();
        roomTypeCombo.setSelectedItem(null);
        for (RoomType type : types) {
            roomTypeCombo.addItem(type);
        }
    }

    // ==================== CARD: HÌNH ẢNH PHÒNG ====================

    private JPanel createImageUploadCard() {
        JPanel card = card();

        card.add(sectionHeader("image", "Hình ảnh phòng"));
        card.add(Box.createVerticalStrut(18));

        JPanel dropZone = new JPanel();
        dropZone.setLayout(new BoxLayout(dropZone, BoxLayout.Y_AXIS));
        dropZone.setBackground(new Color(250, 250, 253));
        dropZone.setBorder(new DashedBorder(PRIMARY_LIGHT.darker(), 2, 10));
        dropZone.setAlignmentX(Component.LEFT_ALIGNMENT);
        dropZone.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));
        dropZone.setPreferredSize(new Dimension(300, 160));

        JLabel cloudIcon = new JLabel(new LineIcon("upload", PRIMARY, 36, 36));
        cloudIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel dropLabel = new JLabel("Kéo thả hình ảnh vào đây");
        dropLabel.setFont(FONT_LABEL);
        dropLabel.setForeground(TEXT_DARK);
        dropLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel orLabel = new JLabel("hoặc");
        orLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        orLabel.setForeground(TEXT_GRAY);
        orLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton chooseBtn = new JButton("Chọn ảnh");
        chooseBtn.setFont(FONT_LABEL);
        chooseBtn.setForeground(PRIMARY);
        chooseBtn.setBackground(WHITE);
        chooseBtn.setBorder(new LineBorder(PRIMARY, 1, true));
        chooseBtn.setFocusPainted(false);
        chooseBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        chooseBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        chooseBtn.addActionListener(e -> chooseImages());

        dropZone.add(Box.createVerticalGlue());
        dropZone.add(cloudIcon);
        dropZone.add(Box.createVerticalStrut(8));
        dropZone.add(dropLabel);
        dropZone.add(Box.createVerticalStrut(4));
        dropZone.add(orLabel);
        dropZone.add(Box.createVerticalStrut(8));
        dropZone.add(chooseBtn);
        dropZone.add(Box.createVerticalGlue());

        card.add(dropZone);
        card.add(Box.createVerticalStrut(10));

        JLabel hint = new JLabel("Chọn tối đa 5 ảnh. Định dạng: JPG, PNG (Tối đa 5MB/ảnh)");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        hint.setForeground(TEXT_GRAY);
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(hint);
        card.add(Box.createVerticalStrut(10));

        thumbnailsPanel = new JPanel(new GridLayout(0, 4, 8, 8));
        thumbnailsPanel.setOpaque(false);
        thumbnailsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        refreshThumbnails();

        card.add(thumbnailsPanel);
        card.add(Box.createVerticalGlue());

        return card;
    }

    private void chooseImages() {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileFilter(new FileNameExtensionFilter("Ảnh (JPG, PNG)", "jpg", "jpeg", "png"));
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            for (File f : chooser.getSelectedFiles()) {
                if (selectedImages.size() >= MAX_IMAGES) {
                    JOptionPane.showMessageDialog(this, "Chỉ được chọn tối đa " + MAX_IMAGES + " ảnh.",
                            "Hotel Management", JOptionPane.WARNING_MESSAGE);
                    break;
                }
                selectedImages.add(f);
            }
            refreshThumbnails();
        }
    }

    private void refreshThumbnails() {
        thumbnailsPanel.removeAll();

        for (File file : selectedImages) {
            thumbnailsPanel.add(createThumbnail(file));
        }

        if (selectedImages.size() < MAX_IMAGES) {
            thumbnailsPanel.add(createAddMoreTile());
        }

        thumbnailsPanel.revalidate();
        thumbnailsPanel.repaint();
    }

    private JLayeredPane createThumbnail(File file) {
        JLayeredPane layered = new JLayeredPane();
        layered.setPreferredSize(new Dimension(120, 90));

        JLabel imageLabel = new JLabel();
        imageLabel.setOpaque(true);
        imageLabel.setBackground(new Color(230, 230, 235));
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setBorder(new LineBorder(BORDER_GRAY, 1, true));
        imageLabel.setBounds(0, 0, 120, 90);
        try {
            ImageIcon icon = new ImageIcon(file.getAbsolutePath());
            Image scaled = icon.getImage().getScaledInstance(120, 90, Image.SCALE_SMOOTH);
            imageLabel.setIcon(new ImageIcon(scaled));
        } catch (Exception ex) {
            imageLabel.setText(file.getName());
            imageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        }

        JButton removeBtn = new JButton("\u2715");
        removeBtn.setFont(new Font("Segoe UI", Font.BOLD, 9));
        removeBtn.setForeground(WHITE);
        removeBtn.setBackground(RED);
        removeBtn.setBorder(BorderFactory.createEmptyBorder());
        removeBtn.setFocusPainted(false);
        removeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        removeBtn.setBounds(100, -6, 22, 22);
        removeBtn.addActionListener(e -> {
            selectedImages.remove(file);
            refreshThumbnails();
        });

        layered.add(imageLabel, Integer.valueOf(0));
        layered.add(removeBtn, Integer.valueOf(1));
        return layered;
    }

    private JPanel createAddMoreTile() {
        JPanel tile = new JPanel();
        tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
        tile.setPreferredSize(new Dimension(120, 90));
        tile.setBackground(WHITE);
        tile.setBorder(new DashedBorder(BORDER_GRAY, 1, 8));
        tile.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel plus = new JLabel("+");
        plus.setFont(new Font("Segoe UI", Font.BOLD, 18));
        plus.setForeground(PRIMARY);
        plus.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel text = new JLabel("Thêm ảnh");
        text.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        text.setForeground(PRIMARY);
        text.setAlignmentX(Component.CENTER_ALIGNMENT);

        tile.add(Box.createVerticalGlue());
        tile.add(plus);
        tile.add(text);
        tile.add(Box.createVerticalGlue());

        tile.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { chooseImages(); }
        });
        return tile;
    }

    // ==================== CARD: TIỆN NGHI PHÒNG ====================

    private JPanel createAmenitiesCard() {
        JPanel card = card();
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(sectionHeader("star", "Tiện nghi phòng"));
        card.add(Box.createVerticalStrut(18));

        String[][] amenities = {
            {"Wi-Fi", "wifi"},
            {"Điều hòa", "snowflake"},
            {"TV", "tv"},
            {"Tủ lạnh", "fridge"},
            {"Bình nóng lạnh", "heater"},
            {"Bồn tắm", "bath"},
            {"Ban công", "balcony"},
            {"Két sắt", "safe"},
            {"Máy sấy tóc", "dryer"},
            {"Bàn làm việc", "desk"},
            {"Ghế sofa", "sofa"},
            {"Khác", "plus"}
        };

        JPanel grid = new JPanel(new GridLayout(0, 4, 20, 16));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);

        for (String[] amenity : amenities) {
            String name = amenity[0];
            String icon = amenity[1];

            if (name.equals("Khác")) {
                grid.add(createOtherAmenityCell(icon));
            } else {
                grid.add(createAmenityCell(name, icon));
            }
        }

        card.add(grid);
        card.add(Box.createVerticalStrut(4));
        return card;
    }

    private JPanel createAmenityCell(String name, String icon) {
        JPanel cell = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        cell.setOpaque(false);

        JCheckBox checkBox = new JCheckBox();
        checkBox.setOpaque(false);
        amenityChecks.put(name, checkBox);

        JLabel iconLabel = new JLabel(new LineIcon(icon, TEXT_GRAY, 16, 16));

        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(FONT_REGULAR);
        nameLabel.setForeground(TEXT_DARK);

        cell.add(checkBox);
        cell.add(iconLabel);
        cell.add(nameLabel);
        return cell;
    }

    private JPanel createOtherAmenityCell(String icon) {
        JPanel cell = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        cell.setOpaque(false);

        JCheckBox checkBox = new JCheckBox();
        checkBox.setOpaque(false);
        amenityChecks.put("Khác", checkBox);

        JLabel nameLabel = new JLabel("Khác");
        nameLabel.setFont(FONT_REGULAR);
        nameLabel.setForeground(TEXT_DARK);

        otherAmenityField = placeholderField("Nhập tiện nghi khác...");
        otherAmenityField.setPreferredSize(new Dimension(150, 30));

        cell.add(checkBox);
        cell.add(new JLabel(new LineIcon(icon, TEXT_GRAY, 16, 16)));
        cell.add(nameLabel);
        cell.add(otherAmenityField);
        return cell;
    }

    // ==================== FOOTER ====================

    private JPanel createFooterPanel() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setOpaque(false);
        footer.setAlignmentX(Component.LEFT_ALIGNMENT);
        footer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        JButton cancelBtn = new RoundedButton("Hủy bỏ", WHITE, TEXT_DARK, BORDER_GRAY);
        cancelBtn.setPreferredSize(new Dimension(120, 38));
        cancelBtn.setFont(FONT_LABEL);
        cancelBtn.setForeground(TEXT_DARK);
        cancelBtn.setFocusPainted(false);
        cancelBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cancelBtn.addActionListener(e -> resetForm());

        JButton saveBtn = new RoundedButton(editingRoom == null ? "Lưu phòng" : "Lưu thay đổi", PRIMARY, WHITE, PRIMARY);
        saveBtn.setPreferredSize(new Dimension(140, 38));
        saveBtn.setFont(FONT_LABEL);
        saveBtn.setFocusPainted(false);
        saveBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        saveBtn.addActionListener(e -> saveRoom());

        footer.add(cancelBtn);
        footer.add(saveBtn);
        return footer;
    }

    private void resetForm() {
        roomNumberField.setText("");
        addPlaceholder(roomNumberField, "Ví dụ: 101");
        priceField.setText("");
        addPlaceholder(priceField, "Ví dụ: 500000");
        roomTypeCombo.setSelectedItem(null);
        floorCombo.setSelectedItem(null);
        capacityCombo.setSelectedItem(null);
        statusCombo.setSelectedIndex(0);
        noteArea.setText("");
        addPlaceholder(noteArea, "Nhập ghi chú (nếu có)...");
        selectedImages.clear();
        refreshThumbnails();
        for (JCheckBox cb : amenityChecks.values()) cb.setSelected(false);
        if (otherAmenityField != null) {
            otherAmenityField.setText("");
            addPlaceholder(otherAmenityField, "Nhập tiện nghi khác...");
        }
    }

    private void saveRoom() {
        String roomNumber = getRealText(roomNumberField, "Ví dụ: 101");
        RoomType type = (RoomType) roomTypeCombo.getSelectedItem();
        Integer floor = (Integer) floorCombo.getSelectedItem();
        String price = getRealText(priceField, "Ví dụ: 500000");
        Integer capacity = (Integer) capacityCombo.getSelectedItem();
        String status = (String) statusCombo.getSelectedItem();

        if (roomNumber.isEmpty() || type == null || floor == null || price.isEmpty() || capacity == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng điền đầy đủ các trường bắt buộc (*).",
                    "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Room room = editingRoom == null ? new Room() : editingRoom;
        room.setRoomNumber(roomNumber);
        room.setRoomTypeId(type.getRoomTypeId());
        room.setFloor(floor);
        room.setStatus(mapStatusToEnglish(status));
        room.setNote(getRealText(noteArea, "Nhập ghi chú (nếu có)..."));
        room.setAmenities(collectAmenitiesJson());

        try {
            if (!selectedImages.isEmpty()) room.setImagePath(copyRoomImages(roomNumber));
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Không thể lưu ảnh phòng: " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        boolean saved = editingRoom == null ? roomService.createRoom(room) : roomService.updateRoom(room);
        if (saved) {
            if (!selectedImages.isEmpty()) {
                ROOM_IMAGES.put(roomNumber, selectedImages.get(0));
            }
            if (editingRoom == null) {
                JOptionPane.showMessageDialog(this, "Đã thêm phòng thành công", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                resetForm();
            } else if (onSaved != null) {
                onSaved.run();
            }
        } else {
            JOptionPane.showMessageDialog(this, "Lỗi khi thêm phòng", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String collectAmenitiesJson() {
        StringBuilder json = new StringBuilder("[");
        for (Map.Entry<String, JCheckBox> entry : amenityChecks.entrySet()) {
            if (!entry.getValue().isSelected()) continue;
            String value = entry.getKey();
            if ("Khác".equals(value)) {
                value = getRealText(otherAmenityField, "Nhập tiện nghi khác...");
                if (value.isEmpty()) continue;
            }
            if (json.length() > 1) json.append(',');
            json.append('"').append(escapeJson(value)).append('"');
        }
        return json.append(']').toString();
    }

    private void populateForm(Room room) {
        roomNumberField.setText(room.getRoomNumber());
        floorCombo.setSelectedItem(room.getFloor());
        statusCombo.setSelectedItem(mapStatusToVietnamese(room.getStatus()));
        noteArea.setText(room.getNote() == null ? "" : room.getNote());

        for (int i = 0; i < roomTypeCombo.getItemCount(); i++) {
            RoomType type = roomTypeCombo.getItemAt(i);
            if (type.getRoomTypeId() == room.getRoomTypeId()) {
                roomTypeCombo.setSelectedIndex(i);
                priceField.setText(String.valueOf((long) type.getPricePerNight()));
                capacityCombo.setSelectedItem(type.getCapacity());
                break;
            }
        }
        loadAmenities(room.getAmenities());
        loadExistingImages(room.getImagePath());
    }

    private void loadAmenities(String json) {
        if (json == null || json.isBlank()) return;
        for (String value : json.replace("[", "").replace("]", "").split(",")) {
            String amenity = value.trim().replaceAll("^\"|\"$", "").replace("\\\"", "\"").replace("\\\\", "\\");
            JCheckBox checkBox = amenityChecks.get(amenity);
            if (checkBox != null) {
                checkBox.setSelected(true);
            } else if (!amenity.isEmpty() && otherAmenityField != null) {
                amenityChecks.get("Khác").setSelected(true);
                otherAmenityField.setText(amenity);
            }
        }
    }

    private void loadExistingImages(String json) {
        if (json == null || json.isBlank()) return;
        for (String value : json.replace("[", "").replace("]", "").split(",")) {
            String path = value.trim().replaceAll("^\"|\"$", "").replace("\\\"", "\"").replace("\\\\", "\\");
            File file = new File(path);
            if (file.isFile()) selectedImages.add(file);
        }
        refreshThumbnails();
    }

    private String mapStatusToVietnamese(String status) {
        return switch (status) {
            case "AVAILABLE" -> "Trống";
            case "OCCUPIED" -> "Đang ở";
            case "RESERVED" -> "Đặt trước";
            case "CLEANING" -> "Dọn dẹp";
            case "MAINTENANCE" -> "Bảo trì";
            default -> status;
        };
    }

    private String copyRoomImages(String roomNumber) throws IOException {
        if (selectedImages.isEmpty()) return null;

        Path targetDirectory = Paths.get("uploads", "rooms", roomNumber);
        Files.createDirectories(targetDirectory);
        StringBuilder json = new StringBuilder("[");
        int index = 1;
        for (File image : selectedImages) {
            String fileName = index++ + "-" + image.getName();
            Path target = targetDirectory.resolve(fileName);
            Files.copy(image.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
            if (json.length() > 1) json.append(',');
            json.append('"').append(escapeJson(target.toString())).append('"');
        }
        return json.append(']').toString();
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String mapStatusToEnglish(String vn) {
        return switch (vn) {
            case "Trống" -> "AVAILABLE";
            case "Đang ở" -> "OCCUPIED";
            case "Đặt trước" -> "RESERVED";
            case "Dọn dẹp" -> "CLEANING";
            case "Bảo trì" -> "MAINTENANCE";
            default -> vn;
        };
    }

    // ==================== UI HELPERS ====================

    private JPanel card() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(WHITE);
        panel.setBorder(new CompoundBorder(
                new LineBorder(BORDER_GRAY, 1, true),
                new EmptyBorder(20, 20, 20, 20)
        ));
        return panel;
    }

    private JLabel sectionHeader(String icon, String text) {
        JLabel label = new JLabel(text, new LineIcon(icon, PRIMARY, 15, 15), SwingConstants.LEFT);
        label.setIconTextGap(8);
        label.setFont(FONT_SECTION);
        label.setForeground(PRIMARY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JLabel fieldLabel(String text, boolean required) {
        JLabel label = new JLabel(required ? text + " *" : text);
        label.setFont(FONT_LABEL);
        label.setForeground(TEXT_DARK);
        if (required) {
            label.setForeground(TEXT_DARK);
        }
        return label;
    }

    private JPanel labeledField(String labelText, boolean required, JComponent field) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = fieldLabel(labelText, required);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        panel.add(label);
        panel.add(Box.createVerticalStrut(6));
        panel.add(field);
        return panel;
    }

    private JPanel fieldRow(JPanel left, JPanel right) {
        JPanel row = new JPanel(new GridLayout(1, 2, 16, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        row.add(left);
        row.add(right);
        return row;
    }

    private JComponent styled(JComboBox<?> combo) {
        combo.setFont(FONT_REGULAR);
        combo.setBackground(WHITE);
        combo.setBorder(new LineBorder(BORDER_GRAY, 1, true));
        combo.setPreferredSize(new Dimension(0, 34));
        return combo;
    }

    private JTextField placeholderField(String placeholder) {
        JTextField field = new JTextField();
        field.setFont(FONT_REGULAR);
        field.setBorder(new CompoundBorder(new LineBorder(BORDER_GRAY, 1, true), new EmptyBorder(6, 10, 6, 10)));
        field.setPreferredSize(new Dimension(0, 34));
        addPlaceholder(field, placeholder);
        return field;
    }

    private void addPlaceholder(JTextField field, String placeholder) {
        field.setText(placeholder);
        field.setForeground(TEXT_PLACEHOLDER);
        for (var l : field.getFocusListeners()) field.removeFocusListener(l);
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(TEXT_DARK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(TEXT_PLACEHOLDER);
                }
            }
        });
    }

    private void addPlaceholder(JTextArea area, String placeholder) {
        area.setText(placeholder);
        area.setForeground(TEXT_PLACEHOLDER);
        for (var l : area.getFocusListeners()) area.removeFocusListener(l);
        area.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (area.getText().equals(placeholder)) {
                    area.setText("");
                    area.setForeground(TEXT_DARK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (area.getText().isEmpty()) {
                    area.setText(placeholder);
                    area.setForeground(TEXT_PLACEHOLDER);
                }
            }
        });
    }

    private String getRealText(JTextField field, String placeholder) {
        String text = field.getText().trim();
        return text.equals(placeholder) ? "" : text;
    }

    private String getRealText(JTextArea area, String placeholder) {
        String text = area.getText().trim();
        return text.equals(placeholder) ? "" : text;
    }

    // ==================== SUPPORT CLASSES ====================

    private static class ColorDotIcon implements Icon {
        private final Color color;
        private final int size;

        ColorDotIcon(Color color, int size) {
            this.color = color;
            this.size = size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(color);
            g.fillOval(x, y + 2, size, size);
        }

        @Override
        public int getIconWidth() { return size; }

        @Override
        public int getIconHeight() { return size + 4; }
    }

    private static class LineIcon implements Icon {
        private final String type;
        private final Color color;
        private final int width;
        private final int height;

        LineIcon(String type, Color color, int width, int height) {
            this.type = type;
            this.color = color;
            this.width = width;
            this.height = height;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int right = x + width - 2;
            int bottom = y + height - 2;
            int centerX = x + width / 2;
            int centerY = y + height / 2;

            switch (type) {
                case "building" -> {
                    g2.drawRect(x + 3, y + 2, width - 6, height - 4);
                    g2.drawLine(centerX, y + 2, centerX, bottom);
                    g2.drawLine(x + 3, y + 6, right, y + 6);
                    g2.drawLine(x + 3, y + 10, right, y + 10);
                }
                case "image" -> {
                    g2.drawRect(x + 2, y + 3, width - 4, height - 6);
                    g2.drawOval(x + 5, y + 5, 3, 3);
                    g2.drawLine(x + 4, bottom - 3, x + 8, y + 8);
                    g2.drawLine(x + 8, y + 8, right - 3, bottom - 3);
                }
                case "star" -> {
                    Polygon star = new Polygon();
                    for (int i = 0; i < 10; i++) {
                        double angle = -Math.PI / 2 + i * Math.PI / 5;
                        int radius = i % 2 == 0 ? width / 2 - 1 : width / 4;
                        star.addPoint(centerX + (int) (Math.cos(angle) * radius),
                                centerY + (int) (Math.sin(angle) * radius));
                    }
                    g2.fillPolygon(star);
                }
                case "upload" -> {
                    g2.drawArc(x + 3, y + 8, width - 6, height - 10, 20, 140);
                    g2.drawLine(centerX, y + 4, centerX, y + 21);
                    g2.drawLine(centerX, y + 4, centerX - 5, y + 9);
                    g2.drawLine(centerX, y + 4, centerX + 5, y + 9);
                }
                case "wifi" -> {
                    g2.drawArc(x + 2, y + 2, width - 4, height + 5, 35, 110);
                    g2.drawArc(x + 5, y + 6, width - 10, height - 1, 35, 110);
                    g2.fillOval(centerX - 1, bottom - 2, 3, 3);
                }
                case "snowflake" -> {
                    g2.drawLine(centerX, y + 2, centerX, bottom);
                    g2.drawLine(x + 2, centerY, right, centerY);
                    g2.drawLine(x + 4, y + 4, right - 4, bottom - 4);
                    g2.drawLine(right - 4, y + 4, x + 4, bottom - 4);
                }
                case "tv" -> {
                    g2.drawRoundRect(x + 2, y + 3, width - 4, height - 7, 2, 2);
                    g2.drawLine(centerX, bottom - 3, centerX, bottom);
                    g2.drawLine(x + 5, bottom, right - 5, bottom);
                }
                case "fridge" -> {
                    g2.drawRoundRect(x + 4, y + 1, width - 8, height - 2, 2, 2);
                    g2.drawLine(x + 4, centerY, right - 1, centerY);
                    g2.drawLine(right - 6, y + 4, right - 6, y + 7);
                }
                case "heater" -> {
                    g2.drawRoundRect(x + 4, y + 3, width - 8, height - 5, 2, 2);
                    g2.drawLine(x + 7, y + 6, x + 7, bottom - 3);
                    g2.drawLine(centerX, y + 6, centerX, bottom - 3);
                }
                case "bath" -> g2.drawArc(x + 2, y + 5, width - 4, height - 6, 180, 180);
                case "balcony" -> {
                    g2.drawRect(x + 2, y + 3, width - 4, height - 5);
                    g2.drawLine(x + 2, centerY, right, centerY);
                    g2.drawLine(x + 6, centerY, x + 6, bottom);
                    g2.drawLine(right - 6, centerY, right - 6, bottom);
                }
                case "safe" -> {
                    g2.drawRoundRect(x + 2, y + 2, width - 4, height - 4, 2, 2);
                    g2.drawOval(centerX - 3, centerY - 3, 6, 6);
                }
                case "dryer" -> {
                    g2.drawOval(x + 2, y + 3, width - 5, height - 7);
                    g2.drawLine(right - 2, y + 7, right, y + 4);
                }
                case "desk" -> {
                    g2.drawLine(x + 2, y + 5, right, y + 5);
                    g2.drawLine(x + 5, y + 5, x + 5, bottom);
                    g2.drawLine(right - 4, y + 5, right - 4, bottom);
                }
                case "sofa" -> {
                    g2.drawRoundRect(x + 2, y + 5, width - 4, height - 6, 2, 2);
                    g2.drawLine(x + 5, y + 5, x + 5, y + 2);
                    g2.drawLine(right - 5, y + 5, right - 5, y + 2);
                }
                case "plus" -> {
                    g2.drawOval(x + 2, y + 2, width - 4, height - 4);
                    g2.drawLine(centerX, y + 5, centerX, bottom - 3);
                    g2.drawLine(x + 5, centerY, right - 3, centerY);
                }
                default -> g2.drawRect(x + 3, y + 3, width - 6, height - 6);
            }
            g2.dispose();
        }

        @Override
        public int getIconWidth() { return width; }

        @Override
        public int getIconHeight() { return height; }
    }

    private static class RoundedButton extends JButton {
        private final Color fillColor;
        private final Color borderColor;

        RoundedButton(String text, Color fillColor, Color textColor, Color borderColor) {
            super(text);
            this.fillColor = fillColor;
            this.borderColor = borderColor;
            setForeground(textColor);
            setContentAreaFilled(false);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fillColor);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
            g2.setColor(borderColor);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Dashed rounded border, similar to the drag & drop upload zone in the design. */
    private static class DashedBorder implements Border {
        private final Color color;
        private final float thickness;
        private final int radius;

        DashedBorder(Color color, float thickness, int radius) {
            this.color = color;
            this.thickness = thickness;
            this.radius = radius;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    0, new float[]{6, 4}, 0));
            g2.drawRoundRect(x + 1, y + 1, width - 3, height - 3, radius, radius);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(6, 6, 6, 6);
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }

    /** Shows a gray hint text when nothing is selected in a combo box. */
    private static class DefaultComboBoxRendererWithHint extends DefaultListCellRenderer {
        private final String hint;

        DefaultComboBoxRendererWithHint(String hint) {
            this.hint = hint;
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                        boolean isSelected, boolean cellHasFocus) {
            Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value == null && index == -1) {
                setText(hint);
                setForeground(TEXT_PLACEHOLDER);
            } else if (value instanceof RoomType) {
                setText(((RoomType) value).getTypeName());
                setForeground(isSelected ? Color.WHITE : TEXT_DARK);
            }
            return c;
        }
    }

    /** Renders each status option with a colored dot, matching the "🟢 Trống" style. */
    private static class StatusComboRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                        boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            String status = value == null ? "" : value.toString();
            Color dot = switch (status) {
                case "Trống" -> GREEN;
                case "Đang ở" -> RED;
                case "Đặt trước" -> new Color(245, 153, 55);
                case "Dọn dẹp" -> new Color(65, 135, 235);
                case "Bảo trì" -> TEXT_GRAY;
                default -> TEXT_GRAY;
            };
            label.setText(status);
            label.setIcon(status.isEmpty() ? null : new ColorDotIcon(dot, 8));
            label.setIconTextGap(8);
            label.setForeground(isSelected ? Color.WHITE : TEXT_DARK);
            return label;
        }
    }
}