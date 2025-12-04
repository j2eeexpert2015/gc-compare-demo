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

Four dashboards are provided:

| Dashboard | Purpose | Key Panels |
|-----------|---------|------------|
| **G1GC vs ZGC Comparison** | Side-by-side comparison | Pause Time, Pause Count, Heap, CPU, Overhead, P99 Latency |
| **ZGC: Generational vs Non-Generational** | Compare ZGC modes | Same metrics showing Gen vs NonGen differences |
| **G1GC Detailed Metrics** | Deep dive into G1GC | + GC Events by Cause (Evacuation Pause, Humongous Allocation) |
| **ZGC Detailed Metrics** | Deep dive into ZGC | + ZGC Cycles by Reason (Allocation Stall ⚠️, Proactive, Warmup) |

### Color Scheme
- 🔴 **Red** = G1GC
- 🟢 **Green** = ZGC Generational
- 🔵 **Blue** = ZGC Non-Generational

---

## ZGC: Generational vs Non-Generational Comparison

### 1. Start ZGC Generational (Terminal 1)
```batch
java -XX:+UseZGC -XX:+ZGenerational -Xms512m -Xmx512m -Dserver.port=8081 -Dspring.application.name=zgc-gen-demo -jar target\gc-compare-demo-1.0.0.jar
```

### 2. Start ZGC Non-Generational (Terminal 2)
```batch
java -XX:+UseZGC -Xms512m -Xmx512m -Dserver.port=8082 -Dspring.application.name=zgc-nongen-demo -jar target\gc-compare-demo-1.0.0.jar
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

## Troubleshooting

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
java -XX:+UseG1GC -Xms512m -Xmx512m -Dserver.port=8080 -Dspring.application.name=g1gc-demo -jar target\gc-compare-demo-1.0.0.jar
```

**Run ZGC:**
```batch
java -XX:+UseZGC -XX:+ZGenerational -Xms512m -Xmx512m -Dserver.port=8081 -Dspring.application.name=zgc-demo -jar target\gc-compare-demo-1.0.0.jar
```

**Run ZGC Non-Generational:**
```batch
java -XX:+UseZGC -Xms512m -Xmx512m -Dserver.port=8082 -Dspring.application.name=zgc-nongen-demo -jar target\gc-compare-demo-1.0.0.jar
```

**Load Test (30 iterations):**
```batch
for /L %i in (1,1,30) do @(curl -s -X POST http://localhost:8080/api/memory/load/20 >nul & curl -s -X POST http://localhost:8081/api/memory/load/20 >nul & echo Iteration %i & timeout /t 3 /nobreak >nul)
```

**Stop Everything:**
```batch
docker-compose down
```

## Learning Resources

- **Original Blog**: [Comparing Java 23 GC Types](https://dev.to/vishalendu/comparing-java-23-gc-types-4aj)
- **Original Repo**: [java-gc-demo](https://github.com/vishalendu/java-gc-demo)
- **Supplement**: [java-gc-demo-supplement](https://github.com/vishalendu/java-gc-demo-supplement)
- **Oracle ZGC Docs**: [Java 21 ZGC Guide](https://docs.oracle.com/en/java/javase/21/gctuning/z-garbage-collector.html)

## License

MIT License - feel free to use for educational purposes.

## Contributing

Contributions welcome! Please:
1. Fork the repository
2. Create a feature branch
3. Submit a pull request

## Author

Created for educational purposes to demonstrate real-world GC performance differences.
