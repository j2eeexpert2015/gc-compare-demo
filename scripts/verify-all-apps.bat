@echo off
REM ========================================
REM Verify All GC Applications
REM ========================================
REM
REM This script checks if all 3 apps are running
REM and responsive
REM ========================================

echo.
echo ========================================
echo Verifying All GC Applications
echo ========================================
echo.

REM Check if curl is available
where curl >nul 2>&1
if %errorlevel% neq 0 (
    echo WARNING: curl not found. Using netstat instead.
    echo.
    goto CHECK_PORTS
)

REM Check each app with curl
echo Checking G1GC (port 8080)...
curl -s http://localhost:8080/actuator/health >nul 2>&1
if %errorlevel% equ 0 (
    echo   [OK] G1GC is running
) else (
    echo   [ERROR] G1GC is NOT running
)

echo Checking Generational ZGC (port 8081)...
curl -s http://localhost:8081/actuator/health >nul 2>&1
if %errorlevel% equ 0 (
    echo   [OK] Generational ZGC is running
) else (
    echo   [ERROR] Generational ZGC is NOT running
)

echo Checking ZGC (port 8082)...
curl -s http://localhost:8082/actuator/health >nul 2>&1
if %errorlevel% equ 0 (
    echo   [OK] ZGC is running
) else (
    echo   [ERROR] ZGC is NOT running
)

goto END

:CHECK_PORTS
echo Checking ports...
echo.

netstat -ano | findstr :8080 | findstr LISTENING >nul 2>&1
if %errorlevel% equ 0 (
    echo   [OK] Port 8080 is in use (G1GC)
) else (
    echo   [ERROR] Port 8080 is NOT in use
)

netstat -ano | findstr :8081 | findstr LISTENING >nul 2>&1
if %errorlevel% equ 0 (
    echo   [OK] Port 8081 is in use (Gen ZGC)
) else (
    echo   [ERROR] Port 8081 is NOT in use
)

netstat -ano | findstr :8082 | findstr LISTENING >nul 2>&1
if %errorlevel% equ 0 (
    echo   [OK] Port 8082 is in use (ZGC)
) else (
    echo   [ERROR] Port 8082 is NOT in use
)

:END
echo.
echo ========================================
echo Verification Complete
echo ========================================
echo.

pause
