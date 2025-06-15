-- Performance Test Data for Certification Report System
-- Optimized SQL script to quickly create 300+ employees with comprehensive certification data
-- This replaces the slow Java entity creation for performance testing

-- Clear existing test data in correct order to avoid constraint violations
DELETE FROM tasks;
DELETE FROM stages;  
DELETE FROM certifications;
DELETE FROM task_definitions;
DELETE FROM stage_definitions;
DELETE FROM certification_definitions;
DELETE FROM employees;

-- Reset sequences (PostgreSQL specific)
ALTER SEQUENCE IF EXISTS employees_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS certification_definitions_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS stage_definitions_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS task_definitions_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS certifications_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS stages_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS tasks_id_seq RESTART WITH 1;

-- =============================================================================
-- 1. CREATE 350 EMPLOYEES (for 300+ employee testing with margin)
-- =============================================================================

-- Using generate_series for efficient bulk creation
INSERT INTO employees (id, first_name, last_name, email, department, position, hire_date, created_at, updated_at)
SELECT 
    'PERF_EMP_' || LPAD(seq::text, 5, '0') as id,
    (ARRAY['Alex', 'Taylor', 'Jordan', 'Casey', 'Morgan', 'Riley', 'Avery', 'Cameron', 'Drew', 'Sage'])[1 + (seq % 10)] || seq as first_name,
    (ARRAY['Smith', 'Johnson', 'Williams', 'Brown', 'Jones', 'Garcia', 'Miller', 'Davis', 'Rodriguez', 'Martinez', 'Hernandez', 'Lopez', 'Gonzalez', 'Wilson', 'Anderson'])[1 + (seq % 15)] as last_name,
    'perf.employee' || seq || '@testcompany.com' as email,
    (ARRAY['Engineering', 'Data Science', 'DevOps', 'Security', 'QA', 'Product', 'Design', 'Marketing', 'Sales', 'Operations'])[1 + (seq % 10)] as department,
    (ARRAY['Junior', 'Senior', 'Lead', 'Principal', 'Manager', 'Director', 'Specialist', 'Analyst', 'Coordinator'])[1 + (seq % 9)] || ' ' || 
    (ARRAY['Engineer', 'Developer', 'Scientist', 'Designer', 'Manager', 'Analyst', 'Specialist'])[1 + (seq % 7)] as position,
    CURRENT_TIMESTAMP - INTERVAL '1 day' * (random() * 1095 + 30) as hire_date, -- Random hire date within last 3 years
    CURRENT_TIMESTAMP as created_at,
    CURRENT_TIMESTAMP as updated_at
FROM generate_series(1, 350) AS seq;

-- =============================================================================
-- 2. CREATE 12 CERTIFICATION DEFINITIONS (comprehensive coverage)
-- =============================================================================

INSERT INTO certification_definitions (id, name, description, category, validity_period_months, total_duration_hours, is_active, created_at, updated_at) VALUES
('CERT_DEF_001', 'Java Enterprise Development', 'Advanced Java enterprise development certification', 'Technical', 24, 120, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('CERT_DEF_002', 'Cloud Architecture Mastery', 'Cloud architecture and deployment strategies', 'Technical', 36, 150, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('CERT_DEF_003', 'DevOps Engineering Excellence', 'CI/CD, infrastructure automation, and monitoring', 'Technical', 24, 100, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('CERT_DEF_004', 'Data Science Fundamentals', 'Statistical analysis, machine learning, and data visualization', 'Technical', 18, 140, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('CERT_DEF_005', 'Cybersecurity Professional', 'Information security, threat analysis, and compliance', 'Security', 24, 130, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('CERT_DEF_006', 'UI/UX Design Specialist', 'User experience design and interface development', 'Design', 12, 80, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('CERT_DEF_007', 'Machine Learning Operations', 'MLOps, model deployment, and monitoring', 'Technical', 24, 110, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('CERT_DEF_008', 'Agile Project Management', 'Scrum, Kanban, and agile leadership', 'Leadership', 24, 90, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('CERT_DEF_009', 'API Design & Architecture', 'RESTful APIs, GraphQL, and microservices', 'Technical', 18, 95, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('CERT_DEF_010', 'Database Administration', 'Database design, optimization, and administration', 'Technical', 36, 125, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('CERT_DEF_011', 'Quality Assurance Engineering', 'Test automation, quality processes, and metrics', 'Quality', 24, 105, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('CERT_DEF_012', 'Leadership & Communication', 'Team leadership, communication, and management skills', 'Leadership', 12, 75, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- =============================================================================
-- 3. CREATE STAGE DEFINITIONS (5 stages per certification = 60 stages total)
-- =============================================================================

INSERT INTO stage_definitions (id, certification_definition_id, name, description, sequence_order, estimated_duration_hours, is_mandatory, created_at, updated_at)
SELECT 
    'STAGE_DEF_' || LPAD(((cert_seq - 1) * 5 + stage_seq)::text, 3, '0') as id,
    'CERT_DEF_' || LPAD(cert_seq::text, 3, '0') as certification_definition_id,
    (ARRAY['Foundation', 'Intermediate', 'Advanced', 'Mastery', 'Certification'])[stage_seq] as name,
    (ARRAY['Foundation', 'Intermediate', 'Advanced', 'Mastery', 'Certification'])[stage_seq] || ' level training and assessment' as description,
    stage_seq as sequence_order,
    20 + (stage_seq * 10) as estimated_duration_hours, -- 30, 40, 50, 60, 70 hours
    true as is_mandatory,
    CURRENT_TIMESTAMP as created_at,
    CURRENT_TIMESTAMP as updated_at
FROM generate_series(1, 12) AS cert_seq,
     generate_series(1, 5) AS stage_seq;

-- =============================================================================
-- 4. CREATE TASK DEFINITIONS (5 tasks per stage = 300 tasks total)
-- =============================================================================

INSERT INTO task_definitions (id, stage_definition_id, name, description, sequence_order, estimated_hours, task_type, is_mandatory, created_at, updated_at)
SELECT 
    'TASK_DEF_' || LPAD(((stage_num - 1) * 5 + task_seq)::text, 3, '0') as id,
    'STAGE_DEF_' || LPAD(stage_num::text, 3, '0') as stage_definition_id,
    (ARRAY['Theory Assessment', 'Practical Lab', 'Case Study', 'Code Review', 'Final Evaluation'])[task_seq] as name,
    'Complete ' || (ARRAY['theory assessment', 'practical lab exercise', 'case study analysis', 'code review session', 'final evaluation'])[task_seq] as description,
    task_seq as sequence_order,
    4 + (task_seq * 2) as estimated_hours, -- 6, 8, 10, 12, 14 hours
    (ARRAY['ASSESSMENT', 'PRACTICAL_EXERCISE', 'PROJECT', 'REVIEW', 'PRESENTATION'])[task_seq] as task_type,
    task_seq <= 3 as is_mandatory, -- First 3 tasks are mandatory
    CURRENT_TIMESTAMP as created_at,
    CURRENT_TIMESTAMP as updated_at
FROM generate_series(1, 60) AS stage_num,
     generate_series(1, 5) AS task_seq;

-- =============================================================================
-- 5. CREATE CERTIFICATIONS (3-4 per employee = ~1200 certifications)
-- =============================================================================

-- Each employee gets 3-4 certifications with realistic distribution
-- Skip every 20th employee (5% have no certifications)
INSERT INTO certifications (id, employee_id, certification_definition_id, status, enrolled_at, due_date, current_stage_sequence, completion_percentage, started_at, completed_at, created_at, updated_at)
SELECT 
    'CERT_' || LPAD(((emp_seq - 1) * 4 + cert_seq)::text, 6, '0') as id,
    'PERF_EMP_' || LPAD(emp_seq::text, 5, '0') as employee_id,
    'CERT_DEF_' || LPAD((((emp_seq - 1) * 4 + cert_seq - 1) % 12 + 1)::text, 3, '0') as certification_definition_id,    CASE 
        WHEN cert_seq = 1 THEN 'COMPLETED'
        WHEN cert_seq = 2 THEN 'IN_PROGRESS'
        WHEN cert_seq = 3 THEN 'IN_PROGRESS'
        ELSE 'NOT_STARTED'
    END as status,
    CURRENT_TIMESTAMP - INTERVAL '1 day' * (random() * 200 + 30) as enrolled_at, -- Enrolled 1-7 months ago
    CURRENT_TIMESTAMP + INTERVAL '1 day' * (30 + random() * 150) as due_date, -- Due in 1-6 months
    CASE 
        WHEN cert_seq = 1 THEN 5 -- Completed
        WHEN cert_seq = 2 THEN 3 -- Advanced stage
        WHEN cert_seq = 3 THEN 2 -- Intermediate stage
        ELSE 1 -- Foundation stage
    END as current_stage_sequence,
    CASE 
        WHEN cert_seq = 1 THEN 100.0 -- Completed
        WHEN cert_seq = 2 THEN 60.0 + random() * 30 -- 60-90%
        WHEN cert_seq = 3 THEN 30.0 + random() * 40 -- 30-70%
        ELSE random() * 20 -- 0-20%
    END as completion_percentage,
    CASE 
        WHEN cert_seq <= 3 THEN CURRENT_TIMESTAMP - INTERVAL '1 day' * (random() * 150 + 15)
        ELSE NULL
    END as started_at,
    CASE 
        WHEN cert_seq = 1 THEN CURRENT_TIMESTAMP - INTERVAL '1 day' * (random() * 60 + 10)
        ELSE NULL
    END as completed_at,
    CURRENT_TIMESTAMP as created_at,
    CURRENT_TIMESTAMP as updated_at
FROM generate_series(1, 350) AS emp_seq,
     generate_series(1, 4) AS cert_seq
WHERE emp_seq % 20 != 0 -- Skip every 20th employee (5% have no certifications)
   AND (cert_seq <= 3 OR (cert_seq = 4 AND emp_seq % 3 = 0)); -- 4th certification for every 3rd employee

-- =============================================================================
-- 6. CREATE STAGES (for each certification = ~6000 stages)
-- =============================================================================

INSERT INTO stages (id, certification_id, stage_definition_id, status, started_at, completed_at, completion_percentage, created_at, updated_at)
SELECT 
    'STAGE_' || LPAD(row_number() OVER ()::text, 6, '0') as id,
    c.id as certification_id,
    'STAGE_DEF_' || LPAD(((SPLIT_PART(c.certification_definition_id, '_', 3)::int - 1) * 5 + stage_seq)::text, 3, '0') as stage_definition_id,    CASE 
        WHEN stage_seq < c.current_stage_sequence THEN 'COMPLETED'
        WHEN stage_seq = c.current_stage_sequence THEN 'IN_PROGRESS'
        ELSE 'NOT_STARTED'
    END as status,
    CASE 
        WHEN stage_seq <= c.current_stage_sequence 
        THEN c.started_at + INTERVAL '1 day' * ((stage_seq - 1) * 25 + random() * 20)
        ELSE NULL
    END as started_at,
    CASE 
        WHEN stage_seq < c.current_stage_sequence 
        THEN c.started_at + INTERVAL '1 day' * (stage_seq * 30 + random() * 15)
        ELSE NULL
    END as completed_at,
    CASE 
        WHEN stage_seq < c.current_stage_sequence THEN 100.0
        WHEN stage_seq = c.current_stage_sequence THEN 30.0 + random() * 50
        ELSE 0.0
    END as completion_percentage,
    CURRENT_TIMESTAMP as created_at,
    CURRENT_TIMESTAMP as updated_at
FROM certifications c,
     generate_series(1, 5) AS stage_seq
WHERE c.started_at IS NOT NULL; -- Only for started certifications

-- =============================================================================
-- 7. CREATE TASKS (for each stage = ~30000 tasks)
-- =============================================================================

INSERT INTO tasks (id, stage_id, task_definition_id, status, started_at, completed_at, score, created_at, updated_at)
SELECT 
    'TASK_' || LPAD(row_number() OVER ()::text, 7, '0') as id,
    s.id as stage_id,
    'TASK_DEF_' || LPAD(((SPLIT_PART(s.stage_definition_id, '_', 3)::int - 1) * 5 + task_seq)::text, 3, '0') as task_definition_id,    CASE 
        WHEN s.status = 'COMPLETED' AND task_seq <= 4 THEN 'COMPLETED'
        WHEN s.status = 'COMPLETED' AND task_seq = 5 THEN 'COMPLETED'
        WHEN s.status = 'IN_PROGRESS' AND task_seq <= 2 THEN 'COMPLETED'
        WHEN s.status = 'IN_PROGRESS' AND task_seq = 3 THEN 'IN_PROGRESS'
        WHEN s.status = 'IN_PROGRESS' AND task_seq > 3 THEN 'NOT_STARTED'
        ELSE 'NOT_STARTED'
    END as status,
    CASE 
        WHEN (s.status = 'COMPLETED' AND task_seq <= 5) OR 
             (s.status = 'IN_PROGRESS' AND task_seq <= 3)
        THEN s.started_at + INTERVAL '1 day' * ((task_seq - 1) * 5 + random() * 3)
        ELSE NULL
    END as started_at,
    CASE 
        WHEN (s.status = 'COMPLETED' AND task_seq <= 5) OR 
             (s.status = 'IN_PROGRESS' AND task_seq <= 2)
        THEN s.started_at + INTERVAL '1 day' * (task_seq * 6 + random() * 4)
        ELSE NULL
    END as completed_at,
    CASE 
        WHEN (s.status = 'COMPLETED' AND task_seq <= 5) OR 
             (s.status = 'IN_PROGRESS' AND task_seq <= 2)
        THEN 70.0 + random() * 30 -- Scores between 70-100
        ELSE NULL
    END as score,
    CURRENT_TIMESTAMP as created_at,
    CURRENT_TIMESTAMP as updated_at
FROM stages s,
     generate_series(1, 5) AS task_seq
WHERE s.started_at IS NOT NULL; -- Only for started stages

-- =============================================================================
-- PERFORMANCE STATISTICS
-- =============================================================================

-- Performance test data creation completed
-- Expected data counts:
-- - Employees: 350
-- - Certification Definitions: 12
-- - Stage Definitions: 60 (5 per certification)
-- - Task Definitions: 300 (5 per stage)
-- - Active Certifications: ~1,400 (4 per employee average)
-- - Active Stages: ~7,000 (5 per certification average)
-- - Active Tasks: ~35,000 (5 per stage average)
-- - Expected Report Pages: 1,000+ (3+ per employee)

-- Update table statistics for query optimizer
ANALYZE employees;
ANALYZE certification_definitions;
ANALYZE stage_definitions;
ANALYZE task_definitions;
ANALYZE certifications;
ANALYZE stages;
ANALYZE tasks;
