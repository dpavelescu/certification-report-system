-- Complete initialization script for Certification Report System
-- This script sets up the user, creates tables, and inserts sample data

-- Create employees table
CREATE TABLE IF NOT EXISTS employees (
    id VARCHAR(36) PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    department VARCHAR(100) NOT NULL,
    position VARCHAR(100) NOT NULL,
    hire_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create reports table  
CREATE TABLE IF NOT EXISTS reports (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
    parameters TEXT,
    file_path VARCHAR(500),
    page_count INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    error_message TEXT
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_employees_email ON employees(email);
CREATE INDEX IF NOT EXISTS idx_employees_department ON employees(department);
CREATE INDEX IF NOT EXISTS idx_reports_status ON reports(status);
CREATE INDEX IF NOT EXISTS idx_reports_type ON reports(type);

-- Insert sample employee data
INSERT INTO employees (id, first_name, last_name, email, department, position, hire_date, created_at, updated_at) VALUES 
('1', 'John', 'Smith', 'john.smith@company.com', 'Engineering', 'Senior Software Engineer', '2020-01-15 00:00:00', NOW(), NOW()),
('2', 'Sarah', 'Wilson', 'sarah.wilson@company.com', 'Engineering', 'Frontend Developer', '2021-03-22 00:00:00', NOW(), NOW()),
('3', 'Mike', 'Davis', 'mike.davis@company.com', 'Engineering', 'DevOps Engineer', '2019-11-08 00:00:00', NOW(), NOW()),
('4', 'Alice', 'Johnson', 'alice.johnson@company.com', 'Engineering', 'Engineering Manager', '2018-06-10 00:00:00', NOW(), NOW()),
('5', 'David', 'Lee', 'david.lee@company.com', 'Marketing', 'Marketing Specialist', '2022-02-14 00:00:00', NOW(), NOW()),
('6', 'Lisa', 'Garcia', 'lisa.garcia@company.com', 'Marketing', 'Marketing Manager', '2019-09-05 00:00:00', NOW(), NOW()),
('7', 'Emily', 'Chen', 'emily.chen@company.com', 'HR', 'HR Specialist', '2021-07-19 00:00:00', NOW(), NOW()),
('8', 'James', 'Martinez', 'james.martinez@company.com', 'HR', 'HR Manager', '2017-12-03 00:00:00', NOW(), NOW()),
('9', 'Robert', 'Brown', 'robert.brown@company.com', 'Executive', 'VP of Operations', '2015-04-20 00:00:00', NOW(), NOW()),
('10', 'Jennifer', 'Taylor', 'jennifer.taylor@company.com', 'Finance', 'Financial Analyst', '2020-10-30 00:00:00', NOW(), NOW()),
('11', 'Michael', 'White', 'michael.white@company.com', 'Finance', 'Finance Manager', '2018-08-12 00:00:00', NOW(), NOW()),
('12', 'Kevin', 'Anderson', 'kevin.anderson@company.com', 'Engineering', 'Backend Developer', '2021-05-18 00:00:00', NOW(), NOW()),
('13', 'Rachel', 'Green', 'rachel.green@company.com', 'Marketing', 'Content Creator', '2022-01-09 00:00:00', NOW(), NOW()),
('14', 'Daniel', 'Kim', 'daniel.kim@company.com', 'Engineering', 'QA Engineer', '2020-07-25 00:00:00', NOW(), NOW()),
('15', 'Maria', 'Rodriguez', 'maria.rodriguez@company.com', 'HR', 'Recruiter', '2021-11-14 00:00:00', NOW(), NOW());
