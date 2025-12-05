package com.example.gcdemo.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class EnhancedMemoryService {

    private final Timer allocationTimer;
    private final Counter youngObjectCounter;
    private final Counter oldObjectCounter;
    
    private static final int OBJECT_SIZE_MB = 10;
    private static final int BYTES_PER_MB = 1024 * 1024;
    
    // Persistent cache for long-lived objects
    private final Map<String, LongLivedObject> longLivedCache = new ConcurrentHashMap<>();
    private final AtomicLong cacheKeyCounter = new AtomicLong(0);
    private final AtomicLong totalCachedBytes = new AtomicLong(0);
    
    // With 512MB heap, limit cache to ~200MB (40% of heap)
    // With 1GB heap, limit cache to ~400MB (40% of heap)
    private static final long MAX_CACHE_MB = 200;
    private static final long MAX_CACHE_BYTES = MAX_CACHE_MB * BYTES_PER_MB;
    
    public EnhancedMemoryService(MeterRegistry registry) {
        this.allocationTimer = Timer.builder("gc.demo.enhanced.allocation.time")
                .description("Time spent allocating objects (enhanced)")
                .register(registry);
        
        this.youngObjectCounter = Counter.builder("gc.demo.enhanced.young.objects")
                .description("Number of short-lived objects created")
                .register(registry);
                
        this.oldObjectCounter = Counter.builder("gc.demo.enhanced.old.objects")
                .description("Number of long-lived objects created")
                .register(registry);
        
        Gauge.builder("gc.demo.enhanced.cache.size", longLivedCache, Map::size)
                .description("Number of long-lived objects in cache")
                .register(registry);
                
        Gauge.builder("gc.demo.enhanced.cache.bytes", totalCachedBytes, AtomicLong::get)
                .description("Total bytes in long-lived cache")
                .register(registry);
    }
    
    /**
     * Mixed workload demonstrating Weak Generational Hypothesis
     */
    public AllocationResult mixedWorkload(int shortLivedMB, int longLivedMB) {
        long startTime = System.nanoTime();
        long totalBytes = 0;
        int youngObjects = 0;
        int oldObjects = 0;
        
        // SHORT-LIVED objects (dies immediately)
        List<byte[]> tempObjects = new ArrayList<>();
        int shortLivedChunks = shortLivedMB / OBJECT_SIZE_MB;
        
        for (int i = 0; i < shortLivedChunks; i++) {
            byte[] chunk = new byte[OBJECT_SIZE_MB * BYTES_PER_MB];
            ThreadLocalRandom.current().nextBytes(chunk);
            tempObjects.add(chunk);
            totalBytes += chunk.length;
            youngObjects++;
            youngObjectCounter.increment();
        }
        
        // LONG-LIVED objects (survives in cache)
        int longLivedChunks = longLivedMB / OBJECT_SIZE_MB;
        
        for (int i = 0; i < longLivedChunks; i++) {
            // Evict oldest if cache exceeds size limit
            if (totalCachedBytes.get() + (OBJECT_SIZE_MB * BYTES_PER_MB) > MAX_CACHE_BYTES) {
                evictOldestEntries(longLivedChunks);
            }
            
            String key = "obj_" + cacheKeyCounter.incrementAndGet();
            byte[] chunk = new byte[OBJECT_SIZE_MB * BYTES_PER_MB];
            ThreadLocalRandom.current().nextBytes(chunk);
            
            LongLivedObject obj = new LongLivedObject(chunk, System.currentTimeMillis());
            longLivedCache.put(key, obj);
            
            totalBytes += chunk.length;
            totalCachedBytes.addAndGet(chunk.length);
            oldObjects++;
            oldObjectCounter.increment();
        }
        
        long durationNanos = System.nanoTime() - startTime;
        allocationTimer.record(durationNanos, java.util.concurrent.TimeUnit.NANOSECONDS);
        
        return new AllocationResult(
            youngObjects + oldObjects, 
            totalBytes, 
            durationNanos,
            youngObjects,
            oldObjects
        );
    }
    
    private void evictOldestEntries(int count) {
        longLivedCache.entrySet().stream()
            .sorted((e1, e2) -> Long.compare(e1.getValue().createdAt, e2.getValue().createdAt))
            .limit(count)
            .forEach(entry -> {
                LongLivedObject removed = longLivedCache.remove(entry.getKey());
                if (removed != null) {
                    totalCachedBytes.addAndGet(-removed.data.length);
                }
            });
    }
    
    public Map<String, Object> getCacheStats() {
        return Map.of(
            "cacheSize", longLivedCache.size(),
            "cachedMB", String.format("%.2f", totalCachedBytes.get() / (double) BYTES_PER_MB),
            "maxCacheMB", MAX_CACHE_MB,
            "youngObjectsCreated", (long) youngObjectCounter.count(),
            "oldObjectsCreated", (long) oldObjectCounter.count(),
            "youngToOldRatio", String.format("%.2f", youngObjectCounter.count() / Math.max(1, oldObjectCounter.count()))
        );
    }
    
    public void clearCache() {
        longLivedCache.clear();
        totalCachedBytes.set(0);
    }
    
    private static class LongLivedObject {
        final byte[] data;
        final long createdAt;
        
        LongLivedObject(byte[] data, long createdAt) {
            this.data = data;
            this.createdAt = createdAt;
        }
    }
    
    public record AllocationResult(
        int objectCount, 
        long totalBytes, 
        long durationNanos,
        int youngObjects,
        int oldObjects
    ) {
        public double durationMs() {
            return durationNanos / 1_000_000.0;
        }
        
        public double totalMB() {
            return totalBytes / (double) BYTES_PER_MB;
        }
    }
}