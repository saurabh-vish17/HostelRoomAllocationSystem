package com.hostel.model;

import java.sql.Date;
import java.sql.Timestamp;

/**
 * Allocation — records a student's room assignment (past or present).
 *
 * Maps to the `allocations` table.
 *
 * IMPORTANT: Allocation records are NEVER deleted.
 *   status = ACTIVE   → student currently lives in the room
 *   status = VACATED  → student has left; vacateDate is set
 *
 * Historical records allow the admin to see who occupied which room
 * and when, which is useful for reporting and audit purposes.
 *
 * Fields:
 *   id          — auto-generated PK
 *   studentId   — FK to students.id (NOT the college roll number)
 *   roomId      — FK to rooms.id
 *   allocDate   — date the room was allocated (java.sql.Date)
 *   vacateDate  — date the student vacated; null if still active
 *   status      — "ACTIVE" or "VACATED"
 *   remarks     — optional notes
 */
public class Allocation {

    // ── Status constants ──────────────────────────────────────────────────
    public static final String STATUS_ACTIVE  = "ACTIVE";
    public static final String STATUS_VACATED = "VACATED";

    private int       id;
    private int       studentId;    // FK → students.id
    private int       roomId;       // FK → rooms.id
    private Date      allocDate;
    private Date      vacateDate;   // null if ACTIVE
    private String    status;
    private String    remarks;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // ── Constructors ───────────────────────────────────────────────────────

    public Allocation() {}

    /**
     * Constructor to create a new (ACTIVE) allocation.
     */
    public Allocation(int studentId, int roomId, Date allocDate, String remarks) {
        this.studentId  = studentId;
        this.roomId     = roomId;
        this.allocDate  = allocDate;
        this.vacateDate = null;
        this.status     = STATUS_ACTIVE;
        this.remarks    = remarks;
    }

    /** Full constructor for mapping a complete DB row. */
    public Allocation(int id, int studentId, int roomId, Date allocDate,
                      Date vacateDate, String status, String remarks,
                      Timestamp createdAt, Timestamp updatedAt) {
        this.id         = id;
        this.studentId  = studentId;
        this.roomId     = roomId;
        this.allocDate  = allocDate;
        this.vacateDate = vacateDate;
        this.status     = status;
        this.remarks    = remarks;
        this.createdAt  = createdAt;
        this.updatedAt  = updatedAt;
    }

    // ── Boolean helpers ───────────────────────────────────────────────────

    public boolean isActive()  { return STATUS_ACTIVE.equals(status);  }
    public boolean isVacated() { return STATUS_VACATED.equals(status); }

    // ── Getters ────────────────────────────────────────────────────────────

    public int       getId()          { return id;          }
    public int       getStudentId()   { return studentId;   }
    public int       getRoomId()      { return roomId;      }
    public Date      getAllocDate()   { return allocDate;   }
    public Date      getVacateDate()  { return vacateDate;  }
    public String    getStatus()      { return status;      }
    public String    getRemarks()     { return remarks;     }
    public Timestamp getCreatedAt()   { return createdAt;   }
    public Timestamp getUpdatedAt()   { return updatedAt;   }

    // ── Setters ────────────────────────────────────────────────────────────

    public void setId(int id)                    { this.id         = id;         }
    public void setStudentId(int studentId)      { this.studentId  = studentId;  }
    public void setRoomId(int roomId)            { this.roomId     = roomId;     }
    public void setAllocDate(Date allocDate)     { this.allocDate  = allocDate;  }
    public void setVacateDate(Date vacateDate)   { this.vacateDate = vacateDate; }
    public void setStatus(String status)         { this.status     = status;     }
    public void setRemarks(String remarks)       { this.remarks    = remarks;    }
    public void setCreatedAt(Timestamp ts)       { this.createdAt  = ts;         }
    public void setUpdatedAt(Timestamp ts)       { this.updatedAt  = ts;         }

    @Override
    public String toString() {
        return "Allocation{" +
               "id="          + id         +
               ", studentId=" + studentId  +
               ", roomId="    + roomId     +
               ", alloc="     + allocDate  +
               ", vacate="    + vacateDate +
               ", status='"   + status     + '\'' +
               '}';
    }
}
