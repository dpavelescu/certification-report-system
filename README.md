# Certification Report System

Employee certification reporting system with intelligent filtering and PDF export. Built with React, Spring Boot, and JasperReports.

## Overview

A web application that provides managers with a streamlined interface featuring intelligent cascaded filtering to export employee certification data into pixel-perfect A4 landscape PDF reports.

## Technology Stack

- **Frontend**: React 18+, Tailwind CSS, Native Fetch API
- **Backend**: Spring Boot 3.2+, Java 17+
- **Database**: PostgreSQL 15+ (Dockerized)
- **Report Engine**: JasperReports 6.20+

## Project Structure

```
certification-report-system/
├── README.md
├── docs/                           # Project documentation
│   ├── delivery-plan.md
│   ├── prd-final.md
│   ├── technical-specifications.md
│   └── requirements-elicitation.md
├── frontend/                       # React application
├── backend/                        # Spring Boot application
├── database/                       # SQL scripts and migrations
└── docker/                         # Docker configurations
```

## Quick Start

### Prerequisites
- Node.js 18+
- Java 17+
- Docker & Docker Compose
- Maven 3.8+

### Development Setup

1. **Start Database**
   ```bash
   cd docker
   docker-compose up -d postgres
   ```

2. **Start Backend**
   ```bash
   cd backend
   mvn spring-boot:run
   ```

3. **Start Frontend**
   ```bash
   cd frontend
   npm install
   npm start
   ```

## Current Implementation Status

### ✅ Iteration 1: Minimal Viable Report Generation (Completed)
- Basic landing page with employee selector
- Simple PDF report generation (Employee Demographics)
- Performance monitoring foundation
- Direct PDF download capability

### 🚧 Iteration 2: Complete Filtering Interface (Planned)
- Cascaded filtering (Employee → Certification → Date Range)
- Dynamic filter updates
- Empty state handling

### 📋 Iteration 3: Complete Report Structure (Planned)
- Three-section reports (Demographics, Summary, Activities)
- Visual status indicators
- Professional audit-ready formatting

### 📋 Iteration 4: Concurrency & Performance Optimization (Planned)
- Async processing with 5 concurrent reports
- Performance optimization
- Production readiness

## Performance Requirements

- **Generation Time**: Up to 20 seconds for MVP (target: 10 seconds)
- **Report Size**: Up to 300 pages
- **Concurrency**: 5 large reports simultaneously
- **Memory**: Optimized for large dataset processing

## Development Guidelines

- Follow the delivery plan for incremental development
- Maintain performance monitoring from the start
- Focus on business value delivery in each iteration
- Validate NFRs continuously throughout development

## Documentation

- [Product Requirements Document](docs/prd-final.md)
- [Technical Specifications](docs/technical-specifications.md)
- [Delivery Plan](docs/delivery-plan.md)
- [Requirements Elicitation](docs/requirements-elicitation.md)
