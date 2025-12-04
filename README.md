# G1GC vs ZGC Performance Comparison Demo

A Spring Boot application to compare G1GC and ZGC garbage collectors using Prometheus and Grafana.

## Prerequisites

- **Java 21+** (for Generational ZGC support)
- **Maven 3.8+**
- **Docker Desktop** (for Prometheus + Grafana)
- **curl** (for load testing)

## Quick Start

### 1. Build the Application
```batch
mvn clean package -DskipTests
```

### 2. Start Prometheus & Grafana
```batch
docker-compose up -d
```

### 3. Start G1GC App (Terminal 1)
```batch
java -XX:+UseG1GC -Xms512m -Xmx512m -XX:StartFlightRecording=filename=g1gc-recording.jfr -Dserver.port=8080 -Dspring.application.name=g1gc-demo -jar target\gc-compare-demo-1.0.0.jar
```

### 4. Start ZGC App (Terminal 2)
```batch
java -XX:+UseZGC -XX:+ZGenerational -Xms512m -Xmx512m -XX:StartFlightRecording=filename=zgc-gen-recording.jfr -Dserver.port=8081 -Dspring.application.name=zgc-demo -jar target\gc-compare-demo-1.0.0.jar
```

### 5. Start ZGC Non-Generational App (Terminal 3) - Optional
```batch
java -XX:+UseZGC -Xms512m -Xmx512m -XX:StartFlightRecording=filename=zgc-nongen-recording.jfr -Dserver.port=8082 -Dspring.application.name=zgc-nongen-demo -jar target\gc-compare-demo-1.0.0.jar
```

### 6. Verify Apps
```batch
curl http://localhost:8080/api/memory/info
curl http://localhost:8081/api/memory/info
curl http://localhost:8082/api/memory/info
```

### 7. Run Load Test

**For G1GC vs ZGC Gen (2 instances):**
```batch
for /L %i in (1,1,50) do @(curl -s -X POST http://localhost:8080/api/memory/load/30 >nul & curl -s -X POST http://localhost:8081/api/memory/load/30 >nul & echo Iteration %i & timeout /t 2 /nobreak >nul)
```

**For All Three GCs (3 instances):**
```batch
for /L %i in (1,1,50) do @(curl -s -X POST http://localhost:8080/api/memory/load/30 >nul & curl -s -X POST http://localhost:8081/api/memory/load/30 >nul & curl -s -X POST http://localhost:8082/api/memory/load/30 >nul & echo Iteration %i & timeout /t 2 /nobreak >nul)
```

### 8. View Dashboards

Open http://localhost:3000 (admin/password)

## Grafana Dashboards

Six dashboards are provided:

| Dashboard | Purpose | Key Panels |
|-----------|---------|------------|
| **G1GC vs ZGC Comparison** | Side-by-side comparison | Pause Time, Pause Count, Heap, CPU, Overhead, P99 Latency |
| **All Three GCs Comparison** | G1GC vs ZGC Gen vs ZGC NonGen | Same metrics showing all three collectors |
| **ZGC: Generational vs Non-Generational** | Compare ZGC modes | Same metrics showing Gen vs NonGen differences |
| **G1GC Detailed Metrics** | Deep dive into G1GC | + GC Events by Cause (Evacuation Pause, Humongous Allocation) |
| **ZGC Detailed Metrics** | Deep dive into ZGC Generational | + ZGC Cycles by Reason (Allocation Stall ⚠️, Proactive, Warmup) |
| **ZGC Non-Generational Detailed Metrics** | Deep dive into ZGC NonGen | Same detailed metrics for NonGen mode |

### Color Scheme
- 🔴 **Red** = G1GC
- 🟢 **Green** = ZGC Generational
- 🔵 **Blue** = ZGC Non-Generational

---

## ZGC: Generational vs Non-Generational Comparison

### 1. Start ZGC Generational (Terminal 1)
```batch
java -XX:+UseZGC -XX:+ZGenerational -Xms512m -Xmx512m -XX:StartFlightRecording=filename=zgc-gen-recording.jfr -Dserver.port=8081 -Dspring.application.name=zgc-gen-demo -jar target\gc-compare-demo-1.0.0.jar
```

### 2. Start ZGC Non-Generational (Terminal 2)
```batch
java -XX:+UseZGC -Xms512m -Xmx512m -XX:StartFlightRecording=filename=zgc-nongen-recording.jfr -Dserver.port=8082 -Dspring.application.name=zgc-nongen-demo -jar target\gc-compare-demo-1.0.0.jar
```

### 3. Verify Both Apps
```batch
curl http://localhost:8081/api/memory/info
curl http://localhost:8082/api/memory/info
```

### 4. Run Load Test
```batch
for /L %i in (1,1,50) do @(curl -s -X POST http://localhost:8081/api/memory/load/30 >nul & curl -s -X POST http://localhost:8082/api/memory/load/30 >nul & echo Iteration %i & timeout /t 2 /nobreak >nul)
```

### 5. View Dashboard

Open http://localhost:3000 (admin/password)

---

## API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/memory/info` | GET | JVM and GC info |
| `/api/memory/load/{count}` | POST | Allocate count * 10MB of garbage |
| `/api/memory/sustained` | POST | Sustained allocation over time |
| `/api/memory/gc-stats` | GET | Current GC statistics |
| `/actuator/prometheus` | GET | Prometheus metrics |

## Key Metrics Compared

| Metric | What it Shows |
|--------|---------------|
| **GC Pause Time** | How long the JVM stops (lower = better) |
| **GC Pause Count** | Number of GC events |
| **Heap Memory Used** | Memory consumption pattern |
| **Process CPU Usage** | CPU overhead of GC |
| **GC Overhead** | % time spent in GC |
| **HTTP Request Latency P99** | Tail latency impact |
| **Allocation Rate** | Memory allocation throughput |

## Expected Results

Based on your previous demos and Vishalendu's findings:

| Metric | G1GC | ZGC (Generational) |
|--------|------|-------------------|
| Max Pause Time | 50-200ms+ | <1ms |
| CPU Overhead | Higher | Lower |
| GC Overhead | Higher | Lower |
| P99 Latency | Spiky | Consistent |

## Project Structure

```
gc-compare-demo/
├── pom.xml
├── src/main/java/com/example/gcdemo/
│   ├── GcCompareDemoApplication.java
│   ├── controller/MemoryController.java
│   └── service/MemoryLoadService.java
├── src/main/resources/
│   └── application.yml
├── docker/
│   ├── docker-compose.yml
│   ├── prometheus.yml
│   └── grafana/provisioning/
│       ├── datasources/
│       └── dashboards/
│           ├── gc-compare.json
│           ├── gc-all-three-compare.json (NEW)
│           ├── g1gc-detailed.json
│           ├── zgc-detailed.json
│           ├── zgc-nongen-detailed.json (NEW)
│           └── zgc-comparison.json
├── scripts/
│   ├── run-g1gc.bat
│   ├── run-zgc.bat
│   └── load-test.bat
└── README.md
```

## Customization

### Adjust Heap Size

```batch
# Smaller heap = more GC pressure (better for demos)
-Xms256m -Xmx256m

# Larger heap = less frequent GC
-Xms1g -Xmx1g
```

### Adjust Load Pattern

```batch
# Light load (10MB)
curl -X POST http://localhost:8080/api/memory/load/10

# Heavy load (100MB)
curl -X POST http://localhost:8080/api/memory/load/100

# Sustained load (10 seconds, 5 objects/sec)
curl -X POST "http://localhost:8080/api/memory/sustained?duration=10&rate=5"
```

## Java Flight Recorder (JFR)

All commands include JFR recording enabled with `-XX:StartFlightRecording=filename=[name].jfr`

**JFR Files Generated (in project root):**
- `g1gc-recording.jfr` - G1GC performance data
- `zgc-gen-recording.jfr` - ZGC Generational performance data
- `zgc-nongen-recording.jfr` - ZGC Non-Generational performance data

**Analyze JFR Files:**
- Open with JDK Mission Control (JMC): `jmc`
- Or use IntelliJ IDEA: File → Open → Select .jfr file
- Or VisualVM with JFR plugin

**Disable JFR (if not needed):**
Remove `-XX:StartFlightRecording=filename=[name].jfr` from the command

## Troubleshooting

**New dashboards not appearing?**
- Restart Docker containers to load new dashboard files:
  ```batch
  docker-compose restart
  ```
- Wait 10-20 seconds for Grafana to reload
- Refresh the Grafana page in your browser

**Prometheus not scraping new ports (8082)?**
- Restart Docker to pick up prometheus.yml changes:
  ```batch
  docker-compose down
  docker-compose up -d
  ```
- Check Prometheus targets: http://localhost:9090/targets

**Want to clear all metrics data and start fresh?**
- Remove all stored metrics and restart:
  ```batch
  docker-compose down -v
  docker-compose up -d
  ```
- The `-v` flag removes volumes with all historical data

**Prometheus not scraping?**
- Check `host.docker.internal` resolves (Docker Desktop feature)
- Verify apps are running on correct ports

**Grafana dashboard empty?**
- Wait 30-60 seconds for metrics to populate
- Check Prometheus targets: http://localhost:9090/targets

**OutOfMemoryError?**
1. Reduce load count: `/api/memory/load/10` instead of `/load/100`
2. Increase heap: `-Xms1g -Xmx1g`

## Command Reference Cheat Sheet

### Quick Commands for Copy-Paste

**Build:**
```batch
mvn clean package -DskipTests
```

**Start Docker:**
```batch
docker-compose up -d
```

**Run G1GC:**
```batch
java -XX:+UseG1GC -Xms512m -Xmx512m -XX:StartFlightRecording=filename=g1gc-recording.jfr -Dserver.port=8080 -Dspring.application.name=g1gc-demo -jar target\gc-compare-demo-1.0.0.jar
```

**Run ZGC:**
```batch
java -XX:+UseZGC -XX:+ZGenerational -Xms512m -Xmx512m -XX:StartFlightRecording=filename=zgc-gen-recording.jfr -Dserver.port=8081 -Dspring.application.name=zgc-demo -jar target\gc-compare-demo-1.0.0.jar
```

**Run ZGC Non-Generational:**
```batch
java -XX:+UseZGC -Xms512m -Xmx512m -XX:StartFlightRecording=filename=zgc-nongen-recording.jfr -Dserver.port=8082 -Dspring.application.name=zgc-nongen-demo -jar target\gc-compare-demo-1.0.0.jar
```

**Load Test (30 iterations):**
```batch
for /L %i in (1,1,30) do @(curl -s -X POST http://localhost:8080/api/memory/load/20 >nul & curl -s -X POST http://localhost:8081/api/memory/load/20 >nul & echo Iteration %i & timeout /t 3 /nobreak >nul)
```

**Stop Everything:**
```batch
docker-compose down
```
