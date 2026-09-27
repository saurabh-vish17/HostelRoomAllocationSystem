package com.hostel.test;

import com.hostel.config.DBConnection;
import com.hostel.model.Room;
import com.hostel.model.RoomDetail;
import com.hostel.model.Student;
import com.hostel.model.User;
import com.hostel.service.AllocationService;
import com.hostel.service.AuthService;
import com.hostel.service.RoomService;
import com.hostel.service.StudentService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * SystemTestRunner — Automated Test Suite & Validation Harness.
 * Tests Login, Student Management, Room Filtering & Maintenance,
 * Allocation Workflow, Vacating Workflow, DB Integrity, and Capacity Counts.
 */
public class SystemTestRunner {

    private final AuthService       authService;
    private final StudentService   studentService;
    private final RoomService      roomService;
    private final AllocationService allocationService;

    private int totalTests  = 0;
    private int passedTests = 0;
    private int failedTests = 0;

    public SystemTestRunner() {
        this.authService       = new AuthService();
        this.studentService   = new StudentService();
        this.roomService      = new RoomService();
        this.allocationService = new AllocationService();
    }

    public static void main(String[] args) {
        System.out.println("=========================================================");
        System.out.println("   HOSTEL ROOM ALLOCATION SYSTEM — COMPLETE TEST SUITE   ");
        System.out.println("=========================================================\n");

        SystemTestRunner runner = new SystemTestRunner();
        runner.runAllTests();
    }

    public void runAllTests() {
        testDatabaseIntegrityAndCapacity();
        testLoginSuite();
        testStudentSuite();
        testRoomSuite();
        testAllocationSuite();
        testVacatingSuite();
        testTransactionFailures();

        printSummaryReport();
    }

    // ── 1. DB Integrity & Capacity Checks ───────────────────────────────────

    private void testDatabaseIntegrityAndCapacity() {
        printHeader("1. DATABASE INTEGRITY & INFRASTRUCTURE CAPACITY TESTS");

        // Test 1.1 DB Connection
        try (Connection conn = DBConnection.getConnection()) {
            assertCondition(conn != null && !conn.isClosed(), "DB Connection: Successfully connected to MySQL");
        } catch (Exception e) {
            recordFail("DB Connection Failed: " + e.getMessage());
        }

        // Test 1.2 Room Counts per Block (D=60, G=212, H=180, Total=452)
        try (Connection conn = DBConnection.getConnection()) {
            checkBlockRoomCount(conn, "D", 60);
            checkBlockRoomCount(conn, "G", 212);
            checkBlockRoomCount(conn, "H", 180);

            String totalSql = "SELECT COUNT(*) FROM rooms";
            try (PreparedStatement ps = conn.prepareStatement(totalSql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int totalRooms = rs.getInt(1);
                    assertCondition(totalRooms == 452, "Total Room Count Verification: Expected 452, Found " + totalRooms);
                }
            }

            // Test 1.3 Total Bed Capacity
            String capSql = "SELECT SUM(capacity) FROM rooms";
            try (PreparedStatement ps = conn.prepareStatement(capSql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int totalCap = rs.getInt(1);
                    assertCondition(totalCap == 1071, "Total Bed Capacity Verification: Expected 1071 beds, Found " + totalCap);
                }
            }

            // Test 1.4 Floor Count Coverage across all Blocks
            String floorSql = "SELECT b.block_name, COUNT(DISTINCT f.floor_number) AS floors " +
                              "FROM floors f JOIN blocks b ON f.block_id = b.id GROUP BY b.block_name";
            try (PreparedStatement ps = conn.prepareStatement(floorSql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String block = rs.getString("block_name");
                    int floors  = rs.getInt("floors");
                    if ("D".equals(block)) assertCondition(floors == 5, "Block D Floors: Expected 5, Found " + floors);
                    if ("G".equals(block)) assertCondition(floors == 4, "Block G Floors: Expected 4, Found " + floors);
                    if ("H".equals(block)) assertCondition(floors == 6, "Block H Floors: Expected 6, Found " + floors);
                }
            }

        } catch (Exception e) {
            recordFail("Capacity verification failed: " + e.getMessage());
        }
    }

    private void checkBlockRoomCount(Connection conn, String blockName, int expectedCount) throws SQLException {
        String sql = "SELECT COUNT(*) FROM rooms r JOIN floors f ON r.floor_id = f.id JOIN blocks b ON f.block_id = b.id WHERE b.block_name = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, blockName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    assertCondition(count == expectedCount, "Block " + blockName + " Room Count: Expected " + expectedCount + ", Found " + count);
                }
            }
        }
    }

    // ── 2. Login Test Suite ────────────────────────────────────────────────

    private void testLoginSuite() {
        printHeader("2. AUTHENTICATION & LOGIN SUITE TESTS");

        // Test 2.1 Correct Credentials
        try {
            User user = authService.login("admin", "admin123");
            assertCondition(user != null && "admin".equalsIgnoreCase(user.getUsername()), "Login: Correct credentials (admin / admin123)");
        } catch (Exception e) {
            recordFail("Login failed with correct credentials: " + e.getMessage());
        }

        // Test 2.2 Wrong Credentials
        try {
            authService.login("admin", "wrongpassword999");
            recordFail("Login: Should have failed for wrong password");
        } catch (Exception e) {
            assertCondition(e.getMessage().contains("Invalid username or password"), "Login: Wrong credentials correctly rejected");
        }

        // Test 2.3 Non-existent user
        try {
            authService.login("non_existent_user_999", "admin123");
            recordFail("Login: Should have failed for non-existent username");
        } catch (Exception e) {
            assertCondition(e.getMessage().contains("Invalid username or password"), "Login: Non-existent user correctly rejected");
        }

        // Test 2.4 Empty Credentials
        try {
            authService.login("", "");
            recordFail("Login: Should have failed for empty credentials");
        } catch (Exception e) {
            assertCondition(e.getMessage().contains("Please enter your username"), "Login: Empty username correctly rejected");
        }

        // Test 2.5 Logout
        try {
            authService.logout();
            assertCondition(true, "Logout: Session cleared successfully");
        } catch (Exception e) {
            recordFail("Logout failed: " + e.getMessage());
        }
    }

    // ── 3. Student Management Test Suite ───────────────────────────────────

    private void testStudentSuite() {
        printHeader("3. STUDENT MANAGEMENT SUITE TESTS");

        String testId = "TEST_" + UUID.randomUUID().toString().substring(0, 6);
        Student testStudent = new Student(
            testId, "Test Student", "B.Tech CSE", 2, "Male", "9876543210", "teststudent@example.com", "123 Campus Lane"
        );

        // Test 3.1 Add Valid Student
        try {
            studentService.addStudent(testStudent);
            assertCondition(true, "Student Add: Valid student (" + testId + ") added successfully");
        } catch (Exception e) {
            recordFail("Student Add failed: " + e.getMessage());
        }

        // Fetch inserted student ID
        Student fetched = null;
        try {
            List<Student> searchResults = studentService.searchStudents(testId);
            if (!searchResults.isEmpty()) {
                fetched = searchResults.get(0);
                assertCondition(fetched != null && fetched.getStudentId().equals(testId), "Student Search: Found student by ID (" + testId + ")");
            } else {
                recordFail("Student Search: Student not found by ID");
            }
        } catch (Exception e) {
            recordFail("Student Search exception: " + e.getMessage());
        }

        // Test 3.2 Update Student
        if (fetched != null) {
            try {
                fetched.setName("Updated Test Student");
                fetched.setPhone("9988776655");
                studentService.updateStudent(fetched);

                Student reFetch = studentService.getStudentById(fetched.getId());
                assertCondition(reFetch != null && "Updated Test Student".equals(reFetch.getName()), "Student Update: Name updated successfully");
            } catch (Exception e) {
                recordFail("Student Update failed: " + e.getMessage());
            }
        }

        // Test 3.3 Prevent Duplicate Student ID
        Student dupStudent = new Student(
            testId, "Duplicate Guy", "B.Tech ECE", 1, "Male", "9123456789", "dup@example.com", "456 Hostel Lane"
        );
        try {
            studentService.addStudent(dupStudent);
            recordFail("Student Add: Should have rejected duplicate Student ID");
        } catch (IllegalArgumentException e) {
            assertCondition(e.getMessage().contains("already exists"), "Student Add: Duplicate Student ID correctly rejected");
        } catch (Exception e) {
            recordFail("Student Add: Unexpected exception on duplicate check: " + e.getMessage());
        }

        // Test 3.4 Invalid Email Validation
        Student badEmailStudent = new Student(
            "BAD_EMAIL_" + UUID.randomUUID().toString().substring(0, 4),
            "Bad Email", "B.Tech", 1, "Female", "9876543210", "invalid-email-format", "123 Lane"
        );
        try {
            studentService.addStudent(badEmailStudent);
            recordFail("Student Add: Should have rejected invalid email");
        } catch (IllegalArgumentException e) {
            assertCondition(e.getMessage().contains("Invalid email format"), "Student Add: Invalid email correctly rejected");
        } catch (Exception e) {
            recordFail("Student Add: Unexpected exception on email check: " + e.getMessage());
        }

        // Test 3.5 Invalid Phone Validation
        Student badPhoneStudent = new Student(
            "BAD_PHONE_" + UUID.randomUUID().toString().substring(0, 4),
            "Bad Phone", "B.Tech", 1, "Female", "123", "good@example.com", "123 Lane"
        );
        try {
            studentService.addStudent(badPhoneStudent);
            recordFail("Student Add: Should have rejected short phone number");
        } catch (IllegalArgumentException e) {
            assertCondition(e.getMessage().contains("Phone number must contain"), "Student Add: Invalid phone format correctly rejected");
        } catch (Exception e) {
            recordFail("Student Add: Unexpected exception on phone check: " + e.getMessage());
        }

        // Clean up test student at end of student tests
        if (fetched != null) {
            try {
                studentService.deleteStudent(fetched.getId());
                assertCondition(true, "Student Delete: Test student deleted cleanly");
            } catch (Exception e) {
                recordFail("Student Delete failed: " + e.getMessage());
            }
        }
    }

    // ── 4. Room Management & Filtering Test Suite ─────────────────────────

    private void testRoomSuite() {
        printHeader("4. ROOM MANAGEMENT & FILTERING SUITE TESTS");

        // Test 4.1 Filter by Block D, Floor 1
        try {
            List<RoomDetail> rooms = roomService.searchAndFilterRooms("D", 1, "ALL", "");
            assertCondition(rooms.size() == 12, "Room Filter (Block D, Floor 1): Expected 12 rooms, Found " + rooms.size());
        } catch (Exception e) {
            recordFail("Room Filter (D, 1) failed: " + e.getMessage());
        }

        // Test 4.2 Filter by Block G, Floor 2
        try {
            List<RoomDetail> rooms = roomService.searchAndFilterRooms("G", 2, "ALL", "");
            assertCondition(rooms.size() == 53, "Room Filter (Block G, Floor 2): Expected 53 rooms, Found " + rooms.size());
        } catch (Exception e) {
            recordFail("Room Filter (G, 2) failed: " + e.getMessage());
        }

        // Test 4.3 Filter by Block H, Floor 6
        try {
            List<RoomDetail> rooms = roomService.searchAndFilterRooms("H", 6, "ALL", "");
            assertCondition(rooms.size() == 30, "Room Filter (Block H, Floor 6): Expected 30 rooms, Found " + rooms.size());
        } catch (Exception e) {
            recordFail("Room Filter (H, 6) failed: " + e.getMessage());
        }

        // Test 4.4 Search Room by Number "D101"
        try {
            List<RoomDetail> rooms = roomService.searchAndFilterRooms("ALL", null, "ALL", "D101");
            assertCondition(rooms.size() >= 1 && "D101".equals(rooms.get(0).getRoomNumber()), "Room Search ('D101'): Found D101 successfully");
        } catch (Exception e) {
            recordFail("Room Search ('D101') failed: " + e.getMessage());
        }

        // Test 4.5 Maintenance Mode Toggle on empty room
        int testRoomId = -1;
        try {
            List<RoomDetail> d101List = roomService.searchAndFilterRooms("ALL", null, "ALL", "D101");
            if (!d101List.isEmpty()) {
                testRoomId = d101List.get(0).getId();
                // Enable maintenance
                boolean enabled = roomService.toggleMaintenanceStatus(testRoomId);
                Room r1 = roomService.getRoomById(testRoomId);
                assertCondition(enabled && Room.STATUS_MAINTENANCE.equals(r1.getStatus()), "Maintenance: Room D101 placed under MAINTENANCE");

                // Restore available
                boolean restored = roomService.toggleMaintenanceStatus(testRoomId);
                Room r2 = roomService.getRoomById(testRoomId);
                assertCondition(restored && Room.STATUS_AVAILABLE.equals(r2.getStatus()), "Maintenance: Room D101 restored to AVAILABLE");
            }
        } catch (Exception e) {
            recordFail("Maintenance toggle test failed: " + e.getMessage());
        }
    }

    // ── 5. Room Allocation Test Suite ──────────────────────────────────────

    private void testAllocationSuite() {
        printHeader("5. ROOM ALLOCATION WORKFLOW SUITE TESTS");

        // Create 2 test students for allocation tests
        Student s1 = new Student("ALLOC_S1_" + UUID.randomUUID().toString().substring(0, 4), "Alloc Student 1", "B.Tech", 1, "Male", "9876543210", "alloc1@example.com", "Hostel A");
        Student s2 = new Student("ALLOC_S2_" + UUID.randomUUID().toString().substring(0, 4), "Alloc Student 2", "B.Tech", 2, "Female", "9876543211", "alloc2@example.com", "Hostel B");
        Student s3 = new Student("ALLOC_S3_" + UUID.randomUUID().toString().substring(0, 4), "Alloc Student 3", "B.Tech", 3, "Male", "9876543212", "alloc3@example.com", "Hostel C");

        try {
            studentService.addStudent(s1);
            studentService.addStudent(s2);
            studentService.addStudent(s3);

            s1 = studentService.searchStudents(s1.getStudentId()).get(0);
            s2 = studentService.searchStudents(s2.getStudentId()).get(0);
            s3 = studentService.searchStudents(s3.getStudentId()).get(0);

            // Fetch a 2-seater room (e.g., D301)
            List<RoomDetail> d301List = roomService.searchAndFilterRooms("ALL", null, "ALL", "D301");
            assertCondition(!d301List.isEmpty(), "Allocation Setup: Found 2-seater room D301");

            int room2SeaterId = d301List.get(0).getId();

            // Test 5.1 Allocate Student 1 to 2-seater room D301 (Partial occupancy: 1/2)
            allocationService.allocateRoom(s1.getId(), room2SeaterId, "Test Allocation S1");
            Room rAfterS1 = roomService.getRoomById(room2SeaterId);
            assertCondition(rAfterS1.getOccupied() == 1 && Room.STATUS_PARTIALLY_OCCUPIED.equals(rAfterS1.getStatus()),
                    "Allocation: Student 1 allocated to 2-seater room D301 (Occupied: 1/2, Status: PARTIALLY_OCCUPIED)");

            // Test 5.2 Attempt Duplicate Active Allocation for Student 1
            try {
                allocationService.allocateRoom(s1.getId(), room2SeaterId, "Duplicate Attempt");
                recordFail("Allocation: Should have blocked duplicate active allocation for Student 1");
            } catch (IllegalArgumentException e) {
                assertCondition(e.getMessage().contains("already has an ACTIVE room allocation"), "Allocation: Duplicate active allocation correctly blocked");
            }

            // Test 5.3 Allocate Student 2 to same 2-seater room D301 (Full occupancy: 2/2)
            allocationService.allocateRoom(s2.getId(), room2SeaterId, "Test Allocation S2");
            Room rAfterS2 = roomService.getRoomById(room2SeaterId);
            assertCondition(rAfterS2.getOccupied() == 2 && Room.STATUS_FULL.equals(rAfterS2.getStatus()),
                    "Allocation: Student 2 allocated to D301 (Occupied: 2/2, Status: FULL)");

            // Test 5.4 Attempt Allocation to FULL room D301 for Student 3
            try {
                allocationService.allocateRoom(s3.getId(), room2SeaterId, "Full Room Attempt");
                recordFail("Allocation: Should have rejected allocation to FULL room");
            } catch (IllegalArgumentException e) {
                assertCondition(e.getMessage().contains("is FULL"), "Allocation: Allocation to FULL room correctly blocked");
            }

            // Test 5.5 Attempt Allocation to Room in MAINTENANCE
            List<RoomDetail> d102List = roomService.searchAndFilterRooms("ALL", null, "ALL", "D102");
            int roomMaintId = d102List.get(0).getId();
            roomService.toggleMaintenanceStatus(roomMaintId); // set MAINTENANCE

            try {
                allocationService.allocateRoom(s3.getId(), roomMaintId, "Maintenance Attempt");
                recordFail("Allocation: Should have rejected allocation to MAINTENANCE room");
            } catch (IllegalArgumentException e) {
                assertCondition(e.getMessage().contains("under MAINTENANCE"), "Allocation: Allocation to MAINTENANCE room correctly blocked");
            } finally {
                roomService.toggleMaintenanceStatus(roomMaintId); // restore AVAILABLE
            }

        } catch (Exception e) {
            recordFail("Allocation suite failure: " + e.getMessage());
        }
    }

    // ── 6. Room Vacating Test Suite ────────────────────────────────────────

    private void testVacatingSuite() {
        printHeader("6. ROOM VACATING WORKFLOW SUITE TESTS");

        try {
            // Find active allocation for D301
            List<com.hostel.model.AllocationDetail> activeAllocs = allocationService.searchActiveAllocations("D301");
            assertCondition(!activeAllocs.isEmpty(), "Vacating Setup: Active allocations found for room D301");

            int allocIdToVacate = activeAllocs.get(0).getAllocId();
            int roomId = activeAllocs.get(0).getRoomId();

            // Test 6.1 Vacate 1 student from FULL (2/2) room D301 -> becomes PARTIALLY_OCCUPIED (1/2)
            allocationService.vacateRoom(allocIdToVacate);
            Room rAfterVacate1 = roomService.getRoomById(roomId);
            assertCondition(rAfterVacate1.getOccupied() == 1 && Room.STATUS_PARTIALLY_OCCUPIED.equals(rAfterVacate1.getStatus()),
                    "Vacating: Vacated 1 student from FULL room D301 (Occupied: 1/2, Status: PARTIALLY_OCCUPIED)");

            // Test 6.2 Attempt to vacate already vacated allocation record
            try {
                allocationService.vacateRoom(allocIdToVacate);
                recordFail("Vacating: Should have rejected already vacated allocation ID");
            } catch (IllegalArgumentException e) {
                assertCondition(e.getMessage().contains("already VACATED"), "Vacating: Attempt to re-vacate already vacated allocation correctly blocked");
            }

            // Test 6.3 Vacate remaining student from D301 -> becomes AVAILABLE (0/2)
            List<com.hostel.model.AllocationDetail> remainingAllocs = allocationService.searchActiveAllocations("D301");
            if (!remainingAllocs.isEmpty()) {
                int allocId2 = remainingAllocs.get(0).getAllocId();
                allocationService.vacateRoom(allocId2);
                Room rAfterVacate2 = roomService.getRoomById(roomId);
                assertCondition(rAfterVacate2.getOccupied() == 0 && Room.STATUS_AVAILABLE.equals(rAfterVacate2.getStatus()),
                        "Vacating: Vacated last student from D301 (Occupied: 0/2, Status: AVAILABLE)");
            }

            // Test 6.4 Invalid Allocation ID check
            try {
                allocationService.vacateRoom(-999);
                recordFail("Vacating: Should have rejected invalid allocation ID -999");
            } catch (IllegalArgumentException e) {
                assertCondition(e.getMessage().contains("valid active allocation"), "Vacating: Invalid allocation ID correctly rejected");
            }

        } catch (Exception e) {
            recordFail("Vacating suite failure: " + e.getMessage());
        }
    }

    // ── 7. Transaction Rollback Tests ──────────────────────────────────────

    private void testTransactionFailures() {
        printHeader("7. DATABASE TRANSACTION ROLLBACK TESTS");

        try (Connection conn = DBConnection.getConnection()) {
            boolean origAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            try {
                // Execute part 1: Insert dummy test record
                String insertSql = "INSERT INTO users (username, password, full_name, role) VALUES ('tx_test_user', 'pass', 'Tx Test', 'ADMIN')";
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    ps.executeUpdate();
                }

                // Execute part 2: Deliberate failure (invalid SQL syntax or foreign key error)
                String failSql = "INSERT INTO non_existent_table_xyz VALUES (123)";
                try (PreparedStatement ps = conn.prepareStatement(failSql)) {
                    ps.executeUpdate();
                }

                conn.commit();
                recordFail("Transaction: Should have thrown exception on invalid table insert");
            } catch (SQLException e) {
                conn.rollback();
                assertCondition(true, "Transaction Rollback: Caught exception and rolled back transaction cleanly");
            } finally {
                conn.setAutoCommit(origAutoCommit);
            }

            // Verify user was NOT inserted due to rollback
            String checkSql = "SELECT COUNT(*) FROM users WHERE username = 'tx_test_user'";
            try (PreparedStatement ps = conn.prepareStatement(checkSql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    assertCondition(rs.getInt(1) == 0, "Transaction Rollback: Verified uncommitted row was successfully rolled back");
                }
            }

        } catch (Exception e) {
            recordFail("Transaction rollback test failed: " + e.getMessage());
        }
    }

    // ── Test Harness Helpers ───────────────────────────────────────────────

    private void assertCondition(boolean condition, String message) {
        totalTests++;
        if (condition) {
            passedTests++;
            System.out.println("  [PASS] ✔ " + message);
        } else {
            failedTests++;
            System.err.println("  [FAIL] ❌ " + message);
        }
    }

    private void recordFail(String message) {
        totalTests++;
        failedTests++;
        System.err.println("  [FAIL] ❌ " + message);
    }

    private void printHeader(String title) {
        System.out.println("\n---------------------------------------------------------");
        System.out.println(" " + title);
        System.out.println("---------------------------------------------------------");
    }

    private void printSummaryReport() {
        System.out.println("\n=========================================================");
        System.out.println("                SYSTEM TEST SUITE SUMMARY                ");
        System.out.println("=========================================================");
        System.out.println(" Total Executed Tests : " + totalTests);
        System.out.println(" Passed Tests         : " + passedTests + "  [ " + String.format("%.1f", (passedTests * 100.0 / totalTests)) + "% ]");
        System.out.println(" Failed Tests         : " + failedTests);
        System.out.println(" System Health Status : " + (failedTests == 0 ? "SUCCESS / ALL PASSED GREEN" : "ATTENTION REQUIRED"));
        System.out.println("=========================================================\n");
    }
}
