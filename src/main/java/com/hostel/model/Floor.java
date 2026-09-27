package com.hostel.model;

import java.sql.Timestamp;

/**
 * Floor — represents a single floor within a hostel block.
 *
 * Maps to the `floors` table.
 *
 * Fields:
 *   id           — auto-generated PK
 *   blockId      — FK to blocks.id
 *   floorNumber  — 1-based floor index
 *   roomsCount   — total number of rooms on this floor
 *   roomCapacity — seating capacity per room on this floor (2 or 3)
 *   createdAt    — creation timestamp
 *
 * Note: roomsCount and roomCapacity were added to the floors table during
 * Phase 2 to enable programmatic room generation and quick statistics.
 */
public class Floor {

    private int       id;
    private int       blockId;
    private int       floorNumber;
    private int       roomsCount;
    private int       roomCapacity;
    private Timestamp createdAt;

    // ── Constructors ───────────────────────────────────────────────────────

    public Floor() {}

    public Floor(int blockId, int floorNumber, int roomsCount, int roomCapacity) {
        this.blockId      = blockId;
        this.floorNumber  = floorNumber;
        this.roomsCount   = roomsCount;
        this.roomCapacity = roomCapacity;
    }

    public Floor(int id, int blockId, int floorNumber,
                 int roomsCount, int roomCapacity, Timestamp createdAt) {
        this.id           = id;
        this.blockId      = blockId;
        this.floorNumber  = floorNumber;
        this.roomsCount   = roomsCount;
        this.roomCapacity = roomCapacity;
        this.createdAt    = createdAt;
    }

    // ── Getters ────────────────────────────────────────────────────────────

    public int       getId()           { return id;           }
    public int       getBlockId()      { return blockId;      }
    public int       getFloorNumber()  { return floorNumber;  }
    public int       getRoomsCount()   { return roomsCount;   }
    public int       getRoomCapacity() { return roomCapacity; }
    public Timestamp getCreatedAt()    { return createdAt;    }

    // ── Setters ────────────────────────────────────────────────────────────

    public void setId(int id)                    { this.id           = id;           }
    public void setBlockId(int blockId)          { this.blockId      = blockId;      }
    public void setFloorNumber(int floorNumber)  { this.floorNumber  = floorNumber;  }
    public void setRoomsCount(int roomsCount)    { this.roomsCount   = roomsCount;   }
    public void setRoomCapacity(int cap)         { this.roomCapacity = cap;          }
    public void setCreatedAt(Timestamp ts)       { this.createdAt    = ts;           }

    @Override
    public String toString() {
        return "Floor{" +
               "id="           + id           +
               ", blockId="    + blockId      +
               ", floor="      + floorNumber  +
               ", rooms="      + roomsCount   +
               ", capacity="   + roomCapacity +
               '}';
    }
}
