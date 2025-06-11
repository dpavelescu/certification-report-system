package com.certreport.repository;

import com.certreport.model.StageDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StageDefinitionRepository extends JpaRepository<StageDefinition, String> {
    
    @Query("SELECT sd FROM StageDefinition sd WHERE sd.certificationDefinition.id = :certificationDefinitionId ORDER BY sd.sequenceOrder")
    List<StageDefinition> findByCertificationDefinitionIdOrderBySequenceOrder(@Param("certificationDefinitionId") String certificationDefinitionId);
    
    @Query("SELECT sd FROM StageDefinition sd WHERE sd.certificationDefinition.id = :certificationDefinitionId AND sd.sequenceOrder = :sequenceOrder")
    Optional<StageDefinition> findByCertificationDefinitionIdAndSequenceOrder(@Param("certificationDefinitionId") String certificationDefinitionId, 
                                                                               @Param("sequenceOrder") Integer sequenceOrder);
    
    @Query("SELECT MAX(sd.sequenceOrder) FROM StageDefinition sd WHERE sd.certificationDefinition.id = :certificationDefinitionId")
    Integer findMaxSequenceOrderByCertificationDefinitionId(@Param("certificationDefinitionId") String certificationDefinitionId);
    
    @Query("SELECT COUNT(sd) FROM StageDefinition sd WHERE sd.certificationDefinition.id = :certificationDefinitionId")
    Long countByCertificationDefinitionId(@Param("certificationDefinitionId") String certificationDefinitionId);
}
