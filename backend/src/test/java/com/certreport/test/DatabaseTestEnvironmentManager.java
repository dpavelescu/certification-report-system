package com.certreport.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Comprehensive test environment manager for performance and integration tests.
 * Handles database state management, optimization indexes, test data setup, and cleanup.
 * 
 * Primary responsibilities:
 * - Clean environment setup/teardown
 * - Performance test data creation
 * - Database optimization index management  
 * - Cross-test contamination prevention
 */
@Component
public class DatabaseTestEnvironmentManager {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private SqlBasedTestDataSetup sqlBasedTestDataSetup;
    
    /**
     * Ensures a completely clean test environment
     * Should be called at the beginning of tests that need isolation
     */
    @Transactional
    public void ensureCleanEnvironment() {
        System.out.println("🧹 Ensuring clean test environment...");
        
        // 1. Clear all test data
        sqlBasedTestDataSetup.clearAllData();
        
        // 2. Remove any optimization indexes (in case previous tests added them)
        removeOptimizationIndexes();
          // 3. Allow JVM to manage memory naturally for optimal performance
        // Note: Manual GC removed - JVM handles memory management efficiently
        
        System.out.println("✅ Clean test environment ready");
    }
    
    /**
     * Ensures test data exists for performance testing
     * Creates data only if not already present
     */
    public void ensurePerformanceTestData() {
        DatabaseState state = getDatabaseState();
        
        if (state.employeeCount < 300) {
            System.out.println("📊 Creating performance test data...");
            sqlBasedTestDataSetup.createPerformanceTestDataset();
            System.out.println("✅ Performance test data ready");
        } else {
            System.out.println("✅ Performance test data already present");
        }
    }
    
    /**
     * Applies database optimization indexes for performance testing
     */
    public void applyOptimizationIndexes() {
        System.out.println("🚀 Applying database optimization indexes...");
        
        try {
            // Certification-related indexes
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_cert_employee_id ON certifications(employee_id)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_cert_def_id ON certifications(certification_definition_id)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_cert_status ON certifications(status)");
            
            // Stage-related indexes  
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_stage_cert_id ON stages(certification_id)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_stage_def_id ON stages(stage_definition_id)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_stage_status ON stages(status)");
            
            // Task-related indexes
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_task_stage_id ON tasks(stage_id)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_task_def_id ON tasks(task_definition_id)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_task_status ON tasks(status)");
            
            // Composite indexes for common join patterns
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_cert_employee_status ON certifications(employee_id, status)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_stage_cert_status ON stages(certification_id, status)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_task_stage_status ON tasks(stage_id, status)");
            
            System.out.println("✅ Database optimization indexes applied");
            
        } catch (Exception e) {
            System.err.println("❌ Failed to apply optimization indexes: " + e.getMessage());
            throw new RuntimeException("Database optimization failed", e);
        }
    }
    
    /**
     * Removes database optimization indexes to restore clean state
     */
    public void removeOptimizationIndexes() {
        System.out.println("🧹 Removing database optimization indexes...");
        
        String[] indexes = {
            "idx_cert_employee_id", "idx_cert_def_id", "idx_cert_status",
            "idx_stage_cert_id", "idx_stage_def_id", "idx_stage_status", 
            "idx_task_stage_id", "idx_task_def_id", "idx_task_status",
            "idx_cert_employee_status", "idx_stage_cert_status", "idx_task_stage_status"
        };
        
        for (String index : indexes) {
            try {
                jdbcTemplate.execute("DROP INDEX IF EXISTS " + index);
            } catch (Exception e) {
                // Continue even if some indexes don't exist
                System.out.println("Note: Index " + index + " not found (this is normal)");
            }
        }
        
        System.out.println("✅ Database optimization indexes removed");
    }
    
    /**
     * Gets current database state for verification
     */
    public DatabaseState getDatabaseState() {
        DatabaseState state = new DatabaseState();
        
        try {
            state.employeeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM employees", Integer.class);
            state.certificationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM certifications", Integer.class);
            state.indexCount = getIndexCount();
            
        } catch (Exception e) {
            System.err.println("Warning: Could not retrieve database state: " + e.getMessage());
        }
        
        return state;
    }
    
    /**
     * Gets count of optimization indexes currently present
     */
    private int getIndexCount() {
        try {
            List<String> indexNames = jdbcTemplate.queryForList(
                "SELECT indexname FROM pg_indexes WHERE tablename IN ('certifications', 'stages', 'tasks') " +
                "AND indexname LIKE 'idx_%'", String.class);
            return indexNames.size();
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Verifies that the test environment is in the expected state
     */
    public void verifyCleanState() {
        DatabaseState state = getDatabaseState();
        
        if (state.employeeCount > 0 || state.certificationCount > 0) {
            throw new IllegalStateException(
                "Test environment is not clean! Found " + state.employeeCount + 
                " employees and " + state.certificationCount + " certifications");
        }
        
        if (state.indexCount > 0) {
            System.out.println("Warning: Found " + state.indexCount + " optimization indexes. Removing...");
            removeOptimizationIndexes();
        }
        
        System.out.println("✅ Test environment verified clean");
    }
    
    /**
     * Cleanup method to be called after tests that may have contaminated the environment
     */
    @Transactional
    public void cleanup() {
        System.out.println("🧹 Performing test cleanup...");
        
        // Clear all data
        sqlBasedTestDataSetup.clearAllData();
        
        // Remove optimization indexes
        removeOptimizationIndexes();
        
        System.out.println("✅ Test cleanup completed");
    }
    
    /**
     * Represents the current state of the test database
     */
    public static class DatabaseState {
        public int employeeCount;
        public int certificationCount;
        public int indexCount;
        
        @Override
        public String toString() {
            return String.format("DatabaseState{employees=%d, certifications=%d, indexes=%d}", 
                               employeeCount, certificationCount, indexCount);
        }
    }
}
