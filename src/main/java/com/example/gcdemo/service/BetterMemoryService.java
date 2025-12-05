package com.example.gcdemo.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Better approach: Let objects naturally survive or die
 * instead of forcing them into a cache
 */
@Service
public class BetterMemoryService {

    private final Timer allocationTimer;
    private final Counter youngObjectCounter;
    private final Counter survivorObjectCounter;
    
    private static final int OBJECT_SIZE_MB = 10;
    private static final int BYTES_PER_MB = 1024 * 1024;
    
    // Hold references for survivors - but let GC manage them
    private volatile List<byte[]> currentSurvivors = new ArrayList<>();
    private final Object lock = new Object();
    
    public BetterMemoryService(MeterRegistry registry) {
        this.allocationTimer = Timer.builder("gc.demo.better.allocation.time")
                .description("Time spent allocating objects")
                .register(registry);
        
        this.youngObjectCounter = Counter.builder("gc.demo.better.young.objects")
                .description("Number of young objects created")
                .register(registry);
                
        this.survivorObjectCounter = Counter.builder("gc.demo.better.survivor.objects")
                .description("Number of survivor objects")
                .register(registry);
    }
    
    /**
     * BETTER APPROACH: Natural generational pattern
     * 
     * Instead of manually caching objects:
     * 1. Create mostly short-lived objects (die immediately)
     * 2. Create some objects that survive a few GC cycles
     * 3. Let Gen ZGC naturally identify and manage generations
     * 
     * This works WITH Gen ZGC instead of against it!
     */
    public AllocationResult naturalGenerationalWorkload(int shortLivedMB, int survivorsMB) {
        long startTime = System.nanoTime();
        long totalBytes = 0;
        int youngCount = 0;
        int survivorCount = 0;
        
        // 1. SHORT-LIVED: Dies immediately when method returns
        List<byte[]> youngObjects = new ArrayList<>();
        int shortLivedChunks = shortLivedMB / OBJECT_SIZE_MB;
        
        for (int i = 0; i < shortLivedChunks; i++) {
            byte[] chunk = new byte[OBJECT_SIZE_MB * BYTES_PER_MB];
            ThreadLocalRandom.current().nextBytes(chunk);
            youngObjects.add(chunk);
            totalBytes += chunk.length;
            youngCount++;
            youngObjectCounter.increment();
        }
        
        // 2. SURVIVORS: Keep reference for next few requests
        // These naturally become "old generation" for Gen ZGC
        List<byte[]> newSurvivors = new ArrayList<>();
        int survivorChunks = survivorsMB / OBJECT_SIZE_MB;
        
        for (int i = 0; i < survivorChunks; i++) {
            byte[] chunk = new byte[OBJECT_SIZE_MB * BYTES_PER_MB];
            ThreadLocalRandom.current().nextBytes(chunk);
            newSurvivors.add(chunk);
            totalBytes += chunk.length;
            survivorCount++;
            survivorObjectCounter.increment();
        }
        
        // 3. Rotate survivors: keep last 20 requests worth
        // This creates a natural old generation without fighting Gen ZGC
        synchronized (lock) {
            currentSurvivors.addAll(newSurvivors);
            
            // Keep only recent survivors (limit to ~200MB total)
            int maxSurvivors = 20; // ~200MB with 10MB chunks
            if (currentSurvivors.size() > maxSurvivors) {
                // Remove oldest (first items become eligible for collection)
                currentSurvivors = new ArrayList<>(
                    currentSurvivors.subList(currentSurvivors.size() - maxSurvivors, currentSurvivors.size())
                );
            }
        }
        
        long durationNanos = System.nanoTime() - startTime;
        allocationTimer.record(durationNanos, java.util.concurrent.TimeUnit.NANOSECONDS);
        
        // youngObjects die here (garbage)
        // newSurvivors live on in currentSurvivors list
        return new AllocationResult(
            youngCount + survivorCount,
            totalBytes,
            durationNanos,
            youngCount,
            survivorCount
        );
    }
    
    public Map<String, Object> getStats() {
        synchronized (lock) {
            long survivorBytes = (long) currentSurvivors.size() * OBJECT_SIZE_MB * BYTES_PER_MB;
            return Map.of(
                "survivorCount", currentSurvivors.size(),
                "survivorMB", survivorBytes / BYTES_PER_MB,
                "youngObjectsCreated", (long) youngObjectCounter.count(),
                "survivorObjectsCreated", (long) survivorObjectCounter.count()
            );
        }
    }
    
    public void clearSurvivors() {
        synchronized (lock) {
            currentSurvivors.clear();
        }
    }
    
    public record AllocationResult(
        int objectCount,
        long totalBytes,
        long durationNanos,
        int youngObjects,
        int survivorObjects
    ) {
        public double durationMs() {
            return durationNanos / 1_000_000.0;
        }
        
        public double totalMB() {
            return totalBytes / (double) (1024 * 1024);
        }
    }
}
