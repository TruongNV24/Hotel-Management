package com.cnj42.hotel.model;

public class CheckoutSummary {
    private final double roomAmount;
    private final double serviceAmount;
    private final double totalAmount;

    public CheckoutSummary(double roomAmount, double serviceAmount) {
        this(roomAmount, serviceAmount, roomAmount + serviceAmount);
    }

    public CheckoutSummary(double roomAmount, double serviceAmount, double totalAmount) {
        this.roomAmount = roomAmount;
        this.serviceAmount = serviceAmount;
        this.totalAmount = totalAmount;
    }

    public double getRoomAmount() {
        return roomAmount;
    }

    public double getServiceAmount() {
        return serviceAmount;
    }

    public double getTotalAmount() {
        return totalAmount;
    }
}
