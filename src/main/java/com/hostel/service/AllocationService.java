package com.hostel.service;

import com.hostel.config.DBConnection;
import com.hostel.dao.AllocationDAO;
import com.hostel.dao.RoomDAO;
import com.hostel.dao.StudentDAO;
import com.hostel.model.AllocationDetail;
import com.hostel.model.Room;
import com.hostel.model.Student;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * AllocationService — business logic layer for Room Allocation and Vacating operations.
 */
public class AllocationService {

    private final AllocationDAO allocationDAO;
    private final StudentDAO    studentDAO;
    private final RoomDAO       roomDAO;

    public AllocationService() {
        this.allocationDAO = new AllocationDAO();
        this.studentDAO    = new StudentDAO();
        this.roomDAO       = new RoomDAO();
    }

    public List<Student> getUnallocatedStudents() throws SQLException {
        return allocationDAO.getUnallocatedStudents();
    }

    public List<AllocationDetail> getAllActiveAllocations() throws SQLException {
        return allocationDAO.getAllActiveAllocationsDetailedList();
    }

    public List<AllocationDetail> searchActiveAllocations(String query) throws SQLException {
        return allocationDAO.searchActiveAllocations(query);
    }

    /**
     * Executes a complete room allocation workflow inside a safe SQL Transaction.
     */
    public void allocateRoom(int studentDbId, int roomId, String remarks) throws IllegalArgumentException, SQLException {
        if (studentDbId <= 0) {
            throw new IllegalArgumentException("Please select a valid student.");
        }
        if (roomId <= 0) {
            throw new IllegalArgumentException("Please select a valid room.");
        }

        Connection conn = DBConnection.getConnection();
        boolean originalAutoCommit = conn.getAutoCommit();

        try {
            conn.setAutoCommit(false);

            // 1. Verify Student Exists
            Student student = studentDAO.findById(studentDbId);
            if (student == null) {
                throw new IllegalArgumentException("Selected student (ID: " + studentDbId + ") does not exist.");
            }

            // 2. Verify Student does NOT have an ACTIVE allocation
            String checkStudentSql = "SELECT COUNT(*) FROM allocations WHERE student_id = ? AND status = 'ACTIVE'";
            try (PreparedStatement ps = conn.prepareStatement(checkStudentSql)) {
                ps.setInt(1, studentDbId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        throw new IllegalArgumentException(
                            "Student " + student.getName() + " (" + student.getStudentId() + ") already has an ACTIVE room allocation.\n" +
                            "A student cannot be allocated to multiple rooms simultaneously."
                        );
                    }
                }
            }

            // 3. Verify Room Status and Lock Row (FOR UPDATE)
            String lockRoomSql = "SELECT room_number, capacity, occupied, status FROM rooms WHERE id = ? FOR UPDATE";
            String roomNumber;
            int capacity;
            int currentOccupied;
            String roomStatus;

            try (PreparedStatement ps = conn.prepareStatement(lockRoomSql)) {
                ps.setInt(1, roomId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException("Selected room (ID: " + roomId + ") does not exist.");
                    }
                    roomNumber      = rs.getString("room_number");
                    capacity        = rs.getInt("capacity");
                    currentOccupied = rs.getInt("occupied");
                    roomStatus      = rs.getString("status");
                }
            }

            // 4. Validate Maintenance Status
            if (Room.STATUS_MAINTENANCE.equals(roomStatus)) {
                throw new IllegalArgumentException(
                    "Room " + roomNumber + " is currently under MAINTENANCE.\n" +
                    "Allocations cannot be made for rooms undergoing maintenance."
                );
            }

            // 5. Validate Capacity
            if (currentOccupied >= capacity) {
                throw new IllegalArgumentException(
                    "Room " + roomNumber + " is FULL (" + currentOccupied + "/" + capacity + " beds occupied).\n" +
                    "No available beds remaining."
                );
            }

            // 6. Create Allocation Record
            allocationDAO.insertAllocationTx(conn, studentDbId, roomId, remarks);

            // 7. Calculate New Occupied Count & Recalculate Room Status
            int newOccupied = currentOccupied + 1;
            String newStatus;

            if (newOccupied >= capacity) {
                newStatus = Room.STATUS_FULL;
            } else if (newOccupied > 0) {
                newStatus = Room.STATUS_PARTIALLY_OCCUPIED;
            } else {
                newStatus = Room.STATUS_AVAILABLE;
            }

            // 8. Update Room Occupancy & Status
            String updateRoomSql = "UPDATE rooms SET occupied = ?, status = ? WHERE id = ? AND ? <= capacity";
            try (PreparedStatement ps = conn.prepareStatement(updateRoomSql)) {
                ps.setInt   (1, newOccupied);
                ps.setString(2, newStatus);
                ps.setInt   (3, roomId);
                ps.setInt   (4, newOccupied);

                int rowsUpdated = ps.executeUpdate();
                if (rowsUpdated != 1) {
                    throw new SQLException("Failed to update room occupancy for Room " + roomNumber + ". Capacity limit violated.");
                }
            }

            // 9. Commit Transaction
            conn.commit();

        } catch (Exception ex) {
            conn.rollback();
            throw ex;
        } finally {
            conn.setAutoCommit(originalAutoCommit);
        }
    }

    /**
     * Executes a complete room vacating workflow inside a safe SQL Transaction.
     *
     * Rules enforced:
     *   1. Finds active allocation.
     *   2. Sets vacate_date = CURRENT_DATE.
     *   3. Changes allocation status to 'VACATED' (historical record PRESERVED, never deleted!).
     *   4. Decreases room occupied count (occupied = occupied - 1).
     *   5. Recalculates room status (AVAILABLE, PARTIALLY_OCCUPIED, FULL, or MAINTENANCE).
     *   6. Rolls back all changes if any error occurs.
     *
     * @param allocationId Primary key of the allocation record (allocations.id)
     * @throws IllegalArgumentException if active allocation not found
     * @throws SQLException             if database operations fail
     */
    public void vacateRoom(int allocationId) throws IllegalArgumentException, SQLException {
        if (allocationId <= 0) {
            throw new IllegalArgumentException("Please select a valid active allocation to vacate.");
        }

        Connection conn = DBConnection.getConnection();
        boolean originalAutoCommit = conn.getAutoCommit();

        try {
            conn.setAutoCommit(false);

            // 1. Lock Allocation and Room records (FOR UPDATE)
            String lockSql =
                "SELECT a.id, a.student_id, a.room_id, a.status AS alloc_status, " +
                "       r.room_number, r.capacity, r.occupied, r.status AS room_status " +
                "FROM allocations a " +
                "JOIN rooms r ON a.room_id = r.id " +
                "WHERE a.id = ? FOR UPDATE";

            int roomId;
            int currentOccupied;
            int capacity;
            String roomStatus;
            String roomNumber;

            try (PreparedStatement ps = conn.prepareStatement(lockSql)) {
                ps.setInt(1, allocationId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException("Allocation record (ID: " + allocationId + ") does not exist.");
                    }
                    String allocStatus = rs.getString("alloc_status");
                    if (!"ACTIVE".equals(allocStatus)) {
                        throw new IllegalArgumentException("This allocation record is already VACATED.");
                    }

                    roomId          = rs.getInt("room_id");
                    currentOccupied = rs.getInt("occupied");
                    capacity        = rs.getInt("capacity");
                    roomStatus      = rs.getString("room_status");
                    roomNumber      = rs.getString("room_number");
                }
            }

            // 2. Mark Allocation as VACATED and set vacate_date = CURRENT_DATE
            String vacateAllocSql = "UPDATE allocations SET status = 'VACATED', vacate_date = CURRENT_DATE WHERE id = ? AND status = 'ACTIVE'";
            try (PreparedStatement ps = conn.prepareStatement(vacateAllocSql)) {
                ps.setInt(1, allocationId);
                int updatedRows = ps.executeUpdate();
                if (updatedRows != 1) {
                    throw new SQLException("Failed to update allocation status for allocation ID: " + allocationId);
                }
            }

            // 3. Decrease Occupied Count & Recalculate Room Status
            int newOccupied = Math.max(0, currentOccupied - 1);
            String newStatus;

            if (Room.STATUS_MAINTENANCE.equals(roomStatus)) {
                newStatus = Room.STATUS_MAINTENANCE; // Remain in MAINTENANCE
            } else if (newOccupied == 0) {
                newStatus = Room.STATUS_AVAILABLE;
            } else if (newOccupied < capacity) {
                newStatus = Room.STATUS_PARTIALLY_OCCUPIED;
            } else {
                newStatus = Room.STATUS_FULL;
            }

            // 4. Update Room Occupancy & Status in Database
            String updateRoomSql = "UPDATE rooms SET occupied = ?, status = ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(updateRoomSql)) {
                ps.setInt   (1, newOccupied);
                ps.setString(2, newStatus);
                ps.setInt   (3, roomId);

                int roomUpdated = ps.executeUpdate();
                if (roomUpdated != 1) {
                    throw new SQLException("Failed to update room occupancy for Room " + roomNumber + ".");
                }
            }

            // 5. Commit Transaction
            conn.commit();

        } catch (Exception ex) {
            conn.rollback();
            throw ex;
        } finally {
            conn.setAutoCommit(originalAutoCommit);
        }
    }

    public List<AllocationDetail> searchAllocationsFiltered(String searchQuery, String blockName, String statusFilter) throws SQLException {
        return allocationDAO.searchAllocationsFiltered(searchQuery, blockName, statusFilter);
    }
}
