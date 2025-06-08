package com.certreport.test;

import com.certreport.dto.ReportRequestDto;
import com.certreport.model.Report;
import com.certreport.service.ReportService;
import com.certreport.service.PerformanceMonitoringService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance tests to validate report generation meets the 5-second requirement
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ReportPerformanceTest {

    @Autowired
    private ReportService reportService;

    @Autowired
    private PerformanceMonitoringService performanceMonitoringService;

    private List<String> smallEmployeeIdList;
    private List<String> mediumEmployeeIdList;
    private List<String> largeEmployeeIdList;

    @BeforeEach
    void setUp() {
        // Small dataset (1-5 employees)
        smallEmployeeIdList = List.of("EMP001", "EMP002", "EMP003");
        
        // Medium dataset (6-15 employees)
        mediumEmployeeIdList = List.of(
            "EMP001", "EMP002", "EMP003", "EMP004", "EMP005",
            "EMP006", "EMP007", "EMP008", "EMP009", "EMP010"
        );
        
        // Large dataset (all 15 employees)
        largeEmployeeIdList = List.of(
            "EMP001", "EMP002", "EMP003", "EMP004", "EMP005",
            "EMP006", "EMP007", "EMP008", "EMP009", "EMP010",
            "EMP011", "EMP012", "EMP013", "EMP014", "EMP015"
        );
    }    @Test
    public void testSmallReportGenerationPerformance() throws Exception {
        ReportRequestDto request = new ReportRequestDto();
        request.setEmployeeIds(smallEmployeeIdList);
        request.setReportType("EMPLOYEE_DEMOGRAPHICS");

        LocalDateTime startTime = LocalDateTime.now();
        
        // generateReport now returns Report directly (initial QUEUED report)
        Report report = reportService.generateReport(request);
        
        // Wait for async processing to complete by polling status
        report = waitForReportCompletion(report.getId(), 10); // 10 second timeout
        
        LocalDateTime endTime = LocalDateTime.now();
        Duration duration = Duration.between(startTime, endTime);

        // Assertions
        assertNotNull(report);
        assertEquals(Report.ReportStatus.COMPLETED, report.getStatus());
        assertTrue(duration.toSeconds() <= 5, 
                  String.format("Small report generation took %d seconds, exceeding 5 second limit", 
                               duration.toSeconds()));
        
        System.out.printf("Small report generation completed in %d ms%n", duration.toMillis());
    }    @Test
    public void testMediumReportGenerationPerformance() throws Exception {
        ReportRequestDto request = new ReportRequestDto();
        request.setEmployeeIds(mediumEmployeeIdList);
        request.setReportType("EMPLOYEE_DEMOGRAPHICS");

        LocalDateTime startTime = LocalDateTime.now();
        
        // generateReport now returns Report directly (initial QUEUED report)
        Report report = reportService.generateReport(request);
        
        // Wait for async processing to complete by polling status
        report = waitForReportCompletion(report.getId(), 10); // 10 second timeout
        
        LocalDateTime endTime = LocalDateTime.now();
        Duration duration = Duration.between(startTime, endTime);

        // Assertions
        assertNotNull(report);
        assertEquals(Report.ReportStatus.COMPLETED, report.getStatus());
        assertTrue(duration.toSeconds() <= 5, 
                  String.format("Medium report generation took %d seconds, exceeding 5 second limit", 
                               duration.toSeconds()));
        
        System.out.printf("Medium report generation completed in %d ms%n", duration.toMillis());
    }    @Test
    public void testLargeReportGenerationPerformance() throws Exception {
        ReportRequestDto request = new ReportRequestDto();
        request.setEmployeeIds(largeEmployeeIdList);
        request.setReportType("EMPLOYEE_DEMOGRAPHICS");

        LocalDateTime startTime = LocalDateTime.now();
        
        // generateReport now returns Report directly (initial QUEUED report)
        Report report = reportService.generateReport(request);
        
        // Wait for async processing to complete by polling status
        report = waitForReportCompletion(report.getId(), 10); // 10 second timeout
        
        LocalDateTime endTime = LocalDateTime.now();
        Duration duration = Duration.between(startTime, endTime);

        // Assertions
        assertNotNull(report);
        assertEquals(Report.ReportStatus.COMPLETED, report.getStatus());
        assertTrue(duration.toSeconds() <= 5, 
                  String.format("Large report generation took %d seconds, exceeding 5 second limit", 
                               duration.toSeconds()));
        
        System.out.printf("Large report generation completed in %d ms%n", duration.toMillis());
    }    @Test
    public void testConcurrentReportGeneration() throws Exception {
        // Test multiple reports being generated concurrently
        ReportRequestDto request1 = new ReportRequestDto();
        request1.setEmployeeIds(List.of("EMP001", "EMP002", "EMP003"));
        request1.setReportType("EMPLOYEE_DEMOGRAPHICS");

        ReportRequestDto request2 = new ReportRequestDto();
        request2.setEmployeeIds(List.of("EMP004", "EMP005", "EMP006"));
        request2.setReportType("EMPLOYEE_DEMOGRAPHICS");

        ReportRequestDto request3 = new ReportRequestDto();
        request3.setEmployeeIds(List.of("EMP007", "EMP008", "EMP009"));
        request3.setReportType("EMPLOYEE_DEMOGRAPHICS");

        LocalDateTime startTime = LocalDateTime.now();

        // Start all reports - each returns initial report with QUEUED status
        Report report1 = reportService.generateReport(request1);
        Report report2 = reportService.generateReport(request2);
        Report report3 = reportService.generateReport(request3);

        // Wait for all to complete
        report1 = waitForReportCompletion(report1.getId(), 15);
        report2 = waitForReportCompletion(report2.getId(), 15);
        report3 = waitForReportCompletion(report3.getId(), 15);

        LocalDateTime endTime = LocalDateTime.now();
        Duration duration = Duration.between(startTime, endTime);

        // Assertions
        assertNotNull(report1);
        assertNotNull(report2);
        assertNotNull(report3);
        assertEquals(Report.ReportStatus.COMPLETED, report1.getStatus());
        assertEquals(Report.ReportStatus.COMPLETED, report2.getStatus());
        assertEquals(Report.ReportStatus.COMPLETED, report3.getStatus());

        // Each individual report should complete within 5 seconds
        // Total concurrent execution should be efficient
        assertTrue(duration.toSeconds() <= 10, 
                  String.format("Concurrent report generation took %d seconds, indicating poor concurrency", 
                               duration.toSeconds()));

        System.out.printf("Concurrent report generation completed in %d ms%n", duration.toMillis());
    }    @Test
    public void testMemoryUsageDuringReportGeneration() throws Exception {
        PerformanceMonitoringService.PerformanceMetrics beforeMetrics = 
            performanceMonitoringService.getCurrentMetrics();

        ReportRequestDto request = new ReportRequestDto();
        request.setEmployeeIds(largeEmployeeIdList);
        request.setReportType("EMPLOYEE_DEMOGRAPHICS");

        // generateReport now returns Report directly (initial QUEUED report)
        Report report = reportService.generateReport(request);
        
        // Wait for async processing to complete by polling status
        report = waitForReportCompletion(report.getId(), 10); // 10 second timeout

        PerformanceMonitoringService.PerformanceMetrics afterMetrics = 
            performanceMonitoringService.getCurrentMetrics();

        // Memory usage should not increase excessively
        long memoryIncrease = afterMetrics.getUsedMemory() - beforeMetrics.getUsedMemory();
        long memoryIncreaseInMB = memoryIncrease / (1024 * 1024);

        assertTrue(memoryIncreaseInMB < 100, 
                  String.format("Memory usage increased by %d MB during report generation", 
                               memoryIncreaseInMB));

        System.out.printf("Memory usage increased by %d MB during report generation%n", 
                         memoryIncreaseInMB);
    }    @Test
    public void testReportGenerationUnderLoad() throws Exception {
        // Simulate load by generating multiple reports in sequence
        int numberOfReports = 5;
        long totalTime = 0;

        for (int i = 0; i < numberOfReports; i++) {
            ReportRequestDto request = new ReportRequestDto();
            request.setEmployeeIds(mediumEmployeeIdList);
            request.setReportType("EMPLOYEE_DEMOGRAPHICS");

            LocalDateTime startTime = LocalDateTime.now();
            
            // generateReport now returns Report directly (initial QUEUED report)
            Report report = reportService.generateReport(request);
            
            // Wait for async processing to complete by polling status
            report = waitForReportCompletion(report.getId(), 10); // 10 second timeout
            
            LocalDateTime endTime = LocalDateTime.now();
            Duration duration = Duration.between(startTime, endTime);
            
            totalTime += duration.toMillis();

            assertEquals(Report.ReportStatus.COMPLETED, report.getStatus());
            assertTrue(duration.toSeconds() <= 5, 
                      String.format("Report %d took %d seconds, exceeding 5 second limit", 
                                   i + 1, duration.toSeconds()));
        }

        double averageTime = (double) totalTime / numberOfReports;
        System.out.printf("Average report generation time under load: %.2f ms%n", averageTime);
          // Average time should still be reasonable
        assertTrue(averageTime < 5000, 
                  String.format("Average report generation time %.2f ms is too high", averageTime));
    }

    /**
     * Helper method to wait for report completion by polling the status
     * @param reportId The ID of the report to wait for
     * @param timeoutSeconds Maximum time to wait in seconds
     * @return The completed report
     * @throws Exception if timeout exceeded or report failed
     */
    private Report waitForReportCompletion(String reportId, int timeoutSeconds) throws Exception {
        long startTime = System.currentTimeMillis();
        long timeoutMillis = timeoutSeconds * 1000L;
        
        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            Report report = reportService.getReportStatus(reportId);
            
            if (report.getStatus() == Report.ReportStatus.COMPLETED) {
                return report;
            } else if (report.getStatus() == Report.ReportStatus.FAILED) {
                throw new RuntimeException("Report generation failed: " + report.getErrorMessage());
            }
            
            // Wait 100ms before polling again
            Thread.sleep(100);
        }
        
        throw new RuntimeException("Report generation timed out after " + timeoutSeconds + " seconds");
    }
}
