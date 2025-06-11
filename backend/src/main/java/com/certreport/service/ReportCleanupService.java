package com.certreport.service;

import com.certreport.model.Report;
import com.certreport.repository.ReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Service for cleaning up stuck reports and managing report lifecycle
 */
@Service
public class ReportCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(ReportCleanupService.class);
    
    // Configuration for cleanup thresholds
    private static final int STUCK_REPORT_TIMEOUT_MINUTES = 30; // 30 minutes
    private static final int MAX_REPORT_AGE_DAYS = 7; // 7 days
    private static final int MAX_REPORTS_TO_KEEP = 1000; // Maximum number of reports to keep

    @Autowired
    private ReportRepository reportRepository;

    /**
     * Scheduled cleanup task that runs every 30 minutes
     */
    @Scheduled(fixedRate = 1800000) // 30 minutes in milliseconds
    public void performScheduledCleanup() {
        logger.info("Starting scheduled report cleanup");
        
        cleanupStuckReports();
        cleanupOldReports();
        cleanupOrphanedFiles();
        
        logger.info("Scheduled report cleanup completed");
    }

    /**
     * Clean up reports that are stuck in QUEUED or IN_PROGRESS status
     */
    public int cleanupStuckReports() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(STUCK_REPORT_TIMEOUT_MINUTES);
          List<Report> stuckReports = reportRepository.findByStatusInAndStartedAtBefore(
            Arrays.asList(Report.ReportStatus.QUEUED, Report.ReportStatus.IN_PROGRESS),
            cutoffTime
        );
        
        int cleanedCount = 0;
        for (Report report : stuckReports) {
            logger.warn("Cleaning up stuck report: {} (Status: {}, Started: {})", 
                       report.getId(), report.getStatus(), report.getStartedAt());
            
            report.setStatus(Report.ReportStatus.FAILED);
            report.setErrorMessage("Report generation timed out and was cleaned up");
            report.setCompletedAt(LocalDateTime.now());
            
            reportRepository.save(report);
            cleanedCount++;
        }
        
        if (cleanedCount > 0) {
            logger.info("Cleaned up {} stuck reports", cleanedCount);
        }
        
        return cleanedCount;
    }

    /**
     * Clean up old reports that exceed the retention period
     */
    public int cleanupOldReports() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(MAX_REPORT_AGE_DAYS);
        
        List<Report> oldReports = reportRepository.findByStartedAtBefore(cutoffTime);
        
        int deletedCount = 0;
        for (Report report : oldReports) {
            // Delete associated file if it exists
            deleteReportFile(report);
            
            // Delete the report record
            reportRepository.delete(report);
            deletedCount++;
        }
        
        if (deletedCount > 0) {
            logger.info("Deleted {} old reports", deletedCount);
        }
        
        return deletedCount;
    }

    /**
     * Clean up reports when the total count exceeds the maximum
     */
    public int cleanupExcessReports() {
        long totalReports = reportRepository.count();
        
        if (totalReports <= MAX_REPORTS_TO_KEEP) {
            return 0;
        }
        
        long excessCount = totalReports - MAX_REPORTS_TO_KEEP;
        List<Report> oldestReports = reportRepository.findOldestReports((int) excessCount);
        
        int deletedCount = 0;
        for (Report report : oldestReports) {
            deleteReportFile(report);
            reportRepository.delete(report);
            deletedCount++;
        }
        
        if (deletedCount > 0) {
            logger.info("Deleted {} excess reports to maintain maximum count", deletedCount);
        }
        
        return deletedCount;
    }

    /**
     * Delete a specific report by ID
     */
    public boolean deleteReport(String reportId) {
        return reportRepository.findById(reportId)
            .map(report -> {
                deleteReportFile(report);
                reportRepository.delete(report);
                logger.info("Manually deleted report: {}", reportId);
                return true;
            })
            .orElse(false);
    }

    /**
     * Clean up orphaned files (files without corresponding database records)
     */
    public int cleanupOrphanedFiles() {
        // Get all report file paths from database
        List<String> dbFilePaths = reportRepository.findAllFilePaths();
        
        // Get all PDF files in the temp directory
        String tempDir = System.getProperty("java.io.tmpdir");
        File tempDirFile = new File(tempDir);
        
        if (!tempDirFile.exists() || !tempDirFile.isDirectory()) {
            return 0;
        }
        
        File[] pdfFiles = tempDirFile.listFiles((dir, name) -> 
            name.startsWith("CertificationReport_") && name.endsWith(".pdf"));
        
        if (pdfFiles == null) {
            return 0;
        }
        
        int deletedCount = 0;
        for (File file : pdfFiles) {
            String filePath = file.getAbsolutePath();
            
            // If file is not referenced in database, delete it
            if (!dbFilePaths.contains(filePath)) {
                if (file.delete()) {
                    logger.info("Deleted orphaned file: {}", filePath);
                    deletedCount++;
                } else {
                    logger.warn("Failed to delete orphaned file: {}", filePath);
                }
            }
        }
        
        if (deletedCount > 0) {
            logger.info("Deleted {} orphaned files", deletedCount);
        }
        
        return deletedCount;
    }

    /**
     * Delete the physical file associated with a report
     */
    private void deleteReportFile(Report report) {
        if (report.getFilePath() != null) {
            File file = new File(report.getFilePath());
            if (file.exists()) {
                if (file.delete()) {
                    logger.debug("Deleted report file: {}", report.getFilePath());
                } else {
                    logger.warn("Failed to delete report file: {}", report.getFilePath());
                }
            }
        }
    }

    /**
     * Get cleanup statistics
     */
    public CleanupStats getCleanupStats() {
        long totalReports = reportRepository.count();
        long queuedReports = reportRepository.countByStatus(Report.ReportStatus.QUEUED);
        long inProgressReports = reportRepository.countByStatus(Report.ReportStatus.IN_PROGRESS);
        long completedReports = reportRepository.countByStatus(Report.ReportStatus.COMPLETED);
        long failedReports = reportRepository.countByStatus(Report.ReportStatus.FAILED);
        
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(STUCK_REPORT_TIMEOUT_MINUTES);        long stuckReports = reportRepository.countByStatusInAndStartedAtBefore(
            Arrays.asList(Report.ReportStatus.QUEUED, Report.ReportStatus.IN_PROGRESS),
            cutoffTime
        );
        
        return new CleanupStats(totalReports, queuedReports, inProgressReports, 
                               completedReports, failedReports, stuckReports);
    }    /**
     * Simple data holder for cleanup statistics
     */
    public record CleanupStats(
        long totalReports,
        long queuedReports,
        long inProgressReports,
        long completedReports,
        long failedReports,
        long stuckReports
    ) {}
}
