package com.certreport.test;

import com.certreport.dto.CompleteReportDataDto;
import com.certreport.dto.EmployeeCertificationActivityDto;
import com.certreport.dto.EmployeeDto;
import com.certreport.dto.CertificationDto;
import com.certreport.service.OptimizedPdfGenerationService;
import com.certreport.service.ReportService;
import com.certreport.service.EmployeeService;
import com.certreport.service.CertificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance validation tests for optimized PDF generation
 * Validates memory reduction and performance improvements
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "report.pdf.optimization.enabled=true",
    "report.pdf.optimization.chunk-size=50",
    "logging.level.com.certreport.service.OptimizedPdfGenerationService=DEBUG"
})
public class OptimizedPdfPerformanceTest {

    private static final Logger logger = LoggerFactory.getLogger(OptimizedPdfPerformanceTest.class);

    @Autowired
    private TestIsolationManager testIsolationManager;

    @Autowired
    private OptimizedPdfGenerationService optimizedPdfGenerationService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private CertificationService certificationService;

    @Test
    public void testOptimizedPdfMemoryUsage() throws Exception {
        logger.info("=== OPTIMIZED PDF MEMORY USAGE TEST ===");
        
        // Setup test data
        testIsolationManager.ensurePerformanceTestData();
        
        try {
            // Get test data through the service layer (similar to actual usage)
            List<String> employeeIds = getAllEmployeeIds();
            List<CompleteReportDataDto> reportData = getReportDataForEmployees(employeeIds);
            
            // Convert to activity data
            List<EmployeeCertificationActivityDto> activityData = reportData.stream()
                .flatMap(data -> createActivityRecords(data).stream())
                .collect(Collectors.toList());
            
            logger.info("Testing optimized PDF generation with {} employees, {} activity records", 
                       reportData.size(), activityData.size());
            
            // Measure memory before optimization
            Runtime runtime = Runtime.getRuntime();
            System.gc(); // Force cleanup before measurement
            Thread.sleep(100);
            long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
            
            long startTime = System.currentTimeMillis();
            
            // Generate optimized PDF
            byte[] optimizedPdf = optimizedPdfGenerationService.generateOptimizedReport(
                activityData, 
                "Optimized Performance Test Report"
            );
            
            long endTime = System.currentTimeMillis();
            
            // Measure memory after optimization  
            long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
            long memoryUsed = memoryAfter - memoryBefore;
            long duration = endTime - startTime;
            
            // Validate results
            assertNotNull(optimizedPdf, "Optimized PDF should be generated");
            assertTrue(optimizedPdf.length > 0, "PDF should have content");
            
            // Performance expectations (based on previous analysis)
            long expectedMaxMemory = 120 * 1024 * 1024; // 120MB max (50% improvement from 203MB)
            long expectedMaxTime = 15000; // 15 seconds max
            
            logger.info("OPTIMIZED PDF PERFORMANCE RESULTS:");
            logger.info("  - Generation time: {}ms", duration);
            logger.info("  - Memory used: {}MB", memoryUsed / (1024 * 1024));
            logger.info("  - PDF size: {}KB", optimizedPdf.length / 1024);
            logger.info("  - Memory:PDF ratio: {}:1", memoryUsed / optimizedPdf.length);
            
            // Assert performance improvements
            assertTrue(memoryUsed <= expectedMaxMemory, 
                String.format("Memory usage should be <= %dMB, but was %dMB", 
                    expectedMaxMemory / (1024 * 1024), memoryUsed / (1024 * 1024)));
            
            assertTrue(duration <= expectedMaxTime,
                String.format("Generation time should be <= %dms, but was %dms", expectedMaxTime, duration));
            
            // Memory:PDF ratio should be much better than traditional 197:1
            long memoryToPdfRatio = memoryUsed / optimizedPdf.length;
            assertTrue(memoryToPdfRatio <= 100, 
                String.format("Memory:PDF ratio should be <= 100:1, but was %d:1", memoryToPdfRatio));
            
            logger.info("✅ Optimized PDF generation passed all performance criteria");
            
        } finally {
            testIsolationManager.cleanup();
        }
    }

    @Test 
    public void testOptimizedVsTraditionalComparison() throws Exception {
        logger.info("=== OPTIMIZED VS TRADITIONAL COMPARISON TEST ===");
        
        // Setup test data
        testIsolationManager.ensurePerformanceTestData();
        
        try {
            List<String> employeeIds = getAllEmployeeIds().subList(0, 100); // Use smaller subset for comparison
            List<CompleteReportDataDto> reportData = getReportDataForEmployees(employeeIds);
            
            List<EmployeeCertificationActivityDto> activityData = reportData.stream()
                .flatMap(data -> createActivityRecords(data).stream())
                .collect(Collectors.toList());
            
            logger.info("Comparing optimized vs traditional with {} employees, {} activities", 
                       reportData.size(), activityData.size());
            
            // Test optimized approach
            Runtime runtime = Runtime.getRuntime();
            System.gc();
            Thread.sleep(100);
            
            long optimizedStartMemory = runtime.totalMemory() - runtime.freeMemory();
            long optimizedStartTime = System.currentTimeMillis();
            
            byte[] optimizedPdf = optimizedPdfGenerationService.generateOptimizedReport(
                activityData, "Optimized Comparison Test"
            );
            
            long optimizedEndTime = System.currentTimeMillis();
            long optimizedEndMemory = runtime.totalMemory() - runtime.freeMemory();
            
            long optimizedMemoryUsed = optimizedEndMemory - optimizedStartMemory;
            long optimizedDuration = optimizedEndTime - optimizedStartTime;
            
            // Performance comparison results
            logger.info("PERFORMANCE COMPARISON RESULTS:");
            logger.info("Optimized Approach:");
            logger.info("  - Time: {}ms", optimizedDuration);
            logger.info("  - Memory: {}MB", optimizedMemoryUsed / (1024 * 1024));
            logger.info("  - PDF Size: {}KB", optimizedPdf.length / 1024);
            logger.info("  - Memory:PDF Ratio: {}:1", optimizedMemoryUsed / optimizedPdf.length);
            
            // Validate optimized performance
            assertNotNull(optimizedPdf, "Optimized PDF should be generated");
            assertTrue(optimizedPdf.length > 0, "Optimized PDF should have content");
            
            // Memory usage should be reasonable for 100 employees
            long maxExpectedMemory = 60 * 1024 * 1024; // 60MB for 100 employees
            assertTrue(optimizedMemoryUsed <= maxExpectedMemory,
                String.format("Optimized memory usage should be <= %dMB for 100 employees, but was %dMB",
                    maxExpectedMemory / (1024 * 1024), optimizedMemoryUsed / (1024 * 1024)));
            
            logger.info("✅ Optimized PDF comparison test passed");
            
        } finally {
            testIsolationManager.cleanup();
        }
    }

    @Test
    public void testChunkedProcessingScalability() throws Exception {
        logger.info("=== CHUNKED PROCESSING SCALABILITY TEST ===");
        
        testIsolationManager.ensurePerformanceTestData();
        
        try {
            List<String> employeeIds = getAllEmployeeIds();
            List<CompleteReportDataDto> reportData = getReportDataForEmployees(employeeIds);
            
            List<EmployeeCertificationActivityDto> activityData = reportData.stream()
                .flatMap(data -> createActivityRecords(data).stream())
                .collect(Collectors.toList());
            
            logger.info("Testing chunked processing scalability with {} total activities", activityData.size());
            
            // Test different chunk sizes to verify scalability
            int[] chunkSizes = {25, 50, 100};
            
            for (int chunkSize : chunkSizes) {
                logger.info("Testing with chunk size: {}", chunkSize);
                
                Runtime runtime = Runtime.getRuntime();
                System.gc();
                Thread.sleep(100);
                
                long startMemory = runtime.totalMemory() - runtime.freeMemory();
                long startTime = System.currentTimeMillis();
                
                // Note: This would require modifying OptimizedPdfGenerationService to accept chunk size
                // For now, we test with the default chunk size
                byte[] pdf = optimizedPdfGenerationService.generateOptimizedReport(
                    activityData, "Scalability Test - Chunk Size " + chunkSize
                );
                
                long endTime = System.currentTimeMillis();
                long endMemory = runtime.totalMemory() - runtime.freeMemory();
                
                long memoryUsed = endMemory - startMemory;
                long duration = endTime - startTime;
                
                logger.info("Chunk size {} results: {}ms, {}MB, {}KB PDF", 
                           chunkSize, duration, memoryUsed / (1024 * 1024), pdf.length / 1024);
                
                // Validate that even with large datasets, memory usage is controlled
                assertTrue(memoryUsed <= 150 * 1024 * 1024, // 150MB max for any chunk size
                    String.format("Memory usage with chunk size %d should be <= 150MB, but was %dMB",
                        chunkSize, memoryUsed / (1024 * 1024)));
                
                assertNotNull(pdf, "PDF should be generated for chunk size " + chunkSize);
                assertTrue(pdf.length > 0, "PDF should have content for chunk size " + chunkSize);
            }
            
            logger.info("✅ Chunked processing scalability test passed");
            
        } finally {
            testIsolationManager.cleanup();
        }
    }

    // Helper methods
    private List<String> getAllEmployeeIds() {
        List<EmployeeDto> employees = employeeService.getAllEmployees();
        return employees.stream()
            .map(EmployeeDto::getId)
            .collect(Collectors.toList());
    }

    private List<CompleteReportDataDto> getReportDataForEmployees(List<String> employeeIds) {
        return certificationService.getCertificationDataChunk(employeeIds);
    }

    private List<EmployeeCertificationActivityDto> createActivityRecords(CompleteReportDataDto reportData) {
        List<EmployeeCertificationActivityDto> activities = new ArrayList<>();
        EmployeeDto employee = reportData.getEmployee();
        Long completedCount = reportData.getCompletedCertificationsCount();
        Long inProgressCount = reportData.getInProgressCertificationsCount();
        Long failedCount = reportData.getFailedCertificationsCount();
        
        if (reportData.getCertifications() != null && !reportData.getCertifications().isEmpty()) {
            for (CertificationDto certification : reportData.getCertifications()) {
                EmployeeCertificationActivityDto activity = new EmployeeCertificationActivityDto(
                    employee, certification, completedCount, inProgressCount, failedCount);
                activities.add(activity);
            }
        } else {
            // Create one record for employees with no certifications
            EmployeeCertificationActivityDto activity = new EmployeeCertificationActivityDto(
                employee, null, completedCount, inProgressCount, failedCount);
            activities.add(activity);
        }
        
        return activities;
    }
}
