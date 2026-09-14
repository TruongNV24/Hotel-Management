package com.cnj42.hotel.model;

public class ServiceUsage {
    private final String serviceName;
    private final int quantity;
    private final double unitPrice;
    private final double totalAmount;

    public ServiceUsage(String serviceName, int quantity, double unitPrice, double totalAmount) {
        this.serviceName = serviceName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalAmount = totalAmount;
    }

    public String getServiceName() {
        return serviceName;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public double getTotalAmount() {
        return totalAmount;
    }
}
