package com.example.gcdemo.controller;

import com.example.gcdemo.service.BetterMemoryService;
import com.example.gcdemo.service.BetterMemoryService.AllocationResult;
import io.micrometer.core.annotation.Timed;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/better")
public class BetterMemoryController {

    private final BetterMemoryService betterMemoryService;

    public BetterMemoryController(BetterMemoryService betterMemoryService) {
        this.betterMemoryService = betterMemoryService;
    }

    /**
     * Natural generational workload
     * POST /api/better/natural/{shortLivedMB}/{survivorsMB}
     * 
     * Example: POST /api/better/natural/80/20
     * 
     * This works WITH Gen ZGC instead of against it:
     * - Short-lived objects die naturally
     * - Survivors are kept alive but rotate out
     * - Gen ZGC identifies patterns and optimizes
     */
    @PostMapping("/natural/{shortLivedMB}/{survivorsMB}")
    @Timed(value = "gc.demo.better.natural.request", description = "Natural generational workload")
    public Map<String, Object> naturalLoad(
            @PathVariable int shortLivedMB,
            @PathVariable int survivorsMB) {
        
        AllocationResult result = betterMemoryService.naturalGenerationalWorkload(shortLivedMB, survivorsMB);
        
        return Map.of(
            "status", "completed",
            "objectsAllocated", result.objectCount(),
            "youngObjects", result.youngObjects(),
            "survivorObjects", result.survivorObjects(),
            "totalMB", String.format("%.2f", result.totalMB()),
            "durationMs", String.format("%.2f", result.durationMs())
        );
    }

    /**
     * Get survivor statistics
     */
    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return betterMemoryService.getStats();
    }

    /**
     * Clear survivors
     */
    @PostMapping("/clear")
    public Map<String, Object> clear() {
        betterMemoryService.clearSurvivors();
        return Map.of(
            "status", "cleared",
            "currentStats", betterMemoryService.getStats()
        );
    }
}
