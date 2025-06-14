# Certification Report System - Technical Specifications

## Document Overview

This document provides comprehensive technical specifications for the Certification Report System, covering architecture, implementation details, and deployment considerations.

> 📋 **Related Documentation**:
> - **[PDF Generation Process](pdf-generation-process.md)** - Deep-dive into performance architecture, chunking strategies, and evidence-based optimization decisions
> - **[Database README](../database/README.md)** - Database setup and configuration details
> - **[Main README](../README.md)** - Project overview and quick start guide

## 1. System Architecture

### 1.1 High-Level Architecture
```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Client Layer  │     │  Service Layer  │     │ Persistence Layer│
│    (React)      │◄───►│  (Spring Boot)  │◄───►│   (PostgreSQL)   │
└─────────────────┘     └─────────────────┘     └─────────────────┘
        │                       │                       
        │                       │                       
┌─────────────────┐     ┌─────────────────┐            
│  UI Components  │     │ Report Engine   │            
│    (Tailwind)   │     │(Jasper Reports) │            
└─────────────────┘     └─────────────────┘            
```

### 1.2 Technology Stack
- **Frontend**: React 18+, Tailwind CSS, Native Fetch API
- **Backend**: Spring Boot 3.2+, Java 17+
- **Database**: PostgreSQL 15+ (Dockerized)
- **Report Engine**: JasperReports 6.20+

## 2. Component Specifications

### 2.1 Frontend Architecture

**Core Technologies**:
- React with functional components and hooks
- Tailwind CSS for styling
- Native Fetch API for HTTP requests
- Context API for state management

**Key Features**:
- Client-side pagination for large datasets
- Report status polling with refresh button
- Responsive design for various screen sizes

**API Client Implementation**:
```javascript
const api = {
  async getReports() {
    const response = await fetch('/api/reports');
    if (!response.ok) throw new Error('Failed to fetch reports');
    return response.json();
  },
  
  async generateReport(params) {
    const response = await fetch('/api/reports', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(params)
    });
    if (!response.ok) throw new Error('Failed to generate report');
    return response.json();
  },
  
  async getReportStatus(id) {
    const response = await fetch(`/api/reports/${id}/status`);
    if (!response.ok) throw new Error('Failed to get report status');
    return response.json();
  }
};
```

### 2.2 Backend Architecture

**Core Components**:
- REST API controllers for report operations
- Service layer for business logic
- Repository layer for data access
- Async task executor for report generation

**Key APIs**:
- `POST /api/reports` - Generate new report
- `GET /api/reports/{id}/status` - Check report status
- `GET /api/reports/{id}/download` - Download completed report
- `GET /api/reports` - List reports with pagination

**Report Processing Flow**:
1. Receive report generation request
2. Queue report for async processing
3. Generate report using JasperReports
4. Update status and store result
5. Provide download link when complete

### 2.3 Database Design

> 📋 **For detailed database architecture and performance optimization decisions, see [PDF Generation Process](pdf-generation-process.md)**  
> This includes end-to-end query flows, chunking strategies, and evidence-based indexing decisions.

**PostgreSQL Schema**:
```sql
-- Core tables for report management
CREATE TABLE reports (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    parameters JSONB,
    file_path VARCHAR(500),
    page_count INTEGER,
    created_at TIMESTAMP DEFAULT NOW(),
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    error_message TEXT
);

-- Essential indexes only (based on performance testing)
CREATE INDEX idx_reports_status ON reports(status);
CREATE INDEX idx_reports_created_at ON reports(created_at);
CREATE INDEX idx_reports_type ON reports(type);
```

**Docker Configuration**:
```yaml
version: '3.8'
services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_USER: certreport
      POSTGRES_PASSWORD: password
      POSTGRES_DB: certification_reports
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"
volumes:
  postgres_data:
```

### 2.4 Report Engine Implementation

> 🔧 **For detailed PDF generation architecture and performance optimizations, see [PDF Generation Process](pdf-generation-process.md)**  
> This covers chunking strategies, query optimization decisions, and evidence-based architectural choices.

**JasperReports Configuration**:
- Pre-compiled report templates for performance
- Async processing with Spring's `@Async`
- Memory-optimized settings for large reports
- Single comprehensive query strategy (proven optimal through testing)

**Service Implementation**:
```java
@Service
public class ReportServiceImpl implements ReportService {
    
    @Async("reportTaskExecutor")
    public CompletableFuture<ReportResult> generateReport(ReportRequest request) {
        // Implementation details in PDF Generation Process documentation
        return processReportAsync(request);
    }
}
```

## 3. Non-Functional Requirements Implementation

### 3.1 Performance Requirements

> ⚡ **Validated Performance Metrics**: See [PDF Generation Process](pdf-generation-process.md) for actual test results and optimization evidence.

**Achieved Performance** (based on testing):
- **300 employees**: 8.98 seconds, 301 pages, 831KB
- **Concurrent processing**: Up to 5 large reports
- **Memory efficiency**: Stable ~150MB heap usage
- **Query optimization**: Single comprehensive query approach (validated through testing)

**Target Performance**:
- Report generation time: Under 15 seconds for 300 employees ✅
- Concurrent report processing: Up to 5 large reports ✅  
- Report size support: Up to 300+ pages ✅

**Implementation Strategy**:
- Async processing to prevent UI blocking
- Chunking strategy for large datasets (50 employees per chunk)
- Evidence-based query optimization (no unnecessary indexes)
- JasperReports memory settings tuning

**Thread Pool Configuration**:
```java
@Configuration
public class AsyncConfig {
    
    @Bean("reportTaskExecutor")
    public Executor reportTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("report-");
        return executor;
    }
}
```

### 3.2 Report Status Tracking

**Status Management**:
- Database-based status tracking
- Polling mechanism for status updates
- Progress indication where possible

**Status Endpoint Response**:
```json
{
  "reportId": "c4f7d93a-1234-5678-abcd-ef1234567890",
  "status": "IN_PROGRESS",
  "progress": 65,
  "estimatedCompletionTime": "2025-06-07T10:16:30Z",
  "message": "Processing page 195 of 300"
}
```

## 4. Testing Strategy

### 4.1 Core Testing Approach

> 🧪 **Performance Testing Results**: See [PDF Generation Process](pdf-generation-process.md) for detailed test evidence and database optimization analysis.

**Testing Focus**:
- End-to-end report generation flow ✅
- Performance validation for NFRs ✅
- Concurrent processing capability ✅
- Database optimization testing (completed with evidence-based decisions)

**Validated Results**:
- 300 employees processed in 8.98 seconds (exceeds 15s target)
- Memory usage remains stable at ~150MB heap
- Single query approach proven optimal through index testing
- Chunking strategy validated for larger datasets

**Performance Tests**:
```java
@Test
void validateReportGenerationPerformance() {
    // Actual validated performance: 8.98s for 300 employees
    // See PDF Generation Process documentation for complete test results
    ReportRequest request = createLargeReportRequest(300);
    
    Instant start = Instant.now();
    ReportResult result = reportService.generateReport(request);
    Duration duration = Duration.between(start, Instant.now());
    
    assertThat(duration.getSeconds()).isLessThanOrEqualTo(15); // Updated based on actual results
    assertThat(result.getPageCount()).isGreaterThan(300);
}

@Test
void validateConcurrentReportGeneration() {
    List<ReportRequest> requests = createMultipleReportRequests(5);
    
    List<CompletableFuture<ReportResult>> futures = requests.stream()
        .map(reportService::generateReport)
        .collect(Collectors.toList());
    
    List<ReportResult> results = futures.stream()
        .map(CompletableFuture::join)
        .collect(Collectors.toList());
    
    assertThat(results).hasSize(5);
    assertThat(results).allMatch(r -> r.getStatus() == COMPLETED);
}
```

## 5. Monitoring and Observability

### 5.1 Spring Boot Actuator Configuration

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

### 5.2 Key Metrics

**Report Generation Metrics**:
```java
@Service
public class ReportServiceImpl {
    private final MeterRegistry meterRegistry;
    private final AtomicInteger activeReports = new AtomicInteger(0);
    
    public CompletableFuture<ReportResult> generateReport(ReportRequest request) {
        Timer.Sample sample = Timer.start(meterRegistry);
        meterRegistry.gauge("reports.active", activeReports.incrementAndGet());
        
        try {
            // Report generation logic
            return doGenerateReport(request);
        } finally {
            sample.stop(meterRegistry.timer("reports.generation.time"));
            activeReports.decrementAndGet();
        }
    }
}
```

**Essential Metrics**:
- `reports.generation.time` - Report generation duration (validated: ~9s for 300 employees)
- `reports.active` - Current active report count
- `reports.pages` - Report page count distribution  
- `jvm.memory.used` - Memory usage during generation (stable ~150MB)
- `reports.query.count` - Database query efficiency tracking

> 📊 **Performance Benchmarks**: See [PDF Generation Process](pdf-generation-process.md) for detailed performance metrics and optimization results.

### 5.3 Performance Monitoring

**Dashboard Metrics**:
- Report generation times (by size) - Current: 8.98s for 300 employees
- Concurrent report processing - Validated: 5 concurrent reports
- Memory usage patterns - Stable ~150MB heap usage
- Database query performance - Optimized single query approach

> 📈 **Real Performance Data**: All metrics above are based on actual testing. See [PDF Generation Process](pdf-generation-process.md) for evidence and methodology.

## 6. Configuration and Deployment

### 6.1 Application Configuration

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/certification_reports
    username: certreport
    password: password
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5

jasper:
  reports:
    compile-on-startup: true
    virtualizer:
      enabled: true
      directory: /tmp/jasper

logging:
  level:
    com.certreport: INFO
    net.sf.jasperreports: WARN
```

### 6.2 JVM Settings

```bash
# Recommended JVM settings for report generation
-Xmx2g
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:+UseStringDeduplication
```

## 7. Security Considerations

### 7.1 Basic Security Measures

- Input validation for report parameters
- File access controls for report storage
- SQL injection prevention in dynamic queries
- Resource limits to prevent DoS

### 7.2 Authentication and Authorization

- Basic authentication for API access
- Role-based access for different report types
- Audit logging for report generation requests

## 8. Error Handling

### 8.1 Error Response Format

```json
{
  "error": {
    "code": "REPORT_GENERATION_FAILED",
    "message": "Failed to generate report due to data access error",
    "timestamp": "2025-06-07T10:15:30Z",
    "reportId": "c4f7d93a-1234-5678-abcd-ef1234567890"
  }
}
```

### 8.2 Error Recovery

- Automatic retry for transient failures
- Graceful degradation when system is under load
- Clear error messages for user guidance

## 9. Development Guidelines

### 9.1 Code Standards

- Follow Spring Boot best practices
- Use consistent naming conventions
- Implement proper logging at key points
- Write unit tests for core business logic

### 9.2 Performance Considerations

> ⚡ **Evidence-Based Optimizations**: All recommendations below are validated through testing. See [PDF Generation Process](pdf-generation-process.md) for test evidence.

**Database Optimization**:
- **Single comprehensive query** with `JOIN FETCH` (proven optimal)
- **No additional indexes** beyond essential ones (tested and validated)
- **Chunking strategy** for large datasets (50 employees per chunk)
- **Connection pool optimization** for concurrent processing

**Memory Management**:
- **Stable heap usage** (~150MB for 300 employees)
- **Efficient object lifecycle** through proper DTO conversion
- **Async processing** to maintain UI responsiveness
- **Automatic cleanup** of temporary report files

**Query Performance**:
- **Elimination of N+1 queries** through JOIN FETCH strategy
- **Minimal database round trips** (1 query vs ~700 individual queries)
- **Buffer cache efficiency** for small-to-medium datasets

## 10. Deployment Architecture

### 10.1 Container Strategy

- Spring Boot application as Docker container
- PostgreSQL as separate Docker container
- Shared volume for report file storage
- Environment-specific configuration files

### 10.2 Resource Requirements

**Minimum Requirements**:
- CPU: 4 cores
- Memory: 4GB RAM
- Storage: 50GB (for reports and database)
- Network: Standard HTTP/HTTPS access

**Recommended for Production**:
- CPU: 8 cores
- Memory: 8GB RAM
- Storage: 200GB SSD
- Load balancer for high availability

## 11. Implementation Status

### Validated Components ✅
- **Performance Architecture**: 8.98s for 300 employees (exceeds 15s target)
- **Database Optimization**: Evidence-based single query approach
- **Memory Management**: Stable ~150MB heap usage with chunking
- **Concurrent Processing**: Validated 5 concurrent reports
- **Error Handling**: Comprehensive async processing with timeouts

### Architecture Decisions
All major architectural decisions have been validated through testing:
- **Query Strategy**: Single comprehensive `JOIN FETCH` proven optimal
- **Index Strategy**: No additional indexes (tested and confirmed faster)
- **Chunking Strategy**: 50 employees per chunk for scalability
- **Memory Strategy**: Efficient DTO conversion with stable usage

> 📚 **For Implementation Details**: See [PDF Generation Process](pdf-generation-process.md) for the complete technical deep-dive into these validated architectural decisions.
