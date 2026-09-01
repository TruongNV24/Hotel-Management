package com.cnj42.hotel.ui;

import com.cnj42.hotel.model.DashboardData;
import com.cnj42.hotel.model.User;
import com.cnj42.hotel.service.DashboardService;
import com.cnj42.hotel.utils.DBConnection;
import java.util.Locale;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.text.DecimalFormat;

public class MainFrame extends JFrame {

    // =========================================================
    // COLORS
    // =========================================================

    private static final Color PRIMARY = new Color(105, 78, 210);
    private static final Color PRIMARY_LIGHT = new Color(239, 235, 255);
    private static final Color LOGO_ACCENT = new Color(196, 186, 255);

    private static final Color SIDEBAR = new Color(27, 35, 55);
    private static final Color SIDEBAR_HOVER = new Color(48, 57, 82);

    private static final Color BACKGROUND = new Color(246, 247, 251);
    private static final Color TEXT_DARK = new Color(35, 40, 52);
    private static final Color TEXT_GRAY = new Color(120, 125, 140);

    private static final Color GREEN = new Color(35, 181, 118);
    private static final Color ORANGE = new Color(245, 153, 55);
    private static final Color BLUE = new Color(65, 135, 235);
    private static final Color RED = new Color(235, 75, 75);
    private static final Color GRAY = new Color(120, 128, 145);
    private static final Color PINK = new Color(224, 96, 160);
    private static final Color TEAL = new Color(38, 175, 175);

    private static final Color[] AVATAR_PALETTE = {
            PRIMARY, BLUE, ORANGE, PINK, TEAL
    };

    private static final DecimalFormat MONEY_FORMAT =
            new DecimalFormat("#,###");

    // =========================================================
    // USER
    // =========================================================

    private final User currentUser;
    private final DashboardService dashboardService;

    // =========================================================
    // UI
    // =========================================================

    private JPanel contentPanel;
    private JLabel pageTitle;
    private JLabel pageDescription;

    // Dashboard statistics
    private JLabel totalRoomsLabel;
    private JLabel availableRoomsLabel;
    private JLabel occupiedRoomsLabel;
    private JLabel reservationsLabel;

    private JLabel totalRoomsDesc;
    private JLabel availableRoomsDesc;
    private JLabel occupiedRoomsDesc;
    private JLabel reservationsDesc;

    private JLabel revenueTotalLabel;
    private JLabel revenueChangeLabel;
    private RevenueChartPanel revenueChartPanel;
    private DonutChartPanel donutChartPanel;
    private JPanel roomLegendPanel;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public MainFrame(User user) {

        this.currentUser = user;
        this.dashboardService = new DashboardService();

        setTitle("Hotel Management System");
        setSize(1440, 900);
        setMinimumSize(new Dimension(1200, 760));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(true);

        initUI();

        ensureDatabaseReady();

        // Load data from MySQL
        loadDashboardData();
    }

    // =========================================================
    // INIT UI
    // =========================================================

    private void initUI() {

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BACKGROUND);

        // Sidebar
        root.add(createSidebar(), BorderLayout.WEST);

        // Main area
        JPanel mainArea = new JPanel(new BorderLayout());
        mainArea.setBackground(BACKGROUND);

        mainArea.add(createHeader(), BorderLayout.NORTH);

        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(BACKGROUND);
        contentPanel.setBorder(new EmptyBorder(25, 30, 30, 30));

        mainArea.add(contentPanel, BorderLayout.CENTER);

        root.add(mainArea, BorderLayout.CENTER);

        setContentPane(root);

        showDashboard();
    }

    // =========================================================
    // SIDEBAR
    // =========================================================

    private JPanel createSidebar() {

        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(235, 0));
        sidebar.setBackground(SIDEBAR);

        // -----------------------------------------------------
        // LOGO  (icon badge + "HOTEL / MANAGEMENT" text, horizontal)
        // -----------------------------------------------------

        JPanel logoPanel = new JPanel(new BorderLayout(12, 0));
        logoPanel.setBackground(SIDEBAR);
        logoPanel.setBorder(new EmptyBorder(24, 22, 24, 20));

        JPanel iconBadge = new RoundedPanel(PRIMARY, 14);
        iconBadge.setLayout(new GridBagLayout());
        iconBadge.setPreferredSize(new Dimension(46, 46));
        iconBadge.add(createVectorIcon("building", Color.WHITE, 24));

        logoPanel.add(iconBadge, BorderLayout.WEST);

        JPanel logoText = new JPanel();
        logoText.setOpaque(false);
        logoText.setLayout(new BoxLayout(logoText, BoxLayout.Y_AXIS));

        JLabel hotelLabel = new JLabel("HOTEL");
        hotelLabel.setFont(new Font("Segoe UI", Font.BOLD, 17));
        hotelLabel.setForeground(Color.WHITE);
        hotelLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("MANAGEMENT");
        subtitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        subtitle.setForeground(LOGO_ACCENT);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        logoText.add(hotelLabel);
        logoText.add(subtitle);

        logoPanel.add(logoText, BorderLayout.CENTER);

        sidebar.add(logoPanel, BorderLayout.NORTH);

        // -----------------------------------------------------
        // MENU
        // -----------------------------------------------------

        JPanel menu = new JPanel();
        menu.setBackground(SIDEBAR);
        menu.setBorder(new EmptyBorder(5, 12, 5, 12));
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));

        addMenuButton(menu, "🏠", "Dashboard", true, this::showDashboard);

        addSectionTitle(menu, "QUẢN LÝ");

        addMenuButton(menu, "🛏", "Quản lý phòng", false, () -> openModule("Quản lý phòng"));
        addMenuButton(menu, "🏷", "Loại phòng", false, () -> openModule("Loại phòng"));
        addMenuButton(menu, "👤", "Khách hàng", false, () -> openModule("Khách hàng"));
        addMenuButton(menu, "📅", "Đặt phòng", false, () -> openModule("Đặt phòng"));
        addMenuButton(menu, "🧳", "Lưu trú", false, () -> openModule("Quản lý lưu trú"));

        addSectionTitle(menu, "DỊCH VỤ");

        addMenuButton(menu, "🍽", "Dịch vụ", false, () -> openModule("Dịch vụ"));
        addMenuButton(menu, "🧾", "Hóa đơn", false, () -> openModule("Hóa đơn"));
        addMenuButton(menu, "💳", "Thanh toán", false, () -> openModule("Thanh toán"));

        sidebar.add(menu, BorderLayout.CENTER);

        // -----------------------------------------------------
        // BOTTOM
        // -----------------------------------------------------

        JPanel bottom = new JPanel();
        bottom.setBackground(SIDEBAR);
        bottom.setBorder(new EmptyBorder(10, 12, 18, 12));
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));

        addMenuButton(bottom, "⚙", "Cài đặt", false, () -> openModule("Cài đặt"));
        addMenuButton(bottom, "↩", "Đăng xuất", false, this::logout);

        sidebar.add(bottom, BorderLayout.SOUTH);

        return sidebar;
    }

    // =========================================================
    // MENU BUTTON
    // =========================================================

    private void addMenuButton(JPanel parent, String icon, String text, boolean selected, Runnable action) {

        JPanel button = selected
                ? new RoundedPanel(PRIMARY, 10)
                : new JPanel();

        button.setLayout(new BorderLayout());
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        button.setPreferredSize(new Dimension(0, 45));
        button.setBorder(new EmptyBorder(0, 12, 0, 10));

        if (!selected) {
            button.setBackground(SIDEBAR);
        }

        JLabel iconLabel = new JLabel(icon);
        iconLabel.setPreferredSize(new Dimension(30, 0));
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        iconLabel.setForeground(selected ? Color.WHITE : new Color(180, 183, 198));

        JLabel textLabel = new JLabel(text);
        textLabel.setFont(new Font("Segoe UI", selected ? Font.BOLD : Font.PLAIN, 13));
        textLabel.setForeground(Color.WHITE);

        button.add(iconLabel, BorderLayout.WEST);
        button.add(textLabel, BorderLayout.CENTER);

        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseEntered(MouseEvent e) {
                if (!selected) {
                    button.setBackground(SIDEBAR_HOVER);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!selected) {
                    button.setBackground(SIDEBAR);
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                action.run();
            }
        });

        parent.add(button);
        parent.add(Box.createVerticalStrut(4));
    }

    private void addSectionTitle(JPanel parent, String text) {

        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 10));
        label.setForeground(new Color(120, 125, 145));
        label.setBorder(new EmptyBorder(18, 13, 8, 0));

        parent.add(label);
    }

    // =========================================================
    // HEADER
    // =========================================================

    private JPanel createHeader() {

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setPreferredSize(new Dimension(0, 75));
        header.setBorder(new EmptyBorder(0, 30, 0, 30));

        // ---- Left: hamburger + title ----

        JPanel leftHeader = new JPanel(new BorderLayout(16, 0));
        leftHeader.setOpaque(false);

        JComponent menuIcon = createVectorIcon("menu", TEXT_DARK, 20);
        menuIcon.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JPanel menuIconWrap = new JPanel(new GridBagLayout());
        menuIconWrap.setOpaque(false);
        menuIconWrap.add(menuIcon);
        leftHeader.add(menuIconWrap, BorderLayout.WEST);

        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));

        pageTitle = new JLabel("Dashboard");
        pageTitle.setFont(new Font("Segoe UI", Font.BOLD, 21));
        pageTitle.setForeground(TEXT_DARK);

        pageDescription = new JLabel("Tổng quan hoạt động khách sạn");
        pageDescription.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        pageDescription.setForeground(TEXT_GRAY);

        titlePanel.add(pageTitle);
        titlePanel.add(Box.createVerticalStrut(3));
        titlePanel.add(pageDescription);

        leftHeader.add(titlePanel, BorderLayout.CENTER);

        header.add(leftHeader, BorderLayout.WEST);

        // ---- Right: notification, name/role, avatar, chevron ----

        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 17));
        userPanel.setOpaque(false);

        userPanel.add(createNotificationIcon(3));

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));

        JLabel name = new JLabel(currentUser != null ? currentUser.getFullName() : "Administrator");
        name.setFont(new Font("Segoe UI", Font.BOLD, 12));
        name.setForeground(TEXT_DARK);
        name.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JLabel role = new JLabel(getUserRole());
        role.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        role.setForeground(TEXT_GRAY);
        role.setAlignmentX(Component.RIGHT_ALIGNMENT);

        info.add(name);
        info.add(role);

        userPanel.add(info);

        userPanel.add(createAvatar(getInitials(), PRIMARY, 38));

        userPanel.add(createVectorIcon("chevron", TEXT_GRAY, 12));

        header.add(userPanel, BorderLayout.EAST);

        return header;
    }

    private JPanel createNotificationIcon(int count) {

        JPanel wrap = new JPanel(null);
        wrap.setOpaque(false);
        wrap.setPreferredSize(new Dimension(30, 34));

        JComponent bell = createVectorIcon("bell", TEXT_DARK, 20);
        bell.setBounds(0, 6, 20, 20);
        wrap.add(bell);

        if (count > 0) {
            JPanel badge = new RoundedPanel(RED, 16);
            badge.setLayout(new GridBagLayout());
            badge.setBounds(15, 0, 16, 16);

            JLabel badgeText = new JLabel(String.valueOf(count));
            badgeText.setFont(new Font("Segoe UI", Font.BOLD, 9));
            badgeText.setForeground(Color.WHITE);
            badge.add(badgeText);

            wrap.add(badge);
            wrap.setComponentZOrder(badge, 0);
        }

        return wrap;
    }

    // =========================================================
    // DASHBOARD
    // =========================================================

    private void showDashboard() {

        pageTitle.setText("Dashboard");
        pageDescription.setText("Tổng quan hoạt động khách sạn");

        contentPanel.removeAll();

        JPanel dashboard = new JPanel(new BorderLayout(0, 20));
        dashboard.setBackground(BACKGROUND);

        dashboard.add(createStatistics(), BorderLayout.NORTH);
        dashboard.add(createDashboardContent(), BorderLayout.CENTER);

        JScrollPane dashboardScroll = new JScrollPane(dashboard,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        dashboardScroll.setBorder(null);
        dashboardScroll.getViewport().setBackground(BACKGROUND);
        dashboardScroll.getVerticalScrollBar().setUnitIncrement(16);

        contentPanel.add(dashboardScroll, BorderLayout.CENTER);

        contentPanel.revalidate();
        contentPanel.repaint();

        loadDashboardData();
    }

    // =========================================================
    // STATISTICS
    // =========================================================

    private JPanel createStatistics() {

        JPanel panel = new JPanel(new GridLayout(1, 4, 18, 0));
        panel.setOpaque(false);

        totalRoomsLabel = new JLabel("0");
        availableRoomsLabel = new JLabel("0");
        occupiedRoomsLabel = new JLabel("0");
        reservationsLabel = new JLabel("0");

        totalRoomsDesc = new JLabel("Tất cả phòng");
        availableRoomsDesc = new JLabel("0% tổng số phòng");
        occupiedRoomsDesc = new JLabel("0% tổng số phòng");
        reservationsDesc = new JLabel("0% tổng số phòng");

        panel.add(createStatCard("TỔNG PHÒNG", totalRoomsLabel, totalRoomsDesc, "building", PRIMARY));
        panel.add(createStatCard("PHÒNG TRỐNG", availableRoomsLabel, availableRoomsDesc, "check", GREEN));
        panel.add(createStatCard("ĐANG SỬ DỤNG", occupiedRoomsLabel, occupiedRoomsDesc, "person", ORANGE));
        panel.add(createStatCard("ĐẶT PHÒNG", reservationsLabel, reservationsDesc, "calendar", BLUE));

        return panel;
    }

    private JPanel createStatCard(String title, JLabel value, JLabel description, String iconType, Color color) {

        JPanel card = new RoundedPanel(Color.WHITE, 16);
        card.setBorder(new EmptyBorder(18, 20, 18, 20));
        card.setLayout(new BorderLayout());

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        titleLabel.setForeground(TEXT_GRAY);

        value.setFont(new Font("Segoe UI", Font.BOLD, 28));
        value.setForeground(color);

        description.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        description.setForeground(TEXT_GRAY);

        left.add(titleLabel);
        left.add(Box.createVerticalStrut(5));
        left.add(value);
        left.add(Box.createVerticalStrut(4));
        left.add(description);

        JPanel iconWrap = new JPanel(new GridBagLayout());
        iconWrap.setOpaque(false);
        iconWrap.setPreferredSize(new Dimension(50, 50));

        JPanel iconCircle = new RoundedPanel(withAlpha(color, 35), 45);
        iconCircle.setLayout(new GridBagLayout());
        iconCircle.setPreferredSize(new Dimension(45, 45));
        iconCircle.add(createVectorIcon(iconType, color, 22));

        iconWrap.add(iconCircle);

        card.add(left, BorderLayout.CENTER);
        card.add(iconWrap, BorderLayout.EAST);

        return card;
    }

    // =========================================================
    // DASHBOARD CONTENT
    // =========================================================

    private JPanel createDashboardContent() {

        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setOpaque(false);

        JPanel topRow = new JPanel(new GridLayout(1, 2, 20, 0));
        topRow.setOpaque(false);

        JPanel roomStatusPanel = createRoomStatusPanel();
        roomStatusPanel.setPreferredSize(new Dimension(0, 430));

        JPanel recentReservationPanel = createRecentReservationPanel();
        recentReservationPanel.setPreferredSize(new Dimension(0, 430));

        topRow.add(roomStatusPanel);
        topRow.add(recentReservationPanel);

        panel.add(topRow, BorderLayout.NORTH);
        panel.add(createBottomDashboardRow(), BorderLayout.CENTER);

        return panel;
    }

    private JPanel createBottomDashboardRow() {

        JPanel row = new JPanel(new GridLayout(1, 2, 20, 0));
        row.setOpaque(false);
        row.add(createRevenuePanel());
        row.add(createRoomStatsChartPanel());

        return row;
    }

    private JPanel createRevenuePanel() {

        JPanel card = createWhiteCard();
        card.setLayout(new BorderLayout(10, 10));
        card.add(createCardTitle("DOANH THU THÁNG NÀY"), BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);

        revenueTotalLabel = new JLabel("0 đ");
        revenueTotalLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        revenueTotalLabel.setForeground(PRIMARY);
        revenueTotalLabel.setBorder(new EmptyBorder(10, 0, 8, 0));

        revenueChangeLabel = new JLabel("↑ 0.0% so với tháng trước");
        revenueChangeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        revenueChangeLabel.setForeground(GREEN);

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.add(revenueTotalLabel);
        textPanel.add(revenueChangeLabel);

        revenueChartPanel = new RevenueChartPanel();

        content.add(textPanel, BorderLayout.NORTH);
        content.add(revenueChartPanel, BorderLayout.CENTER);

        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JPanel createRoomStatsChartPanel() {

        JPanel card = createWhiteCard();
        card.setLayout(new BorderLayout(15, 15));
        card.add(createCardTitle("THỐNG KÊ TÌNH TRẠNG PHÒNG"), BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(10, 0));
        content.setOpaque(false);

        donutChartPanel = new DonutChartPanel();

        JPanel donutWrapper = new JPanel(new GridBagLayout());
        donutWrapper.setOpaque(false);
        donutWrapper.add(donutChartPanel);

        roomLegendPanel = new JPanel();
        roomLegendPanel.setOpaque(false);
        roomLegendPanel.setLayout(new BoxLayout(roomLegendPanel, BoxLayout.Y_AXIS));

        content.add(donutWrapper, BorderLayout.WEST);
        content.add(roomLegendPanel, BorderLayout.CENTER);

        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private void addLegendRow(JPanel parent, String label, int count, double percent, Color color) {

        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);

        JLabel dot = new JLabel("●");
        dot.setForeground(color);
        dot.setFont(new Font("Segoe UI", Font.BOLD, 18));

        JLabel text = new JLabel(label);
        text.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        text.setForeground(TEXT_DARK);

        left.add(dot);
        left.add(text);

        JLabel value = new JLabel(count + " phòng (" + String.format("%.1f", percent) + "%)");
        value.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        value.setForeground(TEXT_GRAY);
        value.setHorizontalAlignment(SwingConstants.RIGHT);

        row.add(left, BorderLayout.WEST);
        row.add(value, BorderLayout.EAST);
        parent.add(row);
        parent.add(Box.createVerticalStrut(8));
    }

    private static class RevenueChartPanel extends JPanel {

        private int[] values = new int[]{0, 0, 0, 0, 0, 0};

        RevenueChartPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(0, 170));
        }

        void setValues(int[] newValues) {
            values = newValues.clone();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {

            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int paddingBottom = 18;
            int paddingLeft = 12;
            int paddingRight = 12;

            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, w, h, 16, 16);

            int maxValue = 1;
            for (int value : values) {
                if (value > maxValue) {
                    maxValue = value;
                }
            }

            int[] xs = new int[values.length];
            int[] ys = new int[values.length];
            for (int i = 0; i < values.length; i++) {
                xs[i] = paddingLeft + (w - paddingLeft - paddingRight) * i / (values.length - 1);
                int graphHeight = h - paddingBottom - 12;
                ys[i] = h - paddingBottom - (int) ((double) values[i] / maxValue * graphHeight);
            }

            int[] fillXs = new int[xs.length + 2];
            int[] fillYs = new int[ys.length + 2];
            System.arraycopy(xs, 0, fillXs, 0, xs.length);
            System.arraycopy(ys, 0, fillYs, 0, ys.length);
            fillXs[xs.length] = xs[xs.length - 1];
            fillYs[ys.length] = h - paddingBottom;
            fillXs[xs.length + 1] = xs[0];
            fillYs[ys.length + 1] = h - paddingBottom;

            GradientPaint gradient = new GradientPaint(
                    0, 0, new Color(105, 78, 210, 70),
                    0, h, new Color(105, 78, 210, 0)
            );
            g2.setPaint(gradient);
            g2.fillPolygon(fillXs, fillYs, fillXs.length);

            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(PRIMARY);
            for (int i = 0; i < xs.length - 1; i++) {
                g2.drawLine(xs[i], ys[i], xs[i + 1], ys[i + 1]);
            }

            g2.setColor(PRIMARY);
            for (int i = 0; i < xs.length; i++) {
                g2.fillOval(xs[i] - 3, ys[i] - 3, 6, 6);
            }
            g2.setColor(Color.WHITE);
            for (int i = 0; i < xs.length; i++) {
                g2.fillOval(xs[i] - 1, ys[i] - 1, 2, 2);
            }

            g2.dispose();
        }
    }

    private static class DonutChartPanel extends JPanel {

        private int[] values = new int[]{0, 0, 0, 0, 0};

        DonutChartPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(180, 180));
        }

        void setValues(int[] newValues) {
            values = newValues.clone();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {

            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int diameter = Math.min(getWidth(), getHeight()) - 20;
            int x = (getWidth() - diameter) / 2;
            int y = (getHeight() - diameter) / 2;

            g2.setStroke(new BasicStroke(18, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));

            int total = 0;
            for (int value : values) {
                total += value;
            }

            int startAngle = 90;
            Color[] colors = {GREEN, ORANGE, PRIMARY, RED, GRAY};

            if (total > 0) {
                for (int i = 0; i < values.length; i++) {
                    int arc = (int) Math.round((values[i] * 360.0) / total);
                    if (arc > 0) {
                        g2.setColor(colors[i]);
                        g2.drawArc(x, y, diameter, diameter, startAngle, -arc);
                        startAngle -= arc;
                    }
                }
            }

            g2.setColor(Color.WHITE);
            g2.fillOval(x + 42, y + 42, diameter - 84, diameter - 84);
            g2.dispose();
        }
    }

    // =========================================================
    // ROOM STATUS
    // =========================================================

    private JPanel createRoomStatusPanel() {

        JPanel card = createWhiteCard();
        card.setLayout(new BorderLayout(0, 12));
        card.setPreferredSize(new Dimension(0, 430));

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        headerRow.add(createCardTitle("TÌNH TRẠNG PHÒNG"), BorderLayout.WEST);

        card.add(headerRow, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);

        body.add(createRoomStatusLegend(), BorderLayout.NORTH);

        JPanel roomContainer = new JPanel();
        roomContainer.setOpaque(false);
        roomContainer.setLayout(new GridLayout(0, 5, 10, 10));

        loadRoomStatus(roomContainer);

        JScrollPane scroll = new JScrollPane(roomContainer);
        scroll.setBorder(null);
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);

        body.add(scroll, BorderLayout.CENTER);

        JPanel viewAllWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 12));
        viewAllWrap.setOpaque(false);
        viewAllWrap.add(createPillButton("Xem tất cả phòng →"));

        body.add(viewAllWrap, BorderLayout.SOUTH);

        card.add(body, BorderLayout.CENTER);

        return card;
    }

    private JPanel createRoomStatusLegend() {

        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        legend.setOpaque(false);

        legend.add(createLegendDot("Trống", GREEN));
        legend.add(createLegendDot("Đang ở", ORANGE));
        legend.add(createLegendDot("Đặt trước", BLUE));
        legend.add(createLegendDot("Bảo trì", RED));
        legend.add(createLegendDot("Dọn dẹp", GRAY));

        return legend;
    }

    private JPanel createLegendDot(String label, Color color) {

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        row.setOpaque(false);

        JLabel dot = new JLabel("●");
        dot.setForeground(color);
        dot.setFont(new Font("Segoe UI", Font.PLAIN, 10));

        JLabel text = new JLabel(label);
        text.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        text.setForeground(TEXT_GRAY);

        row.add(dot);
        row.add(text);

        return row;
    }

    private JPanel createPillButton(String text) {

        JPanel pill = new RoundedPanel(PRIMARY_LIGHT, 18);
        pill.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        pill.setBorder(new EmptyBorder(8, 18, 8, 18));
        pill.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(PRIMARY);

        pill.add(label);
        return pill;
    }

    private void loadRoomStatus(JPanel container) {

        String sql = "SELECT room_number, status " +
                "FROM rooms " +
                "ORDER BY room_number";

        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()
        ) {

            while (rs.next()) {
                String roomNumber = rs.getString("room_number");
                String status = rs.getString("status");
                container.add(createRoomItem(roomNumber, status));
            }

        } catch (SQLException e) {
            showDatabaseError("Không thể tải tình trạng phòng", e);
        }
    }

    private JPanel createRoomItem(String roomNumber, String status) {

        Color color = getRoomStatusColor(status);

        JPanel panel = new RoundedPanel(Color.WHITE, 10);
        panel.setBorder(new EmptyBorder(10, 5, 10, 5));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel icon = new JLabel(getRoomStatusIcon(status));
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        icon.setForeground(color);
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        icon.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel number = new JLabel(roomNumber);
        number.setFont(new Font("Segoe UI", Font.BOLD, 15));
        number.setForeground(TEXT_DARK);
        number.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel statusLabel = new JLabel(translateRoomStatus(status));
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        statusLabel.setForeground(color);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(icon);
        panel.add(Box.createVerticalStrut(4));
        panel.add(number);
        panel.add(Box.createVerticalStrut(2));
        panel.add(statusLabel);

        return panel;
    }

    // =========================================================
    // RECENT RESERVATIONS
    // =========================================================

    private JPanel createRecentReservationPanel() {

        JPanel card = createWhiteCard();
        card.setLayout(new BorderLayout(0, 15));
        card.setPreferredSize(new Dimension(0, 430));

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        headerRow.add(createCardTitle("ĐẶT PHÒNG GẦN ĐÂY"), BorderLayout.WEST);

        JLabel viewAll = new JLabel("Xem tất cả →");
        viewAll.setFont(new Font("Segoe UI", Font.BOLD, 12));
        viewAll.setForeground(PRIMARY);
        viewAll.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        headerRow.add(viewAll, BorderLayout.EAST);

        card.add(headerRow, BorderLayout.NORTH);

        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));

        loadRecentReservations(list);

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);

        card.add(scroll, BorderLayout.CENTER);

        return card;
    }

    private void loadRecentReservations(JPanel container) {

        /*
         * Lấy 5 đơn đặt phòng gần nhất.
         *
         * Quan hệ:
         * reservations
         *      ↓
         * reservation_guests
         *      ↓
         * guests
         */

        String sql = "SELECT " +
                "r.reservation_id, " +
                "r.check_in_date, " +
                "r.status, " +
                "g.full_name " +
                "FROM reservations r " +
                "LEFT JOIN reservation_guests rg " +
                "ON r.reservation_id = rg.reservation_id " +
                "LEFT JOIN guests g " +
                "ON rg.guest_id = g.guest_id " +
                "ORDER BY r.created_at DESC " +
                "LIMIT 5";

        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()
        ) {

            int index = 0;

            while (rs.next()) {

                String guest = rs.getString("full_name");
                Date date = rs.getDate("check_in_date");
                String status = rs.getString("status");

                container.add(createReservationItem(guest, date, status, index));
                index++;
            }

        } catch (SQLException e) {
            showDatabaseError("Không thể tải danh sách đặt phòng", e);
        }
    }

    private JPanel createReservationItem(String guest, Date date, String status, int index) {

        JPanel item = new JPanel(new BorderLayout());
        item.setOpaque(false);
        item.setBorder(new EmptyBorder(8, 5, 8, 5));

        Color avatarColor = AVATAR_PALETTE[index % AVATAR_PALETTE.length];

        JPanel avatarWrap = new JPanel(new GridBagLayout());
        avatarWrap.setOpaque(false);
        avatarWrap.add(createAvatar(getInitials(guest), avatarColor, 38));

        item.add(avatarWrap, BorderLayout.WEST);

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setBorder(new EmptyBorder(0, 10, 0, 0));
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));

        JLabel guestLabel = new JLabel(guest != null ? guest : "Khách hàng");
        guestLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        guestLabel.setForeground(TEXT_DARK);

        JLabel dateLabel = new JLabel(date != null ? date.toString() : "");
        dateLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        dateLabel.setForeground(TEXT_GRAY);

        info.add(guestLabel);
        info.add(dateLabel);

        item.add(info, BorderLayout.CENTER);

        JPanel badgeWrap = new JPanel(new GridBagLayout());
        badgeWrap.setOpaque(false);
        badgeWrap.add(createStatusBadge(
                translateReservationStatus(status),
                getReservationStatusColor(status)
        ));

        item.add(badgeWrap, BorderLayout.EAST);

        return item;
    }

    private JPanel createStatusBadge(String text, Color color) {

        JPanel badge = new RoundedPanel(withAlpha(color, 35), 10);
        badge.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        badge.setBorder(new EmptyBorder(4, 10, 4, 10));

        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 10));
        label.setForeground(color);

        badge.add(label);
        return badge;
    }

    // =========================================================
    // LOAD DASHBOARD DATA FROM MYSQL
    // =========================================================

    private void ensureDatabaseReady() {

        try (Connection conn = DBConnection.getConnection()) {
            conn.setCatalog("hotel_management");

            boolean hasUsers = tableExists(conn, "users");
            boolean hasRooms = tableExists(conn, "rooms");
            boolean hasReservations = tableExists(conn, "reservations");

            if (!hasUsers || !hasRooms || !hasReservations) {
                bootstrapDatabaseFromSqlFile();
                return;
            }

            int userCount = countRows(conn, "users");
            int roomCount = countRows(conn, "rooms");
            int reservationCount = countRows(conn, "reservations");

            if (userCount == 0 || roomCount == 0 || reservationCount == 0) {
                bootstrapDatabaseFromSqlFile();
            }

        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("Unknown database")) {
                bootstrapDatabaseFromSqlFile();
                return;
            }
            System.err.println("Database bootstrap check failed: " + e.getMessage());
        }
    }

    private boolean tableExists(Connection conn, String tableName) throws SQLException {

        String sql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tableName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private int countRows(Connection conn, String tableName) throws SQLException {

        String sql = "SELECT COUNT(*) FROM " + tableName;

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private void bootstrapDatabaseFromSqlFile() {

        Path sqlPath = Paths.get("database", "hotel_management.sql");

        if (!Files.exists(sqlPath)) {
            System.err.println("SQL bootstrap file not found: " + sqlPath.toAbsolutePath());
            return;
        }

        try {
            String sql = Files.readString(sqlPath, StandardCharsets.UTF_8);
            String cleaned = sql
                    .replaceAll("(?s)/\\*.*?\\*/", "")
                    .replaceAll("--.*", "")
                    .replaceAll("(?m)^\\s*USE\\s+.*;?\\s*$", "");

            String[] statements = cleaned.split(";");

            try (Connection conn = DBConnection.getConnection();
                 Statement statement = conn.createStatement()) {

                statement.execute("CREATE DATABASE IF NOT EXISTS hotel_management");
                statement.execute("USE hotel_management");

                for (String part : statements) {
                    String trimmed = part.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("CREATE DATABASE") || trimmed.startsWith("DROP DATABASE")
                            || trimmed.startsWith("USE ")) {
                        continue;
                    }

                    if (trimmed.startsWith("INSERT") || trimmed.startsWith("CREATE TABLE")
                            || trimmed.startsWith("CREATE VIEW") || trimmed.startsWith("ALTER")
                            || trimmed.startsWith("CREATE INDEX") || trimmed.startsWith("INSERT INTO")) {
                        statement.execute(trimmed);
                    }
                }
            }

        } catch (IOException | SQLException e) {
            System.err.println("Failed to bootstrap database from SQL file: " + e.getMessage());
        }
    }

    private void loadDashboardData() {

        SwingUtilities.invokeLater(() -> {
            DashboardData data = dashboardService.getDashboardData();

            int total = data.getTotalRooms();
            int available = data.getAvailableRooms();
            int occupied = data.getOccupiedRooms();
            int reservations = data.getActiveReservations();

            totalRoomsLabel.setText(String.valueOf(total));
            availableRoomsLabel.setText(String.valueOf(available));
            occupiedRoomsLabel.setText(String.valueOf(occupied));
            reservationsLabel.setText(String.valueOf(reservations));

            totalRoomsDesc.setText("Tất cả phòng");
            availableRoomsDesc.setText(formatPercentDesc(available, total));
            occupiedRoomsDesc.setText(formatPercentDesc(occupied, total));
            reservationsDesc.setText(formatPercentDesc(reservations, total));

            int[] roomStatusValues = data.getRoomStatusSummary();
            donutChartPanel.setValues(roomStatusValues);
            refreshRoomLegend(roomStatusValues);

            revenueChartPanel.setValues(data.getRevenueTrend());
            revenueTotalLabel.setText(formatMoney(data.getMonthlyRevenue()));
            revenueChangeLabel.setText(buildRevenueChangeText(data.getMonthlyRevenue(), data.getPreviousMonthRevenue()));
        });
    }

    private int[] loadRoomStatusSummary(Connection conn) throws SQLException {

        String sql = "SELECT status, COUNT(*) FROM rooms GROUP BY status";
        int[] values = new int[]{0, 0, 0, 0, 0};

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String status = rs.getString("status");
                int count = rs.getInt(2);

                switch (status == null ? "" : status.toUpperCase()) {
                    case "AVAILABLE" -> values[0] = count;
                    case "OCCUPIED" -> values[1] = count;
                    case "RESERVED" -> values[2] = count;
                    case "MAINTENANCE" -> values[3] = count;
                    case "CLEANING" -> values[4] = count;
                    default -> {
                    }
                }
            }
        }

        return values;
    }

    private void refreshRoomLegend(int[] values) {

        if (roomLegendPanel == null) {
            return;
        }

        roomLegendPanel.removeAll();

        int total = 0;
        for (int value : values) {
            total += value;
        }

        String[] labels = {"Trống", "Đang ở", "Đặt trước", "Bảo trì", "Dọn dẹp"};
        Color[] colors = {GREEN, ORANGE, PRIMARY, RED, GRAY};

        for (int i = 0; i < labels.length; i++) {
            double percent = total > 0 ? (values[i] * 100.0 / total) : 0.0;
            roomLegendPanel.add(addLegendRow(labels[i], values[i], percent, colors[i]));
            roomLegendPanel.add(Box.createVerticalStrut(8));
        }

        roomLegendPanel.revalidate();
        roomLegendPanel.repaint();
    }

    private JPanel addLegendRow(String label, int count, double percent, Color color) {

        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);

        JLabel dot = new JLabel("●");
        dot.setForeground(color);
        dot.setFont(new Font("Segoe UI", Font.BOLD, 18));

        JLabel text = new JLabel(label);
        text.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        text.setForeground(TEXT_DARK);

        left.add(dot);
        left.add(text);

        JLabel value = new JLabel(count + " phòng (" + String.format(Locale.US, "%.1f", percent) + "%)");
        value.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        value.setForeground(TEXT_GRAY);
        value.setHorizontalAlignment(SwingConstants.RIGHT);

        row.add(left, BorderLayout.WEST);
        row.add(value, BorderLayout.EAST);
        return row;
    }

    private int[] loadRevenueTrend(Connection conn) throws SQLException {

        String sql = "SELECT DATE_FORMAT(issued_at, '%Y-%m') AS month_key, COALESCE(SUM(total_amount), 0) AS total " +
                "FROM invoices " +
                "WHERE issued_at >= DATE_SUB(CURRENT_DATE, INTERVAL 5 MONTH) " +
                "GROUP BY DATE_FORMAT(issued_at, '%Y-%m') " +
                "ORDER BY month_key ASC";

        int[] values = new int[6];
        java.util.Map<String, Integer> monthMap = new java.util.HashMap<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String month = rs.getString("month_key");
                int amount = rs.getInt("total");
                monthMap.put(month, amount);
            }
        }

        java.util.Calendar cal = java.util.Calendar.getInstance();
        for (int i = 5; i >= 0; i--) {
            cal.add(java.util.Calendar.MONTH, -1);
            String key = String.format(Locale.US, "%04d-%02d", cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1);
            values[5 - i] = monthMap.getOrDefault(key, 0);
        }

        return values;
    }

    private long loadMonthlyRevenue(Connection conn) throws SQLException {

        String sql = "SELECT COALESCE(SUM(total_amount), 0) FROM invoices WHERE MONTH(issued_at) = MONTH(CURRENT_DATE) AND YEAR(issued_at) = YEAR(CURRENT_DATE)";

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }

    private long loadPreviousMonthRevenue(Connection conn) throws SQLException {

        String sql = "SELECT COALESCE(SUM(total_amount), 0) FROM invoices WHERE MONTH(issued_at) = MONTH(DATE_SUB(CURRENT_DATE, INTERVAL 1 MONTH)) AND YEAR(issued_at) = YEAR(DATE_SUB(CURRENT_DATE, INTERVAL 1 MONTH))";

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }

    private String formatMoney(long value) {
        return MONEY_FORMAT.format(value) + " đ";
    }

    private String buildRevenueChangeText(long current, long previous) {

        if (previous <= 0) {
            if (current <= 0) {
                return "↑ 0.0% so với tháng trước";
            }
            return "↑ 100.0% so với tháng trước";
        }

        double change = ((current - previous) * 100.0) / previous;
        String sign = change >= 0 ? "↑" : "↓";
        return sign + " " + String.format(Locale.US, "%.1f", Math.abs(change)) + "% so với tháng trước";
    }

    private String formatPercentDesc(int value, int total) {

        if (total <= 0) {
            return "0.0% tổng số phòng";
        }

        double percent = (value * 100.0) / total;
        return String.format("%.1f", percent) + "% tổng số phòng";
    }

    private int executeCount(Connection conn, String sql) throws SQLException {

        try (
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()
        ) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        }

        return 0;
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private static Color withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    // =========================================================
    // VECTOR ICONS (drawn instead of emoji so rendering is
    // crisp and identical on every machine / font setup)
    // =========================================================

    private JComponent createVectorIcon(String type, Color color, int size) {

        JComponent icon = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {

                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);

                float strokeWidth = Math.max(1.5f, getWidth() * 0.09f);
                g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                int s = Math.min(getWidth(), getHeight());

                switch (type) {
                    case "building" -> paintBuilding(g2, s);
                    case "check" -> paintCheck(g2, s);
                    case "person" -> paintPerson(g2, s);
                    case "calendar" -> paintCalendar(g2, s);
                    case "menu" -> paintMenu(g2, s);
                    case "bell" -> paintBell(g2, s);
                    case "chevron" -> paintChevron(g2, s);
                    default -> {
                        // no-op: unknown icon type
                    }
                }

                g2.dispose();
            }
        };

        icon.setPreferredSize(new Dimension(size, size));
        icon.setOpaque(false);
        return icon;
    }

    private void paintBuilding(Graphics2D g2, int s) {

        int pad = (int) (s * 0.16);
        int bx = pad;
        int by = (int) (s * 0.10);
        int bw = s - 2 * pad;
        int bh = s - by - pad;

        g2.drawRoundRect(bx, by, bw, bh, (int) (s * 0.10), (int) (s * 0.10));

        int winW = (int) (bw * 0.20);
        int winH = (int) (bh * 0.12);

        for (int row = 1; row <= 3; row++) {
            for (int col = 1; col <= 2; col++) {
                int wx = bx + (bw * col) / 3 - winW / 2;
                int wy = by + (bh * row) / 5 - winH / 2;
                g2.fillRoundRect(wx, wy, winW, winH, 1, 1);
            }
        }
    }

    private void paintCheck(Graphics2D g2, int s) {

        int[] xs = {(int) (s * 0.20), (int) (s * 0.42), (int) (s * 0.82)};
        int[] ys = {(int) (s * 0.52), (int) (s * 0.72), (int) (s * 0.28)};

        g2.drawPolyline(xs, ys, 3);
    }

    private void paintPerson(Graphics2D g2, int s) {

        int headD = (int) (s * 0.36);
        int headX = s / 2 - headD / 2;
        int headY = (int) (s * 0.10);

        g2.drawOval(headX, headY, headD, headD);

        int bodyW = (int) (s * 0.62);
        int bodyX = s / 2 - bodyW / 2;
        int bodyY = headY + headD + (int) (s * 0.06);
        int bodyH = (int) (s * 0.44);

        g2.drawArc(bodyX, bodyY, bodyW, bodyH * 2, 0, 180);
    }

    private void paintCalendar(Graphics2D g2, int s) {

        int cx = (int) (s * 0.14);
        int cy = (int) (s * 0.22);
        int cw = s - 2 * cx;
        int ch = s - cy - (int) (s * 0.12);

        g2.drawRoundRect(cx, cy, cw, ch, (int) (s * 0.10), (int) (s * 0.10));
        g2.drawLine(cx, cy + (int) (ch * 0.32), cx + cw, cy + (int) (ch * 0.32));

        g2.drawLine(cx + (int) (cw * 0.25), cy - (int) (s * 0.08), cx + (int) (cw * 0.25), cy + (int) (s * 0.04));
        g2.drawLine(cx + (int) (cw * 0.75), cy - (int) (s * 0.08), cx + (int) (cw * 0.75), cy + (int) (s * 0.04));

        int dotSize = Math.max(2, (int) (s * 0.09));
        g2.fillOval(cx + cw / 2 - dotSize / 2, cy + (int) (ch * 0.55), dotSize, dotSize);
    }

    private void paintMenu(Graphics2D g2, int s) {

        int lineW = (int) (s * 0.78);
        int lx = (s - lineW) / 2;
        int spacing = s / 4;

        g2.drawLine(lx, s / 2 - spacing, lx + lineW, s / 2 - spacing);
        g2.drawLine(lx, s / 2, lx + lineW, s / 2);
        g2.drawLine(lx, s / 2 + spacing, lx + lineW, s / 2 + spacing);
    }

    private void paintBell(Graphics2D g2, int s) {

        Path2D path = new Path2D.Double();
        path.moveTo(s * 0.50, s * 0.10);
        path.curveTo(s * 0.24, s * 0.10, s * 0.20, s * 0.44, s * 0.18, s * 0.64);
        path.lineTo(s * 0.82, s * 0.64);
        path.curveTo(s * 0.80, s * 0.44, s * 0.76, s * 0.10, s * 0.50, s * 0.10);
        path.closePath();

        g2.draw(path);
        g2.drawLine((int) (s * 0.34), (int) (s * 0.70), (int) (s * 0.66), (int) (s * 0.70));

        int clapper = Math.max(3, (int) (s * 0.14));
        g2.fillOval(s / 2 - clapper / 2, (int) (s * 0.74), clapper, clapper);
    }

    private void paintChevron(Graphics2D g2, int s) {

        int[] xs = {(int) (s * 0.18), s / 2, (int) (s * 0.82)};
        int[] ys = {(int) (s * 0.32), (int) (s * 0.68), (int) (s * 0.32)};

        g2.drawPolyline(xs, ys, 3);
    }

    // =========================================================
    // AVATAR (perfect circle, drawn instead of an opaque JLabel
    // so it never renders as a square)
    // =========================================================

    private JComponent createAvatar(String initials, Color bgColor, int size) {

        JComponent avatar = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {

                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(bgColor);
                g2.fillOval(0, 0, getWidth(), getHeight());

                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, (int) (getHeight() * 0.34)));

                FontMetrics fm = g2.getFontMetrics();
                int textW = fm.stringWidth(initials);
                int textH = fm.getAscent();

                g2.drawString(
                        initials,
                        (getWidth() - textW) / 2,
                        (getHeight() + textH) / 2 - 2
                );

                g2.dispose();
            }
        };

        avatar.setPreferredSize(new Dimension(size, size));
        avatar.setOpaque(false);
        return avatar;
    }

    private JPanel createWhiteCard() {

        JPanel panel = new RoundedPanel(Color.WHITE, 16);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        return panel;
    }

    private JLabel createCardTitle(String text) {

        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        label.setForeground(TEXT_DARK);

        return label;
    }

    private Color getRoomStatusColor(String status) {

        if (status == null) {
            return GRAY;
        }

        return switch (status.toUpperCase()) {
            case "AVAILABLE" -> GREEN;
            case "OCCUPIED" -> ORANGE;
            case "BOOKED" -> BLUE;
            case "MAINTENANCE" -> RED;
            case "CLEANING" -> GRAY;
            default -> GRAY;
        };
    }

    private String getRoomStatusIcon(String status) {

        if (status == null) {
            return "🛏";
        }

        return switch (status.toUpperCase()) {
            case "AVAILABLE" -> "🛏";
            case "OCCUPIED" -> "👤";
            case "BOOKED" -> "📅";
            case "MAINTENANCE" -> "🔧";
            case "CLEANING" -> "✨";
            default -> "🛏";
        };
    }

    private String translateRoomStatus(String status) {

        if (status == null) {
            return "Không xác định";
        }

        return switch (status.toUpperCase()) {
            case "AVAILABLE" -> "Trống";
            case "OCCUPIED" -> "Đang ở";
            case "BOOKED" -> "Đặt trước";
            case "MAINTENANCE" -> "Bảo trì";
            case "CLEANING" -> "Dọn dẹp";
            default -> status;
        };
    }

    private Color getReservationStatusColor(String status) {

        if (status == null) {
            return GRAY;
        }

        return switch (status.toUpperCase()) {
            case "CONFIRMED" -> GREEN;
            case "PENDING" -> BLUE;
            case "CANCELLED" -> RED;
            default -> GRAY;
        };
    }

    private String translateReservationStatus(String status) {

        if (status == null) {
            return "";
        }

        return switch (status.toUpperCase()) {
            case "CONFIRMED" -> "Đã xác nhận";
            case "PENDING" -> "Đặt trước";
            case "CANCELLED" -> "Đã hủy";
            default -> status;
        };
    }

    private String getInitials() {

        if (currentUser == null || currentUser.getFullName() == null) {
            return "AD";
        }

        return getInitials(currentUser.getFullName());
    }

    private String getInitials(String name) {

        if (name == null || name.trim().isEmpty()) {
            return "?";
        }

        String[] parts = name.trim().split("\\s+");

        if (parts.length == 1) {
            return parts[0]
                    .substring(0, Math.min(2, parts[0].length()))
                    .toUpperCase();
        }

        return (parts[0].charAt(0) + "" + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    private String getUserRole() {

        if (currentUser == null || currentUser.getRole() == null) {
            return "USER";
        }

        return currentUser.getRole().toString();
    }

    // =========================================================
    // MODULE
    // =========================================================

    private void openModule(String module) {

        JOptionPane.showMessageDialog(
                this,
                module + "\n\nModule đang được phát triển.",
                "Hotel Management",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    // =========================================================
    // LOGOUT
    // =========================================================

    private void logout() {

        int result = JOptionPane.showConfirmDialog(
                this,
                "Bạn có chắc muốn đăng xuất?",
                "Đăng xuất",
                JOptionPane.YES_NO_OPTION
        );

        if (result == JOptionPane.YES_OPTION) {
            dispose();
            SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
        }
    }

    // =========================================================
    // DATABASE ERROR
    // =========================================================

    private void showDatabaseError(String message, SQLException e) {
        System.err.println(message + ": " + e.getMessage());
    }

    // =========================================================
    // ROUNDED PANEL
    // =========================================================

    private static class RoundedPanel extends JPanel {

        private final Color backgroundColor;
        private final int radius;

        public RoundedPanel(Color backgroundColor, int radius) {
            this.backgroundColor = backgroundColor;
            this.radius = radius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(backgroundColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();

            super.paintComponent(g);
        }
    }
}