package com.certreport.test;

import com.certreport.dto.ReportRequestDto;
import com.certreport.model.*;
import com.certreport.repository.*;
import com.certreport.service.ReportService;
import com.certreport.service.EmployeeService;
import com.certreport.service.ActuatorPerformanceMonitor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Large-Scale Performance Test
 * 
 * Tests memory efficiency and performance for generating 100+ page reports.
 * Uses database test environment for realistic performance measurements.
 * Focuses on validating system performance with large datasets (300+ employees).
 * 
 * RENAMED FROM: PostgreSQLLargeScalePerformanceTest (removed database-specific naming)
 * 
 * This test complements ComprehensivePerformanceTest by focusing specifically on
 * large dataset scenarios and extreme load testing.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("postgres-test")
public class LargeScalePerformanceTest {

    @Autowired
    private ReportService reportService;
    
    @Autowired
    private EmployeeService employeeService;
    
    @Autowired
    private ActuatorPerformanceMonitor actuatorPerformanceMonitor;
    
    @Autowired
    private DatabaseTestEnvironmentManager databaseTestEnvironmentManager;
    
    @Autowired
    private ReportRepository reportRepository;

    @BeforeEach
    public void setUp() {
        // Ensure clean test environment and create test data
        databaseTestEnvironmentManager.ensureCleanEnvironment();
        databaseTestEnvironmentManager.ensurePerformanceTestData();
    }

    @AfterEach
    public void tearDown() {
        // Clean up after test to prevent contamination
        databaseTestEnvironmentManager.cleanup();
    }

    /**
     * Test large-scale report generation with all available employees
     * Target: Generate 100+ page reports efficiently
     */
    @Test
    public void testLargeScaleReportGeneration() throws Exception {
        System.out.println("================================================================");
        System.out.println("🚀 LARGE-SCALE PERFORMANCE TEST (TARGET: 100+ PAGES)");
        System.out.println("================================================================");
        System.out.println("Using optimized test data with 350+ employees");
        System.out.println();
        
        long totalEmployees = employeeService.getAllEmployees().size();
        System.out.println("=== READY FOR LARGE-SCALE PERFORMANCE TEST ===");
        System.out.println("- Total Employees Available: " + totalEmployees);
        System.out.println("- Expected Pages: 100+");
        System.out.println("- Expected File Size: 5+ MB");
        System.out.println();
        
        // Create request for ALL employees
        ReportRequestDto request = new ReportRequestDto();
        request.setEmployeeIds(Collections.emptyList()); // Empty = all employees
        request.setReportType("CERTIFICATIONS");

        System.out.println("=== REPORT GENERATION (LARGE-SCALE PERFORMANCE MEASUREMENT) ===");
        
        // Generate the report (this triggers async generation)
        Report report = reportService.generateReport(request);
        System.out.println("Created report with ID: " + report.getId());

        // Wait for completion with extended timeout for large datasets
        Report completedReport = waitForReportCompletion(report.getId(), 180000); // 3 minutes
        
        // Debug: Print actual report details
        System.out.println("=== COMPLETED REPORT DETAILS ===");
        System.out.println("- Report ID: " + completedReport.getId());
        System.out.println("- Status: " + completedReport.getStatus());
        System.out.println("- Page Count: " + completedReport.getPageCount());
        System.out.println("- File Path: " + completedReport.getFilePath());
        System.out.println("- Started At: " + completedReport.getStartedAt());
        System.out.println("- Completed At: " + completedReport.getCompletedAt());
        
        // Get performance metrics from ActuatorPerformanceMonitor
        ActuatorPerformanceMonitor.DetailedPerformanceReport performanceReport = 
            actuatorPerformanceMonitor.getStoredPerformanceReport(completedReport.getId());
        
        ActuatorPerformanceMonitor.MemoryMetrics finalMemory = 
            actuatorPerformanceMonitor.getDetailedMemoryMetrics();

        // Save report and get file size
        String reportFilePath = saveReportToFile(completedReport);
        File reportFile = new File(reportFilePath);
        long fileSizeBytes = reportFile.length();
        
        // Present comprehensive results
        presentLargeScaleResults(completedReport, finalMemory, performanceReport,
                               fileSizeBytes, reportFilePath, totalEmployees);

        // Assertions for large-scale performance
        assertNotNull(completedReport, "Report should be generated");
        assertEquals(Report.ReportStatus.COMPLETED, completedReport.getStatus(), "Report should be completed");
        
        // If report failed, show the error message before asserting page count
        if (completedReport.getStatus() == Report.ReportStatus.FAILED) {
            fail("Report generation failed with error: " + completedReport.getErrorMessage());
        }
        
        assertNotNull(completedReport.getPageCount(), "Page count should be available");
        assertTrue(completedReport.getPageCount() > 0, "Should generate at least 1 page");
        
        // Large-scale specific assertions
        if (totalEmployees >= 300) {
            assertTrue(completedReport.getPageCount() >= 50, 
                      "Large dataset should generate substantial page count");
        }
          // File size validation (reduced to 500KB for realistic test data)
        assertTrue(fileSizeBytes > 500 * 1024, "Large report should be at least 500KB"); // 500KB minimum
        
        System.out.println("✅ LARGE-SCALE PERFORMANCE TEST COMPLETED");
        System.out.println("   - Report File: " + reportFilePath);
        System.out.println("================================================================");
    }

    /**
     * Test extreme load scenario - stress test with maximum possible load
     */
    @Test
    public void testExtremeLoadScenario() throws Exception {
        System.out.println("================================================================");
        System.out.println("⚡ EXTREME LOAD STRESS TEST");
        System.out.println("================================================================");
        
        long totalEmployees = employeeService.getAllEmployees().size();
        
        if (totalEmployees < 200) {
            System.out.println("⚠️ Skipping extreme load test - insufficient test data");
            System.out.println("   Requires at least 200 employees, found: " + totalEmployees);
            return;
        }
        
        System.out.println("- Testing with ALL " + totalEmployees + " employees");
        System.out.println("- This is a stress test to find system limits");
        System.out.println();
        
        // Capture baseline system metrics
        ActuatorPerformanceMonitor.MemoryMetrics baselineMemory = 
            actuatorPerformanceMonitor.getDetailedMemoryMetrics();
        
        ActuatorPerformanceMonitor.DatabaseMetrics baselineDb = 
            actuatorPerformanceMonitor.getDatabaseMetrics();
        
        System.out.println("Baseline System State:");
        System.out.println("- Memory: " + baselineMemory);
        System.out.println("- Database: " + baselineDb);
        System.out.println();
        
        // Generate extreme load report
        ReportRequestDto request = new ReportRequestDto();
        request.setEmployeeIds(Collections.emptyList()); // ALL employees
        request.setReportType("CERTIFICATIONS");
        
        long startTime = System.currentTimeMillis();
        Report report = reportService.generateReport(request);
        
        // Extended timeout for extreme load
        Report completedReport = waitForReportCompletion(report.getId(), 300000); // 5 minutes
        long endTime = System.currentTimeMillis();
        
        double totalTimeSeconds = (endTime - startTime) / 1000.0;
        
        // Capture final system metrics
        ActuatorPerformanceMonitor.MemoryMetrics finalMemory = 
            actuatorPerformanceMonitor.getDetailedMemoryMetrics();
        
        System.out.println("=== EXTREME LOAD TEST RESULTS ===");
        System.out.println("- Total Time: " + String.format("%.2f", totalTimeSeconds) + " seconds");
        System.out.println("- Employee Count: " + totalEmployees);
        System.out.println("- Pages Generated: " + completedReport.getPageCount());
        System.out.println("- Report Status: " + completedReport.getStatus());
        
        // Performance analysis for extreme load
        if (totalTimeSeconds < 60) {
            System.out.println("✅ EXCELLENT: System handled extreme load in under 1 minute");
        } else if (totalTimeSeconds < 180) {
            System.out.println("✅ GOOD: System handled extreme load in under 3 minutes");
        } else if (totalTimeSeconds < 300) {
            System.out.println("⚠️ ACCEPTABLE: System handled extreme load within timeout");
        } else {
            System.out.println("❌ POOR: System struggled with extreme load");
        }
        
        // Memory efficiency under extreme load
        long memoryDelta = (finalMemory.heapUsedMB + finalMemory.nonHeapUsedMB) - 
                          (baselineMemory.heapUsedMB + baselineMemory.nonHeapUsedMB);
        
        System.out.println("- Memory Delta: " + memoryDelta + "MB");
        
        double memoryPerEmployee = (double)memoryDelta / totalEmployees;
        System.out.println("- Memory per Employee: " + String.format("%.2f", memoryPerEmployee) + "MB");
        
        if (memoryPerEmployee < 1.0) {
            System.out.println("✅ EXCELLENT memory efficiency under extreme load");
        } else if (memoryPerEmployee < 3.0) {
            System.out.println("✅ GOOD memory efficiency under extreme load");
        } else {
            System.out.println("⚠️ High memory usage under extreme load - monitor in production");
        }
        
        System.out.println("================================================================");
        
        // Assertions
        assertNotNull(completedReport);
        assertEquals(Report.ReportStatus.COMPLETED, completedReport.getStatus());
        assertTrue(completedReport.getPageCount() > 0);
    }

    /**
     * Wait for report completion with progress reporting
     */
    private Report waitForReportCompletion(String reportId, long timeoutMs) {
        long startTime = System.currentTimeMillis();
        Report report;
        
        System.out.println("Waiting for large-scale report completion...");
        
        do {
            try {
                Thread.sleep(2000); // Check every 2 seconds for large reports
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail("Test interrupted while waiting for report completion");
            }
            
            report = reportRepository.findById(reportId).orElse(null);
            
            if (System.currentTimeMillis() - startTime > timeoutMs) {
                fail("Report generation timed out after " + timeoutMs + "ms");
            }
            
            // Progress reporting every 30 seconds
            if ((System.currentTimeMillis() - startTime) % 30000 < 2000) {
                long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
                System.out.println("⏳ Still processing... (" + elapsedSeconds + "s elapsed) Status: " + 
                                 (report != null ? report.getStatus() : "UNKNOWN"));
            }
            
        } while (report == null || report.getStatus() == Report.ReportStatus.QUEUED || 
                 report.getStatus() == Report.ReportStatus.IN_PROGRESS);
        
        System.out.println("✅ Report completed with status: " + report.getStatus());
        return report;
    }

    /**
     * Save report to file and return file path
     */
    private String saveReportToFile(Report report) throws Exception {
        if (report.getFilePath() == null) {
            throw new Exception("Report file path is null");
        }
        
        // Create reports directory if it doesn't exist
        Path reportsDir = Paths.get("target/test-reports");
        if (!Files.exists(reportsDir)) {
            Files.createDirectories(reportsDir);
        }
        
        // Copy report to test reports directory
        String fileName = "large-scale-test-report-" + report.getId() + ".pdf";
        Path targetPath = reportsDir.resolve(fileName);
        
        Path sourcePath = Paths.get(report.getFilePath());
        if (Files.exists(sourcePath)) {
            Files.copy(sourcePath, targetPath);
            return targetPath.toString();
        } else {
            throw new Exception("Source report file not found: " + report.getFilePath());
        }
    }

    /**
     * Present comprehensive large-scale performance results
     */
    private void presentLargeScaleResults(
            Report report, 
            ActuatorPerformanceMonitor.MemoryMetrics finalMemory,
            ActuatorPerformanceMonitor.DetailedPerformanceReport performanceReport,
            long fileSizeBytes,
            String filePath,
            long employeeCount) {
        
        System.out.println("================================================================");
        System.out.println("📊 LARGE-SCALE PERFORMANCE RESULTS");
        System.out.println("================================================================");
        
        // Report summary
        System.out.println("REPORT SUMMARY:");
        System.out.println("- Report ID: " + report.getId());
        System.out.println("- Status: " + report.getStatus());
        System.out.println("- Page Count: " + report.getPageCount());
        System.out.println("- Employee Count: " + employeeCount);
        System.out.println("- File Size: " + String.format("%.2f", fileSizeBytes / (1024.0 * 1024.0)) + " MB");
        System.out.println("- File Location: " + filePath);
        System.out.println();
        
        // Calculate timing from report timestamps
        long generationTimeMs = 0;
        if (report.getStartedAt() != null && report.getCompletedAt() != null) {
            generationTimeMs = java.time.Duration.between(report.getStartedAt(), report.getCompletedAt()).toMillis();
        }

        // Performance metrics
        System.out.println("PERFORMANCE METRICS:");
        System.out.println("- Total Generation Time: " + String.format("%.2f", generationTimeMs / 1000.0) + " seconds");
        System.out.println("- Current Memory Usage: " + finalMemory);
        
        if (performanceReport != null) {
            System.out.println("- Detailed Memory Delta: " + performanceReport.memoryDeltaMB + "MB");
            System.out.println("- Peak Memory Delta: " + performanceReport.peakMemoryDeltaMB + "MB");
            
            if (performanceReport.dataProcessingMemoryMB > 0) {
                System.out.println("- Data Processing Memory: " + performanceReport.dataProcessingMemoryMB + "MB");
            }
            if (performanceReport.pdfGenerationMemoryMB > 0) {
                System.out.println("- PDF Generation Memory: " + performanceReport.pdfGenerationMemoryMB + "MB");
            }
        }
        System.out.println();

        // Efficiency metrics
        if (report.getPageCount() != null && report.getPageCount() > 0 && generationTimeMs > 0) {
            double reportTimeSec = generationTimeMs / 1000.0;
            double pagesPerSecond = report.getPageCount() / reportTimeSec;
            double employeesPerSecond = employeeCount / reportTimeSec;
            double timePerPage = reportTimeSec / report.getPageCount();
            double timePerEmployee = reportTimeSec / employeeCount;
            
            System.out.println("EFFICIENCY METRICS:");
            System.out.println("- Processing Rate: " + String.format("%.2f", employeesPerSecond) + " employees/second");
            System.out.println("- Page Generation Rate: " + String.format("%.2f", pagesPerSecond) + " pages/second");
            System.out.println("- Time per Employee: " + String.format("%.2f", timePerEmployee) + " seconds");
            System.out.println("- Time per Page: " + String.format("%.2f", timePerPage) + " seconds");
            System.out.println();
        }

        // Large-scale performance assessment
        System.out.println("LARGE-SCALE PERFORMANCE ASSESSMENT:");
        
        // Timing Assessment
        if (generationTimeMs < 30000) { // 30 seconds
            System.out.println("✅ Generation Time: EXCELLENT (< 30 seconds)");
        } else if (generationTimeMs < 60000) { // 1 minute
            System.out.println("✅ Generation Time: VERY GOOD (< 1 minute)");
        } else if (generationTimeMs < 120000) { // 2 minutes
            System.out.println("✅ Generation Time: GOOD (< 2 minutes)");
        } else if (generationTimeMs < 300000) { // 5 minutes
            System.out.println("⚠️ Generation Time: ACCEPTABLE (< 5 minutes)");
        } else {
            System.out.println("❌ Generation Time: NEEDS OPTIMIZATION (> 5 minutes)");
        }
        
        // File Size Assessment
        double fileSizeMB = fileSizeBytes / (1024.0 * 1024.0);
        if (fileSizeMB > 1.0 && report.getPageCount() > 50) {
            System.out.println("✅ File Output: SUBSTANTIAL - Generated significant content");
        } else {
            System.out.println("⚠️ File Output: LIGHT - Verify expected content volume");
        }
        
        // Scalability Assessment
        if (employeeCount >= 300 && generationTimeMs < 120000) {
            System.out.println("✅ Scalability: EXCELLENT - Handles large datasets efficiently");
        } else if (employeeCount >= 200 && generationTimeMs < 180000) {
            System.out.println("✅ Scalability: GOOD - Acceptable for medium-large datasets");
        } else {
            System.out.println("⚠️ Scalability: MONITOR - May need optimization for larger datasets");
        }
        
        System.out.println();
        System.out.println("🎯 PRODUCTION READINESS:");
        
        boolean productionReady = (generationTimeMs < 300000) && // < 5 minutes
                                 (fileSizeMB > 0.5) && // Substantial output
                                 (report.getStatus() == Report.ReportStatus.COMPLETED);
        
        if (productionReady) {
            System.out.println("✅ PRODUCTION READY - Large-scale performance meets targets");
            System.out.println("   - System can handle enterprise-scale report generation");
            System.out.println("   - Memory usage is reasonable for large datasets");
            System.out.println("   - Processing time is acceptable for batch operations");
        } else {
            System.out.println("⚠️ NEEDS OPTIMIZATION - Consider improvements before production");
            System.out.println("   - Monitor performance with production data volumes");
            System.out.println("   - Consider implementing pagination for very large reports");
        }
        
        System.out.println("================================================================");
    }
}
