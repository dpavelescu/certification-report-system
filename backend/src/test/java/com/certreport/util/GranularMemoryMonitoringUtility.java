package com.certreport.util;

import com.certreport.model.Report;
import com.certreport.repository.ReportRepository;
import com.certreport.service.ActuatorPerformanceMonitor;
import com.certreport.service.ReportService;
import com.certreport.dto.ReportRequestDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;

/**
 * Utility for granular memory monitoring - tracks data processing memory usage using ActuatorPerformanceMonitor
 * 
 * This utility demonstrates how to use ActuatorPerformanceMonitor for detailed memory analysis
 * and provides granular insights into memory usage patterns during report generation.
 * 
 * MOVED FROM TEST PACKAGE - This is not a test but a utility for memory analysis.
 * 
 * Usage:
 * - Can be run independently to analyze memory usage for specific employee subsets
 * - Provides detailed memory breakdown: data processing vs framework overhead
 * - Demonstrates memory scaling patterns and efficiency metrics
 * - Integrates with the comprehensive performance monitoring architecture
 * 
 * Related Documentation:
 * - See pdf-generation-process.md for complete memory management architecture
 * - See performance-summary.md for key memory efficiency metrics
 * - See technical-specifications.md for NFR implementation details
 */
@Component
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("postgres-test")
public class GranularMemoryMonitoringUtility {

    @Autowired
    private ReportService reportService;    @Autowired
    private ReportRepository reportRepository;

    @SuppressWarnings("unused")
    @Autowired
    private ActuatorPerformanceMonitor actuatorPerformanceMonitor;

    /**
     * Monitors granular memory usage for a specific employee subset
     * This demonstrates how to use ActuatorPerformanceMonitor for detailed memory analysis
     */
    public void monitorGranularMemoryUsage() throws Exception {
        System.out.println("=== GRANULAR MEMORY MONITORING UTILITY ===");
        System.out.println("This utility isolates memory usage for data processing vs framework overhead");
        System.out.println();

        // Create report request for a subset of employees
        ReportRequestDto request = new ReportRequestDto();
        request.setReportType("CERTIFICATIONS");
        
        // Use first 50 employees for focused testing
        request.setEmployeeIds(Arrays.asList(
            "1", "2", "3", "4", "5", "6", "7", "8", "9", "10",
            "11", "12", "13", "14", "15", "16", "17", "18", "19", "20",
            "21", "22", "23", "24", "25", "26", "27", "28", "29", "30",
            "31", "32", "33", "34", "35", "36", "37", "38", "39", "40",
            "41", "42", "43", "44", "45", "46", "47", "48", "49", "50"
        ));

        System.out.println("📊 MONITORING WITH 50 EMPLOYEES");
        System.out.println("Expected data volume: ~1/6 of full dataset");
        System.out.println("This will help establish memory usage per employee");
        System.out.println();

        // Generate the report
        Report report = reportService.generateReport(request);
        System.out.println("Created report with ID: " + report.getId());

        // Wait for completion
        Report completedReport = waitForReportCompletion(report.getId(), 30000);

        System.out.println();
        System.out.println("=== GRANULAR MEMORY ANALYSIS RESULTS ===");
        System.out.println("Report Status: " + completedReport.getStatus());
        
        if (completedReport.getStatus() == Report.ReportStatus.COMPLETED) {
            System.out.println("✅ Report generated successfully");
            System.out.println("Pages: " + completedReport.getPageCount());
            
            // The granular memory details will be printed by ActuatorPerformanceMonitor
            // during report generation
            System.out.println();
            System.out.println("🔍 MEMORY USAGE BREAKDOWN:");
            System.out.println("(See detailed breakdown in logs above)");
            System.out.println();
            System.out.println("📈 SCALING ANALYSIS:");
            System.out.println("Based on 50-employee test results:");
            System.out.println("- Data processing memory per employee can be calculated");
            System.out.println("- PDF generation efficiency can be measured");
            System.out.println("- Framework overhead is isolated and measurable");
            
        } else {
            System.out.println("❌ Report generation failed: " + completedReport.getErrorMessage());
        }

        System.out.println();
        System.out.println("=== UTILITY COMPLETED ===");
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
                throw new RuntimeException("Test interrupted while waiting for report completion");
            }

            report = reportRepository.findById(reportId).orElse(null);

            if (System.currentTimeMillis() - startTime > timeoutMs) {
                throw new RuntimeException("Report generation timed out after " + timeoutMs + "ms");
            }

        } while (report == null || 
                 report.getStatus() == Report.ReportStatus.QUEUED || 
                 report.getStatus() == Report.ReportStatus.IN_PROGRESS);

        System.out.println("Report completed with status: " + report.getStatus());
        return report;
    }
}
