package com.cnj42.hotel.model;

public class Room {

    private int roomId;
    private String roomNumber;
    private int roomTypeId;
    private int floor;
    private String status;
    private String imagePath;
    private String amenities;
    private String note;

    // For convenience - to hold room type info in list views
    private RoomType roomType;

    public Room() {
    }

    public Room(int roomId, String roomNumber, int roomTypeId, int floor, String status, String note) {
        this.roomId = roomId;
        this.roomNumber = roomNumber;
        this.roomTypeId = roomTypeId;
        this.floor = floor;
        this.status = status;
        this.note = note;
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public int getRoomTypeId() {
        return roomTypeId;
    }

    public void setRoomTypeId(int roomTypeId) {
        this.roomTypeId = roomTypeId;
    }

    public int getFloor() {
        return floor;
    }

    public void setFloor(int floor) {
        this.floor = floor;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getAmenities() {
        return amenities;
    }

    public void setAmenities(String amenities) {
        this.amenities = amenities;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public void setRoomType(RoomType roomType) {
        this.roomType = roomType;
    }

    @Override
    public String toString() {
        return "Room{" +
                "roomId=" + roomId +
                ", roomNumber='" + roomNumber + '\'' +
                ", roomTypeId=" + roomTypeId +
                ", floor=" + floor +
                ", status='" + status + '\'' +
                '}';
    }
}
