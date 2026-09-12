package com.cnj42.hotel.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ReportData {

    private BigDecimal totalRevenue = BigDecimal.ZERO;
    private int invoiceCount;
    private BigDecimal paidRevenue = BigDecimal.ZERO;
    private int completedReservations;
    private String errorMessage;
    private int year;
    private Integer month;
    private final List<RevenueRow> revenueRows = new ArrayList<>();
    private final List<ReservationStatusRow> reservationStatusRows = new ArrayList<>();
    private final List<RoomStatusRow> roomStatusRows = new ArrayList<>();
    private final List<InvoiceDetailRow> invoiceDetailRows = new ArrayList<>();

    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
    public int getInvoiceCount() { return invoiceCount; }
    public void setInvoiceCount(int invoiceCount) { this.invoiceCount = invoiceCount; }
    public BigDecimal getPaidRevenue() { return paidRevenue; }
    public void setPaidRevenue(BigDecimal paidRevenue) { this.paidRevenue = paidRevenue; }
    public int getCompletedReservations() { return completedReservations; }
    public void setCompletedReservations(int completedReservations) { this.completedReservations = completedReservations; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public Integer getMonth() { return month; }
    public void setMonth(Integer month) { this.month = month; }
    public List<RevenueRow> getRevenueRows() { return revenueRows; }
    public List<ReservationStatusRow> getReservationStatusRows() { return reservationStatusRows; }
    public List<RoomStatusRow> getRoomStatusRows() { return roomStatusRows; }
    public List<InvoiceDetailRow> getInvoiceDetailRows() { return invoiceDetailRows; }

    public static class RevenueRow {
        private final String month;
        private final int invoiceCount;
        private final BigDecimal revenue;

        public RevenueRow(String month, int invoiceCount, BigDecimal revenue) {
            this.month = month;
            this.invoiceCount = invoiceCount;
            this.revenue = revenue;
        }

        public String getMonth() { return month; }
        public int getInvoiceCount() { return invoiceCount; }
        public BigDecimal getRevenue() { return revenue; }
    }

    public static class ReservationStatusRow {
        private final String status;
        private final int count;
        private final double percentage;

        public ReservationStatusRow(String status, int count, double percentage) {
            this.status = status;
            this.count = count;
            this.percentage = percentage;
        }

        public String getStatus() { return status; }
        public int getCount() { return count; }
        public double getPercentage() { return percentage; }
    }

    public static class RoomStatusRow {
        private final String status;
        private final int count;

        public RoomStatusRow(String status, int count) {
            this.status = status;
            this.count = count;
        }

        public String getStatus() { return status; }
        public int getCount() { return count; }
    }

    public static class InvoiceDetailRow {
        private final String invoiceCode;
        private final String issuedAt;
        private final String actualCheckIn;
        private final String actualCheckOut;
        private final String guestName;
        private final String roomNumber;
        private final String itemType;
        private final String description;
        private final int quantity;
        private final BigDecimal unitPrice;
        private final BigDecimal amount;
        private final BigDecimal invoiceTotal;
        private final BigDecimal paidAmount;
        private final String paymentDate;
        private final String status;

        public InvoiceDetailRow(String invoiceCode, String issuedAt, String actualCheckIn,
                                String actualCheckOut, String guestName, String roomNumber,
                                String itemType, String description, int quantity,
                                BigDecimal unitPrice, BigDecimal amount, BigDecimal invoiceTotal,
                                BigDecimal paidAmount, String paymentDate, String status) {
            this.invoiceCode = invoiceCode;
            this.issuedAt = issuedAt;
            this.actualCheckIn = actualCheckIn;
            this.actualCheckOut = actualCheckOut;
            this.guestName = guestName;
            this.roomNumber = roomNumber;
            this.itemType = itemType;
            this.description = description;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.amount = amount;
            this.invoiceTotal = invoiceTotal;
            this.paidAmount = paidAmount;
            this.paymentDate = paymentDate;
            this.status = status;
        }

        public String getInvoiceCode() { return invoiceCode; }
        public String getIssuedAt() { return issuedAt; }
        public String getActualCheckIn() { return actualCheckIn; }
        public String getActualCheckOut() { return actualCheckOut; }
        public String getGuestName() { return guestName; }
        public String getRoomNumber() { return roomNumber; }
        public String getItemType() { return itemType; }
        public String getDescription() { return description; }
        public int getQuantity() { return quantity; }
        public BigDecimal getUnitPrice() { return unitPrice; }
        public BigDecimal getAmount() { return amount; }
        public BigDecimal getInvoiceTotal() { return invoiceTotal; }
        public BigDecimal getPaidAmount() { return paidAmount; }
        public String getPaymentDate() { return paymentDate; }
        public String getStatus() { return status; }
    }
}