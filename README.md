# G1GC vs ZGC Performance Comparison Demo

A Spring Boot application to compare G1GC and ZGC garbage collectors using Prometheus and Grafana.

Inspired by [Vishalendu's Java GC Demo](https://dev.to/vishalendu/comparing-java-23-gc-types-4aj).

## Prerequisites

- **Java 21+** (for Generational ZGC support)
- **Maven 3.8+**
- **Docker Desktop** (for Prometheus + Grafana)
- **curl** (for load testing)

## Complete Command Sequence

Here's the complete step-by-step command sequence to run the demo:

### 1. Build the Application
```batch
cd C:\Intellij_Workspace\gc-compare-demo
mvn clean package -DskipTests
```

### 2. Start Prometheus & Grafana
```batch
cd C:\Intellij_Workspace\gc-compare-demo\docker
docker-compose up -d
```

Access points:
- **Grafana**: http://localhost:3000 (admin/password)
- **Prometheus**: http://localhost:9090

### 3. Start G1GC App (Terminal 1)
```batch
cd C:\Intellij_Workspace\gc-compare-demo
java -XX:+UseG1GC -Xms512m -Xmx512m -Dserver.port=8080 -Dspring.application.name=g1gc-demo -jar target\gc-compare-demo-1.0.0.jar
```

### 4. Start ZGC App (Terminal 2)
```batch
cd C:\Intellij_Workspace\gc-compare-demo
java -XX:+UseZGC -XX:+ZGenerational -Xms512m -Xmx512m -Dserver.port=8081 -Dspring.application.name=zgc-demo -jar target\gc-compare-demo-1.0.0.jar
```

### 5. Verify Both Apps (Terminal 3)
```batch
curl http://localhost:8080/api/memory/info
curl http://localhost:8081/api/memory/info
```

### 6. Run Load Test

**Full Load Test (30 iterations, 5 sec intervals):**
```batch
for /L %i in (1,1,30) do @(curl -s -X POST http://localhost:8080/api/memory/load/10 >nul & curl -s -X POST http://localhost:8081/api/memory/load/10 >nul & echo Iteration %i done & timeout /t 5 /nobreak >nul)
```

**Faster Load Test (30 iterations, 3 sec intervals):**
```batch
for /L %i in (1,1,30) do @(curl -s -X POST http://localhost:8080/api/memory/load/20 >nul & curl -s -X POST http://localhost:8081/api/memory/load/20 >nul & echo Iteration %i & timeout /t 3 /nobreak >nul)
```

**Quick Single Test:**
```batch
curl -X POST http://localhost:8080/api/memory/load/10
curl -X POST http://localhost:8081/api/memory/load/10
```

### 7. View Dashboards
- **Grafana**: http://localhost:3000 (admin/password)
- **Prometheus**: http://localhost:9090

Navigate to **Dashboards** → "G1GC vs ZGC Comparison"

### 8. What You Should See

After running the load test:

- **G1GC Max Pause**: Jump to 20-100ms+ (red spike)
- **ZGC Max Pause**: Stay at <1ms (green stays flat)
- **HTTP Latency**: Data will appear showing performance impact
- **Heap Memory**: Sawtooth pattern on both

## Alternative: Using Batch Scripts

If you prefer batch scripts over direct commands:

**Terminal 1 - G1GC:**
```batch
scripts\run-g1gc.bat
```

**Terminal 2 - ZGC:**
```batch
scripts\run-zgc.bat
```

**Load Test:**
```batch
scripts\load-test.bat
```

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

## Command Reference Cheat Sheet

### Quick Commands for Copy-Paste

**Build:**
```batch
mvn clean package -DskipTests
```

**Start Docker:**
```batch
cd docker && docker-compose up -d
```

**Run G1GC:**
```batch
java -XX:+UseG1GC -Xms512m -Xmx512m -Dserver.port=8080 -Dspring.application.name=g1gc-demo -jar target\gc-compare-demo-1.0.0.jar
```

**Run ZGC:**
```batch
java -XX:+UseZGC -XX:+ZGenerational -Xms512m -Xmx512m -Dserver.port=8081 -Dspring.application.name=zgc-demo -jar target\gc-compare-demo-1.0.0.jar
```

**Load Test (30 iterations):**
```batch
for /L %i in (1,1,30) do @(curl -s -X POST http://localhost:8080/api/memory/load/20 >nul & curl -s -X POST http://localhost:8081/api/memory/load/20 >nul & echo Iteration %i & timeout /t 3 /nobreak >nul)
```

**Stop Everything:**
```batch
cd docker && docker-compose down
```

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

Based on Vishalendu's findings and typical production workloads:

| Metric | G1GC | ZGC (Generational) |
|--------|------|-------------------|
| **Max Pause Time** | 50-200ms+ | <1ms |
| **CPU Overhead** | Higher spikes | Lower, consistent |
| **GC Overhead** | 5-15% | <1% |
| **P99 Latency** | Spiky | Consistent |
| **Pause Count** | Lower | Higher (but sub-ms) |
| **Throughput** | Slightly higher | Slightly lower |

### Real-World Trade-offs

**Choose G1GC when:**
- ✅ Batch processing jobs
- ✅ Throughput > latency
- ✅ Heap size < 4GB
- ✅ Occasional 50-100ms pauses are acceptable
- ✅ Budget-constrained (older hardware OK)

**Choose ZGC when:**
- ✅ User-facing APIs
- ✅ Latency-critical systems (trading, gaming, real-time)
- ✅ Large heaps (8GB+)
- ✅ Predictable P99/P999 latency required
- ✅ Modern hardware available (Java 21+)

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
├── scripts/
│   ├── run-g1gc.bat
│   ├── run-zgc.bat
│   └── load-test.bat
└── README.md
```

## Customization

### Adjust Heap Size

Edit the batch scripts or direct commands to change `-Xms` and `-Xmx`:

```batch
# Smaller heap = more GC pressure (better for demos)
-Xms256m -Xmx256m

# Larger heap = less frequent GC (more realistic production)
-Xms1g -Xmx1g
```

### Adjust Load Pattern

Modify the load test parameters:

**Light Load (500MB):**
```batch
curl -X POST http://localhost:8080/api/memory/load/50
```

**Heavy Load (2GB - may cause OutOfMemoryError with 512MB heap):**
```batch
curl -X POST http://localhost:8080/api/memory/load/200
```

**Sustained Load (10 seconds, 5 objects/sec):**
```batch
curl -X POST "http://localhost:8080/api/memory/sustained?duration=10&rate=5"
```

### Different GC Configurations

**G1GC with custom pause target:**
```batch
java -XX:+UseG1GC -XX:MaxGCPauseMillis=50 -Xms512m -Xmx512m -Dserver.port=8080 -jar target\gc-compare-demo-1.0.0.jar
```

**ZGC with custom concurrency:**
```batch
java -XX:+UseZGC -XX:+ZGenerational -XX:ConcGCThreads=2 -Xms512m -Xmx512m -Dserver.port=8081 -jar target\gc-compare-demo-1.0.0.jar
```

## Troubleshooting

### Prometheus not scraping?

1. Check `host.docker.internal` resolves (Docker Desktop feature)
2. Verify apps are running on correct ports:
   ```batch
   curl http://localhost:8080/actuator/prometheus
   curl http://localhost:8081/actuator/prometheus
   ```
3. Check Prometheus targets: http://localhost:9090/targets

### Grafana dashboard empty?

1. Wait 30-60 seconds for metrics to populate
2. Check Prometheus is receiving data: http://localhost:9090/graph
3. Try running a load test to generate metrics
4. Verify datasource connection in Grafana

### OutOfMemoryError?

1. Reduce load count in test script
2. Increase heap size in batch scripts:
   ```batch
   -Xms1g -Xmx1g
   ```
3. Run lighter load tests:
   ```batch
   curl -X POST http://localhost:8080/api/memory/load/10
   ```

### Docker permission denied?

Ensure Docker Desktop is running and has proper permissions.

### Port already in use?

Kill existing processes:
```batch
# Find process using port 8080
netstat -ano | findstr :8080

# Kill process (replace PID)
taskkill /PID <PID> /F
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
