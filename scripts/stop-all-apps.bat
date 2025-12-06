@echo off
REM ========================================
REM Stop All GC Applications
REM ========================================
REM
REM This script stops all running Java apps
REM on ports 8080, 8081, 8082
REM ========================================

echo.
echo ========================================
echo Stopping All GC Applications
echo ========================================
echo.

REM Find and kill Java processes on port 8080
echo Stopping G1GC (port 8080)...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :8080 ^| findstr LISTENING') do (
    echo   Killing process %%a
    taskkill /F /PID %%a >nul 2>&1
)

REM Find and kill Java processes on port 8081
echo Stopping Generational ZGC (port 8081)...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :8081 ^| findstr LISTENING') do (
    echo   Killing process %%a
    taskkill /F /PID %%a >nul 2>&1
)

REM Find and kill Java processes on port 8082
echo Stopping ZGC (port 8082)...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :8082 ^| findstr LISTENING') do (
    echo   Killing process %%a
    taskkill /F /PID %%a >nul 2>&1
)

echo.
echo ========================================
echo All applications stopped!
echo ========================================
echo.

pause
