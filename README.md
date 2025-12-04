# G1GC vs ZGC Performance Comparison Demo

A Spring Boot application to compare G1GC and ZGC garbage collectors using Prometheus and Grafana.

Inspired by [Vishalendu's Java GC Demo](https://dev.to/vishalendu/comparing-java-23-gc-types-4aj).

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

Access points:
- **Grafana**: http://localhost:3000 (admin/password)
- **Prometheus**: http://localhost:9090

### 3. Run Both GC Instances

Open **two separate terminals**:

**Terminal 1 - G1GC (port 8080):**
```batch
scripts\run-g1gc.bat
```

**Terminal 2 - ZGC (port 8081):**
```batch
scripts\run-zgc.bat
```

### 4. Verify Apps are Running

```batch
curl http://localhost:8080/api/memory/info
curl http://localhost:8081/api/memory/info
```

### 5. Run Load Test

```batch
scripts\load-test.bat
```

### 6. View Results in Grafana

1. Open http://localhost:3000
2. Login: admin / password
3. Go to Dashboards → "G1GC vs ZGC Comparison"

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
├── scripts/
│   ├── run-g1gc.bat
│   ├── run-zgc.bat
│   └── load-test.bat
└── README.md
```

## Customization

### Adjust Heap Size

Edit the batch scripts to change `-Xms` and `-Xmx`:
```batch
-Xms256m -Xmx256m   # Smaller heap = more GC pressure
-Xms1g -Xmx1g       # Larger heap = less frequent GC
```

### Adjust Load Pattern

In `load-test.bat`:
- Change `ITERATIONS` for longer tests
- Change `SLEEP_SECONDS` for faster/slower load
- Adjust `/load/{count}` values for different memory pressure

## Troubleshooting

**Prometheus not scraping?**
- Check `host.docker.internal` resolves (Docker Desktop feature)
- Verify apps are running on correct ports

**Grafana dashboard empty?**
- Wait 30-60 seconds for metrics to populate
- Check Prometheus targets: http://localhost:9090/targets

**OutOfMemoryError?**
- Reduce load count in test script
- Increase heap size in batch scripts
