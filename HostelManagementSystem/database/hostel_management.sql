-- ================================================================
-- Hostel Room Allocation and Management System
-- Phase 2: Complete Database Schema + Seed Data
-- ================================================================
--
-- Database  : hostel_management
-- Total rooms: 452  (D:60  G:212  H:180)
-- Total beds : computed from room capacity (see verification)
--
-- HOW TO RUN:
--   MySQL Workbench → File → Open SQL Script → select this file
--   Click the lightning bolt (⚡) to run the entire script.
--
-- DEVELOPMENT LOGIN:
--   Username : admin
--   Password : admin123  (plain-text for Phase 2 only)
--   NOTE: Phase 4 (AuthService) will replace this with BCrypt hash.
-- ================================================================


-- ================================================================
-- STEP 1: Create / recreate the database
-- ================================================================
DROP DATABASE IF EXISTS hostel_management;

CREATE DATABASE hostel_management
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE hostel_management;


-- ================================================================
-- STEP 2: TABLE — users  (admin accounts)
-- ================================================================
CREATE TABLE users (
    id          INT             NOT NULL AUTO_INCREMENT,
    username    VARCHAR(50)     NOT NULL,
    password    VARCHAR(255)    NOT NULL,   -- BCrypt hash (Phase 4)
    full_name   VARCHAR(100)    NOT NULL,
    role        VARCHAR(20)     NOT NULL DEFAULT 'ADMIN',
    is_active   TINYINT(1)      NOT NULL DEFAULT 1,
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
                                         ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE  KEY uq_username (username)
);


-- ================================================================
-- STEP 3: TABLE — blocks  (D, G, H)
-- ================================================================
CREATE TABLE blocks (
    id              INT         NOT NULL AUTO_INCREMENT,
    block_name      CHAR(1)     NOT NULL,
    total_floors    INT         NOT NULL,
    description     VARCHAR(255),
    created_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE  KEY uq_block_name (block_name),
    INDEX   idx_block_name   (block_name)
);


-- ================================================================
-- STEP 4: TABLE — floors
--
-- rooms_count   : number of rooms on this floor (used for generation + stats)
-- room_capacity : seating capacity per room on this floor
-- ================================================================
CREATE TABLE floors (
    id              INT         NOT NULL AUTO_INCREMENT,
    block_id        INT         NOT NULL,
    floor_number    INT         NOT NULL,
    rooms_count     INT         NOT NULL,
    room_capacity   INT         NOT NULL,
    created_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE  KEY uq_block_floor  (block_id, floor_number),
    INDEX   idx_floor_block     (block_id),

    CONSTRAINT fk_floor_block
        FOREIGN KEY (block_id) REFERENCES blocks(id)
        ON DELETE CASCADE ON UPDATE CASCADE
);


-- ================================================================
-- STEP 5: TABLE — rooms
--
-- room_number : e.g. D101, G253, H430
-- capacity    : max students in this room (2 or 3)
-- occupied    : current count — updated on every allocation/vacate
-- status      : AVAILABLE | PARTIALLY_OCCUPIED | FULL | MAINTENANCE
-- ================================================================
CREATE TABLE rooms (
    id              INT         NOT NULL AUTO_INCREMENT,
    floor_id        INT         NOT NULL,
    room_number     VARCHAR(10) NOT NULL,
    capacity        INT         NOT NULL,
    occupied        INT         NOT NULL DEFAULT 0,
    status          ENUM('AVAILABLE','PARTIALLY_OCCUPIED','FULL','MAINTENANCE')
                                NOT NULL DEFAULT 'AVAILABLE',
    created_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
                                         ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE  KEY uq_room_number  (room_number),
    INDEX   idx_room_floor      (floor_id),
    INDEX   idx_room_status     (status),
    INDEX   idx_room_number     (room_number),

    -- Prevent occupied count from going negative or exceeding capacity
    CONSTRAINT chk_occupied_range
        CHECK (occupied >= 0 AND occupied <= capacity),

    CONSTRAINT fk_room_floor
        FOREIGN KEY (floor_id) REFERENCES floors(id)
        ON DELETE CASCADE ON UPDATE CASCADE
);


-- ================================================================
-- STEP 6: TABLE — students
-- ================================================================
CREATE TABLE students (
    id          INT             NOT NULL AUTO_INCREMENT,
    student_id  VARCHAR(20)     NOT NULL,   -- college roll number
    name        VARCHAR(100)    NOT NULL,
    course      VARCHAR(100),
    year        INT,
    gender      ENUM('Male','Female','Other'),
    phone       VARCHAR(15),
    email       VARCHAR(100),
    address     TEXT,
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
                                         ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE  KEY uq_student_id   (student_id),
    INDEX   idx_student_name    (name),
    INDEX   idx_student_course  (course)
);


-- ================================================================
-- STEP 7: TABLE — allocations
--
-- Historical records are NEVER deleted.
-- status = ACTIVE   → student is currently living in the room
-- status = VACATED  → student has left (vacate_date is set)
-- ================================================================
CREATE TABLE allocations (
    id              INT         NOT NULL AUTO_INCREMENT,
    student_id      INT         NOT NULL,
    room_id         INT         NOT NULL,
    alloc_date      DATE        NOT NULL,
    vacate_date     DATE,
    status          ENUM('ACTIVE','VACATED') NOT NULL DEFAULT 'ACTIVE',
    remarks         TEXT,
    created_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
                                         ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX   idx_alloc_student   (student_id),
    INDEX   idx_alloc_room      (room_id),
    INDEX   idx_alloc_status    (status),

    CONSTRAINT fk_alloc_student
        FOREIGN KEY (student_id) REFERENCES students(id)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT fk_alloc_room
        FOREIGN KEY (room_id) REFERENCES rooms(id)
        ON DELETE RESTRICT ON UPDATE CASCADE
);


-- ================================================================
-- STEP 8: SEED — Blocks
-- ================================================================
INSERT INTO blocks (block_name, total_floors, description) VALUES
('D', 5, 'D Block — 5 floors, 12 rooms per floor, 60 rooms, mixed 2/3-seater'),
('G', 4, 'G Block — 4 floors, 53 rooms per floor, 212 rooms, mixed 2/3-seater'),
('H', 6, 'H Block — 6 floors, 30 rooms per floor, 180 rooms, mixed 2/3-seater');


-- ================================================================
-- STEP 9: SEED — Floors
-- (rooms_count × room_capacity defines the type for each floor)
-- ================================================================

-- ── D Block ──────────────────────────────────────────────────
INSERT INTO floors (block_id, floor_number, rooms_count, room_capacity)
SELECT id, 1, 12, 3 FROM blocks WHERE block_name = 'D';   -- 3-seater

INSERT INTO floors (block_id, floor_number, rooms_count, room_capacity)
SELECT id, 2, 12, 3 FROM blocks WHERE block_name = 'D';   -- 3-seater

INSERT INTO floors (block_id, floor_number, rooms_count, room_capacity)
SELECT id, 3, 12, 2 FROM blocks WHERE block_name = 'D';   -- 2-seater

INSERT INTO floors (block_id, floor_number, rooms_count, room_capacity)
SELECT id, 4, 12, 2 FROM blocks WHERE block_name = 'D';   -- 2-seater

INSERT INTO floors (block_id, floor_number, rooms_count, room_capacity)
SELECT id, 5, 12, 2 FROM blocks WHERE block_name = 'D';   -- 2-seater

-- ── G Block ──────────────────────────────────────────────────
INSERT INTO floors (block_id, floor_number, rooms_count, room_capacity)
SELECT id, 1, 53, 2 FROM blocks WHERE block_name = 'G';   -- 2-seater

INSERT INTO floors (block_id, floor_number, rooms_count, room_capacity)
SELECT id, 2, 53, 3 FROM blocks WHERE block_name = 'G';   -- 3-seater

INSERT INTO floors (block_id, floor_number, rooms_count, room_capacity)
SELECT id, 3, 53, 2 FROM blocks WHERE block_name = 'G';   -- 2-seater

INSERT INTO floors (block_id, floor_number, rooms_count, room_capacity)
SELECT id, 4, 53, 2 FROM blocks WHERE block_name = 'G';   -- 2-seater

-- ── H Block ──────────────────────────────────────────────────
INSERT INTO floors (block_id, floor_number, rooms_count, room_capacity)
SELECT id, 1, 30, 2 FROM blocks WHERE block_name = 'H';   -- 2-seater

INSERT INTO floors (block_id, floor_number, rooms_count, room_capacity)
SELECT id, 2, 30, 3 FROM blocks WHERE block_name = 'H';   -- 3-seater

INSERT INTO floors (block_id, floor_number, rooms_count, room_capacity)
SELECT id, 3, 30, 3 FROM blocks WHERE block_name = 'H';   -- 3-seater

INSERT INTO floors (block_id, floor_number, rooms_count, room_capacity)
SELECT id, 4, 30, 3 FROM blocks WHERE block_name = 'H';   -- 3-seater

INSERT INTO floors (block_id, floor_number, rooms_count, room_capacity)
SELECT id, 5, 30, 2 FROM blocks WHERE block_name = 'H';   -- 2-seater

INSERT INTO floors (block_id, floor_number, rooms_count, room_capacity)
SELECT id, 6, 30, 2 FROM blocks WHERE block_name = 'H';   -- 2-seater


-- ================================================================
-- STEP 10: STORED PROCEDURE — generate_rooms_for_floor
--
-- Generates room rows for one floor programmatically.
-- Room number format:  BLOCK + FLOOR + zero-padded room index
-- Examples: D101 (block=D, floor=1, room=01)
--           G253 (block=G, floor=2, room=53)
--           H630 (block=H, floor=6, room=30)
-- ================================================================
DELIMITER //

CREATE PROCEDURE generate_rooms_for_floor(
    IN p_block_name   CHAR(1),
    IN p_floor_number INT,
    IN p_rooms_count  INT,
    IN p_capacity     INT
)
BEGIN
    DECLARE i           INT     DEFAULT 1;
    DECLARE v_floor_id  INT;
    DECLARE v_room_num  VARCHAR(10);

    -- Resolve the floor's primary key
    SELECT f.id
    INTO   v_floor_id
    FROM   floors f
    JOIN   blocks b ON f.block_id = b.id
    WHERE  b.block_name   = p_block_name
      AND  f.floor_number = p_floor_number
    LIMIT  1;

    -- Loop: insert one row per room
    room_loop: WHILE i <= p_rooms_count DO

        -- e.g. CONCAT('D', 1, LPAD(1,2,'0')) = 'D101'
        --      CONCAT('G', 2, LPAD(53,2,'0')) = 'G253'
        SET v_room_num = CONCAT(p_block_name, p_floor_number, LPAD(i, 2, '0'));

        INSERT INTO rooms (floor_id, room_number, capacity, occupied, status)
        VALUES (v_floor_id, v_room_num, p_capacity, 0, 'AVAILABLE');

        SET i = i + 1;
    END WHILE room_loop;

END //

DELIMITER ;


-- ================================================================
-- STEP 11: GENERATE all 452 rooms
-- ================================================================

-- ── D Block: 5 floors × 12 rooms = 60 rooms ──────────────────
CALL generate_rooms_for_floor('D', 1, 12, 3);  -- D101–D112 (3-seater)
CALL generate_rooms_for_floor('D', 2, 12, 3);  -- D201–D212 (3-seater)
CALL generate_rooms_for_floor('D', 3, 12, 2);  -- D301–D312 (2-seater)
CALL generate_rooms_for_floor('D', 4, 12, 2);  -- D401–D412 (2-seater)
CALL generate_rooms_for_floor('D', 5, 12, 2);  -- D501–D512 (2-seater)

-- ── G Block: 4 floors × 53 rooms = 212 rooms ─────────────────
CALL generate_rooms_for_floor('G', 1, 53, 2);  -- G101–G153 (2-seater)
CALL generate_rooms_for_floor('G', 2, 53, 3);  -- G201–G253 (3-seater)
CALL generate_rooms_for_floor('G', 3, 53, 2);  -- G301–G353 (2-seater)
CALL generate_rooms_for_floor('G', 4, 53, 2);  -- G401–G453 (2-seater)

-- ── H Block: 6 floors × 30 rooms = 180 rooms ─────────────────
CALL generate_rooms_for_floor('H', 1, 30, 2);  -- H101–H130 (2-seater)
CALL generate_rooms_for_floor('H', 2, 30, 3);  -- H201–H230 (3-seater)
CALL generate_rooms_for_floor('H', 3, 30, 3);  -- H301–H330 (3-seater)
CALL generate_rooms_for_floor('H', 4, 30, 3);  -- H401–H430 (3-seater)
CALL generate_rooms_for_floor('H', 5, 30, 2);  -- H501–H530 (2-seater)
CALL generate_rooms_for_floor('H', 6, 30, 2);  -- H601–H630 (2-seater)

-- Clean up — procedure is no longer needed at runtime
DROP PROCEDURE IF EXISTS generate_rooms_for_floor;


-- ================================================================
-- STEP 12: SEED — Default admin user
--
-- Development credentials:
--   Username : admin
--   Password : admin123
--
-- SECURITY NOTE:
--   The password is stored as plain text here ONLY for Phase 2
--   (no Java code runs this script — Workbench runs it manually).
--   In Phase 4, AuthService will BCrypt-hash all passwords.
--   After Phase 4, run:
--     UPDATE users
--     SET password = '<bcrypt-hash-of-admin123>'
--     WHERE username = 'admin';
-- ================================================================
INSERT INTO users (username, password, full_name, role, is_active)
VALUES ('admin', 'admin123', 'System Administrator', 'ADMIN', 1);


-- ================================================================
-- VERIFICATION QUERIES
-- Run these after importing to confirm everything is correct.
-- ================================================================

-- ── V1: Room count per block ──────────────────────────────────
SELECT
    b.block_name                        AS `Block`,
    COUNT(r.id)                         AS `Total Rooms`,
    CASE b.block_name
        WHEN 'D' THEN 60
        WHEN 'G' THEN 212
        WHEN 'H' THEN 180
    END                                 AS `Expected Rooms`,
    IF(COUNT(r.id) = CASE b.block_name
        WHEN 'D' THEN 60
        WHEN 'G' THEN 212
        WHEN 'H' THEN 180
        END, '✓ PASS', '✗ FAIL')       AS `Status`
FROM blocks b
JOIN floors f ON f.block_id = b.id
JOIN rooms  r ON r.floor_id = f.id
GROUP BY b.block_name
ORDER BY b.block_name;

-- ── V2: Total room count ─────────────────────────────────────
SELECT
    COUNT(*)                                    AS `Total Rooms`,
    IF(COUNT(*) = 452, '✓ PASS 452', '✗ FAIL') AS `Status`
FROM rooms;

-- ── V3: Total bed capacity per block ─────────────────────────
SELECT
    b.block_name    AS `Block`,
    SUM(r.capacity) AS `Total Capacity`
FROM blocks b
JOIN floors f ON f.block_id = b.id
JOIN rooms  r ON r.floor_id = f.id
GROUP BY b.block_name
ORDER BY b.block_name;

-- ── V4: Grand total capacity ─────────────────────────────────
SELECT SUM(capacity) AS `Grand Total Capacity` FROM rooms;

-- ── V5: Floor-by-floor breakdown ─────────────────────────────
SELECT
    b.block_name            AS `Block`,
    f.floor_number          AS `Floor`,
    f.room_capacity         AS `Seats/Room`,
    COUNT(r.id)             AS `Rooms Generated`,
    SUM(r.capacity)         AS `Floor Capacity`
FROM blocks b
JOIN floors f ON f.block_id = b.id
JOIN rooms  r ON r.floor_id = f.id
GROUP BY b.block_name, f.floor_number, f.room_capacity
ORDER BY b.block_name, f.floor_number;

-- ── V6: Sample room numbers — spot check ─────────────────────
-- D Block floor 1 (expected D101–D112)
SELECT room_number FROM rooms
WHERE room_number LIKE 'D1%' ORDER BY room_number;

-- G Block floor 2 (expected G201–G253)
SELECT room_number FROM rooms
WHERE room_number LIKE 'G2%' ORDER BY room_number;

-- H Block floor 6 (expected H601–H630)
SELECT room_number FROM rooms
WHERE room_number LIKE 'H6%' ORDER BY room_number;

-- ── V7: First and last room of each block ────────────────────
SELECT
    b.block_name,
    MIN(r.room_number) AS first_room,
    MAX(r.room_number) AS last_room
FROM blocks b
JOIN floors f ON f.block_id = b.id
JOIN rooms  r ON r.floor_id = f.id
GROUP BY b.block_name
ORDER BY b.block_name;

-- ── V8: Confirm all rooms are AVAILABLE and unoccupied ───────
SELECT
    status,
    COUNT(*) AS room_count
FROM rooms
GROUP BY status;
-- Expected: AVAILABLE = 452, no other status

-- ── V9: Admin user created ───────────────────────────────────
SELECT id, username, full_name, role, is_active FROM users;
-- Expected: 1 row — admin / System Administrator / ADMIN

-- ================================================================
-- END OF SCRIPT
-- ================================================================
