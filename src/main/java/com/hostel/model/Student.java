package com.hostel.model;

import java.sql.Timestamp;

/**
 * Student — represents a student who can be allocated a hostel room.
 *
 * Maps to the `students` table.
 *
 * Fields:
 *   id        — auto-generated PK (internal DB id)
 *   studentId — college roll number (unique, shown in UI)
 *   name      — full name
 *   course    — e.g. "B.Tech CSE", "MBA"
 *   year      — academic year (1, 2, 3, 4)
 *   gender    — "Male" | "Female" | "Other"
 *   phone     — contact number
 *   email     — email address
 *   address   — full address
 */
public class Student {

    private int       id;
    private String    studentId;   // College roll number, e.g. "CS2024001"
    private String    name;
    private String    course;
    private int       year;
    private String    gender;      // "Male" | "Female" | "Other"
    private String    phone;
    private String    email;
    private String    address;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // ── Constructors ───────────────────────────────────────────────────────

    public Student() {}

    /**
     * Constructor for creating a new student record.
     */
    public Student(String studentId, String name, String course, int year,
                   String gender, String phone, String email, String address) {
        this.studentId = studentId;
        this.name      = name;
        this.course    = course;
        this.year      = year;
        this.gender    = gender;
        this.phone     = phone;
        this.email     = email;
        this.address   = address;
    }

    /** Full constructor for mapping a complete DB row. */
    public Student(int id, String studentId, String name, String course, int year,
                   String gender, String phone, String email, String address,
                   Timestamp createdAt, Timestamp updatedAt) {
        this.id        = id;
        this.studentId = studentId;
        this.name      = name;
        this.course    = course;
        this.year      = year;
        this.gender    = gender;
        this.phone     = phone;
        this.email     = email;
        this.address   = address;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // ── Getters ────────────────────────────────────────────────────────────

    public int       getId()         { return id;         }
    public String    getStudentId()  { return studentId;  }
    public String    getName()       { return name;       }
    public String    getCourse()     { return course;     }
    public int       getYear()       { return year;       }
    public String    getGender()     { return gender;     }
    public String    getPhone()      { return phone;      }
    public String    getEmail()      { return email;      }
    public String    getAddress()    { return address;    }
    public Timestamp getCreatedAt()  { return createdAt;  }
    public Timestamp getUpdatedAt()  { return updatedAt;  }

    // ── Setters ────────────────────────────────────────────────────────────

    public void setId(int id)                    { this.id        = id;        }
    public void setStudentId(String studentId)   { this.studentId = studentId; }
    public void setName(String name)             { this.name      = name;      }
    public void setCourse(String course)         { this.course    = course;    }
    public void setYear(int year)                { this.year      = year;      }
    public void setGender(String gender)         { this.gender    = gender;    }
    public void setPhone(String phone)           { this.phone     = phone;     }
    public void setEmail(String email)           { this.email     = email;     }
    public void setAddress(String address)       { this.address   = address;   }
    public void setCreatedAt(Timestamp ts)       { this.createdAt = ts;        }
    public void setUpdatedAt(Timestamp ts)       { this.updatedAt = ts;        }

    @Override
    public String toString() {
        return "Student{" +
               "id="          + id        +
               ", studentId='"+ studentId + '\'' +
               ", name='"     + name      + '\'' +
               ", course='"   + course    + '\'' +
               ", year="      + year      +
               ", gender='"   + gender    + '\'' +
               '}';
    }
}
