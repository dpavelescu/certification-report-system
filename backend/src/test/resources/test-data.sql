-- Test data for certification report system

-- Insert employees (the ones referenced in performance tests)
INSERT INTO employees (id, first_name, last_name, email, department, position, hire_date, created_at, updated_at) VALUES
('EMP001', 'John', 'Doe', 'john.doe@company.com', 'Engineering', 'Software Engineer', '2023-01-15 09:00:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('EMP002', 'Jane', 'Smith', 'jane.smith@company.com', 'Engineering', 'Senior Developer', '2023-02-01 09:00:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('EMP003', 'Bob', 'Johnson', 'bob.johnson@company.com', 'Engineering', 'DevOps Engineer', '2023-03-10 09:00:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('EMP004', 'Alice', 'Williams', 'alice.williams@company.com', 'Engineering', 'Tech Lead', '2023-01-20 09:00:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('EMP005', 'Charlie', 'Brown', 'charlie.brown@company.com', 'Engineering', 'Full Stack Developer', '2023-04-05 09:00:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('EMP006', 'Diana', 'Davis', 'diana.davis@company.com', 'QA', 'QA Engineer', '2023-02-15 09:00:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('EMP007', 'Eve', 'Miller', 'eve.miller@company.com', 'QA', 'Senior QA', '2023-03-01 09:00:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('EMP008', 'Frank', 'Wilson', 'frank.wilson@company.com', 'Product', 'Product Manager', '2023-01-10 09:00:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('EMP009', 'Grace', 'Moore', 'grace.moore@company.com', 'Product', 'Product Owner', '2023-05-01 09:00:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('EMP010', 'Henry', 'Taylor', 'henry.taylor@company.com', 'Design', 'UX Designer', '2023-04-20 09:00:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('EMP011', 'Ivy', 'Anderson', 'ivy.anderson@company.com', 'Design', 'UI Designer', '2023-06-01 09:00:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('EMP012', 'Jack', 'Thomas', 'jack.thomas@company.com', 'Engineering', 'Backend Developer', '2023-07-10 09:00:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('EMP013', 'Kelly', 'Jackson', 'kelly.jackson@company.com', 'Engineering', 'Frontend Developer', '2023-08-15 09:00:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('EMP014', 'Liam', 'White', 'liam.white@company.com', 'Security', 'Security Engineer', '2023-09-01 09:00:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('EMP015', 'Mia', 'Harris', 'mia.harris@company.com', 'Security', 'Security Analyst', '2023-10-05 09:00:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert certification definitions
INSERT INTO certification_definitions (id, name, description, category, created_at, updated_at) VALUES
('CERT001', 'AWS Cloud Practitioner', 'AWS Cloud Practitioner certification', 'CLOUD', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('CERT002', 'Java SE 11 Developer', 'Oracle Java SE 11 Developer certification', 'PROGRAMMING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('CERT003', 'Spring Professional', 'VMware Spring Professional certification', 'FRAMEWORK', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('CERT004', 'Docker Certified Associate', 'Docker Certified Associate certification', 'CONTAINERIZATION', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('CERT005', 'Kubernetes Administrator', 'Certified Kubernetes Administrator (CKA)', 'ORCHESTRATION', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert stage definitions for each certification definition
INSERT INTO stage_definitions (id, certification_definition_id, name, description, sequence_order, estimated_duration_hours, created_at, updated_at) VALUES
-- AWS Cloud Practitioner stages
('STAGE001', 'CERT001', 'Study Phase', 'Study AWS fundamentals', 1, 40, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('STAGE002', 'CERT001', 'Practice Tests', 'Complete practice exams', 2, 20, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('STAGE003', 'CERT001', 'Final Exam', 'Take the certification exam', 3, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Java SE 11 Developer stages  
('STAGE004', 'CERT002', 'Core Java Study', 'Study Java fundamentals', 1, 60, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('STAGE005', 'CERT002', 'Advanced Topics', 'Study advanced Java concepts', 2, 40, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('STAGE006', 'CERT002', 'Certification Exam', 'Take the Java certification exam', 3, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Spring Professional stages
('STAGE007', 'CERT003', 'Spring Core', 'Learn Spring framework basics', 1, 30, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('STAGE008', 'CERT003', 'Spring Boot', 'Master Spring Boot development', 2, 25, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('STAGE009', 'CERT003', 'Spring Data & Security', 'Advanced Spring topics', 3, 20, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('STAGE010', 'CERT003', 'Certification Test', 'Complete Spring certification', 4, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert some certification enrollments to make the data realistic
INSERT INTO certifications (id, employee_id, certification_definition_id, status, enrolled_at, due_date, current_stage_sequence, completion_percentage, created_at, updated_at) VALUES
-- EMP001 certifications
('CERT_ENROLL001', 'EMP001', 'CERT001', 'COMPLETED', '2024-01-15 09:00:00', '2024-06-15 23:59:59', 3, 100.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('CERT_ENROLL002', 'EMP001', 'CERT002', 'IN_PROGRESS', '2024-03-01 09:00:00', '2024-09-01 23:59:59', 2, 65.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- EMP002 certifications
('CERT_ENROLL003', 'EMP002', 'CERT002', 'COMPLETED', '2024-01-01 09:00:00', '2024-07-01 23:59:59', 3, 100.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('CERT_ENROLL004', 'EMP002', 'CERT003', 'IN_PROGRESS', '2024-04-01 09:00:00', '2024-10-01 23:59:59', 2, 45.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- EMP003 certifications
('CERT_ENROLL005', 'EMP003', 'CERT004', 'IN_PROGRESS', '2024-02-15 09:00:00', '2024-08-15 23:59:59', 1, 30.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('CERT_ENROLL006', 'EMP003', 'CERT005', 'NOT_STARTED', '2024-05-01 09:00:00', '2024-11-01 23:59:59', 1, 0.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- More employees with various certification statuses
('CERT_ENROLL007', 'EMP004', 'CERT001', 'COMPLETED', '2024-01-10 09:00:00', '2024-06-10 23:59:59', 3, 100.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('CERT_ENROLL008', 'EMP004', 'CERT003', 'COMPLETED', '2024-02-01 09:00:00', '2024-08-01 23:59:59', 4, 100.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('CERT_ENROLL009', 'EMP005', 'CERT002', 'IN_PROGRESS', '2024-03-15 09:00:00', '2024-09-15 23:59:59', 1, 25.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('CERT_ENROLL010', 'EMP006', 'CERT001', 'NOT_STARTED', '2024-05-20 09:00:00', '2024-11-20 23:59:59', 1, 0.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
