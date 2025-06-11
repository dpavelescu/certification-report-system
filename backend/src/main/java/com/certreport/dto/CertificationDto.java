package com.certreport.dto;

import com.certreport.model.Certification;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

public class CertificationDto {    private String id;
    private EmployeeDto employee;
    private CertificationDefinitionDto certificationDefinition;
    private Certification.CertificationStatus status;
    private Double completionPercentage;
    private String currentStageId;
    private String currentStageName;
    private Integer currentStageSequence;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime enrolledAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime completedAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dueDate;
    
    private List<StageProgressDto> stageProgress;
    
    // Constructors
    public CertificationDto() {}
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public EmployeeDto getEmployee() {
        return employee;
    }
    
    public void setEmployee(EmployeeDto employee) {
        this.employee = employee;
    }
    
    public CertificationDefinitionDto getCertificationDefinition() {
        return certificationDefinition;
    }
    
    public void setCertificationDefinition(CertificationDefinitionDto certificationDefinition) {
        this.certificationDefinition = certificationDefinition;
    }
      public Certification.CertificationStatus getStatus() {
        return status;
    }
    
    public void setStatus(Certification.CertificationStatus status) {
        this.status = status;
    }
    
    public Double getCompletionPercentage() {
        return completionPercentage;
    }
    
    public void setCompletionPercentage(Double completionPercentage) {
        this.completionPercentage = completionPercentage;
    }
    
    public String getCurrentStageId() {
        return currentStageId;
    }
    
    public void setCurrentStageId(String currentStageId) {
        this.currentStageId = currentStageId;
    }
    
    public String getCurrentStageName() {
        return currentStageName;
    }
    
    public void setCurrentStageName(String currentStageName) {
        this.currentStageName = currentStageName;
    }
    
    public Integer getCurrentStageSequence() {
        return currentStageSequence;
    }
    
    public void setCurrentStageSequence(Integer currentStageSequence) {
        this.currentStageSequence = currentStageSequence;
    }
    
    public LocalDateTime getEnrolledAt() {
        return enrolledAt;
    }
    
    public void setEnrolledAt(LocalDateTime enrolledAt) {
        this.enrolledAt = enrolledAt;
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
    
    public List<StageProgressDto> getStageProgress() {
        return stageProgress;
    }
    
    public void setStageProgress(List<StageProgressDto> stageProgress) {
        this.stageProgress = stageProgress;
    }
}
