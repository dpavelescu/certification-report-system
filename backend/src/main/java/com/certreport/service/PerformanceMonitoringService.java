package com.certreport.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for monitoring and tracking performance metrics during report generation
 * @deprecated Use ActuatorPerformanceMonitor instead for comprehensive monitoring
 */
@Deprecated
// @Service  // Disabled - use ActuatorPerformanceMonitor instead
public class PerformanceMonitoringService {private static final Logger logger = LoggerFactory.getLogger(PerformanceMonitoringService.class);

    @Autowired
    private Counter reportGenerationCounter;

    @Autowired
    private Counter reportFailureCounter;

    @Autowired
    private Timer reportGenerationTimer;
    
    // Simple counters for tracking active reports
    private final AtomicInteger activeReports = new AtomicInteger(0);
    private final AtomicInteger totalReports = new AtomicInteger(0);
    private final AtomicInteger failedReports = new AtomicInteger(0);/**
     * Start monitoring a report generation process
     * @param reportId The ID of the report being generated
     * @return Timer sample for measuring duration
     */
    public Timer.Sample startReportGeneration(String reportId) {
        logger.info("Starting performance monitoring for report: {}", reportId);
        activeReports.incrementAndGet();
        
        // Log memory usage at start
        logMemoryUsage("Report generation started for: " + reportId);
        
        return Timer.start();
    }    /**
     * Complete monitoring for a successful report generation
     * @param sample Timer sample from start method
     * @param reportId The ID of the report
     */
    public void completeReportGeneration(Timer.Sample sample, String reportId) {
        // Stop timer and record duration
        long durationNanos = sample.stop(reportGenerationTimer);
        Duration duration = Duration.ofNanos(durationNanos);
        
        // Update counters
        activeReports.decrementAndGet();
        totalReports.incrementAndGet();
        reportGenerationCounter.increment();
        
        // Log completion metrics
        logger.info("Report generation completed for: {} in {} ms", 
                   reportId, duration.toMillis());
        
        // Check if generation time exceeds 5 seconds (requirement)
        if (duration.toSeconds() > 5) {
            logger.warn("Report generation for {} took {} seconds, exceeding 5 second target", 
                       reportId, duration.toSeconds());
        }
        
        logMemoryUsage("Report generation completed for: " + reportId);
    }    /**
     * Record a failed report generation
     * @param sample Timer sample from start method
     * @param reportId The ID of the report
     * @param error The error that occurred
     */
    public void recordReportGenerationFailure(Timer.Sample sample, String reportId, Exception error) {
        // Stop timer and record duration even for failures
        long durationNanos = sample.stop(reportGenerationTimer);
        Duration duration = Duration.ofNanos(durationNanos);
        
        // Update counters
        activeReports.decrementAndGet();
        failedReports.incrementAndGet();
        reportFailureCounter.increment();
        
        // Log failure metrics
        logger.error("Report generation failed for: {} after {} ms. Error: {}", 
                    reportId, duration.toMillis(), error.getMessage());
        
        logMemoryUsage("Report generation failed for: " + reportId);
    }    /**
     * Get current performance metrics
     */
    public PerformanceMetrics getCurrentMetrics() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        return new PerformanceMetrics(
            activeReports.get(),
            totalReports.get(),
            failedReports.get(),
            usedMemory,
            totalMemory,
            calculateSuccessRate()
        );
    }

    /**
     * Log current memory usage
     */
    private void logMemoryUsage(String context) {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        logger.info("Memory Usage - {}: Used: {} MB, Free: {} MB, Total: {} MB, Max: {} MB",
                   context,
                   usedMemory / (1024 * 1024),
                   freeMemory / (1024 * 1024),
                   totalMemory / (1024 * 1024),
                   maxMemory / (1024 * 1024));
    }    /**
     * Calculate success rate percentage
     */
    private double calculateSuccessRate() {
        int total = totalReports.get();
        int failed = failedReports.get();
        
        if (total == 0) {
            return 100.0; // No reports generated yet
        }
        
        return ((double) (total - failed) / total) * 100.0;
    }

    /**
     * Data class for performance metrics
     */
    public static class PerformanceMetrics {
        private final int activeReports;
        private final int totalReports;
        private final int failedReports;
        private final long usedMemory;
        private final long totalMemory;
        private final double successRate;

        public PerformanceMetrics(int activeReports, int totalReports, int failedReports, 
                                 long usedMemory, long totalMemory, double successRate) {
            this.activeReports = activeReports;
            this.totalReports = totalReports;
            this.failedReports = failedReports;
            this.usedMemory = usedMemory;
            this.totalMemory = totalMemory;
            this.successRate = successRate;
        }

        // Getters
        public int getActiveReports() { return activeReports; }
        public int getTotalReports() { return totalReports; }
        public int getFailedReports() { return failedReports; }
        public long getUsedMemory() { return usedMemory; }
        public long getTotalMemory() { return totalMemory; }
        public double getSuccessRate() { return successRate; }
    }
}
