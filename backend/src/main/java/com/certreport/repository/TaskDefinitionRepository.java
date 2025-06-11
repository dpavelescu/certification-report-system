package com.certreport.repository;

import com.certreport.model.TaskDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskDefinitionRepository extends JpaRepository<TaskDefinition, String> {
    
    @Query("SELECT td FROM TaskDefinition td WHERE td.stageDefinition.id = :stageDefinitionId ORDER BY td.sequenceOrder")
    List<TaskDefinition> findByStageDefinitionIdOrderBySequenceOrder(@Param("stageDefinitionId") String stageDefinitionId);
    
    @Query("SELECT td FROM TaskDefinition td WHERE td.stageDefinition.id = :stageDefinitionId AND td.sequenceOrder = :sequenceOrder")
    Optional<TaskDefinition> findByStageDefinitionIdAndSequenceOrder(@Param("stageDefinitionId") String stageDefinitionId, 
                                                                      @Param("sequenceOrder") Integer sequenceOrder);
      @Query("SELECT td FROM TaskDefinition td WHERE td.taskType = :taskType")
    List<TaskDefinition> findByTaskType(@Param("taskType") TaskDefinition.TaskType taskType);
    
    @Query("SELECT td FROM TaskDefinition td WHERE td.stageDefinition.id = :stageDefinitionId AND td.isMandatory = true ORDER BY td.sequenceOrder")
    List<TaskDefinition> findMandatoryByStageDefinitionId(@Param("stageDefinitionId") String stageDefinitionId);
      @Query("SELECT td FROM TaskDefinition td WHERE td.requiresSupervisor = true")
    List<TaskDefinition> findRequiringSupervisorApproval();
    
    @Query("SELECT MAX(td.sequenceOrder) FROM TaskDefinition td WHERE td.stageDefinition.id = :stageDefinitionId")
    Integer findMaxSequenceOrderByStageDefinitionId(@Param("stageDefinitionId") String stageDefinitionId);
    
    @Query("SELECT COUNT(td) FROM TaskDefinition td WHERE td.stageDefinition.id = :stageDefinitionId")
    Long countByStageDefinitionId(@Param("stageDefinitionId") String stageDefinitionId);
}
