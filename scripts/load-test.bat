@echo off
REM ================================================
REM Load Test Script - Hits both G1GC and ZGC instances
REM Similar to Vishalendu's testme script
REM ================================================

setlocal enabledelayedexpansion

set ITERATIONS=30
set SLEEP_SECONDS=10

echo ================================================
echo GC Comparison Load Test
echo Iterations: %ITERATIONS%
echo Sleep between calls: %SLEEP_SECONDS%s
echo ================================================
echo.
echo Make sure both apps are running:
echo   - G1GC on http://localhost:8080
echo   - ZGC  on http://localhost:8081
echo.
echo Press any key to start the test...
pause > nul

for /L %%i in (1,1,%ITERATIONS%) do (
    echo.
    echo === Iteration %%i of %ITERATIONS% ===
    
    REM Light load - 500 (50 * 10MB = 500MB)
    echo [%%i] Sending light load (500MB) to both instances...
    
    curl -s -X POST http://localhost:8080/api/memory/load/50 > nul
    curl -s -X POST http://localhost:8081/api/memory/load/50 > nul
    
    echo [%%i] Sleeping %SLEEP_SECONDS%s...
    timeout /t %SLEEP_SECONDS% /nobreak > nul
    
    REM Heavy load - 800 (80 * 10MB = 800MB)
    echo [%%i] Sending heavy load (800MB) to both instances...
    
    curl -s -X POST http://localhost:8080/api/memory/load/80 > nul
    curl -s -X POST http://localhost:8081/api/memory/load/80 > nul
    
    echo [%%i] Sleeping %SLEEP_SECONDS%s...
    timeout /t %SLEEP_SECONDS% /nobreak > nul
)

echo.
echo ================================================
echo Load test completed!
echo Check Grafana at http://localhost:3000 for results
echo ================================================

pause
