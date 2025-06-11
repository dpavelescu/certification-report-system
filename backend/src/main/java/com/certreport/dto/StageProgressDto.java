package com.certreport.dto;

import com.certreport.model.Stage;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public class StageProgressDto {
    private String id;
    private String stageDefinitionId;
    private String name;    private String description;
    private Integer sequenceOrder;
    private Stage.StageStatus status;
    private Double completionPercentage;
    private Double estimatedDurationHours;
    private Boolean isMandatory;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startedAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime completedAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dueDate;
    
    private Long completedTasks;
    private Long totalTasks;
    
    // Constructors
    public StageProgressDto() {}
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getStageDefinitionId() {
        return stageDefinitionId;
    }
    
    public void setStageDefinitionId(String stageDefinitionId) {
        this.stageDefinitionId = stageDefinitionId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Integer getSequenceOrder() {
        return sequenceOrder;
    }
    
    public void setSequenceOrder(Integer sequenceOrder) {
        this.sequenceOrder = sequenceOrder;
    }
      public Stage.StageStatus getStatus() {
        return status;
    }
    
    public void setStatus(Stage.StageStatus status) {
        this.status = status;
    }
    
    public Double getCompletionPercentage() {
        return completionPercentage;
    }
    
    public void setCompletionPercentage(Double completionPercentage) {
        this.completionPercentage = completionPercentage;
    }
    
    public Double getEstimatedDurationHours() {
        return estimatedDurationHours;
    }
    
    public void setEstimatedDurationHours(Double estimatedDurationHours) {
        this.estimatedDurationHours = estimatedDurationHours;
    }
    
    public Boolean getIsMandatory() {
        return isMandatory;
    }
    
    public void setIsMandatory(Boolean isMandatory) {
        this.isMandatory = isMandatory;
    }
    
    public LocalDateTime getStartedAt() {
        return startedAt;
    }
    
    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }
    
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
    
    public LocalDateTime getDueDate() {
        return dueDate;
    }
    
    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }
    
    public Long getCompletedTasks() {
        return completedTasks;
    }
    
    public void setCompletedTasks(Long completedTasks) {
        this.completedTasks = completedTasks;
    }
    
    public Long getTotalTasks() {
        return totalTasks;
    }
    
    public void setTotalTasks(Long totalTasks) {
        this.totalTasks = totalTasks;
    }
}
