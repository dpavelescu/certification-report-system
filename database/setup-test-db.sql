-- Create test database for performance testing
-- This script should be run as a PostgreSQL superuser or database owner

-- Create test database if it doesn't exist
SELECT 'CREATE DATABASE certreport_test' 
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'certreport_test')\gexec

-- Grant permissions to certuser for the test database
GRANT ALL PRIVILEGES ON DATABASE certreport_test TO certuser;

-- Connect to test database and ensure schema permissions
\c certreport_test

-- Grant schema permissions
GRANT ALL ON SCHEMA public TO certuser;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO certuser;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO certuser;

-- Set default privileges for future objects
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO certuser;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO certuser;
