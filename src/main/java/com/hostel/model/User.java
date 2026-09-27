package com.hostel.model;

import java.sql.Timestamp;

/**
 * User — represents an admin account in the system.
 *
 * Maps to the `users` table in hostel_management database.
 *
 * Fields:
 *   id         — auto-generated primary key
 *   username   — unique login name
 *   password   — BCrypt hash (plain-text in Phase 2 seed, hashed from Phase 4)
 *   fullName   — display name shown in the UI
 *   role       — always "ADMIN" for now (multi-role support is optional)
 *   isActive   — soft-delete flag
 *   createdAt  — record creation timestamp
 *   updatedAt  — last update timestamp
 */
public class User {

    private int        id;
    private String     username;
    private String     password;   // BCrypt hash — never display in UI
    private String     fullName;
    private String     role;
    private boolean    isActive;
    private Timestamp  createdAt;
    private Timestamp  updatedAt;

    // ── Constructors ───────────────────────────────────────────────────────

    /** No-arg constructor (used by DAO when mapping ResultSet) */
    public User() {}

    /**
     * Constructor for creating a new user (id and timestamps set by DB).
     */
    public User(String username, String password, String fullName, String role) {
        this.username  = username;
        this.password  = password;
        this.fullName  = fullName;
        this.role      = role;
        this.isActive  = true;
    }

    /** Full constructor (used when mapping a complete DB row). */
    public User(int id, String username, String password, String fullName,
                String role, boolean isActive, Timestamp createdAt, Timestamp updatedAt) {
        this.id        = id;
        this.username  = username;
        this.password  = password;
        this.fullName  = fullName;
        this.role      = role;
        this.isActive  = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // ── Getters ────────────────────────────────────────────────────────────

    public int       getId()        { return id;        }
    public String    getUsername()  { return username;  }
    public String    getPassword()  { return password;  }
    public String    getFullName()  { return fullName;  }
    public String    getRole()      { return role;      }
    public boolean   isActive()     { return isActive;  }
    public Timestamp getCreatedAt() { return createdAt; }
    public Timestamp getUpdatedAt() { return updatedAt; }

    // ── Setters ────────────────────────────────────────────────────────────

    public void setId(int id)                    { this.id        = id;        }
    public void setUsername(String username)     { this.username  = username;  }
    public void setPassword(String password)     { this.password  = password;  }
    public void setFullName(String fullName)     { this.fullName  = fullName;  }
    public void setRole(String role)             { this.role      = role;      }
    public void setActive(boolean isActive)      { this.isActive  = isActive;  }
    public void setCreatedAt(Timestamp createdAt){ this.createdAt = createdAt; }
    public void setUpdatedAt(Timestamp updatedAt){ this.updatedAt = updatedAt; }

    // ── toString (for logging — never log the password!) ──────────────────

    @Override
    public String toString() {
        return "User{" +
               "id="       + id       +
               ", user='"  + username + '\'' +
               ", name='"  + fullName + '\'' +
               ", role='"  + role     + '\'' +
               ", active=" + isActive +
               '}';
    }
}
