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
cd docker
docker-compose up -d
```

---

## Three Test Scenarios with 4GB Heap

All tests use **4GB heap** with **JFR Recording enabled** for detailed analysis.

---

## Scenario 1: G1GC vs ZGC Generational Comparison

### Start Applications

**Terminal 1 - G1GC (4GB heap with JFR):**
```batch
java -XX:+UseG1GC -Xms4g -Xmx4g -XX:StartFlightRecording=filename=g1gc-recording.jfr,dumponexit=true,settings=profile -Dserver.port=8080 -Dspring.application.name=g1gc-demo -jar target\gc-compare-demo-1.0.0.jar
```

**Terminal 2 - ZGC Generational (4GB heap with JFR):**
```batch
java -XX:+UseZGC -XX:+ZGenerational -Xms4g -Xmx4g -XX:StartFlightRecording=filename=zgc-gen-recording.jfr,dumponexit=true,settings=profile -Dserver.port=8081 -Dspring.application.name=zgc-demo -jar target\gc-compare-demo-1.0.0.jar
```

### Verify Applications
```batch
curl http://localhost:8080/api/memory/info
curl http://localhost:8081/api/memory/info
```

### Run Test - Natural Generational Workload
```batch
for /L %i in (1,1,200) do @(curl -s -X POST http://localhost:8080/api/better/natural/80/20 & curl -s -X POST http://localhost:8081/api/better/natural/80/20 & timeout /t 1 /nobreak >nul)
```

### View Dashboard
- Open: http://localhost:3000 (admin/password)
- Dashboard: **"G1GC vs ZGC Comparison"**
- Expected: ZGC shows sub-millisecond pauses, G1GC shows 20-200ms pauses

### Analyze JFR Files
```batch
jmc g1gc-recording.jfr
jmc zgc-gen-recording.jfr
```

---

## Scenario 2: ZGC Generational vs Non-Generational Comparison

### Start Applications

**Terminal 1 - ZGC Generational (4GB heap with JFR):**
```batch
java -XX:+UseZGC -XX:+ZGenerational -Xms4g -Xmx4g -XX:StartFlightRecording=filename=zgc-gen-recording.jfr,dumponexit=true,settings=profile -Dserver.port=8081 -Dspring.application.name=zgc-gen-demo -jar target\gc-compare-demo-1.0.0.jar
```

**Terminal 2 - ZGC Non-Generational (4GB heap with JFR):**
```batch
java -XX:+UseZGC -Xms4g -Xmx4g -XX:StartFlightRecording=filename=zgc-nongen-recording.jfr,dumponexit=true,settings=profile -Dserver.port=8082 -Dspring.application.name=zgc-nongen-demo -jar target\gc-compare-demo-1.0.0.jar
```

### Verify Applications
```batch
curl http://localhost:8081/api/better/stats
curl http://localhost:8082/api/better/stats
```

### Run Test - Natural Generational Workload
```batch
for /L %i in (1,1,200) do @(curl -s -X POST http://localhost:8081/api/better/natural/80/20 & curl -s -X POST http://localhost:8082/api/better/natural/80/20 & timeout /t 1 /nobreak >nul)
```

### View Dashboard
- Open: http://localhost:3000
- Dashboard: **"ZGC: Generational vs Non-Generational Comparison"**
- Expected: Gen ZGC shows 30-40% fewer total pauses than NonGen ZGC

### Analyze JFR Files
```batch
jmc zgc-gen-recording.jfr
jmc zgc-nongen-recording.jfr
```

---

## Scenario 3: All Three GCs Comparison (G1GC vs ZGC Gen vs ZGC NonGen)

### Start Applications

**Terminal 1 - G1GC (4GB heap with JFR):**
```batch
java -XX:+UseG1GC -Xms4g -Xmx4g -XX:StartFlightRecording=filename=g1gc-recording.jfr,dumponexit=true,settings=profile -Dserver.port=8080 -Dspring.application.name=g1gc-demo -jar target\gc-compare-demo-1.0.0.jar
```

**Terminal 2 - ZGC Generational (4GB heap with JFR):**
```batch
java -XX:+UseZGC -XX:+ZGenerational -Xms4g -Xmx4g -XX:StartFlightRecording=filename=zgc-gen-recording.jfr,dumponexit=true,settings=profile -Dserver.port=8081 -Dspring.application.name=zgc-gen-demo -jar target\gc-compare-demo-1.0.0.jar
```

**Terminal 3 - ZGC Non-Generational (4GB heap with JFR):**
```batch
java -XX:+UseZGC -Xms4g -Xmx4g -XX:StartFlightRecording=filename=zgc-nongen-recording.jfr,dumponexit=true,settings=profile -Dserver.port=8082 -Dspring.application.name=zgc-nongen-demo -jar target\gc-compare-demo-1.0.0.jar
```

### Verify Applications
```batch
curl http://localhost:8080/api/memory/info
curl http://localhost:8081/api/better/stats
curl http://localhost:8082/api/better/stats
```

### Run Test - Natural Generational Workload (All Three)
```batch
for /L %i in (1,1,200) do @(curl -s -X POST http://localhost:8080/api/better/natural/80/20 & curl -s -X POST http://localhost:8081/api/better/natural/80/20 & curl -s -X POST http://localhost:8082/api/better/natural/80/20 & timeout /t 1 /nobreak >nul)
```

### View Dashboard
- Open: http://localhost:3000
- Dashboard: **"All Three GCs Comparison"**
- Expected Results:
  - G1GC: Longest pauses (20-200ms)
  - ZGC Gen: Lowest total pause count
  - ZGC NonGen: Sub-millisecond pauses but more frequent
  - Both ZGC variants: <1ms max pause time

### Analyze JFR Files
```batch
jmc g1gc-recording.jfr
jmc zgc-gen-recording.jfr
jmc zgc-nongen-recording.jfr
```

---

## API Endpoints

### Natural Generational Workload (Recommended)
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/better/natural/{shortLivedMB}/{longLivedMB}` | POST | Natural generational pattern that works WITH Gen ZGC |
| `/api/better/stats` | GET | Get survivor statistics |
| `/api/better/clear` | POST | Clear survivors |

**Example:**
```batch
curl -X POST http://localhost:8081/api/better/natural/80/20
```
Creates 80MB short-lived objects + 20MB survivors that rotate naturally.

### Legacy Endpoints
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/memory/info` | GET | JVM and GC info |
| `/api/memory/load/{count}` | POST | Uniform allocation (all objects die together) |
| `/api/memory/gc-stats` | GET | Current GC statistics |
| `/actuator/prometheus` | GET | Prometheus metrics |

---

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

## Expected Results Summary

### With 4GB Heap + Natural Generational Workload:

| Metric | G1GC | ZGC Gen | ZGC NonGen | Best |
|--------|------|---------|------------|------|
| **Max Pause Time** | 50-200ms | <1ms | <1ms | ZGC (both) ✅ |
| **Total Pauses (200 iter)** | ~100-150 | ~150-200 | ~200-250 | G1GC ✅ |
| **GC Overhead %** | 0.5-1% | <0.1% | <0.1% | ZGC (both) ✅ |
| **P99 Latency** | Spiky | Consistent | Consistent | ZGC (both) ✅ |
| **CPU Usage** | Higher | Lower | Moderate | ZGC Gen ✅ |
| **Allocation Stalls** | None | None | Possible | Gen ✅ |

**Key Insights:**
- **G1GC:** Fewest total pauses but MUCH longer pause times (20-200ms)
- **ZGC Gen:** Best overall - sub-ms pauses with lowest CPU overhead
- **ZGC NonGen:** Sub-ms pauses but 20-30% more pauses than Gen
- **For Production:** ZGC Generational is recommended for low-latency applications

---

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

---

## Understanding Natural Generational Workload

The `/api/better/natural/{short}/{long}` endpoint creates a realistic workload:

```
Request 1: POST /api/better/natural/80/20
├─ 80MB short-lived objects → die immediately (young generation)
└─ 20MB survivors → kept alive, rotate out after 20 requests (old generation)

Request 2: POST /api/better/natural/80/20
├─ 80MB short-lived objects → die immediately
└─ 20MB survivors → add to pool (now 40MB total survivors)

Request 20: POST /api/better/natural/80/20
├─ 80MB short-lived objects → die immediately
└─ 20MB survivors → add to pool (~200MB survivors, oldest start rotating out)
```

**Why This Works:**
- Gen ZGC recognizes the pattern and optimizes young collection
- NonGen ZGC treats all objects equally = more work
- Result: Gen ZGC shows 30-40% efficiency gain with 4GB heap

---

## JFR Analysis Tips

After running tests, analyze JFR files with JDK Mission Control:

**Key Areas to Compare:**
1. **GC** → Compare pause times and frequencies
2. **Memory** → Look at allocation patterns
3. **Java Application** → Check for allocation stalls
4. **JVM Internals** → Compare GC algorithms behavior

**G1GC Specifics:**
- Check "Evacuation Pause" frequency
- Look for "Humongous Allocations"

**ZGC Specifics:**
- Check for "Allocation Stall" events (should be none or minimal)
- Compare "Proactive" vs "Allocation Rate" cycles
- Gen ZGC: Look at young vs old generation collections

---

## Troubleshooting

**Apps not starting?**
- Verify Java 21+: `java -version`
- Check ports available: `netstat -ano | findstr "8080"`

**Prometheus not scraping?**
- Check targets: http://localhost:9090/targets
- Verify `host.docker.internal` resolves

**Grafana dashboard empty?**
- Wait 30-60 seconds for metrics to populate
- Verify Prometheus targets are UP

**OutOfMemoryError?**
- With 4GB heap, this should not happen
- Check if other applications are consuming memory
- Verify JVM is actually using 4GB: check JFR recording or logs

---

## Project Structure

```
gc-compare-demo/
├── pom.xml
├── src/main/java/com/example/gcdemo/
│   ├── GcCompareDemoApplication.java
│   ├── controller/
│   │   ├── MemoryController.java
│   │   └── BetterMemoryController.java
│   └── service/
│       ├── MemoryLoadService.java
│       └── BetterMemoryService.java
├── src/main/resources/
│   └── application.yml
├── docker/
│   ├── docker-compose.yml
│   ├── prometheus.yml
│   └── grafana/provisioning/
│       ├── datasources/
│       └── dashboards/
│           ├── gc-compare.json
│           ├── gc-all-three-compare.json
│           ├── zgc-comparison.json
│           ├── g1gc-detailed.json
│           ├── zgc-detailed.json
│           └── zgc-nongen-detailed.json
└── README.md
```

---

## Command Reference (4GB Heap)

### Start Commands with JFR

**G1GC:**
```batch
java -XX:+UseG1GC -Xms4g -Xmx4g -XX:StartFlightRecording=filename=g1gc-recording.jfr,dumponexit=true,settings=profile -Dserver.port=8080 -Dspring.application.name=g1gc-demo -jar target\gc-compare-demo-1.0.0.jar
```

**ZGC Generational:**
```batch
java -XX:+UseZGC -XX:+ZGenerational -Xms4g -Xmx4g -XX:StartFlightRecording=filename=zgc-gen-recording.jfr,dumponexit=true,settings=profile -Dserver.port=8081 -Dspring.application.name=zgc-gen-demo -jar target\gc-compare-demo-1.0.0.jar
```

**ZGC Non-Generational:**
```batch
java -XX:+UseZGC -Xms4g -Xmx4g -XX:StartFlightRecording=filename=zgc-nongen-recording.jfr,dumponexit=true,settings=profile -Dserver.port=8082 -Dspring.application.name=zgc-nongen-demo -jar target\gc-compare-demo-1.0.0.jar
```

### Test Commands

**Scenario 1 (G1GC vs ZGC Gen):**
```batch
for /L %i in (1,1,200) do @(curl -s -X POST http://localhost:8080/api/better/natural/80/20 & curl -s -X POST http://localhost:8081/api/better/natural/80/20 & timeout /t 1 /nobreak >nul)
```

**Scenario 2 (ZGC Gen vs NonGen):**
```batch
for /L %i in (1,1,200) do @(curl -s -X POST http://localhost:8081/api/better/natural/80/20 & curl -s -X POST http://localhost:8082/api/better/natural/80/20 & timeout /t 1 /nobreak >nul)
```

**Scenario 3 (All Three):**
```batch
for /L %i in (1,1,200) do @(curl -s -X POST http://localhost:8080/api/better/natural/80/20 & curl -s -X POST http://localhost:8081/api/better/natural/80/20 & curl -s -X POST http://localhost:8082/api/better/natural/80/20 & timeout /t 1 /nobreak >nul)
```

---

## Additional Resources

- **JEP 439:** Generational ZGC - https://openjdk.org/jeps/439
- **ZGC Wiki:** https://wiki.openjdk.org/display/zgc
- **G1GC Tuning:** https://docs.oracle.com/en/java/javase/21/gctuning/
- **JFR Documentation:** https://docs.oracle.com/javacomponents/jmc-latest/

---

## License

This project is for educational purposes demonstrating GC behavior comparison.
