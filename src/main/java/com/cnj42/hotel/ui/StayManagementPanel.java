package com.cnj42.hotel.ui;

import com.cnj42.hotel.model.CheckoutSummary;
import com.cnj42.hotel.model.ServiceUsage;
import com.cnj42.hotel.model.StayDetail;
import com.cnj42.hotel.service.ReservationService;
import com.cnj42.hotel.service.StayService;
import com.cnj42.hotel.utils.DBConnection;

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
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class StayManagementPanel extends JPanel {

    private static final Color PRIMARY = new Color(105, 78, 210);
    private static final Color PRIMARY_LIGHT = new Color(239, 235, 255);
    private static final Color BACKGROUND = new Color(246, 247, 251);
    private static final Color CARD = new Color(255, 255, 255);
    private static final Color BORDER = new Color(230, 234, 241);
    private static final Color TEXT_DARK = new Color(35, 40, 52);
    private static final Color TEXT_MUTED = new Color(120, 125, 140);

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
    private final Integer currentUserId;
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
        this(null);
    }

    public StayManagementPanel(Integer currentUserId) {
        this.currentUserId = currentUserId;

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
        setBorder(new EmptyBorder(14, 16, 18, 16));

        add(buildHeaderPanel(), BorderLayout.NORTH);

        JPanel contentCard = new JPanel(new BorderLayout(0, 14));
        contentCard.setOpaque(false);
        contentCard.add(buildStatsPanel(), BorderLayout.NORTH);
        contentCard.add(buildTablePanel(), BorderLayout.CENTER);
        add(contentCard, BorderLayout.CENTER);

        refreshStayData();
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

    private JPanel buildHeaderPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Quản lý lưu trú");
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setForeground(TEXT_DARK);

        JLabel subtitle = new JLabel("Quản lý quá trình nhận phòng, lưu trú và trả phòng của khách");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(TEXT_MUTED);

        panel.add(title);
        panel.add(Box.createVerticalStrut(4));
        panel.add(subtitle);
        return panel;
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

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
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
        createGuestButton.setBackground(PRIMARY);
        createGuestButton.setForeground(Color.WHITE);
        createGuestButton.setFocusPainted(false);
        createGuestButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        createGuestButton.setBorder(new EmptyBorder(8, 18, 8, 18));
        createGuestButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        createGuestButton.addActionListener(e -> openCreateGuestDialog());

        JButton createReservationButton = new JButton("+ Đặt phòng");
        createReservationButton.setBackground(PRIMARY);
        createReservationButton.setForeground(Color.WHITE);
        createReservationButton.setFocusPainted(false);
        createReservationButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        createReservationButton.setBorder(new EmptyBorder(8, 18, 8, 18));
        createReservationButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        createReservationButton.addActionListener(e -> openCreateReservationDialog());

        JButton refreshButton = new JButton("Làm mới");
        refreshButton.setBackground(PRIMARY);
        refreshButton.setForeground(Color.WHITE);
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
        stayTable.setRowHeight(84);
        stayTable.setFillsViewportHeight(true);
        stayTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        stayTable.setSelectionBackground(new Color(238, 240, 255));
        stayTable.setSelectionForeground(TEXT_DARK);
        stayTable.setShowGrid(false);
        stayTable.setIntercellSpacing(new Dimension(0, 8));
        stayTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        stayTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        stayTable.getTableHeader().setBackground(Color.WHITE);
        stayTable.getTableHeader().setForeground(TEXT_DARK);
        stayTable.getTableHeader().setReorderingAllowed(false);
        stayTable.setDefaultRenderer(Object.class, new StayStatusRenderer());
        stayTable.getColumnModel().getColumn(ACTION_COL).setCellRenderer(new StayActionRenderer());
        stayTable.getColumnModel().getColumn(ACTION_COL).setCellEditor(new StayActionEditor());
        stayTable.getColumnModel().getColumn(ACTION_COL).setPreferredWidth(260);
        stayTable.getColumnModel().getColumn(ACTION_COL).setMinWidth(250);
        stayTable.getColumnModel().getColumn(ACTION_COL).setMaxWidth(280);

        stayTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = stayTable.rowAtPoint(e.getPoint());
                int col = stayTable.columnAtPoint(e.getPoint());
                if (row < 0 || col != ACTION_COL) {
                    return;
                }
                Object value = stayTable.getValueAt(row, ACTION_COL);
                if (value instanceof StayRow stayRow) {
                    if ("WAITING_CHECK_IN".equals(stayRow.getStatusCode())) {
                        openCheckInDialog(stayRow);
                    } else if ("IN_HOUSE".equals(stayRow.getStatusCode()) || "CHECKOUT_PENDING".equals(stayRow.getStatusCode())) {
                        openCheckoutDialog(stayRow);
                    } else {
                        openStayDetailDialog(stayRow);
                    }
                }
            }
        });

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

        JScrollPane scrollPane = new JScrollPane(stayTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(CARD);
        card.add(scrollPane, BorderLayout.CENTER);

        return card;
    }

    private void refreshStayData() {
        loadStats();
        loadStayTable();
    }

    private void loadStats() {
        String sql = "SELECT " +
                "SUM(CASE WHEN s.stay_id IS NULL AND res.status IN ('PENDING','CONFIRMED') THEN 1 ELSE 0 END) AS waiting_checkin, " +
                "SUM(CASE WHEN s.status = 'CHECKED_IN' AND s.actual_check_out IS NULL THEN 1 ELSE 0 END) AS in_house, " +
                "SUM(CASE WHEN s.status = 'CHECKED_IN' AND s.actual_check_out IS NULL AND res.check_out_date <= CURDATE() THEN 1 ELSE 0 END) AS waiting_checkout, " +
                "SUM(CASE WHEN s.status = 'CHECKED_OUT' OR res.status = 'COMPLETED' THEN 1 ELSE 0 END) AS checked_out " +
                "FROM reservations res " +
                "LEFT JOIN stays s ON s.reservation_id = res.reservation_id";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                waitingCheckinCount.setText(String.valueOf(nvl(rs.getInt("waiting_checkin"))));
                inHouseCount.setText(String.valueOf(nvl(rs.getInt("in_house"))));
                waitingCheckoutCount.setText(String.valueOf(nvl(rs.getInt("waiting_checkout"))));
                checkedOutCount.setText(String.valueOf(nvl(rs.getInt("checked_out"))));
            } else {
                waitingCheckinCount.setText("0");
                inHouseCount.setText("0");
                waitingCheckoutCount.setText("0");
                checkedOutCount.setText("0");
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Không thể tải thống kê lưu trú: " + ex.getMessage(), "Lỗi dữ liệu", JOptionPane.ERROR_MESSAGE);
            waitingCheckinCount.setText("0");
            inHouseCount.setText("0");
            waitingCheckoutCount.setText("0");
            checkedOutCount.setText("0");
        }
    }

    private void loadStayTable() {
        tableModel.setRowCount(0);

        StringBuilder sql = new StringBuilder(
                "SELECT res.reservation_id, res.reservation_code, g.full_name AS guest_name, g.phone, " +
                "r.room_number, r.room_id, res.check_in_date, res.check_out_date, " +
                "s.stay_id, s.actual_check_in, s.actual_check_out, s.status AS stay_status, res.number_of_guests, " +
                "CASE " +
                "WHEN s.stay_id IS NULL AND res.status IN ('PENDING','CONFIRMED') THEN 'WAITING_CHECK_IN' " +
                "WHEN s.status = 'CHECKED_IN' AND s.actual_check_out IS NULL AND res.check_out_date <= CURDATE() THEN 'CHECKOUT_PENDING' " +
                "WHEN s.status = 'CHECKED_IN' AND s.actual_check_out IS NULL THEN 'IN_HOUSE' " +
                "WHEN s.status = 'CHECKED_OUT' OR res.status = 'COMPLETED' THEN 'CHECKED_OUT' " +
                "ELSE 'WAITING_CHECK_IN' " +
                "END AS display_status " +
                "FROM reservations res " +
                "LEFT JOIN guests g ON g.guest_id = res.guest_id " +
                "LEFT JOIN rooms r ON r.room_id = res.room_id " +
                "LEFT JOIN stays s ON s.reservation_id = res.reservation_id " +
                "LEFT JOIN reservation_guests rg ON rg.reservation_id = res.reservation_id " +
                "WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim();
        if (!keyword.isEmpty()) {
            sql.append("AND (g.full_name LIKE ? OR g.phone LIKE ? OR r.room_number LIKE ? OR res.reservation_code LIKE ?) ");
            String like = "%" + keyword + "%";
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
        }

        String statusFilterValue = (String) statusFilterCombo.getSelectedItem();
        String normalizedStatus = normalizeStatusForQuery(statusFilterValue);
        if (normalizedStatus != null) {
            sql.append("AND CASE ");
            sql.append("WHEN s.stay_id IS NULL AND res.status IN ('PENDING','CONFIRMED') THEN 'WAITING_CHECK_IN' ");
            sql.append("WHEN s.status = 'CHECKED_IN' AND s.actual_check_out IS NULL AND res.check_out_date <= CURDATE() THEN 'CHECKOUT_PENDING' ");
            sql.append("WHEN s.status = 'CHECKED_IN' AND s.actual_check_out IS NULL THEN 'IN_HOUSE' ");
            sql.append("WHEN s.status = 'CHECKED_OUT' OR res.status = 'COMPLETED' THEN 'CHECKED_OUT' ");
            sql.append("ELSE 'WAITING_CHECK_IN' END = ? ");
            params.add(normalizedStatus);
        }

        String fromText = fromDateField.getText() == null ? "" : fromDateField.getText().trim();
        String toText = toDateField.getText() == null ? "" : toDateField.getText().trim();
        if (!fromText.isEmpty()) {
            sql.append("AND DATE(COALESCE(s.actual_check_in, res.check_in_date)) >= ? ");
            params.add(parseDate(fromText).toString());
        }
        if (!toText.isEmpty()) {
            sql.append("AND DATE(COALESCE(s.actual_check_in, res.check_in_date)) <= ? ");
            params.add(parseDate(toText).toString());
        }

        sql.append("ORDER BY COALESCE(s.actual_check_in, res.check_in_date) DESC, res.reservation_id DESC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            int index = 1;
            for (Object value : params) {
                stmt.setObject(index++, value);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                int rowIndex = 1;
                while (rs.next()) {
                    StayRow stayRow = new StayRow();
                    stayRow.setReservationId(rs.getInt("reservation_id"));
                    stayRow.setReservationCode(rs.getString("reservation_code"));
                    stayRow.setGuestName(rs.getString("guest_name"));
                    stayRow.setPhone(rs.getString("phone"));
                    stayRow.setRoomNumber(rs.getString("room_number"));
                    stayRow.setRoomId(rs.getInt("room_id"));
                    stayRow.setStayId(rs.getObject("stay_id") == null ? 0 : rs.getInt("stay_id"));
                    stayRow.setCheckInDate(readDate(rs.getDate("check_in_date")));
                    stayRow.setExpectedCheckOutDate(readDate(rs.getDate("check_out_date")));
                    stayRow.setActualCheckOutDate(readDateTime(rs.getTimestamp("actual_check_out")));
                    stayRow.setNumberOfGuests(rs.getInt("number_of_guests"));
                    stayRow.setStatusCode(rs.getString("display_status"));
                    stayRow.setStayStatus(rs.getString("stay_status"));
                    stayRow.setActualCheckIn(readDateTime(rs.getTimestamp("actual_check_in")));

                    String checkinText = stayRow.getActualCheckIn() == null ? formatLocalDate(stayRow.getCheckInDate()) : formatDateTime(stayRow.getActualCheckIn());
                    String expectedCheckoutText = stayRow.getExpectedCheckOutDate() == null ? "-" : formatLocalDate(stayRow.getExpectedCheckOutDate());
                    String actualCheckoutText = stayRow.getActualCheckOutDate() == null ? "-" : formatDateTime(stayRow.getActualCheckOutDate());

                    Object[] row = new Object[]{
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
                    };
                    tableModel.addRow(row);
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Không thể tải danh sách lưu trú: " + ex.getMessage(), "Lỗi dữ liệu", JOptionPane.ERROR_MESSAGE);
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

    private LocalDateTime readDateTime(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return timestamp.toLocalDateTime();
    }

    private LocalDate readDate(Date date) {
        if (date == null) {
            return null;
        }
        return date.toLocalDate();
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
        String fetchReservation = "SELECT reservation_id, room_id, status, check_in_date, check_out_date FROM reservations WHERE reservation_id = ?";
        String updateReservation = "UPDATE reservations SET status = 'CHECKED_IN' WHERE reservation_id = ? AND status IN ('PENDING', 'CONFIRMED')";
        String insertStay = "INSERT INTO stays (reservation_id, room_id, actual_check_in, status, check_in_by) VALUES (?, ?, NOW(), 'CHECKED_IN', ?)";
        String updateRoom = "UPDATE rooms SET status = 'OCCUPIED' WHERE room_id = ?";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            int currentRoomId = -1;
            String reservationStatus = null;
            String checkInDate = null;
            String checkOutDate = null;
            try (PreparedStatement ps = conn.prepareStatement(fetchReservation)) {
                ps.setInt(1, reservationId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return false;
                    }
                    currentRoomId = rs.getInt("room_id");
                    reservationStatus = rs.getString("status");
                    checkInDate = rs.getString("check_in_date");
                    checkOutDate = rs.getString("check_out_date");
                }
            }

            if (currentRoomId != roomId || reservationStatus == null || (!"PENDING".equals(reservationStatus) && !"CONFIRMED".equals(reservationStatus))) {
                conn.rollback();
                return false;
            }

            try (PreparedStatement ps = conn.prepareStatement(updateReservation)) {
                ps.setInt(1, reservationId);
                if (ps.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(insertStay)) {
                ps.setInt(1, reservationId);
                ps.setInt(2, roomId);
                if (currentUserId != null) {
                    ps.setInt(3, currentUserId);
                } else {
                    ps.setNull(3, Types.INTEGER);
                }
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(updateRoom)) {
                ps.setInt(1, roomId);
                ps.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException ex) {
            System.err.println("Lỗi check-in: " + ex.getMessage());
            return false;
        }
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

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "CHECK-OUT", true);
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
        summaryPanel.add(new JLabel("Tổng cộng:"));
        summaryPanel.add(new JLabel(formatCurrency(summary.getTotalAmount())));

        JPanel methodPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        methodPanel.setOpaque(false);
        methodPanel.add(new JLabel("Phương thức thanh toán:"));
        JComboBox<String> paymentMethod = new JComboBox<>(new String[]{"CASH", "CARD", "BANK_TRANSFER"});
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
        content.add(methodPanel, BorderLayout.SOUTH);

        dialog.add(content, BorderLayout.CENTER);
        dialog.add(footer, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(this);

        cancel.addActionListener(e -> dialog.dispose());
        confirm.addActionListener(e -> {
            boolean ok = performCheckout(stayRow, summary, (String) paymentMethod.getSelectedItem());
            if (ok) {
                JOptionPane.showMessageDialog(this, "Thanh toán và check-out thành công.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
                refreshStayData();
            } else {
                JOptionPane.showMessageDialog(this, "Không thể thanh toán hoặc check-out.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.setVisible(true);
    }

    private CheckoutSummary loadCheckoutSummary(int reservationId, int roomId, int stayId) {
        String roomSql = "SELECT rt.price_per_night FROM rooms r JOIN room_types rt ON rt.room_type_id = r.room_type_id WHERE r.room_id = ?";
        String serviceSql = "SELECT COALESCE(SUM(su.quantity * su.unit_price), 0) AS service_amount FROM service_usages su WHERE su.stay_id = ?";

        BigDecimal roomAmount = BigDecimal.ZERO;
        BigDecimal serviceAmount = BigDecimal.ZERO;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement roomPs = conn.prepareStatement(roomSql)) {
            roomPs.setInt(1, roomId);
            try (ResultSet roomRs = roomPs.executeQuery()) {
                if (roomRs.next()) {
                    roomAmount = roomAmount.add(BigDecimal.valueOf(roomRs.getDouble("price_per_night")));
                }
            }

            try (PreparedStatement servicePs = conn.prepareStatement(serviceSql)) {
                servicePs.setInt(1, stayId);
                try (ResultSet serviceRs = servicePs.executeQuery()) {
                    if (serviceRs.next()) {
                        serviceAmount = BigDecimal.valueOf(serviceRs.getDouble("service_amount"));
                    }
                }
            }
        } catch (SQLException ex) {
            System.err.println("Lỗi khi tính chi phí checkout: " + ex.getMessage());
            return null;
        }

        BigDecimal total = roomAmount.add(serviceAmount);
        return new CheckoutSummary(roomAmount.doubleValue(), serviceAmount.doubleValue(), total.doubleValue());
    }

    private boolean performCheckout(StayRow stayRow, CheckoutSummary summary, String paymentMethod) {
        if (stayRow == null || stayRow.getStayId() <= 0) {
            return false;
        }

        final String invoiceCode = "INV" + System.currentTimeMillis();
        String checkExistingInvoice = "SELECT COUNT(*) FROM invoices WHERE stay_id = ?";
        String checkStayStatus = "SELECT status FROM stays WHERE stay_id = ?";
        String invoiceInsert = "INSERT INTO invoices (invoice_code, stay_id, room_amount, service_amount, discount_amount, tax_amount, total_amount, status, created_by) VALUES (?, ?, ?, ?, 0, 0, ?, 'UNPAID', ?)";
        String invoiceDetailRoom = "INSERT INTO invoice_details (invoice_id, item_type, description, quantity, unit_price, amount) VALUES (?, 'ROOM', ?, ?, ?, ?)";
        String invoiceDetailService = "INSERT INTO invoice_details (invoice_id, item_type, description, quantity, unit_price, amount) VALUES (?, 'SERVICE', ?, ?, ?, ?)";
        String paymentInsert = "INSERT INTO payments (invoice_id, amount, payment_method, payment_date, note, received_by) VALUES (?, ?, ?, NOW(), ?, ?)";
        String stayUpdate = "UPDATE stays SET status = 'CHECKED_OUT', actual_check_out = NOW(), check_out_by = ? WHERE stay_id = ? AND status <> 'CHECKED_OUT'";
        String reservationUpdate = "UPDATE reservations SET status = 'COMPLETED' WHERE reservation_id = ? AND status <> 'COMPLETED'";
        String roomUpdate = "UPDATE rooms SET status = 'CLEANING' WHERE room_id = ?";
        String serviceUsageSql = "SELECT s.service_name, su.quantity, su.unit_price, (su.quantity * su.unit_price) AS total_amount FROM service_usages su JOIN services s ON s.service_id = su.service_id WHERE su.stay_id = ?";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement checkStayPs = conn.prepareStatement(checkStayStatus)) {
                checkStayPs.setInt(1, stayRow.getStayId());
                try (ResultSet rs = checkStayPs.executeQuery()) {
                    if (rs.next() && "CHECKED_OUT".equalsIgnoreCase(rs.getString("status"))) {
                        conn.rollback();
                        return false;
                    }
                }
            }

            try (PreparedStatement existingPs = conn.prepareStatement(checkExistingInvoice)) {
                existingPs.setInt(1, stayRow.getStayId());
                try (ResultSet rs = existingPs.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        conn.rollback();
                        return false;
                    }
                }
            }

            int invoiceId;
            try (PreparedStatement invoicePs = conn.prepareStatement(invoiceInsert, Statement.RETURN_GENERATED_KEYS)) {
                invoicePs.setString(1, invoiceCode);
                invoicePs.setInt(2, stayRow.getStayId());
                invoicePs.setDouble(3, summary.getRoomAmount());
                invoicePs.setDouble(4, summary.getServiceAmount());
                invoicePs.setDouble(5, summary.getTotalAmount());
                if (currentUserId != null) {
                    invoicePs.setInt(6, currentUserId);
                } else {
                    invoicePs.setNull(6, Types.INTEGER);
                }
                invoicePs.executeUpdate();

                try (ResultSet keys = invoicePs.getGeneratedKeys()) {
                    if (!keys.next()) {
                        conn.rollback();
                        return false;
                    }
                    invoiceId = keys.getInt(1);
                }
            }

            try (PreparedStatement roomDetail = conn.prepareStatement(invoiceDetailRoom)) {
                roomDetail.setInt(1, invoiceId);
                roomDetail.setString(2, "Phòng " + stayRow.getRoomNumber());
                roomDetail.setInt(3, 1);
                roomDetail.setDouble(4, summary.getRoomAmount());
                roomDetail.setDouble(5, summary.getRoomAmount());
                roomDetail.executeUpdate();
            }

            try (PreparedStatement serviceDetail = conn.prepareStatement(invoiceDetailService)) {
                try (PreparedStatement usagePs = conn.prepareStatement(serviceUsageSql)) {
                    usagePs.setInt(1, stayRow.getStayId());
                    try (ResultSet usageRs = usagePs.executeQuery()) {
                        while (usageRs.next()) {
                            serviceDetail.setInt(1, invoiceId);
                            serviceDetail.setString(2, usageRs.getString("service_name"));
                            serviceDetail.setInt(3, usageRs.getInt("quantity"));
                            serviceDetail.setDouble(4, usageRs.getDouble("unit_price"));
                            serviceDetail.setDouble(5, usageRs.getDouble("total_amount"));
                            serviceDetail.executeUpdate();
                        }
                    }
                }
            }

            try (PreparedStatement paymentPs = conn.prepareStatement(paymentInsert)) {
                paymentPs.setInt(1, invoiceId);
                paymentPs.setDouble(2, summary.getTotalAmount());
                paymentPs.setString(3, paymentMethod == null ? "CASH" : paymentMethod);
                paymentPs.setString(4, "Thanh toán khi check-out");
                if (currentUserId != null) {
                    paymentPs.setInt(5, currentUserId);
                } else {
                    paymentPs.setNull(5, Types.INTEGER);
                }
                paymentPs.executeUpdate();
            }

            try (PreparedStatement stayPs = conn.prepareStatement(stayUpdate)) {
                if (currentUserId != null) {
                    stayPs.setInt(1, currentUserId);
                } else {
                    stayPs.setNull(1, Types.INTEGER);
                }
                stayPs.setInt(2, stayRow.getStayId());
                stayPs.executeUpdate();
            }

            try (PreparedStatement reservationPs = conn.prepareStatement(reservationUpdate)) {
                reservationPs.setInt(1, stayRow.getReservationId());
                reservationPs.executeUpdate();
            }

            try (PreparedStatement roomPs = conn.prepareStatement(roomUpdate)) {
                roomPs.setInt(1, stayRow.getRoomId());
                roomPs.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException ex) {
            System.err.println("Lỗi khi thực hiện checkout: " + ex.getMessage());
            return false;
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
        String detailSql = "SELECT res.reservation_id, res.reservation_code, g.full_name, g.phone, g.email, " +
                "r.room_number, rt.type_name, rt.price_per_night, s.actual_check_in, res.check_out_date, s.actual_check_out, " +
                "res.number_of_guests, CASE " +
                "WHEN s.stay_id IS NULL AND res.status IN ('PENDING','CONFIRMED') THEN 'WAITING_CHECK_IN' " +
                "WHEN s.status = 'CHECKED_IN' AND s.actual_check_out IS NULL AND res.check_out_date <= CURDATE() THEN 'CHECKOUT_PENDING' " +
                "WHEN s.status = 'CHECKED_IN' AND s.actual_check_out IS NULL THEN 'IN_HOUSE' " +
                "WHEN s.status = 'CHECKED_OUT' OR res.status = 'COMPLETED' THEN 'CHECKED_OUT' " +
                "ELSE 'WAITING_CHECK_IN' END AS status_code " +
                "FROM reservations res " +
                "JOIN guests g ON g.guest_id = res.guest_id " +
                "JOIN rooms r ON r.room_id = res.room_id " +
                "JOIN room_types rt ON rt.room_type_id = r.room_type_id " +
                "LEFT JOIN stays s ON s.reservation_id = res.reservation_id " +
                "WHERE res.reservation_id = ?";

        String serviceSql = "SELECT ser.service_name, su.quantity, su.unit_price, (su.quantity * su.unit_price) AS total_amount " +
                "FROM service_usages su JOIN services ser ON ser.service_id = su.service_id WHERE su.stay_id = ?";

        StayDetail detail = null;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement detailPs = conn.prepareStatement(detailSql)) {
            detailPs.setInt(1, reservationId);
            try (ResultSet detailRs = detailPs.executeQuery()) {
                if (!detailRs.next()) {
                    return null;
                }
                detail = new StayDetail();
                detail.setReservationCode(detailRs.getString("reservation_code"));
                detail.setGuestName(detailRs.getString("full_name"));
                detail.setPhone(detailRs.getString("phone"));
                detail.setEmail(detailRs.getString("email"));
                detail.setRoomNumber(detailRs.getString("room_number"));
                detail.setRoomType(detailRs.getString("type_name"));
                detail.setRoomPrice(detailRs.getDouble("price_per_night"));
                detail.setActualCheckIn(readDateTime(detailRs.getTimestamp("actual_check_in")));
                detail.setExpectedCheckOut(readDate(detailRs.getDate("check_out_date")));
                detail.setActualCheckOut(readDateTime(detailRs.getTimestamp("actual_check_out")));
                detail.setNumberOfGuests(detailRs.getInt("number_of_guests"));
                detail.setStatusCode(detailRs.getString("status_code"));
            }

            if (detail != null && stayId > 0) {
                try (PreparedStatement servicePs = conn.prepareStatement(serviceSql)) {
                    servicePs.setInt(1, stayId);
                    try (ResultSet serviceRs = servicePs.executeQuery()) {
                        while (serviceRs.next()) {
                            detail.getServiceUsages().add(new ServiceUsage(
                                    serviceRs.getString("service_name"),
                                    serviceRs.getInt("quantity"),
                                    serviceRs.getDouble("unit_price"),
                                    serviceRs.getDouble("total_amount")
                            ));
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            System.err.println("Lỗi tải chi tiết lưu trú: " + ex.getMessage());
            return null;
        }

        return detail;
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
                    JButton checkout = createActionButton("Check-out", new Color(255, 240, 218), ORANGE, ORANGE);
                    checkout.addActionListener(e -> openCheckoutDialog(stayRow));
                    panel.add(checkout);
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
                    JButton checkoutBtn = createActionButton("Check-out", new Color(255, 240, 218), ORANGE, ORANGE);
                    checkoutBtn.addActionListener(e -> {
                        openCheckoutDialog(stayRow);
                        fireEditingStopped();
                    });
                    panel.add(checkoutBtn);
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
