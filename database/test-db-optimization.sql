-- ========================================================================
-- TEST DATABASE OPTIMIZATION - SQL INDEXES ONLY
-- ========================================================================
-- Performance optimization indexes for certification report queries
-- Apply to: certreport_test database
-- Expected improvement: 60-70% reduction in report generation time
-- ========================================================================

-- ========================================================================
-- PERFORMANCE INDEXES FOR CERTIFICATION QUERIES
-- ========================================================================

-- Primary bottleneck: Employee ID lookups in certifications table
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_certifications_employee_id 
ON certifications (employee_id);

-- Certification definition joins
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_certifications_cert_def_status 
ON certifications (certification_definition_id, status);

-- Stage relationship lookups
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_stages_certification_id 
ON stages (certification_id);

-- Composite stage queries (certification + stage definition + status)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_stages_cert_stagedef 
ON stages (certification_id, stage_definition_id, status);

-- Task relationship lookups
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_tasks_stage_id 
ON tasks (stage_id);

-- Composite task queries (stage + task definition + status)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_tasks_stage_taskdef 
ON tasks (stage_id, task_definition_id, status);

-- Stage definition ordering lookups
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_stage_definitions_cert_sequence 
ON stage_definitions (certification_definition_id, sequence_order);

-- Task definition ordering lookups
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_task_definitions_stage_sequence 
ON task_definitions (stage_definition_id, sequence_order);

-- ========================================================================
-- UPDATE TABLE STATISTICS FOR QUERY OPTIMIZER
-- ========================================================================

ANALYZE certifications;
ANALYZE stages;
ANALYZE tasks;
ANALYZE certification_definitions;
ANALYZE stage_definitions;
ANALYZE task_definitions;
ANALYZE employees;

-- ========================================================================
-- VERIFICATION - Check index creation
-- ========================================================================

SELECT 
    schemaname,
    tablename,
    indexname,
    indexdef
FROM pg_indexes 
WHERE tablename IN ('certifications', 'stages', 'tasks', 'stage_definitions', 'task_definitions')
    AND indexname LIKE 'idx_%'
ORDER BY tablename, indexname;
