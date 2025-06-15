package com.certreport.test;

import com.certreport.controller.ReportController;
import com.certreport.dto.ReportRequestDto;
import com.certreport.model.Report;
import com.certreport.service.ReportService;
import com.certreport.service.ReportCleanupService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReportController.class)
public class ReportControllerTest {    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;

    @MockitoBean
    private ReportCleanupService reportCleanupService;

    @Autowired
    private ObjectMapper objectMapper;

    private Report testReport;
    private ReportRequestDto testRequest;

    @BeforeEach
    void setUp() {
        testReport = new Report();
        testReport.setId("REP001");
        testReport.setStatus(Report.ReportStatus.QUEUED);
        testReport.setType("EMPLOYEE_DEMOGRAPHICS");
        testReport.setCreatedAt(LocalDateTime.now());

        testRequest = new ReportRequestDto();
        testRequest.setEmployeeIds(Arrays.asList("EMP001", "EMP002"));
        testRequest.setReportType("EMPLOYEE_DEMOGRAPHICS");
    }

    @Test
    void testBasic() throws Exception {
        // Basic test to ensure controller loads
        mockMvc.perform(get("/api/reports"))
                .andExpect(status().isOk());
    }

    @Test
    void testGenerateReport_Success() throws Exception {
        // Given
        when(reportService.generateReport(any(ReportRequestDto.class))).thenReturn(testReport);

        // When & Then
        mockMvc.perform(post("/api/reports/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("REP001"))
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andExpect(jsonPath("$.type").value("EMPLOYEE_DEMOGRAPHICS"));

        verify(reportService).generateReport(any(ReportRequestDto.class));
    }    @Test
    void testGenerateReport_InvalidRequest() throws Exception {
        // Given - empty request body
        ReportRequestDto invalidRequest = new ReportRequestDto();
        // Don't set any fields - should cause validation errors
        
        // Mock service to return null for invalid request
        when(reportService.generateReport(any(ReportRequestDto.class))).thenReturn(null);

        // When & Then
        mockMvc.perform(post("/api/reports/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetAllReports_Success() throws Exception {
        // Given
        List<Report> reports = Arrays.asList(testReport);
        when(reportService.getAllReports()).thenReturn(reports);

        // When & Then
        mockMvc.perform(get("/api/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value("REP001"))
                .andExpect(jsonPath("$[0].status").value("QUEUED"))
                .andExpect(jsonPath("$[0].type").value("EMPLOYEE_DEMOGRAPHICS"));

        verify(reportService).getAllReports();
    }

    @Test
    void testGetAllReports_EmptyList() throws Exception {
        // Given
        when(reportService.getAllReports()).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(reportService).getAllReports();
    }    @Test
    void testGetReportStatus_Success() throws Exception {
        // Given
        String reportId = "REP001";
        when(reportService.getReportStatus(reportId)).thenReturn(testReport);

        // When & Then
        mockMvc.perform(get("/api/reports/{id}/status", reportId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportId").value("REP001"))
                .andExpect(jsonPath("$.status").value("QUEUED"));

        verify(reportService).getReportStatus(reportId);
    }

    @Test
    void testGetReportStatus_NotFound() throws Exception {
        // Given
        String reportId = "NONEXISTENT";
        when(reportService.getReportStatus(reportId))
                .thenThrow(new RuntimeException("Report not found: " + reportId));

        // When & Then
        mockMvc.perform(get("/api/reports/{id}/status", reportId))
                .andExpect(status().isNotFound());

        verify(reportService).getReportStatus(reportId);
    }

    @Test
    void testGenerateReport_ServiceError() throws Exception {
        // Given
        when(reportService.generateReport(any(ReportRequestDto.class)))
                .thenThrow(new RuntimeException("Service error"));

        // When & Then
        mockMvc.perform(post("/api/reports/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isBadRequest());

        verify(reportService).generateReport(any(ReportRequestDto.class));
    }
}
