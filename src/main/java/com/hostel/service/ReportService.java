package com.hostel.service;

import com.hostel.dao.AllocationDAO;
import com.hostel.dao.RoomDAO;
import com.hostel.model.AllocationHistoryStats;
import com.hostel.model.DashboardStats;
import com.hostel.model.FloorStats;

import java.sql.SQLException;
import java.util.List;

/**
 * ReportService — Service layer for compiling system-wide reports and analytics.
 */
public class ReportService {

    private final DashboardService dashboardService;
    private final RoomDAO          roomDAO;
    private final AllocationDAO    allocationDAO;

    public ReportService() {
        this.dashboardService = new DashboardService();
        this.roomDAO          = new RoomDAO();
        this.allocationDAO    = new AllocationDAO();
    }

    /**
     * Retrieves overall system statistics and block-wise breakdowns.
     */
    public DashboardStats getOverallStats() throws SQLException {
        return dashboardService.getDashboardStatistics();
    }

    /**
     * Retrieves floor-level room and bed statistics for all blocks.
     */
    public List<FloorStats> getFloorWiseStats() throws SQLException {
        return roomDAO.getFloorWiseStats();
    }

    /**
     * Retrieves allocation history metrics (active, vacated, total).
     */
    public AllocationHistoryStats getAllocationHistoryStats() throws SQLException {
        return allocationDAO.getAllocationHistoryStats();
    }
}
