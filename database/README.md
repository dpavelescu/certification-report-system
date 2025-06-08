# Database Setup for Certification Report System

This directory contains the database configuration and initialization scripts for the Certification Report System.

## Components

### Docker Setup
- `docker-compose.yml` - PostgreSQL and pgAdmin containers
- `init-data.sql` - Sample employee data and database optimization

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
- `reports` - Report generation tracking

Indexes are created for optimal query performance on:
- Department filtering
- Manager relationships
- Email lookups
- Report status tracking
