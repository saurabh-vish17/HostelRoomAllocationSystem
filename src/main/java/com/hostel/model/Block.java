package com.hostel.model;

import java.sql.Timestamp;

/**
 * Block — represents a hostel block (D, G, or H).
 *
 * Maps to the `blocks` table.
 *
 * Fields:
 *   id          — auto-generated PK
 *   blockName   — single character: "D", "G", or "H"
 *   totalFloors — number of floors in this block
 *   description — optional description text
 *   createdAt   — creation timestamp
 */
public class Block {

    private int       id;
    private String    blockName;
    private int       totalFloors;
    private String    description;
    private Timestamp createdAt;

    // ── Constructors ───────────────────────────────────────────────────────

    public Block() {}

    public Block(String blockName, int totalFloors, String description) {
        this.blockName   = blockName;
        this.totalFloors = totalFloors;
        this.description = description;
    }

    public Block(int id, String blockName, int totalFloors,
                 String description, Timestamp createdAt) {
        this.id          = id;
        this.blockName   = blockName;
        this.totalFloors = totalFloors;
        this.description = description;
        this.createdAt   = createdAt;
    }

    // ── Getters ────────────────────────────────────────────────────────────

    public int       getId()          { return id;          }
    public String    getBlockName()   { return blockName;   }
    public int       getTotalFloors() { return totalFloors; }
    public String    getDescription() { return description; }
    public Timestamp getCreatedAt()   { return createdAt;   }

    // ── Setters ────────────────────────────────────────────────────────────

    public void setId(int id)                    { this.id          = id;          }
    public void setBlockName(String blockName)   { this.blockName   = blockName;   }
    public void setTotalFloors(int totalFloors)  { this.totalFloors = totalFloors; }
    public void setDescription(String desc)      { this.description = desc;        }
    public void setCreatedAt(Timestamp ts)       { this.createdAt   = ts;          }

    @Override
    public String toString() {
        return "Block{" +
               "id="          + id          +
               ", blockName='"+ blockName   + '\'' +
               ", floors="    + totalFloors +
               '}';
    }
}
