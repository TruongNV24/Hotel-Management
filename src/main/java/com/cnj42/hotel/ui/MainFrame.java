package com.cnj42.hotel.ui;

import com.cnj42.hotel.model.User;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    private final User currentUser;

    public MainFrame(User user) {

        this.currentUser = user;

        setTitle("Hotel Management");
        setSize(1000, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initUI();
    }

    private void initUI() {

        JPanel mainPanel = new JPanel(new BorderLayout());

        mainPanel.add(createHeader(), BorderLayout.NORTH);

        mainPanel.add(createContent(), BorderLayout.CENTER);

        mainPanel.add(createFooter(), BorderLayout.SOUTH);

        add(mainPanel);
    }

    private JPanel createHeader() {

        JPanel panel = new JPanel(new BorderLayout());

        panel.setBorder(
                BorderFactory.createEmptyBorder(
                        15, 20, 15, 20
                )
        );

        JLabel titleLabel = new JLabel(
                "HOTEL MANAGEMENT SYSTEM"
        );

        titleLabel.setFont(
                new Font("Arial", Font.BOLD, 22)
        );

        JLabel userLabel = new JLabel(
                currentUser.getFullName()
                        + " | "
                        + currentUser.getRole()
        );

        panel.add(titleLabel, BorderLayout.WEST);
        panel.add(userLabel, BorderLayout.EAST);

        return panel;
    }

    private JPanel createContent() {

        JPanel panel = new JPanel(
                new GridBagLayout()
        );

        JLabel welcomeLabel = new JLabel(
                "Chào mừng "
                        + currentUser.getFullName()
                        + " đến với hệ thống quản lý khách sạn"
        );

        welcomeLabel.setFont(
                new Font("Arial", Font.PLAIN, 18)
        );

        panel.add(welcomeLabel);

        return panel;
    }

    private JPanel createFooter() {

        JPanel panel = new JPanel(
                new FlowLayout(FlowLayout.RIGHT)
        );

        JButton logoutButton = new JButton("Đăng xuất");

        logoutButton.addActionListener(e -> logout());

        panel.add(logoutButton);

        return panel;
    }

    private void logout() {

        int result = JOptionPane.showConfirmDialog(
                this,
                "Bạn có chắc muốn đăng xuất?",
                "Đăng xuất",
                JOptionPane.YES_NO_OPTION
        );

        if (result == JOptionPane.YES_OPTION) {

            dispose();

            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
        }
    }
}