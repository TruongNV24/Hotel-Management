package com.cnj42.hotel.model;

public class DashboardData {

    private int totalRooms;
    private int availableRooms;
    private int occupiedRooms;
    private int activeReservations;
    private int[] roomStatusSummary;
    private int[] revenueTrend;
    private long monthlyRevenue;
    private long previousMonthRevenue;

    public DashboardData() {
        this.roomStatusSummary = new int[]{0, 0, 0, 0, 0};
        this.revenueTrend = new int[]{0, 0, 0, 0, 0, 0};
    }

    public int getTotalRooms() {
        return totalRooms;
    }

    public void setTotalRooms(int totalRooms) {
        this.totalRooms = totalRooms;
    }

    public int getAvailableRooms() {
        return availableRooms;
    }

    public void setAvailableRooms(int availableRooms) {
        this.availableRooms = availableRooms;
    }

    public int getOccupiedRooms() {
        return occupiedRooms;
    }

    public void setOccupiedRooms(int occupiedRooms) {
        this.occupiedRooms = occupiedRooms;
    }

    public int getActiveReservations() {
        return activeReservations;
    }

    public void setActiveReservations(int activeReservations) {
        this.activeReservations = activeReservations;
    }

    public int[] getRoomStatusSummary() {
        return roomStatusSummary;
    }

    public void setRoomStatusSummary(int[] roomStatusSummary) {
        this.roomStatusSummary = roomStatusSummary;
    }

    public int[] getRevenueTrend() {
        return revenueTrend;
    }

    public void setRevenueTrend(int[] revenueTrend) {
        this.revenueTrend = revenueTrend;
    }

    public long getMonthlyRevenue() {
        return monthlyRevenue;
    }

    public void setMonthlyRevenue(long monthlyRevenue) {
        this.monthlyRevenue = monthlyRevenue;
    }

    public long getPreviousMonthRevenue() {
        return previousMonthRevenue;
    }

    public void setPreviousMonthRevenue(long previousMonthRevenue) {
        this.previousMonthRevenue = previousMonthRevenue;
    }
}
