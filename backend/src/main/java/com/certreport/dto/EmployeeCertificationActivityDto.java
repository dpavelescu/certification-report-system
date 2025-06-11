package com.certreport.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * DTO that represents an employee-certification combination for activity reporting
 * Each instance represents one certification activity for one employee
 */
public class EmployeeCertificationActivityDto {
    // Employee information
    private EmployeeDto employee;
      // Certification information
    private String certificationId;
    private String certificationName;
    private String certificationCategory;
    private String certificationDescription;
    private String status;
    private Double completionPercentage;
    private String currentStageName;
    private Integer currentStageSequence;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime enrolledAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime completedAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dueDate;
    
    // Summary counts for the employee (same for all certifications of this employee)
    private Long completedCertificationsCount;
    private Long inProgressCertificationsCount;
    private Long failedCertificationsCount;
    
    // Constructors
    public EmployeeCertificationActivityDto() {}
    
    public EmployeeCertificationActivityDto(EmployeeDto employee, CertificationDto certification,
                                           Long completedCount, Long inProgressCount, Long failedCount) {
        this.employee = employee;
        this.completedCertificationsCount = completedCount;
        this.inProgressCertificationsCount = inProgressCount;
        this.failedCertificationsCount = failedCount;
        
        if (certification != null) {
            this.certificationId = certification.getId();
            this.certificationName = certification.getCertificationDefinition() != null ? 
                    certification.getCertificationDefinition().getName() : "Unknown";
            this.certificationCategory = certification.getCertificationDefinition() != null ? 
                    certification.getCertificationDefinition().getCategory() : "Unknown";
            this.certificationDescription = certification.getCertificationDefinition() != null ? 
                    certification.getCertificationDefinition().getDescription() : "";
            this.status = certification.getStatus() != null ? certification.getStatus().toString() : "UNKNOWN";
            this.completionPercentage = certification.getCompletionPercentage();
            this.currentStageName = certification.getCurrentStageName();
            this.currentStageSequence = certification.getCurrentStageSequence();
            this.enrolledAt = certification.getEnrolledAt();
            this.completedAt = certification.getCompletedAt();
            this.dueDate = certification.getDueDate();
        }
    }
    
    // Getters and Setters
    public EmployeeDto getEmployee() { return employee; }
    public void setEmployee(EmployeeDto employee) { this.employee = employee; }
    
    public String getCertificationId() { return certificationId; }
    public void setCertificationId(String certificationId) { this.certificationId = certificationId; }
    
    public String getCertificationName() { return certificationName; }
    public void setCertificationName(String certificationName) { this.certificationName = certificationName; }
    
    public String getCertificationCategory() { return certificationCategory; }
    public void setCertificationCategory(String certificationCategory) { this.certificationCategory = certificationCategory; }
    
    public String getCertificationDescription() { return certificationDescription; }
    public void setCertificationDescription(String certificationDescription) { this.certificationDescription = certificationDescription; }
      public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public Double getCompletionPercentage() { return completionPercentage; }
    public void setCompletionPercentage(Double completionPercentage) { this.completionPercentage = completionPercentage; }
    
    public String getCurrentStageName() { return currentStageName; }
    public void setCurrentStageName(String currentStageName) { this.currentStageName = currentStageName; }
    
    public Integer getCurrentStageSequence() { return currentStageSequence; }
    public void setCurrentStageSequence(Integer currentStageSequence) { this.currentStageSequence = currentStageSequence; }
    
    public LocalDateTime getEnrolledAt() { return enrolledAt; }
    public void setEnrolledAt(LocalDateTime enrolledAt) { this.enrolledAt = enrolledAt; }
    
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    
    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
    
    public Long getCompletedCertificationsCount() { return completedCertificationsCount; }
    public void setCompletedCertificationsCount(Long completedCertificationsCount) { this.completedCertificationsCount = completedCertificationsCount; }
    
    public Long getInProgressCertificationsCount() { return inProgressCertificationsCount; }
    public void setInProgressCertificationsCount(Long inProgressCertificationsCount) { this.inProgressCertificationsCount = inProgressCertificationsCount; }
    
    public Long getFailedCertificationsCount() { return failedCertificationsCount; }
    public void setFailedCertificationsCount(Long failedCertificationsCount) { this.failedCertificationsCount = failedCertificationsCount; }
}
