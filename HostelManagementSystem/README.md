# Hostel Room Allocation and Management System
> **Version:** 1.0.0  
> **Built With:** Java 17 (Swing & FlatLaf 3.4.1), JDBC, MySQL 8.x, BCrypt Security  

---

## 📋 Overview

The **Hostel Room Allocation and Management System** is a modern, enterprise-grade desktop application designed for managing college hostel operations. Featuring a FlatLaf **"Dark Slate"** visual interface, the system provides student record management, multi-block room capacity tracking, transaction-safe bed allocations, vacating workflows, and dynamic dashboard reporting.

---

## 🛠 Prerequisites & System Requirements

| Component | Minimum Specification | Recommended |
|-----------|------------------------|-------------|
| **Java Environment** | Java 17 (64-bit) JRE/JDK | OpenJDK 17 or Oracle JDK 17+ |
| **Database Server** | MySQL Server 8.0.x | MySQL Server 8.0 or 8.3 |
| **Operating System** | Windows 10 / 11 (64-bit) | Windows 11 |
| **Display Resolution**| 1280 × 720 | 1920 × 1080 |

---

## 📂 Package Distribution Structure

```
HostelManagementSystem/
│
├── HostelManagementSystem.jar   # Executable Fat JAR (all dependencies bundled)
├── run.bat                      # One-click Windows launcher script
├── database/
│   └── hostel_management.sql    # Database schema & initial 452-room seed data
├── config/
│   └── db.properties            # External MySQL database credentials
└── README.md                    # Installation & deployment guide
```

---

## 🗄️ Step 1: Database Setup

1. Open your MySQL client (e.g., **MySQL Workbench**, **DBeaver**, or **MySQL Command Line**).
2. Login to your MySQL server as `root` (or an administrator user).
3. Import and execute the SQL initialization script located at:
   ```path
   HostelManagementSystem/database/hostel_management.sql
   ```
   *In MySQL Workbench: Go to `File` → `Open SQL Script...` → Select `hostel_management.sql` → Click the ⚡ Execute button.*
4. Verify that the database `hostel_management` is created with tables `users`, `blocks`, `floors`, `rooms`, and `allocations`.
5. **Initial Login Credentials:**
   - **Username:** `admin`
   - **Password:** `admin123`  *(On first login, password auto-migrates to an encrypted BCrypt hash)*

---

## ⚙️ Step 2: Database Configuration

Before starting the application, configure your MySQL database connection credentials:

1. Open the file `config/db.properties` in any text editor (Notepad, VS Code, etc.).
2. Update the credentials to match your local MySQL configuration:

```properties
# JDBC Connection URL
db.url=jdbc:mysql://localhost:3306/hostel_management?useSSL=false&serverTimezone=Asia/Kolkata&allowPublicKeyRetrieval=true

# MySQL Database Credentials
db.username=root
db.password=YOUR_MYSQL_PASSWORD_HERE

# JDBC Driver Class
db.driver=com.mysql.cj.jdbc.Driver
```

---

## 🚀 Step 3: Running the Application

### Option A: One-Click Windows Launcher (Recommended)
Double-click `run.bat` located inside the `HostelManagementSystem/` folder.

### Option B: Executable JAR Direct Double-Click
Double-click `HostelManagementSystem.jar` directly.

### Option C: Command Line Execution
Open Command Prompt or PowerShell in the `HostelManagementSystem/` folder and run:
```cmd
java -jar HostelManagementSystem.jar
```

---

## 📦 Step 4: Packaging for Another Windows Computer

To deploy the application to another Windows machine, choose one of the following methods:

### Method 1: Portable ZIP Package (Simplest)
1. Compress the entire `HostelManagementSystem/` directory into a `.zip` archive.
2. Transfer `HostelManagementSystem.zip` to the target computer.
3. On the target computer:
   - Ensure Java 17+ and MySQL 8.x are installed.
   - Run `database/hostel_management.sql` on the target MySQL server.
   - Edit `config/db.properties` to set the target database password.
   - Double-click `run.bat` to launch.

---

### Method 2: Native Windows Installer (`.exe` / `.msi`) using `jpackage`

You can package the application into a standalone Windows installer containing a bundled Java Runtime Environment (JRE). The user won't need Java pre-installed.

#### Prerequisites for `jpackage`:
- JDK 17 or higher installed (with `jpackage.exe` in JDK `bin/`).
- **WiX Toolset v3.x** installed (required only if generating `.msi` installers).

#### Step-by-Step `jpackage` Command:

Open Command Prompt / PowerShell as Administrator in the `HostelManagementSystem/` directory and execute:

```cmd
jpackage ^
  --type exe ^
  --name "Hostel Management System" ^
  --app-version "1.0.0" ^
  --description "Hostel Room Allocation and Management Desktop System" ^
  --vendor "Hostel Management Inc." ^
  --input . ^
  --main-jar HostelManagementSystem.jar ^
  --main-class com.hostel.Main ^
  --dest installer ^
  --win-shortcut ^
  --win-menu
```

This will generate `installer/Hostel Management System-1.0.0.exe`, a standalone native Windows installer!

---

## 🏢 Hostel Infrastructure & Capacity Reference

| Block | Total Floors | Total Rooms | Room Capacity Specs | Total Block Beds |
|-------|--------------|-------------|----------------------|------------------|
| **Block D** | 5 Floors | 60 Rooms | Floors 1-2: 3 beds<br>Floors 3-5: 2 beds | **144 Beds** |
| **Block G** | 4 Floors | 212 Rooms | Floor 2: 3 beds<br>Floors 1, 3, 4: 2 beds | **477 Beds** |
| **Block H** | 6 Floors | 180 Rooms | Floors 2-4: 3 beds<br>Floors 1, 5, 6: 2 beds | **450 Beds** |
| **TOTAL** | **15 Floors** | **452 Rooms** | — | **1,071 Beds** |

---

## 🛡️ License & Technical Support
This software is provided ready for production deployment. All business rules, allocation concurrency locks, input validations, and transaction rollbacks are enforced at the service and database layers.
