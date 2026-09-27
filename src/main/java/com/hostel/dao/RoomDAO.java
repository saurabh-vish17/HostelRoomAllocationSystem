package com.hostel.dao;

import com.hostel.config.DBConnection;
import com.hostel.model.Room;
import com.hostel.model.RoomDetail;
import com.hostel.model.RoomOccupantDetail;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * RoomDAO — read/write data access layer for the rooms table.
 */
public class RoomDAO {

    private Room mapRow(ResultSet rs) throws SQLException {
        return new Room(
            rs.getInt("id"),
            rs.getInt("floor_id"),
            rs.getString("room_number"),
            rs.getInt("capacity"),
            rs.getInt("occupied"),
            rs.getString("status"),
            rs.getTimestamp("created_at"),
            rs.getTimestamp("updated_at")
        );
    }

    public List<Room> getRoomsByFloor(int floorId) throws SQLException {
        List<Room> list = new ArrayList<>();
        String sql = "SELECT id, floor_id, room_number, capacity, occupied, status, created_at, updated_at " +
                     "FROM rooms WHERE floor_id = ? ORDER BY room_number ASC";

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, floorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public List<Room> getAvailableRoomsByFloor(int floorId) throws SQLException {
        List<Room> list = new ArrayList<>();
        String sql = "SELECT id, floor_id, room_number, capacity, occupied, status, created_at, updated_at " +
                     "FROM rooms " +
                     "WHERE floor_id = ? AND status IN ('AVAILABLE', 'PARTIALLY_OCCUPIED') AND occupied < capacity " +
                     "ORDER BY room_number ASC";

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, floorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public Room findById(int id) throws SQLException {
        String sql = "SELECT id, floor_id, room_number, capacity, occupied, status, created_at, updated_at " +
                     "FROM rooms WHERE id = ?";

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public Room findByRoomNumber(String roomNumber) throws SQLException {
        String sql = "SELECT id, floor_id, room_number, capacity, occupied, status, created_at, updated_at " +
                     "FROM rooms WHERE room_number = ?";

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, roomNumber);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    /**
     * Advanced Room Search & Multi-criteria Filtering query.
     * Joins rooms, floors, and blocks tables.
     */
    public List<RoomDetail> searchAndFilterRooms(String blockName, Integer floorNumber,
                                                String statusFilter, String searchQuery) throws SQLException {
        List<RoomDetail> list = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
            "SELECT r.id, r.floor_id, b.block_name, f.floor_number, r.room_number, " +
            "       r.capacity, r.occupied, r.status " +
            "FROM rooms r " +
            "JOIN floors f ON r.floor_id = f.id " +
            "JOIN blocks b ON f.block_id = b.id " +
            "WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();

        if (blockName != null && !blockName.trim().isEmpty() && !"ALL".equalsIgnoreCase(blockName)) {
            sql.append("AND b.block_name = ? ");
            params.add(blockName.trim());
        }

        if (floorNumber != null && floorNumber > 0) {
            sql.append("AND f.floor_number = ? ");
            params.add(floorNumber);
        }

        if (statusFilter != null && !statusFilter.trim().isEmpty() && !"ALL".equalsIgnoreCase(statusFilter)) {
            sql.append("AND r.status = ? ");
            params.add(statusFilter.trim());
        }

        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            sql.append("AND r.room_number LIKE ? ");
            params.add("%" + searchQuery.trim() + "%");
        }

        sql.append("ORDER BY b.block_name ASC, f.floor_number ASC, r.room_number ASC");

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new RoomDetail(
                        rs.getInt("id"),
                        rs.getInt("floor_id"),
                        rs.getString("block_name"),
                        rs.getInt("floor_number"),
                        rs.getString("room_number"),
                        rs.getInt("capacity"),
                        rs.getInt("occupied"),
                        rs.getString("status")
                    ));
                }
            }
        }
        return list;
    }

    /**
     * Fetches details of active student occupants in a room.
     */
    public List<RoomOccupantDetail> getOccupantsByRoomId(int roomId) throws SQLException {
        List<RoomOccupantDetail> list = new ArrayList<>();
        String sql =
            "SELECT s.student_id, s.name, s.course, s.year, s.phone, s.email, a.alloc_date " +
            "FROM allocations a " +
            "JOIN students s ON a.student_id = s.id " +
            "WHERE a.room_id = ? AND a.status = 'ACTIVE' " +
            "ORDER BY a.alloc_date ASC";

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new RoomOccupantDetail(
                        rs.getString("student_id"),
                        rs.getString("name"),
                        rs.getString("course"),
                        rs.getInt("year"),
                        rs.getString("phone"),
                        rs.getString("email"),
                        rs.getDate("alloc_date")
                    ));
                }
            }
        }
        return list;
    }

    public boolean updateOccupancyAndStatus(int roomId, int newOccupied, String newStatus) throws SQLException {
        String sql = "UPDATE rooms SET occupied = ?, status = ? " +
                     "WHERE id = ? AND ? >= 0 AND ? <= capacity";

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt   (1, newOccupied);
            ps.setString(2, newStatus);
            ps.setInt   (3, roomId);
            ps.setInt   (4, newOccupied);
            ps.setInt   (5, newOccupied);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean setMaintenanceStatus(int roomId, boolean setMaintenance) throws SQLException {
        String sql;
        if (setMaintenance) {
            sql = "UPDATE rooms SET status = 'MAINTENANCE' WHERE id = ?";
            try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
                ps.setInt(1, roomId);
                return ps.executeUpdate() == 1;
            }
        } else {
            sql = "UPDATE rooms SET status = " +
                  "CASE " +
                  "  WHEN occupied = 0             THEN 'AVAILABLE' " +
                  "  WHEN occupied >= capacity      THEN 'FULL' " +
                  "  ELSE 'PARTIALLY_OCCUPIED' " +
                  "END " +
                  "WHERE id = ?";
            try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
                ps.setInt(1, roomId);
                return ps.executeUpdate() == 1;
            }
        }
    }

    public ResultSet getStatsByBlock(String blockName) throws SQLException {
        String sql =
            "SELECT " +
            "  COUNT(r.id)                                        AS total_rooms, " +
            "  SUM(r.capacity)                                    AS total_capacity, " +
            "  SUM(r.occupied)                                    AS total_occupied, " +
            "  SUM(r.capacity - r.occupied)                       AS total_available, " +
            "  SUM(CASE WHEN r.status = 'FULL'               THEN 1 ELSE 0 END) AS full_rooms, " +
            "  SUM(CASE WHEN r.status = 'PARTIALLY_OCCUPIED' THEN 1 ELSE 0 END) AS partial_rooms, " +
            "  SUM(CASE WHEN r.status = 'AVAILABLE'          THEN 1 ELSE 0 END) AS available_rooms, " +
            "  SUM(CASE WHEN r.status = 'MAINTENANCE'        THEN 1 ELSE 0 END) AS maintenance_rooms " +
            "FROM rooms r " +
            "JOIN floors f ON r.floor_id = f.id " +
            "JOIN blocks b ON f.block_id = b.id " +
            "WHERE b.block_name = ?";

        PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql);
        ps.setString(1, blockName);
        return ps.executeQuery();
    }

    public ResultSet getOverallStats() throws SQLException {
        String sql =
            "SELECT " +
            "  COUNT(id)                                          AS total_rooms, " +
            "  SUM(capacity)                                      AS total_capacity, " +
            "  SUM(occupied)                                      AS total_occupied, " +
            "  SUM(capacity - occupied)                           AS available_beds, " +
            "  SUM(CASE WHEN status = 'AVAILABLE'          THEN 1 ELSE 0 END) AS available_rooms, " +
            "  SUM(CASE WHEN status = 'FULL'               THEN 1 ELSE 0 END) AS full_rooms, " +
            "  SUM(CASE WHEN status = 'PARTIALLY_OCCUPIED' THEN 1 ELSE 0 END) AS partial_rooms, " +
            "  SUM(CASE WHEN status = 'MAINTENANCE'        THEN 1 ELSE 0 END) AS maintenance_rooms " +
            "FROM rooms";

        PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql);
        return ps.executeQuery();
    }

    public List<com.hostel.model.FloorStats> getFloorWiseStats() throws SQLException {
        List<com.hostel.model.FloorStats> list = new ArrayList<>();
        String sql =
            "SELECT " +
            "  b.block_name, " +
            "  f.floor_number, " +
            "  COUNT(r.id)                                  AS total_rooms, " +
            "  COALESCE(SUM(r.capacity), 0)                 AS total_capacity, " +
            "  COALESCE(SUM(r.occupied), 0)                 AS total_occupied, " +
            "  COALESCE(SUM(r.capacity - r.occupied), 0)     AS available_beds " +
            "FROM blocks b " +
            "JOIN floors f ON f.block_id = b.id " +
            "LEFT JOIN rooms r ON r.floor_id = f.id " +
            "GROUP BY b.block_name, f.floor_number, f.id, b.id " +
            "ORDER BY b.block_name ASC, f.floor_number ASC";

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new com.hostel.model.FloorStats(
                    rs.getString("block_name"),
                    rs.getInt("floor_number"),
                    rs.getInt("total_rooms"),
                    rs.getInt("total_capacity"),
                    rs.getInt("total_occupied"),
                    rs.getInt("available_beds")
                ));
            }
        }
        return list;
    }
}
