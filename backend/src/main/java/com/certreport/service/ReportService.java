package com.certreport.service;

import com.certreport.dto.EmployeeDto;
import com.certreport.dto.ReportRequestDto;
import com.certreport.dto.CompleteReportDataDto;
import com.certreport.dto.CertificationDto;
import com.certreport.dto.EmployeeCertificationActivityDto;
import com.certreport.model.Report;
import com.certreport.model.Certification;
import com.certreport.repository.ReportRepository;
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
    private final ReportRepository reportRepository;
    private final EmployeeService employeeService;
    private final CertificationService certificationService;
    private final PerformanceMonitoringService performanceMonitoringService;
      public ReportService(ReportRepository reportRepository, 
                        EmployeeService employeeService,
                        CertificationService certificationService,
                        PerformanceMonitoringService performanceMonitoringService) {
        this.reportRepository = reportRepository;
        this.employeeService = employeeService;
        this.certificationService = certificationService;
        this.performanceMonitoringService = performanceMonitoringService;    }

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
          return report;    }    private String generateCertificationsPdfReport(List<CompleteReportDataDto> reportData, String reportId) throws JRException {
        // Load the certifications report template
        InputStream reportTemplate = getClass().getResourceAsStream("/reports/certifications_report.jrxml");
        if (reportTemplate == null) {
            throw new RuntimeException("Certifications report template not found at /reports/certifications_report.jrxml");
        }
        
        // Compile report
        JasperReport jasperReport = JasperCompileManager.compileReport(reportTemplate);
        
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
        
        // Fill report
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
        
        // Export to PDF
        String fileName = String.format("CertificationReport_%s_%s.pdf", 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")),
                reportId.substring(0, 8));
        String filePath = System.getProperty("java.io.tmpdir") + File.separator + fileName;
        
        JasperExportManager.exportReportToPdfFile(jasperPrint, filePath);
        
        logger.info("Generated certifications report with {} employees and {} certification activities", 
                   reportData.size(), activityData.size());
        
        return filePath;
    }
    
    private int calculatePageCount(int employeeCount) {
        // Simple calculation: approximately 20 employees per page
        return Math.max(1, (int) Math.ceil(employeeCount / 20.0));
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
        // Parse employee IDs from parameters string like "ReportRequestDto{reportType='employee_demographics', employeeIds=[2, 5]}"
        if (parameters == null || parameters.trim().isEmpty()) {
            logger.warn("Parameters is null or empty, returning empty employee ID list");
            return new ArrayList<>();
        }
        
        try {
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
        Timer.Sample timerSample = performanceMonitoringService.startReportGeneration(report.getId());
        
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
            
            // Build complete report data with certification details
            List<CompleteReportDataDto> completeReportData = buildCompleteReportData(employeeIds);
            
            // Generate PDF
            String filePath = generateCertificationsPdfReport(completeReportData, report.getId());
            
            // Update report with completion
            report.setStatus(Report.ReportStatus.COMPLETED);
            report.setFilePath(filePath);
            report.setCompletedAt(LocalDateTime.now());
            report.setPageCount(calculatePageCount(completeReportData.size()));
            reportRepository.save(report);
            
            performanceMonitoringService.completeReportGeneration(timerSample, report.getId());
            
            logger.info("Successfully completed async report generation for report {}", report.getId());
            
        } catch (Exception e) {
            logger.error("Error generating report {}: {}", report.getId(), e.getMessage(), e);
            
            // Update report with error status
            report.setStatus(Report.ReportStatus.FAILED);
            report.setErrorMessage(e.getMessage());
            report.setCompletedAt(LocalDateTime.now());
            reportRepository.save(report);
            
            performanceMonitoringService.recordReportGenerationFailure(timerSample, report.getId(), e);
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * Builds complete report data with detailed certification information including stages and tasks
     */
    private List<CompleteReportDataDto> buildCompleteReportData(List<String> employeeIds) {
        List<CompleteReportDataDto> reportData = new ArrayList<>();
        
        // Get employee data
        List<EmployeeDto> employees = employeeService.getEmployeesByIds(employeeIds);
        
        for (EmployeeDto employee : employees) {
            // Get detailed certifications for each employee including stage progress
            List<CertificationDto> certifications = certificationService.getCertificationsByEmployeeId(employee.getId());
            
            // Enrich certification data with proper sorting
            certifications = enrichAndSortCertificationData(certifications);
            
            CompleteReportDataDto employeeReportData = new CompleteReportDataDto(employee, certifications);
            reportData.add(employeeReportData);
        }
        
        // Sort employees by department, then by last name
        reportData.sort((a, b) -> {
            int deptCompare = a.getEmployee().getDepartment().compareTo(b.getEmployee().getDepartment());
            if (deptCompare != 0) return deptCompare;
            return a.getEmployee().getLastName().compareTo(b.getEmployee().getLastName());
        });
        
        logger.info("Built complete report data for {} employees with detailed certification information", reportData.size());
        return reportData;
    }
    
    /**
     * Enriches certification data with proper sorting for comprehensive reporting
     */
    private List<CertificationDto> enrichAndSortCertificationData(List<CertificationDto> certifications) {
        if (certifications == null || certifications.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Sort certifications by status priority (Completed, In-Progress, Failed, others)
        // Then by certification name
        return certifications.stream()
                .sorted((a, b) -> {
                    // First sort by status priority
                    int statusPriority1 = getStatusPriority(a.getStatus());
                    int statusPriority2 = getStatusPriority(b.getStatus());
                    
                    if (statusPriority1 != statusPriority2) {
                        return Integer.compare(statusPriority1, statusPriority2);
                    }
                    
                    // Then by certification name
                    String name1 = a.getCertificationDefinition() != null ? 
                            a.getCertificationDefinition().getName() : "";
                    String name2 = b.getCertificationDefinition() != null ? 
                            b.getCertificationDefinition().getName() : "";
                    return name1.compareTo(name2);
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Returns priority for certification status sorting
     */
    private int getStatusPriority(Certification.CertificationStatus status) {
        if (status == null) return 99;
        
        switch (status) {
            case COMPLETED: return 1;
            case IN_PROGRESS: return 2;
            case FAILED: return 3;
            case OVERDUE: return 4;
            case SUSPENDED: return 5;
            case EXPIRED: return 6;
            case NOT_STARTED: return 7;
            default: return 8;
        }
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
