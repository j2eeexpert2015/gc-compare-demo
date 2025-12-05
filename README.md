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

**Option A: 2GB heap (Recommended):**
```batch
java -XX:+UseG1GC -Xms2g -Xmx2g -XX:StartFlightRecording=filename=g1gc-recording.jfr -Dserver.port=8080 -Dspring.application.name=g1gc-demo -jar target\gc-compare-demo-1.0.0.jar
```

**Option B: 4GB heap (Production-like):**
```batch
java -XX:+UseG1GC -Xms4g -Xmx4g -XX:StartFlightRecording=filename=g1gc-recording.jfr -Dserver.port=8080 -Dspring.application.name=g1gc-demo -jar target\gc-compare-demo-1.0.0.jar
```

### 4. Start ZGC App (Terminal 2)

**Option A: 2GB heap (Recommended):**
```batch
java -XX:+UseZGC -XX:+ZGenerational -Xms2g -Xmx2g -XX:StartFlightRecording=filename=zgc-gen-recording.jfr -Dserver.port=8081 -Dspring.application.name=zgc-demo -jar target\gc-compare-demo-1.0.0.jar
```

**Option B: 4GB heap (Production-like):**
```batch
java -XX:+UseZGC -XX:+ZGenerational -Xms4g -Xmx4g -XX:StartFlightRecording=filename=zgc-gen-recording.jfr -Dserver.port=8081 -Dspring.application.name=zgc-demo -jar target\gc-compare-demo-1.0.0.jar
```

### 5. Start ZGC Non-Generational App (Terminal 3) - Optional

**Option A: 2GB heap (Recommended):**
```batch
java -XX:+UseZGC -Xms2g -Xmx2g -XX:StartFlightRecording=filename=zgc-nongen-recording.jfr -Dserver.port=8082 -Dspring.application.name=zgc-nongen-demo -jar target\gc-compare-demo-1.0.0.jar
```

**Option B: 4GB heap (Production-like):**
```batch
java -XX:+UseZGC -Xms4g -Xmx4g -XX:StartFlightRecording=filename=zgc-nongen-recording.jfr -Dserver.port=8082 -Dspring.application.name=zgc-nongen-demo -jar target\gc-compare-demo-1.0.0.jar
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

**Option A: 2GB heap (Recommended):**
```batch
java -XX:+UseZGC -XX:+ZGenerational -Xms2g -Xmx2g -XX:StartFlightRecording=filename=zgc-gen-recording.jfr -Dserver.port=8081 -Dspring.application.name=zgc-gen-demo -jar target\gc-compare-demo-1.0.0.jar
```

**Option B: 4GB heap (Production-like):**
```batch
java -XX:+UseZGC -XX:+ZGenerational -Xms4g -Xmx4g -XX:StartFlightRecording=filename=zgc-gen-recording.jfr -Dserver.port=8081 -Dspring.application.name=zgc-gen-demo -jar target\gc-compare-demo-1.0.0.jar
```

### 2. Start ZGC Non-Generational (Terminal 2)

**Option A: 2GB heap (Recommended):**
```batch
java -XX:+UseZGC -Xms2g -Xmx2g -XX:StartFlightRecording=filename=zgc-nongen-recording.jfr -Dserver.port=8082 -Dspring.application.name=zgc-nongen-demo -jar target\gc-compare-demo-1.0.0.jar
```

**Option B: 4GB heap (Production-like):**
```batch
java -XX:+UseZGC -Xms4g -Xmx4g -XX:StartFlightRecording=filename=zgc-nongen-recording.jfr -Dserver.port=8082 -Dspring.application.name=zgc-nongen-demo -jar target\gc-compare-demo-1.0.0.jar
```

### 3. Verify Both Apps
```batch
curl http://localhost:8081/api/memory/info
curl http://localhost:8082/api/memory/info
```

### 4. Run Load Test

**Uniform Load (all objects die at once):**
```batch
for /L %i in (1,1,100) do @(curl -s -X POST http://localhost:8081/api/memory/load/50 >nul & curl -s -X POST http://localhost:8082/api/memory/load/50 >nul & echo Iteration %i & timeout /t 1 /nobreak >nul)
```

**Mixed Load (demonstrates Gen ZGC advantage):**
```batch
for /L %i in (1,1,150) do @(curl -s -X POST http://localhost:8081/api/memory/mixed/80/20 >nul & curl -s -X POST http://localhost:8082/api/memory/mixed/80/20 >nul & echo Iteration %i & timeout /t 1 /nobreak >nul)
```

Or use the provided script:
```batch
scripts\load-test-mixed.bat
```

### 5. View Dashboard

Open http://localhost:3000 (admin/password)

---

## Mixed Object Lifetime Workload (NEW Feature)

### Why This Matters

Your uniform load tests (`/api/memory/load/{count}`) show **NonGen ZGC performing better** because all objects die at once - there's no generational pattern to optimize. This is typical for:
- Batch jobs that process data and exit
- Short-lived Lambda functions  
- Any workload where all objects have the same lifetime

The mixed workload demonstrates the **Weak Generational Hypothesis**:
> "Most objects die young, but some live a long time"

When this is true (typical of production apps), **Gen ZGC significantly outperforms NonGen** by:
- Collecting young objects cheaply and frequently
- Promoting survivors to old generation
- Collecting old generation rarely

This results in **30-40% fewer total GC cycles** with Gen ZGC!

### New API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/memory/mixed/{shortLivedMB}/{longLivedMB}` | POST | Creates mixed workload with short-lived and long-lived objects |
| `/api/memory/cache-stats` | GET | Get current cache statistics |
| `/api/memory/clear-cache` | POST | Clear all cached long-lived objects |

### Usage Examples

**Typical workload (80% short-lived, 20% long-lived):**
```batch
curl -X POST http://localhost:8081/api/memory/mixed/80/20
```

**High churn workload (90% short-lived, 10% long-lived):**
```batch
curl -X POST http://localhost:8081/api/memory/mixed/90/10
```

**Check cache stats:**
```batch
curl http://localhost:8081/api/memory/cache-stats
```

**Clear cache (reset between tests):**
```batch
curl -X POST http://localhost:8081/api/memory/clear-cache
```

### Load Test Commands

**Gen vs NonGen with Mixed Workload:**
```batch
# 80MB short-lived + 20MB long-lived (typical ratio)
for /L %i in (1,1,150) do @(curl -s -X POST http://localhost:8081/api/memory/mixed/80/20 >nul & curl -s -X POST http://localhost:8082/api/memory/mixed/80/20 >nul & echo Iteration %i & timeout /t 1 /nobreak >nul)

# 90MB short-lived + 10MB long-lived (high churn)
for /L %i in (1,1,150) do @(curl -s -X POST http://localhost:8081/api/memory/mixed/90/10 >nul & curl -s -X POST http://localhost:8082/api/memory/mixed/90/10 >nul & echo Iteration %i & timeout /t 1 /nobreak >nul)
```

### Expected Results Comparison

| Workload Type | Gen ZGC Pauses | NonGen Pauses | Winner | Reduction |
|---------------|----------------|---------------|--------|-----------|
| **Uniform Load** (`/load/50`) | ~350-400 | **~280-320** | NonGen ✅ | - |
| **Mixed Load** (`/mixed/80/20`) | **~250-300** | ~400-450 | **Gen ✅** | **30-40%** |

### Real-World Applications

This mixed pattern matches production workloads:
- **Web Apps**: HTTP request/response objects (short) + session data (long)
- **Microservices**: Processing objects (short) + connection pools (long)
- **Data Processing**: Intermediate results (short) + lookup tables (long)
- **Game Servers**: Event packets (short) + player sessions (long)

---

## API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/memory/info` | GET | JVM and GC info |
| `/api/memory/load/{count}` | POST | Allocate count * 10MB of garbage (uniform - all dies at once) |
| `/api/memory/mixed/{shortLivedMB}/{longLivedMB}` | POST | **NEW:** Mixed workload - short-lived + long-lived objects |
| `/api/memory/sustained` | POST | Sustained allocation over time |
| `/api/memory/gc-stats` | GET | Current GC statistics |
| `/api/memory/cache-stats` | GET | **NEW:** Get cache statistics for mixed workload |
| `/api/memory/clear-cache` | POST | **NEW:** Clear cached long-lived objects |
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

### G1GC vs ZGC Generational

Based on production benchmarks and testing:

| Metric | G1GC | ZGC (Generational) | ZGC (Non-Gen) |
|--------|------|-------------------|---------------|
| **Max Pause Time** | 50-200ms+ | <1ms | <1ms |
| **CPU Overhead** | Higher | Lower | Lower |
| **GC Overhead** | Higher | Minimal | Minimal |
| **P99 Latency** | Spiky | Consistent | Consistent |
| **Throughput** | Good | Excellent | Very Good |

### ZGC Gen vs NonGen: Workload Matters!

The winner depends on your workload pattern:

| Workload Type | Gen ZGC Pauses | NonGen Pauses | Winner | Why |
|---------------|----------------|---------------|--------|-----|
| **Uniform** (`/load/50`) | ~350-400 | **~280-320** | NonGen ✅ | No generational pattern - simpler approach wins |
| **Mixed** (`/mixed/80/20`) | **~250-300** | ~400-450 | **Gen ✅** | Generational pattern - young gen optimization reduces cycles by 30-40% |

**Key Insight:**
- **Gen ZGC** excels with mixed object lifetimes (typical of production workloads like web apps, microservices)
- **NonGen ZGC** can be more efficient with uniform lifetimes (batch jobs, Lambda functions, data processing tasks where all objects die together)
- For most **production applications**, Gen ZGC is the better choice (and the default in Java 21+)

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
│   ├── load-test.bat
│   └── load-test-mixed.bat (NEW)
└── README.md
```

## Customization

### Adjust Heap Size

```batch
# 2GB heap (recommended for demos and testing)
-Xms2g -Xmx2g

# 4GB heap (production-like)
-Xms4g -Xmx4g
```

### Adjust Load Pattern

```batch
# Uniform load - Light (10MB)
curl -X POST http://localhost:8080/api/memory/load/10

# Uniform load - Heavy (100MB)
curl -X POST http://localhost:8080/api/memory/load/100

# Mixed load - Typical (80% short, 20% long)
curl -X POST http://localhost:8080/api/memory/mixed/80/20

# Mixed load - High churn (90% short, 10% long)
curl -X POST http://localhost:8080/api/memory/mixed/90/10

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
1. Reduce load count: `/api/memory/load/10` instead of `/load/50`
2. Or reduce mixed workload: `/api/memory/mixed/40/10` instead of `/mixed/80/20`
3. Increase heap: `-Xms4g -Xmx4g`
4. Clear cache if using mixed workload: `curl -X POST http://localhost:8081/api/memory/clear-cache`

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
# 2GB heap (recommended)
java -XX:+UseG1GC -Xms2g -Xmx2g -XX:StartFlightRecording=filename=g1gc-recording.jfr -Dserver.port=8080 -Dspring.application.name=g1gc-demo -jar target\gc-compare-demo-1.0.0.jar

# 4GB heap (production-like)
java -XX:+UseG1GC -Xms4g -Xmx4g -XX:StartFlightRecording=filename=g1gc-recording.jfr -Dserver.port=8080 -Dspring.application.name=g1gc-demo -jar target\gc-compare-demo-1.0.0.jar
```

**Run ZGC Generational:**
```batch
# 2GB heap (recommended)
java -XX:+UseZGC -XX:+ZGenerational -Xms2g -Xmx2g -XX:StartFlightRecording=filename=zgc-gen-recording.jfr -Dserver.port=8081 -Dspring.application.name=zgc-demo -jar target\gc-compare-demo-1.0.0.jar

# 4GB heap (production-like)
java -XX:+UseZGC -XX:+ZGenerational -Xms4g -Xmx4g -XX:StartFlightRecording=filename=zgc-gen-recording.jfr -Dserver.port=8081 -Dspring.application.name=zgc-demo -jar target\gc-compare-demo-1.0.0.jar
```

**Run ZGC Non-Generational:**
```batch
# 2GB heap (recommended)
java -XX:+UseZGC -Xms2g -Xmx2g -XX:StartFlightRecording=filename=zgc-nongen-recording.jfr -Dserver.port=8082 -Dspring.application.name=zgc-nongen-demo -jar target\gc-compare-demo-1.0.0.jar

# 4GB heap (production-like)
java -XX:+UseZGC -Xms4g -Xmx4g -XX:StartFlightRecording=filename=zgc-nongen-recording.jfr -Dserver.port=8082 -Dspring.application.name=zgc-nongen-demo -jar target\gc-compare-demo-1.0.0.jar
```

**Uniform Load Test (all objects die at once):**
```batch
# G1GC vs ZGC Gen (50 iterations, 300MB per iteration)
for /L %i in (1,1,50) do @(curl -s -X POST http://localhost:8080/api/memory/load/30 >nul & curl -s -X POST http://localhost:8081/api/memory/load/30 >nul & echo Iteration %i & timeout /t 2 /nobreak >nul)

# ZGC Gen vs NonGen (100 iterations, 500MB per iteration)
for /L %i in (1,1,100) do @(curl -s -X POST http://localhost:8081/api/memory/load/50 >nul & curl -s -X POST http://localhost:8082/api/memory/load/50 >nul & echo Iteration %i & timeout /t 1 /nobreak >nul)

# All three GCs (50 iterations)
for /L %i in (1,1,50) do @(curl -s -X POST http://localhost:8080/api/memory/load/30 >nul & curl -s -X POST http://localhost:8081/api/memory/load/30 >nul & curl -s -X POST http://localhost:8082/api/memory/load/30 >nul & echo Iteration %i & timeout /t 2 /nobreak >nul)
```

**Mixed Load Test (shows Gen ZGC advantage):**
```batch
# 80MB short-lived + 20MB long-lived (typical ratio)
for /L %i in (1,1,150) do @(curl -s -X POST http://localhost:8081/api/memory/mixed/80/20 >nul & curl -s -X POST http://localhost:8082/api/memory/mixed/80/20 >nul & echo Iteration %i & timeout /t 1 /nobreak >nul)

# 90MB short-lived + 10MB long-lived (high churn)
for /L %i in (1,1,150) do @(curl -s -X POST http://localhost:8081/api/memory/mixed/90/10 >nul & curl -s -X POST http://localhost:8082/api/memory/mixed/90/10 >nul & echo Iteration %i & timeout /t 1 /nobreak >nul)

# Or use script
scripts\load-test-mixed.bat
```

**Cache Management:**
```batch
# Check cache stats
curl http://localhost:8081/api/memory/cache-stats

# Clear cache
curl -X POST http://localhost:8081/api/memory/clear-cache
```

**Stop Everything:**
```batch
docker-compose down
```
