package com.certreport.dto;

public class CertificationDefinitionDto {
    private String id;
    private String name;
    private String description;
    private String category;
    private Double totalDurationHours;
    private Integer validityPeriodMonths;
    private Boolean isActive;
    private Long enrollmentCount;
    
    // Constructors
    public CertificationDefinitionDto() {}
    
    public CertificationDefinitionDto(String id, String name, String description, String category, 
                                     Double totalDurationHours, Integer validityPeriodMonths, Boolean isActive) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.totalDurationHours = totalDurationHours;
        this.validityPeriodMonths = validityPeriodMonths;
        this.isActive = isActive;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
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
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public Double getTotalDurationHours() {
        return totalDurationHours;
    }
    
    public void setTotalDurationHours(Double totalDurationHours) {
        this.totalDurationHours = totalDurationHours;
    }
    
    public Integer getValidityPeriodMonths() {
        return validityPeriodMonths;
    }
    
    public void setValidityPeriodMonths(Integer validityPeriodMonths) {
        this.validityPeriodMonths = validityPeriodMonths;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public Long getEnrollmentCount() {
        return enrollmentCount;
    }
    
    public void setEnrollmentCount(Long enrollmentCount) {
        this.enrollmentCount = enrollmentCount;
    }
}
