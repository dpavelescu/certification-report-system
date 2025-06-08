package com.certreport.service;

import com.certreport.dto.EmployeeDto;
import com.certreport.dto.ReportRequestDto;
import com.certreport.model.Report;
import com.certreport.repository.ReportRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ReportService {
    
    private final ReportRepository reportRepository;
    private final EmployeeService employeeService;
    private final MeterRegistry meterRegistry;
    private final AtomicInteger activeReports = new AtomicInteger(0);
    
    @Autowired
    public ReportService(ReportRepository reportRepository, 
                        EmployeeService employeeService,
                        MeterRegistry meterRegistry) {
        this.reportRepository = reportRepository;
        this.employeeService = employeeService;
        this.meterRegistry = meterRegistry;
    }
      @Async("reportTaskExecutor")
    public CompletableFuture<Report> generateReport(ReportRequestDto request) {
        Timer.Sample sample = Timer.start(meterRegistry);
        meterRegistry.gauge("reports.active", activeReports.incrementAndGet());
        
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
        
        // Update status to IN_PROGRESS and continue processing asynchronously
        report.setStatus(Report.ReportStatus.IN_PROGRESS);
        report = reportRepository.save(report);
        
        try {
            // Get employee data
            List<EmployeeDto> employees = employeeService.getEmployeesByIds(request.getEmployeeIds());
            
            // Generate PDF report
            String filePath = generatePdfReport(employees, report.getId());
            
            // Update report with results
            report.setFilePath(filePath);
            report.setPageCount(calculatePageCount(employees.size()));
            report.setStatus(Report.ReportStatus.COMPLETED);
            report.setCompletedAt(LocalDateTime.now());
            
            // Record metrics
            meterRegistry.summary("reports.pages").record(report.getPageCount());
            
        } catch (Exception e) {
            report.setStatus(Report.ReportStatus.FAILED);
            report.setErrorMessage(e.getMessage());
            report.setCompletedAt(LocalDateTime.now());
        } finally {
            sample.stop(meterRegistry.timer("reports.generation.time", 
                    "reportType", report.getType()));
            activeReports.decrementAndGet();
        }
        
        return CompletableFuture.completedFuture(reportRepository.save(report));
    }
    
    public Report createInitialReport(ReportRequestDto request) {
        // Generate report name based on type and timestamp
        String reportName = generateReportName(request.getReportType(), request.getEmployeeIds().size());
        
        // Create and save report record with QUEUED status
        Report report = new Report(
                reportName,
                request.getReportType() != null ? request.getReportType() : "EMPLOYEE_DEMOGRAPHICS",
                request.toString()
        );
        report.setStatus(Report.ReportStatus.QUEUED);
        report.setStartedAt(LocalDateTime.now());
        
        return reportRepository.save(report);
    }
    
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
            throw new RuntimeException("Report not ready for download");        }
        
        File file = new File(report.getFilePath());
        if (!file.exists()) {
            throw new RuntimeException("Report file not found");
        }
        
        return file;
    }
    
    private String generateReportName(String reportType, int employeeCount) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String type = reportType != null ? reportType.toLowerCase().replace("_", " ") : "employee demographics";
        return String.format("%s_report_%d_employees_%s.pdf", 
            type.replace(" ", "_"), employeeCount, timestamp);
    }
}
