package com.certreport.dto;

import java.time.LocalDateTime;

public class EmployeeDto {
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String department;
    private String position;
    private LocalDateTime hireDate;
    
    // Constructors
    public EmployeeDto() {}
    
    public EmployeeDto(String id, String firstName, String lastName, String email, String department, String position, LocalDateTime hireDate) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.department = department;
        this.position = position;
        this.hireDate = hireDate;
    }
      // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
      public LocalDateTime getHireDate() { return hireDate; }
    public void setHireDate(LocalDateTime hireDate) { this.hireDate = hireDate; }
}
