package com.certreport.service;

import com.certreport.dto.EmployeeDto;
import com.certreport.dto.ReportRequestDto;
import com.certreport.dto.CompleteReportDataDto;
import com.certreport.dto.CertificationDto;
import com.certreport.dto.EmployeeCertificationActivityDto;
import com.certreport.model.Report;
import com.certreport.repository.ReportRepository;
import com.certreport.config.PdfGenerationProperties;
import io.micrometer.core.instrument.Timer;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class ReportService {
      private static final Logger logger = LoggerFactory.getLogger(ReportService.class);
    
    private final PdfGenerationProperties pdfProperties;
      private final ReportRepository reportRepository;
    private final EmployeeService employeeService;
    private final CertificationService certificationService;
    private final ActuatorPerformanceMonitor actuatorPerformanceMonitor;
    private final MemoryEfficientPdfGenerationService memoryEfficientPdfGenerationService;    public ReportService(ReportRepository reportRepository, 
                        EmployeeService employeeService,
                        CertificationService certificationService,
                        ActuatorPerformanceMonitor actuatorPerformanceMonitor,
                        MemoryEfficientPdfGenerationService memoryEfficientPdfGenerationService,
                        PdfGenerationProperties pdfProperties) {
        this.reportRepository = reportRepository;
        this.employeeService = employeeService;
        this.certificationService = certificationService;
        this.actuatorPerformanceMonitor = actuatorPerformanceMonitor;
        this.memoryEfficientPdfGenerationService = memoryEfficientPdfGenerationService;
        this.pdfProperties = pdfProperties;
    }

    public Report generateReport(ReportRequestDto request) {
        // Generate report name based on type and timestamp
        String reportName = generateReportName(request.getReportType(), request.getEmployeeIds().size());
        
        // Create and save report record with QUEUED status first
        Report report = new Report(
                reportName,
                request.getReportType() != null ? request.getReportType() : "EMPLOYEE_DEMOGRAPHICS",
                request.toString()
        );
        report.setStatus(Report.ReportStatus.QUEUED);
        report.setStartedAt(LocalDateTime.now());
        report = reportRepository.save(report);
        
        // Start async processing
        generateReportAsync(report);
        
        return report;
    }

    private String generateCertificationsPdfReport(List<CompleteReportDataDto> reportData, String reportId) throws JRException {
        // Record memory snapshot for PDF generation start
        actuatorPerformanceMonitor.recordMemorySnapshot(reportId, "PDF Generation Start");
        
        // Load the certifications report template
        InputStream reportTemplate = getClass().getResourceAsStream("/reports/certifications_report.jrxml");
        if (reportTemplate == null) {
            throw new RuntimeException("Certifications report template not found at /reports/certifications_report.jrxml");
        }
        
        // Compile report
        JasperReport jasperReport = JasperCompileManager.compileReport(reportTemplate);
        actuatorPerformanceMonitor.recordMemorySnapshot(reportId, "Template Compiled");
        
        // Create flattened data structure for individual certification activities
        List<EmployeeCertificationActivityDto> activityData = createActivityDataFromReportData(reportData);
        
        // Prepare data source with flattened activity data
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(activityData);
        
        // Report parameters
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("REPORT_TITLE", "Employee Certification Report");
        parameters.put("GENERATION_DATE", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        parameters.put("TOTAL_EMPLOYEES", reportData.size());
        parameters.put("COMPANY_NAME", "CertReport Systems");
        
        // Calculate certification statistics
        long totalCertifications = reportData.stream()
                .mapToLong(data -> data.getCertifications() != null ? data.getCertifications().size() : 0)
                .sum();
        long completedCertifications = reportData.stream()
                .mapToLong(CompleteReportDataDto::getCompletedCertificationsCount)
                .sum();
        long inProgressCertifications = reportData.stream()
                .mapToLong(CompleteReportDataDto::getInProgressCertificationsCount)
                .sum();
        long failedCertifications = reportData.stream()
                .mapToLong(CompleteReportDataDto::getFailedCertificationsCount)
                .sum();
        
        parameters.put("TOTAL_CERTIFICATIONS", totalCertifications);
        parameters.put("COMPLETED_CERTIFICATIONS", completedCertifications);
        parameters.put("IN_PROGRESS_CERTIFICATIONS", inProgressCertifications);
        parameters.put("FAILED_CERTIFICATIONS", failedCertifications);
        
        actuatorPerformanceMonitor.recordMemorySnapshot(reportId, "Data Prepared");
        
        JasperPrint jasperPrint;
        int actualPageCount;
        
        try {
            // Fill report
            jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
                  // Extract actual page count from JasperPrint
            actualPageCount = jasperPrint.getPages().size();
            
            // Save the page count to the report
            Report report = reportRepository.findById(reportId)
                    .orElseThrow(() -> new RuntimeException("Report not found: " + reportId));
            report.setPageCount(actualPageCount);
            reportRepository.save(report);
            
        } catch (Exception e) {
            logger.error("Error in PDF generation: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage(), e);
        }
        
        actuatorPerformanceMonitor.recordMemorySnapshot(reportId, "Report Filled");
        
        // Export to PDF
        String fileName = String.format("CertificationReport_%s_%s.pdf", 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")),
                reportId.substring(0, 8));
        String filePath = System.getProperty("java.io.tmpdir") + File.separator + fileName;
        
        JasperExportManager.exportReportToPdfFile(jasperPrint, filePath);
        
        // Get actual file size
        File generatedFile = new File(filePath);
        long fileSizeBytes = generatedFile.exists() ? generatedFile.length() : 0;
        
        // Record final memory snapshot
        actuatorPerformanceMonitor.recordMemorySnapshot(reportId, "PDF Generation Complete");
        
        logger.info("Generated certifications report with {} employees, {} certification activities, {} pages, {} KB", 
                   reportData.size(), activityData.size(), actualPageCount, fileSizeBytes / 1024);
        
        return filePath;
    }
      /**
     * Intelligently selects PDF generation approach based on memory constraints.
     * Falls back to traditional approach if memory-efficient processing is disabled or fails.
     */
    private String generateMemoryConstrainedCertificationsPdfReport(List<CompleteReportDataDto> reportData, String reportId) throws Exception {        if (!pdfProperties.isEnabled()) {
            logger.info("Memory-efficient PDF processing disabled, using traditional approach");
            return generateCertificationsPdfReport(reportData, reportId);
        }
          // Check if we should use optimized approach based on memory threshold
        // Note: This memory check is for intelligent algorithm selection, not performance monitoring
        Runtime runtime = Runtime.getRuntime();
        long estimatedMemoryUsage = reportData.size() * 580_000; // ~580KB per employee (from analysis)
        long availableMemory = runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory());
          if (estimatedMemoryUsage > pdfProperties.getThresholdMb() * 1024 * 1024 || availableMemory < estimatedMemoryUsage * 2) {
            logger.info("Using memory-efficient PDF generation due to memory constraints. Estimated: {}MB, Available: {}MB", 
                       estimatedMemoryUsage / (1024 * 1024), availableMemory / (1024 * 1024));
            
            try {
                return generateWithMemoryEfficientService(reportData, reportId);
            } catch (Exception e) {
                logger.warn("Memory-efficient PDF generation failed, falling back to traditional approach: {}", e.getMessage());
                return generateCertificationsPdfReport(reportData, reportId);
            }
        } else {
            logger.info("Using traditional PDF generation for optimal performance");
            return generateCertificationsPdfReport(reportData, reportId);
        }
    }
      /**
     * Use the MemoryEfficientPdfGenerationService for memory-constrained PDF creation
     */
    private String generateWithMemoryEfficientService(List<CompleteReportDataDto> reportData, String reportId) throws Exception {
        actuatorPerformanceMonitor.recordMemorySnapshot(reportId, "Memory-Efficient PDF Generation Start");
          // Convert to activity data for memory-efficient service
        List<EmployeeCertificationActivityDto> activityData = createActivityDataFromReportData(reportData);
          // Generate memory-efficient PDF
        byte[] pdfBytes = memoryEfficientPdfGenerationService.generateOptimizedReport(
            activityData, 
            "Employee Certification Report"
        );
          // Save to file
        String fileName = String.format("CertificationReport_MemoryEfficient_%s_%s.pdf", 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")),
                reportId.substring(0, 8));
        String filePath = System.getProperty("java.io.tmpdir") + File.separator + fileName;
        
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(filePath)) {
            fos.write(pdfBytes);
        }
        
        // Update report with page count (estimate based on content)
        Report report = reportRepository.findById(reportId).orElseThrow();
        int estimatedPageCount = Math.max(1, activityData.size() / 20); // ~20 activities per page
        report.setPageCount(estimatedPageCount);
        reportRepository.save(report);
          actuatorPerformanceMonitor.recordMemorySnapshot(reportId, "Memory-Efficient PDF Generation Complete");
        
        logger.info("Generated memory-efficient PDF with {} employees, {} activities, ~{} pages, {} KB",
                   reportData.size(), activityData.size(), estimatedPageCount, pdfBytes.length / 1024);
        
        return filePath;
    }

    public Report getReportStatus(String reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found: " + reportId));
    }
    
    public List<Report> getAllReports() {
        return reportRepository.findAllByOrderByCreatedAtDesc();
    }
    
    public File getReportFile(String reportId) {
        Report report = getReportStatus(reportId);
        if (report.getStatus() != Report.ReportStatus.COMPLETED || report.getFilePath() == null) {
            throw new RuntimeException("Report not ready for download: Status is " + report.getStatus());
        }
        
        File file = new File(report.getFilePath());
        if (!file.exists()) {
            // Mark the report as failed since its file is missing
            report.setStatus(Report.ReportStatus.FAILED);
            report.setErrorMessage("Report file was deleted or moved from: " + report.getFilePath());
            reportRepository.save(report);
            throw new RuntimeException("Report file no longer exists. The file may have been cleaned up. Please regenerate the report.");
        }
        
        return file;
    }
    
    private String generateReportName(String reportType, int employeeCount) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String type = reportType != null ? reportType.toLowerCase().replace("_", " ") : "employee demographics";
        return String.format("%s_report_%d_employees_%s.pdf", 
            type.replace(" ", "_"), employeeCount, timestamp);
    }

    private List<String> parseEmployeeIdsFromParameters(String parameters) {
        if (parameters == null || parameters.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            // Check for empty employeeIds list first (employeeIds=[])
            if (parameters.contains("employeeIds=[]")) {
                return new ArrayList<>();
            }
            
            String pattern = "employeeIds=\\[([^\\]]+)\\]";
            java.util.regex.Pattern regex = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher matcher = regex.matcher(parameters);
            
            if (matcher.find()) {
                String idsString = matcher.group(1);
                return Arrays.asList(idsString.split(",\\s*"));
            }
        } catch (Exception e) {
            logger.error("Error parsing employee IDs from parameters: {}", parameters, e);
        }
        
        return new ArrayList<>();
    }

    @Async
    public CompletableFuture<Void> generateReportAsync(Report report) {
        // Start Actuator monitoring for execution time AND memory
        Timer.Sample timerSample = actuatorPerformanceMonitor.startReportGeneration(
            report.getId(),
            0, // Will update with actual employee count
            0  // Will update with actual page count
        );
        
        try {
            // Parse employee IDs from report parameters
            List<String> employeeIds = parseEmployeeIdsFromParameters(report.getParameters());
            
            if (employeeIds.isEmpty()) {
                logger.warn("No employee IDs found in report parameters for report {}", report.getId());
                // Get all employees if no specific IDs provided
                List<EmployeeDto> allEmployees = employeeService.getAllEmployees();
                employeeIds = allEmployees.stream().map(EmployeeDto::getId).collect(Collectors.toList());
            }
              logger.info("Starting async report generation for {} employees", employeeIds.size());
            
            // Update report status
            report.setStatus(Report.ReportStatus.IN_PROGRESS);
            reportRepository.save(report);
            
            // Record memory snapshot before data processing starts
            actuatorPerformanceMonitor.recordDataProcessingStart(report.getId());
            
            // Build complete report data with certification details
            List<CompleteReportDataDto> completeReportData = buildCompleteReportData(employeeIds);
            
            // Record memory snapshot after data loading completes
            actuatorPerformanceMonitor.recordDataProcessingComplete(report.getId());
              // Record memory snapshot before PDF generation
            actuatorPerformanceMonitor.recordPdfGenerationStart(report.getId());
              // Generate PDF using memory-efficient approach when beneficial
            String filePath = generateMemoryConstrainedCertificationsPdfReport(completeReportData, report.getId());
            
            // Record memory snapshot after PDF generation completes
            actuatorPerformanceMonitor.recordPdfGenerationComplete(report.getId());
            
            // Reload the report to get the updated page count (set inside generateCertificationsPdfReport)
            report = reportRepository.findById(report.getId()).orElseThrow();
            
            // Get Actuator performance report with both timing and memory
            ActuatorPerformanceMonitor.DetailedPerformanceReport performanceReport = 
                actuatorPerformanceMonitor.completeReportGeneration(
                    timerSample, 
                    report.getId(),
                    report.getPageCount() != null ? report.getPageCount() : 0,
                    new File(filePath).length()
                );
            
            // Update report with completion (page count already set in generateCertificationsPdfReport)
            report.setStatus(Report.ReportStatus.COMPLETED);
            report.setFilePath(filePath);
            report.setCompletedAt(LocalDateTime.now());
            reportRepository.save(report);
            
            logger.info("Successfully completed async report generation for report {} - {} pages, {} KB, {} ms", 
                       report.getId(), 
                       report.getPageCount(),
                       new File(filePath).length() / 1024,
                       performanceReport.durationMs);
            
        } catch (Exception e) {
            logger.error("Error generating report {}: {}", report.getId(), e.getMessage(), e);
            
            // Complete Actuator monitoring even on failure to capture error metrics
            try {
                actuatorPerformanceMonitor.completeReportGeneration(timerSample, report.getId(), 0, 0L);
            } catch (Exception monitoringException) {
                logger.warn("Failed to complete performance monitoring for failed report {}: {}", 
                           report.getId(), monitoringException.getMessage());
            }
            
            // Update report with error status
            report.setStatus(Report.ReportStatus.FAILED);
            report.setErrorMessage(e.getMessage());
            report.setCompletedAt(LocalDateTime.now());
            reportRepository.save(report);
        }
        
        return CompletableFuture.completedFuture(null);
    }
      /**
     * Builds complete report data with detailed certification information using efficient batch queries
     * 
     * SIMPLIFIED APPROACH: Always process all data in single pass for optimal performance
     * - For reports up to 2MB (~1000+ employees), single-pass is most efficient
     * - Chunking infrastructure kept as fallback for extreme edge cases
     * - Validated through performance testing: 8.98s for 300 employees
     */
    private List<CompleteReportDataDto> buildCompleteReportData(List<String> employeeIds) {
        logger.info("Building complete report data for {} employees using single-pass processing", employeeIds.size());
        
        // SIMPLIFIED: Always use single comprehensive query for optimal performance
        // This approach is validated to handle 300+ employees efficiently (8.98s)
        // For 2MB reports (~1000 employees), this remains the optimal strategy
        List<CompleteReportDataDto> reportData;
        
        // Single-pass processing (recommended for all typical report sizes)
        reportData = certificationService.getCertificationDataChunk(employeeIds);
        logger.info("Processed all {} employees in single optimized query", employeeIds.size());
        
        // Fallback chunking logic - only activated for extreme cases (disabled by default)
        // Uncomment and modify the threshold if chunking becomes necessary for very large datasets
        /*
        if (employeeIds.size() <= 1000) {  // Increased threshold - most reports fit in single pass
            // Single comprehensive query - optimal for typical report sizes
            reportData = certificationService.getCertificationDataChunk(employeeIds);
        } else {
            // Chunked processing for extreme edge cases (>1000 employees)
            reportData = new ArrayList<>();
            int chunkSize = 50; // Process 50 employees at a time
            
            for (int i = 0; i < employeeIds.size(); i += chunkSize) {
                int endIndex = Math.min(i + chunkSize, employeeIds.size());
                List<String> chunk = employeeIds.subList(i, endIndex);
                
                List<CompleteReportDataDto> chunkData = certificationService.getCertificationDataChunk(chunk);
                reportData.addAll(chunkData);
                
                logger.debug("Processed chunk {}/{} ({} employees)", 
                           (i / chunkSize) + 1, 
                           (employeeIds.size() + chunkSize - 1) / chunkSize,
                           chunk.size());
            }
        }
        */
        
        // Sort employees by department, then by last name (already sorted in query but ensure consistency)
        reportData.sort((a, b) -> {
            int deptCompare = a.getEmployee().getDepartment().compareTo(b.getEmployee().getDepartment());
            if (deptCompare != 0) return deptCompare;
            return a.getEmployee().getLastName().compareTo(b.getEmployee().getLastName());
        });
        
        logger.info("Built complete report data for {} employees with efficient batch queries", reportData.size());
        return reportData;
    }

    /**
     * Creates flattened activity data where each record represents an employee-certification combination
     */
    private List<EmployeeCertificationActivityDto> createActivityDataFromReportData(List<CompleteReportDataDto> reportData) {
        List<EmployeeCertificationActivityDto> activityData = new ArrayList<>();
        
        for (CompleteReportDataDto employeeData : reportData) {
            EmployeeDto employee = employeeData.getEmployee();
            Long completedCount = employeeData.getCompletedCertificationsCount();
            Long inProgressCount = employeeData.getInProgressCertificationsCount();
            Long failedCount = employeeData.getFailedCertificationsCount();
            
            if (employeeData.getCertifications() != null && !employeeData.getCertifications().isEmpty()) {
                // Create one activity record for each certification
                for (CertificationDto certification : employeeData.getCertifications()) {
                    EmployeeCertificationActivityDto activity = new EmployeeCertificationActivityDto(
                            employee, certification, completedCount, inProgressCount, failedCount);
                    activityData.add(activity);
                }
            } else {
                // Create one record for employees with no certifications
                EmployeeCertificationActivityDto activity = new EmployeeCertificationActivityDto(
                        employee, null, completedCount, inProgressCount, failedCount);
                activityData.add(activity);
            }
        }
          logger.info("Created {} activity records from {} employee records", activityData.size(), reportData.size());
        return activityData;
    }
}