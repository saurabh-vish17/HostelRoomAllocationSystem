@echo off
title Hostel Room Allocation System
echo Starting Hostel Room Allocation System...
java -jar HostelManagementSystem.jar
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Application exited with code %ERRORLEVEL%.
    pause
)
