package com.certreport.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "certification_definitions")
public class CertificationDefinition {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    private String name;
      @Column(columnDefinition = "TEXT")
    private String description;
    
    private String category;
    
    @Column(name = "total_duration_hours")
    private Double totalDurationHours;
    
    @Column(name = "validity_period_months")
    private Integer validityPeriodMonths;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @OneToMany(mappedBy = "certificationDefinition", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("sequenceOrder ASC")
    private List<StageDefinition> stages = new ArrayList<>();
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public CertificationDefinition() {}
      public CertificationDefinition(String name, String description, String category, Double totalDurationHours, Integer validityPeriodMonths) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.totalDurationHours = totalDurationHours;
        this.validityPeriodMonths = validityPeriodMonths;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
      public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public Double getTotalDurationHours() { return totalDurationHours; }
    public void setTotalDurationHours(Double totalDurationHours) { this.totalDurationHours = totalDurationHours; }
    
    public Integer getValidityPeriodMonths() { return validityPeriodMonths; }
    public void setValidityPeriodMonths(Integer validityPeriodMonths) { this.validityPeriodMonths = validityPeriodMonths; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    // Convenience method for backward compatibility
    public void setActive(Boolean active) { this.isActive = active; }
    
    public List<StageDefinition> getStages() { return stages; }
    public void setStages(List<StageDefinition> stages) { this.stages = stages; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
