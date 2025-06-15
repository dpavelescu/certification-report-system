package com.certreport.test;

import com.certreport.dto.ReportRequestDto;
import com.certreport.model.Report;
import com.certreport.service.ReportService;
import com.certreport.service.EmployeeService;
import com.certreport.service.ActuatorPerformanceMonitor;
import com.certreport.repository.ReportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive Actuator-based performance analysis test.
 * Uses ONLY Spring Boot Actuator for non-intrusive monitoring.
 * No manual timing, memory measurements, or GC calls.
 */
@SpringBootTest
@ActiveProfiles("postgres-test")
public class ActuatorBasedPerformanceAnalysisTest {

    @Autowired
    private ReportService reportService;
    
    @Autowired
    private EmployeeService employeeService;
      @Autowired
    private ActuatorPerformanceMonitor actuatorPerformanceMonitor;
      
    @Autowired
    private ReportRepository reportRepository;
      @Autowired
    private DatabaseTestEnvironmentManager databaseTestEnvironmentManager;

    @BeforeEach
    public void setUp() {
        // Ensure clean test environment and create test data
        databaseTestEnvironmentManager.ensureCleanEnvironment();
        databaseTestEnvironmentManager.ensurePerformanceTestData();
    }

    @Test
    public void testActuatorBasedPerformanceAnalysis() {
        System.out.println("=== ACTUATOR-ONLY PERFORMANCE ANALYSIS ===");
        System.out.println("Using Spring Boot Actuator for non-intrusive monitoring");
        System.out.println();

        // Get current data context
        long totalEmployees = employeeService.getAllEmployees().size();
        
        System.out.println("Test Environment:");
        System.out.println("- Total Employees: " + totalEmployees);
        System.out.println("- Monitoring: 100% Actuator-based (non-intrusive)");
        System.out.println();
        
        // Generate report with moderate dataset (first 10 employees)
        List<String> employeeIds = employeeService.getAllEmployees().stream()
                .limit(Math.min(10, totalEmployees))
                .map(emp -> emp.getId())
                .collect(Collectors.toList());
        
        if (employeeIds.isEmpty()) {
            fail("No employees available for performance testing. Please ensure test data is seeded.");
        }
        
        System.out.println("Generating report for " + employeeIds.size() + " employees...");
        
        // Generate report - ActuatorPerformanceMonitor tracks everything automatically
        ReportRequestDto request = new ReportRequestDto();
        request.setEmployeeIds(employeeIds);
        request.setReportType("EMPLOYEE_DEMOGRAPHICS");
        
        Report report = reportService.generateReport(request);
        Report completedReport = waitForReportCompletion(report.getId(), 30000);
        
        // Get Actuator-based metrics (non-intrusive)
        ActuatorPerformanceMonitor.MemoryMetrics finalMemory = 
            actuatorPerformanceMonitor.getDetailedMemoryMetrics();
        
        ActuatorPerformanceMonitor.DatabaseMetrics dbMetrics = 
            actuatorPerformanceMonitor.getDatabaseMetrics();
        
        // Present results using only Actuator data
        presentActuatorAnalysis(completedReport, finalMemory, dbMetrics, employeeIds.size());
        
        // Basic assertions
        assertNotNull(completedReport);
        assertEquals(Report.ReportStatus.COMPLETED, completedReport.getStatus());
        assertNotNull(completedReport.getPageCount());
        assertTrue(completedReport.getPageCount() > 0);
        
        System.out.println("✅ ACTUATOR-BASED PERFORMANCE TEST COMPLETED");
    }    private Report waitForReportCompletion(String reportId, long timeoutMs) {
        // Note: This timing is for test infrastructure timeout, not performance measurement
        long startTime = System.currentTimeMillis();
        Report report;
        
        do {
            try {
                Thread.sleep(1000); // Check every second
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail("Test interrupted while waiting for report completion");
            }
            
            report = reportRepository.findById(reportId).orElse(null);
            
            if (System.currentTimeMillis() - startTime > timeoutMs) {
                fail("Report generation timed out after " + timeoutMs + "ms");
            }
            
        } while (report == null || report.getStatus() == Report.ReportStatus.QUEUED || 
                 report.getStatus() == Report.ReportStatus.IN_PROGRESS);
        
        return report;
    }

    private void presentActuatorAnalysis(
            Report report, 
            ActuatorPerformanceMonitor.MemoryMetrics finalMemory,
            ActuatorPerformanceMonitor.DatabaseMetrics dbMetrics,
            int employeeCount) {
        
        System.out.println("=== ACTUATOR-BASED PERFORMANCE RESULTS ===");
        System.out.println();
        
        // Basic report information
        System.out.println("REPORT SUMMARY:");
        System.out.println("- Report ID: " + report.getId());
        System.out.println("- Status: " + report.getStatus());
        System.out.println("- Page Count: " + report.getPageCount());
        System.out.println("- Employee Count: " + employeeCount);
        
        if (report.getFilePath() != null) {
            File reportFile = new File(report.getFilePath());
            if (reportFile.exists()) {
                long fileSizeKB = reportFile.length() / 1024;
                System.out.println("- File Size: " + fileSizeKB + " KB");
            }
        }
        
        // Timing analysis from report timestamps (non-intrusive)
        if (report.getStartedAt() != null && report.getCompletedAt() != null) {
            long durationMs = java.time.Duration.between(
                report.getStartedAt(), report.getCompletedAt()).toMillis();
            
            System.out.println();
            System.out.println("TIMING ANALYSIS (from report timestamps):");
            System.out.println("- Generation Time: " + (durationMs / 1000.0) + " seconds");
            
            if (report.getPageCount() != null && report.getPageCount() > 0 && durationMs > 0) {
                double pagesPerSecond = (report.getPageCount() * 1000.0) / durationMs;
                double employeesPerSecond = (employeeCount * 1000.0) / durationMs;
                System.out.println("- Throughput: " + String.format("%.2f", pagesPerSecond) + " pages/second");
                System.out.println("- Processing Rate: " + String.format("%.2f", employeesPerSecond) + " employees/second");
            }
        }
        
        // Actuator memory metrics (non-intrusive)
        System.out.println();
        System.out.println("ACTUATOR MEMORY METRICS:");        System.out.println("- Current Heap: " + finalMemory.heapUsedMB + "MB / " + finalMemory.heapMaxMB + "MB");
        System.out.println("- Non-Heap: " + finalMemory.nonHeapUsedMB + "MB");
        System.out.println("- GC Time: " + finalMemory.gcTimeMs + "ms");
        
        // Database metrics (non-intrusive)
        System.out.println();
        System.out.println("DATABASE METRICS:");
        System.out.println("- Active Connections: " + dbMetrics.activeConnections);
        System.out.println("- Idle Connections: " + dbMetrics.idleConnections);
        System.out.println("- Total Connections: " + dbMetrics.totalConnections);
        
        // Performance assessment (based on non-intrusive metrics)
        System.out.println();
        System.out.println("PERFORMANCE ASSESSMENT:");
        
        if (report.getStartedAt() != null && report.getCompletedAt() != null) {
            long durationMs = java.time.Duration.between(
                report.getStartedAt(), report.getCompletedAt()).toMillis();
            
            if (durationMs < 5000) {
                System.out.println("✅ Generation time: EXCELLENT (< 5 seconds)");
            } else if (durationMs < 15000) {
                System.out.println("✅ Generation time: GOOD (< 15 seconds)");
            } else {
                System.out.println("⚠️ Generation time: ACCEPTABLE (> 15 seconds)");
            }
        }
        
        // Memory efficiency (based on current metrics, not deltas)
        if (finalMemory.heapUsedMB < 100) {
            System.out.println("✅ Memory usage: EFFICIENT (< 100MB)");
        } else if (finalMemory.heapUsedMB < 300) {
            System.out.println("✅ Memory usage: MODERATE (< 300MB)");
        } else {
            System.out.println("⚠️ Memory usage: HIGH (> 300MB)");
        }
        
        System.out.println();
        System.out.println("📊 NOTE: All metrics collected non-intrusively via Spring Boot Actuator");
        System.out.println("🚀 This monitoring approach is production-ready and extensible");
    }
}
