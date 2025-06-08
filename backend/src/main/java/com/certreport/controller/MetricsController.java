package com.certreport.controller;

import com.certreport.service.PerformanceMonitoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for exposing performance metrics and monitoring data
 */
@RestController
@RequestMapping("/api/metrics")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class MetricsController {

    @Autowired
    private PerformanceMonitoringService performanceMonitoringService;

    /**
     * Get current performance metrics
     */
    @GetMapping("/performance")
    public ResponseEntity<PerformanceMonitoringService.PerformanceMetrics> getPerformanceMetrics() {
        PerformanceMonitoringService.PerformanceMetrics metrics = 
            performanceMonitoringService.getCurrentMetrics();
        return ResponseEntity.ok(metrics);
    }

    /**
     * Get health status of the report generation system
     */
    @GetMapping("/health")
    public ResponseEntity<HealthStatus> getHealthStatus() {
        PerformanceMonitoringService.PerformanceMetrics metrics = 
            performanceMonitoringService.getCurrentMetrics();
            
        // Calculate memory usage percentage
        double memoryUsagePercent = (double) metrics.getUsedMemory() / metrics.getTotalMemory() * 100;
        
        // Determine health status based on metrics
        String status = "healthy";
        String message = "System is operating normally";
        
        if (metrics.getActiveReports() > 10) {
            status = "warning";
            message = "High number of active report generations";
        } else if (memoryUsagePercent > 90) {
            status = "critical";
            message = "High memory usage detected";
        } else if (metrics.getSuccessRate() < 90) {
            status = "warning";
            message = "Report generation success rate below 90%";
        }
        
        return ResponseEntity.ok(new HealthStatus(status, message, metrics));
    }
    
    /**
     * Health status response class
     */
    public static class HealthStatus {
        private final String status;
        private final String message;
        private final PerformanceMonitoringService.PerformanceMetrics metrics;
        
        public HealthStatus(String status, String message, 
                           PerformanceMonitoringService.PerformanceMetrics metrics) {
            this.status = status;
            this.message = message;
            this.metrics = metrics;
        }
        
        public String getStatus() { return status; }
        public String getMessage() { return message; }
        public PerformanceMonitoringService.PerformanceMetrics getMetrics() { return metrics; }
    }
}
