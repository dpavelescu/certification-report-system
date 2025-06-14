package com.certreport.repository;

import com.certreport.model.Certification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CertificationRepository extends JpaRepository<Certification, String> {
    
    @Query("SELECT c FROM Certification c " +
           "JOIN FETCH c.employee e " +
           "JOIN FETCH c.certificationDefinition cd " +
           "WHERE (:employeeIds IS NULL OR e.id IN :employeeIds) " +
           "AND (:certificationDefinitionIds IS NULL OR cd.id IN :certificationDefinitionIds) " +
           "AND (:statuses IS NULL OR c.status IN :statuses) " +
           "AND (:startDate IS NULL OR c.enrolledAt >= :startDate) " +
           "AND (:endDate IS NULL OR c.enrolledAt <= :endDate)")
    Page<Certification> findWithFilters(@Param("employeeIds") List<String> employeeIds,
                                       @Param("certificationDefinitionIds") List<String> certificationDefinitionIds,
                                       @Param("statuses") List<Certification.CertificationStatus> statuses,
                                       @Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate,
                                       Pageable pageable);
    
    @Query("SELECT c FROM Certification c WHERE c.employee.id = :employeeId ORDER BY c.enrolledAt DESC")
    List<Certification> findByEmployeeId(@Param("employeeId") String employeeId);
    
    @Query("SELECT c FROM Certification c WHERE c.certificationDefinition.id = :certificationDefinitionId ORDER BY c.enrolledAt DESC")
    List<Certification> findByCertificationDefinitionId(@Param("certificationDefinitionId") String certificationDefinitionId);
    
    @Query("SELECT c FROM Certification c WHERE c.employee.id = :employeeId AND c.certificationDefinition.id = :certificationDefinitionId")
    Optional<Certification> findByEmployeeIdAndCertificationDefinitionId(@Param("employeeId") String employeeId, 
                                                                          @Param("certificationDefinitionId") String certificationDefinitionId);

    @Query("SELECT c FROM Certification c WHERE c.status = :status ORDER BY c.enrolledAt DESC")
    List<Certification> findByStatus(@Param("status") Certification.CertificationStatus status);
    
    @Query("SELECT c FROM Certification c WHERE c.status IN :statuses ORDER BY c.enrolledAt DESC")
    List<Certification> findByStatusIn(@Param("statuses") List<Certification.CertificationStatus> statuses);
    
    @Query("SELECT c FROM Certification c WHERE c.dueDate IS NOT NULL AND c.dueDate < :currentDate AND c.status NOT IN ('COMPLETED', 'FAILED')")
    List<Certification> findOverdue(@Param("currentDate") LocalDateTime currentDate);
    
    @Query("SELECT c FROM Certification c WHERE c.dueDate IS NOT NULL AND c.dueDate BETWEEN :startDate AND :endDate AND c.status NOT IN ('COMPLETED', 'FAILED')")
    List<Certification> findDueSoon(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Efficient batch queries for reporting
    @Query("SELECT c FROM Certification c " +
           "JOIN FETCH c.employee e " +
           "JOIN FETCH c.certificationDefinition cd " +
           "WHERE e.id IN :employeeIds " +
           "ORDER BY e.department, e.lastName, e.firstName, cd.name")
    List<Certification> findByEmployeeIdInWithDetails(@Param("employeeIds") List<String> employeeIds);
    
    @Query("SELECT COUNT(c) FROM Certification c WHERE c.certificationDefinition.id = :certificationDefinitionId")
    Long countByCertificationDefinitionId(@Param("certificationDefinitionId") String certificationDefinitionId);
    
    @Query("SELECT COUNT(c) FROM Certification c WHERE c.employee.id = :employeeId")
    Long countByEmployeeId(@Param("employeeId") String employeeId);
      @Query("SELECT COUNT(c) FROM Certification c WHERE c.status = :status")
    Long countByStatus(@Param("status") Certification.CertificationStatus status);
    
    // Optimized single query for report data - now fetches all data including tasks
    @Query("SELECT DISTINCT c FROM Certification c " +
           "JOIN FETCH c.employee e " +
           "JOIN FETCH c.certificationDefinition cd " +
           "LEFT JOIN FETCH c.stages s " +
           "LEFT JOIN FETCH s.stageDefinition sd " +
           "LEFT JOIN FETCH s.tasks t " +
           "LEFT JOIN FETCH t.taskDefinition td " +
           "WHERE e.id IN :employeeIds " +
           "ORDER BY e.department, e.lastName, e.firstName, cd.name")
    List<Certification> findCompleteReportDataByEmployeeIds(@Param("employeeIds") List<String> employeeIds);
}
