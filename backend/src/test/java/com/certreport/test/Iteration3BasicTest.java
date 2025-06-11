package com.certreport.test;

import com.certreport.dto.*;
import com.certreport.model.*;
import com.certreport.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple test to verify Iteration 3 basic functionality
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class Iteration3BasicTest {

    @Autowired
    private ReportService reportService;
    
    @Autowired
    private EmployeeService employeeService;
      @Autowired
    private CertificationService certificationService;
    
    @Test
    void testBasicReportCreation() {
        // Given: Request for certification report
        ReportRequestDto request = new ReportRequestDto();
        request.setReportType("CERTIFICATION");
        request.setEmployeeIds(List.of("EMP001"));

        // When: Generate report
        Report report = reportService.generateReport(request);

        // Then: Verify report creation
        assertNotNull(report);
        assertNotNull(report.getId());
        assertEquals("CERTIFICATION", report.getType());
        assertEquals(Report.ReportStatus.COMPLETED, report.getStatus());
    }

    @Test
    void testEmployeeDataRetrieval() {
        // When: Get all employees
        List<EmployeeDto> employees = employeeService.getAllEmployees();

        // Then: Verify employees exist
        assertFalse(employees.isEmpty(), "Test data should contain employees");
        
        EmployeeDto firstEmployee = employees.get(0);
        assertNotNull(firstEmployee.getId());
        assertNotNull(firstEmployee.getFirstName());
        assertNotNull(firstEmployee.getLastName());
        assertNotNull(firstEmployee.getDepartment());
    }

    @Test
    void testCertificationDataRetrieval() {
        // Given: First employee
        List<EmployeeDto> employees = employeeService.getAllEmployees();
        assertFalse(employees.isEmpty());
        String employeeId = employees.get(0).getId();

        // When: Get certifications
        List<CertificationDto> certifications = certificationService.getCertificationsByEmployeeId(employeeId);

        // Then: Verify structure (may be empty, that's ok)
        assertNotNull(certifications);
        
        for (CertificationDto cert : certifications) {
            assertNotNull(cert.getId());
            assertNotNull(cert.getStatus());
            if (cert.getCertificationDefinition() != null) {
                assertNotNull(cert.getCertificationDefinition().getName());
            }
        }
    }
}
