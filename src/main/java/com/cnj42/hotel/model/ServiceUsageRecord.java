package com.cnj42.hotel.model;

import java.time.LocalDateTime;

public class ServiceUsageRecord {
    private int usageId;
    private int stayId;
    private int serviceId;
    private String serviceName;
    private int quantity;
    private double unitPrice;
    private double totalAmount;
    private LocalDateTime usedAt;
    private String note;
    private Integer createdBy;

    public int getUsageId() { return usageId; }
    public void setUsageId(int usageId) { this.usageId = usageId; }
    public int getStayId() { return stayId; }
    public void setStayId(int stayId) { this.stayId = stayId; }
    public int getServiceId() { return serviceId; }
    public void setServiceId(int serviceId) { this.serviceId = serviceId; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    public LocalDateTime getUsedAt() { return usedAt; }
    public void setUsedAt(LocalDateTime usedAt) { this.usedAt = usedAt; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public Integer getCreatedBy() { return createdBy; }
    public void setCreatedBy(Integer createdBy) { this.createdBy = createdBy; }
}
