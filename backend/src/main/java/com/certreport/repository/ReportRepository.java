package com.certreport.repository;

import com.certreport.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, String> {
    
    List<Report> findByStatusOrderByCreatedAtDesc(Report.ReportStatus status);
    
    @Query("SELECT r FROM Report r WHERE r.createdAt >= :since ORDER BY r.createdAt DESC")
    List<Report> findRecentReports(LocalDateTime since);
    
    @Query("SELECT COUNT(r) FROM Report r WHERE r.status = :status")
    Long countByStatus(Report.ReportStatus status);
}
