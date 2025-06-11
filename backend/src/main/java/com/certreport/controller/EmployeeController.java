package com.certreport.controller;

import com.certreport.dto.EmployeeDto;
import com.certreport.service.EmployeeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {
      private static final Logger logger = LoggerFactory.getLogger(EmployeeController.class);
    private final EmployeeService employeeService;
    
    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }
    
    @GetMapping
    public ResponseEntity<List<EmployeeDto>> getAllEmployees(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String department) {
        
        logger.info("DevTools Test: Fetching all employees - search: {}, department: {}", search, department);
        
        List<EmployeeDto> employees;
        
        if (search != null && !search.trim().isEmpty()) {
            employees = employeeService.searchEmployees(search);
        } else if (department != null && !department.trim().isEmpty()) {
            employees = employeeService.getEmployeesByDepartment(department);
        } else {
            employees = employeeService.getAllEmployees();
        }
        
        return ResponseEntity.ok(employees);
    }
      @GetMapping("/{id}")
    public ResponseEntity<EmployeeDto> getEmployeeById(@PathVariable String id) {
        return employeeService.getEmployeeById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/departments")
    public ResponseEntity<List<String>> getAllDepartments() {
        return ResponseEntity.ok(employeeService.getAllDepartments());
    }
    
    // Additional endpoints for frontend compatibility
    @GetMapping("/search")
    public ResponseEntity<List<EmployeeDto>> searchEmployees(@RequestParam("q") String searchTerm) {
        logger.info("Searching employees with term: {}", searchTerm);
        List<EmployeeDto> employees = employeeService.searchEmployees(searchTerm);
        return ResponseEntity.ok(employees);
    }
    
    @GetMapping("/department/{department}")
    public ResponseEntity<List<EmployeeDto>> getEmployeesByDepartmentPath(@PathVariable String department) {
        logger.info("Fetching employees for department: {}", department);
        List<EmployeeDto> employees = employeeService.getEmployeesByDepartment(department);
        return ResponseEntity.ok(employees);
    }
}
