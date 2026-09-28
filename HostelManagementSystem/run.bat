@echo off
title Hostel Management System - Launcher
color 0A

echo.
echo  ==========================================
echo   Hostel Room Allocation Management System
echo  ==========================================
echo.

REM Check if Java is installed
java -version >nul 2>&1
IF %ERRORLEVEL% NEQ 0 (
    color 0C
    echo  [ERROR] Java is not installed or not found in PATH.
    echo.
    echo  Please install Java 17 or higher from:
    echo  https://adoptium.net/
    echo.
    pause
    exit /b 1
)

REM Check if the JAR file exists
IF NOT EXIST "%~dp0HostelManagementSystem.jar" (
    color 0C
    echo  [ERROR] HostelManagementSystem.jar not found!
    echo  Expected location: %~dp0HostelManagementSystem.jar
    echo.
    pause
    exit /b 1
)

echo  Starting application...
echo  (This window will close once the app is running)
echo.

REM Launch the JAR from its own directory so config/db.properties is found correctly
cd /d "%~dp0"
start "" javaw -jar HostelManagementSystem.jar

echo  Application launched successfully!
timeout /t 2 >nul
exit
