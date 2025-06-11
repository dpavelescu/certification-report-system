package com.certreport.repository;

import com.certreport.model.CertificationDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CertificationDefinitionRepository extends JpaRepository<CertificationDefinition, String> {
    
    @Query("SELECT cd FROM CertificationDefinition cd WHERE cd.isActive = true ORDER BY cd.name")
    List<CertificationDefinition> findAllActive();
    
    @Query("SELECT cd FROM CertificationDefinition cd WHERE LOWER(cd.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(cd.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<CertificationDefinition> findByNameOrDescriptionContainingIgnoreCase(@Param("searchTerm") String searchTerm);
    
    @Query("SELECT cd FROM CertificationDefinition cd WHERE cd.category = :category AND cd.isActive = true")
    List<CertificationDefinition> findByCategoryAndActive(@Param("category") String category);
    
    @Query("SELECT DISTINCT cd.category FROM CertificationDefinition cd WHERE cd.isActive = true ORDER BY cd.category")
    List<String> findDistinctCategories();
    
    Optional<CertificationDefinition> findByNameIgnoreCase(String name);
    
    @Query("SELECT COUNT(c) FROM Certification c WHERE c.certificationDefinition.id = :certificationDefinitionId")
    Long countEnrollmentsByCertificationDefinitionId(@Param("certificationDefinitionId") String certificationDefinitionId);
}
