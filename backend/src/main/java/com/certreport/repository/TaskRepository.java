package com.certreport.repository;

import com.certreport.model.Task;
import com.certreport.model.TaskDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, String> {
    
    @Query("SELECT t FROM Task t WHERE t.stage.id = :stageId ORDER BY t.taskDefinition.sequenceOrder")
    List<Task> findByStageIdOrderBySequence(@Param("stageId") String stageId);
    
    @Query("SELECT t FROM Task t WHERE t.stage.id = :stageId AND t.taskDefinition.sequenceOrder = :sequenceOrder")
    Optional<Task> findByStageIdAndSequenceOrder(@Param("stageId") String stageId, 
                                                  @Param("sequenceOrder") Integer sequenceOrder);
    
    @Query("SELECT t FROM Task t WHERE t.stage.certification.employee.id = :employeeId ORDER BY t.stage.certification.enrolledAt DESC, t.taskDefinition.sequenceOrder")
    List<Task> findByEmployeeId(@Param("employeeId") String employeeId);
      @Query("SELECT t FROM Task t WHERE t.status = :status ORDER BY t.createdAt DESC")
    List<Task> findByStatus(@Param("status") Task.TaskStatus status);
    
    @Query("SELECT t FROM Task t WHERE t.status IN :statuses ORDER BY t.createdAt DESC")
    List<Task> findByStatusIn(@Param("statuses") List<Task.TaskStatus> statuses);
    
    @Query("SELECT t FROM Task t WHERE t.taskDefinition.taskType = :taskType ORDER BY t.createdAt DESC")
    List<Task> findByTaskType(@Param("taskType") TaskDefinition.TaskType taskType);
    
    @Query("SELECT t FROM Task t WHERE t.dueDate IS NOT NULL AND t.dueDate < :currentDate AND t.status NOT IN ('COMPLETED', 'FAILED', 'SKIPPED')")
    List<Task> findOverdue(@Param("currentDate") LocalDateTime currentDate);
    
    @Query("SELECT t FROM Task t WHERE t.dueDate IS NOT NULL AND t.dueDate BETWEEN :startDate AND :endDate AND t.status NOT IN ('COMPLETED', 'FAILED', 'SKIPPED')")
    List<Task> findDueSoon(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
      @Query("SELECT t FROM Task t WHERE t.taskDefinition.requiresSupervisor = true AND t.supervisorApproved = false AND t.status = 'COMPLETED'")
    List<Task> findPendingSupervisorApproval();
    
    @Query("SELECT t FROM Task t WHERE t.supervisorId = :supervisorId AND t.supervisorApproved = false AND t.status = 'COMPLETED'")
    List<Task> findPendingApprovalBySupervisor(@Param("supervisorId") String supervisorId);
    
    @Query("SELECT COUNT(t) FROM Task t WHERE t.stage.id = :stageId AND t.status = 'COMPLETED'")
    Long countCompletedTasksByStageId(@Param("stageId") String stageId);
    
    @Query("SELECT COUNT(t) FROM Task t WHERE t.stage.id = :stageId")
    Long countTotalTasksByStageId(@Param("stageId") String stageId);
    
    @Query("SELECT SUM(t.actualHours) FROM Task t WHERE t.stage.certification.id = :certificationId AND t.status = 'COMPLETED'")
    Double sumActualHoursByCertificationId(@Param("certificationId") String certificationId);
}
