package com.hostel.dao;

import com.hostel.config.DBConnection;
import com.hostel.model.Floor;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * FloorDAO — Data access layer for hostel floors.
 */
public class FloorDAO {

    public static class FloorStat {
        private final int floorId;
        private final int floorNumber;
        private final int roomsCount;
        private final int roomCapacity;
        private final int totalBeds;
        private final int occupiedBeds;
        private final int availableBeds;

        public FloorStat(int floorId, int floorNumber, int roomsCount, int roomCapacity,
                         int totalBeds, int occupiedBeds, int availableBeds) {
            this.floorId       = floorId;
            this.floorNumber   = floorNumber;
            this.roomsCount    = roomsCount;
            this.roomCapacity  = roomCapacity;
            this.totalBeds     = totalBeds;
            this.occupiedBeds  = occupiedBeds;
            this.availableBeds = availableBeds;
        }

        public int getFloorId()       { return floorId;       }
        public int getFloorNumber()   { return floorNumber;   }
        public int getRoomsCount()    { return roomsCount;    }
        public int getRoomCapacity()  { return roomCapacity;  }
        public int getTotalBeds()     { return totalBeds;     }
        public int getOccupiedBeds()  { return occupiedBeds;  }
        public int getAvailableBeds() { return availableBeds; }
    }

    private Floor mapRow(ResultSet rs) throws SQLException {
        return new Floor(
            rs.getInt("id"),
            rs.getInt("block_id"),
            rs.getInt("floor_number"),
            rs.getInt("rooms_count"),
            rs.getInt("room_capacity"),
            rs.getTimestamp("created_at")
        );
    }

    public List<Floor> getFloorsByBlock(int blockId) throws SQLException {
        List<Floor> list = new ArrayList<>();
        String sql = "SELECT id, block_id, floor_number, rooms_count, room_capacity, created_at " +
                     "FROM floors WHERE block_id = ? ORDER BY floor_number ASC";

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, blockId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public Floor findById(int id) throws SQLException {
        String sql = "SELECT id, block_id, floor_number, rooms_count, room_capacity, created_at " +
                     "FROM floors WHERE id = ?";

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public Floor findByBlockAndFloor(int blockId, int floorNumber) throws SQLException {
        String sql = "SELECT id, block_id, floor_number, rooms_count, room_capacity, created_at " +
                     "FROM floors WHERE block_id = ? AND floor_number = ?";

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, blockId);
            ps.setInt(2, floorNumber);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public void addFloor(Floor floor) throws SQLException {
        String sql = "INSERT INTO floors (block_id, floor_number, rooms_count, room_capacity) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, floor.getBlockId());
            ps.setInt(2, floor.getFloorNumber());
            ps.setInt(3, floor.getRoomsCount());
            ps.setInt(4, floor.getRoomCapacity());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    floor.setId(keys.getInt(1));
                }
            }
        }
    }

    public boolean updateFloor(Floor floor) throws SQLException {
        String sql = "UPDATE floors SET rooms_count = ?, room_capacity = ? WHERE id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, floor.getRoomsCount());
            ps.setInt(2, floor.getRoomCapacity());
            ps.setInt(3, floor.getId());
            return ps.executeUpdate() == 1;
        }
    }

    public boolean deleteFloor(int floorId) throws SQLException {
        String sql = "DELETE FROM floors WHERE id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, floorId);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean hasRoomsOrAllocations(int floorId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM rooms WHERE floor_id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, floorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) return true;
            }
        }

        String sqlAlloc = "SELECT COUNT(*) FROM allocations a " +
                          "JOIN rooms r ON a.room_id = r.id " +
                          "WHERE r.floor_id = ? AND a.status = 'ACTIVE'";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sqlAlloc)) {
            ps.setInt(1, floorId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /**
     * Calculates floor-by-floor breakdown live from MySQL for a specific block.
     */
    public List<FloorStat> getFloorStatsByBlock(int blockId) throws SQLException {
        List<FloorStat> list = new ArrayList<>();
        String sql =
            "SELECT " +
            "  f.id            AS floor_id, " +
            "  f.floor_number  AS floor_num, " +
            "  f.rooms_count   AS f_rooms, " +
            "  f.room_capacity AS f_cap, " +
            "  COALESCE(SUM(r.capacity), 0) AS total_beds, " +
            "  COALESCE(SUM(r.occupied), 0) AS occupied_beds " +
            "FROM floors f " +
            "LEFT JOIN rooms r ON f.id = r.floor_id " +
            "WHERE f.block_id = ? " +
            "GROUP BY f.id, f.floor_number, f.rooms_count, f.room_capacity " +
            "ORDER BY f.floor_number ASC";

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, blockId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int fid      = rs.getInt("floor_id");
                    int fnum     = rs.getInt("floor_num");
                    int rcount   = rs.getInt("f_rooms");
                    int rcap     = rs.getInt("f_cap");
                    int tbeds    = rs.getInt("total_beds");
                    int occbeds  = rs.getInt("occupied_beds");
                    int availbeds= tbeds - occbeds;

                    list.add(new FloorStat(fid, fnum, rcount, rcap, tbeds, occbeds, Math.max(0, availbeds)));
                }
            }
        }
        return list;
    }
}
