package com.hostel.model;

/**
 * FloorStats — Data Transfer Object representing room and bed statistics for a single floor.
 */
public class FloorStats {

    private String blockName;
    private int    floorNumber;
    private int    totalRooms;
    private int    totalCapacity;
    private int    occupiedBeds;
    private int    availableBeds;

    public FloorStats() {}

    public FloorStats(String blockName, int floorNumber, int totalRooms, int totalCapacity, int occupiedBeds, int availableBeds) {
        this.blockName     = blockName;
        this.floorNumber   = floorNumber;
        this.totalRooms    = totalRooms;
        this.totalCapacity = totalCapacity;
        this.occupiedBeds  = occupiedBeds;
        this.availableBeds = availableBeds;
    }

    public String getBlockName() { return blockName; }
    public void setBlockName(String blockName) { this.blockName = blockName; }

    public int getFloorNumber() { return floorNumber; }
    public void setFloorNumber(int floorNumber) { this.floorNumber = floorNumber; }

    public int getTotalRooms() { return totalRooms; }
    public void setTotalRooms(int totalRooms) { this.totalRooms = totalRooms; }

    public int getTotalCapacity() { return totalCapacity; }
    public void setTotalCapacity(int totalCapacity) { this.totalCapacity = totalCapacity; }

    public int getOccupiedBeds() { return occupiedBeds; }
    public void setOccupiedBeds(int occupiedBeds) { this.occupiedBeds = occupiedBeds; }

    public int getAvailableBeds() { return availableBeds; }
    public void setAvailableBeds(int availableBeds) { this.availableBeds = availableBeds; }

    @Override
    public String toString() {
        return "FloorStats{" +
                "blockName='" + blockName + '\'' +
                ", floorNumber=" + floorNumber +
                ", totalRooms=" + totalRooms +
                ", totalCapacity=" + totalCapacity +
                ", occupiedBeds=" + occupiedBeds +
                ", availableBeds=" + availableBeds +
                '}';
    }
}
