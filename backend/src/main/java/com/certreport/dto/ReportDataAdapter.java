package com.certreport.dto;

import java.time.LocalDateTime;

/**
 * Adapter class that provides nested field access for JasperReports template compatibility
 * Wraps ReportDataDto to match the existing template's field structure (employee.firstName, etc.)
 */
public class ReportDataAdapter {
    private final ReportDataDto data;
    private final EmployeeAdapter employee;
    
    public ReportDataAdapter(ReportDataDto data) {
        this.data = data;
        this.employee = new EmployeeAdapter(data);
    }
    
    // Employee nested object for template compatibility
    public EmployeeAdapter getEmployee() {
        return employee;
    }
    
    // Direct certification fields (flat structure)
    public String getCertificationId() {
        return data.getEmployeeId() + "_" + data.getCertificationName(); // Generate ID
    }
    
    public String getCertificationName() {
        return data.getCertificationName();
    }
    
    public String getCertificationCategory() {
        return data.getCertificationCategory();
    }
    
    public String getCertificationDescription() {
        return data.getCertificationName() + " Certification"; // Default description
    }
    
    public String getStatus() {
        return data.getStatus();
    }
    
    public Double getCompletionPercentage() {
        // Convert from "85.5%" string to Double
        String percentStr = data.getCompletionPercentage();
        if (percentStr != null && percentStr.endsWith("%")) {
            try {
                return Double.parseDouble(percentStr.substring(0, percentStr.length() - 1));
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        return 0.0;
    }
    
    public String getCurrentStageName() {
        return data.getCurrentStage();
    }
    
    public Integer getCurrentStageSequence() {
        return 1; // Default sequence
    }
    
    public LocalDateTime getEnrolledAt() {
        return parseDate(data.getEnrolledDate());
    }
    
    public LocalDateTime getCompletedAt() {
        return parseDate(data.getCompletedDate());
    }
    
    public LocalDateTime getDueDate() {
        return parseDate(data.getDueDate());
    }
    
    public Long getCompletedCertificationsCount() {
        return (long) data.getCompletedCount();
    }
    
    public Long getInProgressCertificationsCount() {
        return (long) data.getInProgressCount();
    }
    
    public Long getFailedCertificationsCount() {
        return (long) data.getFailedCount();
    }
    
    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateStr + "T00:00:00");
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Nested employee adapter to match template's employee.* field structure
     */
    public static class EmployeeAdapter {
        private final ReportDataDto data;
        
        public EmployeeAdapter(ReportDataDto data) {
            this.data = data;
        }
        
        public String getId() {
            return data.getEmployeeId();
        }
        
        public String getFirstName() {
            String fullName = data.getEmployeeName();
            if (fullName != null && fullName.contains(" ")) {
                return fullName.substring(0, fullName.lastIndexOf(" "));
            }
            return fullName;
        }
        
        public String getLastName() {
            String fullName = data.getEmployeeName();
            if (fullName != null && fullName.contains(" ")) {
                return fullName.substring(fullName.lastIndexOf(" ") + 1);
            }
            return "";
        }
        
        public String getEmail() {
            // Generate email from name if not available
            String firstName = getFirstName();
            String lastName = getLastName();
            if (firstName != null && lastName != null) {
                return firstName.toLowerCase() + "." + lastName.toLowerCase() + "@company.com";
            }
            return data.getEmployeeId() + "@company.com";
        }
        
        public String getDepartment() {
            return data.getDepartment();
        }
        
        public String getPosition() {
            return data.getPosition();
        }
        
        public LocalDateTime getHireDate() {
            // Default hire date since not available in lightweight DTO
            return LocalDateTime.now().minusYears(2);
        }
    }
}
