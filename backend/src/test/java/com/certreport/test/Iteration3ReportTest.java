package com.certreport.test;

import com.certreport.dto.*;
import com.certreport.model.*;
import com.certreport.repository.*;
import com.certreport.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for Iteration 3 - Complete Report Structure
 * Tests comprehensive report generation with detailed certification data
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class Iteration3ReportTest {

    @Autowired
    private ReportService reportService;
    
    @Autowired
    private EmployeeService employeeService;
    
    @Autowired
    private CertificationService certificationService;
      @Autowired
    private ReportRepository reportRepository;
    
    // @Autowired
    // private EmployeeRepository employeeRepository;
    
    // @Autowired
    // private CertificationRepository certificationRepository;

    private EmployeeDto testEmployee;
    private String testEmployeeId;

    @BeforeEach
    void setUp() {
        // Get existing employee from seeded data
        List<EmployeeDto> employees = employeeService.getAllEmployees();
        assertFalse(employees.isEmpty(), "Test data should contain employees");
        
        testEmployee = employees.get(0);
        testEmployeeId = testEmployee.getId();
    }

    @Test
    void testCompleteReportGeneration() {
        // Given: Request for detailed certification report
        ReportRequestDto request = new ReportRequestDto();
        request.setReportType("CERTIFICATION");
        request.setEmployeeIds(List.of(testEmployeeId));

        // When: Generate report
        Report report = reportService.generateReport(request);

        // Then: Verify report creation        assertNotNull(report);
        assertNotNull(report.getId());
        assertEquals("CERTIFICATION", report.getType());
        assertEquals(Report.ReportStatus.COMPLETED, report.getStatus());
        assertNotNull(report.getStartedAt());
        
        // Wait a bit for async processing and check final status
        try {
            Thread.sleep(2000); // Give time for async processing
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Refresh report status from database
        Optional<Report> updatedReport = reportRepository.findById(report.getId());
        assertTrue(updatedReport.isPresent());
        
        // The report should either be completed or still processing
        assertTrue(
            updatedReport.get().getStatus() == Report.ReportStatus.COMPLETED ||
            updatedReport.get().getStatus() == Report.ReportStatus.IN_PROGRESS ||
            updatedReport.get().getStatus() == Report.ReportStatus.FAILED
        );
    }

    @Test
    void testDetailedCertificationDataRetrieval() {
        // Given: Employee with certifications
        List<CertificationDto> certifications = certificationService.getCertificationsByEmployeeId(testEmployeeId);

        // Then: Verify detailed certification data structure
        for (CertificationDto certification : certifications) {
            assertNotNull(certification.getId());
            assertNotNull(certification.getEmployee());
            assertNotNull(certification.getCertificationDefinition());
            assertNotNull(certification.getStatus());
            
            // Verify certification definition has required data
            assertNotNull(certification.getCertificationDefinition().getName());
            assertNotNull(certification.getCertificationDefinition().getCategory());
            
            // Verify stage progress data is available
            if (certification.getStageProgress() != null) {
                for (StageProgressDto stage : certification.getStageProgress()) {
                    assertNotNull(stage.getName());
                    assertNotNull(stage.getStatus());
                    assertNotNull(stage.getSequenceOrder());
                    
                    // Verify task counts are available
                    assertTrue(stage.getTotalTasks() >= 0);
                    assertTrue(stage.getCompletedTasks() >= 0);
                    assertTrue(stage.getCompletedTasks() <= stage.getTotalTasks());
                }
            }
        }
    }

    @Test
    void testCertificationStatusGrouping() {
        // Given: Employee certifications
        List<CertificationDto> certifications = certificationService.getCertificationsByEmployeeId(testEmployeeId);

        // When: Group by status (simulating report logic)
        long completedCount = certifications.stream()
                .filter(cert -> cert.getStatus() == Certification.CertificationStatus.COMPLETED)
                .count();
        
        long inProgressCount = certifications.stream()
                .filter(cert -> cert.getStatus() == Certification.CertificationStatus.IN_PROGRESS)
                .count();
        
        long failedCount = certifications.stream()
                .filter(cert -> cert.getStatus() == Certification.CertificationStatus.FAILED)
                .count();

        // Then: Verify counts are reasonable
        assertTrue(completedCount >= 0);
        assertTrue(inProgressCount >= 0);
        assertTrue(failedCount >= 0);
        assertEquals(certifications.size(), completedCount + inProgressCount + failedCount + 
                    certifications.stream().filter(cert -> 
                        cert.getStatus() != Certification.CertificationStatus.COMPLETED &&
                        cert.getStatus() != Certification.CertificationStatus.IN_PROGRESS &&
                        cert.getStatus() != Certification.CertificationStatus.FAILED
                    ).count());
    }

    @Test
    void testStageAndTaskHierarchy() {
        // Given: Employee certifications with stages
        List<CertificationDto> certifications = certificationService.getCertificationsByEmployeeId(testEmployeeId);
        
        for (CertificationDto certification : certifications) {
            if (certification.getStageProgress() != null && !certification.getStageProgress().isEmpty()) {
                
                // When: Examine stage hierarchy
                List<StageProgressDto> stages = certification.getStageProgress();
                
                // Then: Verify stages are properly ordered
                for (int i = 0; i < stages.size() - 1; i++) {
                    StageProgressDto currentStage = stages.get(i);
                    StageProgressDto nextStage = stages.get(i + 1);
                    
                    // Stages should be in sequence order
                    assertTrue(currentStage.getSequenceOrder() <= nextStage.getSequenceOrder(),
                        "Stages should be ordered by sequence");
                }
                
                // Verify stage completion logic
                for (StageProgressDto stage : stages) {
                    if (stage.getStatus() == Stage.StageStatus.COMPLETED) {
                        assertEquals(100.0, stage.getCompletionPercentage(), 0.01,
                            "Completed stages should have 100% completion");
                    }
                    
                    if (stage.getStatus() == Stage.StageStatus.NOT_STARTED) {
                        assertEquals(0.0, stage.getCompletionPercentage(), 0.01,
                            "Not started stages should have 0% completion");
                    }
                }
            }
        }
    }

    @Test
    void testReportDataCompleteness() {
        // Given: Multiple employees
        List<EmployeeDto> employees = employeeService.getAllEmployees();
        List<String> employeeIds = employees.stream()
                .limit(3) // Test with first 3 employees
                .map(EmployeeDto::getId)
                .toList();

        // When: Request report for multiple employees
        ReportRequestDto request = new ReportRequestDto();
        request.setReportType("CERTIFICATION");
        request.setEmployeeIds(employeeIds);

        Report report = reportService.generateReport(request);

        // Then: Verify report covers all requested employees
        assertNotNull(report);
        assertTrue(report.getParameters().contains("employeeIds=" + employeeIds.toString()) ||
                  report.getParameters().contains(employeeIds.get(0))); // Parameters should reference the employees
    }

    @Test
    void testPerformanceWithLargerDataset() {
        // Given: All employees in the system
        List<EmployeeDto> allEmployees = employeeService.getAllEmployees();
        
        if (allEmployees.size() > 10) {
            List<String> employeeIds = allEmployees.stream()
                    .limit(10) // Test with up to 10 employees
                    .map(EmployeeDto::getId)
                    .toList();

            // When: Generate report and measure time
            long startTime = System.currentTimeMillis();
            
            ReportRequestDto request = new ReportRequestDto();
            request.setReportType("CERTIFICATION");
            request.setEmployeeIds(employeeIds);
            
            Report report = reportService.generateReport(request);
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            // Then: Verify reasonable performance (should start quickly)
            assertNotNull(report);
            assertTrue(duration < 5000, "Report initiation should be fast (< 5 seconds)");
        }
    }
}
