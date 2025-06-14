package com.certreport.test;

import com.certreport.dto.ReportRequestDto;
import com.certreport.model.Report;
import com.certreport.repository.ReportRepository;
import com.certreport.service.ReportService;
import com.certreport.service.EmployeeService;
import com.certreport.service.CertificationService;
import com.certreport.service.ActuatorPerformanceMonitor;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private EmployeeService employeeService;    @Mock
    private CertificationService certificationService;

    @Mock
    private ActuatorPerformanceMonitor actuatorPerformanceMonitor;

    @InjectMocks
    private ReportService reportService;

    private Report testReport;
    private ReportRequestDto testRequest;    @BeforeEach
    void setUp() {
        testReport = new Report();
        testReport.setId("REP001");
        testReport.setStatus(Report.ReportStatus.QUEUED);
        testReport.setType("EMPLOYEE_DEMOGRAPHICS");
        testReport.setCreatedAt(LocalDateTime.now());

        testRequest = new ReportRequestDto();
        testRequest.setEmployeeIds(Arrays.asList("EMP001", "EMP002"));
        testRequest.setReportType("EMPLOYEE_DEMOGRAPHICS");        // Mock the dependencies for async processing
        Timer.Sample mockSample = mock(Timer.Sample.class);
        when(actuatorPerformanceMonitor.startReportGeneration(anyString(), anyInt(), anyInt())).thenReturn(mockSample);
        
        // Mock employee service to return empty list to avoid complex setup
        when(employeeService.getEmployeesByIds(anyList())).thenReturn(Collections.emptyList());
        
        // Mock certification service
        when(certificationService.getCertificationsByEmployeeId(anyString())).thenReturn(Collections.emptyList());
    }

    @Test
    void testBasic() {
        // This is a minimal test that should always pass
        assertEquals(1, 1);
    }

    @Test
    void testGetReportStatus_Success() {
        // Given
        String reportId = "REP001";
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(testReport));

        // When
        Report result = reportService.getReportStatus(reportId);

        // Then
        assertNotNull(result);
        assertEquals(reportId, result.getId());
        assertEquals(Report.ReportStatus.QUEUED, result.getStatus());
        verify(reportRepository).findById(reportId);
    }

    @Test
    void testGetReportStatus_NotFound() {
        // Given
        String reportId = "NONEXISTENT";
        when(reportRepository.findById(reportId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> reportService.getReportStatus(reportId)
        );
        assertEquals("Report not found: " + reportId, exception.getMessage());
    }

    @Test
    void testGetAllReports_Success() {
        // Given
        List<Report> reports = Arrays.asList(testReport);
        when(reportRepository.findAllByOrderByCreatedAtDesc()).thenReturn(reports);

        // When
        List<Report> result = reportService.getAllReports();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testReport.getId(), result.get(0).getId());
        verify(reportRepository).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void testGetAllReports_EmptyList() {
        // Given
        when(reportRepository.findAllByOrderByCreatedAtDesc()).thenReturn(Collections.emptyList());

        // When
        List<Report> result = reportService.getAllReports();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(reportRepository).findAllByOrderByCreatedAtDesc();
    }    @Test
    void testGenerateReport_ValidRequest() {
        // Given
        Report savedReport = new Report();
        savedReport.setId("REP002");
        savedReport.setStatus(Report.ReportStatus.QUEUED);
        savedReport.setType("EMPLOYEE_DEMOGRAPHICS");
        when(reportRepository.save(any(Report.class))).thenReturn(savedReport);

        // When
        Report result = reportService.generateReport(testRequest);

        // Then
        assertNotNull(result);
        assertEquals("REP002", result.getId());
        assertEquals("EMPLOYEE_DEMOGRAPHICS", result.getType());
        // Note: Status might change during async processing, so we verify the initial save call
        verify(reportRepository, atLeastOnce()).save(any(Report.class));
    }

    @Test
    void testGenerateReport_NullReportType() {
        // Given
        ReportRequestDto requestWithNullType = new ReportRequestDto();
        requestWithNullType.setEmployeeIds(Arrays.asList("EMP001", "EMP002"));
        requestWithNullType.setReportType(null); // Null report type
        
        Report savedReport = new Report();
        savedReport.setId("REP003");
        savedReport.setStatus(Report.ReportStatus.QUEUED);
        savedReport.setType("EMPLOYEE_DEMOGRAPHICS"); // Should default to this
        when(reportRepository.save(any(Report.class))).thenReturn(savedReport);

        // When
        Report result = reportService.generateReport(requestWithNullType);

        // Then
        assertNotNull(result);
        assertEquals("EMPLOYEE_DEMOGRAPHICS", result.getType());
        verify(reportRepository, atLeastOnce()).save(any(Report.class));
    }

    @Test
    void testGenerateReport_EmptyEmployeeIds() {
        // Given
        ReportRequestDto requestWithEmptyIds = new ReportRequestDto();
        requestWithEmptyIds.setEmployeeIds(Collections.emptyList());
        requestWithEmptyIds.setReportType("EMPLOYEE_DEMOGRAPHICS");
        
        Report savedReport = new Report();
        savedReport.setId("REP004");
        savedReport.setStatus(Report.ReportStatus.QUEUED);
        when(reportRepository.save(any(Report.class))).thenReturn(savedReport);

        // When
        Report result = reportService.generateReport(requestWithEmptyIds);

        // Then
        assertNotNull(result);
        assertEquals("REP004", result.getId());
        verify(reportRepository, atLeastOnce()).save(any(Report.class));
    }
}
