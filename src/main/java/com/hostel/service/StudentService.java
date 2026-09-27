package com.hostel.service;

import com.hostel.dao.AllocationDAO;
import com.hostel.dao.StudentDAO;
import com.hostel.model.Student;

import java.sql.SQLException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * StudentService — Business logic layer for student management.
 *
 * Responsibilities:
 *   1. Validate mandatory fields (ID, Name, Course, Year, Phone, Email, Address)
 *   2. Enforce email format via Regex
 *   3. Enforce phone format validation
 *   4. Check for duplicate Student IDs on Add and Update
 *   5. Prevent deletion of students with active hostel allocations
 *   6. Delegate CRUD execution to StudentDAO
 */
public class StudentService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^[0-9+ -]{10,15}$"
    );

    private final StudentDAO    studentDAO;
    private final AllocationDAO allocationDAO;

    public StudentService() {
        this.studentDAO    = new StudentDAO();
        this.allocationDAO = new AllocationDAO();
    }

    // ── CREATE ─────────────────────────────────────────────────────────────

    /**
     * Adds a new student after full validation.
     *
     * @param student Student object to create
     * @throws IllegalArgumentException if validation fails
     * @throws SQLException on DB operation error
     */
    public void addStudent(Student student) throws IllegalArgumentException, SQLException {
        validateStudent(student);

        // Duplicate Student ID check
        Student existing = studentDAO.findByStudentId(student.getStudentId().trim());
        if (existing != null) {
            throw new IllegalArgumentException(
                "A student with Student ID '" + student.getStudentId() + "' already exists."
            );
        }

        studentDAO.addStudent(student);
    }

    // ── UPDATE ─────────────────────────────────────────────────────────────

    /**
     * Updates an existing student record after validation.
     *
     * @param student Student object with valid PK id set
     * @throws IllegalArgumentException if validation fails
     * @throws SQLException on DB operation error
     */
    public void updateStudent(Student student) throws IllegalArgumentException, SQLException {
        if (student.getId() <= 0) {
            throw new IllegalArgumentException("Invalid student ID for update operation.");
        }

        validateStudent(student);

        // Check duplicate Student ID assigned to ANOTHER student
        Student existing = studentDAO.findByStudentId(student.getStudentId().trim());
        if (existing != null && existing.getId() != student.getId()) {
            throw new IllegalArgumentException(
                "Student ID '" + student.getStudentId() + "' is already assigned to another student (" +
                existing.getName() + ")."
            );
        }

        boolean updated = studentDAO.updateStudent(student);
        if (!updated) {
            throw gravitationException("Failed to update student record. Record not found.");
        }
    }

    private IllegalArgumentException gravitationException(String msg) {
        return new IllegalArgumentException(msg);
    }

    // ── DELETE ─────────────────────────────────────────────────────────────

    /**
     * Deletes a student by PK ID.
     * Prevents deletion if student has an active allocation.
     *
     * @param id Student PK
     * @throws IllegalArgumentException if student is currently allocated a room
     * @throws SQLException on DB deletion error
     */
    public void deleteStudent(int id) throws IllegalArgumentException, SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("Please select a student to delete.");
        }

        // Check if student has an active allocation
        boolean hasActiveAllocation = allocationDAO.hasActiveAllocation(id);
        if (hasActiveAllocation) {
            throw new IllegalArgumentException(
                "Cannot delete this student because they currently have an ACTIVE room allocation.\n" +
                "Please vacate the student from their room before deleting their profile."
            );
        }

        try {
            boolean deleted = studentDAO.deleteStudent(id);
            if (!deleted) {
                throw new IllegalArgumentException("Student record not found or already deleted.");
            }
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("foreign key constraint")) {
                throw new IllegalArgumentException(
                    "Cannot delete student with past allocation history.\n" +
                    "To maintain audit records, students with allocation history cannot be deleted."
                );
            }
            throw e;
        }
    }

    // ── READ / SEARCH ──────────────────────────────────────────────────────

    public List<Student> getAllStudents() throws SQLException {
        return studentDAO.getAllStudents();
    }

    public List<Student> searchStudents(String keyword) throws SQLException {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllStudents();
        }
        return studentDAO.searchStudents(keyword.trim());
    }

    public List<Student> searchStudentsFiltered(String studentId, String name, String course, Integer year) throws SQLException {
        return studentDAO.searchStudentsFiltered(studentId, name, course, year);
    }

    public Student getStudentById(int id) throws SQLException {
        return studentDAO.findById(id);
    }

    public Student getStudentByStudentId(String studentId) throws SQLException {
        if (studentId == null || studentId.trim().isEmpty()) return null;
        return studentDAO.findByStudentId(studentId.trim());
    }

    // ── VALIDATION RULES ───────────────────────────────────────────────────

    private void validateStudent(Student s) throws IllegalArgumentException {
        if (s == null) {
            throw new IllegalArgumentException("Student data cannot be empty.");
        }

        if (s.getStudentId() == null || s.getStudentId().trim().isEmpty()) {
            throw new IllegalArgumentException("Student ID / Roll Number is required.");
        }

        if (s.getName() == null || s.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Student Full Name is required.");
        }
        if (s.getName().trim().length() < 2) {
            throw new IllegalArgumentException("Student Name must be at least 2 characters long.");
        }

        if (s.getCourse() == null || s.getCourse().trim().isEmpty()) {
            throw new IllegalArgumentException("Course is required.");
        }

        if (s.getYear() < 1 || s.getYear() > 5) {
            throw new IllegalArgumentException("Year must be between 1 and 5.");
        }

        if (s.getGender() == null || s.getGender().trim().isEmpty()) {
            throw new IllegalArgumentException("Gender selection is required.");
        }

        if (s.getPhone() == null || s.getPhone().trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number is required.");
        }
        if (!PHONE_PATTERN.matcher(s.getPhone().trim()).matches()) {
            throw new IllegalArgumentException("Phone number must contain 10–15 digits (e.g., 9876543210).");
        }

        if (s.getEmail() == null || s.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email address is required.");
        }
        if (!EMAIL_PATTERN.matcher(s.getEmail().trim()).matches()) {
            throw new IllegalArgumentException("Invalid email format (e.g., student@example.com).");
        }

        if (s.getAddress() == null || s.getAddress().trim().isEmpty()) {
            throw new IllegalArgumentException("Address is required.");
        }
    }
}
