package com.certreport.test;

import com.certreport.dto.CompleteReportDataDto;
import com.certreport.dto.ReportRequestDto;
import com.certreport.model.Report;
import com.certreport.service.CertificationService;
import com.certreport.service.ReportService;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManagerFactory;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class QueryPerformanceTest {

    @Autowired
    private ReportService reportService;
      @Autowired
    private CertificationService certificationService;
    
    // @Autowired
    // private EmployeeService employeeService;
    
    @Autowired
    private EntityManagerFactory entityManagerFactory;
    
    @Test
    public void analyzeQueryPerformance() {
        // Enable Hibernate statistics
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        Statistics statistics = sessionFactory.getStatistics();
        statistics.clear();
        statistics.setStatisticsEnabled(true);
        
        System.out.println("=== STARTING QUERY PERFORMANCE ANALYSIS ===");
        
        // Test 1: Check current batching approach
        System.out.println("\n--- Test 1: Current batching approach ---");
        long startTime = System.currentTimeMillis();
        
        // Get a small sample of employee IDs
        List<String> sampleEmployeeIds = Arrays.asList("1", "2", "3", "4", "5");
        List<CompleteReportDataDto> batchData = certificationService.getCertificationDataChunk(sampleEmployeeIds);
        
        long batchTime = System.currentTimeMillis() - startTime;
        
        System.out.println("Batch approach results:");
        System.out.println("- Time: " + batchTime + "ms");
        System.out.println("- Employees processed: " + batchData.size());
        System.out.println("- Queries executed: " + statistics.getQueryExecutionCount());
        System.out.println("- Query cache hits: " + statistics.getQueryCacheHitCount());
        System.out.println("- Query cache misses: " + statistics.getQueryCacheMissCount());
        System.out.println("- Entity fetch count: " + statistics.getEntityFetchCount());
        System.out.println("- Collection fetch count: " + statistics.getCollectionFetchCount());
        
        // Clear statistics for next test
        statistics.clear();
        
        // Test 2: Compare with individual queries (old approach)
        System.out.println("\n--- Test 2: Individual query approach (for comparison) ---");
        startTime = System.currentTimeMillis();
        
        for (String employeeId : sampleEmployeeIds) {
            certificationService.getCertificationsByEmployeeId(employeeId);
        }
        
        long individualTime = System.currentTimeMillis() - startTime;
        
        System.out.println("Individual query approach results:");
        System.out.println("- Time: " + individualTime + "ms");
        System.out.println("- Queries executed: " + statistics.getQueryExecutionCount());
        System.out.println("- Entity fetch count: " + statistics.getEntityFetchCount());
        System.out.println("- Collection fetch count: " + statistics.getCollectionFetchCount());
        
        // Performance comparison
        System.out.println("\n--- Performance Comparison ---");
        System.out.println("Batch approach time: " + batchTime + "ms");
        System.out.println("Individual queries time: " + individualTime + "ms");
        if (individualTime > 0) {
            double improvement = ((double)(individualTime - batchTime) / individualTime) * 100;
            System.out.println("Performance improvement: " + String.format("%.1f", improvement) + "%");
        }
        
        // Clear statistics for final test
        statistics.clear();
        
        // Test 3: Test chunked processing
        System.out.println("\n--- Test 3: Chunked processing ---");
        startTime = System.currentTimeMillis();
        
        List<CompleteReportDataDto> chunkedData = certificationService.getAllCertificationDataChunked(10);
        
        long chunkedTime = System.currentTimeMillis() - startTime;
        
        System.out.println("Chunked processing results:");
        System.out.println("- Time: " + chunkedTime + "ms");
        System.out.println("- Total employees processed: " + chunkedData.size());
        System.out.println("- Queries executed: " + statistics.getQueryExecutionCount());
        System.out.println("- Entity fetch count: " + statistics.getEntityFetchCount());
        System.out.println("- Collection fetch count: " + statistics.getCollectionFetchCount());
        
        System.out.println("\n=== QUERY PERFORMANCE ANALYSIS COMPLETE ===");
        
        // Verify data was retrieved
        assertNotNull(batchData);
        assertTrue(batchData.size() > 0);
    }
    
    @Test
    public void analyzeReportGenerationBottlenecks() {
        System.out.println("=== ANALYZING REPORT GENERATION BOTTLENECKS ===");
        
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
        
        System.out.println("Report generation analysis (5 employees):");
        System.out.println("- Total time: " + totalTime + "ms");
        System.out.println("- Report status: " + report.getStatus());
        System.out.println("- Total queries executed: " + statistics.getQueryExecutionCount());
        System.out.println("- Entity loads: " + statistics.getEntityLoadCount());
        System.out.println("- Entity fetch count: " + statistics.getEntityFetchCount());
        System.out.println("- Collection fetch count: " + statistics.getCollectionFetchCount());
        System.out.println("- Second level cache hits: " + statistics.getSecondLevelCacheHitCount());
        System.out.println("- Second level cache misses: " + statistics.getSecondLevelCacheMissCount());
        
        if (report.getPageCount() != null) {
            System.out.println("- Pages generated: " + report.getPageCount());
            if (report.getPageCount() > 0) {
                System.out.println("- Time per page: " + (totalTime / report.getPageCount()) + "ms");
            }
        }
        
        System.out.println("=== BOTTLENECK ANALYSIS COMPLETE ===");
        
        assertNotNull(report);
    }
}
