# Database Setup for Certification Report System

This directory contains the database configuration and initialization scripts for the Certification Report System.

## Components

### Docker Setup
- `docker-compose.yml` - PostgreSQL and pgAdmin containers
- `init.sql` - Database initialization and sample data

## Quick Start

1. **Start the database:**
   ```bash
   cd docker
   docker-compose up -d
   ```

2. **Access pgAdmin (optional):**
   - URL: http://localhost:5050
   - Email: admin@certreport.com
   - Password: admin

3. **Database Connection Details:**
   - Host: localhost
   - Port: 5432
   - Database: certreport
   - Username: certuser
   - Password: certpass

## Sample Data

The system includes 15 sample employees across different departments:
- **Engineering**: 6 employees (including manager)
- **Marketing**: 3 employees (including manager) 
- **HR**: 3 employees (including manager)
- **Finance**: 2 employees (including manager)
- **Executive**: 1 VP

## Database Schema

The Spring Boot application will automatically create the necessary tables:
- `employees` - Employee information  
- `certifications` - Employee certification records
- `certification_definitions` - Available certification types
- `stages` - Certification progress stages
- `tasks` - Individual certification tasks
- `reports` - Report generation tracking

The system uses efficient batch queries with JOIN FETCH for optimal performance.
