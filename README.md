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
java -XX:+UseG1GC -Xms512m -Xmx512m -Dserver.port=8080 -Dspring.application.name=g1gc-demo -jar target\gc-compare-demo-1.0.0.jar
```

### 4. Start ZGC App (Terminal 2)
```batch
java -XX:+UseZGC -XX:+ZGenerational -Xms512m -Xmx512m -Dserver.port=8081 -Dspring.application.name=zgc-demo -jar target\gc-compare-demo-1.0.0.jar
```

### 5. Verify Both Apps
```batch
curl http://localhost:8080/api/memory/info
curl http://localhost:8081/api/memory/info
```

### 6. Run Load Test
```batch
for /L %i in (1,1,50) do @(curl -s -X POST http://localhost:8080/api/memory/load/30 >nul & curl -s -X POST http://localhost:8081/api/memory/load/30 >nul & echo Iteration %i & timeout /t 2 /nobreak >nul)
```

### 7. View Dashboards

Open http://localhost:3000 (admin/password)

## Grafana Dashboards

Three dashboards are provided:

| Dashboard | Purpose | Key Panels |
|-----------|---------|------------|
| **G1GC vs ZGC Comparison** | Side-by-side comparison | Pause Time, Pause Count, Heap, CPU, Overhead, P99 Latency |
| **G1GC Detailed Metrics** | Deep dive into G1GC | + GC Events by Cause (Evacuation Pause, Humongous Allocation) |
| **ZGC Detailed Metrics** | Deep dive into ZGC | + ZGC Cycles by Reason (Allocation Stall ⚠️, Proactive, Warmup) |

### Color Scheme
- 🔴 **Red** = G1GC
- 🟢 **Green** = ZGC

## API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/memory/info` | GET | JVM and GC info |
| `/api/memory/load/{count}` | POST | Allocate count × 1MB of garbage |
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
| **GC Events by Cause** | Why G1GC pauses (Evacuation, Humongous, etc.) |
| **ZGC Cycles by Reason** | Why ZGC runs (Allocation Rate, Stall, Proactive) |

## Expected Results

| Metric | G1GC | ZGC (Generational) |
|--------|------|-------------------|
| **Max Pause Time** | 20-200ms | <1ms |
| **CPU Overhead** | Higher spikes | Lower, consistent |
| **GC Overhead** | Higher | Near zero |
| **P99 Latency** | Spiky | Consistent |
| **Pause Count** | Lower | Higher (but sub-ms) |

## When to Use Each GC

**Choose G1GC when:**
- Batch processing jobs
- Throughput > latency
- Heap size < 4GB
- 50-100ms pauses are acceptable

**Choose ZGC when:**
- User-facing APIs
- Latency-critical systems (trading, gaming, real-time)
- Large heaps (8GB+)
- Predictable P99/P999 latency required

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
│       ├── datasources/prometheus.yml
│       └── dashboards/
│           ├── gc-compare.json
│           ├── g1gc-detailed.json
│           └── zgc-detailed.json
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

### GC Tuning Options

```batch
# G1GC with custom pause target
java -XX:+UseG1GC -XX:MaxGCPauseMillis=50 -Xms512m -Xmx512m ...

# ZGC with custom concurrency
java -XX:+UseZGC -XX:+ZGenerational -XX:ConcGCThreads=2 -Xms512m -Xmx512m ...
```

## Troubleshooting

### Prometheus not scraping?
```batch
# Verify apps expose metrics
curl http://localhost:8080/actuator/prometheus
curl http://localhost:8081/actuator/prometheus

# Check Prometheus targets
# Open http://localhost:9090/targets
```

### Grafana dashboard empty?
1. Wait 30-60 seconds for metrics to populate
2. Run a load test to generate data
3. Check Prometheus datasource connection

### OutOfMemoryError?
1. Reduce load count: `/api/memory/load/10` instead of `/load/100`
2. Increase heap: `-Xms1g -Xmx1g`

### Port already in use?
```batch
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

## Command Cheat Sheet

| Action | Command |
|--------|---------|
| Build | `mvn clean package -DskipTests` |
| Start Docker | `docker-compose up -d` |
| Stop Docker | `docker-compose down` |
| Run G1GC | `java -XX:+UseG1GC -Xms512m -Xmx512m -Dserver.port=8080 -Dspring.application.name=g1gc-demo -jar target\gc-compare-demo-1.0.0.jar` |
| Run ZGC | `java -XX:+UseZGC -XX:+ZGenerational -Xms512m -Xmx512m -Dserver.port=8081 -Dspring.application.name=zgc-demo -jar target\gc-compare-demo-1.0.0.jar` |
| Load Test | `for /L %i in (1,1,50) do @(curl -s -X POST http://localhost:8080/api/memory/load/30 >nul & curl -s -X POST http://localhost:8081/api/memory/load/30 >nul & echo Iteration %i & timeout /t 2 /nobreak >nul)` |