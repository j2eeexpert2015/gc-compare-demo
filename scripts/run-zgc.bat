@echo off
REM ================================================
REM Run GC Demo with ZGC (Generational) on port 8081
REM ================================================

set JAVA_OPTS=-XX:+UseZGC ^
    -XX:+ZGenerational ^
    -Xms512m ^
    -Xmx512m ^
    -Xlog:gc*:file=logs/zgc.log:time,uptime:filecount=5,filesize=10m ^
    -Dserver.port=8081 ^
    -Dspring.application.name=zgc-demo

echo ================================================
echo Starting GC Demo with ZGC (Generational)
echo Port: 8081
echo Heap: 512MB
echo ================================================
echo.
echo Prometheus endpoint: http://localhost:8081/actuator/prometheus
echo Info endpoint:       http://localhost:8081/api/memory/info
echo.

if not exist logs mkdir logs

"%JAVA_HOME%\bin\java" %JAVA_OPTS% -jar target\gc-compare-demo-1.0.0.jar

pause
