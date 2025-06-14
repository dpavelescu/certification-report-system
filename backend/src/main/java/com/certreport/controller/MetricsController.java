package com.certreport.controller;

import com.certreport.service.ActuatorPerformanceMonitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for exposing performance metrics and monitoring data
 */
@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

    @Autowired
    private ActuatorPerformanceMonitor actuatorPerformanceMonitor;    /**
     * Get current performance metrics
     */
    @GetMapping("/performance")
    public ResponseEntity<ActuatorPerformanceMonitor.MemoryMetrics> getPerformanceMetrics() {
        ActuatorPerformanceMonitor.MemoryMetrics metrics = 
            actuatorPerformanceMonitor.getDetailedMemoryMetrics();
        return ResponseEntity.ok(metrics);
    }    /**
     * Get health status of the report generation system
     */
    @GetMapping("/health")
    public ResponseEntity<HealthStatus> getHealthStatus() {
        ActuatorPerformanceMonitor.MemoryMetrics memoryMetrics = 
            actuatorPerformanceMonitor.getDetailedMemoryMetrics();
        ActuatorPerformanceMonitor.DatabaseMetrics dbMetrics = 
            actuatorPerformanceMonitor.getDatabaseMetrics();
            
        // Calculate memory usage percentage (heap used vs heap max)
        double memoryUsagePercent = memoryMetrics.heapMaxMB > 0 ? 
            (double) memoryMetrics.heapUsedMB / memoryMetrics.heapMaxMB * 100 : 0;
        
        // Determine health status based on metrics
        String status = "healthy";
        String message = "System is operating normally";
        
        if (memoryUsagePercent > 90) {
            status = "critical";
            message = "High memory usage detected";
        } else if (memoryUsagePercent > 75) {
            status = "warning";
            message = "Memory usage is elevated";
        }
        
        return ResponseEntity.ok(new HealthStatus(status, message, memoryMetrics, dbMetrics));
    }
      /**
     * Health status response class
     */
    public static class HealthStatus {
        private final String status;
        private final String message;
        private final ActuatorPerformanceMonitor.MemoryMetrics memoryMetrics;
        private final ActuatorPerformanceMonitor.DatabaseMetrics databaseMetrics;
        
        public HealthStatus(String status, String message, 
                           ActuatorPerformanceMonitor.MemoryMetrics memoryMetrics,
                           ActuatorPerformanceMonitor.DatabaseMetrics databaseMetrics) {
            this.status = status;
            this.message = message;
            this.memoryMetrics = memoryMetrics;
            this.databaseMetrics = databaseMetrics;
        }
        
        public String getStatus() { return status; }
        public String getMessage() { return message; }
        public ActuatorPerformanceMonitor.MemoryMetrics getMemoryMetrics() { return memoryMetrics; }
        public ActuatorPerformanceMonitor.DatabaseMetrics getDatabaseMetrics() { return databaseMetrics; }
    }
}
