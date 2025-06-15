package com.certreport.test;

import com.certreport.service.ActuatorPerformanceMonitor;
import com.certreport.service.MemoryEfficientPdfGenerationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic validation test for performance monitoring services.
 * 
 * Ensures that core performance monitoring components are properly initialized
 * and basic functionality works without requiring complex test data setup.
 * 
 * This replaces the removed ActuatorPerformanceMonitorValidationTest with
 * a broader service validation approach.
 */
@SpringBootTest
@ActiveProfiles("postgres-test")
public class PerformanceServicesValidationTest {

    @Autowired
    private ActuatorPerformanceMonitor actuatorPerformanceMonitor;
    
    @Autowired
    private MemoryEfficientPdfGenerationService memoryEfficientPdfGenerationService;

    @Test
    public void testActuatorPerformanceMonitorInitialization() {
        // Verify ActuatorPerformanceMonitor is properly initialized
        assertNotNull(actuatorPerformanceMonitor, "ActuatorPerformanceMonitor should be autowired");
        
        // Test basic memory metrics collection
        ActuatorPerformanceMonitor.MemoryMetrics memoryMetrics = 
            actuatorPerformanceMonitor.getDetailedMemoryMetrics();
        
        assertNotNull(memoryMetrics, "Memory metrics should not be null");
        assertTrue(memoryMetrics.heapUsedMB >= 0, "Heap usage should be non-negative");
        assertTrue(memoryMetrics.heapMaxMB > 0, "Heap max should be positive");
        
        // Test database metrics collection
        ActuatorPerformanceMonitor.DatabaseMetrics dbMetrics = 
            actuatorPerformanceMonitor.getDatabaseMetrics();
        
        assertNotNull(dbMetrics, "Database metrics should not be null");
        assertTrue(dbMetrics.totalConnections >= 0, "Total connections should be non-negative");
        
        System.out.println("✅ ActuatorPerformanceMonitor validation completed");
        System.out.println("   - Memory Metrics: " + memoryMetrics);
        System.out.println("   - Database Metrics: " + dbMetrics);
    }
      @Test
    public void testMemoryEfficientPdfGenerationServiceInitialization() {
        // Verify MemoryEfficientPdfGenerationService is properly initialized
        assertNotNull(memoryEfficientPdfGenerationService, 
                     "MemoryEfficientPdfGenerationService should be autowired");
        
        // Test basic service availability (no specific configuration methods to test)
        System.out.println("✅ MemoryEfficientPdfGenerationService validation completed");
        System.out.println("   - Service initialized correctly");
        System.out.println("   - Service is available for dependency injection");
    }
    
    @Test
    public void testPerformanceMonitoringIntegration() {
        // Test integration between performance monitoring components
        
        // Start a mock performance monitoring session
        String testReportId = "validation-test-" + System.currentTimeMillis();
        
        var timerSample = actuatorPerformanceMonitor.startReportGeneration(testReportId, 1, 1);
        assertNotNull(timerSample, "Timer sample should be created");
        
        // Record some memory snapshots
        actuatorPerformanceMonitor.recordMemorySnapshot(testReportId, "Validation Test Start");
        actuatorPerformanceMonitor.recordMemorySnapshot(testReportId, "Validation Test End");
        
        // Complete the monitoring session
        var performanceReport = actuatorPerformanceMonitor.completeReportGeneration(
            timerSample, testReportId, 1, 1024);
        
        assertNotNull(performanceReport, "Performance report should be generated");
        assertTrue(performanceReport.durationMs >= 0, "Duration should be non-negative");
        
        System.out.println("✅ Performance monitoring integration validation completed");
        System.out.println("   - Mock monitoring session completed successfully");
        System.out.println("   - Performance report generated: " + performanceReport.durationMs + "ms");
    }
}
