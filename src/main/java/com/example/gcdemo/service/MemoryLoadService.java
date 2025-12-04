package com.example.gcdemo.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class MemoryLoadService {

    private final Timer allocationTimer;
    
    // Size of each allocated object (10 MB)
    private static final int OBJECT_SIZE_MB = 10;
    private static final int BYTES_PER_MB = 1024 * 1024;
    
    public MemoryLoadService(MeterRegistry registry) {
        // Custom timer to track allocation duration
        this.allocationTimer = Timer.builder("gc.demo.allocation.time")
                .description("Time spent allocating objects")
                .register(registry);
    }
    
    /**
     * Allocates temporary objects to create GC pressure.
     * Similar to Vishalendu's approach: creates count * 10MB objects
     * that become garbage immediately after method returns.
     * 
     * @param count number of 10MB objects to allocate
     * @return info about what was allocated
     */
    public AllocationResult allocateAndDiscard(int count) {
        long startTime = System.nanoTime();
        long totalBytes = 0;
        
        // Allocate objects - they'll become garbage after this method
        List<byte[]> tempObjects = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            byte[] chunk = new byte[OBJECT_SIZE_MB * BYTES_PER_MB];
            // Fill with random data to prevent JVM optimizations
            ThreadLocalRandom.current().nextBytes(chunk);
            tempObjects.add(chunk);
            totalBytes += chunk.length;
        }
        
        long durationNanos = System.nanoTime() - startTime;
        
        // Record to Micrometer
        allocationTimer.record(durationNanos, java.util.concurrent.TimeUnit.NANOSECONDS);
        
        // Objects in tempObjects become garbage when method returns
        return new AllocationResult(count, totalBytes, durationNanos);
    }
    
    /**
     * Sustained allocation - keeps allocating for a duration.
     * Good for creating sustained GC pressure.
     * 
     * @param durationSeconds how long to keep allocating
     * @param objectsPerSecond rate of allocation
     * @return summary of allocations
     */
    public AllocationResult sustainedLoad(int durationSeconds, int objectsPerSecond) {
        long startTime = System.currentTimeMillis();
        long endTime = startTime + (durationSeconds * 1000L);
        long totalBytes = 0;
        int totalObjects = 0;
        
        long intervalMs = 1000 / objectsPerSecond;
        
        while (System.currentTimeMillis() < endTime) {
            byte[] chunk = new byte[OBJECT_SIZE_MB * BYTES_PER_MB];
            ThreadLocalRandom.current().nextBytes(chunk);
            totalBytes += chunk.length;
            totalObjects++;
            
            try {
                Thread.sleep(intervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        long durationNanos = (System.currentTimeMillis() - startTime) * 1_000_000L;
        return new AllocationResult(totalObjects, totalBytes, durationNanos);
    }
    
    public record AllocationResult(int objectCount, long totalBytes, long durationNanos) {
        public double durationMs() {
            return durationNanos / 1_000_000.0;
        }
        
        public double totalMB() {
            return totalBytes / (double) BYTES_PER_MB;
        }
    }
}
