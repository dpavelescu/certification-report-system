package com.certreport.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Actuator-based Performance Monitor
 * Uses Spring Boot Actuator and Micrometer for accurate metrics collection
 */
@Service
public class ActuatorPerformanceMonitor {

    private static final Logger logger = LoggerFactory.getLogger(ActuatorPerformanceMonitor.class);

    private final MeterRegistry meterRegistry;
    private final MetricsEndpoint metricsEndpoint;
    private final MemoryMXBean memoryMXBean;
    
    // Performance tracking for reports
    private final Map<String, ReportPerformanceData> reportMetrics = new ConcurrentHashMap<>();
    private final Map<String, List<MemorySnapshot>> memoryTimeSeries = new ConcurrentHashMap<>();
    
    // Micrometer metrics
    private final Timer reportGenerationTimer;
    private final Counter reportsGeneratedCounter;
    private final AtomicLong activeReportsGauge;    public ActuatorPerformanceMonitor(MeterRegistry meterRegistry, MetricsEndpoint metricsEndpoint) {
        this.meterRegistry = meterRegistry;
        this.metricsEndpoint = metricsEndpoint;
        this.memoryMXBean = ManagementFactory.getMemoryMXBean();
        
        // Register custom metrics
        this.reportGenerationTimer = Timer.builder("report.generation.time")
                .description("Time taken to generate reports")
                .tag("type", "certification")
                .register(meterRegistry);
                
        this.reportsGeneratedCounter = Counter.builder("report.generation.count")
                .description("Number of reports generated")
                .tag("type", "certification")
                .register(meterRegistry);
                
        this.activeReportsGauge = meterRegistry.gauge("report.generation.active", new AtomicLong(0));
    }    /**
     * Start monitoring a report generation process with baseline memory capture
     */
    public Timer.Sample startReportGeneration(String reportId, int expectedEmployees, int expectedPages) {
        activeReportsGauge.incrementAndGet();
        
        ReportPerformanceData data = new ReportPerformanceData();
        data.reportId = reportId;
        data.startTime = LocalDateTime.now();
        data.expectedEmployees = expectedEmployees;
        data.expectedPages = expectedPages;
        data.baselineMemorySnapshot = captureDetailedMemorySnapshot(); // Clean baseline
        data.startMemorySnapshot = data.baselineMemorySnapshot; // Same as baseline initially
        
        reportMetrics.put(reportId, data);        memoryTimeSeries.put(reportId, new ArrayList<>());
        
        // Record baseline memory snapshot
        recordMemorySnapshot(reportId, "Baseline");
        
        return Timer.start(meterRegistry);
    }

    /**
     * Record memory snapshot specifically before data loading begins
     */
    public void recordDataProcessingStart(String reportId) {
        recordMemorySnapshot(reportId, "Before Data Loading");
    }

    /**
     * Record memory snapshot after data loading but before PDF generation
     */
    public void recordDataProcessingComplete(String reportId) {
        recordMemorySnapshot(reportId, "Data Loading Complete");
    }

    /**
     * Record memory snapshot before PDF generation starts
     */
    public void recordPdfGenerationStart(String reportId) {
        recordMemorySnapshot(reportId, "Before PDF Generation");
    }

    /**
     * Record memory snapshot after PDF generation completes
     */
    public void recordPdfGenerationComplete(String reportId) {
        recordMemorySnapshot(reportId, "PDF Generation Complete");
    }

    /**
     * Record a memory snapshot during report generation
     */
    public void recordMemorySnapshot(String reportId, String phase) {
        List<MemorySnapshot> snapshots = memoryTimeSeries.get(reportId);
        if (snapshots != null) {
            MemorySnapshot snapshot = new MemorySnapshot();
            snapshot.timestamp = LocalDateTime.now();
            snapshot.phase = phase;
            snapshot.memoryUsage = captureDetailedMemorySnapshot();
            snapshots.add(snapshot);
        }
    }    /**
     * Complete report generation monitoring
     */    public DetailedPerformanceReport completeReportGeneration(Timer.Sample timerSample, String reportId, 
                                                             int actualPages, long fileSizeBytes) {        // Stop timer and CAPTURE the actual measured duration in NANOSECONDS
        long actualDurationNanos = timerSample.stop(reportGenerationTimer);
        reportsGeneratedCounter.increment();
        activeReportsGauge.decrementAndGet();
        
        logger.debug("Timer measurement for {}: {}ms", reportId, actualDurationNanos / 1_000_000);
        
        ReportPerformanceData data = reportMetrics.get(reportId);
        if (data != null) {
            data.endTime = LocalDateTime.now();
            data.actualPages = actualPages;
            data.fileSizeBytes = fileSizeBytes;
            // Convert nanoseconds to milliseconds properly
            data.actualDurationMs = actualDurationNanos / 1_000_000; // Convert nanos to millis
            data.endMemorySnapshot = captureDetailedMemorySnapshot();
            
            // Record final memory snapshot
            recordMemorySnapshot(reportId, "Generation Complete");
            
            return generateDetailedReport(reportId);
        }
        
        return new DetailedPerformanceReport(); // Empty report if data not found
    }

    /**
     * Get detailed memory usage from Actuator metrics
     */
    public MemoryMetrics getDetailedMemoryMetrics() {
        MemoryMetrics metrics = new MemoryMetrics();        try {
            // Heap memory
            var heapUsed = metricsEndpoint.metric("jvm.memory.used", Arrays.asList("area:heap"));
            var heapMax = metricsEndpoint.metric("jvm.memory.max", Arrays.asList("area:heap"));
            
            if (heapUsed != null && !heapUsed.getMeasurements().isEmpty()) {
                metrics.heapUsedMB = (long) (heapUsed.getMeasurements().get(0).getValue() / (1024 * 1024));
            }
            if (heapMax != null && !heapMax.getMeasurements().isEmpty()) {
                metrics.heapMaxMB = (long) (heapMax.getMeasurements().get(0).getValue() / (1024 * 1024));
            }
            
            // Non-heap memory
            var nonHeapUsed = metricsEndpoint.metric("jvm.memory.used", Arrays.asList("area:nonheap"));
            if (nonHeapUsed != null && !nonHeapUsed.getMeasurements().isEmpty()) {
                metrics.nonHeapUsedMB = (long) (nonHeapUsed.getMeasurements().get(0).getValue() / (1024 * 1024));
            }
            
            // GC information
            var gcTime = metricsEndpoint.metric("jvm.gc.pause", null);
            if (gcTime != null && !gcTime.getMeasurements().isEmpty()) {
                metrics.gcTimeMs = (long) (gcTime.getMeasurements().get(0).getValue() * 1000);
            }
            
        } catch (Exception e) {
            // Fallback to JMX if Actuator metrics fail
            MemoryUsage heapMemory = memoryMXBean.getHeapMemoryUsage();
            metrics.heapUsedMB = heapMemory.getUsed() / (1024 * 1024);
            metrics.heapMaxMB = heapMemory.getMax() / (1024 * 1024);
            
            MemoryUsage nonHeapMemory = memoryMXBean.getNonHeapMemoryUsage();
            metrics.nonHeapUsedMB = nonHeapMemory.getUsed() / (1024 * 1024);
        }
        
        return metrics;
    }

    /**
     * Get database connection pool metrics
     */
    public DatabaseMetrics getDatabaseMetrics() {
        DatabaseMetrics metrics = new DatabaseMetrics();        try {            // HikariCP metrics
            var activeConnections = metricsEndpoint.metric("hikaricp.connections.active", null);
            if (activeConnections != null && !activeConnections.getMeasurements().isEmpty()) {
                metrics.activeConnections = activeConnections.getMeasurements().get(0).getValue().intValue();
            }
            
            var idleConnections = metricsEndpoint.metric("hikaricp.connections.idle", null);
            if (idleConnections != null && !idleConnections.getMeasurements().isEmpty()) {
                metrics.idleConnections = idleConnections.getMeasurements().get(0).getValue().intValue();
            }
            
            var totalConnections = metricsEndpoint.metric("hikaricp.connections", null);
            if (totalConnections != null && !totalConnections.getMeasurements().isEmpty()) {
                metrics.totalConnections = totalConnections.getMeasurements().get(0).getValue().intValue();
            }
            
        } catch (Exception e) {
            // Set defaults if metrics not available
            metrics.activeConnections = -1;
            metrics.idleConnections = -1;
            metrics.totalConnections = -1;
        }
          return metrics;
    }

    // Private helper methods
    private MemoryMetrics captureDetailedMemorySnapshot() {
        return getDetailedMemoryMetrics();
    }private DetailedPerformanceReport generateDetailedReport(String reportId) {
        ReportPerformanceData data = reportMetrics.get(reportId);
        List<MemorySnapshot> snapshots = memoryTimeSeries.get(reportId);
        
        if (data == null) {
            return new DetailedPerformanceReport();
        }
        
        DetailedPerformanceReport report = new DetailedPerformanceReport();
        report.reportId = reportId;
        report.startTime = data.startTime;
        report.endTime = data.endTime;
        // Use Timer's actual measurement instead of manual calculation
        report.durationMs = data.actualDurationMs;
        
        report.expectedEmployees = data.expectedEmployees;
        report.expectedPages = data.expectedPages;
        report.actualPages = data.actualPages;
        report.fileSizeBytes = data.fileSizeBytes;
        
        report.startMemorySnapshot = data.startMemorySnapshot;
        report.endMemorySnapshot = data.endMemorySnapshot;        report.memoryTimeSeries = snapshots != null ? new ArrayList<>(snapshots) : new ArrayList<>();            // Calculate comprehensive memory deltas with granular breakdown
        if (data.baselineMemorySnapshot != null && data.endMemorySnapshot != null && snapshots != null) {
            long baselineTotal = data.baselineMemorySnapshot.heapUsedMB + data.baselineMemorySnapshot.nonHeapUsedMB;
            long endTotal = data.endMemorySnapshot.heapUsedMB + data.endMemorySnapshot.nonHeapUsedMB;
            
            // Total memory delta from clean baseline
            report.memoryDeltaMB = endTotal - baselineTotal;
            
            // ENHANCED: Calculate peak memory usage
            long peakMemoryUsage = snapshots.stream()
                .mapToLong(snapshot -> snapshot.memoryUsage.heapUsedMB + snapshot.memoryUsage.nonHeapUsedMB)
                .max()
                .orElse(baselineTotal);
            report.peakMemoryDeltaMB = peakMemoryUsage - baselineTotal;
            
            // Find specific phase memory usage
            MemorySnapshot dataLoadingComplete = findSnapshotByPhase(snapshots, "Data Loading Complete");
            MemorySnapshot pdfGenerationComplete = findSnapshotByPhase(snapshots, "PDF Generation Complete");
            
            if (dataLoadingComplete != null) {
                long dataLoadingTotal = dataLoadingComplete.memoryUsage.heapUsedMB + dataLoadingComplete.memoryUsage.nonHeapUsedMB;
                report.dataProcessingMemoryMB = dataLoadingTotal - baselineTotal;
            }
            
            if (pdfGenerationComplete != null && dataLoadingComplete != null) {
                long pdfGenTotal = pdfGenerationComplete.memoryUsage.heapUsedMB + pdfGenerationComplete.memoryUsage.nonHeapUsedMB;
                long dataLoadingTotal = dataLoadingComplete.memoryUsage.heapUsedMB + dataLoadingComplete.memoryUsage.nonHeapUsedMB;
                report.pdfGenerationMemoryMB = pdfGenTotal - dataLoadingTotal;
            }
              // Estimate framework overhead (any memory not accounted for by data + PDF)
            report.frameworkOverheadMB = report.memoryDeltaMB - report.dataProcessingMemoryMB - report.pdfGenerationMemoryMB;
            
            // Memory calculation details for analysis
            logger.debug("Memory Analysis for {}: Baseline: {}MB, Final: {}MB, Delta: {}MB", 
                reportId, baselineTotal, endTotal, report.memoryDeltaMB);
        } else {
            // Fallback to original calculation if baseline not available
            if (data.startMemorySnapshot != null && data.endMemorySnapshot != null) {
                long startTotal = data.startMemorySnapshot.heapUsedMB + data.startMemorySnapshot.nonHeapUsedMB;
                long endTotal = data.endMemorySnapshot.heapUsedMB + data.endMemorySnapshot.nonHeapUsedMB;                report.memoryDeltaMB = endTotal - startTotal;
                
                logger.debug("Memory Calculation (Fallback): Start={}MB, End={}MB, Delta={}MB", 
                    startTotal, endTotal, report.memoryDeltaMB);
            }
        }
        
        // ENHANCED: Calculate throughput metrics
        if (report.durationMs > 0) {
            double durationSec = report.durationMs / 1000.0;
            
            if (report.actualPages > 0) {
                report.pagesPerSecond = report.actualPages / durationSec;
            }
            
            if (report.fileSizeBytes > 0) {
                report.mbPerSecond = (report.fileSizeBytes / (1024.0 * 1024.0)) / durationSec;
            }
            
            if (data.expectedEmployees > 0) {
                report.employeesPerSecond = data.expectedEmployees / durationSec;
            }
        }
        
        return report;
    }    /**
     * Helper method to find a memory snapshot by phase name
     */
    private static MemorySnapshot findSnapshotByPhase(List<MemorySnapshot> snapshots, String phaseName) {
        return snapshots.stream()
                .filter(snapshot -> phaseName.equals(snapshot.phase))
                .findFirst()
                .orElse(null);
    }

    // Data classes
    public static class ReportPerformanceData {
        String reportId;
        LocalDateTime startTime;
        LocalDateTime endTime;
        long actualDurationMs; // Store Timer's actual measurement
        int expectedEmployees;
        int expectedPages;
        int actualPages;
        long fileSizeBytes;
        MemoryMetrics baselineMemorySnapshot; // Clean baseline memory before any processing
        MemoryMetrics startMemorySnapshot;
        MemoryMetrics endMemorySnapshot;
    }

    public static class MemorySnapshot {
        public LocalDateTime timestamp;
        public String phase;
        public MemoryMetrics memoryUsage;
    }

    public static class MemoryMetrics {
        public long heapUsedMB;
        public long heapMaxMB;
        public long nonHeapUsedMB;
        public long gcTimeMs;
        
        @Override
        public String toString() {
            return String.format("Heap: %dMB/%dMB, NonHeap: %dMB, GC: %dms", 
                    heapUsedMB, heapMaxMB, nonHeapUsedMB, gcTimeMs);
        }
    }

    public static class DatabaseMetrics {
        public int activeConnections;
        public int idleConnections;
        public int totalConnections;
        
        @Override
        public String toString() {
            return String.format("Connections - Active: %d, Idle: %d, Total: %d", 
                    activeConnections, idleConnections, totalConnections);
        }
    }    public static class DetailedPerformanceReport {
        public String reportId;
        public LocalDateTime startTime;
        public LocalDateTime endTime;
        public long durationMs;
        public int expectedEmployees;
        public int expectedPages;
        public int actualPages;
        public long fileSizeBytes;
        public long memoryDeltaMB; // Total memory delta (includes framework overhead)
        public long dataProcessingMemoryMB; // Memory used specifically for report data processing
        public long pdfGenerationMemoryMB; // Memory used specifically for PDF generation
        public long frameworkOverheadMB; // Estimated framework/JVM overhead
        
        // ENHANCED PRECISION METRICS
        public long peakMemoryDeltaMB; // Peak memory usage above baseline
        public double pagesPerSecond; // Pages generated per second
        public double mbPerSecond; // Data processing speed in MB/second
        public double employeesPerSecond; // Employee processing throughput
        
        public MemoryMetrics startMemorySnapshot;
        public MemoryMetrics endMemorySnapshot;
        public List<MemorySnapshot> memoryTimeSeries = new ArrayList<>();
    }
    
    /**
     * Get stored performance data for a completed report
     */
    public DetailedPerformanceReport getStoredPerformanceReport(String reportId) {
        ReportPerformanceData data = reportMetrics.get(reportId);
        if (data == null) {
            return null;
        }
        
        // Create a minimal performance report with available data
        DetailedPerformanceReport report = new DetailedPerformanceReport();
        report.reportId = reportId;
        report.durationMs = data.endTime != null && data.startTime != null ? 
            Duration.between(data.startTime, data.endTime).toMillis() : 0;
        
        // Calculate memory metrics from snapshots if available
        calculateDetailedMemoryMetrics(data, report);
        
        return report;
    }
    
    /**
     * Calculate detailed memory metrics for the report
     */
    private void calculateDetailedMemoryMetrics(ReportPerformanceData data, DetailedPerformanceReport report) {
        if (data.baselineMemorySnapshot != null && data.endMemorySnapshot != null) {
            long baselineTotal = data.baselineMemorySnapshot.heapUsedMB + data.baselineMemorySnapshot.nonHeapUsedMB;
            long endTotal = data.endMemorySnapshot.heapUsedMB + data.endMemorySnapshot.nonHeapUsedMB;
            
            // Total memory delta from clean baseline
            report.memoryDeltaMB = endTotal - baselineTotal;
            
            // Find specific phase memory usage
            MemorySnapshot dataLoadingComplete = findSnapshotByPhase(memoryTimeSeries.get(data.reportId), "Data Loading Complete");
            MemorySnapshot pdfGenerationComplete = findSnapshotByPhase(memoryTimeSeries.get(data.reportId), "PDF Generation Complete");
            
            if (dataLoadingComplete != null) {
                long dataLoadingTotal = dataLoadingComplete.memoryUsage.heapUsedMB + dataLoadingComplete.memoryUsage.nonHeapUsedMB;
                report.dataProcessingMemoryMB = dataLoadingTotal - baselineTotal;
            }
            
            if (pdfGenerationComplete != null && dataLoadingComplete != null) {
                long pdfGenTotal = pdfGenerationComplete.memoryUsage.heapUsedMB + pdfGenerationComplete.memoryUsage.nonHeapUsedMB;
                long dataLoadingTotal = dataLoadingComplete.memoryUsage.heapUsedMB + dataLoadingComplete.memoryUsage.nonHeapUsedMB;
                report.pdfGenerationMemoryMB = pdfGenTotal - dataLoadingTotal;
            }
            
            // Estimate framework overhead (any memory not accounted for by data + PDF)
            report.frameworkOverheadMB = report.memoryDeltaMB - report.dataProcessingMemoryMB - report.pdfGenerationMemoryMB;
        } else {
            // Fallback to original calculation if baseline not available
            if (data.startMemorySnapshot != null && data.endMemorySnapshot != null) {
                long startTotal = data.startMemorySnapshot.heapUsedMB + data.startMemorySnapshot.nonHeapUsedMB;
                long endTotal = data.endMemorySnapshot.heapUsedMB + data.endMemorySnapshot.nonHeapUsedMB;
                report.memoryDeltaMB = endTotal - startTotal;
            }
        }
    }
}
