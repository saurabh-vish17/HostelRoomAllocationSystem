package com.hostel.model;

/**
 * RoomDetail — Data Transfer Object (DTO) combining Room, Floor, and Block information.
 *
 * Used for displaying room management tables and reports.
 */
public class RoomDetail {
    private int    id;
    private int    floorId;
    private String blockName;
    private int    floorNumber;
    private String roomNumber;
    private int    capacity;
    private int    occupied;
    private String status;

    public RoomDetail() {}

    public RoomDetail(int id, int floorId, String blockName, int floorNumber,
                      String roomNumber, int capacity, int occupied, String status) {
        this.id          = id;
        this.floorId     = floorId;
        this.blockName   = blockName;
        this.floorNumber = floorNumber;
        this.roomNumber  = roomNumber;
        this.capacity    = capacity;
        this.occupied    = occupied;
        this.status      = status;
    }

    public int getAvailableBeds() {
        return Math.max(0, capacity - occupied);
    }

    public int    getId()          { return id;          }
    public int    getFloorId()     { return floorId;     }
    public String getBlockName()   { return blockName;   }
    public int    getFloorNumber() { return floorNumber; }
    public String getRoomNumber()  { return roomNumber;  }
    public int    getCapacity()    { return capacity;    }
    public int    getOccupied()    { return occupied;    }
    public String getStatus()      { return status;      }

    public void setId(int id)                    { this.id          = id;          }
    public void setFloorId(int floorId)          { this.floorId     = floorId;     }
    public void setBlockName(String blockName)   { this.blockName   = blockName;   }
    public void setFloorNumber(int floorNumber) { this.floorNumber = floorNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber  = roomNumber;  }
    public void setCapacity(int capacity)        { this.capacity    = capacity;    }
    public void setOccupied(int occupied)        { this.occupied    = occupied;    }
    public void setStatus(String status)         { this.status      = status;      }
}
