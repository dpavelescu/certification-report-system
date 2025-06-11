package com.certreport.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stage_definitions")
public class StageDefinition {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
      @Column(name = "sequence_order", nullable = false)
    private Integer sequenceOrder;
    
    @Column(name = "estimated_duration_hours")
    private Double estimatedDurationHours;
    
    @Column(name = "is_mandatory")
    private Boolean isMandatory = true;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "certification_definition_id", nullable = false)
    private CertificationDefinition certificationDefinition;
    
    @OneToMany(mappedBy = "stageDefinition", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("sequenceOrder ASC")
    private List<TaskDefinition> tasks = new ArrayList<>();
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public StageDefinition() {}
      public StageDefinition(String name, String description, Integer sequenceOrder, Double estimatedDurationHours, CertificationDefinition certificationDefinition) {
        this.name = name;
        this.description = description;
        this.sequenceOrder = sequenceOrder;
        this.estimatedDurationHours = estimatedDurationHours;
        this.certificationDefinition = certificationDefinition;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Integer getSequenceOrder() { return sequenceOrder; }    public void setSequenceOrder(Integer sequenceOrder) { this.sequenceOrder = sequenceOrder; }
    
    public Double getEstimatedDurationHours() { return estimatedDurationHours; }
    public void setEstimatedDurationHours(Double estimatedDurationHours) { this.estimatedDurationHours = estimatedDurationHours; }
    
    public Boolean getIsMandatory() { return isMandatory; }
    public void setIsMandatory(Boolean isMandatory) { this.isMandatory = isMandatory; }
    
    // Convenience method for backward compatibility
    public void setMandatory(Boolean mandatory) { this.isMandatory = mandatory; }
    
    public CertificationDefinition getCertificationDefinition() { return certificationDefinition; }
    public void setCertificationDefinition(CertificationDefinition certificationDefinition) { this.certificationDefinition = certificationDefinition; }
    
    public List<TaskDefinition> getTasks() { return tasks; }
    public void setTasks(List<TaskDefinition> tasks) { this.tasks = tasks; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
