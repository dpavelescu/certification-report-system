package com.certreport.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.LinkedHashSet;

@Entity
@Table(name = "certifications")
public class Certification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "certification_definition_id", nullable = false)
    private CertificationDefinition certificationDefinition;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CertificationStatus status = CertificationStatus.NOT_STARTED;
    
    @Column(name = "enrolled_at")
    private LocalDateTime enrolledAt;
    
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "due_date")
    private LocalDateTime dueDate;
    
    @Column(name = "current_stage_sequence")
    private Integer currentStageSequence = 1;
    
    @Column(name = "completion_percentage")
    private Double completionPercentage = 0.0;
    
    @Column(columnDefinition = "TEXT")
    private String notes;    @OneToMany(mappedBy = "certification", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Stage> stages = new LinkedHashSet<>();
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
      public enum CertificationStatus {
        NOT_STARTED,
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        OVERDUE,
        SUSPENDED,
        EXPIRED
    }
    
    // Constructors
    public Certification() {}
    
    public Certification(Employee employee, CertificationDefinition certificationDefinition, LocalDateTime enrolledAt) {        this.employee = employee;
        this.certificationDefinition = certificationDefinition;
        this.enrolledAt = enrolledAt;
        // Calculate due date based on certification definition validity period
        if (certificationDefinition.getValidityPeriodMonths() != null) {
            this.dueDate = enrolledAt.plusMonths(certificationDefinition.getValidityPeriodMonths());
        }
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }
    
    public CertificationDefinition getCertificationDefinition() { return certificationDefinition; }
    public void setCertificationDefinition(CertificationDefinition certificationDefinition) { this.certificationDefinition = certificationDefinition; }
    
    public CertificationStatus getStatus() { return status; }
    public void setStatus(CertificationStatus status) { this.status = status; }
    
    public LocalDateTime getEnrolledAt() { return enrolledAt; }
    public void setEnrolledAt(LocalDateTime enrolledAt) { this.enrolledAt = enrolledAt; }
    
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    
    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
    
    public Integer getCurrentStageSequence() { return currentStageSequence; }
    public void setCurrentStageSequence(Integer currentStageSequence) { this.currentStageSequence = currentStageSequence; }
    
    public Double getCompletionPercentage() { return completionPercentage; }
    public void setCompletionPercentage(Double completionPercentage) { this.completionPercentage = completionPercentage; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
      public Set<Stage> getStages() { return stages; }
    public void setStages(Set<Stage> stages) { this.stages = stages; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
