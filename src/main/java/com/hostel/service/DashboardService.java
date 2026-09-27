package com.hostel.service;

import com.hostel.dao.RoomDAO;
import com.hostel.dao.StudentDAO;
import com.hostel.model.DashboardStats;
import com.hostel.model.DashboardStats.BlockStats;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * DashboardService — Service layer for calculating dashboard statistics.
 *
 * All metrics are calculated live from MySQL via DAOs.
 */
public class DashboardService {

    private final RoomDAO    roomDAO;
    private final StudentDAO studentDAO;

    public DashboardService() {
        this.roomDAO    = new RoomDAO();
        this.studentDAO = new StudentDAO();
    }

    /**
     * Fetches fresh statistics from the database and returns a populated DashboardStats object.
     *
     * @return DashboardStats containing overall and block-level statistics.
     * @throws SQLException on database query failure.
     */
    public DashboardStats getDashboardStatistics() throws SQLException {
        DashboardStats stats = new DashboardStats();

        // 1. Total Students
        stats.setTotalStudents(studentDAO.getTotalCount());

        // 2. Overall Room Statistics
        try (ResultSet rs = roomDAO.getOverallStats()) {
            if (rs.next()) {
                stats.setTotalRooms(rs.getInt("total_rooms"));
                stats.setTotalCapacity(rs.getInt("total_capacity"));
                stats.setOccupiedBeds(rs.getInt("total_occupied"));
                stats.setAvailableBeds(rs.getInt("available_beds"));
                stats.setAvailableRooms(rs.getInt("available_rooms"));
                stats.setFullRooms(rs.getInt("full_rooms"));
                stats.setPartiallyOccupiedRooms(rs.getInt("partial_rooms"));
                stats.setMaintenanceRooms(rs.getInt("maintenance_rooms"));
            }
        }

        // 3. Block D Statistics
        stats.setBlockD(fetchBlockStats("D"));

        // 4. Block G Statistics
        stats.setBlockG(fetchBlockStats("G"));

        // 5. Block H Statistics
        stats.setBlockH(fetchBlockStats("H"));

        return stats;
    }

    private BlockStats fetchBlockStats(String blockName) throws SQLException {
        try (ResultSet rs = roomDAO.getStatsByBlock(blockName)) {
            if (rs.next()) {
                return new BlockStats(
                    blockName,
                    rs.getInt("total_rooms"),
                    rs.getInt("total_capacity"),
                    rs.getInt("total_occupied"),
                    rs.getInt("total_available"),
                    rs.getInt("available_rooms"),
                    rs.getInt("full_rooms"),
                    rs.getInt("partial_rooms"),
                    rs.getInt("maintenance_rooms")
                );
            }
        }
        return new BlockStats(blockName, 0, 0, 0, 0, 0, 0, 0, 0);
    }
}
