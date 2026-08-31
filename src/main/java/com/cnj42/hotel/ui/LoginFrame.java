package com.cnj42.hotel.ui;

import com.cnj42.hotel.model.User;
import com.cnj42.hotel.service.AuthService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

public class LoginFrame extends JFrame {

    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JButton loginButton;
        private final JLabel statusLabel;

    private final AuthService authService;

    public LoginFrame() {

        authService = new AuthService();

        setTitle("Hotel Management - Login");
        setSize(1024, 768);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        GradientPanel background = new GradientPanel();

        background.setLayout(new GridBagLayout());

        RoundedPanel card = new RoundedPanel(
                new Color(255, 255, 255),
                16
        );

        card.setPreferredSize(new Dimension(890, 626));
        card.setLayout(new GridLayout(1, 2));

        IllustrationPanel illustrationPanel = new IllustrationPanel();

        card.add(illustrationPanel);

        JPanel formPanel = new JPanel();
        formPanel.setOpaque(false);
        formPanel.setBorder(
                new EmptyBorder(60, 54, 50, 54)
        );

        formPanel.setLayout(
                new BoxLayout(formPanel, BoxLayout.Y_AXIS)
        );

        JLabel brandLabel = new JLabel("▥");
        brandLabel.setFont(new Font("Segoe UI Symbol", Font.BOLD, 27));
        brandLabel.setForeground(Color.WHITE);
        brandLabel.setHorizontalAlignment(SwingConstants.CENTER);
        brandLabel.setOpaque(true);
        brandLabel.setBackground(new Color(105, 79, 229));
        brandLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        brandLabel.setPreferredSize(new Dimension(50, 52));
        brandLabel.setMaximumSize(new Dimension(50, 52));

        JLabel titleLabel = new JLabel("Member Login");

        titleLabel.setFont(
                new Font("Segoe UI", Font.BOLD, 24)
        );

        titleLabel.setForeground(
                new Color(40, 40, 40)
        );

        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Please sign in to your account");

        subtitleLabel.setFont(
                new Font("Segoe UI", Font.PLAIN, 13)
        );

        subtitleLabel.setForeground(
                new Color(140, 140, 140)
        );

        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        formPanel.add(brandLabel);
        formPanel.add(Box.createVerticalStrut(18));
        formPanel.add(titleLabel);

        formPanel.add(Box.createVerticalStrut(8));

        formPanel.add(subtitleLabel);

        formPanel.add(Box.createVerticalStrut(29));

        usernameField = new RoundedTextField("Username");
        usernameField.setPreferredSize(new Dimension(343, 55));
        usernameField.setMinimumSize(new Dimension(343, 55));
        usernameField.setMaximumSize(new Dimension(343, 55));
        usernameField.setAlignmentX(Component.CENTER_ALIGNMENT);

        formPanel.add(usernameField);

        formPanel.add(Box.createVerticalStrut(17));

        passwordField = new RoundedPasswordField("Password");
        passwordField.setPreferredSize(new Dimension(343, 55));
        passwordField.setMinimumSize(new Dimension(343, 55));
        passwordField.setMaximumSize(new Dimension(343, 55));
        passwordField.setAlignmentX(Component.CENTER_ALIGNMENT);

        formPanel.add(passwordField);

        formPanel.add(Box.createVerticalStrut(16));

        JPanel optionsPanel = new JPanel(new BorderLayout());
        optionsPanel.setOpaque(false);
        optionsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        JCheckBox rememberBox = new JCheckBox("Remember me");
        rememberBox.setOpaque(false);
        rememberBox.setFocusPainted(false);
        rememberBox.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        rememberBox.setForeground(new Color(125, 125, 150));
        JLabel forgotLabel = new JLabel("Forgot password?");
        forgotLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        forgotLabel.setForeground(new Color(105, 79, 229));
        optionsPanel.add(rememberBox, BorderLayout.WEST);
        optionsPanel.add(forgotLabel, BorderLayout.EAST);
        formPanel.add(optionsPanel);
        formPanel.add(Box.createVerticalStrut(14));

        loginButton = new RoundedButton("➜  LOGIN");

        loginButton.setPreferredSize(
                new Dimension(100, 42)
        );

        loginButton.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 42)
        );

        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        formPanel.add(loginButton);

        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setPreferredSize(new Dimension(343, 24));
        statusLabel.setMaximumSize(new Dimension(343, 24));
        formPanel.add(statusLabel);

        formPanel.add(Box.createVerticalStrut(22));
        formPanel.add(new SeparatorPanel());
        formPanel.add(Box.createVerticalStrut(18));
        JButton createAccountButton = new OutlineButton("♙  Create New Account");
        createAccountButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        createAccountButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        formPanel.add(createAccountButton);

        card.add(formPanel);

        background.add(card);

        add(background);

        loginButton.addActionListener(e -> handleLogin());

        passwordField.addActionListener(e -> handleLogin());

        getRootPane().setDefaultButton(loginButton);

        SwingUtilities.invokeLater(
                () -> usernameField.requestFocusInWindow()
        );
    }

    private void handleLogin() {

        String username = usernameField.getText().trim();

        String password = new String(
                passwordField.getPassword()
        );

        if (username.isEmpty() || password.isEmpty()) {
                        showStatus("Please enter username and password.", new Color(205, 116, 30));
            return;
        }

        loginButton.setEnabled(false);

        try {

            User user = authService.login(
                    username,
                    password
            );

            if (user == null) {
                showStatus("Username or password is incorrect.", new Color(205, 55, 85));
                passwordField.setText("");
                passwordField.requestFocusInWindow();

                return;
            }

            dispose();

            MainFrame mainFrame = new MainFrame(user);
            mainFrame.setVisible(true);

        } finally {

            loginButton.setEnabled(true);
        }
    }

        private void showStatus(String message, Color color) {
                statusLabel.setText(message);
                statusLabel.setForeground(color);
                Timer timer = new Timer(3500, event -> statusLabel.setText(" "));
                timer.setRepeats(false);
                timer.start();
        }

    public static void main(String[] args) {

        try {
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName()
            );
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {

            LoginFrame loginFrame = new LoginFrame();

            loginFrame.setVisible(true);
        });
    }

    private static class GradientPanel extends JPanel {

        @Override
        protected void paintComponent(Graphics g) {

            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY
            );

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            int width = getWidth();
            int height = getHeight();

            GradientPaint gradient = new GradientPaint(
                    0,
                    0,
                    new Color(105, 80, 205),
                    width,
                    height,
                    new Color(180, 75, 190)
            );

            g2.setPaint(gradient);
            g2.fillRect(0, 0, width, height);

            g2.dispose();
        }
    }

    private static class RoundedPanel extends JPanel {

        private final Color backgroundColor;
        private final int radius;

        public RoundedPanel(
                Color backgroundColor,
                int radius
        ) {
            this.backgroundColor = backgroundColor;
            this.radius = radius;

            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {

            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            g2.setColor(backgroundColor);

            g2.fill(
                    new RoundRectangle2D.Double(
                            0,
                            0,
                            getWidth(),
                            getHeight(),
                            radius,
                            radius
                    )
            );

            g2.dispose();

            super.paintComponent(g);
        }
    }

    private static class RoundedTextField extends JTextField {

        private final String placeholder;

        public RoundedTextField(String placeholder) {

            this.placeholder = placeholder;

            setFont(
                    new Font("Segoe UI", Font.PLAIN, 13)
            );

            setForeground(
                    new Color(70, 70, 70)
            );

            setBorder(new EmptyBorder(0, 48, 0, 15));

            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {

            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            g2.setColor(new Color(252, 252, 255));
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            g2.setColor(new Color(224, 224, 235));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            g2.setColor(new Color(245, 242, 255));
            g2.fillRoundRect(10, 9, 34, 34, 9, 9);
            g2.setColor(new Color(109, 82, 231));
            g2.fillOval(22, 15, 10, 10);
            g2.fillRoundRect(18, 27, 18, 10, 6, 6);

            if (getText().isEmpty()
                    && !hasFocus()) {

                g2.setColor(
                        new Color(160, 160, 160)
                );

                g2.setFont(
                        new Font(
                                "Segoe UI",
                                Font.PLAIN,
                                12
                        )
                );

                g2.drawString(
                        placeholder,
                        48,
                        getHeight() / 2 + 5
                );
            }

            g2.dispose();

            super.paintComponent(g);
        }
    }

    private static class RoundedPasswordField
            extends JPasswordField {

        private final String placeholder;

        public RoundedPasswordField(String placeholder) {

            this.placeholder = placeholder;

            setFont(
                    new Font("Segoe UI", Font.PLAIN, 13)
            );

            setForeground(
                    new Color(70, 70, 70)
            );

            setBorder(new EmptyBorder(0, 48, 0, 15));

            setOpaque(false);

            setEchoChar('•');
        }

        @Override
        protected void paintComponent(Graphics g) {

            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            g2.setColor(new Color(252, 252, 255));
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            g2.setColor(new Color(224, 224, 235));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            g2.setColor(new Color(245, 242, 255));
            g2.fillRoundRect(10, 9, 34, 34, 9, 9);
            g2.setColor(new Color(109, 82, 231));
            g2.fillRoundRect(22, 16, 10, 15, 3, 3);
            g2.drawLine(19, 31, 35, 31);

            if (getPassword().length == 0
                    && !hasFocus()) {

                g2.setColor(
                        new Color(160, 160, 160)
                );

                g2.setFont(
                        new Font(
                                "Segoe UI",
                                Font.PLAIN,
                                12
                        )
                );

                g2.drawString(
                        placeholder,
                        48,
                        getHeight() / 2 + 5
                );
            }

            g2.dispose();

            super.paintComponent(g);
        }
    }

    private static class RoundedButton
            extends JButton {

        private boolean hover = false;

        public RoundedButton(String text) {

            super(text);

            setFont(
                    new Font(
                            "Segoe UI",
                            Font.BOLD,
                            12
                    )
            );

            setForeground(Color.WHITE);

            setBorderPainted(false);
            setFocusPainted(false);
            setContentAreaFilled(false);

            setCursor(
                    new Cursor(Cursor.HAND_CURSOR)
            );

            addMouseListener(
                    new java.awt.event.MouseAdapter() {

                        @Override
                        public void mouseEntered(
                                java.awt.event.MouseEvent e
                        ) {
                            hover = true;
                            repaint();
                        }

                        @Override
                        public void mouseExited(
                                java.awt.event.MouseEvent e
                        ) {
                            hover = false;
                            repaint();
                        }
                    }
            );
        }

        @Override
        protected void paintComponent(Graphics g) {

            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            Color buttonColor;

            if (!isEnabled()) {

                buttonColor =
                        new Color(180, 180, 180);

            } else if (hover) {

                buttonColor =
                        new Color(177, 71, 220);

            } else {

                buttonColor =
                        new Color(91, 83, 235);
            }

            g2.setColor(buttonColor);

            g2.fillRoundRect(
                    0,
                    0,
                    getWidth(),
                    getHeight(),
                    12,
                    12
            );

            g2.dispose();

            super.paintComponent(g);
        }
    }

    private static class IllustrationPanel
            extends JPanel {

        public IllustrationPanel() {

            setOpaque(false);

            setPreferredSize(
                    new Dimension(380, 450)
            );
        }

        @Override
        protected void paintComponent(Graphics g) {

            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            int centerX = getWidth() / 2;
            int centerY = getHeight() / 2;

            g2.setColor(new Color(244, 237, 255));

            g2.fill(
                    new Ellipse2D.Double(
                            centerX - 131, centerY - 131, 262, 262
                    )
            );

            int buildingX = centerX - 78;
            int buildingY = centerY - 65;
            g2.setColor(new Color(110, 95, 216));
            g2.fillRect(buildingX, buildingY, 156, 120);
            g2.setColor(new Color(151, 139, 235));
            g2.fillRect(buildingX - 26, buildingY + 28, 208, 92);
            g2.setColor(new Color(130, 116, 225));
            g2.fillRect(buildingX - 34, buildingY + 22, 224, 8);
            g2.setColor(new Color(92, 73, 177));
            g2.fillRect(centerX - 36, buildingY - 8, 72, 35);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
            g2.drawString("HOTEL", centerX - 29, buildingY + 15);
            g2.setColor(new Color(232, 228, 255));
            for (int row = 0; row < 2; row++) {
                for (int column = -2; column <= 2; column++) {
                    g2.fillRect(centerX + column * 32 - 9, buildingY + 49 + row * 31, 18, 20);
                }
            }
            g2.setColor(new Color(57, 77, 177));
            g2.fillRect(centerX - 18, buildingY + 84, 36, 36);
            g2.setColor(new Color(91, 193, 106));
            g2.fillOval(centerX - 112, centerY + 34, 19, 47);
            g2.fillOval(centerX + 93, centerY + 34, 19, 47);
            g2.setColor(new Color(78, 159, 92));
            g2.fillRect(centerX - 104, centerY + 68, 4, 26);
            g2.fillRect(centerX + 101, centerY + 68, 4, 26);
            g2.setColor(new Color(73, 150, 91));
            g2.fillRoundRect(centerX - 125, centerY + 84, 250, 9, 5, 5);

            g2.setColor(new Color(56, 166, 255));

            g2.fillOval(
                    centerX - 125,
                    centerY - 105,
                    7,
                    7
            );

            g2.fillOval(
                    centerX + 105,
                    centerY + 95,
                    6,
                    6
            );

            g2.setColor(new Color(214, 93, 232));

            Polygon triangle = new Polygon();

            triangle.addPoint(centerX + 112, centerY - 72);
            triangle.addPoint(centerX + 112, centerY - 52);
            triangle.addPoint(centerX + 128, centerY - 62);

            g2.drawPolygon(triangle);

                        g2.setColor(new Color(61, 45, 126));
                        g2.setFont(new Font("Segoe UI", Font.BOLD, 24));
                        String welcome = "WELCOME BACK!";
                        g2.drawString(welcome, centerX - g2.getFontMetrics().stringWidth(welcome) / 2, centerY + 190);
                        g2.setColor(new Color(125, 125, 155));
                        g2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                        String message = "Sign in to continue to Hotel Management System";
                        g2.drawString(message, centerX - g2.getFontMetrics().stringWidth(message) / 2, centerY + 219);

            g2.dispose();
        }
    }

        private static class OutlineButton extends JButton {
                OutlineButton(String text) {
                        super(text);
                        setFont(new Font("Segoe UI", Font.PLAIN, 13));
                        setForeground(new Color(105, 79, 229));
                        setBorder(BorderFactory.createLineBorder(new Color(219, 205, 255), 1, true));
                        setContentAreaFilled(false);
                        setFocusPainted(false);
                        setCursor(new Cursor(Cursor.HAND_CURSOR));
                }
        }

        private static class SeparatorPanel extends JPanel {
                SeparatorPanel() {
                        setOpaque(false);
                        setPreferredSize(new Dimension(1, 28));
                }

                @Override
                protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setColor(new Color(235, 233, 242));
                        g2.drawLine(0, 14, getWidth() / 2 - 22, 14);
                        g2.drawLine(getWidth() / 2 + 22, 14, getWidth(), 14);
                        g2.setColor(new Color(225, 225, 235));
                        g2.drawOval(getWidth() / 2 - 16, 0, 32, 28);
                        g2.setColor(new Color(156, 156, 180));
                        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                        g2.drawString("OR", getWidth() / 2 - 9, 18);
                        g2.dispose();
                }
        }
}