package com.certreport.controller;

import com.certreport.dto.ReportRequestDto;
import com.certreport.model.Report;
import com.certreport.service.ReportService;
import com.certreport.service.ReportCleanupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

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
    private final ReportCleanupService reportCleanupService;
    
    public ReportController(ReportService reportService, ReportCleanupService reportCleanupService) {
        this.reportService = reportService;
        this.reportCleanupService = reportCleanupService;
    }    @PostMapping("/generate")
    public ResponseEntity<Report> generateReport(@RequestBody ReportRequestDto request) {
        logger.info("Received report generation request: reportType={}, employeeIds={}", 
                   request.getReportType(), request.getEmployeeIds());
        try {
            // Create report and start async processing
            Report initialReport = reportService.generateReport(request);
            
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
    }    @GetMapping("/{id}/download")
    public ResponseEntity<?> downloadReport(@PathVariable String id) {
        try {
            File reportFile = reportService.getReportFile(id);
            FileSystemResource resource = new FileSystemResource(reportFile);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + reportFile.getName())
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(resource);
        } catch (Exception e) {
            logger.error("Error downloading report {}: {}", id, e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("reportId", id);
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping
    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
    public ResponseEntity<List<Report>> getAllReports() {
        return ResponseEntity.ok(reportService.getAllReports());
    }

    /**
     * Delete a specific report
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteReport(@PathVariable String id) {
        try {
            boolean deleted = reportCleanupService.deleteReport(id);
            Map<String, String> response = new HashMap<>();
            
            if (deleted) {
                response.put("message", "Report deleted successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("error", "Report not found");
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error deleting report: {}", id, e);
            Map<String, String> response = new HashMap<>();
            response.put("error", "Failed to delete report: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Clean up stuck reports
     */
    @PostMapping("/cleanup/stuck")
    public ResponseEntity<Map<String, Object>> cleanupStuckReports() {
        try {
            int cleanedCount = reportCleanupService.cleanupStuckReports();
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Cleanup completed");
            response.put("cleanedReports", cleanedCount);
            
            logger.info("Manual cleanup of stuck reports completed: {} reports cleaned", cleanedCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error during stuck reports cleanup", e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Cleanup failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get cleanup statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<ReportCleanupService.CleanupStats> getCleanupStats() {
        try {
            ReportCleanupService.CleanupStats stats = reportCleanupService.getCleanupStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error getting cleanup stats", e);
            return ResponseEntity.badRequest().build();
        }
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

    @GetMapping("/{id}")
    public ResponseEntity<Report> getReport(@PathVariable String id) {
        try {
            Report report = reportService.getReportStatus(id);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            logger.error("Error getting report {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}