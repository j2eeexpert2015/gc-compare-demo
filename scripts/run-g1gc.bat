@echo off
REM ================================================
REM Run GC Demo with G1GC on port 8080
REM ================================================



set JAVA_OPTS=-XX:+UseG1GC ^
    -Xms512m ^
    -Xmx512m ^
    -XX:+PrintGCDetails ^
    -Xlog:gc*:file=logs/g1gc.log:time,uptime:filecount=5,filesize=10m ^
    -Dserver.port=8080 ^
    -Dspring.application.name=g1gc-demo

echo ================================================
echo Starting GC Demo with G1GC
echo Port: 8080
echo Heap: 512MB
echo ================================================
echo.
echo Prometheus endpoint: http://localhost:8080/actuator/prometheus
echo Info endpoint:       http://localhost:8080/api/memory/info
echo.

if not exist logs mkdir logs

"%JAVA_HOME%\bin\java" %JAVA_OPTS% -jar target\gc-compare-demo-1.0.0.jar

pause
