package com.hostel.dao;

import com.hostel.config.DBConnection;
import com.hostel.model.Block;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * BlockDAO — Data access layer for hostel blocks (D, G, H).
 *
 * Methods:
 *   getAllBlocks()                     → returns D, G, H ordered by block_name
 *   findById(id)                      → lookup by PK
 *   findByName(name)                  → lookup by "D", "G", or "H"
 *   addBlock(block)                   → insert new block
 *   updateBlock(block)                → update description / total_floors
 *   deleteBlock(id)                   → delete block (if no dependent rooms)
 *   hasRoomsOrAllocations(blockId)    → check dependencies before deletion
 */
public class BlockDAO {

    private Block mapRow(ResultSet rs) throws SQLException {
        return new Block(
            rs.getInt("id"),
            rs.getString("block_name"),
            rs.getInt("total_floors"),
            rs.getString("description"),
            rs.getTimestamp("created_at")
        );
    }

    public List<Block> getAllBlocks() throws SQLException {
        List<Block> list = new ArrayList<>();
        String sql = "SELECT id, block_name, total_floors, description, created_at " +
                     "FROM blocks ORDER BY block_name ASC";

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    public Block findById(int id) throws SQLException {
        String sql = "SELECT id, block_name, total_floors, description, created_at " +
                     "FROM blocks WHERE id = ?";

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public Block findByName(String blockName) throws SQLException {
        String sql = "SELECT id, block_name, total_floors, description, created_at " +
                     "FROM blocks WHERE block_name = ?";

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, blockName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public void addBlock(Block block) throws SQLException {
        String sql = "INSERT INTO blocks (block_name, total_floors, description) VALUES (?, ?, ?)";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, block.getBlockName());
            ps.setInt(2, block.getTotalFloors());
            ps.setString(3, block.getDescription());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    block.setId(keys.getInt(1));
                }
            }
        }
    }

    public boolean updateBlock(Block block) throws SQLException {
        String sql = "UPDATE blocks SET block_name = ?, total_floors = ?, description = ? WHERE id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, block.getBlockName());
            ps.setInt(2, block.getTotalFloors());
            ps.setString(3, block.getDescription());
            ps.setInt(4, block.getId());
            return ps.executeUpdate() == 1;
        }
    }

    public boolean deleteBlock(int blockId) throws SQLException {
        String sql = "DELETE FROM blocks WHERE id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, blockId);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean hasRoomsOrAllocations(int blockId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM rooms r JOIN floors f ON r.floor_id = f.id WHERE f.block_id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, blockId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) return true;
            }
        }

        String sqlAlloc = "SELECT COUNT(*) FROM allocations a " +
                          "JOIN rooms r ON a.room_id = r.id " +
                          "JOIN floors f ON r.floor_id = f.id " +
                          "WHERE f.block_id = ? AND a.status = 'ACTIVE'";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sqlAlloc)) {
            ps.setInt(1, blockId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }
}
