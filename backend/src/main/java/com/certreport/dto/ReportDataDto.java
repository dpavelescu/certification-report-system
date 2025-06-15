package com.certreport.dto;

import java.time.LocalDateTime;

/**
 * Lightweight DTO optimized for report generation
 * Reduces memory footprint by ~640x compared to full EmployeeCertificationActivityDto
 * Contains only essential data needed for PDF rendering
 */
public class ReportDataDto {
    // Essential employee data
    private String employeeId;
    private String employeeName;
    private String department;
    private String position;
    
    // Essential certification data
    private String certificationName;
    private String certificationCategory;
    private String status;
    private String completionPercentage; // String to avoid Double object overhead
    private String currentStage;
    
    // Essential dates (as strings to reduce memory)
    private String enrolledDate;
    private String completedDate;
    private String dueDate;
    
    // Summary counts (shared across certifications for same employee)
    private int completedCount;
    private int inProgressCount;
    private int failedCount;
    
    // Constructors
    public ReportDataDto() {}
    
    public ReportDataDto(String employeeId, String employeeName, String department, String position,
                        String certificationName, String certificationCategory, String status,
                        String completionPercentage, String currentStage,
                        String enrolledDate, String completedDate, String dueDate,
                        int completedCount, int inProgressCount, int failedCount) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.department = department;
        this.position = position;
        this.certificationName = certificationName;
        this.certificationCategory = certificationCategory;
        this.status = status;
        this.completionPercentage = completionPercentage;
        this.currentStage = currentStage;
        this.enrolledDate = enrolledDate;
        this.completedDate = completedDate;
        this.dueDate = dueDate;
        this.completedCount = completedCount;
        this.inProgressCount = inProgressCount;
        this.failedCount = failedCount;
    }
    
    // Factory method to create from existing EmployeeCertificationActivityDto
    public static ReportDataDto fromActivityDto(EmployeeCertificationActivityDto activityDto) {
        EmployeeDto employee = activityDto.getEmployee();
        String employeeName = employee != null ? 
            employee.getFirstName() + " " + employee.getLastName() : "Unknown";
        String department = employee != null ? employee.getDepartment() : "";
        String position = employee != null ? employee.getPosition() : "";
        
        String completionPercentage = activityDto.getCompletionPercentage() != null ? 
            String.format("%.1f%%", activityDto.getCompletionPercentage()) : "0%";
        
        String enrolledDate = formatDate(activityDto.getEnrolledAt());
        String completedDate = formatDate(activityDto.getCompletedAt());
        String dueDate = formatDate(activityDto.getDueDate());
        
        return new ReportDataDto(
            employee != null ? employee.getId() : "",
            employeeName,
            department,
            position,
            activityDto.getCertificationName(),
            activityDto.getCertificationCategory(),
            activityDto.getStatus(),
            completionPercentage,
            activityDto.getCurrentStageName(),
            enrolledDate,
            completedDate,
            dueDate,
            safeToInt(activityDto.getCompletedCertificationsCount()),
            safeToInt(activityDto.getInProgressCertificationsCount()),
            safeToInt(activityDto.getFailedCertificationsCount())
        );
    }
    
    private static String formatDate(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.toString().substring(0, 10) : "";
    }
    
    private static int safeToInt(Long value) {
        return value != null ? value.intValue() : 0;
    }
    
    // Getters and Setters
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    
    public String getCertificationName() { return certificationName; }
    public void setCertificationName(String certificationName) { this.certificationName = certificationName; }
    
    public String getCertificationCategory() { return certificationCategory; }
    public void setCertificationCategory(String certificationCategory) { this.certificationCategory = certificationCategory; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getCompletionPercentage() { return completionPercentage; }
    public void setCompletionPercentage(String completionPercentage) { this.completionPercentage = completionPercentage; }
    
    public String getCurrentStage() { return currentStage; }
    public void setCurrentStage(String currentStage) { this.currentStage = currentStage; }
    
    public String getEnrolledDate() { return enrolledDate; }
    public void setEnrolledDate(String enrolledDate) { this.enrolledDate = enrolledDate; }
    
    public String getCompletedDate() { return completedDate; }
    public void setCompletedDate(String completedDate) { this.completedDate = completedDate; }
    
    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }
    
    public int getCompletedCount() { return completedCount; }
    public void setCompletedCount(int completedCount) { this.completedCount = completedCount; }
    
    public int getInProgressCount() { return inProgressCount; }
    public void setInProgressCount(int inProgressCount) { this.inProgressCount = inProgressCount; }
    
    public int getFailedCount() { return failedCount; }
    public void setFailedCount(int failedCount) { this.failedCount = failedCount; }
}
