package com.certreport.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class ReportRequestDto {
    @JsonProperty("reportType")
    private String reportType;
      @JsonProperty("employeeIds")
    private List<String> employeeIds;
    
    // Constructors
    public ReportRequestDto() {}
    
    public ReportRequestDto(String reportType, List<String> employeeIds) {
        this.reportType = reportType;
        this.employeeIds = employeeIds;
    }
      // Getters and Setters
    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }
      public List<String> getEmployeeIds() { return employeeIds; }
    public void setEmployeeIds(List<String> employeeIds) { this.employeeIds = employeeIds; }
    
    @Override
    public String toString() {
        return "ReportRequestDto{" +
                "reportType='" + reportType + '\'' +
                ", employeeIds=" + employeeIds +
                '}';
    }
}
