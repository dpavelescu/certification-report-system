package com.certreport.dto;

import java.util.List;

public class CertificationFilterResponseDto {
    private List<CertificationDto> certifications;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int pageSize;
    private String sortBy;
    private String sortDirection;
    
    // Constructors
    public CertificationFilterResponseDto() {}
    
    public CertificationFilterResponseDto(List<CertificationDto> certifications, long totalElements, 
                                         int totalPages, int currentPage, int pageSize, 
                                         String sortBy, String sortDirection) {
        this.certifications = certifications;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.sortBy = sortBy;
        this.sortDirection = sortDirection;
    }
    
    // Getters and Setters
    public List<CertificationDto> getCertifications() {
        return certifications;
    }
    
    public void setCertifications(List<CertificationDto> certifications) {
        this.certifications = certifications;
    }
    
    public long getTotalElements() {
        return totalElements;
    }
    
    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }
    
    public int getTotalPages() {
        return totalPages;
    }
    
    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
    
    public int getCurrentPage() {
        return currentPage;
    }
    
    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }
    
    public int getPageSize() {
        return pageSize;
    }
    
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
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
