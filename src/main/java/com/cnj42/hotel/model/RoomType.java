package com.cnj42.hotel.model;

public class RoomType {

    private int roomTypeId;
    private String typeName;
    private String description;
    private int capacity;
    private double pricePerNight;
    private String status;

    public RoomType() {
    }

    public RoomType(int roomTypeId, String typeName, String description, int capacity, double pricePerNight, String status) {
        this.roomTypeId = roomTypeId;
        this.typeName = typeName;
        this.description = description;
        this.capacity = capacity;
        this.pricePerNight = pricePerNight;
        this.status = status;
    }

    public int getRoomTypeId() {
        return roomTypeId;
    }

    public void setRoomTypeId(int roomTypeId) {
        this.roomTypeId = roomTypeId;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public double getPricePerNight() {
        return pricePerNight;
    }

    public void setPricePerNight(double pricePerNight) {
        this.pricePerNight = pricePerNight;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "RoomType{" +
                "roomTypeId=" + roomTypeId +
                ", typeName='" + typeName + '\'' +
                ", capacity=" + capacity +
                ", pricePerNight=" + pricePerNight +
                ", status='" + status + '\'' +
                '}';
    }
}
