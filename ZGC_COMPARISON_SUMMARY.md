# ZGC Generational vs Non-Generational Comparison - Summary

## Files Created

### 1. scripts/run-zgc-nongen.bat
- Runs ZGC in Non-Generational mode on port 8082
- Heap options: 2GB (recommended) or 4GB (production-like)

### 2. docker/prometheus.yml (UPDATED)
- Now scrapes 3 instances:
  - 8080: G1GC
  - 8081: ZGC Generational
  - 8082: ZGC Non-Generational

### 3. docker/grafana/provisioning/dashboards/zgc-comparison.json
- New dashboard comparing ZGC Gen vs ZGC NonGen
- Color scheme: Green (Gen) vs Blue (NonGen)
- Same metrics as G1GC vs ZGC dashboard

### 4. README.md (UPDATED)
- Added complete ZGC comparison section with all commands
- Updated project structure
- Updated troubleshooting to include port 8082
- Added command to cheat sheet

## How to Use

1. Restart Docker to pick up new Prometheus config:
   ```batch
   cd docker
   docker-compose down
   docker-compose up -d
   ```

2. Start both ZGC instances:
   ```batch
   # Terminal 1 - ZGC Generational (2GB recommended)
   java -XX:+UseZGC -XX:+ZGenerational -Xms2g -Xmx2g -Dserver.port=8081 -Dspring.application.name=zgc-gen-demo -jar target\gc-compare-demo-1.0.0.jar
   
   # Terminal 1 - ZGC Generational (4GB production-like)
   java -XX:+UseZGC -XX:+ZGenerational -Xms4g -Xmx4g -Dserver.port=8081 -Dspring.application.name=zgc-gen-demo -jar target\gc-compare-demo-1.0.0.jar
   
   # Terminal 2 - ZGC Non-Generational (2GB recommended)
   java -XX:+UseZGC -Xms2g -Xmx2g -Dserver.port=8082 -Dspring.application.name=zgc-nongen-demo -jar target\gc-compare-demo-1.0.0.jar
   
   # Terminal 2 - ZGC Non-Generational (4GB production-like)
   java -XX:+UseZGC -Xms4g -Xmx4g -Dserver.port=8082 -Dspring.application.name=zgc-nongen-demo -jar target\gc-compare-demo-1.0.0.jar
   ```

3. Run load test comparing both ZGCs:
   ```batch
   for /L %i in (1,1,30) do @(curl -s -X POST http://localhost:8081/api/memory/load/20 >nul & curl -s -X POST http://localhost:8082/api/memory/load/20 >nul & echo Iteration %i & timeout /t 3 /nobreak >nul)
   ```

4. View dashboard: http://localhost:3000 → "ZGC: Generational vs Non-Generational Comparison"

## Expected Differences

| Metric | ZGC Generational | ZGC Non-Generational |
|--------|-----------------|---------------------|
| Pause Time | <1ms | <1ms (similar) |
| Throughput | Slightly better | Slightly lower |
| CPU Usage | Lower | Slightly higher |
| Young Object Handling | More efficient | Treats all objects same |

## Your Existing Dashboards

✅ NOT IMPACTED - All existing dashboards remain unchanged:
- G1GC vs ZGC Comparison (ports 8080, 8081)
- G1GC Detailed Metrics
- ZGC Detailed Metrics

## Next Steps

You can now run:
1. G1GC vs ZGC Generational (original comparison)
2. ZGC Generational vs Non-Generational (new comparison)
3. All three together for a complete picture
