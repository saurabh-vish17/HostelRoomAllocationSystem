package com.hostel.dao;

import com.hostel.config.DBConnection;
import com.hostel.model.Allocation;
import com.hostel.model.AllocationDetail;
import com.hostel.model.Student;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * AllocationDAO — CRUD operations and queries for allocation management.
 */
public class AllocationDAO {

    private Allocation mapRow(ResultSet rs) throws SQLException {
        return new Allocation(
            rs.getInt("id"),
            rs.getInt("student_id"),
            rs.getInt("room_id"),
            rs.getDate("alloc_date"),
            rs.getDate("vacate_date"),
            rs.getString("status"),
            rs.getString("remarks"),
            rs.getTimestamp("created_at"),
            rs.getTimestamp("updated_at")
        );
    }

    /**
     * Checks if a student currently has an ACTIVE allocation.
     */
    public boolean hasActiveAllocation(int studentId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM allocations WHERE student_id = ? AND status = 'ACTIVE'";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /**
     * Fetches unallocated students (students without an ACTIVE allocation).
     */
    public List<Student> getUnallocatedStudents() throws SQLException {
        List<Student> list = new ArrayList<>();
        String sql = "SELECT id, student_id, name, course, year, gender, phone, email, address, created_at, updated_at " +
                     "FROM students " +
                     "WHERE id NOT IN (SELECT student_id FROM allocations WHERE status = 'ACTIVE') " +
                     "ORDER BY name ASC";

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Student(
                    rs.getInt("id"),
                    rs.getString("student_id"),
                    rs.getString("name"),
                    rs.getString("course"),
                    rs.getInt("year"),
                    rs.getString("gender"),
                    rs.getString("phone"),
                    rs.getString("email"),
                    rs.getString("address"),
                    rs.getTimestamp("created_at"),
                    rs.getTimestamp("updated_at")
                ));
            }
        }
        return list;
    }

    /**
     * Fetches all active allocations with combined student, room, floor, and block details.
     */
    public List<AllocationDetail> getAllActiveAllocationsDetailedList() throws SQLException {
        return searchActiveAllocations(null);
    }

    /**
     * Advanced Multi-Criteria Allocation Search & Filtering.
     * Filter by Student (Name/ID), Room number, Block name, and Status (ACTIVE, VACATED, or ALL).
     */
    public List<AllocationDetail> searchAllocationsFiltered(String searchQuery, String blockName, String statusFilter) throws SQLException {
        List<AllocationDetail> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT " +
            "  a.id            AS alloc_id, " +
            "  s.id            AS student_db_id, " +
            "  s.student_id    AS student_roll, " +
            "  s.name          AS student_name, " +
            "  s.course        AS student_course, " +
            "  s.year          AS student_year, " +
            "  r.id            AS room_id, " +
            "  b.block_name    AS block_name, " +
            "  f.floor_number  AS floor_number, " +
            "  r.room_number   AS room_number, " +
            "  r.capacity      AS room_capacity, " +
            "  r.occupied      AS room_occupied, " +
            "  a.alloc_date    AS alloc_date, " +
            "  a.vacate_date   AS vacate_date, " +
            "  a.status        AS alloc_status, " +
            "  a.remarks       AS remarks " +
            "FROM allocations a " +
            "JOIN students s ON a.student_id = s.id " +
            "JOIN rooms    r ON a.room_id    = r.id " +
            "JOIN floors   f ON r.floor_id   = f.id " +
            "JOIN blocks   b ON f.block_id   = b.id " +
            "WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();

        if (statusFilter != null && !statusFilter.trim().isEmpty() && !"ALL".equalsIgnoreCase(statusFilter)) {
            sql.append("AND a.status = ? ");
            params.add(statusFilter.trim().toUpperCase());
        }

        if (blockName != null && !blockName.trim().isEmpty() && !"ALL".equalsIgnoreCase(blockName)) {
            sql.append("AND b.block_name = ? ");
            params.add(blockName.trim().toUpperCase());
        }

        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            sql.append("AND (s.student_id LIKE ? OR s.name LIKE ? OR r.room_number LIKE ?) ");
            String pattern = "%" + searchQuery.trim() + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }

        sql.append("ORDER BY a.alloc_date DESC, r.room_number ASC");

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new AllocationDetail(
                        rs.getInt("alloc_id"),
                        rs.getInt("student_db_id"),
                        rs.getString("student_roll"),
                        rs.getString("student_name"),
                        rs.getString("student_course"),
                        rs.getInt("student_year"),
                        rs.getInt("room_id"),
                        rs.getString("block_name"),
                        rs.getInt("floor_number"),
                        rs.getString("room_number"),
                        rs.getInt("room_capacity"),
                        rs.getInt("room_occupied"),
                        rs.getDate("alloc_date"),
                        rs.getDate("vacate_date"),
                        rs.getString("alloc_status"),
                        rs.getString("remarks")
                    ));
                }
            }
        }
        return list;
    }

    /**
     * Searches active allocations by Student ID (Roll number), Student Name, or Room Number.
     */
    public List<AllocationDetail> searchActiveAllocations(String searchQuery) throws SQLException {
        List<AllocationDetail> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT " +
            "  a.id            AS alloc_id, " +
            "  s.id            AS student_db_id, " +
            "  s.student_id    AS student_roll, " +
            "  s.name          AS student_name, " +
            "  s.course        AS student_course, " +
            "  s.year          AS student_year, " +
            "  r.id            AS room_id, " +
            "  b.block_name    AS block_name, " +
            "  f.floor_number  AS floor_number, " +
            "  r.room_number   AS room_number, " +
            "  r.capacity      AS room_capacity, " +
            "  r.occupied      AS room_occupied, " +
            "  a.alloc_date    AS alloc_date, " +
            "  a.vacate_date   AS vacate_date, " +
            "  a.status        AS alloc_status, " +
            "  a.remarks       AS remarks " +
            "FROM allocations a " +
            "JOIN students s ON a.student_id = s.id " +
            "JOIN rooms    r ON a.room_id    = r.id " +
            "JOIN floors   f ON r.floor_id   = f.id " +
            "JOIN blocks   b ON f.block_id   = b.id " +
            "WHERE a.status = 'ACTIVE' "
        );

        boolean hasSearch = searchQuery != null && !searchQuery.trim().isEmpty();
        if (hasSearch) {
            sql.append("AND (s.student_id LIKE ? OR s.name LIKE ? OR r.room_number LIKE ?) ");
        }

        sql.append("ORDER BY r.room_number ASC, s.name ASC");

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql.toString())) {
            if (hasSearch) {
                String term = "%" + searchQuery.trim() + "%";
                ps.setString(1, term);
                ps.setString(2, term);
                ps.setString(3, term);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new AllocationDetail(
                        rs.getInt("alloc_id"),
                        rs.getInt("student_db_id"),
                        rs.getString("student_roll"),
                        rs.getString("student_name"),
                        rs.getString("student_course"),
                        rs.getInt("student_year"),
                        rs.getInt("room_id"),
                        rs.getString("block_name"),
                        rs.getInt("floor_number"),
                        rs.getString("room_number"),
                        rs.getInt("room_capacity"),
                        rs.getInt("room_occupied"),
                        rs.getDate("alloc_date"),
                        rs.getDate("vacate_date"),
                        rs.getString("alloc_status"),
                        rs.getString("remarks")
                    ));
                }
            }
        }
        return list;
    }

    /**
     * Inserts an allocation within a transaction.
     */
    public int insertAllocationTx(Connection conn, int studentId, int roomId, String remarks) throws SQLException {
        String sql = "INSERT INTO allocations (student_id, room_id, alloc_date, vacate_date, status, remarks) " +
                     "VALUES (?, ?, CURRENT_DATE, NULL, 'ACTIVE', ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, studentId);
            ps.setInt(2, roomId);
            ps.setString(3, remarks != null ? remarks : "Room allocated");
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Failed to retrieve generated key for allocation.");
    }

    public int getActiveAllocationCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM allocations WHERE status = 'ACTIVE'";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public com.hostel.model.AllocationHistoryStats getAllocationHistoryStats() throws SQLException {
        String sql =
            "SELECT " +
            "  COUNT(*)                                                       AS total_allocations, " +
            "  COALESCE(SUM(CASE WHEN status = 'ACTIVE'  THEN 1 ELSE 0 END), 0) AS active_allocations, " +
            "  COALESCE(SUM(CASE WHEN status = 'VACATED' THEN 1 ELSE 0 END), 0) AS vacated_allocations " +
            "FROM allocations";

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return new com.hostel.model.AllocationHistoryStats(
                    rs.getInt("active_allocations"),
                    rs.getInt("vacated_allocations"),
                    rs.getInt("total_allocations")
                );
            }
        }
        return new com.hostel.model.AllocationHistoryStats(0, 0, 0);
    }
}
