package com.certreport.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.LinkedHashSet;

@Entity
@Table(name = "stages")
public class Stage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "certification_id", nullable = false)
    private Certification certification;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_definition_id", nullable = false)
    private StageDefinition stageDefinition;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StageStatus status = StageStatus.NOT_STARTED;
    
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "due_date")
    private LocalDateTime dueDate;
    
    @Column(name = "completion_percentage")
    private Double completionPercentage = 0.0;
    
    @Column(columnDefinition = "TEXT")
    private String notes;    @OneToMany(mappedBy = "stage", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Task> tasks = new LinkedHashSet<>();
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
      public enum StageStatus {
        NOT_STARTED,
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        SKIPPED,
        OVERDUE,
        SUSPENDED
    }
    
    // Constructors
    public Stage() {}
      public Stage(Certification certification, StageDefinition stageDefinition) {
        this.certification = certification;
        this.stageDefinition = stageDefinition;
        // Calculate due date based on stage definition duration (estimate 8 hours per day)
        if (stageDefinition.getEstimatedDurationHours() != null && certification.getEnrolledAt() != null) {
            long estimatedDays = Math.round(stageDefinition.getEstimatedDurationHours() / 8.0);
            this.dueDate = certification.getEnrolledAt().plusDays(estimatedDays);
        }
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public Certification getCertification() { return certification; }
    public void setCertification(Certification certification) { this.certification = certification; }
    
    public StageDefinition getStageDefinition() { return stageDefinition; }
    public void setStageDefinition(StageDefinition stageDefinition) { this.stageDefinition = stageDefinition; }
    
    public StageStatus getStatus() { return status; }
    public void setStatus(StageStatus status) { this.status = status; }
    
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    
    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
    
    public Double getCompletionPercentage() { return completionPercentage; }
    public void setCompletionPercentage(Double completionPercentage) { this.completionPercentage = completionPercentage; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
      public Set<Task> getTasks() { return tasks; }
    public void setTasks(Set<Task> tasks) { this.tasks = tasks; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
