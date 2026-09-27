package com.hostel.dao;

import com.hostel.config.DBConnection;
import com.hostel.model.Student;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * StudentDAO — CRUD operations for the students table.
 *
 * All SQL uses PreparedStatement to prevent SQL injection.
 *
 * Methods:
 *   addStudent(student)              → INSERT
 *   updateStudent(student)           → UPDATE
 *   deleteStudent(id)                → DELETE (only if no active allocation)
 *   findById(id)                     → SELECT by PK
 *   findByStudentId(studentId)       → SELECT by college roll number
 *   getAllStudents()                  → SELECT all, ordered by name
 *   searchStudents(keyword)          → LIKE search on name, studentId, course
 *   getTotalCount()                  → COUNT(*) for dashboard
 */
public class StudentDAO {

    // ── Map a ResultSet row to a Student object ────────────────────────────

    private Student mapRow(ResultSet rs) throws SQLException {
        return new Student(
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
        );
    }

    // ── CREATE ─────────────────────────────────────────────────────────────

    /**
     * Inserts a new student record.
     * Sets the generated PK into the student object.
     *
     * @param student the student to insert (id field is ignored — set by DB)
     * @throws SQLException if studentId already exists (UNIQUE constraint)
     */
    public void addStudent(Student student) throws SQLException {
        String sql = "INSERT INTO students " +
                     "(student_id, name, course, year, gender, phone, email, address) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, student.getStudentId());
            ps.setString(2, student.getName());
            ps.setString(3, student.getCourse());
            ps.setInt   (4, student.getYear());
            ps.setString(5, student.getGender());
            ps.setString(6, student.getPhone());
            ps.setString(7, student.getEmail());
            ps.setString(8, student.getAddress());
            ps.executeUpdate();

            // Fetch the auto-generated id and set it on the Student object
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    student.setId(keys.getInt(1));
                }
            }
        }
    }

    // ── UPDATE ─────────────────────────────────────────────────────────────

    /**
     * Updates an existing student's details.
     *
     * @param student must have a valid id set
     * @return true if the row was updated
     * @throws SQLException on database error
     */
    public boolean updateStudent(Student student) throws SQLException {
        String sql = "UPDATE students SET " +
                     "student_id = ?, name = ?, course = ?, year = ?, " +
                     "gender = ?, phone = ?, email = ?, address = ? " +
                     "WHERE id = ?";

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, student.getStudentId());
            ps.setString(2, student.getName());
            ps.setString(3, student.getCourse());
            ps.setInt   (4, student.getYear());
            ps.setString(5, student.getGender());
            ps.setString(6, student.getPhone());
            ps.setString(7, student.getEmail());
            ps.setString(8, student.getAddress());
            ps.setInt   (9, student.getId());
            return ps.executeUpdate() == 1;
        }
    }

    // ── DELETE ─────────────────────────────────────────────────────────────

    /**
     * Deletes a student by PK.
     * Will fail (SQLException) if the student has any allocation records,
     * because of the ON DELETE RESTRICT foreign key on allocations.
     *
     * @param id the student's PK
     * @return true if deleted
     * @throws SQLException if student has allocations or does not exist
     */
    public boolean deleteStudent(int id) throws SQLException {
        String sql = "DELETE FROM students WHERE id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        }
    }

    // ── READ ──────────────────────────────────────────────────────────────

    /**
     * Find a student by their internal DB PK.
     * Returns null if not found.
     */
    public Student findById(int id) throws SQLException {
        String sql = "SELECT id, student_id, name, course, year, gender, " +
                     "phone, email, address, created_at, updated_at " +
                     "FROM students WHERE id = ?";

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    /**
     * Find a student by their college roll number (student_id column).
     * Returns null if not found.
     */
    public Student findByStudentId(String studentId) throws SQLException {
        String sql = "SELECT id, student_id, name, course, year, gender, " +
                     "phone, email, address, created_at, updated_at " +
                     "FROM students WHERE student_id = ?";

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    /**
     * Returns all students ordered by name.
     */
    public List<Student> getAllStudents() throws SQLException {
        List<Student> list = new ArrayList<>();
        String sql = "SELECT id, student_id, name, course, year, gender, " +
                     "phone, email, address, created_at, updated_at " +
                     "FROM students ORDER BY name ASC";

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    /**
     * Advanced Multi-field Student Search & Filtering.
     * Uses PreparedStatement to filter by Student ID, Name, Course, and Year.
     */
    public List<Student> searchStudentsFiltered(String studentId, String name, String course, Integer year) throws SQLException {
        List<Student> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT id, student_id, name, course, year, gender, phone, email, address, created_at, updated_at " +
            "FROM students WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();

        if (studentId != null && !studentId.trim().isEmpty()) {
            sql.append("AND student_id LIKE ? ");
            params.add("%" + studentId.trim() + "%");
        }
        if (name != null && !name.trim().isEmpty()) {
            sql.append("AND name LIKE ? ");
            params.add("%" + name.trim() + "%");
        }
        if (course != null && !course.trim().isEmpty() && !"ALL".equalsIgnoreCase(course)) {
            sql.append("AND course LIKE ? ");
            params.add("%" + course.trim() + "%");
        }
        if (year != null && year > 0) {
            sql.append("AND year = ? ");
            params.add(year);
        }

        sql.append("ORDER BY name ASC");

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    /**
     * Search students by partial match on name, student_id, or course.
     * Case-insensitive.
     *
     * @param keyword the search term (will be wrapped in %keyword%)
     * @return matching students
     */
    public List<Student> searchStudents(String keyword) throws SQLException {
        List<Student> list = new ArrayList<>();
        String pattern = "%" + keyword + "%";
        String sql = "SELECT id, student_id, name, course, year, gender, " +
                     "phone, email, address, created_at, updated_at " +
                     "FROM students " +
                     "WHERE name LIKE ? OR student_id LIKE ? OR course LIKE ? " +
                     "ORDER BY name ASC";

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    /**
     * Returns total student count.
     * Used by the Dashboard for the "Total Students" stat card.
     */
    public int getTotalCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM students";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
}
