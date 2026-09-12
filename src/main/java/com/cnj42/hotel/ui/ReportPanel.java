package com.cnj42.hotel.ui;

import com.cnj42.hotel.model.ReportData;
import com.cnj42.hotel.service.ReportService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.time.Year;
import java.text.DecimalFormat;

public class ReportPanel extends JPanel {

    private static final Color BACKGROUND = new Color(246, 247, 251);
    private static final Color PRIMARY = new Color(105, 78, 210);
    private static final Color TEXT_DARK = new Color(35, 40, 52);
    private static final Color TEXT_GRAY = new Color(120, 125, 140);
    private static final DecimalFormat MONEY = new DecimalFormat("#,##0");

    private final ReportService reportService = new ReportService();
    private ReportData currentReport;
    private final JComboBox<Integer> yearBox = new JComboBox<>();
    private final JComboBox<String> monthBox = new JComboBox<>();
    private final RevenueBarChart chart = new RevenueBarChart();
    private final JLabel revenueValue = new JLabel("0 đ");
    private final JLabel invoiceValue = new JLabel("0");
    private final JLabel paidValue = new JLabel("0 đ");
    private final JLabel reservationValue = new JLabel("0");
    private final DefaultTableModel revenueModel = createModel("Tháng", "Số hóa đơn", "Doanh thu");
    private final DefaultTableModel reservationModel = createModel("Trạng thái", "Số đặt phòng", "Tỷ lệ");
    private final DefaultTableModel roomModel = createModel("Trạng thái phòng", "Số phòng");
    private final JLabel statusLabel = new JLabel(" ");

    public ReportPanel() {
        setLayout(new BorderLayout(0, 16));
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(4, 0, 0, 0));

        JPanel header = new JPanel(new BorderLayout(12, 8));
        header.setOpaque(false);
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JLabel title = new JLabel("Tổng quan báo cáo");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(TEXT_DARK);
        heading.add(title, BorderLayout.WEST);
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        filters.setOpaque(false);
        for (int year = Year.now().getValue(); year >= Year.now().getValue() - 5; year--) yearBox.addItem(year);
        monthBox.addItem("Cả năm");
        for (int month = 1; month <= 12; month++) monthBox.addItem("Tháng " + month);
        JButton refresh = new JButton("↻  Xem báo cáo");
        JButton export = new JButton("Xuất Excel");
        refresh.addActionListener(e -> loadReport());
        export.addActionListener(e -> exportReport());
        filters.add(new JLabel("Năm:"));
        filters.add(yearBox);
        filters.add(new JLabel("Kỳ xem:"));
        filters.add(monthBox);
        filters.add(refresh);
        filters.add(export);
        header.add(heading, BorderLayout.WEST);
        header.add(filters, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.add(createSummary());
        body.add(Box.createVerticalStrut(16));
        body.add(createChartCard());
        body.add(Box.createVerticalStrut(16));

        JPanel tables = new JPanel(new GridLayout(1, 3, 16, 0));
        tables.setOpaque(false);
        tables.add(createTableCard("DOANH THU THEO KỲ", revenueModel));
        tables.add(createTableCard("TÌNH TRẠNG ĐẶT PHÒNG", reservationModel));
        tables.add(createTableCard("TÌNH TRẠNG PHÒNG HIỆN TẠI", roomModel));
        tables.setPreferredSize(new Dimension(0, 260));
        tables.setMinimumSize(new Dimension(900, 260));
        statusLabel.setForeground(TEXT_GRAY);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        JPanel footer = new JPanel(new BorderLayout(0, 6));
        footer.setOpaque(false);
        footer.add(tables, BorderLayout.CENTER);
        footer.add(statusLabel, BorderLayout.SOUTH);
        body.add(footer);

        JScrollPane scrollPane = new JScrollPane(body,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(BACKGROUND);
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        add(scrollPane, BorderLayout.CENTER);

        loadReport();
    }

    private JPanel createChartCard() {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(Color.WHITE);
        card.setBorder(new EmptyBorder(14, 16, 10, 16));
        card.setPreferredSize(new Dimension(0, 390));
        card.setMinimumSize(new Dimension(900, 390));
        JLabel title = new JLabel("BIỂU ĐỒ DOANH THU");
        title.setFont(new Font("Segoe UI", Font.BOLD, 11));
        title.setForeground(TEXT_GRAY);
        card.add(title, BorderLayout.NORTH);
        card.add(chart, BorderLayout.CENTER);
        return card;
    }

    private JPanel createSummary() {
        JPanel summary = new JPanel(new GridLayout(1, 4, 14, 0));
        summary.setOpaque(false);
        summary.add(createMetric("DOANH THU KỲ ĐÃ CHỌN", revenueValue, PRIMARY));
        summary.add(createMetric("TỔNG HÓA ĐƠN", invoiceValue, new Color(65, 135, 235)));
        summary.add(createMetric("ĐÃ THANH TOÁN", paidValue, new Color(35, 181, 118)));
        summary.add(createMetric("ĐẶT PHÒNG HOÀN TẤT", reservationValue, new Color(245, 153, 55)));
        return summary;
    }

    private JPanel createMetric(String caption, JLabel value, Color color) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(Color.WHITE);
        card.setBorder(new EmptyBorder(16, 18, 16, 18));
        JLabel label = new JLabel(caption);
        label.setFont(new Font("Segoe UI", Font.BOLD, 10));
        label.setForeground(TEXT_GRAY);
        value.setFont(new Font("Segoe UI", Font.BOLD, 24));
        value.setForeground(color);
        card.add(label, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    private JPanel createTableCard(String title, DefaultTableModel model) {
        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(Color.WHITE);
        card.setBorder(new EmptyBorder(16, 16, 16, 16));
        JLabel heading = new JLabel(title);
        heading.setFont(new Font("Segoe UI", Font.BOLD, 11));
        heading.setForeground(TEXT_GRAY);
        JTable table = new JTable(model);
        table.setRowHeight(34);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(235, 236, 242));
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        table.getTableHeader().setForeground(TEXT_GRAY);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(235, 236, 242)));
        card.add(heading, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private void loadReport() {
        revenueModel.setRowCount(0);
        reservationModel.setRowCount(0);
        roomModel.setRowCount(0);
        int year = (Integer) yearBox.getSelectedItem();
        Integer month = monthBox.getSelectedIndex() == 0 ? null : monthBox.getSelectedIndex();
        statusLabel.setText("Đang tải dữ liệu báo cáo...");

        new SwingWorker<ReportData, Void>() {
            @Override
            protected ReportData doInBackground() {
                return reportService.getReportData(year, month);
            }

            @Override
            protected void done() {
                try {
                    updateReport(doneReport(), month);
                } catch (Exception exception) {
                    statusLabel.setText("Không thể tải dữ liệu báo cáo: " + exception.getMessage());
                }
            }

            private ReportData doneReport() throws Exception {
                return get();
            }
        }.execute();
    }

    private void updateReport(ReportData report, Integer month) {
        currentReport = report;
        revenueValue.setText(formatMoney(report.getTotalRevenue()));
        invoiceValue.setText(String.valueOf(report.getInvoiceCount()));
        paidValue.setText(formatMoney(report.getPaidRevenue()));
        reservationValue.setText(String.valueOf(report.getCompletedReservations()));

        for (ReportData.RevenueRow row : report.getRevenueRows()) {
            revenueModel.addRow(new Object[]{month == null ? "Tháng " + row.getMonth() : "Ngày " + row.getMonth(), row.getInvoiceCount(), formatMoney(row.getRevenue())});
        }
        for (ReportData.ReservationStatusRow row : report.getReservationStatusRows()) {
            reservationModel.addRow(new Object[]{row.getStatus(), row.getCount(), String.format("%.1f%%", row.getPercentage())});
        }
        for (ReportData.RoomStatusRow row : report.getRoomStatusRows()) {
            roomModel.addRow(new Object[]{row.getStatus(), row.getCount()});
        }
        chart.setData(report.getRevenueRows(), month == null ? "Doanh thu theo tháng" : "Doanh thu theo ngày");

        if (report.getErrorMessage() == null) {
            statusLabel.setText("Cập nhật lúc " + new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date()));
        } else {
            statusLabel.setText("Không thể tải dữ liệu báo cáo: " + report.getErrorMessage());
        }
    }

    private void exportReport() {
        if (currentReport == null || currentReport.getErrorMessage() != null) {
            JOptionPane.showMessageDialog(this, "Dữ liệu báo cáo chưa tải xong.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int year = currentReport.getYear();
        Integer month = currentReport.getMonth();
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("bao-cao-doanh-thu-" + year + (month == null ? "" : "-" + month) + ".xlsx"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File outputFile = chooser.getSelectedFile();
            if (!outputFile.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".xlsx")) {
                outputFile = new File(outputFile.getAbsolutePath() + ".xlsx");
            }
            File selectedFile = outputFile;
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            new SwingWorker<Void, Void>() {
                private Throwable failure;

                @Override
                protected Void doInBackground() {
                    try {
                        reportService.exportToExcel(currentReport, selectedFile);
                    } catch (Throwable throwable) {
                        failure = throwable;
                    }
                    return null;
                }

                @Override
                protected void done() {
                    setCursor(Cursor.getDefaultCursor());
                    if (failure == null) {
                        JOptionPane.showMessageDialog(ReportPanel.this,
                                "Đã xuất báo cáo Excel thành công:\n" + selectedFile.getAbsolutePath());
                    } else {
                        JOptionPane.showMessageDialog(ReportPanel.this,
                                "Không thể xuất Excel: " + failure.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                        failure.printStackTrace();
                    }
                }
            }.execute();
        }
    }

    private static DefaultTableModel createModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private static String formatMoney(java.math.BigDecimal amount) {
        return MONEY.format(amount == null ? java.math.BigDecimal.ZERO : amount) + " đ";
    }

    private static class RevenueBarChart extends JPanel {
        private java.util.List<ReportData.RevenueRow> rows = java.util.Collections.emptyList();
        private String title = "";

        RevenueBarChart() {
            setOpaque(false);
            setPreferredSize(new Dimension(1050, 340));
        }

        void setData(java.util.List<ReportData.RevenueRow> rows, String title) {
            this.rows = new java.util.ArrayList<>(rows);
            this.title = title;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int left = 78, bottom = 48, top = 18, right = 18;
            int width = Math.max(1, getWidth() - left - right);
            int height = Math.max(1, getHeight() - top - bottom);
            java.math.BigDecimal max = java.math.BigDecimal.ONE;
            for (ReportData.RevenueRow row : rows) if (row.getRevenue().compareTo(max) > 0) max = row.getRevenue();
            int slot = rows.isEmpty() ? width : Math.max(1, width / rows.size());
            g.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            for (int line = 0; line <= 5; line++) {
                int y = top + height - (height * line / 5);
                g.setColor(new Color(225, 226, 234));
                g.drawLine(left, y, left + width, y);
                g.setColor(TEXT_GRAY);
                java.math.BigDecimal axisValue = max.multiply(java.math.BigDecimal.valueOf(line))
                        .divide(java.math.BigDecimal.valueOf(5), 0, java.math.RoundingMode.HALF_UP);
                g.drawString(formatAxisMoney(axisValue), 4, y + 4);
            }
            g.setColor(new Color(150, 152, 165));
            g.drawLine(left, top, left, top + height);
            g.drawLine(left, top + height, left + width, top + height);
            g.setColor(TEXT_DARK);
            g.setFont(new Font("Segoe UI", Font.BOLD, 11));
            g.drawString("Doanh thu (VNĐ)", 4, top - 4);
            g.drawString(title, left + width / 2 - 50, getHeight() - 4);

            for (int i = 0; i < rows.size(); i++) {
                ReportData.RevenueRow row = rows.get(i);
                int barHeight = row.getRevenue().multiply(java.math.BigDecimal.valueOf(height)).divide(max, 0, java.math.RoundingMode.HALF_UP).intValue();
                int barWidth = Math.max(6, slot - 10);
                int x = left + i * slot + 5;
                int y = top + height - barHeight;
                g.setColor(PRIMARY);
                g.fillRoundRect(x, y, barWidth, barHeight, 6, 6);
                g.setColor(TEXT_GRAY);
                g.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                String label = row.getMonth();
                int labelOffset = Math.max(0, (barWidth - g.getFontMetrics().stringWidth(label)) / 2);
                g.drawString(label, x + labelOffset, top + height + 18);
            }
            if (rows.isEmpty()) {
                g.setColor(TEXT_GRAY);
                g.drawString("Chưa có dữ liệu doanh thu", left + 12, top + height / 2);
            }
            g.dispose();
        }

        private static String formatAxisMoney(java.math.BigDecimal value) {
            if (value.compareTo(java.math.BigDecimal.valueOf(1_000_000)) >= 0) {
                return value.divide(java.math.BigDecimal.valueOf(1_000_000), 1, java.math.RoundingMode.HALF_UP) + " tr";
            }
            if (value.compareTo(java.math.BigDecimal.valueOf(1_000)) >= 0) {
                return value.divide(java.math.BigDecimal.valueOf(1_000), 0, java.math.RoundingMode.HALF_UP) + "k";
            }
            return value.toPlainString();
        }
    }
}