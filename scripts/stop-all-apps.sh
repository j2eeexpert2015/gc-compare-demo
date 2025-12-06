#!/bin/bash

########################################
# Stop All GC Applications
########################################
#
# This script stops all running Java apps
# on ports 8080, 8081, 8082
########################################

echo ""
echo "========================================"
echo "Stopping All GC Applications"
echo "========================================"
echo ""

# Function to kill process on port
kill_port() {
    local port=$1
    local name=$2
    
    echo "Stopping $name (port $port)..."
    
    # Try lsof first (macOS and some Linux)
    if command -v lsof &> /dev/null; then
        local pids=$(lsof -ti:$port)
        if [ ! -z "$pids" ]; then
            for pid in $pids; do
                echo "  Killing process $pid"
                kill -9 $pid 2>/dev/null
            done
            return 0
        fi
    fi
    
    # Try fuser (Linux)
    if command -v fuser &> /dev/null; then
        local pids=$(fuser $port/tcp 2>/dev/null)
        if [ ! -z "$pids" ]; then
            for pid in $pids; do
                echo "  Killing process $pid"
                kill -9 $pid 2>/dev/null
            done
            return 0
        fi
    fi
    
    # Try netstat + ps (fallback)
    if command -v netstat &> /dev/null; then
        local pids=$(netstat -nlp 2>/dev/null | grep ":$port " | awk '{print $7}' | cut -d'/' -f1)
        if [ ! -z "$pids" ]; then
            for pid in $pids; do
                if [ ! -z "$pid" ] && [ "$pid" != "-" ]; then
                    echo "  Killing process $pid"
                    kill -9 $pid 2>/dev/null
                fi
            done
            return 0
        fi
    fi
    
    echo "  No process found on port $port"
    return 1
}

# Kill processes on each port
kill_port 8080 "G1GC"
echo ""

kill_port 8081 "Generational ZGC"
echo ""

kill_port 8082 "ZGC"
echo ""

# Also try to kill any remaining java processes running the JAR
echo "Cleaning up any remaining processes..."
pkill -f "gc-comparison-demo-0.0.1-SNAPSHOT.jar" 2>/dev/null
echo ""

echo "========================================"
echo "All applications stopped!"
echo "========================================"
echo ""

# Verify ports are free
sleep 2
echo "Verifying ports are free..."
all_free=true

for port in 8080 8081 8082; do
    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1 || netstat -an 2>/dev/null | grep -q ":$port.*LISTEN"; then
        echo "  ✗ Port $port is still in use"
        all_free=false
    else
        echo "  ✓ Port $port is free"
    fi
done

echo ""
if $all_free; then
    echo "✓ All ports are free!"
else
    echo "⚠ Some ports are still in use. You may need to manually kill processes."
    echo "  Use: lsof -ti:8080 | xargs kill -9"
    echo "  Or:  fuser -k 8080/tcp"
fi
echo ""
