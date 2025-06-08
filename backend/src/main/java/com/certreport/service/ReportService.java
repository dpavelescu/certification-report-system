package com.certreport.service;

import com.certreport.dto.EmployeeDto;
import com.certreport.dto.ReportRequestDto;
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
    private final PerformanceMonitoringService performanceMonitoringService;
      public ReportService(ReportRepository reportRepository, 
                        EmployeeService employeeService,
                        PerformanceMonitoringService performanceMonitoringService) {
        this.reportRepository = reportRepository;
        this.employeeService = employeeService;
        this.performanceMonitoringService = performanceMonitoringService;
    }    @Async("reportTaskExecutor")
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
            
            // Generate PDF report
            String filePath = generatePdfReport(employees, report.getId());
            
            // Update report with results
            report.setFilePath(filePath);
            report.setPageCount(calculatePageCount(employees.size()));
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
        
        return report;    }

    private String generatePdfReport(List<EmployeeDto> employees, String reportId) throws JRException {
        // Load report template
        InputStream reportTemplate = getClass().getResourceAsStream("/reports/employee_demographics.jrxml");
        if (reportTemplate == null) {
            throw new RuntimeException("Report template not found");
        }
        
        // Compile report
        JasperReport jasperReport = JasperCompileManager.compileReport(reportTemplate);
        
        // Prepare data source
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(employees);
        
        // Report parameters
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("REPORT_TITLE", "Employee Demographics Report");
        parameters.put("GENERATION_DATE", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        parameters.put("TOTAL_EMPLOYEES", employees.size());
        
        // Fill report
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
        
        // Export to PDF
        String fileName = String.format("CertificationReport_%s_%s.pdf", 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")),
                reportId.toString().substring(0, 8));
        String filePath = System.getProperty("java.io.tmpdir") + File.separator + fileName;
        
        JasperExportManager.exportReportToPdfFile(jasperPrint, filePath);
        
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
        return reportRepository.findAll();
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
