package com.hostel.model;

/**
 * DashboardStats — Data Transfer Object holding all dynamic metrics
 * calculated from the MySQL database.
 */
public class DashboardStats {

    // ── Overall Metrics ───────────────────────────────────────────────────
    private int totalStudents;
    private int totalRooms;
    private int totalCapacity;
    private int occupiedBeds;
    private int availableBeds;
    private int availableRooms;
    private int fullRooms;
    private int partiallyOccupiedRooms;
    private int maintenanceRooms;

    // ── Block-level Metrics Inner DTO ─────────────────────────────────────
    public static class BlockStats {
        private String blockName;
        private int totalRooms;
        private int totalCapacity;
        private int occupiedBeds;
        private int availableBeds;
        private int availableRooms;
        private int fullRooms;
        private int partialRooms;
        private int maintenanceRooms;

        public BlockStats(String blockName, int totalRooms, int totalCapacity,
                          int occupiedBeds, int availableBeds, int availableRooms,
                          int fullRooms, int partialRooms, int maintenanceRooms) {
            this.blockName        = blockName;
            this.totalRooms       = totalRooms;
            this.totalCapacity    = totalCapacity;
            this.occupiedBeds     = occupiedBeds;
            this.availableBeds    = availableBeds;
            this.availableRooms   = availableRooms;
            this.fullRooms        = fullRooms;
            this.partialRooms     = partialRooms;
            this.maintenanceRooms = maintenanceRooms;
        }

        public String getBlockName()        { return blockName;        }
        public int getTotalRooms()          { return totalRooms;       }
        public int getTotalCapacity()       { return totalCapacity;    }
        public int getOccupiedBeds()        { return occupiedBeds;     }
        public int getAvailableBeds()       { return availableBeds;    }
        public int getAvailableRooms()      { return availableRooms;   }
        public int getFullRooms()           { return fullRooms;        }
        public int getPartialRooms()        { return partialRooms;     }
        public int getMaintenanceRooms()    { return maintenanceRooms; }

        public double getOccupancyPercentage() {
            return totalCapacity > 0 ? ((double) occupiedBeds / totalCapacity) * 100 : 0;
        }
    }

    private BlockStats blockD;
    private BlockStats blockG;
    private BlockStats blockH;

    // ── Getters & Setters ─────────────────────────────────────────────────
    public int getTotalStudents()           { return totalStudents;           }
    public void setTotalStudents(int v)     { this.totalStudents = v;         }

    public int getTotalRooms()              { return totalRooms;              }
    public void setTotalRooms(int v)        { this.totalRooms = v;            }

    public int getTotalCapacity()           { return totalCapacity;           }
    public void setTotalCapacity(int v)     { this.totalCapacity = v;         }

    public int getOccupiedBeds()            { return occupiedBeds;            }
    public void setOccupiedBeds(int v)      { this.occupiedBeds = v;          }

    public int getAvailableBeds()           { return availableBeds;           }
    public void setAvailableBeds(int v)     { this.availableBeds = v;         }

    public int getAvailableRooms()          { return availableRooms;          }
    public void setAvailableRooms(int v)    { this.availableRooms = v;        }

    public int getFullRooms()               { return fullRooms;               }
    public void setFullRooms(int v)         { this.fullRooms = v;             }

    public int getPartiallyOccupiedRooms()  { return partiallyOccupiedRooms;  }
    public void setPartiallyOccupiedRooms(int v) { this.partiallyOccupiedRooms = v; }

    public int getMaintenanceRooms()        { return maintenanceRooms;        }
    public void setMaintenanceRooms(int v)  { this.maintenanceRooms = v;      }

    public BlockStats getBlockD()           { return blockD;                  }
    public void setBlockD(BlockStats b)     { this.blockD = b;                }

    public BlockStats getBlockG()           { return blockG;                  }
    public void setBlockG(BlockStats b)     { this.blockG = b;                }

    public BlockStats getBlockH()           { return blockH;                  }
    public void setBlockH(BlockStats b)     { this.blockH = b;                }

    public double getOverallOccupancyPercentage() {
        return totalCapacity > 0 ? ((double) occupiedBeds / totalCapacity) * 100 : 0;
    }
}
