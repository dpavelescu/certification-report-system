package com.certreport.dto;

import com.certreport.model.Certification;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

public class CertificationFilterRequestDto {
      private List<String> employeeIds;
    private List<String> certificationDefinitionIds;
    private List<Certification.CertificationStatus> statuses;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endDate;
    
    private int page = 0;
    private int size = 30;
    private String sortBy = "enrolledAt";
    private String sortDirection = "DESC";
    
    // Constructors
    public CertificationFilterRequestDto() {}
    
    // Getters and Setters
    public List<String> getEmployeeIds() {
        return employeeIds;
    }
    
    public void setEmployeeIds(List<String> employeeIds) {
        this.employeeIds = employeeIds;
    }
    
    public List<String> getCertificationDefinitionIds() {
        return certificationDefinitionIds;
    }
    
    public void setCertificationDefinitionIds(List<String> certificationDefinitionIds) {
        this.certificationDefinitionIds = certificationDefinitionIds;
    }
      public List<Certification.CertificationStatus> getStatuses() {
        return statuses;
    }
    
    public void setStatuses(List<Certification.CertificationStatus> statuses) {
        this.statuses = statuses;
    }
    
    public LocalDateTime getStartDate() {
        return startDate;
    }
    
    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }
    
    public LocalDateTime getEndDate() {
        return endDate;
    }
    
    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }
    
    public int getPage() {
        return page;
    }
    
    public void setPage(int page) {
        this.page = page;
    }
    
    public int getSize() {
        return size;
    }
    
    public void setSize(int size) {
        this.size = size;
    }
    
    public String getSortBy() {
        return sortBy;
    }
    
    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }
    
    public String getSortDirection() {
        return sortDirection;
    }
    
    public void setSortDirection(String sortDirection) {
        this.sortDirection = sortDirection;
    }
}
