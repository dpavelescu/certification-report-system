package com.certreport.service;

import com.certreport.dto.EmployeeDto;
import com.certreport.dto.ReportRequestDto;
import com.certreport.dto.CompleteReportDataDto;
import com.certreport.dto.CertificationDto;
import com.certreport.model.Report;
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
        this.performanceMonitoringService = performanceMonitoringService;
    }@Async("reportTaskExecutor")
    public CompletableFuture<Report> generateReportAsync(Report report) {
        Timer.Sample sample = performanceMonitoringService.startReportGeneration(report.getId());
        
        try {
            // Update status to IN_PROGRESS
            report.setStatus(Report.ReportStatus.IN_PROGRESS);
            report = reportRepository.save(report);
            
            // Parse the parameters to get employee IDs
            String parameters = report.getParameters();
            // Extract employee IDs from parameters string like "ReportRequestDto{reportType='employee_demographics', employeeIds=[2, 5]}"
            List<String> employeeIds = parseEmployeeIdsFromParameters(parameters);
              // Get employee data
            List<EmployeeDto> employees = employeeService.getEmployeesByIds(employeeIds);
            
            // Get complete report data including certifications
            List<CompleteReportDataDto> completeReportData = new ArrayList<>();
            for (EmployeeDto employee : employees) {
                List<CertificationDto> certifications = certificationService.getCertificationsByEmployeeId(employee.getId());
                completeReportData.add(new CompleteReportDataDto(employee, certifications));
            }
              // Generate PDF report with complete data
            String filePath = generateCertificationsPdfReport(completeReportData, report.getId());
              // Update report with results
            report.setFilePath(filePath);
            report.setPageCount(calculatePageCount(completeReportData.size()));
            report.setStatus(Report.ReportStatus.COMPLETED);
            report.setCompletedAt(LocalDateTime.now());
            
            // Record successful completion
            performanceMonitoringService.completeReportGeneration(sample, report.getId());
            
        } catch (Exception e) {
            report.setStatus(Report.ReportStatus.FAILED);
            report.setErrorMessage(e.getMessage());
            report.setCompletedAt(LocalDateTime.now());
            
            // Record failure
            performanceMonitoringService.recordReportGenerationFailure(sample, report.getId(), e);
        }
        
        return CompletableFuture.completedFuture(reportRepository.save(report));
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
          return report;    }    private String generateCertificationsPdfReport(List<CompleteReportDataDto> reportData, String reportId) throws JRException {        // Load the certifications report template
        InputStream reportTemplate = getClass().getResourceAsStream("/reports/certifications_report.jrxml");
        if (reportTemplate == null) {
            throw new RuntimeException("Certifications report template not found at /reports/certifications_report.jrxml");
        }
        
        // Compile report
        JasperReport jasperReport = JasperCompileManager.compileReport(reportTemplate);
        
        // Prepare data source with complete report data
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(reportData);
        
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
          logger.info("Generated certifications report with {} employees and {} total certifications", 
                   reportData.size(), totalCertifications);
        
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
}
