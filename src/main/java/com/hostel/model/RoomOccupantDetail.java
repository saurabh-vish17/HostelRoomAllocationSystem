package com.hostel.model;

import java.sql.Date;

/**
 * RoomOccupantDetail — DTO representing a student currently residing in a specific room.
 */
public class RoomOccupantDetail {
    private String studentId;
    private String name;
    private String course;
    private int    year;
    private String phone;
    private String email;
    private Date   allocDate;

    public RoomOccupantDetail(String studentId, String name, String course,
                              int year, String phone, String email, Date allocDate) {
        this.studentId = studentId;
        this.name      = name;
        this.course    = course;
        this.year      = year;
        this.phone     = phone;
        this.email     = email;
        this.allocDate = allocDate;
    }

    public String getStudentId() { return studentId; }
    public String getName()      { return name;      }
    public String getCourse()    { return course;    }
    public int    getYear()      { return year;      }
    public String getPhone()     { return phone;     }
    public String getEmail()     { return email;     }
    public Date   getAllocDate() { return allocDate; }
}
