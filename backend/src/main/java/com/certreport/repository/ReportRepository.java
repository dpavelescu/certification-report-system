package com.certreport.repository;

import com.certreport.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, String> {
    
    List<Report> findByStatusOrderByCreatedAtDesc(Report.ReportStatus status);
    
    List<Report> findAllByOrderByCreatedAtDesc();
    
    @Query("SELECT r FROM Report r WHERE r.createdAt >= :since ORDER BY r.createdAt DESC")
    List<Report> findRecentReports(LocalDateTime since);
    
    @Query("SELECT COUNT(r) FROM Report r WHERE r.status = :status")
    Long countByStatus(Report.ReportStatus status);
    
    // Cleanup related queries
    List<Report> findByStatusInAndStartedAtBefore(List<Report.ReportStatus> statuses, LocalDateTime cutoffTime);
    
    List<Report> findByStartedAtBefore(LocalDateTime cutoffTime);
    
    @Query("SELECT COUNT(r) FROM Report r WHERE r.status IN :statuses AND r.startedAt < :cutoffTime")
    Long countByStatusInAndStartedAtBefore(@Param("statuses") List<Report.ReportStatus> statuses, 
                                          @Param("cutoffTime") LocalDateTime cutoffTime);
    
    @Query("SELECT r.filePath FROM Report r WHERE r.filePath IS NOT NULL")
    List<String> findAllFilePaths();
      @Query(value = "SELECT * FROM reports ORDER BY started_at ASC LIMIT :limit", nativeQuery = true)
    List<Report> findOldestReports(@Param("limit") int limit);
}
