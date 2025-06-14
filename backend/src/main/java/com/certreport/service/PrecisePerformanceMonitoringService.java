package com.certreport.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.time.Instant;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Enhanced performance monitoring service that provides precise measurements
 * for report generation time, memory footprint, and page counting
 */
@Service
public class PrecisePerformanceMonitoringService {
    
    private static final Logger logger = LoggerFactory.getLogger(PrecisePerformanceMonitoringService.class);
    
    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private final Map<String, ReportMetrics> activeReportMetrics = new ConcurrentHashMap<>();
    
    /**
     * Start precise monitoring for a report generation
     */
    public void startPreciseMonitoring(String reportId) {
        // Force garbage collection before baseline measurement
        System.gc();
        try {
            Thread.sleep(100); // Give GC time to complete
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Take baseline measurements
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
        
        ReportMetrics metrics = new ReportMetrics();
        metrics.reportId = reportId;
        metrics.startTime = Instant.now();
        metrics.baselineHeapMemory = heapUsage.getUsed();
        metrics.baselineNonHeapMemory = nonHeapUsage.getUsed();
        metrics.baselineTotalMemory = metrics.baselineHeapMemory + metrics.baselineNonHeapMemory;
        metrics.memorySnapshots = new ArrayList<>();
        
        // Take initial snapshot
        metrics.memorySnapshots.add(new MemorySnapshot(
            Instant.now(), 
            "Baseline", 
            heapUsage.getUsed(), 
            nonHeapUsage.getUsed()
        ));
        
        activeReportMetrics.put(reportId, metrics);
        
        logger.info("Started precise monitoring for report {}: Baseline memory: {} MB", 
                   reportId, metrics.baselineTotalMemory / (1024 * 1024));
    }
    
    /**
     * Record a memory snapshot during report generation
     */
    public void recordMemorySnapshot(String reportId, String stage) {
        ReportMetrics metrics = activeReportMetrics.get(reportId);
        if (metrics == null) {
            logger.warn("No metrics found for report {}", reportId);
            return;
        }
        
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
        
        MemorySnapshot snapshot = new MemorySnapshot(
            Instant.now(),
            stage,
            heapUsage.getUsed(),
            nonHeapUsage.getUsed()
        );
        
        metrics.memorySnapshots.add(snapshot);
        
        long currentTotal = snapshot.heapUsed + snapshot.nonHeapUsed;
        long memoryDelta = currentTotal - metrics.baselineTotalMemory;
        
        logger.info("Memory snapshot for report {} at {}: Current: {} MB, Delta: {} MB", 
                   reportId, stage, 
                   currentTotal / (1024 * 1024), 
                   memoryDelta / (1024 * 1024));
    }
    
    /**
     * Start PDF generation timing (separate from overall report timing)
     */
    public void startPdfGeneration(String reportId) {
        ReportMetrics metrics = activeReportMetrics.get(reportId);
        if (metrics != null) {
            metrics.pdfGenerationStartTime = Instant.now();
            recordMemorySnapshot(reportId, "PDF Generation Start");
        }
    }
    
    /**
     * Complete PDF generation and record actual page count
     */
    public void completePdfGeneration(String reportId, int actualPageCount, long fileSizeBytes) {
        ReportMetrics metrics = activeReportMetrics.get(reportId);
        if (metrics != null) {
            metrics.pdfGenerationEndTime = Instant.now();
            metrics.actualPageCount = actualPageCount;
            metrics.fileSizeBytes = fileSizeBytes;
            recordMemorySnapshot(reportId, "PDF Generation Complete");
            
            long pdfGenerationMs = Duration.between(metrics.pdfGenerationStartTime, metrics.pdfGenerationEndTime).toMillis();
            logger.info("PDF generation for report {} completed: {} pages, {} KB, {} ms", 
                       reportId, actualPageCount, fileSizeBytes / 1024, pdfGenerationMs);
        }
    }
    
    /**
     * Complete monitoring and return detailed metrics
     */
    public DetailedPerformanceReport completeMonitoring(String reportId) {
        ReportMetrics metrics = activeReportMetrics.remove(reportId);
        if (metrics == null) {
            logger.warn("No metrics found for report {}", reportId);
            return null;
        }
        
        metrics.endTime = Instant.now();
        recordMemorySnapshot(reportId, "Report Complete");
        
        // Calculate final metrics
        long totalDurationMs = Duration.between(metrics.startTime, metrics.endTime).toMillis();
        long pdfGenerationMs = metrics.pdfGenerationStartTime != null && metrics.pdfGenerationEndTime != null
            ? Duration.between(metrics.pdfGenerationStartTime, metrics.pdfGenerationEndTime).toMillis()
            : 0;
        
        // Calculate memory metrics
        MemorySnapshot lastSnapshot = metrics.memorySnapshots.get(metrics.memorySnapshots.size() - 1);
        long finalTotalMemory = lastSnapshot.heapUsed + lastSnapshot.nonHeapUsed;
        long memoryDelta = finalTotalMemory - metrics.baselineTotalMemory;
        
        // Find peak memory usage
        long peakMemoryUsage = metrics.memorySnapshots.stream()
            .mapToLong(snapshot -> snapshot.heapUsed + snapshot.nonHeapUsed)
            .max()
            .orElse(metrics.baselineTotalMemory);
        long peakMemoryDelta = peakMemoryUsage - metrics.baselineTotalMemory;
        
        DetailedPerformanceReport report = new DetailedPerformanceReport();
        report.reportId = reportId;
        report.totalDurationMs = totalDurationMs;
        report.pdfGenerationDurationMs = pdfGenerationMs;
        report.actualPageCount = metrics.actualPageCount;
        report.fileSizeBytes = metrics.fileSizeBytes;
        report.baselineMemoryMB = metrics.baselineTotalMemory / (1024.0 * 1024.0);
        report.finalMemoryMB = finalTotalMemory / (1024.0 * 1024.0);
        report.memoryDeltaMB = memoryDelta / (1024.0 * 1024.0);
        report.peakMemoryDeltaMB = peakMemoryDelta / (1024.0 * 1024.0);
        report.memorySnapshots = new ArrayList<>(metrics.memorySnapshots);
        
        // Calculate throughput metrics
        if (metrics.actualPageCount > 0 && pdfGenerationMs > 0) {
            report.pagesPerSecond = (double) metrics.actualPageCount / (pdfGenerationMs / 1000.0);
        }
        if (metrics.fileSizeBytes > 0 && pdfGenerationMs > 0) {
            report.mbPerSecond = (metrics.fileSizeBytes / (1024.0 * 1024.0)) / (pdfGenerationMs / 1000.0);
        }
        
        logDetailedReport(report);
        return report;
    }
    
    private void logDetailedReport(DetailedPerformanceReport report) {
        logger.info("=== DETAILED PERFORMANCE REPORT FOR {} ===", report.reportId);
        logger.info("Total Duration: {} ms ({} seconds)", report.totalDurationMs, report.totalDurationMs / 1000.0);
        logger.info("PDF Generation Duration: {} ms ({} seconds)", report.pdfGenerationDurationMs, report.pdfGenerationDurationMs / 1000.0);
        logger.info("Actual Page Count: {} pages", report.actualPageCount);
        logger.info("File Size: {} KB", report.fileSizeBytes / 1024);        logger.info("Memory Baseline: {} MB", String.format("%.2f", report.baselineMemoryMB));
        logger.info("Memory Final: {} MB", String.format("%.2f", report.finalMemoryMB));
        logger.info("Memory Delta: {} MB", String.format("%.2f", report.memoryDeltaMB));
        logger.info("Peak Memory Delta: {} MB", String.format("%.2f", report.peakMemoryDeltaMB));
        if (report.pagesPerSecond > 0) {            logger.info("Throughput: {} pages/second", String.format("%.2f", report.pagesPerSecond));
        }
        if (report.mbPerSecond > 0) {
            logger.info("Processing Speed: {} MB/second", String.format("%.2f", report.mbPerSecond));
        }
        logger.info("Memory Progression:");
        for (MemorySnapshot snapshot : report.memorySnapshots) {
            long totalMemory = snapshot.heapUsed + snapshot.nonHeapUsed;
            logger.info("  {}: {} MB", snapshot.stage, String.format("%.2f", totalMemory / (1024.0 * 1024.0)));
        }
        logger.info("=== END PERFORMANCE REPORT ===");
    }
      /**
     * Internal class to track metrics during report generation
     */
    private static class ReportMetrics {
        @SuppressWarnings("unused")
        String reportId;  // Used for tracking but not currently analyzed
        Instant startTime;
        Instant endTime;
        Instant pdfGenerationStartTime;
        Instant pdfGenerationEndTime;
        long baselineHeapMemory;
        long baselineNonHeapMemory;
        long baselineTotalMemory;
        int actualPageCount;
        long fileSizeBytes;
        List<MemorySnapshot> memorySnapshots;
    }
    
    /**
     * Memory snapshot at a specific point in time
     */
    public static class MemorySnapshot {
        public final Instant timestamp;
        public final String stage;
        public final long heapUsed;
        public final long nonHeapUsed;
        
        public MemorySnapshot(Instant timestamp, String stage, long heapUsed, long nonHeapUsed) {
            this.timestamp = timestamp;
            this.stage = stage;
            this.heapUsed = heapUsed;
            this.nonHeapUsed = nonHeapUsed;
        }
    }
      /**
     * Detailed performance report with precise measurements
     */
    public static class DetailedPerformanceReport {
        public String reportId;
        public long totalDurationMs;
        public long pdfGenerationDurationMs;
        public int actualPageCount;
        public long fileSizeBytes;
        public double baselineMemoryMB;
        public double finalMemoryMB;
        public double memoryDeltaMB;
        public double peakMemoryDeltaMB;
        public double pagesPerSecond;
        public double mbPerSecond;
        public List<MemorySnapshot> memorySnapshots;
        
        // Getter methods
        public int getActualPageCount() { return actualPageCount; }
        public long getFileSizeBytes() { return fileSizeBytes; }
        public long getPdfGenerationTimeMs() { return pdfGenerationDurationMs; }
        public double getMemoryDeltaMB() { return memoryDeltaMB; }
        public double getPagesPerSecond() { return pagesPerSecond; }
        
        public boolean meetsPerformanceTargets() {
            return pdfGenerationDurationMs < 10000 && // Under 10 seconds
                   Math.abs(memoryDeltaMB) < 100 &&    // Under 100MB memory delta
                   actualPageCount >= 1;               // Generated successfully
        }
        
        public String getPerformanceSummary() {
            return String.format(
                "Report %s: %d pages in %.2fs, %.2fMB memory delta, %.2f pages/sec",
                reportId, actualPageCount, pdfGenerationDurationMs / 1000.0, 
                memoryDeltaMB, pagesPerSecond
            );
        }
    }
}
