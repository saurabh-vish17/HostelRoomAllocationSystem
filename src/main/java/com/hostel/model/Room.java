package com.hostel.model;

import java.sql.Timestamp;

/**
 * Room — represents a single hostel room.
 *
 * Maps to the `rooms` table.
 *
 * Status lifecycle:
 *   AVAILABLE          ← occupied == 0
 *   PARTIALLY_OCCUPIED ← 0 < occupied < capacity
 *   FULL               ← occupied == capacity
 *   MAINTENANCE        ← manually set by admin (no allocations allowed)
 *
 * availableBeds is a computed property: capacity - occupied.
 * It is NOT stored in the database — always derived at runtime.
 */
public class Room {

    // ── Status constants (match the ENUM in the DB exactly) ──────────────
    public static final String STATUS_AVAILABLE           = "AVAILABLE";
    public static final String STATUS_PARTIALLY_OCCUPIED  = "PARTIALLY_OCCUPIED";
    public static final String STATUS_FULL                = "FULL";
    public static final String STATUS_MAINTENANCE         = "MAINTENANCE";

    private int       id;
    private int       floorId;
    private String    roomNumber;   // e.g. "D101", "G253", "H430"
    private int       capacity;     // max occupants (2 or 3)
    private int       occupied;     // current occupant count
    private String    status;       // one of the STATUS_ constants above
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // ── Constructors ───────────────────────────────────────────────────────

    public Room() {}

    public Room(int floorId, String roomNumber, int capacity) {
        this.floorId    = floorId;
        this.roomNumber = roomNumber;
        this.capacity   = capacity;
        this.occupied   = 0;
        this.status     = STATUS_AVAILABLE;
    }

    public Room(int id, int floorId, String roomNumber, int capacity,
                int occupied, String status, Timestamp createdAt, Timestamp updatedAt) {
        this.id         = id;
        this.floorId    = floorId;
        this.roomNumber = roomNumber;
        this.capacity   = capacity;
        this.occupied   = occupied;
        this.status     = status;
        this.createdAt  = createdAt;
        this.updatedAt  = updatedAt;
    }

    // ── Computed property ─────────────────────────────────────────────────

    /**
     * Returns how many beds are still free.
     * This is always capacity - occupied (never stored in DB).
     */
    public int getAvailableBeds() {
        return capacity - occupied;
    }

    /**
     * Calculates what the status SHOULD be based on current occupancy.
     * Used by RoomService and AllocationService after updating occupied count.
     *
     * @param capacity max occupants
     * @param occupied current occupants
     * @return the correct status string
     */
    public static String calculateStatus(int capacity, int occupied) {
        if (occupied <= 0)            return STATUS_AVAILABLE;
        if (occupied >= capacity)     return STATUS_FULL;
        return STATUS_PARTIALLY_OCCUPIED;
    }

    /**
     * Convenience: recalculate and update this room's own status.
     * Call this after changing the occupied count.
     */
    public void recalculateStatus() {
        // Do not override MAINTENANCE status automatically
        if (!STATUS_MAINTENANCE.equals(this.status)) {
            this.status = calculateStatus(capacity, occupied);
        }
    }

    // ── Boolean helpers ────────────────────────────────────────────────────

    public boolean isAvailable()     { return STATUS_AVAILABLE.equals(status);          }
    public boolean isFull()          { return STATUS_FULL.equals(status);               }
    public boolean isMaintenance()   { return STATUS_MAINTENANCE.equals(status);        }
    public boolean hasAvailableBeds(){ return !isFull() && !isMaintenance();            }

    // ── Getters ────────────────────────────────────────────────────────────

    public int       getId()          { return id;          }
    public int       getFloorId()     { return floorId;     }
    public String    getRoomNumber()  { return roomNumber;  }
    public int       getCapacity()    { return capacity;    }
    public int       getOccupied()    { return occupied;    }
    public String    getStatus()      { return status;      }
    public Timestamp getCreatedAt()   { return createdAt;   }
    public Timestamp getUpdatedAt()   { return updatedAt;   }

    // ── Setters ────────────────────────────────────────────────────────────

    public void setId(int id)                    { this.id         = id;         }
    public void setFloorId(int floorId)          { this.floorId    = floorId;    }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    public void setCapacity(int capacity)        { this.capacity   = capacity;   }
    public void setOccupied(int occupied)        { this.occupied   = occupied;   }
    public void setStatus(String status)         { this.status     = status;     }
    public void setCreatedAt(Timestamp ts)       { this.createdAt  = ts;         }
    public void setUpdatedAt(Timestamp ts)       { this.updatedAt  = ts;         }

    @Override
    public String toString() {
        return "Room{" +
               "id="         + id                   +
               ", room='"    + roomNumber            + '\'' +
               ", cap="      + capacity              +
               ", occ="      + occupied              +
               ", avail="    + getAvailableBeds()    +
               ", status='"  + status                + '\'' +
               '}';
    }
}
