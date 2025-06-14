package com.certreport.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
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
    }

    /**
     * Start monitoring a report generation process
     */
    public Timer.Sample startReportGeneration(String reportId, int expectedEmployees, int expectedPages) {
        activeReportsGauge.incrementAndGet();
        
        ReportPerformanceData data = new ReportPerformanceData();
        data.reportId = reportId;
        data.startTime = LocalDateTime.now();
        data.expectedEmployees = expectedEmployees;
        data.expectedPages = expectedPages;
        data.startMemorySnapshot = captureDetailedMemorySnapshot();
        
        reportMetrics.put(reportId, data);
        memoryTimeSeries.put(reportId, new ArrayList<>());
        
        // Record initial memory snapshot
        recordMemorySnapshot(reportId, "Generation Start");
        
        return Timer.start(meterRegistry);
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
                                                             int actualPages, long fileSizeBytes) {
        // Stop timer and CAPTURE the actual measured duration in NANOSECONDS
        long actualDurationNanos = timerSample.stop(reportGenerationTimer);
        reportsGeneratedCounter.increment();
        activeReportsGauge.decrementAndGet();
        
        // Debug: Print the raw timer measurement
        System.out.println("DEBUG Timer Measurement:");
        System.out.println("  Raw nanoseconds: " + actualDurationNanos);
        System.out.println("  Converted to milliseconds: " + (actualDurationNanos / 1_000_000));
        System.out.println("  Converted to seconds: " + (actualDurationNanos / 1_000_000_000.0));
        
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
        report.endMemorySnapshot = data.endMemorySnapshot;
        report.memoryTimeSeries = snapshots != null ? new ArrayList<>(snapshots) : new ArrayList<>();
          // Calculate comprehensive memory delta (heap + non-heap)
        if (data.startMemorySnapshot != null && data.endMemorySnapshot != null) {
            long startTotal = data.startMemorySnapshot.heapUsedMB + data.startMemorySnapshot.nonHeapUsedMB;
            long endTotal = data.endMemorySnapshot.heapUsedMB + data.endMemorySnapshot.nonHeapUsedMB;
            report.memoryDeltaMB = endTotal - startTotal;
            
            // Debug: Print memory calculation details
            System.out.println("DEBUG Memory Calculation:");
            System.out.println("  Start: Heap=" + data.startMemorySnapshot.heapUsedMB + "MB, NonHeap=" + data.startMemorySnapshot.nonHeapUsedMB + "MB, Total=" + startTotal + "MB");
            System.out.println("  End: Heap=" + data.endMemorySnapshot.heapUsedMB + "MB, NonHeap=" + data.endMemorySnapshot.nonHeapUsedMB + "MB, Total=" + endTotal + "MB");
            System.out.println("  Delta: " + report.memoryDeltaMB + "MB");
        }
        
        return report;
    }    // Data classes
    public static class ReportPerformanceData {
        String reportId;
        LocalDateTime startTime;
        LocalDateTime endTime;
        long actualDurationMs; // Store Timer's actual measurement
        int expectedEmployees;
        int expectedPages;
        int actualPages;
        long fileSizeBytes;
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
    }

    public static class DetailedPerformanceReport {
        public String reportId;
        public LocalDateTime startTime;
        public LocalDateTime endTime;
        public long durationMs;
        public int expectedEmployees;
        public int expectedPages;
        public int actualPages;
        public long fileSizeBytes;
        public long memoryDeltaMB;
        public MemoryMetrics startMemorySnapshot;
        public MemoryMetrics endMemorySnapshot;
        public List<MemorySnapshot> memoryTimeSeries = new ArrayList<>();
    }
}
