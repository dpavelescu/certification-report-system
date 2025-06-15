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
 * Large Report Performance Test
 * Tests memory efficiency and performance for generating 100+ page reports
 * Uses PostgreSQL test database for realistic performance measurements
 * Now uses TestIsolationManager for proper test isolation and SQL-based test data
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("postgres-test")
public class LargeReportTestClean {

    @Autowired
    private ReportService reportService;
    
    @Autowired
    private EmployeeService employeeService;
    
    @Autowired
    private ActuatorPerformanceMonitor actuatorPerformanceMonitor;
    
    @Autowired
    private TestIsolationManager testIsolationManager;
    
    @Autowired
    private ReportRepository reportRepository;

    @BeforeEach
    public void setUp() {
        // Ensure clean test environment and create test data
        testIsolationManager.ensureCleanEnvironment();
        testIsolationManager.ensurePerformanceTestData();
        
        // Force garbage collection before test
        System.gc();
        try {
            Thread.sleep(1000); // Allow GC to complete
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @AfterEach
    public void tearDown() {
        // Clean up after test to prevent contamination
        testIsolationManager.cleanup();
    }

    @Test
    public void testLargeReportGeneration() throws Exception {
        System.out.println("=== LARGE REPORT GENERATION TEST (TARGET: 100+ PAGES) ===");
        System.out.println("Using SQL-optimized test data with 350 employees");
        System.out.println();
        
        long totalEmployees = employeeService.getAllEmployees().size();
        System.out.println("=== READY FOR PERFORMANCE TEST ===");
        System.out.println("- Total Employees Available: " + totalEmployees);
        System.out.println();

        // Get baseline metrics from actual services
        long baselineMemoryMB = Runtime.getRuntime().totalMemory() / (1024 * 1024);
        System.out.println("Baseline Memory: " + baselineMemoryMB + " MB");
        System.out.println();
        
        // Create request for ALL employees
        ReportRequestDto request = new ReportRequestDto();
        request.setEmployeeIds(Collections.emptyList()); // Empty = all employees
        request.setReportType("CERTIFICATIONS");

        System.out.println("=== REPORT GENERATION (PERFORMANCE MEASUREMENT) ===");
        
        // Generate the report (this triggers async generation)
        // The REAL timing happens inside ReportService.generateReportAsync() using ActuatorPerformanceMonitor
        Report report = reportService.generateReport(request);
        System.out.println("Created report with ID: " + report.getId());

        // Wait for completion (the real work happens in async method)
        Report completedReport = waitForReportCompletion(report.getId(), 60000);
        
        // Debug: Print actual report details
        System.out.println("DEBUG - Completed Report Details:");
        System.out.println("- Report ID: " + completedReport.getId());
        System.out.println("- Status: " + completedReport.getStatus());
        System.out.println("- Page Count: " + completedReport.getPageCount());
        System.out.println("- File Path: " + completedReport.getFilePath());
        System.out.println("- Error Message: " + completedReport.getErrorMessage());
        System.out.println("- Started At: " + completedReport.getStartedAt());
        System.out.println("- Completed At: " + completedReport.getCompletedAt());
        
        // Get performance metrics from the SAME ActuatorPerformanceMonitor used by ReportService
        ActuatorPerformanceMonitor.MemoryMetrics finalMemory = actuatorPerformanceMonitor.getDetailedMemoryMetrics();

        // Save report and get file size
        String reportFilePath = saveReportToFile(completedReport);
        File reportFile = new File(reportFilePath);
        long fileSizeBytes = reportFile.length();

        // Present results using simple metrics (real timing happened inside ReportService)
        presentResults(completedReport, finalMemory, baselineMemoryMB, 
                      fileSizeBytes, reportFilePath, totalEmployees);

        // Basic assertions
        assertNotNull(completedReport, "Report should be generated");
        assertEquals(Report.ReportStatus.COMPLETED, completedReport.getStatus(), "Report should be completed");
        
        // If report failed, show the error message before asserting page count
        if (completedReport.getStatus() == Report.ReportStatus.FAILED) {
            fail("Report generation failed with error: " + completedReport.getErrorMessage());
        }
        
        assertNotNull(completedReport.getPageCount(), "Page count should be available");
        assertTrue(completedReport.getPageCount() > 0, "Should generate at least 1 page");
        
        System.out.println("✅ LARGE REPORT TEST COMPLETED");
        System.out.println("   - Report File: " + reportFilePath);
        System.out.println();
    }

    private Report waitForReportCompletion(String reportId, long timeoutMs) {
        long startTime = System.currentTimeMillis();
        Report report;
        
        System.out.println("Waiting for report completion...");
        
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
        
        System.out.println("Report completed with status: " + report.getStatus());
        return report;
    }

    private void presentResults(
            Report report, 
            ActuatorPerformanceMonitor.MemoryMetrics finalMemory,
            long baselineMemoryMB,
            long fileSizeBytes,
            String filePath,
            long employeeCount) {
        
        System.out.println("=== PERFORMANCE RESULTS ===");
        System.out.println();
        
        // Report summary
        System.out.println("REPORT SUMMARY:");
        System.out.println("- Report ID: " + report.getId());
        System.out.println("- Status: " + report.getStatus());
        System.out.println("- Page Count: " + report.getPageCount());
        System.out.println("- Employee Count: " + employeeCount);
        System.out.println("- File Size: " + (fileSizeBytes / 1024) + " KB");
        System.out.println("- File Location: " + filePath);
        System.out.println();
        
        // Calculate approximate generation time from timestamps
        long generationTimeMs = 0;
        if (report.getStartedAt() != null && report.getCompletedAt() != null) {
            generationTimeMs = java.time.Duration.between(report.getStartedAt(), report.getCompletedAt()).toMillis();
        }

        // Performance metrics (timing comes from ReportService internal monitoring)
        System.out.println("PERFORMANCE METRICS:");
        System.out.println("- Report Generation Time: " + (generationTimeMs / 1000.0) + " seconds [FROM TIMESTAMPS]");
        System.out.println("- Final Memory: " + finalMemory);
        System.out.println();

        // Efficiency metrics
        if (report.getPageCount() != null && report.getPageCount() > 0 && generationTimeMs > 0) {
            double reportTimeSec = generationTimeMs / 1000.0;
            double pagesPerSecond = report.getPageCount() / reportTimeSec;
            double employeesPerSecond = employeeCount / reportTimeSec;
            
            System.out.println("EFFICIENCY METRICS:");
            System.out.println("- Pages/Second: " + String.format("%.2f", pagesPerSecond));
            System.out.println("- Employees/Second: " + String.format("%.2f", employeesPerSecond));
            System.out.println();
        }

        // Performance assessment
        System.out.println("PERFORMANCE ASSESSMENT:");
        
        // Timing Assessment
        if (generationTimeMs < 10000) {
            System.out.println("✅ Generation time: EXCELLENT (< 10 seconds)");
        } else if (generationTimeMs < 30000) {
            System.out.println("✅ Generation time: GOOD (< 30 seconds)");
        } else {
            System.out.println("⚠️ Generation time: NEEDS OPTIMIZATION (> 30 seconds)");
        }
        
        // Memory Assessment (based on granular analysis logged during generation)
        System.out.println();
        System.out.println("MEMORY PERFORMANCE ANALYSIS:");
        System.out.println("📊 Granular memory breakdown is logged above during report generation");
        System.out.println("🔍 Key Memory Metrics to Review:");
        System.out.println("   • Data Processing Memory: Memory used for loading employee/certification data");
        System.out.println("   • PDF Generation Memory: Memory used by JasperReports engine");
        System.out.println("   • Framework Overhead: Spring/Hibernate baseline (should be ~0MB)");
        System.out.println();
        
        // Memory Efficiency Assessment based on common patterns
        long fileSizeKB = fileSizeBytes / 1024;
        if (fileSizeKB > 0) {
            // Estimate expected memory usage based on file size and employee count
            double memoryPerEmployeeMB = finalMemory.heapUsedMB / employeeCount;
            double memoryToFileSizeRatio = (finalMemory.heapUsedMB * 1024) / fileSizeKB;
            
            System.out.println("MEMORY EFFICIENCY INDICATORS:");
            System.out.println("- Memory per Employee: " + String.format("%.2f", memoryPerEmployeeMB) + " MB");
            System.out.println("- Memory:File Size Ratio: " + String.format("%.0f", memoryToFileSizeRatio) + ":1");
            
            if (memoryPerEmployeeMB < 1.0) {
                System.out.println("✅ Memory per Employee: EXCELLENT (< 1MB per employee)");
            } else if (memoryPerEmployeeMB < 3.0) {
                System.out.println("✅ Memory per Employee: GOOD (< 3MB per employee)");
            } else {
                System.out.println("⚠️ Memory per Employee: HIGH (> 3MB per employee)");
            }
            
            if (memoryToFileSizeRatio < 200) {
                System.out.println("✅ Memory Efficiency: EXCELLENT (< 200:1 ratio)");
            } else if (memoryToFileSizeRatio < 400) {
                System.out.println("✅ Memory Efficiency: GOOD (< 400:1 ratio)");
            } else {
                System.out.println("⚠️ Memory Efficiency: NEEDS REVIEW (> 400:1 ratio)");
            }
        }
        
        // Scalability Assessment
        System.out.println();
        System.out.println("SCALABILITY ASSESSMENT:");
        double estimatedTimeFor1000 = (generationTimeMs * 1000.0) / employeeCount;
        double estimatedMemoryFor1000MB = (finalMemory.heapUsedMB * 1000.0) / employeeCount;
        
        System.out.println("- Estimated time for 1000 employees: " + String.format("%.1f", estimatedTimeFor1000 / 1000.0) + " seconds");
        System.out.println("- Estimated memory for 1000 employees: " + String.format("%.0f", estimatedMemoryFor1000MB) + " MB");
        
        if (estimatedTimeFor1000 < 30000) {
            System.out.println("✅ Scalability: EXCELLENT (1000 employees < 30s)");
        } else if (estimatedTimeFor1000 < 60000) {
            System.out.println("✅ Scalability: GOOD (1000 employees < 60s)");
        } else {
            System.out.println("⚠️ Scalability: LIMITED (1000 employees > 60s)");
        }
        
        if (estimatedMemoryFor1000MB < 1000) {
            System.out.println("✅ Memory Scalability: EXCELLENT (1000 employees < 1GB)");
        } else if (estimatedMemoryFor1000MB < 2000) {
            System.out.println("✅ Memory Scalability: GOOD (1000 employees < 2GB)");
        } else {
            System.out.println("⚠️ Memory Scalability: HIGH (1000 employees > 2GB)");
        }
        System.out.println();
    }

    private String saveReportToFile(Report report) throws Exception {
        // Create target directory if it doesn't exist
        Path targetDir = Paths.get("target");
        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }
        
        // Save to target/large-report.pdf
        Path outputPath = targetDir.resolve("large-report.pdf");

        if (report.getFilePath() != null) {
            // Copy from existing file path
            Path sourcePath = Paths.get(report.getFilePath());
            Files.copy(sourcePath, outputPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } else {
            throw new IllegalStateException("Report has no file path");
        }
        
        return outputPath.toAbsolutePath().toString();
    }
}
