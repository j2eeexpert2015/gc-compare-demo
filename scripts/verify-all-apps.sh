#!/bin/bash

########################################
# Verify All GC Applications
########################################
#
# This script checks if all 3 apps are running
# and responsive
########################################

echo ""
echo "========================================"
echo "Verifying All GC Applications"
echo "========================================"
echo ""

# Function to check if port is listening
check_port() {
    local port=$1
    local name=$2
    
    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1 || netstat -an 2>/dev/null | grep -q ":$port.*LISTEN"; then
        echo "  ✓ Port $port is listening ($name)"
        return 0
    else
        echo "  ✗ Port $port is NOT listening ($name)"
        return 1
    fi
}

# Function to check HTTP health endpoint
check_health() {
    local port=$1
    local name=$2
    
    if command -v curl &> /dev/null; then
        if curl -s -f http://localhost:$port/actuator/health > /dev/null 2>&1; then
            echo "  ✓ $name is responding to health checks"
            return 0
        else
            echo "  ✗ $name is NOT responding to health checks"
            return 1
        fi
    else
        echo "  ⚠ curl not found, skipping health check"
        return 0
    fi
}

# Check each app
all_ok=true

echo "Checking G1GC (port 8080)..."
if check_port 8080 "G1GC"; then
    check_health 8080 "G1GC"
else
    all_ok=false
fi
echo ""

echo "Checking Generational ZGC (port 8081)..."
if check_port 8081 "Gen ZGC"; then
    check_health 8081 "Gen ZGC"
else
    all_ok=false
fi
echo ""

echo "Checking ZGC (port 8082)..."
if check_port 8082 "ZGC"; then
    check_health 8082 "ZGC"
else
    all_ok=false
fi
echo ""

# Summary
echo "========================================"
if $all_ok; then
    echo "✓ All applications are running!"
    echo "========================================"
    echo ""
    exit 0
else
    echo "✗ Some applications are NOT running!"
    echo "========================================"
    echo ""
    echo "To start apps, run: ./start-all-apps.sh"
    echo ""
    exit 1
fi
