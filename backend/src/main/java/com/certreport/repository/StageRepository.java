package com.certreport.repository;

import com.certreport.model.Stage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StageRepository extends JpaRepository<Stage, String> {
    
    @Query("SELECT s FROM Stage s WHERE s.certification.id = :certificationId ORDER BY s.stageDefinition.sequenceOrder")
    List<Stage> findByCertificationIdOrderBySequence(@Param("certificationId") String certificationId);
    
    @Query("SELECT s FROM Stage s WHERE s.certification.id = :certificationId AND s.stageDefinition.sequenceOrder = :sequenceOrder")
    Optional<Stage> findByCertificationIdAndSequenceOrder(@Param("certificationId") String certificationId, 
                                                           @Param("sequenceOrder") Integer sequenceOrder);
    
    @Query("SELECT s FROM Stage s WHERE s.certification.employee.id = :employeeId ORDER BY s.certification.enrolledAt DESC, s.stageDefinition.sequenceOrder")
    List<Stage> findByEmployeeId(@Param("employeeId") String employeeId);
      @Query("SELECT s FROM Stage s WHERE s.status = :status ORDER BY s.createdAt DESC")
    List<Stage> findByStatus(@Param("status") Stage.StageStatus status);
    
    @Query("SELECT s FROM Stage s WHERE s.status IN :statuses ORDER BY s.createdAt DESC")
    List<Stage> findByStatusIn(@Param("statuses") List<Stage.StageStatus> statuses);
    
    @Query("SELECT s FROM Stage s WHERE s.dueDate IS NOT NULL AND s.dueDate < :currentDate AND s.status NOT IN ('COMPLETED', 'FAILED')")
    List<Stage> findOverdue(@Param("currentDate") LocalDateTime currentDate);
    
    @Query("SELECT s FROM Stage s WHERE s.dueDate IS NOT NULL AND s.dueDate BETWEEN :startDate AND :endDate AND s.status NOT IN ('COMPLETED', 'FAILED')")
    List<Stage> findDueSoon(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT s FROM Stage s WHERE s.certification.id = :certificationId AND s.status = 'IN_PROGRESS'")
    Optional<Stage> findCurrentStageByCertificationId(@Param("certificationId") String certificationId);
    
    @Query("SELECT COUNT(s) FROM Stage s WHERE s.certification.id = :certificationId AND s.status = 'COMPLETED'")
    Long countCompletedStagesByCertificationId(@Param("certificationId") String certificationId);
    
    @Query("SELECT COUNT(s) FROM Stage s WHERE s.certification.id = :certificationId")
    Long countTotalStagesByCertificationId(@Param("certificationId") String certificationId);
    
    // Efficient batch queries for reporting
    @Query("SELECT s FROM Stage s " +
           "JOIN FETCH s.stageDefinition sd " +
           "WHERE s.certification.id IN :certificationIds " +
           "ORDER BY s.certification.id, sd.sequenceOrder")
    List<Stage> findByCertificationIdsWithDetails(@Param("certificationIds") List<String> certificationIds);
}
