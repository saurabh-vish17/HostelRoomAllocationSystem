package com.hostel.model;

/**
 * AllocationHistoryStats — Data Transfer Object representing historical allocation metric counts.
 */
public class AllocationHistoryStats {

    private int activeAllocations;
    private int vacatedAllocations;
    private int totalAllocations;

    public AllocationHistoryStats() {}

    public AllocationHistoryStats(int activeAllocations, int vacatedAllocations, int totalAllocations) {
        this.activeAllocations  = activeAllocations;
        this.vacatedAllocations = vacatedAllocations;
        this.totalAllocations   = totalAllocations;
    }

    public int getActiveAllocations() { return activeAllocations; }
    public void setActiveAllocations(int activeAllocations) { this.activeAllocations = activeAllocations; }

    public int getVacatedAllocations() { return vacatedAllocations; }
    public void setVacatedAllocations(int vacatedAllocations) { this.vacatedAllocations = vacatedAllocations; }

    public int getTotalAllocations() { return totalAllocations; }
    public void setTotalAllocations(int totalAllocations) { this.totalAllocations = totalAllocations; }

    @Override
    public String toString() {
        return "AllocationHistoryStats{" +
                "activeAllocations=" + activeAllocations +
                ", vacatedAllocations=" + vacatedAllocations +
                ", totalAllocations=" + totalAllocations +
                '}';
    }
}
