package com.example.gcdemo.controller;

import com.example.gcdemo.service.EnhancedMemoryService;
import com.example.gcdemo.service.EnhancedMemoryService.AllocationResult;
import io.micrometer.core.annotation.Timed;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/enhanced")
public class EnhancedMemoryController {

    private final EnhancedMemoryService enhancedMemoryService;

    public EnhancedMemoryController(EnhancedMemoryService enhancedMemoryService) {
        this.enhancedMemoryService = enhancedMemoryService;
    }

    /**
     * Mixed workload endpoint
     * POST /api/enhanced/mixed/{shortLivedMB}/{longLivedMB}
     * 
     * Example: POST /api/enhanced/mixed/80/20
     * - 80MB dies immediately (young generation)
     * - 20MB stays in cache (old generation)
     * 
     * This demonstrates Gen ZGC's advantage:
     * - Gen ZGC collects young cheaply and frequently
     * - NonGen ZGC must scan everything every time
     */
    @PostMapping("/mixed/{shortLivedMB}/{longLivedMB}")
    @Timed(value = "gc.demo.enhanced.mixed.request", description = "Time for enhanced mixed workload")
    public Map<String, Object> mixedLoad(
            @PathVariable int shortLivedMB,
            @PathVariable int longLivedMB) {
        
        AllocationResult result = enhancedMemoryService.mixedWorkload(shortLivedMB, longLivedMB);
        
        return Map.of(
            "status", "completed",
            "objectsAllocated", result.objectCount(),
            "youngObjects", result.youngObjects(),
            "oldObjects", result.oldObjects(),
            "totalMB", String.format("%.2f", result.totalMB()),
            "durationMs", String.format("%.2f", result.durationMs())
        );
    }

    /**
     * Get cache statistics
     * GET /api/enhanced/cache-stats
     */
    @GetMapping("/cache-stats")
    public Map<String, Object> cacheStats() {
        return enhancedMemoryService.getCacheStats();
    }

    /**
     * Clear cache
     * POST /api/enhanced/clear-cache
     */
    @PostMapping("/clear-cache")
    public Map<String, Object> clearCache() {
        enhancedMemoryService.clearCache();
        return Map.of(
            "status", "cache cleared",
            "currentStats", enhancedMemoryService.getCacheStats()
        );
    }
}
