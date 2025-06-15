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

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    
    private static final Logger logger = LoggerFactory.getLogger(ReportController.class);
    private final ReportService reportService;
    private final ReportCleanupService reportCleanupService;
    
    public ReportController(ReportService reportService, ReportCleanupService reportCleanupService) {
        this.reportService = reportService;
        this.reportCleanupService = reportCleanupService;
    }    @PostMapping("/generate")    public ResponseEntity<Report> generateReport(@RequestBody ReportRequestDto request) {
        logger.info("Received report generation request: reportType={}, employeeIds={}", 
                   request.getReportType(), request.getEmployeeIds());
        try {
            // Create report and start async processing
            Report initialReport = reportService.generateReport(request);
            
            if (initialReport == null) {
                logger.error("Report generation failed - service returned null");
                return ResponseEntity.badRequest().build();
            }
            
            logger.info("Created initial report with ID: {}", initialReport.getId());
            return ResponseEntity.ok(initialReport);
        } catch (Exception e) {
            logger.error("Error generating report", e);
            return ResponseEntity.badRequest().build();
        }
    }@GetMapping("/{id}/status")
    public ResponseEntity<ReportStatusResponse> getReportStatus(@PathVariable String id) {
        try {
            Report report = reportService.getReportStatus(id);
            return ResponseEntity.ok(new ReportStatusResponse(
                    report.getId(),
                    report.getStatus(),
                    calculateProgress(report),
                    report.getCompletedAt(),
                    getStatusMessage(report)
            ));
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
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage(), "reportId", id));
        }
    }

    @GetMapping
    public ResponseEntity<List<Report>> getAllReports() {
        return ResponseEntity.ok(reportService.getAllReports());
    }

    /**
     * Delete a specific report
     */    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteReport(@PathVariable String id) {
        try {
            boolean deleted = reportCleanupService.deleteReport(id);
            return deleted 
                ? ResponseEntity.ok(Map.of("message", "Report deleted successfully"))
                : ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error deleting report: {}", id, e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to delete report: " + e.getMessage()));
        }
    }

    /**
     * Clean up stuck reports
     */    @PostMapping("/cleanup/stuck")
    public ResponseEntity<Map<String, Object>> cleanupStuckReports() {
        try {
            int cleanedCount = reportCleanupService.cleanupStuckReports();
            logger.info("Manual cleanup of stuck reports completed: {} reports cleaned", cleanedCount);
            return ResponseEntity.ok(Map.of(
                "message", "Cleanup completed",
                "cleanedReports", cleanedCount
            ));
        } catch (Exception e) {
            logger.error("Error during stuck reports cleanup", e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Cleanup failed: " + e.getMessage()));
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
    }    // Simple response record instead of verbose inner class
    public record ReportStatusResponse(
        String reportId,
        Report.ReportStatus status,
        int progress,
        java.time.LocalDateTime completedAt,
        String message
    ) {}

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