package com.certreport.test;

import com.certreport.dto.ReportRequestDto;
import com.certreport.model.Report;
import com.certreport.service.ReportService;
import com.certreport.service.EmployeeService;
import com.certreport.service.ActuatorPerformanceMonitor;
import com.certreport.repository.ReportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive Performance Test Suite
 * 
 * CONSOLIDATED FROM:
 * - ActuatorBasedPerformanceAnalysisTest
 * - MetricsBasedPerformanceTest  
 * - PerformanceTest
 * 
 * Uses Spring Boot Actuator for comprehensive, production-ready performance monitoring.
 * Focuses on performance characterization and monitoring rather than before/after optimization comparisons.
 * 
 * Test Categories:
 * 1. Small Dataset Performance (5-10 employees)
 * 2. Medium Dataset Performance (50+ employees) 
 * 3. Large Dataset Performance (all employees)
 * 4. Memory Efficiency Analysis
 * 5. Scalability Assessment
 */
@SpringBootTest
@ActiveProfiles("postgres-test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ComprehensivePerformanceTest {

    @Autowired
    private ReportService reportService;
    
    @Autowired
    private EmployeeService employeeService;
    
    @Autowired
    private ActuatorPerformanceMonitor actuatorPerformanceMonitor;
    
    @Autowired
    private ReportRepository reportRepository;
      @BeforeEach
    public void setUp() {
        // Clean test environment - using direct repository cleanup
        reportRepository.deleteAll();
        
        // Ensure we have test data available
        // Test data will be created by individual test methods as needed
    }

    /**
     * Test 1: Small Dataset Performance Analysis
     * Tests performance characteristics with a small employee dataset
     */
    @Test
    @Order(1)
    public void testSmallDatasetPerformance() {
        System.out.println("================================================================");
        System.out.println("🔍 SMALL DATASET PERFORMANCE ANALYSIS (5 EMPLOYEES)");
        System.out.println("================================================================");

        // Get current data context
        long totalEmployees = employeeService.getAllEmployees().size();
        
        System.out.println("Test Environment:");
        System.out.println("- Total Employees Available: " + totalEmployees);
        System.out.println("- Test Size: 5 employees");
        System.out.println("- Monitoring: Spring Boot Actuator");
        System.out.println();
        
        // Generate report with small dataset (first 5 employees)
        List<String> employeeIds = employeeService.getAllEmployees().stream()
                .limit(5)
                .map(emp -> emp.getId())
                .collect(Collectors.toList());
        
        if (employeeIds.isEmpty()) {
            fail("No employees available for performance testing. Please ensure test data is seeded.");
        }
        
        PerformanceTestResult result = executePerformanceTest("SMALL_DATASET", employeeIds, "CERTIFICATIONS");
        
        // Analysis and assertions
        analyzeSmallDatasetResults(result);
        
        // Basic validation
        assertNotNull(result.completedReport);
        assertEquals(Report.ReportStatus.COMPLETED, result.completedReport.getStatus());
        assertNotNull(result.completedReport.getPageCount());
        assertTrue(result.completedReport.getPageCount() > 0);
        
        System.out.println("✅ SMALL DATASET PERFORMANCE TEST COMPLETED");
        System.out.println("================================================================");
    }

    /**
     * Test 2: Medium Dataset Performance Analysis  
     * Tests performance characteristics with a medium employee dataset
     */
    @Test
    @Order(2)
    public void testMediumDatasetPerformance() {
        System.out.println("================================================================");
        System.out.println("🔍 MEDIUM DATASET PERFORMANCE ANALYSIS (50 EMPLOYEES)");
        System.out.println("================================================================");

        long totalEmployees = employeeService.getAllEmployees().size();
        int testSize = Math.min(50, (int)totalEmployees);
        
        System.out.println("Test Environment:");
        System.out.println("- Total Employees Available: " + totalEmployees);
        System.out.println("- Test Size: " + testSize + " employees");
        System.out.println("- Expected Load: Medium");
        System.out.println();
        
        // Generate report with medium dataset
        List<String> employeeIds = employeeService.getAllEmployees().stream()
                .limit(testSize)
                .map(emp -> emp.getId())
                .collect(Collectors.toList());
        
        PerformanceTestResult result = executePerformanceTest("MEDIUM_DATASET", employeeIds, "CERTIFICATIONS");
        
        // Analysis and assertions
        analyzeMediumDatasetResults(result, testSize);
        
        // Validation
        assertNotNull(result.completedReport);
        assertEquals(Report.ReportStatus.COMPLETED, result.completedReport.getStatus());
        
        System.out.println("✅ MEDIUM DATASET PERFORMANCE TEST COMPLETED");
        System.out.println("================================================================");
    }

    /**
     * Test 3: Large Dataset Performance Analysis
     * Tests performance characteristics with all available employees
     */
    @Test
    @Order(3)
    public void testLargeDatasetPerformance() {
        System.out.println("================================================================");
        System.out.println("🔍 LARGE DATASET PERFORMANCE ANALYSIS (ALL EMPLOYEES)");
        System.out.println("================================================================");

        long totalEmployees = employeeService.getAllEmployees().size();
        
        System.out.println("Test Environment:");
        System.out.println("- Total Employees: " + totalEmployees);
        System.out.println("- Test Size: ALL employees");
        System.out.println("- Expected Load: High");
        System.out.println();
        
        // Generate report for ALL employees
        PerformanceTestResult result = executePerformanceTest("LARGE_DATASET", Collections.emptyList(), "CERTIFICATIONS");
        
        // Analysis and assertions
        analyzeLargeDatasetResults(result, (int)totalEmployees);
        
        // Validation
        assertNotNull(result.completedReport);
        assertEquals(Report.ReportStatus.COMPLETED, result.completedReport.getStatus());
        
        System.out.println("✅ LARGE DATASET PERFORMANCE TEST COMPLETED");
        System.out.println("================================================================");
    }

    /**
     * Test 4: Memory Efficiency Analysis
     * Analyzes memory usage patterns and efficiency
     */
    @Test
    @Order(4)
    public void testMemoryEfficiencyAnalysis() {
        System.out.println("================================================================");
        System.out.println("🧠 MEMORY EFFICIENCY ANALYSIS");
        System.out.println("================================================================");

        // Capture baseline memory metrics
        ActuatorPerformanceMonitor.MemoryMetrics baseline = 
            actuatorPerformanceMonitor.getDetailedMemoryMetrics();
        
        System.out.println("Baseline Memory Metrics:");
        System.out.println("- Heap Used: " + baseline.heapUsedMB + "MB");
        System.out.println("- Heap Max: " + baseline.heapMaxMB + "MB");
        System.out.println("- Non-Heap Used: " + baseline.nonHeapUsedMB + "MB");
        System.out.println("- GC Time: " + baseline.gcTimeMs + "ms");
        System.out.println();

        // Test multiple report generations to analyze memory patterns
        List<PerformanceTestResult> results = new ArrayList<>();
        
        for (int i = 1; i <= 3; i++) {
            System.out.println("--- Memory Test Iteration " + i + " ---");
            
            List<String> employeeIds = employeeService.getAllEmployees().stream()
                    .limit(10)
                    .map(emp -> emp.getId())
                    .collect(Collectors.toList());
            
            PerformanceTestResult result = executePerformanceTest("MEMORY_TEST_" + i, employeeIds, "CERTIFICATIONS");
            results.add(result);
            
            System.out.println("Iteration " + i + " completed");
        }
        
        // Analyze memory trends
        analyzeMemoryEfficiency(baseline, results);
        
        System.out.println("✅ MEMORY EFFICIENCY ANALYSIS COMPLETED");
        System.out.println("================================================================");
    }

    /**
     * Test 5: Scalability Assessment
     * Analyzes how performance scales with dataset size
     */
    @Test
    @Order(5)
    public void testScalabilityAssessment() {
        System.out.println("================================================================");
        System.out.println("📈 SCALABILITY ASSESSMENT");
        System.out.println("================================================================");

        long totalEmployees = employeeService.getAllEmployees().size();
        
        // Test different dataset sizes to assess scalability
        int[] testSizes = {5, 20, 50, Math.min(100, (int)totalEmployees)};
        List<PerformanceTestResult> scalabilityResults = new ArrayList<>();
        
        for (int testSize : testSizes) {
            if (testSize > totalEmployees) continue;
            
            System.out.println("--- Testing Scalability with " + testSize + " employees ---");
            
            List<String> employeeIds = employeeService.getAllEmployees().stream()
                    .limit(testSize)
                    .map(emp -> emp.getId())
                    .collect(Collectors.toList());
            
            PerformanceTestResult result = executePerformanceTest("SCALABILITY_" + testSize, employeeIds, "CERTIFICATIONS");
            result.employeeCount = testSize;
            scalabilityResults.add(result);
        }
        
        // Analyze scalability patterns
        analyzeScalability(scalabilityResults);
        
        System.out.println("✅ SCALABILITY ASSESSMENT COMPLETED");
        System.out.println("================================================================");
    }

    /**
     * Execute a performance test and capture comprehensive metrics
     */
    private PerformanceTestResult executePerformanceTest(String testName, List<String> employeeIds, String reportType) {
        System.out.println("Executing test: " + testName);
        
        // Capture pre-test metrics
        ActuatorPerformanceMonitor.MemoryMetrics preTestMemory = 
            actuatorPerformanceMonitor.getDetailedMemoryMetrics();
        
        ActuatorPerformanceMonitor.DatabaseMetrics preTestDb = 
            actuatorPerformanceMonitor.getDatabaseMetrics();
        
        // Generate report
        ReportRequestDto request = new ReportRequestDto();
        request.setEmployeeIds(employeeIds);
        request.setReportType(reportType);
        
        long startTime = System.currentTimeMillis();
        Report report = reportService.generateReport(request);
        
        // Wait for completion
        Report completedReport = waitForReportCompletion(report.getId(), 60000);
        long endTime = System.currentTimeMillis();
        
        // Capture post-test metrics
        ActuatorPerformanceMonitor.MemoryMetrics postTestMemory = 
            actuatorPerformanceMonitor.getDetailedMemoryMetrics();
        
        ActuatorPerformanceMonitor.DatabaseMetrics postTestDb = 
            actuatorPerformanceMonitor.getDatabaseMetrics();
        
        // Get detailed performance report if available
        ActuatorPerformanceMonitor.DetailedPerformanceReport detailedReport = 
            actuatorPerformanceMonitor.getStoredPerformanceReport(completedReport.getId());
        
        // Create result object
        PerformanceTestResult result = new PerformanceTestResult();
        result.testName = testName;
        result.employeeCount = employeeIds.size();
        result.durationMs = endTime - startTime;
        result.completedReport = completedReport;
        result.preTestMemory = preTestMemory;
        result.postTestMemory = postTestMemory;
        result.preTestDb = preTestDb;
        result.postTestDb = postTestDb;
        result.detailedReport = detailedReport;
        
        return result;
    }

    /**
     * Analyze small dataset test results
     */
    private void analyzeSmallDatasetResults(PerformanceTestResult result) {
        System.out.println("=== SMALL DATASET ANALYSIS ===");
        System.out.println("Test Duration: " + (result.durationMs / 1000.0) + " seconds");
        System.out.println("Pages Generated: " + result.completedReport.getPageCount());
        System.out.println("Report Status: " + result.completedReport.getStatus());
        
        if (result.completedReport.getPageCount() != null && result.completedReport.getPageCount() > 0) {
            double timePerPage = result.durationMs / result.completedReport.getPageCount();
            System.out.println("Time per Page: " + String.format("%.2f", timePerPage) + "ms");
        }
        
        long memoryDelta = (result.postTestMemory.heapUsedMB + result.postTestMemory.nonHeapUsedMB) - 
                          (result.preTestMemory.heapUsedMB + result.preTestMemory.nonHeapUsedMB);
        
        System.out.println("Memory Delta: " + memoryDelta + "MB");
        System.out.println("Memory per Employee: " + String.format("%.2f", (double)memoryDelta / result.employeeCount) + "MB");
        
        // Performance assessment
        if (result.durationMs < 5000) {
            System.out.println("✅ Performance: EXCELLENT (< 5 seconds)");
        } else if (result.durationMs < 15000) {
            System.out.println("✅ Performance: GOOD (< 15 seconds)");
        } else {
            System.out.println("⚠️ Performance: NEEDS OPTIMIZATION (> 15 seconds)");
        }
        
        System.out.println();
    }

    /**
     * Analyze medium dataset test results
     */
    private void analyzeMediumDatasetResults(PerformanceTestResult result, int employeeCount) {
        System.out.println("=== MEDIUM DATASET ANALYSIS ===");
        System.out.println("Test Duration: " + (result.durationMs / 1000.0) + " seconds");
        System.out.println("Employee Count: " + employeeCount);
        System.out.println("Pages Generated: " + result.completedReport.getPageCount());
        
        if (result.durationMs > 0) {
            double employeesPerSecond = employeeCount / (result.durationMs / 1000.0);
            System.out.println("Processing Rate: " + String.format("%.2f", employeesPerSecond) + " employees/second");
        }
        
        long memoryDelta = (result.postTestMemory.heapUsedMB + result.postTestMemory.nonHeapUsedMB) - 
                          (result.preTestMemory.heapUsedMB + result.preTestMemory.nonHeapUsedMB);
        
        System.out.println("Memory Delta: " + memoryDelta + "MB");
        System.out.println("Memory per Employee: " + String.format("%.2f", (double)memoryDelta / employeeCount) + "MB");
        
        // Scalability indicators
        double memoryPerEmployee = (double)memoryDelta / employeeCount;
        if (memoryPerEmployee < 2.0) {
            System.out.println("✅ Memory Efficiency: EXCELLENT (< 2MB per employee)");
        } else if (memoryPerEmployee < 5.0) {
            System.out.println("✅ Memory Efficiency: GOOD (< 5MB per employee)");
        } else {
            System.out.println("⚠️ Memory Efficiency: NEEDS OPTIMIZATION (> 5MB per employee)");
        }
        
        System.out.println();
    }

    /**
     * Analyze large dataset test results
     */
    private void analyzeLargeDatasetResults(PerformanceTestResult result, int employeeCount) {
        System.out.println("=== LARGE DATASET ANALYSIS ===");
        System.out.println("Test Duration: " + (result.durationMs / 1000.0) + " seconds");
        System.out.println("Employee Count: " + employeeCount);
        System.out.println("Pages Generated: " + result.completedReport.getPageCount());
        
        if (result.completedReport.getFilePath() != null) {
            File reportFile = new File(result.completedReport.getFilePath());
            if (reportFile.exists()) {
                long fileSizeKB = reportFile.length() / 1024;
                System.out.println("File Size: " + fileSizeKB + " KB");
            }
        }
        
        // Performance assessment for large datasets
        if (result.durationMs < 30000) {
            System.out.println("✅ Large Dataset Performance: EXCELLENT (< 30 seconds)");
        } else if (result.durationMs < 60000) {
            System.out.println("✅ Large Dataset Performance: GOOD (< 60 seconds)");
        } else if (result.durationMs < 120000) {
            System.out.println("⚠️ Large Dataset Performance: ACCEPTABLE (< 2 minutes)");
        } else {
            System.out.println("❌ Large Dataset Performance: NEEDS OPTIMIZATION (> 2 minutes)");
        }
        
        System.out.println();
    }

    /**
     * Analyze memory efficiency across multiple test iterations
     */
    private void analyzeMemoryEfficiency(ActuatorPerformanceMonitor.MemoryMetrics baseline, 
                                       List<PerformanceTestResult> results) {
        System.out.println("=== MEMORY EFFICIENCY ANALYSIS ===");
        System.out.println("Baseline Heap: " + baseline.heapUsedMB + "MB");
        
        for (int i = 0; i < results.size(); i++) {
            PerformanceTestResult result = results.get(i);
            long memoryDelta = result.postTestMemory.heapUsedMB - baseline.heapUsedMB;
            
            System.out.println("Iteration " + (i + 1) + " Memory Delta: " + memoryDelta + "MB");
        }
        
        // Check for memory leaks (increasing memory usage across iterations)
        boolean possibleLeak = false;
        if (results.size() >= 2) {
            long firstDelta = results.get(0).postTestMemory.heapUsedMB - baseline.heapUsedMB;
            long lastDelta = results.get(results.size() - 1).postTestMemory.heapUsedMB - baseline.heapUsedMB;
            
            if (lastDelta > firstDelta + 20) { // 20MB threshold
                possibleLeak = true;
            }
        }
        
        if (possibleLeak) {
            System.out.println("⚠️ POTENTIAL MEMORY LEAK DETECTED - Memory usage increasing across iterations");
        } else {
            System.out.println("✅ Memory usage stable across iterations");
        }
        
        System.out.println();
    }

    /**
     * Analyze scalability patterns across different dataset sizes
     */
    private void analyzeScalability(List<PerformanceTestResult> results) {
        System.out.println("=== SCALABILITY ANALYSIS ===");
        
        System.out.println("Performance by Dataset Size:");
        for (PerformanceTestResult result : results) {
            double timePerEmployee = result.durationMs / (double)result.employeeCount;
            System.out.println("- " + result.employeeCount + " employees: " + 
                             String.format("%.2f", result.durationMs / 1000.0) + "s (" + 
                             String.format("%.1f", timePerEmployee) + "ms per employee)");
        }
        
        // Check for linear scalability
        if (results.size() >= 2) {
            PerformanceTestResult smallest = results.get(0);
            PerformanceTestResult largest = results.get(results.size() - 1);
            
            double smallTimePerEmployee = smallest.durationMs / (double)smallest.employeeCount;
            double largeTimePerEmployee = largest.durationMs / (double)largest.employeeCount;
            
            double scalabilityRatio = largeTimePerEmployee / smallTimePerEmployee;
            
            System.out.println();
            System.out.println("Scalability Analysis:");
            System.out.println("- Small dataset: " + String.format("%.1f", smallTimePerEmployee) + "ms per employee");
            System.out.println("- Large dataset: " + String.format("%.1f", largeTimePerEmployee) + "ms per employee");
            System.out.println("- Scalability ratio: " + String.format("%.2f", scalabilityRatio));
            
            if (scalabilityRatio < 1.5) {
                System.out.println("✅ EXCELLENT scalability - nearly linear performance");
            } else if (scalabilityRatio < 2.5) {
                System.out.println("✅ GOOD scalability - acceptable performance degradation");
            } else {
                System.out.println("⚠️ POOR scalability - significant performance degradation with size");
            }
        }
        
        System.out.println();
    }

    /**
     * Wait for report completion with timeout
     */
    private Report waitForReportCompletion(String reportId, long timeoutMs) {
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
    }    /**
     * Data class to hold performance test results
     */
    private static class PerformanceTestResult {
        int employeeCount;
        long durationMs;
        Report completedReport;
        ActuatorPerformanceMonitor.MemoryMetrics preTestMemory;
        ActuatorPerformanceMonitor.MemoryMetrics postTestMemory;
        
        // Additional fields available for future analysis if needed
        @SuppressWarnings("unused")
        String testName;
        @SuppressWarnings("unused")
        ActuatorPerformanceMonitor.DatabaseMetrics preTestDb;
        @SuppressWarnings("unused")
        ActuatorPerformanceMonitor.DatabaseMetrics postTestDb;
        @SuppressWarnings("unused")
        ActuatorPerformanceMonitor.DetailedPerformanceReport detailedReport;
    }
}
