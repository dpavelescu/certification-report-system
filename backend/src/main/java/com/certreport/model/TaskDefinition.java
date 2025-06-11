package com.certreport.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "task_definitions")
public class TaskDefinition {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false)
    private TaskType taskType;
    
    @Column(name = "sequence_order", nullable = false)
    private Integer sequenceOrder;
      @Column(name = "estimated_hours")
    private Double estimatedHours;
    
    @Column(name = "is_mandatory")
    private Boolean isMandatory = true;
    
    @Column(name = "requires_supervisor")
    private Boolean requiresSupervisor = false;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_definition_id", nullable = false)
    private StageDefinition stageDefinition;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
      public enum TaskType {
        ELEARNING,
        CLASSROOM_TRAINING,
        PRACTICAL_EXERCISE,
        SUPERVISED_TASK,
        ASSESSMENT,
        DOCUMENTATION_REVIEW,
        FIELD_WORK,
        PRESENTATION,
        EXAM,
        WORKSHOP,
        CASE_STUDY,
        PROJECT,
        REVIEW
    }
    
    // Constructors
    public TaskDefinition() {}
      public TaskDefinition(String name, String description, TaskType taskType, Integer sequenceOrder, 
                         Double estimatedHours, StageDefinition stageDefinition) {
        this.name = name;
        this.description = description;
        this.taskType = taskType;
        this.sequenceOrder = sequenceOrder;
        this.estimatedHours = estimatedHours;
        this.stageDefinition = stageDefinition;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public TaskType getTaskType() { return taskType; }
    public void setTaskType(TaskType taskType) { this.taskType = taskType; }
      public Integer getSequenceOrder() { return sequenceOrder; }
    public void setSequenceOrder(Integer sequenceOrder) { this.sequenceOrder = sequenceOrder; }
    
    public Double getEstimatedHours() { return estimatedHours; }
    public void setEstimatedHours(Double estimatedHours) { this.estimatedHours = estimatedHours; }
    
    public Boolean getIsMandatory() { return isMandatory; }
    public void setIsMandatory(Boolean isMandatory) { this.isMandatory = isMandatory; }
    
    public Boolean getRequiresSupervisor() { return requiresSupervisor; }
    public void setRequiresSupervisor(Boolean requiresSupervisor) { this.requiresSupervisor = requiresSupervisor; }
    
    // Convenience methods for backward compatibility
    public Boolean getRequiresSupervisorApproval() { return requiresSupervisor; }
    public void setRequiresSupervisorApproval(Boolean requiresSupervisorApproval) { this.requiresSupervisor = requiresSupervisorApproval; }
    
    public void setMandatory(Boolean mandatory) { this.isMandatory = mandatory; }
    
    public StageDefinition getStageDefinition() { return stageDefinition; }
    public void setStageDefinition(StageDefinition stageDefinition) { this.stageDefinition = stageDefinition; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
