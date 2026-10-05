package com.cnj42.hotel.ui;

import com.cnj42.hotel.model.CheckoutSummary;
import com.cnj42.hotel.model.Service;
import com.cnj42.hotel.model.ServiceUsage;
import com.cnj42.hotel.model.Stay;
import com.cnj42.hotel.model.StayDetail;
import com.cnj42.hotel.service.ReservationService;
import com.cnj42.hotel.service.ServiceService;
import com.cnj42.hotel.service.ServiceUsageService;
import com.cnj42.hotel.service.StayService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

public class StayManagementPanel extends JPanel {

    private static final Color PRIMARY = new Color(24, 119, 135);
    private static final Color PRIMARY_LIGHT = new Color(231, 245, 246);
    private static final Color BACKGROUND = new Color(244, 248, 248);
    private static final Color CARD = new Color(255, 255, 255);
    private static final Color BORDER = new Color(225, 233, 235);
    private static final Color TEXT_DARK = new Color(30, 53, 61);
    private static final Color TEXT_MUTED = new Color(105, 127, 132);

    private static final Color GREEN = new Color(58, 176, 116);
    private static final Color GREEN_BG = new Color(224, 244, 234);
    private static final Color BLUE = new Color(72, 126, 220);
    private static final Color BLUE_BG = new Color(227, 236, 255);
    private static final Color ORANGE = new Color(230, 143, 62);
    private static final Color ORANGE_BG = new Color(255, 240, 218);
    private static final Color RED = new Color(217, 95, 95);
    private static final Color RED_BG = new Color(255, 230, 230);
    private static final Color GRAY = new Color(120, 128, 145);
    private static final Color GRAY_BG = new Color(236, 238, 241);

    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DISPLAY_DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final int ACTION_COL = 9;

    private final StayService stayService = new StayService();
    private final ReservationService reservationService = new ReservationService();
    private final ServiceService serviceService = new ServiceService();
    private final ServiceUsageService serviceUsageService = new ServiceUsageService();
    private final Integer currentUserId;
    private final IntConsumer checkoutComplete;
    private DefaultTableModel tableModel;
    private JTable stayTable;
    private final JTextField searchField;
    private final JComboBox<String> statusFilterCombo;
    private final JTextField fromDateField;
    private final JTextField toDateField;
    private final JLabel waitingCheckinCount;
    private final JLabel inHouseCount;
    private final JLabel waitingCheckoutCount;
    private final JLabel checkedOutCount;

    public StayManagementPanel() {
        this(null, null);
    }

    public StayManagementPanel(Integer currentUserId) {
        this(currentUserId, null);
    }

    public StayManagementPanel(Integer currentUserId, IntConsumer checkoutComplete) {
        this.currentUserId = currentUserId;
        this.checkoutComplete = checkoutComplete;

        searchField = new JTextField();
        statusFilterCombo = new JComboBox<>(new String[]{
                "Tất cả",
                "Chờ check-in",
                "Đang ở",
                "Chờ check-out",
                "Đã check-out"
        });
        fromDateField = new JTextField();
        toDateField = new JTextField();
        waitingCheckinCount = new JLabel("0");
        inHouseCount = new JLabel("0");
        waitingCheckoutCount = new JLabel("0");
        checkedOutCount = new JLabel("0");

        setLayout(new BorderLayout(0, 18));
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(8, 14, 18, 14));

        JPanel contentCard = new JPanel(new BorderLayout(0, 14));
        contentCard.setOpaque(false);
        contentCard.add(buildStatsPanel(), BorderLayout.NORTH);
        contentCard.add(buildTablePanel(), BorderLayout.CENTER);
        add(contentCard, BorderLayout.CENTER);

        SwingUtilities.invokeLater(this::refreshStayData);
    }

    public static String getStatusLabel(String status) {
        if (status == null) {
            return "CHỜ CHECK-IN";
        }

        switch (status.trim().toUpperCase()) {
            case "PENDING":
            case "CONFIRMED":
            case "WAITING_CHECK_IN":
            case "WAITING_CHECKIN":
                return "CHỜ CHECK-IN";
            case "CHECKED_IN":
            case "IN_HOUSE":
                return "ĐANG Ở";
            case "CHECKOUT_PENDING":
            case "WAITING_CHECK_OUT":
            case "WAITING_CHECKOUT":
                return "CHỜ CHECK-OUT";
            case "CHECKED_OUT":
            case "COMPLETED":
                return "ĐÃ CHECK-OUT";
            default:
                return status.toUpperCase();
        }
    }

    private JPanel buildStatsPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 4, 14, 0));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(4, 0, 8, 0));

        panel.add(createStatCard("Đang chờ check-in", waitingCheckinCount, "Khách chưa nhận phòng", BLUE, new Color(235, 241, 255)));
        panel.add(createStatCard("Đang lưu trú", inHouseCount, "Khách đang ở", GREEN, new Color(230, 247, 237)));
        panel.add(createStatCard("Chờ check-out", waitingCheckoutCount, "Sắp hết hạn lưu trú", ORANGE, new Color(255, 245, 233)));
        panel.add(createStatCard("Đã check-out", checkedOutCount, "Khách đã hoàn tất", RED, new Color(255, 234, 236)));

        return panel;
    }

    private JPanel createStatCard(String title, JLabel valueLabel, String helperText, Color accent, Color softBackground) {
        JPanel outer = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        outer.setOpaque(false);
        outer.setBorder(new EmptyBorder(0, 0, 0, 0));

        JPanel card = new JPanel(new BorderLayout(12, 0));
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(16, 16, 16, 16));

        JPanel iconPanel = new JPanel(new GridBagLayout());
        iconPanel.setOpaque(false);
        JLabel icon = new JLabel("•");
        icon.setFont(new Font("Segoe UI", Font.BOLD, 22));
        icon.setForeground(accent);
        icon.setOpaque(true);
        icon.setBackground(softBackground);
        icon.setHorizontalAlignment(SwingConstants.CENTER);
        icon.setPreferredSize(new Dimension(38, 38));
        iconPanel.add(icon);
        card.add(iconPanel, BorderLayout.WEST);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        titleLabel.setForeground(TEXT_MUTED);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        valueLabel.setForeground(accent);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel helper = new JLabel(helperText);
        helper.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        helper.setForeground(TEXT_MUTED);
        helper.setAlignmentX(Component.LEFT_ALIGNMENT);
        helper.setHorizontalAlignment(SwingConstants.LEFT);

        center.add(titleLabel);
        center.add(Box.createVerticalStrut(4));
        center.add(valueLabel);
        center.add(Box.createVerticalStrut(3));
        center.add(helper);

        card.add(center, BorderLayout.CENTER);
        outer.add(card);
        return outer;
    }

    private JPanel buildTablePanel() {
        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                new EmptyBorder(14, 14, 14, 14)
        ));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.setOpaque(false);

        searchField.setPreferredSize(new Dimension(260, 36));
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                new EmptyBorder(0, 10, 0, 10)
        ));
        searchField.putClientProperty("JTextField.placeholderText", "Tìm kiếm khách, phòng, mã đặt phòng");
        searchField.addActionListener(e -> refreshStayData());

        JPanel searchWrapper = new JPanel(new BorderLayout());
        searchWrapper.setOpaque(false);
        searchWrapper.add(searchField, BorderLayout.CENTER);

        JLabel statusLabel = new JLabel("Trạng thái");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        statusLabel.setForeground(TEXT_DARK);

        statusFilterCombo.setPreferredSize(new Dimension(170, 36));
        statusFilterCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        statusFilterCombo.addActionListener(e -> refreshStayData());

        JLabel fromLabel = new JLabel("Từ");
        fromLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        fromLabel.setForeground(TEXT_DARK);
        fromDateField.setPreferredSize(new Dimension(120, 36));
        fromDateField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                new EmptyBorder(0, 10, 0, 10)
        ));
        fromDateField.putClientProperty("JTextField.placeholderText", "dd/MM/yyyy");

        JLabel toLabel = new JLabel("Đến");
        toLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        toLabel.setForeground(TEXT_DARK);
        toDateField.setPreferredSize(new Dimension(120, 36));
        toDateField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                new EmptyBorder(0, 10, 0, 10)
        ));
        toDateField.putClientProperty("JTextField.placeholderText", "dd/MM/yyyy");

        JButton createGuestButton = new JButton("+ Khách mới");
        styleToolbarButton(createGuestButton, PRIMARY, Color.WHITE);
        createGuestButton.setFocusPainted(false);
        createGuestButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        createGuestButton.setBorder(new EmptyBorder(8, 18, 8, 18));
        createGuestButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        createGuestButton.addActionListener(e -> openCreateGuestDialog());

        JButton createReservationButton = new JButton("+ Đặt phòng");
        styleToolbarButton(createReservationButton, PRIMARY, Color.WHITE);
        createReservationButton.setFocusPainted(false);
        createReservationButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        createReservationButton.setBorder(new EmptyBorder(8, 18, 8, 18));
        createReservationButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        createReservationButton.addActionListener(e -> openCreateReservationDialog());

        JButton refreshButton = new JButton("Làm mới");
        styleToolbarButton(refreshButton, new Color(239, 246, 247), PRIMARY);
        refreshButton.setFocusPainted(false);
        refreshButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refreshButton.setBorder(new EmptyBorder(8, 18, 8, 18));
        refreshButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        refreshButton.addActionListener(e -> refreshStayData());

        toolbar.add(searchWrapper);
        toolbar.add(statusLabel);
        toolbar.add(statusFilterCombo);
        toolbar.add(fromLabel);
        toolbar.add(fromDateField);
        toolbar.add(toLabel);
        toolbar.add(toDateField);
        toolbar.add(createGuestButton);
        toolbar.add(createReservationButton);
        toolbar.add(refreshButton);

        card.add(toolbar, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new Object[]{
                "STT", "Mã lưu trú", "Khách hàng", "Phòng",
                "Ngày check-in", "Ngày dự kiến check-out", "Ngày check-out",
                "Số khách", "Trạng thái", "Thao tác"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        stayTable = new JTable(tableModel);
        stayTable.setRowHeight(78);
        stayTable.setFillsViewportHeight(true);
        stayTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        stayTable.setSelectionBackground(new Color(225, 243, 245));
        stayTable.setSelectionForeground(TEXT_DARK);
        stayTable.setShowGrid(false);
        stayTable.setIntercellSpacing(new Dimension(0, 6));
        stayTable.setBackground(Color.WHITE);
        stayTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        stayTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        stayTable.getTableHeader().setBackground(Color.WHITE);
        stayTable.getTableHeader().setForeground(TEXT_DARK);
        stayTable.getTableHeader().setPreferredSize(new Dimension(0, 38));
        stayTable.getTableHeader().setReorderingAllowed(false);
        stayTable.setDefaultRenderer(Object.class, new StayStatusRenderer());
        stayTable.getColumnModel().getColumn(ACTION_COL).setCellRenderer(new StayActionRenderer());
        stayTable.getColumnModel().getColumn(ACTION_COL).setPreferredWidth(260);
        stayTable.getColumnModel().getColumn(ACTION_COL).setMinWidth(250);
        stayTable.getColumnModel().getColumn(ACTION_COL).setMaxWidth(280);

        stayTable.getColumnModel().getColumn(0).setPreferredWidth(55);
        stayTable.getColumnModel().getColumn(1).setPreferredWidth(130);
        stayTable.getColumnModel().getColumn(2).setPreferredWidth(180);
        stayTable.getColumnModel().getColumn(3).setPreferredWidth(110);
        stayTable.getColumnModel().getColumn(4).setPreferredWidth(140);
        stayTable.getColumnModel().getColumn(5).setPreferredWidth(170);
        stayTable.getColumnModel().getColumn(6).setPreferredWidth(150);
        stayTable.getColumnModel().getColumn(7).setPreferredWidth(70);
        stayTable.getColumnModel().getColumn(8).setPreferredWidth(140);
        stayTable.getColumnModel().getColumn(9).setPreferredWidth(220);

        stayTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                int row = stayTable.rowAtPoint(event.getPoint());
                int column = stayTable.columnAtPoint(event.getPoint());
                if (row < 0 || column != ACTION_COL || event.getClickCount() != 1) {
                    return;
                }

                Object value = stayTable.getValueAt(row, ACTION_COL);
                if (value instanceof StayRow stayRow) {
                    handleActionClick(stayRow, event.getX() - stayTable.getCellRect(row, ACTION_COL, true).x,
                            event.getY() - stayTable.getCellRect(row, ACTION_COL, true).y);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(stayTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(CARD);
        card.add(scrollPane, BorderLayout.CENTER);

        return card;
    }

    private void styleToolbarButton(JButton button, Color background, Color foreground) {
        button.setBackground(background);
        button.setForeground(foreground);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(false);
        button.setBorder(new EmptyBorder(8, 14, 8, 14));
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void handleActionClick(StayRow stayRow, int x, int y) {
        if (x < 8 || x > 252 || y < 4 || y > 80) {
            return;
        }

        int buttonColumn = x < 130 ? 0 : 1;
        int buttonRow = y < 42 ? 0 : 1;
        int buttonIndex = buttonRow * 2 + buttonColumn;

        if (buttonIndex == 0) {
            openStayDetailDialog(stayRow);
            return;
        }

        if ("WAITING_CHECK_IN".equals(stayRow.getStatusCode())) {
            if (buttonIndex == 1) {
                openUpdateReservationDialog(stayRow);
            } else if (buttonIndex == 2) {
                cancelReservation(stayRow);
            } else if (buttonIndex == 3) {
                openCheckInDialog(stayRow);
            }
        } else if ("IN_HOUSE".equals(stayRow.getStatusCode()) || "CHECKOUT_PENDING".equals(stayRow.getStatusCode())) {
            if (buttonIndex == 1) {
                openAddServiceDialog(stayRow);
            } else if (buttonIndex == 2) {
                openCheckoutDialog(stayRow);
            }
        } else if ("CHECKED_OUT".equals(stayRow.getStatusCode()) && buttonIndex == 1) {
            openInvoice(stayRow);
        }
    }

    private void openInvoice(StayRow stayRow) {
        if (stayRow == null || checkoutComplete == null) {
            return;
        }

        Integer invoiceId = stayService.findInvoiceIdByStay(stayRow.getStayId());
        if (invoiceId != null) {
            checkoutComplete.accept(invoiceId);
            return;
        }
        if (invoiceId == null) {
            JOptionPane.showMessageDialog(this, "Chưa tìm thấy hóa đơn cho lượt lưu trú này.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void refreshStayData() {
        loadStats();
        loadStayTable();
    }

    private void loadStats() {
        java.util.Map<String, Integer> stats = stayService.getStayStats();
        waitingCheckinCount.setText(String.valueOf(stats.getOrDefault("waiting_checkin", 0)));
        inHouseCount.setText(String.valueOf(stats.getOrDefault("in_house", 0)));
        waitingCheckoutCount.setText(String.valueOf(stats.getOrDefault("waiting_checkout", 0)));
        checkedOutCount.setText(String.valueOf(stats.getOrDefault("checked_out", 0)));
    }

    private void loadStayTable() {
        tableModel.setRowCount(0);
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim();
        String fromText = fromDateField.getText() == null ? "" : fromDateField.getText().trim();
        String toText = toDateField.getText() == null ? "" : toDateField.getText().trim();
        LocalDate fromDate = fromText.isEmpty() ? null : parseDate(fromText);
        LocalDate toDate = toText.isEmpty() ? null : parseDate(toText);
        List<Stay> stays = stayService.searchStays(
                keyword,
                normalizeStatusForQuery((String) statusFilterCombo.getSelectedItem()),
                fromDate,
                toDate
        );

        int rowIndex = 1;
        for (Stay stay : stays) {
            StayRow stayRow = new StayRow();
            stayRow.setReservationId(stay.getReservationId());
            stayRow.setReservationCode(stay.getReservationCode());
            stayRow.setGuestName(stay.getGuestName());
            stayRow.setPhone(stay.getPhone());
            stayRow.setRoomNumber(stay.getRoomNumber());
            stayRow.setRoomId(stay.getRoomId());
            stayRow.setStayId(stay.getStayId());
            stayRow.setCheckInDate(stay.getCheckInDate());
            stayRow.setExpectedCheckOutDate(stay.getExpectedCheckOutDate());
            stayRow.setActualCheckOutDate(stay.getActualCheckOut());
            stayRow.setNumberOfGuests(stay.getNumberOfGuests());
            stayRow.setStatusCode(stay.getStatusCode());
            stayRow.setActualCheckIn(stay.getActualCheckIn());

            String checkinText = stayRow.getActualCheckIn() == null ? formatLocalDate(stayRow.getCheckInDate()) : formatDateTime(stayRow.getActualCheckIn());
            String expectedCheckoutText = formatLocalDate(stayRow.getExpectedCheckOutDate());
            String actualCheckoutText = stayRow.getActualCheckOutDate() == null ? "-" : formatDateTime(stayRow.getActualCheckOutDate());

            tableModel.addRow(new Object[]{
                    rowIndex++,
                    stayRow.getReservationCode(),
                    stayRow.getGuestName(),
                    stayRow.getRoomNumber(),
                    checkinText,
                    expectedCheckoutText,
                    actualCheckoutText,
                    stayRow.getNumberOfGuests(),
                    getStatusLabel(stayRow.getStatusCode()),
                    stayRow
            });
        }
    }

    private String normalizeStatusForQuery(String selectedItem) {
        if (selectedItem == null || "Tất cả".equals(selectedItem)) {
            return null;
        }
        switch (selectedItem) {
            case "Chờ check-in":
                return "WAITING_CHECK_IN";
            case "Đang ở":
                return "IN_HOUSE";
            case "Chờ check-out":
                return "CHECKOUT_PENDING";
            case "Đã check-out":
                return "CHECKED_OUT";
            default:
                return null;
        }
    }

    private LocalDate parseDate(String dateText) {
        String trimmed = dateText.trim();
        if (trimmed.isEmpty()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(trimmed, DISPLAY_DATE);
        } catch (Exception ex) {
            return LocalDate.parse(trimmed);
        }
    }

    private String formatLocalDate(LocalDate date) {
        return date == null ? "-" : date.format(DISPLAY_DATE);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "-" : dateTime.format(DISPLAY_DATE_TIME);
    }

    private int nvl(int value) {
        return value < 0 ? 0 : value;
    }

    private void openCreateGuestDialog() {
        GuestDialog guestDialog = new GuestDialog(SwingUtilities.getWindowAncestor(this));
        guestDialog.setVisible(true);
        if (guestDialog.isSaved()) {
            JOptionPane.showMessageDialog(this, "Tạo khách hàng thành công.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            refreshStayData();
        }
    }

    private void openCreateReservationDialog() {
        ReservationDialog reservationDialog = new ReservationDialog(SwingUtilities.getWindowAncestor(this), null, currentUserId);
        reservationDialog.setVisible(true);
        if (reservationDialog.isSaved()) {
            JOptionPane.showMessageDialog(this, "Tạo đặt phòng thành công.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            refreshStayData();
        }
    }

    private void openUpdateReservationDialog(StayRow stayRow) {
        if (stayRow == null) {
            return;
        }
        ReservationDialog reservationDialog = new ReservationDialog(SwingUtilities.getWindowAncestor(this), stayRow.getReservationId(), currentUserId);
        reservationDialog.setVisible(true);
        if (reservationDialog.isSaved()) {
            JOptionPane.showMessageDialog(this, "Cập nhật đặt phòng thành công.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            refreshStayData();
        }
    }

    private void cancelReservation(StayRow stayRow) {
        if (stayRow == null) {
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có muốn hủy đặt phòng này không?",
                "Xác nhận hủy",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        boolean ok = reservationService.cancelReservation(stayRow.getReservationId(), stayRow.getRoomId());
        if (ok) {
            JOptionPane.showMessageDialog(this, "Hủy đặt phòng thành công.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            refreshStayData();
        } else {
            JOptionPane.showMessageDialog(this, "Hủy đặt phòng thất bại hoặc trạng thái không hợp lệ.", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openCheckInDialog(StayRow stayRow) {
        if (stayRow == null) {
            return;
        }

        String guestName = stayRow.getGuestName();
        String roomNumber = stayRow.getRoomNumber();
        String checkInDate = formatDateTime(stayRow.getActualCheckIn() == null ? LocalDateTime.now() : stayRow.getActualCheckIn());
        String expectedCheckout = stayRow.getExpectedCheckOutDate() == null ? "-" : stayRow.getExpectedCheckOutDate().format(DISPLAY_DATE);

        int choice = JOptionPane.showConfirmDialog(
                this,
                "<html><b>XÁC NHẬN CHECK-IN</b><br><br>" +
                        "Khách hàng:<br>" + guestName + "<br><br>" +
                        "Phòng:<br>" + roomNumber + "<br><br>" +
                        "Ngày check-in:<br>" + checkInDate + "<br><br>" +
                        "Ngày dự kiến check-out:<br>" + expectedCheckout + "<br><br>" +
                        "Số khách:<br>" + stayRow.getNumberOfGuests() + "</html>",
                "Xác nhận check-in",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        boolean success = performCheckIn(stayRow.getReservationId(), stayRow.getRoomId());
        if (success) {
            JOptionPane.showMessageDialog(this, "Check-in thành công.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            refreshStayData();
        } else {
            JOptionPane.showMessageDialog(this, "Check-in thất bại. Vui lòng kiểm tra lại dữ liệu hoặc trạng thái phòng.", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean performCheckIn(int reservationId, int roomId) {
        return stayService.checkInReservation(reservationId, roomId, currentUserId);
    }

    private void openCheckoutDialog(StayRow stayRow) {
        if (stayRow == null) {
            return;
        }

        CheckoutSummary summary = loadCheckoutSummary(stayRow.getReservationId(), stayRow.getRoomId(), stayRow.getStayId());
        if (summary == null) {
            JOptionPane.showMessageDialog(this, "Không thể tính toán chi phí cho lưu trú này.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "THANH TOÁN & CHECK-OUT", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setResizable(false);

        JPanel content = new JPanel();
        content.setLayout(new BorderLayout(16, 16));
        content.setBorder(new EmptyBorder(18, 18, 18, 18));
        content.setBackground(CARD);

        JPanel summaryPanel = new JPanel();
        summaryPanel.setLayout(new GridLayout(0, 2, 12, 8));
        summaryPanel.setOpaque(false);
        summaryPanel.add(new JLabel("Khách:"));
        summaryPanel.add(new JLabel(stayRow.getGuestName()));
        summaryPanel.add(new JLabel("Phòng:"));
        summaryPanel.add(new JLabel(stayRow.getRoomNumber()));
        summaryPanel.add(new JLabel("Tiền phòng:"));
        summaryPanel.add(new JLabel(formatCurrency(summary.getRoomAmount())));
        summaryPanel.add(new JLabel("Dịch vụ:"));
        summaryPanel.add(new JLabel(formatCurrency(summary.getServiceAmount())));
        summaryPanel.add(new JLabel("Tổng trước giảm:"));
        summaryPanel.add(new JLabel(formatCurrency(summary.getTotalAmount())));

        JLabel discountValue = new JLabel(formatCurrency(0));
        summaryPanel.add(new JLabel("Giảm giá:"));
        summaryPanel.add(discountValue);

        JLabel totalValue = new JLabel(formatCurrency(summary.getTotalAmount()));
        totalValue.setFont(totalValue.getFont().deriveFont(Font.BOLD));
        summaryPanel.add(new JLabel("Tổng thanh toán:"));
        summaryPanel.add(totalValue);

        JPanel discountPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        discountPanel.setOpaque(false);
        discountPanel.add(new JLabel("Mã giảm giá:"));
        JComboBox<DiscountOption> discountBox = new JComboBox<>(new DiscountOption[]{
            new DiscountOption("Không áp dụng", 0),
            new DiscountOption("WELCOME5 - Giảm 5%", 5),
            new DiscountOption("STAY10 - Giảm 10%", 10),
            new DiscountOption("VIP15 - Giảm 15%", 15)
        });
        discountBox.setPreferredSize(new Dimension(210, 32));
        discountPanel.add(Box.createHorizontalStrut(12));
        discountPanel.add(discountBox);

        JPanel methodPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        methodPanel.setOpaque(false);
        methodPanel.add(new JLabel("Phương thức thanh toán:"));
        JComboBox<String> paymentMethod = new JComboBox<>(new String[]{"CASH", "BANK_TRANSFER", "CARD"});
        paymentMethod.setPreferredSize(new Dimension(170, 32));
        methodPanel.add(Box.createHorizontalStrut(12));
        methodPanel.add(paymentMethod);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setOpaque(false);
        JButton cancel = new JButton("Hủy");
        JButton confirm = new JButton("Thanh toán & Check-out");
        confirm.setBackground(PRIMARY);
        confirm.setForeground(Color.WHITE);
        confirm.setFocusPainted(false);
        cancel.setFocusPainted(false);
        footer.add(cancel);
        footer.add(confirm);

        content.add(summaryPanel, BorderLayout.CENTER);
        JPanel paymentPanel = new JPanel();
        paymentPanel.setLayout(new BoxLayout(paymentPanel, BoxLayout.Y_AXIS));
        paymentPanel.setOpaque(false);
        paymentPanel.add(discountPanel);
        paymentPanel.add(Box.createVerticalStrut(8));
        paymentPanel.add(methodPanel);
        content.add(paymentPanel, BorderLayout.SOUTH);

        dialog.add(content, BorderLayout.CENTER);
        dialog.add(footer, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(this);

        cancel.addActionListener(e -> dialog.dispose());
        discountBox.addActionListener(e -> {
            DiscountOption option = (DiscountOption) discountBox.getSelectedItem();
            double discountAmount = summary.getTotalAmount() * (option == null ? 0 : option.percentage) / 100;
            discountValue.setText(formatCurrency(discountAmount));
            totalValue.setText(formatCurrency(summary.getTotalAmount() - discountAmount));
        });

        confirm.addActionListener(e -> {
            DiscountOption option = (DiscountOption) discountBox.getSelectedItem();
            double discountAmount = summary.getTotalAmount() * (option == null ? 0 : option.percentage) / 100;
            int invoiceId = performCheckout(stayRow, summary, discountAmount, (String) paymentMethod.getSelectedItem());
            if (invoiceId > 0) {
                JOptionPane.showMessageDialog(this, "Thanh toán và check-out thành công.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
                refreshStayData();
                if (checkoutComplete != null) {
                    checkoutComplete.accept(invoiceId);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Không thể thanh toán hoặc check-out.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.setVisible(true);
    }

    private CheckoutSummary loadCheckoutSummary(int reservationId, int roomId, int stayId) {
        return stayService.getCurrentCheckoutSummary(stayId, roomId);
    }

    private void openAddServiceDialog(StayRow stayRow) {
        if (stayRow == null || stayRow.getStayId() <= 0) {
            JOptionPane.showMessageDialog(this, "Không tìm thấy lưu trú để thêm dịch vụ.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<Service> services = serviceService.search(null, "ACTIVE");
        if (services == null || services.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Hiện không có dịch vụ nào đang hoạt động để khách đặt thêm.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Thêm dịch vụ", true);
        dialog.setLayout(new BorderLayout(12, 12));
        dialog.setResizable(false);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(14, 14, 14, 14));
        panel.setBackground(CARD);

        JLabel titleLabel = new JLabel("Dịch vụ khách yêu cầu");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        titleLabel.setForeground(TEXT_DARK);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(10));

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(CARD);

        List<JCheckBox> checkBoxes = new ArrayList<>();
        List<JTextField> quantityFields = new ArrayList<>();

        for (Service service : services) {
            JPanel row = new JPanel(new BorderLayout(12, 8));
            row.setBackground(CARD);
            row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER, 1),
                    new EmptyBorder(8, 10, 8, 10)
            ));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));

            JCheckBox selectedBox = new JCheckBox();
            selectedBox.setOpaque(false);
            selectedBox.setFocusPainted(false);

            JPanel infoPanel = new JPanel(new BorderLayout(10, 0));
            infoPanel.setOpaque(false);

            JLabel nameLabel = new JLabel(service.getServiceName());
            nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
            nameLabel.setForeground(TEXT_DARK);

            JLabel priceLabel = new JLabel(formatCurrency(service.getPrice()));
            priceLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            priceLabel.setForeground(PRIMARY);

            JPanel textPanel = new JPanel();
            textPanel.setOpaque(false);
            textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
            textPanel.add(nameLabel);
            textPanel.add(Box.createVerticalStrut(2));
            textPanel.add(priceLabel);
            infoPanel.add(textPanel, BorderLayout.CENTER);

            JPanel quantityPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
            quantityPanel.setOpaque(false);
            JLabel qtyLabel = new JLabel("Số lượng:");
            qtyLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            qtyLabel.setForeground(TEXT_MUTED);

            JTextField quantityField = new JTextField("1");
            quantityField.setColumns(5);
            quantityField.setHorizontalAlignment(JTextField.CENTER);
            quantityField.setEnabled(false);

            selectedBox.addActionListener(e -> quantityField.setEnabled(selectedBox.isSelected()));

            quantityPanel.add(qtyLabel);
            quantityPanel.add(quantityField);
            infoPanel.add(quantityPanel, BorderLayout.EAST);

            row.add(selectedBox, BorderLayout.WEST);
            row.add(infoPanel, BorderLayout.CENTER);
            listPanel.add(row);
            listPanel.add(Box.createVerticalStrut(8));

            checkBoxes.add(selectedBox);
            quantityFields.add(quantityField);
        }

        JScrollPane scrollPane = new JScrollPane(listPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setPreferredSize(new Dimension(520, 260));
        panel.add(scrollPane);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setOpaque(false);
        JButton cancel = new JButton("Hủy");
        JButton confirm = new JButton("Lưu dịch vụ");
        confirm.setBackground(PRIMARY);
        confirm.setForeground(Color.WHITE);
        footer.add(cancel);
        footer.add(confirm);

        cancel.addActionListener(e -> dialog.dispose());
        confirm.addActionListener(e -> {
            boolean hasSelection = false;
            int successCount = 0;
            List<String> invalidItems = new ArrayList<>();

            for (int i = 0; i < services.size(); i++) {
                Service service = services.get(i);
                JCheckBox checkBox = checkBoxes.get(i);
                JTextField quantityField = quantityFields.get(i);

                if (!checkBox.isSelected()) {
                    continue;
                }

                hasSelection = true;

                int quantity;
                try {
                    quantity = Integer.parseInt(quantityField.getText().trim());
                    if (quantity <= 0) {
                        throw new NumberFormatException();
                    }
                } catch (NumberFormatException ex) {
                    invalidItems.add(service.getServiceName());
                    continue;
                }

                int usageId = serviceUsageService.create(stayRow.getStayId(), service.getServiceId(), quantity, null, currentUserId);
                if (usageId > 0) {
                    successCount++;
                }
            }

            if (!hasSelection) {
                JOptionPane.showMessageDialog(this, "Vui lòng tích chọn ít nhất một dịch vụ khách yêu cầu.", "Thiếu dữ liệu", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (!invalidItems.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Một số dịch vụ có số lượng không hợp lệ: " + String.join(", ", invalidItems) + ".",
                        "Lỗi dữ liệu",
                        JOptionPane.ERROR_MESSAGE);
            }

            if (successCount <= 0) {
                JOptionPane.showMessageDialog(this, "Không thể thêm dịch vụ cho lưu trú này.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            JOptionPane.showMessageDialog(this, "Thêm dịch vụ thành công.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            dialog.dispose();
            refreshStayData();
        });

        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(footer, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private int performCheckout(StayRow stayRow, CheckoutSummary summary, double discountAmount, String paymentMethod) {
        if (stayRow == null || stayRow.getStayId() <= 0) {
            return -1;
        }
        return stayService.checkoutAndPay(stayRow.getStayId(), stayRow.getReservationId(),
                stayRow.getRoomId(), stayRow.getRoomNumber(), summary, discountAmount,
                paymentMethod, currentUserId);
    }

    private static final class DiscountOption {
        private final String label;
        private final double percentage;

        private DiscountOption(String label, double percentage) {
            this.label = label;
            this.percentage = percentage;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private void openStayDetailDialog(StayRow stayRow) {
        if (stayRow == null) {
            return;
        }

        StayDetail detail = loadStayDetail(stayRow.getReservationId(), stayRow.getRoomId(), stayRow.getStayId());
        if (detail == null) {
            JOptionPane.showMessageDialog(this, "Không thể tải chi tiết lưu trú.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Chi tiết lưu trú", true);
        dialog.setLayout(new BorderLayout(16, 16));
        dialog.setSize(760, 560);
        dialog.setLocationRelativeTo(this);

        JPanel content = new JPanel(new BorderLayout(18, 18));
        content.setBorder(new EmptyBorder(18, 18, 18, 18));
        content.setBackground(CARD);

        JPanel infoPanel = new JPanel(new GridLayout(1, 2, 18, 0));
        infoPanel.setOpaque(false);

        JPanel guestPanel = new JPanel(new GridLayout(0, 2, 10, 8));
        guestPanel.setOpaque(false);
        guestPanel.setBorder(BorderFactory.createTitledBorder(new LineBorder(BORDER, 1), "THÔNG TIN KHÁCH"));
        guestPanel.add(new JLabel("Họ tên:"));
        guestPanel.add(new JLabel(detail.getGuestName()));
        guestPanel.add(new JLabel("Số điện thoại:"));
        guestPanel.add(new JLabel(detail.getPhone()));
        guestPanel.add(new JLabel("Email:"));
        guestPanel.add(new JLabel(detail.getEmail() == null || detail.getEmail().isBlank() ? "-" : detail.getEmail()));

        JPanel roomPanel = new JPanel(new GridLayout(0, 2, 10, 8));
        roomPanel.setOpaque(false);
        roomPanel.setBorder(BorderFactory.createTitledBorder(new LineBorder(BORDER, 1), "THÔNG TIN PHÒNG"));
        roomPanel.add(new JLabel("Số phòng:"));
        roomPanel.add(new JLabel(detail.getRoomNumber()));
        roomPanel.add(new JLabel("Loại phòng:"));
        roomPanel.add(new JLabel(detail.getRoomType()));
        roomPanel.add(new JLabel("Giá phòng:"));
        roomPanel.add(new JLabel(formatCurrency(detail.getRoomPrice())));

        infoPanel.add(guestPanel);
        infoPanel.add(roomPanel);

        JPanel stayPanel = new JPanel(new GridLayout(0, 2, 10, 8));
        stayPanel.setOpaque(false);
        stayPanel.setBorder(BorderFactory.createTitledBorder(new LineBorder(BORDER, 1), "THÔNG TIN LƯU TRÚ"));
        stayPanel.add(new JLabel("Mã lưu trú:"));
        stayPanel.add(new JLabel(detail.getReservationCode()));
        stayPanel.add(new JLabel("Check-in:"));
        stayPanel.add(new JLabel(detail.getActualCheckIn() == null ? "-" : detail.getActualCheckIn().format(DISPLAY_DATE_TIME)));
        stayPanel.add(new JLabel("Check-out dự kiến:"));
        stayPanel.add(new JLabel(detail.getExpectedCheckOut() == null ? "-" : detail.getExpectedCheckOut().format(DISPLAY_DATE)));
        stayPanel.add(new JLabel("Check-out thực tế:"));
        stayPanel.add(new JLabel(detail.getActualCheckOut() == null ? "-" : detail.getActualCheckOut().format(DISPLAY_DATE_TIME)));
        stayPanel.add(new JLabel("Số khách:"));
        stayPanel.add(new JLabel(String.valueOf(detail.getNumberOfGuests())));
        stayPanel.add(new JLabel("Trạng thái:"));
        stayPanel.add(new JLabel(getStatusLabel(detail.getStatusCode())));

        JPanel servicesPanel = new JPanel(new BorderLayout(8, 8));
        servicesPanel.setOpaque(false);
        servicesPanel.setBorder(BorderFactory.createTitledBorder(new LineBorder(BORDER, 1), "DỊCH VỤ ĐÃ SỬ DỤNG"));

        DefaultTableModel serviceModel = new DefaultTableModel(new Object[]{"Tên dịch vụ", "Số lượng", "Đơn giá", "Thành tiền"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (ServiceUsage usage : detail.getServiceUsages()) {
            serviceModel.addRow(new Object[]{
                    usage.getServiceName(),
                    usage.getQuantity(),
                    formatCurrency(usage.getUnitPrice()),
                    formatCurrency(usage.getTotalAmount())
            });
        }

        JTable serviceTable = new JTable(serviceModel);
        serviceTable.setRowHeight(38);
        serviceTable.setShowGrid(false);
        serviceTable.getTableHeader().setBackground(Color.WHITE);
        serviceTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        serviceTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        servicesPanel.add(new JScrollPane(serviceTable), BorderLayout.CENTER);

        JPanel top = new JPanel(new BorderLayout(18, 0));
        top.setOpaque(false);
        top.add(infoPanel, BorderLayout.CENTER);
        top.add(stayPanel, BorderLayout.EAST);

        content.add(top, BorderLayout.NORTH);
        content.add(servicesPanel, BorderLayout.CENTER);

        JButton close = new JButton("Đóng");
        close.addActionListener(e -> dialog.dispose());
        JPanel closePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        closePanel.setOpaque(false);
        closePanel.add(close);
        content.add(closePanel, BorderLayout.SOUTH);

        dialog.add(content, BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    private StayDetail loadStayDetail(int reservationId, int roomId, int stayId) {
        return stayService.getStayDetail(reservationId);
    }

    private static JButton createActionButton(String text, Color background, Color foreground, Color borderColor) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(true);
        button.setRolloverEnabled(true);
        button.setBackground(background);
        button.setForeground(foreground);
        button.setFont(new Font("Segoe UI", Font.BOLD, 11));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor, 1, true),
                new EmptyBorder(5, 10, 5, 10)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(90, 34));
        button.setMinimumSize(new Dimension(82, 34));
        button.setMaximumSize(new Dimension(120, 34));
        button.setMargin(new Insets(4, 8, 4, 8));
        return button;
    }

    private String formatCurrency(double value) {
        return String.format("%,.0f đ", value);
    }

    private static class StayStatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (c instanceof JLabel label) {
                String text = value == null ? "" : value.toString();
                label.setText(text);
                label.setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);
                label.setForeground(TEXT_DARK);
                label.setBorder(new EmptyBorder(4, 8, 4, 8));
                label.setHorizontalAlignment(SwingConstants.LEFT);
            }
            return c;
        }
    }

    private class StayActionRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JPanel panel = new JPanel(new GridLayout(2, 2, 6, 8));
            panel.setOpaque(true);
            panel.setBackground(isSelected ? table.getSelectionBackground() : CARD);
            panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

            JButton detailButton = createActionButton("Chi tiết", new Color(245, 247, 255), PRIMARY, PRIMARY);
            detailButton.addActionListener(e -> {
                if (value instanceof StayRow stayRow) {
                    openStayDetailDialog(stayRow);
                }
            });

            panel.add(detailButton);
            if (value instanceof StayRow stayRow) {
                if ("WAITING_CHECK_IN".equals(stayRow.getStatusCode())) {
                    JButton edit = createActionButton("Sửa", new Color(245, 247, 255), PRIMARY, PRIMARY);
                    edit.addActionListener(e -> openUpdateReservationDialog(stayRow));
                    panel.add(edit);

                    JButton cancel = createActionButton("Hủy", new Color(255, 230, 230), RED, RED);
                    cancel.addActionListener(e -> cancelReservation(stayRow));
                    panel.add(cancel);

                    JButton checkin = createActionButton("Check-in", new Color(224, 244, 234), GREEN, GREEN);
                    checkin.addActionListener(e -> openCheckInDialog(stayRow));
                    panel.add(checkin);
                } else if ("IN_HOUSE".equals(stayRow.getStatusCode()) || "CHECKOUT_PENDING".equals(stayRow.getStatusCode())) {
                    JButton addServiceBtn = createActionButton("Add Service", new Color(227, 236, 255), BLUE, BLUE);
                    addServiceBtn.addActionListener(e -> openAddServiceDialog(stayRow));
                    panel.add(addServiceBtn);

                    JButton checkoutBtn = createActionButton("Check-out", new Color(255, 240, 218), ORANGE, ORANGE);
                    checkoutBtn.addActionListener(e -> openCheckoutDialog(stayRow));
                    panel.add(checkoutBtn);
                } else if ("CHECKED_OUT".equals(stayRow.getStatusCode())) {
                    JButton viewInvoiceBtn = createActionButton("View Invoice", new Color(245, 247, 255), PRIMARY, PRIMARY);
                    viewInvoiceBtn.addActionListener(e -> openInvoice(stayRow));
                    panel.add(viewInvoiceBtn);
                }
            }
            return panel;
        }
    }

    private class StayActionEditor extends AbstractCellEditor implements TableCellEditor {
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            if (value instanceof StayRow stayRow) {
                JPanel panel = new JPanel(new GridLayout(2, 2, 6, 8));
                panel.setOpaque(true);
                panel.setBackground(isSelected ? table.getSelectionBackground() : CARD);
                panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

                JButton detailBtn = createActionButton("Chi tiết", new Color(245, 247, 255), PRIMARY, PRIMARY);
                detailBtn.addActionListener(e -> {
                    openStayDetailDialog(stayRow);
                    fireEditingStopped();
                });
                panel.add(detailBtn);

                if ("WAITING_CHECK_IN".equals(stayRow.getStatusCode())) {
                    JButton editBtn = createActionButton("Sửa", new Color(245, 247, 255), PRIMARY, PRIMARY);
                    editBtn.addActionListener(e -> {
                        openUpdateReservationDialog(stayRow);
                        fireEditingStopped();
                    });
                    panel.add(editBtn);

                    JButton cancelBtn = createActionButton("Hủy", new Color(255, 230, 230), RED, RED);
                    cancelBtn.addActionListener(e -> {
                        cancelReservation(stayRow);
                        fireEditingStopped();
                    });
                    panel.add(cancelBtn);

                    JButton checkinBtn = createActionButton("Check-in", new Color(224, 244, 234), GREEN, GREEN);
                    checkinBtn.addActionListener(e -> {
                        openCheckInDialog(stayRow);
                        fireEditingStopped();
                    });
                    panel.add(checkinBtn);
                } else if ("IN_HOUSE".equals(stayRow.getStatusCode()) || "CHECKOUT_PENDING".equals(stayRow.getStatusCode())) {
                    JButton addServiceBtn = createActionButton("Add Service", new Color(227, 236, 255), BLUE, BLUE);
                    addServiceBtn.addActionListener(e -> {
                        openAddServiceDialog(stayRow);
                        fireEditingStopped();
                    });
                    panel.add(addServiceBtn);

                    JButton checkoutBtn = createActionButton("Check-out", new Color(255, 240, 218), ORANGE, ORANGE);
                    checkoutBtn.addActionListener(e -> {
                        openCheckoutDialog(stayRow);
                        fireEditingStopped();
                    });
                    panel.add(checkoutBtn);
                } else if ("CHECKED_OUT".equals(stayRow.getStatusCode())) {
                    JButton viewInvoiceBtn = createActionButton("View Invoice", new Color(245, 247, 255), PRIMARY, PRIMARY);
                    viewInvoiceBtn.addActionListener(e -> {
                        openInvoice(stayRow);
                        fireEditingStopped();
                    });
                    panel.add(viewInvoiceBtn);
                }
                return panel;
            }
            return new JPanel();
        }

        @Override
        public Object getCellEditorValue() {
            return null;
        }
    }

    private static class StayRow {
        private int reservationId;
        private String reservationCode;
        private String guestName;
        private String phone;
        private String roomNumber;
        private int roomId;
        private int stayId;
        private LocalDate checkInDate;
        private LocalDate expectedCheckOutDate;
        private LocalDateTime actualCheckOutDate;
        private int numberOfGuests;
        private String statusCode;
        private String stayStatus;
        private LocalDateTime actualCheckIn;

        public int getReservationId() { return reservationId; }
        public void setReservationId(int reservationId) { this.reservationId = reservationId; }
        public String getReservationCode() { return reservationCode; }
        public void setReservationCode(String reservationCode) { this.reservationCode = reservationCode; }
        public String getGuestName() { return guestName; }
        public void setGuestName(String guestName) { this.guestName = guestName; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getRoomNumber() { return roomNumber; }
        public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
        public int getRoomId() { return roomId; }
        public void setRoomId(int roomId) { this.roomId = roomId; }
        public int getStayId() { return stayId; }
        public void setStayId(int stayId) { this.stayId = stayId; }
        public LocalDate getCheckInDate() { return checkInDate; }
        public void setCheckInDate(LocalDate checkInDate) { this.checkInDate = checkInDate; }
        public LocalDate getExpectedCheckOutDate() { return expectedCheckOutDate; }
        public void setExpectedCheckOutDate(LocalDate expectedCheckOutDate) { this.expectedCheckOutDate = expectedCheckOutDate; }
        public LocalDateTime getActualCheckOutDate() { return actualCheckOutDate; }
        public void setActualCheckOutDate(LocalDateTime actualCheckOutDate) { this.actualCheckOutDate = actualCheckOutDate; }
        public int getNumberOfGuests() { return numberOfGuests; }
        public void setNumberOfGuests(int numberOfGuests) { this.numberOfGuests = numberOfGuests; }
        public String getStatusCode() { return statusCode; }
        public void setStatusCode(String statusCode) { this.statusCode = statusCode; }
        public String getStayStatus() { return stayStatus; }
        public void setStayStatus(String stayStatus) { this.stayStatus = stayStatus; }
        public LocalDateTime getActualCheckIn() { return actualCheckIn; }
        public void setActualCheckIn(LocalDateTime actualCheckIn) { this.actualCheckIn = actualCheckIn; }
    }

}
