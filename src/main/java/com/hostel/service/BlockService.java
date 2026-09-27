package com.hostel.service;

import com.hostel.dao.BlockDAO;
import com.hostel.dao.FloorDAO;
import com.hostel.dao.FloorDAO.FloorStat;
import com.hostel.model.Block;
import com.hostel.model.Floor;

import java.sql.SQLException;
import java.util.List;

/**
 * BlockService — Business logic layer for Block & Floor management.
 *
 * Enforces safety rules:
 *   - Cannot delete a Block or Floor if rooms/allocations exist.
 *   - Validates room counts and capacity boundaries (> 0).
 */
public class BlockService {

    private final BlockDAO blockDAO;
    private final FloorDAO floorDAO;

    public BlockService() {
        this.blockDAO = new BlockDAO();
        this.floorDAO = new FloorDAO();
    }

    public List<Block> getAllBlocks() throws SQLException {
        return blockDAO.getAllBlocks();
    }

    public Block getBlockById(int id) throws SQLException {
        return blockDAO.findById(id);
    }

    public Block getBlockByName(String name) throws SQLException {
        if (name == null || name.trim().isEmpty()) return null;
        return blockDAO.findByName(name.trim().toUpperCase());
    }

    public List<Floor> getFloorsByBlock(int blockId) throws SQLException {
        return floorDAO.getFloorsByBlock(blockId);
    }

    public List<FloorStat> getFloorStatsByBlock(int blockId) throws SQLException {
        return floorDAO.getFloorStatsByBlock(blockId);
    }

    public Floor getFloorById(int floorId) throws SQLException {
        return floorDAO.findById(floorId);
    }

    /**
     * Updates rooms count or default seating capacity for a floor.
     *
     * @param floorId      Floor PK
     * @param roomsCount   Number of rooms
     * @param roomCapacity Default seating capacity (2 or 3)
     * @throws IllegalArgumentException if validation fails
     */
    public void updateFloor(int floorId, int roomsCount, int roomCapacity) throws IllegalArgumentException, SQLException {
        if (floorId <= 0) {
            throw new IllegalArgumentException("Invalid floor ID.");
        }
        if (roomsCount <= 0) {
            throw new IllegalArgumentException("Rooms count must be greater than 0.");
        }
        if (roomCapacity <= 0 || roomCapacity > 10) {
            throw new IllegalArgumentException("Room capacity must be between 1 and 10 beds.");
        }

        Floor floor = floorDAO.findById(floorId);
        if (floor == null) {
            throw new IllegalArgumentException("Floor not found.");
        }

        floor.setRoomsCount(roomsCount);
        floor.setRoomCapacity(roomCapacity);

        boolean updated = floorDAO.updateFloor(floor);
        if (!updated) {
            throw new IllegalArgumentException("Failed to update floor record.");
        }
    }

    /**
     * Deletes a floor if no rooms or allocations depend on it.
     */
    public void deleteFloor(int floorId) throws IllegalArgumentException, SQLException {
        if (floorId <= 0) {
            throw new IllegalArgumentException("Please select a valid floor to delete.");
        }

        boolean hasDependencies = floorDAO.hasRoomsOrAllocations(floorId);
        if (hasDependencies) {
            throw new IllegalArgumentException(
                "Cannot delete this floor because rooms or student allocations exist for it.\n" +
                "Deletion is blocked to preserve data integrity."
            );
        }

        boolean deleted = floorDAO.deleteFloor(floorId);
        if (!deleted) {
            throw new IllegalArgumentException("Floor not found or already deleted.");
        }
    }

    /**
     * Deletes a block if no rooms or allocations depend on it.
     */
    public void deleteBlock(int blockId) throws IllegalArgumentException, SQLException {
        if (blockId <= 0) {
            throw new IllegalArgumentException("Please select a valid block to delete.");
        }

        boolean hasDependencies = blockDAO.hasRoomsOrAllocations(blockId);
        if (hasDependencies) {
            throw new IllegalArgumentException(
                "Cannot delete this Block because floors, rooms, or student allocations exist for it.\n" +
                "Deletion is blocked to preserve data integrity."
            );
        }

        boolean deleted = blockDAO.deleteBlock(blockId);
        if (!deleted) {
            throw new IllegalArgumentException("Block not found or already deleted.");
        }
    }
}
