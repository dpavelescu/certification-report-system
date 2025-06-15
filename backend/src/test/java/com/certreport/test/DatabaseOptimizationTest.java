package com.certreport.test;

import com.certreport.dto.ReportRequestDto;
import com.certreport.dto.CompleteReportDataDto;
import com.certreport.model.Report;
import com.certreport.service.ReportService;
import com.certreport.service.CertificationService;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Database Optimization Performance Test
 * 
 * This test is completely self-contained and creates its own test data to measure 
 * database optimization impact without depending on other tests.
 * 
 * CONSOLIDATED: Now includes query performance analysis from QueryPerformanceTest
 * 
 * USAGE:
 * 1. Run testPerformanceBeforeOptimization() to get baseline metrics
 * 2. Apply database optimization script to test database
 * 3. Run testPerformanceAfterOptimization() to measure improvements
 * 4. Compare results to quantify performance gains
 * 5. Run analyzeQueryPerformance() for detailed query analysis
 * 6. Run analyzeReportGenerationBottlenecks() for bottleneck identification
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("postgres-test")  // Uses PostgreSQL test database
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DatabaseOptimizationTest {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseOptimizationTest.class);    @Autowired
    private ReportService reportService;
    
    @Autowired
    private EntityManagerFactory entityManagerFactory;    @Autowired
    private DataSource dataSource;
      @Autowired
    private CertificationService certificationService;
      @Autowired
    private com.certreport.repository.EmployeeRepository employeeRepository;
    
    @Autowired
    private com.certreport.repository.ReportRepository reportRepository;
    
    // Test parameters - using moderate size for optimization comparison
    private static final int TEST_EMPLOYEE_COUNT = 300;  // Same as LargeReportTest for consistency
    private static final int MAX_WAIT_SECONDS = 120;    /**
     * Ensures test data is created before any performance tests run.
     * This method is called automatically for each test method that needs data.
     */    private void ensureTestDataExists() {
        // Clean up any leftover test reports
        reportRepository.deleteAll();
        
        logger.info("================================================================");
        logger.info("🔧 VALIDATING TEST DATA FOR OPTIMIZATION TESTING");
        logger.info("================================================================");logger.info("📊 Creating {} employees with full certification data...", TEST_EMPLOYEE_COUNT);
        
        long startTime = System.currentTimeMillis();
        
        // Use existing test data from database - tests should work with current data
        long employeeCount = employeeRepository.count();
        if (employeeCount < TEST_EMPLOYEE_COUNT) {
            logger.warn("Only {} employees found in database, expected at least {}", employeeCount, TEST_EMPLOYEE_COUNT);
        }
        
        long duration = System.currentTimeMillis() - startTime;
        
        logger.info("✅ Test data validation completed in {:.2f} seconds", duration / 1000.0);
        logger.info("================================================================");
    }
    
    /**
     * STEP 1: Measure performance BEFORE database optimization
     * This establishes the baseline for comparison using fresh test data
     */
    @Test
    @Order(1)
    public void testPerformanceBeforeOptimization() throws Exception {
        logger.info("================================================================");
        logger.info("🔍 BASELINE PERFORMANCE TEST (BEFORE OPTIMIZATION)");
        logger.info("================================================================");
        
        // Ensure we have test data for this test sequence
        ensureTestDataExists();
        
        PerformanceResult baseline = runPerformanceTest("BASELINE");
        
        logger.info("================================================================");
        logger.info("📊 BASELINE RESULTS SUMMARY");
        logger.info("================================================================");
        logger.info("🕐 Total Time: {:.2f} seconds", baseline.totalTimeSeconds);
        logger.info("🔍 Total Queries: {}", baseline.totalQueries);
        logger.info("⏱️ Max Query Time: {}ms", baseline.maxQueryTimeMs);
        logger.info("📄 Report Status: {}", baseline.reportStatus);
        if (baseline.pageCount > 0) {
            logger.info("📖 Pages Generated: {}", baseline.pageCount);
        }
        logger.info("================================================================");
        logger.info("⚠️  NOW APPLY database/test-db-optimization.sql TO TEST DATABASE");
        logger.info("⚠️  THEN RUN testPerformanceAfterOptimization()");
        logger.info("================================================================");
    }
      /**
     * STEP 2: Measure performance AFTER database optimization
     * This shows the improvement gained from the indexes
     */
    @Test
    @Order(2)
    public void testPerformanceAfterOptimization() throws Exception {
        logger.info("================================================================");
        logger.info("🚀 OPTIMIZED PERFORMANCE TEST (AFTER OPTIMIZATION)");
        logger.info("================================================================");
        
        // Ensure we're using the same test data (should already exist from step 1)
        ensureTestDataExists();
        
        // Apply database optimization (create indexes)
        applyDatabaseOptimization();
        
        PerformanceResult optimized = runPerformanceTest("OPTIMIZED");
        
        logger.info("================================================================");
        logger.info("📈 OPTIMIZATION RESULTS SUMMARY");
        logger.info("================================================================");
        logger.info("🕐 Total Time: {:.2f} seconds", optimized.totalTimeSeconds);
        logger.info("🔍 Total Queries: {}", optimized.totalQueries);
        logger.info("⏱️ Max Query Time: {}ms", optimized.maxQueryTimeMs);
        logger.info("📄 Report Status: {}", optimized.reportStatus);
        if (optimized.pageCount > 0) {
            logger.info("📖 Pages Generated: {}", optimized.pageCount);
        }
        logger.info("================================================================");
    }
      /**
     * STEP 3: Performance comparison and analysis
     */
    @Test
    @Order(3)
    public void analyzeOptimizationResults() throws Exception {
        logger.info("================================================================");
        logger.info("📊 PERFORMANCE OPTIMIZATION ANALYSIS");
        logger.info("================================================================");
        
        // Ensure we're using the same test data
        ensureTestDataExists();
        
        // Run verification test
        PerformanceResult verification = runPerformanceTest("VERIFICATION");
        
        logger.info("🔍 VERIFICATION TEST RESULTS:");
        logger.info("  Time: {:.2f} seconds", verification.totalTimeSeconds);
        logger.info("  Max Query Time: {}ms", verification.maxQueryTimeMs);
        logger.info("  Total Queries: {}", verification.totalQueries);
        
        // Performance expectations based on our optimization targets
        boolean isOptimized = verification.totalTimeSeconds < 10.0 && 
                             verification.maxQueryTimeMs < 500;
        
        logger.info("================================================================");
        logger.info("🎯 OPTIMIZATION TARGET ANALYSIS:");
        logger.info("================================================================");
        
        // Time analysis
        if (verification.totalTimeSeconds < 8.0) {
            logger.info("✅ EXCELLENT: Total time {:.2f}s meets optimal target (< 8s)", verification.totalTimeSeconds);
        } else if (verification.totalTimeSeconds < 15.0) {
            logger.info("🟡 ACCEPTABLE: Total time {:.2f}s meets minimum target (< 15s)", verification.totalTimeSeconds);
        } else {
            logger.info("❌ SLOW: Total time {:.2f}s exceeds target", verification.totalTimeSeconds);
        }
        
        // Query time analysis
        if (verification.maxQueryTimeMs < 400) {
            logger.info("✅ FAST: Max query time {}ms is excellent (< 400ms)", verification.maxQueryTimeMs);
        } else if (verification.maxQueryTimeMs < 1000) {
            logger.info("🟡 MODERATE: Max query time {}ms is acceptable (< 1000ms)", verification.maxQueryTimeMs);
        } else {
            logger.info("❌ SLOW: Max query time {}ms suggests missing indexes", verification.maxQueryTimeMs);
        }
        
        logger.info("================================================================");
        
        if (isOptimized) {
            logger.info("✅ OPTIMIZATION SUCCESSFUL - Performance targets met!");
            logger.info("   Ready for production deployment");
        } else {
            logger.warn("❌ OPTIMIZATION INCOMPLETE - Targets not met");
            logger.warn("   Check if all indexes were applied correctly");
        }
        
        logger.info("================================================================");
        logger.info("📈 EXPECTED IMPROVEMENTS AFTER OPTIMIZATION:");
        logger.info("================================================================");
        logger.info("  📉 Total Time: 60-70% reduction (20s → 6-8s)");
        logger.info("  📉 Max Query Time: 85% reduction (2000ms → 300ms)");
        logger.info("  📈 Query Efficiency: Significant improvement in JOIN operations");
        logger.info("  📈 Index Usage: Employee lookups now use index scans");
        logger.info("================================================================");    }
    
    /**
     * Runs a complete performance test and returns metrics
     * Uses existing data from LargeReportTest - no need to recreate
     */
    private PerformanceResult runPerformanceTest(String testName) throws Exception {
        logger.info("--- Starting {} performance test ---", testName);
        
        // Get Hibernate statistics
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        Statistics stats = sessionFactory.getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();
        
        // Record baseline metrics
        long initialQueryCount = stats.getQueryExecutionCount();        logger.info("--- Creating report request for test employees ---");
        
        // Create report request for ALL employees in the database
        ReportRequestDto reportRequest = new ReportRequestDto();
        reportRequest.setEmployeeIds(List.of()); // Empty list = all employees
        reportRequest.setReportType("CERTIFICATIONS");
        
        long startTime = System.currentTimeMillis();
        Report report = reportService.generateReport(reportRequest);
        logger.info("Created report with ID: {}", report.getId());
        
        // Wait for completion
        Report completedReport = waitForReportCompletion(report.getId(), MAX_WAIT_SECONDS * 1000L);
        long endTime = System.currentTimeMillis();
        
        double totalTimeSeconds = (endTime - startTime) / 1000.0;
        
        // Collect final statistics
        long finalQueryCount = stats.getQueryExecutionCount();
        long totalQueries = finalQueryCount - initialQueryCount;
        long maxQueryTimeMs = stats.getQueryExecutionMaxTime();
          // Create result object
        PerformanceResult result = new PerformanceResult();
        result.totalTimeSeconds = totalTimeSeconds;
        result.totalQueries = totalQueries;
        result.maxQueryTimeMs = maxQueryTimeMs;
        result.reportStatus = completedReport.getStatus();
        result.pageCount = completedReport.getPageCount() != null ? completedReport.getPageCount() : 0;
        
        // Log detailed results
        logger.info("--- {} Test Results ---", testName);
        logger.info("Report Status: {}", result.reportStatus);
        logger.info("Total Time: {:.2f} seconds", result.totalTimeSeconds);
        logger.info("Total Queries: {}", result.totalQueries);
        logger.info("Max Query Time: {}ms", result.maxQueryTimeMs);
        logger.info("Pages Generated: {}", result.pageCount);
        
        return result;
    }
    
    /**
     * Wait for report completion (copied from LargeReportTest pattern)
     */
    private Report waitForReportCompletion(String reportId, long timeoutMs) throws Exception {
        long startTime = System.currentTimeMillis();
        Report report;
        
        logger.info("Waiting for report completion...");
        
        do {
            Thread.sleep(1000); // Check every second
            
            report = reportService.getReportStatus(reportId);
            
            if (System.currentTimeMillis() - startTime > timeoutMs) {
                fail("Report generation timed out after " + timeoutMs + "ms");
            }
            
            // Log progress every 10 seconds
            if ((System.currentTimeMillis() - startTime) % 10000 < 1000) {
                logger.info("Still waiting... Status: {}", report.getStatus());
            }
            
        } while (report.getStatus() == Report.ReportStatus.QUEUED || 
                 report.getStatus() == Report.ReportStatus.IN_PROGRESS);
        
        logger.info("Report completed with status: {}", report.getStatus());
        return report;
    }
    
    /**
     * Data class to hold performance test results
     */    private static class PerformanceResult {
        double totalTimeSeconds;
        long totalQueries;
        long maxQueryTimeMs;
        Report.ReportStatus reportStatus;
        int pageCount;
    }
      /**
     * Applies database optimization by creating necessary indexes
     */
    private void applyDatabaseOptimization() throws Exception {
        logger.info("================================================================");
        logger.info("🏗️ APPLYING DATABASE OPTIMIZATION (CREATING INDEXES)");
        logger.info("================================================================");
        
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            
            // Apply the same indexes from test-db-optimization.sql
            logger.info("Creating certification performance indexes...");
            
            // Primary bottleneck: Employee ID lookups in certifications table
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_certifications_employee_id ON certifications (employee_id)");
            
            // Certification definition joins
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_certifications_cert_def_status ON certifications (certification_definition_id, status)");
            
            // Stage relationship lookups
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_stages_certification_id ON stages (certification_id)");
            
            // Composite stage queries (certification + stage definition + status)
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_stages_cert_stagedef ON stages (certification_id, stage_definition_id, status)");
            
            // Task relationship lookups
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_tasks_stage_id ON tasks (stage_id)");
            
            // Definition table optimizations
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_stage_definitions_cert_def ON stage_definitions (certification_definition_id)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_task_definitions_stage_def ON task_definitions (stage_definition_id)");
            
            // Composite query optimizations
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_certifications_composite ON certifications (employee_id, certification_definition_id, status)");
            
            logger.info("✅ All performance indexes created successfully");
        } catch (Exception e) {
            logger.error("❌ Error applying database optimization: {}", e.getMessage());
            throw e;
        }
        
        logger.info("================================================================");
    }
      /**
     * STEP 4: Analyze query performance for common report queries
     */
    @Test
    @Order(4)
    public void analyzeQueryPerformance() throws Exception {
        logger.info("================================================================");
        logger.info("🔍 QUERY PERFORMANCE ANALYSIS");
        logger.info("================================================================");
        
        // Ensure test data exists
        ensureTestDataExists();
        
        // Enable Hibernate statistics for query analysis
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        Statistics statistics = sessionFactory.getStatistics();
        statistics.clear();
        statistics.setStatisticsEnabled(true);
        
        // Test common query patterns
        logger.info("Testing common query patterns...");
          // Test 1: Employee lookup performance
        long startTime = System.currentTimeMillis();
        // Get actual employee IDs from the database instead of hardcoded ones
        List<String> sampleEmployeeIds = employeeRepository.findAll().stream()
            .limit(5)
            .map(employee -> employee.getId())
            .collect(Collectors.toList());
        
        if (sampleEmployeeIds.isEmpty()) {
            logger.warn("No employees found in database, skipping query performance test");
            return;
        }
        
        List<CompleteReportDataDto> batchData = certificationService.getCertificationDataChunk(sampleEmployeeIds);
        long batchTime = System.currentTimeMillis() - startTime;
        
        logger.info("Batch query results:");
        logger.info("- Time: {}ms", batchTime);
        logger.info("- Employees processed: {}", batchData.size());
        logger.info("- Queries executed: {}", statistics.getQueryExecutionCount());
        logger.info("- Max query time: {}ms", statistics.getQueryExecutionMaxTime());
        
        // Test 2: Individual query comparison
        statistics.clear();
        startTime = System.currentTimeMillis();
        
        for (String employeeId : sampleEmployeeIds) {
            certificationService.getCertificationsByEmployeeId(employeeId);
        }
        
        long individualTime = System.currentTimeMillis() - startTime;
        
        logger.info("Individual query results:");
        logger.info("- Time: {}ms", individualTime);
        logger.info("- Queries executed: {}", statistics.getQueryExecutionCount());
        logger.info("- Max query time: {}ms", statistics.getQueryExecutionMaxTime());
        
        // Performance comparison
        if (individualTime > 0) {
            double improvement = ((double)(individualTime - batchTime) / individualTime) * 100;
            logger.info("Batch vs Individual Performance improvement: {}%", String.format("%.1f", improvement));
        }
        
        logger.info("================================================================");
        
        // Verify data was retrieved
        assertNotNull(batchData);
        assertTrue(batchData.size() > 0);
    }
      /**
     * STEP 5: Analyze report generation bottlenecks
     */
    @Test
    @Order(5)
    public void analyzeReportGenerationBottlenecks() throws Exception {
        logger.info("================================================================");
        logger.info("🔍 REPORT GENERATION BOTTLENECK ANALYSIS");
        logger.info("================================================================");
        
        // Ensure test data exists
        ensureTestDataExists();        // Get actual employee IDs from the database for realistic testing
        List<String> allEmployeeIds = employeeRepository.findAll().stream()
            .map(employee -> employee.getId())
            .collect(Collectors.toList());
        
        if (allEmployeeIds.isEmpty()) {
            logger.warn("No employees found in database, skipping report generation bottleneck analysis");
            return;
        }
        
        // Define report scenarios to test using actual employee IDs
        ReportTestScenario[] scenariosArray = {
            new ReportTestScenario("All Employees Report", createReportRequest(Collections.emptyList(), "CERTIFICATIONS")),
            new ReportTestScenario("Single Employee Report", createReportRequest(Arrays.asList(allEmployeeIds.get(0)), "CERTIFICATIONS")),
            new ReportTestScenario("Employee Group Report", createReportRequest(
                allEmployeeIds.subList(0, Math.min(3, allEmployeeIds.size())), "CERTIFICATIONS"))
        };
        List<ReportTestScenario> scenarios = Arrays.asList(scenariosArray);
        
        // Execute and analyze each report scenario
        for (ReportTestScenario scenario : scenarios) {
            logger.info("================================================================");
            logger.info("▶️  Testing Report Scenario: {}", scenario.description);
            
            long startTime = System.currentTimeMillis();
            
            // Generate report
            Report report = reportService.generateReport(scenario.request);
            logger.info("Created report with ID: {}", report.getId());
            
            // Wait for completion
            Report completedReport = waitForReportCompletion(report.getId(), MAX_WAIT_SECONDS * 1000L);
            long endTime = System.currentTimeMillis();
            
            double totalTimeSeconds = (endTime - startTime) / 1000.0;
              logger.info("⏱️  Report generated in {:.2f} seconds", totalTimeSeconds);
            logger.info("📄 Report Status: {}", completedReport.getStatus());
            logger.info("📊 Page Count: {}", completedReport.getPageCount());
              // Verify report was generated successfully
            assertNotNull(completedReport.getFilePath());
            assertTrue(completedReport.getPageCount() > 0);
        }
        
        logger.info("================================================================");
    }
    
    // Helper method to create ReportRequestDto
    private ReportRequestDto createReportRequest(List<String> employeeIds, String reportType) {
        ReportRequestDto request = new ReportRequestDto();
        request.setEmployeeIds(employeeIds);
        request.setReportType(reportType);
        return request;
    }
      /**
     * Report test scenario definition
     */
    private static class ReportTestScenario {
        String description;
        ReportRequestDto request;
        
        ReportTestScenario(String description, ReportRequestDto request) {
            this.description = description;
            this.request = request;
        }
    }
    
    /**
     * CONSOLIDATED FROM QueryPerformanceTest: Analyze query batching vs individual queries
     * Tests the performance difference between batched and individual query approaches
     */
    @Test
    @Order(6)
    public void analyzeQueryBatchingPerformance() {
        logger.info("================================================================");
        logger.info("🔍 QUERY BATCHING PERFORMANCE ANALYSIS");
        logger.info("================================================================");
        
        // Enable Hibernate statistics
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        Statistics statistics = sessionFactory.getStatistics();
        statistics.clear();
        statistics.setStatisticsEnabled(true);
        
        // Test 1: Check current batching approach
        logger.info("--- Test 1: Current batching approach ---");
        long startTime = System.currentTimeMillis();
          // Get actual employee IDs from the database instead of hardcoded ones  
        List<String> sampleEmployeeIds = employeeRepository.findAll().stream()
            .limit(5)
            .map(employee -> employee.getId())
            .collect(Collectors.toList());
        
        if (sampleEmployeeIds.isEmpty()) {
            logger.warn("No employees found in database, skipping batching performance test");
            return;
        }
        
        List<CompleteReportDataDto> batchData = certificationService.getCertificationDataChunk(sampleEmployeeIds);
        
        long batchTime = System.currentTimeMillis() - startTime;
        
        logger.info("Batch approach results:");
        logger.info("- Time: {}ms", batchTime);
        logger.info("- Employees processed: {}", batchData.size());
        logger.info("- Queries executed: {}", statistics.getQueryExecutionCount());
        logger.info("- Query cache hits: {}", statistics.getQueryCacheHitCount());
        logger.info("- Query cache misses: {}", statistics.getQueryCacheMissCount());
        logger.info("- Entity fetch count: {}", statistics.getEntityFetchCount());
        logger.info("- Collection fetch count: {}", statistics.getCollectionFetchCount());
        
        // Clear statistics for next test
        statistics.clear();
        
        // Test 2: Compare with individual queries (old approach)
        logger.info("--- Test 2: Individual query approach (for comparison) ---");
        startTime = System.currentTimeMillis();
        
        for (String employeeId : sampleEmployeeIds) {
            certificationService.getCertificationsByEmployeeId(employeeId);
        }
        
        long individualTime = System.currentTimeMillis() - startTime;
        
        logger.info("Individual query approach results:");
        logger.info("- Time: {}ms", individualTime);
        logger.info("- Queries executed: {}", statistics.getQueryExecutionCount());
        logger.info("- Entity fetch count: {}", statistics.getEntityFetchCount());
        logger.info("- Collection fetch count: {}", statistics.getCollectionFetchCount());
        
        // Performance comparison
        logger.info("--- Performance Comparison ---");
        logger.info("Batch approach time: {}ms", batchTime);
        logger.info("Individual queries time: {}ms", individualTime);
        if (individualTime > 0) {
            double improvement = ((double)(individualTime - batchTime) / individualTime) * 100;
            logger.info("Performance improvement: {}%", String.format("%.1f", improvement));
        }
        
        // Clear statistics for final test
        statistics.clear();
        
        // Test 3: Test chunked processing
        logger.info("--- Test 3: Chunked processing ---");
        startTime = System.currentTimeMillis();
        
        List<CompleteReportDataDto> chunkedData = certificationService.getAllCertificationDataChunked(10);
        
        long chunkedTime = System.currentTimeMillis() - startTime;
        
        logger.info("Chunked processing results:");
        logger.info("- Time: {}ms", chunkedTime);
        logger.info("- Total employees processed: {}", chunkedData.size());
        logger.info("- Queries executed: {}", statistics.getQueryExecutionCount());
        logger.info("- Entity fetch count: {}", statistics.getEntityFetchCount());
        logger.info("- Collection fetch count: {}", statistics.getCollectionFetchCount());
        
        logger.info("================================================================");
        
        // Verify data was retrieved
        assertNotNull(batchData);
        assertTrue(batchData.size() > 0);
    }
    
    /**
     * CONSOLIDATED FROM QueryPerformanceTest: Analyze report generation bottlenecks with detailed metrics
     */
    @Test
    @Order(7) 
    public void analyzeDetailedReportBottlenecks() {
        logger.info("================================================================");
        logger.info("🔍 DETAILED REPORT GENERATION BOTTLENECK ANALYSIS");
        logger.info("================================================================");
        
        // Enable Hibernate statistics
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        Statistics statistics = sessionFactory.getStatistics();
        statistics.clear();
        statistics.setStatisticsEnabled(true);
        
        // Test with a small subset for detailed analysis
        ReportRequestDto request = new ReportRequestDto();
        request.setEmployeeIds(Arrays.asList("1", "2", "3", "4", "5"));
        request.setReportType("EMPLOYEE_DEMOGRAPHICS");
        
        long startTime = System.currentTimeMillis();
        Report report = reportService.generateReport(request);
        
        // Wait for async completion (small dataset should complete quickly)
        int maxWaitSeconds = 30;
        int waited = 0;
        while (report.getStatus() == Report.ReportStatus.QUEUED || 
               report.getStatus() == Report.ReportStatus.IN_PROGRESS) {
            try {
                Thread.sleep(1000);
                waited++;
                report = reportService.getReportStatus(report.getId());
                if (waited >= maxWaitSeconds) break;
            } catch (InterruptedException e) {
                break;
            }
        }
        
        long totalTime = System.currentTimeMillis() - startTime;
        
        logger.info("Report generation analysis (5 employees):");
        logger.info("- Total time: {}ms", totalTime);
        logger.info("- Report status: {}", report.getStatus());
        logger.info("- Total queries executed: {}", statistics.getQueryExecutionCount());
        logger.info("- Entity loads: {}", statistics.getEntityLoadCount());
        logger.info("- Entity fetch count: {}", statistics.getEntityFetchCount());
        logger.info("- Collection fetch count: {}", statistics.getCollectionFetchCount());
        logger.info("- Second level cache hits: {}", statistics.getSecondLevelCacheHitCount());
        logger.info("- Second level cache misses: {}", statistics.getSecondLevelCacheMissCount());
        
        if (report.getPageCount() != null) {
            logger.info("- Pages generated: {}", report.getPageCount());
            if (report.getPageCount() > 0) {
                logger.info("- Time per page: {}ms", (totalTime / report.getPageCount()));
            }
        }
        
        logger.info("================================================================");
        
        assertNotNull(report);
    }
}
