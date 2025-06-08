package com.certreport.service;

import com.certreport.dto.EmployeeDto;
import com.certreport.model.Employee;
import com.certreport.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EmployeeService {
    
    private final EmployeeRepository employeeRepository;
    
    @Autowired
    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }
    
    public List<EmployeeDto> getAllEmployees() {
        return employeeRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public Optional<EmployeeDto> getEmployeeById(String id) {
        return employeeRepository.findById(id)
                .map(this::convertToDto);
    }
    
    public List<EmployeeDto> searchEmployees(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getAllEmployees();
        }
        
        return employeeRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(searchTerm)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<EmployeeDto> getEmployeesByDepartment(String department) {
        return employeeRepository.findByDepartment(department).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<String> getAllDepartments() {
        return employeeRepository.findDistinctDepartments();
    }
    
    public List<EmployeeDto> getEmployeesByIds(List<String> employeeIds) {
        return employeeRepository.findAllById(employeeIds).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
      private EmployeeDto convertToDto(Employee employee) {
        return new EmployeeDto(
                employee.getId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getEmail(),
                employee.getDepartment(),
                employee.getPosition(),
                employee.getHireDate()
        );
    }
}
