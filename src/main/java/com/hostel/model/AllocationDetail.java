package com.hostel.model;

import java.sql.Date;

/**
 * AllocationDetail — DTO combining Allocation, Student, Room, Floor, and Block information.
 *
 * Used for displaying active allocation tables and historical allocation reports.
 */
public class AllocationDetail {
    private int    allocId;
    private int    studentDbId;
    private String studentRoll;
    private String studentName;
    private String course;
    private int    year;
    private int    roomId;
    private String blockName;
    private int    floorNumber;
    private String roomNumber;
    private int    capacity;
    private int    occupied;
    private Date   allocDate;
    private Date   vacateDate;
    private String status;
    private String remarks;

    public AllocationDetail() {}

    public AllocationDetail(int allocId, int studentDbId, String studentRoll, String studentName,
                            String course, int year, int roomId, String blockName, int floorNumber,
                            String roomNumber, int capacity, int occupied, Date allocDate,
                            Date vacateDate, String status, String remarks) {
        this.allocId     = allocId;
        this.studentDbId = studentDbId;
        this.studentRoll = studentRoll;
        this.studentName = studentName;
        this.course      = course;
        this.year        = year;
        this.roomId      = roomId;
        this.blockName   = blockName;
        this.floorNumber = floorNumber;
        this.roomNumber  = roomNumber;
        this.capacity    = capacity;
        this.occupied    = occupied;
        this.allocDate   = allocDate;
        this.vacateDate  = vacateDate;
        this.status      = status;
        this.remarks     = remarks;
    }

    public int getAvailableBeds() {
        return Math.max(0, capacity - occupied);
    }

    public int    getAllocId()     { return allocId;     }
    public int    getStudentDbId() { return studentDbId; }
    public String getStudentRoll() { return studentRoll; }
    public String getStudentName() { return studentName; }
    public String getCourse()      { return course;      }
    public int    getYear()        { return year;        }
    public int    getRoomId()      { return roomId;      }
    public String getBlockName()   { return blockName;   }
    public int    getFloorNumber() { return floorNumber; }
    public String getRoomNumber()  { return roomNumber;  }
    public int    getCapacity()    { return capacity;    }
    public int    getOccupied()    { return occupied;    }
    public Date   getAllocDate()   { return allocDate;   }
    public Date   getVacateDate()  { return vacateDate;  }
    public String getStatus()      { return status;      }
    public String getRemarks()     { return remarks;     }
}
