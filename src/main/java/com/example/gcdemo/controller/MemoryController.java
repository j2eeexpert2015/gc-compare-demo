package com.example.gcdemo.controller;

import com.example.gcdemo.service.MemoryLoadService;
import com.example.gcdemo.service.MemoryLoadService.AllocationResult;
import io.micrometer.core.annotation.Timed;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/memory")
public class MemoryController {

    private final MemoryLoadService memoryLoadService;
    
    @Value("${spring.application.name:gc-demo}")
    private String appName;

    public MemoryController(MemoryLoadService memoryLoadService) {
        this.memoryLoadService = memoryLoadService;
    }

    /**
     * Health check / info endpoint
     */
    @GetMapping("/info")
    public Map<String, Object> info() {
        Map<String, Object> info = new HashMap<>();
        info.put("application", appName);
        info.put("javaVersion", System.getProperty("java.version"));
        info.put("jvmName", System.getProperty("java.vm.name"));
        
        // Get active GC
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            info.put("gc_" + gc.getName().replace(" ", "_"), Map.of(
                "collectionCount", gc.getCollectionCount(),
                "collectionTimeMs", gc.getCollectionTime()
            ));
        }
        
        // Memory info
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        info.put("heapUsed", memory.getHeapMemoryUsage().getUsed() / (1024 * 1024) + " MB");
        info.put("heapMax", memory.getHeapMemoryUsage().getMax() / (1024 * 1024) + " MB");
        
        return info;
    }

    /**
     * Burst allocation - POST /api/memory/load/{count}
     * Creates count * 10MB of garbage objects.
     * Similar to Vishalendu's endpoint.
     */
    @PostMapping("/load/{count}")
    @Timed(value = "gc.demo.load.request", description = "Time for load request")
    public Map<String, Object> load(@PathVariable int count) {
        AllocationResult result = memoryLoadService.allocateAndDiscard(count);
        
        return Map.of(
            "status", "completed",
            "objectsAllocated", result.objectCount(),
            "totalMB", String.format("%.2f", result.totalMB()),
            "durationMs", String.format("%.2f", result.durationMs())
        );
    }

    /**
     * Sustained load - POST /api/memory/sustained?duration=10&rate=5
     * Allocates objects continuously for the given duration.
     */
    @PostMapping("/sustained")
    @Timed(value = "gc.demo.sustained.request", description = "Time for sustained load request")
    public Map<String, Object> sustainedLoad(
            @RequestParam(defaultValue = "10") int duration,
            @RequestParam(defaultValue = "5") int rate) {
        
        AllocationResult result = memoryLoadService.sustainedLoad(duration, rate);
        
        return Map.of(
            "status", "completed",
            "durationSeconds", duration,
            "objectsAllocated", result.objectCount(),
            "totalMB", String.format("%.2f", result.totalMB()),
            "actualDurationMs", String.format("%.2f", result.durationMs())
        );
    }

    /**
     * Quick GC stats endpoint
     */
    @GetMapping("/gc-stats")
    public Map<String, Object> gcStats() {
        Map<String, Object> stats = new HashMap<>();
        
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            stats.put(gc.getName(), Map.of(
                "collectionCount", gc.getCollectionCount(),
                "collectionTimeMs", gc.getCollectionTime(),
                "memoryPools", gc.getMemoryPoolNames()
            ));
        }
        
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        stats.put("heap", Map.of(
            "usedMB", memory.getHeapMemoryUsage().getUsed() / (1024 * 1024),
            "committedMB", memory.getHeapMemoryUsage().getCommitted() / (1024 * 1024),
            "maxMB", memory.getHeapMemoryUsage().getMax() / (1024 * 1024)
        ));
        
        return stats;
    }
}
