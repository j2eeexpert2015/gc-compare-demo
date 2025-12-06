#!/bin/bash

########################################
# Start All 3 GC Applications Together
########################################
#
# This script starts:
#   1. G1GC app on port 8080
#   2. Generational ZGC app on port 8081
#   3. ZGC app on port 8082
#
# Each app runs in a separate terminal window
########################################

echo ""
echo "========================================"
echo "Starting All GC Applications"
echo "========================================"
echo ""

# Check if JAR file exists
if [ ! -f "target/gc-comparison-demo-0.0.1-SNAPSHOT.jar" ]; then
    echo "ERROR: JAR file not found!"
    echo "Please run: mvn clean package"
    echo ""
    exit 1
fi

# Detect OS and terminal
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    TERMINAL_CMD="osascript -e 'tell app \"Terminal\" to do script"
elif command -v gnome-terminal &> /dev/null; then
    # Linux with GNOME Terminal
    TERMINAL_CMD="gnome-terminal"
elif command -v konsole &> /dev/null; then
    # Linux with KDE Konsole
    TERMINAL_CMD="konsole"
elif command -v xterm &> /dev/null; then
    # Linux with xterm
    TERMINAL_CMD="xterm"
else
    echo "WARNING: No suitable terminal found. Using background mode."
    TERMINAL_CMD="background"
fi

echo "Starting applications in separate terminals..."
echo ""

# Start G1GC on port 8080
echo "[1/3] Starting G1GC on port 8080..."
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    osascript -e 'tell app "Terminal" to do script "cd \"'$(pwd)'\" && java -Xmx4g -Xms4g -XX:+UseG1GC -jar target/gc-comparison-demo-0.0.1-SNAPSHOT.jar --server.port=8080"'
elif [ "$TERMINAL_CMD" = "gnome-terminal" ]; then
    # GNOME Terminal
    gnome-terminal --title="G1GC (Port 8080)" -- bash -c "java -Xmx4g -Xms4g -XX:+UseG1GC -jar target/gc-comparison-demo-0.0.1-SNAPSHOT.jar --server.port=8080; exec bash"
elif [ "$TERMINAL_CMD" = "konsole" ]; then
    # KDE Konsole
    konsole --title "G1GC (Port 8080)" -e bash -c "java -Xmx4g -Xms4g -XX:+UseG1GC -jar target/gc-comparison-demo-0.0.1-SNAPSHOT.jar --server.port=8080; exec bash" &
elif [ "$TERMINAL_CMD" = "xterm" ]; then
    # xterm
    xterm -title "G1GC (Port 8080)" -e "java -Xmx4g -Xms4g -XX:+UseG1GC -jar target/gc-comparison-demo-0.0.1-SNAPSHOT.jar --server.port=8080" &
else
    # Background mode
    nohup java -Xmx4g -Xms4g -XX:+UseG1GC -jar target/gc-comparison-demo-0.0.1-SNAPSHOT.jar --server.port=8080 > logs/g1gc.log 2>&1 &
fi

# Wait 5 seconds
sleep 5

# Start Generational ZGC on port 8081
echo "[2/3] Starting Generational ZGC on port 8081..."
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    osascript -e 'tell app "Terminal" to do script "cd \"'$(pwd)'\" && java -Xmx4g -Xms4g -XX:+UseZGC -XX:+ZGenerational -jar target/gc-comparison-demo-0.0.1-SNAPSHOT.jar --server.port=8081"'
elif [ "$TERMINAL_CMD" = "gnome-terminal" ]; then
    # GNOME Terminal
    gnome-terminal --title="Generational ZGC (Port 8081)" -- bash -c "java -Xmx4g -Xms4g -XX:+UseZGC -XX:+ZGenerational -jar target/gc-comparison-demo-0.0.1-SNAPSHOT.jar --server.port=8081; exec bash"
elif [ "$TERMINAL_CMD" = "konsole" ]; then
    # KDE Konsole
    konsole --title "Generational ZGC (Port 8081)" -e bash -c "java -Xmx4g -Xms4g -XX:+UseZGC -XX:+ZGenerational -jar target/gc-comparison-demo-0.0.1-SNAPSHOT.jar --server.port=8081; exec bash" &
elif [ "$TERMINAL_CMD" = "xterm" ]; then
    # xterm
    xterm -title "Generational ZGC (Port 8081)" -e "java -Xmx4g -Xms4g -XX:+UseZGC -XX:+ZGenerational -jar target/gc-comparison-demo-0.0.1-SNAPSHOT.jar --server.port=8081" &
else
    # Background mode
    nohup java -Xmx4g -Xms4g -XX:+UseZGC -XX:+ZGenerational -jar target/gc-comparison-demo-0.0.1-SNAPSHOT.jar --server.port=8081 > logs/gen-zgc.log 2>&1 &
fi

# Wait 5 seconds
sleep 5

# Start ZGC on port 8082
echo "[3/3] Starting ZGC on port 8082..."
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    osascript -e 'tell app "Terminal" to do script "cd \"'$(pwd)'\" && java -Xmx4g -Xms4g -XX:+UseZGC -XX:-ZGenerational -jar target/gc-comparison-demo-0.0.1-SNAPSHOT.jar --server.port=8082"'
elif [ "$TERMINAL_CMD" = "gnome-terminal" ]; then
    # GNOME Terminal
    gnome-terminal --title="ZGC (Port 8082)" -- bash -c "java -Xmx4g -Xms4g -XX:+UseZGC -XX:-ZGenerational -jar target/gc-comparison-demo-0.0.1-SNAPSHOT.jar --server.port=8082; exec bash"
elif [ "$TERMINAL_CMD" = "konsole" ]; then
    # KDE Konsole
    konsole --title "ZGC (Port 8082)" -e bash -c "java -Xmx4g -Xms4g -XX:+UseZGC -XX:-ZGenerational -jar target/gc-comparison-demo-0.0.1-SNAPSHOT.jar --server.port=8082; exec bash" &
elif [ "$TERMINAL_CMD" = "xterm" ]; then
    # xterm
    xterm -title "ZGC (Port 8082)" -e "java -Xmx4g -Xms4g -XX:+UseZGC -XX:-ZGenerational -jar target/gc-comparison-demo-0.0.1-SNAPSHOT.jar --server.port=8082" &
else
    # Background mode
    nohup java -Xmx4g -Xms4g -XX:+UseZGC -XX:-ZGenerational -jar target/gc-comparison-demo-0.0.1-SNAPSHOT.jar --server.port=8082 > logs/zgc.log 2>&1 &
fi

echo ""
echo "========================================"
echo "All applications started successfully!"
echo "========================================"
echo ""

if [[ "$OSTYPE" == "darwin"* ]]; then
    echo "You should see 3 new Terminal windows:"
elif [ "$TERMINAL_CMD" = "background" ]; then
    echo "Applications running in background"
    echo "Log files:"
    echo "  logs/g1gc.log"
    echo "  logs/gen-zgc.log"
    echo "  logs/zgc.log"
else
    echo "You should see 3 new terminal windows:"
fi

echo "  - G1GC (Port 8080)"
echo "  - Generational ZGC (Port 8081)"
echo "  - ZGC (Port 8082)"
echo ""
echo "Wait ~30 seconds for all apps to fully start"
echo ""
echo "To verify apps are running:"
echo "  curl http://localhost:8080/actuator/health"
echo "  curl http://localhost:8081/actuator/health"
echo "  curl http://localhost:8082/actuator/health"
echo ""
echo "To stop apps: Close each window or run stop-all-apps.sh"
echo ""
echo "========================================"
