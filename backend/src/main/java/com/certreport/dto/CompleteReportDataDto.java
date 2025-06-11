package com.certreport.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO that combines employee data with their certification information for comprehensive reporting
 */
public class CompleteReportDataDto {
    private EmployeeDto employee;
    private List<CertificationDto> certifications;
    private LocalDateTime reportGeneratedAt;
    
    // Constructors
    public CompleteReportDataDto() {}
    
    public CompleteReportDataDto(EmployeeDto employee, List<CertificationDto> certifications) {
        this.employee = employee;
        this.certifications = certifications;
        this.reportGeneratedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public EmployeeDto getEmployee() { return employee; }
    public void setEmployee(EmployeeDto employee) { this.employee = employee; }
    
    public List<CertificationDto> getCertifications() { return certifications; }
    public void setCertifications(List<CertificationDto> certifications) { this.certifications = certifications; }
    
    public LocalDateTime getReportGeneratedAt() { return reportGeneratedAt; }
    public void setReportGeneratedAt(LocalDateTime reportGeneratedAt) { this.reportGeneratedAt = reportGeneratedAt; }
    
    // Convenience methods for report generation
    public String getEmployeeFullName() {
        if (employee == null) return "";
        return employee.getFirstName() + " " + employee.getLastName();
    }
    
    public long getCompletedCertificationsCount() {
        if (certifications == null) return 0;
        return certifications.stream()
                .filter(cert -> "COMPLETED".equals(cert.getStatus().toString()))
                .count();
    }
    
    public long getInProgressCertificationsCount() {
        if (certifications == null) return 0;
        return certifications.stream()
                .filter(cert -> "IN_PROGRESS".equals(cert.getStatus().toString()))
                .count();    }
    
    public long getFailedCertificationsCount() {
        if (certifications == null) return 0;
        return certifications.stream()
                .filter(cert -> "FAILED".equals(cert.getStatus().toString()))
                .count();
    }
    
    public boolean hasCertifications() {
        return certifications != null && !certifications.isEmpty();
    }
}
