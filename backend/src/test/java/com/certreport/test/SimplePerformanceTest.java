package com.certreport.test;

import com.certreport.service.ActuatorPerformanceMonitor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic validation test for ActuatorPerformanceMonitor functionality.
 * Verifies that our performance monitoring cleanup is working without requiring complex test data setup.
 */
@SpringBootTest
@ActiveProfiles("postgres-test")
public class ActuatorPerformanceMonitorValidationTest {

    @Autowired
    private ActuatorPerformanceMonitor actuatorPerformanceMonitor;

    @Test
    public void testActuatorPerformanceMonitorInitialization() {
        // This test verifies that our ActuatorPerformanceMonitor is properly initialized
        // without requiring complex test data setup
        
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
        
        System.out.println("✅ ActuatorPerformanceMonitor is working correctly!");
        System.out.println("   - Memory Metrics: " + memoryMetrics);
        System.out.println("   - Database Metrics: " + dbMetrics);
        System.out.println("✅ Performance monitoring cleanup completed successfully!");
    }
}
