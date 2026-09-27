package com.hostel.service;

import com.hostel.config.DBConnection;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * RoomInitializationService — safe room generation and infrastructure verification service.
 *
 * Checks if rooms exist for each block and floor according to the hostel specification,
 * generating any missing rooms within a database transaction while guaranteeing no duplicates.
 *
 * Hostel Infrastructure Specification:
 *   - Block D: 5 floors × 12 rooms = 60 rooms (F1-2: 3 beds, F3-5: 2 beds)
 *   - Block G: 4 floors × 53 rooms = 212 rooms (F1,3,4: 2 beds, F2: 3 beds)
 *   - Block H: 6 floors × 30 rooms = 180 rooms (F1,5,6: 2 beds, F2-4: 3 beds)
 *   - Grand Total: 452 rooms
 */
public class RoomInitializationService {

    public static class RoomInitializationResult {
        private final int blockDRooms;
        private final int blockGRooms;
        private final int blockHRooms;
        private final int totalRooms;
        private final int roomsGenerated;
        private final int roomsSkipped;

        public RoomInitializationResult(int blockDRooms, int blockGRooms, int blockHRooms,
                                        int totalRooms, int roomsGenerated, int roomsSkipped) {
            this.blockDRooms    = blockDRooms;
            this.blockGRooms    = blockGRooms;
            this.blockHRooms    = blockHRooms;
            this.totalRooms     = totalRooms;
            this.roomsGenerated = roomsGenerated;
            this.roomsSkipped   = roomsSkipped;
        }

        public int getBlockDRooms()    { return blockDRooms;    }
        public int getBlockGRooms()    { return blockGRooms;    }
        public int getBlockHRooms()    { return blockHRooms;    }
        public int getTotalRooms()     { return totalRooms;     }
        public int getRoomsGenerated() { return roomsGenerated; }
        public int getRoomsSkipped()   { return roomsSkipped;   }

        @Override
        public String toString() {
            return String.format(
                "Room Initialization Summary:\n" +
                "  D = %d rooms\n" +
                "  G = %d rooms\n" +
                "  H = %d rooms\n" +
                "  Total = %d rooms\n" +
                "  Newly Generated = %d\n" +
                "  Skipped (Already Exists) = %d",
                blockDRooms, blockGRooms, blockHRooms, totalRooms, roomsGenerated, roomsSkipped
            );
        }
    }

    private static class FloorSpec {
        final int floorNumber;
        final int roomsCount;
        final int roomCapacity;

        FloorSpec(int floorNumber, int roomsCount, int roomCapacity) {
            this.floorNumber  = floorNumber;
            this.roomsCount   = roomsCount;
            this.roomCapacity = roomCapacity;
        }
    }

    /**
     * Verifies room existence and generates any missing rooms safely using SQL transactions.
     * Guaranteed never to duplicate existing rooms.
     */
    public RoomInitializationResult initializeOrVerifyRooms() throws SQLException {
        Connection conn = DBConnection.getConnection();
        boolean originalAutoCommit = conn.getAutoCommit();

        int generatedCount = 0;
        int skippedCount   = 0;

        try {
            conn.setAutoCommit(false);

            // Define Specifications for D, G, H
            FloorSpec[] specsD = {
                new FloorSpec(1, 12, 3),
                new FloorSpec(2, 12, 3),
                new FloorSpec(3, 12, 2),
                new FloorSpec(4, 12, 2),
                new FloorSpec(5, 12, 2)
            };

            FloorSpec[] specsG = {
                new FloorSpec(1, 53, 2),
                new FloorSpec(2, 53, 3),
                new FloorSpec(3, 53, 2),
                new FloorSpec(4, 53, 2)
            };

            FloorSpec[] specsH = {
                new FloorSpec(1, 30, 2),
                new FloorSpec(2, 30, 3),
                new FloorSpec(3, 30, 3),
                new FloorSpec(4, 30, 3),
                new FloorSpec(5, 30, 2),
                new FloorSpec(6, 30, 2)
            };

            Map<String, FloorSpec[]> specMap = new HashMap<>();
            specMap.put("D", specsD);
            specMap.put("G", specsG);
            specMap.put("H", specsH);

            for (Map.Entry<String, FloorSpec[]> entry : specMap.entrySet()) {
                String blockName = entry.getKey();
                FloorSpec[] specs = entry.getValue();

                int blockId = getOrCreateBlock(conn, blockName, specs.length);

                for (FloorSpec spec : specs) {
                    int floorId = getOrCreateFloor(conn, blockId, spec.floorNumber, spec.roomsCount, spec.roomCapacity);

                    for (int i = 1; i <= spec.roomsCount; i++) {
                        String roomNumber = String.format("%s%d%02d", blockName, spec.floorNumber, i);

                        if (roomExists(conn, roomNumber)) {
                            skippedCount++;
                        } else {
                            insertRoom(conn, floorId, roomNumber, spec.roomCapacity);
                            generatedCount++;
                        }
                    }
                }
            }

            conn.commit();

        } catch (Exception ex) {
            conn.rollback();
            throw new SQLException("Failed to initialize/verify rooms. Transaction rolled back.\n" + ex.getMessage(), ex);
        } finally {
            conn.setAutoCommit(originalAutoCommit);
        }

        // Fetch counts after initialization
        int countD     = getRoomCountForBlock("D");
        int countG     = getRoomCountForBlock("G");
        int countH     = getRoomCountForBlock("H");
        int countTotal = getTotalRoomCount();

        return new RoomInitializationResult(countD, countG, countH, countTotal, generatedCount, skippedCount);
    }

    // ── Database Transaction Helpers ───────────────────────────────────────

    private int getOrCreateBlock(Connection conn, String blockName, int totalFloors) throws SQLException {
        String selectSql = "SELECT id FROM blocks WHERE block_name = ?";
        try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
            ps.setString(1, blockName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        }

        String insertSql = "INSERT INTO blocks (block_name, total_floors, description) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, blockName);
            ps.setInt(2, totalFloors);
            ps.setString(3, "Block " + blockName + " hostel infrastructure");
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        throw new SQLException("Failed to resolve Block ID for: " + blockName);
    }

    private int getOrCreateFloor(Connection conn, int blockId, int floorNumber, int roomsCount, int roomCapacity) throws SQLException {
        String selectSql = "SELECT id FROM floors WHERE block_id = ? AND floor_number = ?";
        try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
            ps.setInt(1, blockId);
            ps.setInt(2, floorNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        }

        String insertSql = "INSERT INTO floors (block_id, floor_number, rooms_count, room_capacity) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, blockId);
            ps.setInt(2, floorNumber);
            ps.setInt(3, roomsCount);
            ps.setInt(4, roomCapacity);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        throw new SQLException("Failed to resolve Floor ID for block: " + blockId + ", floor: " + floorNumber);
    }

    private boolean roomExists(Connection conn, String roomNumber) throws SQLException {
        String sql = "SELECT COUNT(*) FROM rooms WHERE room_number = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roomNumber);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private void insertRoom(Connection conn, int floorId, String roomNumber, int capacity) throws SQLException {
        String sql = "INSERT INTO rooms (floor_id, room_number, capacity, occupied, status) VALUES (?, ?, ?, 0, 'AVAILABLE')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, floorId);
            ps.setString(2, roomNumber);
            ps.setInt(3, capacity);
            ps.executeUpdate();
        }
    }

    // ── Count Reporting Queries ────────────────────────────────────────────

    public int getRoomCountForBlock(String blockName) throws SQLException {
        String sql = "SELECT COUNT(r.id) FROM rooms r " +
                     "JOIN floors f ON r.floor_id = f.id " +
                     "JOIN blocks b ON f.block_id = b.id " +
                     "WHERE b.block_name = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, blockName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public int getTotalRoomCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM rooms";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
}
