package com.hostel.service;

import com.hostel.dao.RoomDAO;
import com.hostel.model.Room;
import com.hostel.model.RoomDetail;
import com.hostel.model.RoomOccupantDetail;

import java.sql.SQLException;
import java.util.List;

/**
 * RoomService — business logic layer for Room Management.
 *
 * Enforces business rules:
 *   - Occupied count cannot exceed capacity (DB CHECK + service layer).
 *   - Cannot set room to MAINTENANCE status if occupied > 0.
 *   - Provides room search, multi-criteria filtering, and room occupancy lookups.
 */
public class RoomService {

    private final RoomDAO roomDAO;

    public RoomService() {
        this.roomDAO = new RoomDAO();
    }

    public List<RoomDetail> searchAndFilterRooms(String blockName, Integer floorNumber,
                                                String statusFilter, String searchQuery) throws SQLException {
        return roomDAO.searchAndFilterRooms(blockName, floorNumber, statusFilter, searchQuery);
    }

    public List<RoomOccupantDetail> getRoomOccupants(int roomId) throws SQLException {
        if (roomId <= 0) {
            throw new IllegalArgumentException("Invalid room selected.");
        }
        return roomDAO.getOccupantsByRoomId(roomId);
    }

    public Room getRoomById(int roomId) throws SQLException {
        if (roomId <= 0) return null;
        return roomDAO.findById(roomId);
    }

    /**
     * Toggles maintenance status for a room.
     * If currently MAINTENANCE, restores status to AVAILABLE.
     * If currently not MAINTENANCE, sets status to MAINTENANCE provided occupied == 0.
     *
     * @param roomId room primary key
     * @return true if updated, false otherwise
     * @throws IllegalArgumentException if room is occupied when enabling maintenance
     */
    public boolean toggleMaintenanceStatus(int roomId) throws IllegalArgumentException, SQLException {
        if (roomId <= 0) {
            throw new IllegalArgumentException("Please select a valid room.");
        }

        Room room = roomDAO.findById(roomId);
        if (room == null) {
            throw new IllegalArgumentException("Room not found.");
        }

        boolean currentMaintenance = Room.STATUS_MAINTENANCE.equals(room.getStatus());
        boolean targetMaintenance = !currentMaintenance;

        if (targetMaintenance && room.getOccupied() > 0) {
            throw new IllegalArgumentException(
                "Cannot mark Room " + room.getRoomNumber() + " as MAINTENANCE because it currently has " +
                room.getOccupied() + " active student occupant(s).\n" +
                "Please vacate or reallocate the student(s) before putting this room under maintenance."
            );
        }

        return roomDAO.setMaintenanceStatus(roomId, targetMaintenance);
    }
}
