package com.certreport.test;

import com.certreport.controller.EmployeeController;
import com.certreport.dto.EmployeeDto;
import com.certreport.service.EmployeeService;
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

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for EmployeeController
 */
@WebMvcTest(EmployeeController.class)
public class EmployeeControllerTest {    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmployeeService employeeService;

    private EmployeeDto testEmployee1;
    private EmployeeDto testEmployee2;

    @BeforeEach
    void setUp() {
        testEmployee1 = new EmployeeDto("EMP001", "John", "Doe", "john.doe@company.com", 
                                       "Engineering", "Software Engineer", LocalDateTime.of(2023, 1, 15, 9, 0));
        testEmployee2 = new EmployeeDto("EMP002", "Jane", "Smith", "jane.smith@company.com", 
                                       "HR", "HR Manager", LocalDateTime.of(2023, 2, 1, 9, 0));
    }

    @Test
    void testGetAllEmployees_Success() throws Exception {
        // Given
        List<EmployeeDto> employees = Arrays.asList(testEmployee1, testEmployee2);
        when(employeeService.getAllEmployees()).thenReturn(employees);

        // When & Then
        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("EMP001"))
                .andExpect(jsonPath("$[0].firstName").value("John"))
                .andExpect(jsonPath("$[0].department").value("Engineering"));

        verify(employeeService).getAllEmployees();
    }

    @Test
    void testGetAllEmployees_EmptyList() throws Exception {
        // Given
        when(employeeService.getAllEmployees()).thenReturn(Arrays.asList());

        // When & Then
        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(employeeService).getAllEmployees();
    }
}
