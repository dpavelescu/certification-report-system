package com.certreport.controller;

import com.certreport.dto.ReportRequestDto;
import com.certreport.model.Report;
import com.certreport.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(
    origins = {"http://localhost:3000", "http://localhost:5173"}, 
    methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS},
    allowedHeaders = "*",
    allowCredentials = "true"
)
public class ReportController {
    
    private static final Logger logger = LoggerFactory.getLogger(ReportController.class);
    private final ReportService reportService;
    
    @Autowired
    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }    @PostMapping("/generate")
    public ResponseEntity<Report> generateReport(@RequestBody ReportRequestDto request) {
        logger.info("Received report generation request: reportType={}, employeeIds={}", 
                   request.getReportType(), request.getEmployeeIds());
        try {
            // Start async report generation and get the initial report object
            CompletableFuture<Report> reportFuture = reportService.generateReport(request);
            
            // Get the initial report object (it should be created synchronously with QUEUED status)
            Report initialReport = reportService.createInitialReport(request);
            
            logger.info("Created initial report with ID: {}", initialReport.getId());
            return ResponseEntity.ok(initialReport);
        } catch (Exception e) {
            logger.error("Error generating report", e);
            return ResponseEntity.badRequest().build();
        }
    }
      @GetMapping("/{id}/status")
    public ResponseEntity<ReportStatusResponse> getReportStatus(@PathVariable String id) {
        try {
            Report report = reportService.getReportStatus(id);
            ReportStatusResponse response = new ReportStatusResponse(
                    report.getId(),
                    report.getStatus(),
                    calculateProgress(report),
                    report.getCompletedAt(),
                    getStatusMessage(report)
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
      @GetMapping("/{id}/download")
    public ResponseEntity<FileSystemResource> downloadReport(@PathVariable String id) {
        try {
            File reportFile = reportService.getReportFile(id);
            FileSystemResource resource = new FileSystemResource(reportFile);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + reportFile.getName())
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
      @GetMapping
    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
    public ResponseEntity<List<Report>> getAllReports() {
        return ResponseEntity.ok(reportService.getAllReports());
    }
    
    private int calculateProgress(Report report) {
        switch (report.getStatus()) {
            case QUEUED:
                return 0;
            case IN_PROGRESS:
                return 50; // Simple progress calculation for MVP
            case COMPLETED:
                return 100;
            case FAILED:
                return 0;
            default:
                return 0;
        }
    }
    
    private String getStatusMessage(Report report) {
        switch (report.getStatus()) {
            case QUEUED:
                return "Report is queued for processing";
            case IN_PROGRESS:
                return "Generating report...";
            case COMPLETED:
                return "Report generated successfully";
            case FAILED:
                return "Report generation failed: " + 
                       (report.getErrorMessage() != null ? report.getErrorMessage() : "Unknown error");
            default:
                return "Unknown status";
        }
    }
      // Inner class for response
    public static class ReportStatusResponse {
        private String reportId;
        private Report.ReportStatus status;
        private int progress;
        private java.time.LocalDateTime completedAt;
        private String message;
        
        public ReportStatusResponse(String reportId, Report.ReportStatus status, int progress, 
                                  java.time.LocalDateTime completedAt, String message) {
            this.reportId = reportId;
            this.status = status;
            this.progress = progress;
            this.completedAt = completedAt;
            this.message = message;
        }
          // Getters
        public String getReportId() { return reportId; }
        public Report.ReportStatus getStatus() { return status; }
        public int getProgress() { return progress; }
        public java.time.LocalDateTime getCompletedAt() { return completedAt; }
        public String getMessage() { return message; }
    }
}
