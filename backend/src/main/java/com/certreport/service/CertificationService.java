package com.certreport.service;

import com.certreport.dto.*;
import com.certreport.model.*;
import com.certreport.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Comparator;

@Service
@Transactional(readOnly = true)
public class CertificationService {
    
    // private static final Logger logger = LoggerFactory.getLogger(CertificationService.class);
    
    private final CertificationRepository certificationRepository;
    private final CertificationDefinitionRepository certificationDefinitionRepository;
    private final StageRepository stageRepository;
    private final TaskRepository taskRepository;
    private final EmployeeRepository employeeRepository;
    
    public CertificationService(CertificationRepository certificationRepository,
                               CertificationDefinitionRepository certificationDefinitionRepository,
                               StageRepository stageRepository,
                               TaskRepository taskRepository,
                               EmployeeRepository employeeRepository) {
        this.certificationRepository = certificationRepository;
        this.certificationDefinitionRepository = certificationDefinitionRepository;
        this.stageRepository = stageRepository;
        this.taskRepository = taskRepository;
        this.employeeRepository = employeeRepository;
    }
    
    public CertificationFilterResponseDto filterCertifications(CertificationFilterRequestDto filterRequest) {
        // Create pageable with sorting
        Sort sort = Sort.by(Sort.Direction.fromString(filterRequest.getSortDirection()), filterRequest.getSortBy());
        Pageable pageable = PageRequest.of(filterRequest.getPage(), filterRequest.getSize(), sort);
        
        // Execute filtered query
        Page<Certification> certificationPage = certificationRepository.findWithFilters(
                filterRequest.getEmployeeIds(),
                filterRequest.getCertificationDefinitionIds(),
                filterRequest.getStatuses(),
                filterRequest.getStartDate(),
                filterRequest.getEndDate(),
                pageable
        );
        
        // Convert to DTOs
        List<CertificationDto> certificationDtos = certificationPage.getContent().stream()
                .map(this::convertToDetailedDto)
                .collect(Collectors.toList());
        
        return new CertificationFilterResponseDto(
                certificationDtos,
                certificationPage.getTotalElements(),
                certificationPage.getTotalPages(),
                certificationPage.getNumber(),
                certificationPage.getSize(),
                filterRequest.getSortBy(),
                filterRequest.getSortDirection()
        );
    }
    
    public List<CertificationDefinitionDto> getAllCertificationDefinitions() {
        return certificationDefinitionRepository.findAllActive().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<CertificationDefinitionDto> getCertificationDefinitionsForEmployees(List<String> employeeIds) {
        if (employeeIds == null || employeeIds.isEmpty()) {
            return getAllCertificationDefinitions();
        }
        
        // Get certification definitions that have enrollments for the specified employees
        List<Certification> certifications = certificationRepository.findAll();
        return certifications.stream()
                .filter(cert -> employeeIds.contains(cert.getEmployee().getId()))
                .map(cert -> cert.getCertificationDefinition())
                .distinct()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public Optional<CertificationDto> getCertificationById(String id) {
        return certificationRepository.findById(id)
                .map(this::convertToDetailedDto);
    }
    
    public List<CertificationDto> getCertificationsByEmployeeId(String employeeId) {
        return certificationRepository.findByEmployeeId(employeeId).stream()
                .map(this::convertToDetailedDto)
                .collect(Collectors.toList());
    }
    
    public List<CertificationDto> getCertificationsByCertificationDefinitionId(String certificationDefinitionId) {
        return certificationRepository.findByCertificationDefinitionId(certificationDefinitionId).stream()
                .map(this::convertToDetailedDto)
                .collect(Collectors.toList());
    }
    
    // Preview methods for filter interface
    public Long getEmployeeCountForCertifications(List<String> certificationDefinitionIds) {
        if (certificationDefinitionIds == null || certificationDefinitionIds.isEmpty()) {
            return employeeRepository.count();
        }
        
        return certificationRepository.findAll().stream()
                .filter(cert -> certificationDefinitionIds.contains(cert.getCertificationDefinition().getId()))
                .map(cert -> cert.getEmployee().getId())
                .distinct()
                .count();
    }
    
    public Long getCertificationCountForEmployees(List<String> employeeIds) {
        if (employeeIds == null || employeeIds.isEmpty()) {
            return certificationRepository.count();
        }
        
        return certificationRepository.findAll().stream()
                .filter(cert -> employeeIds.contains(cert.getEmployee().getId()))
                .count();
    }
    
    // DTO Conversion methods
    private CertificationDto convertToDetailedDto(Certification certification) {
        CertificationDto dto = new CertificationDto();
        dto.setId(certification.getId());
        dto.setEmployee(convertToDto(certification.getEmployee()));
        dto.setCertificationDefinition(convertToDto(certification.getCertificationDefinition()));
        dto.setStatus(certification.getStatus());
        dto.setCompletionPercentage(certification.getCompletionPercentage());
        dto.setEnrolledAt(certification.getEnrolledAt());
        dto.setCompletedAt(certification.getCompletedAt());
        dto.setDueDate(certification.getDueDate());
        
        // Get current stage information
        Optional<Stage> currentStage = stageRepository.findCurrentStageByCertificationId(certification.getId());
        if (currentStage.isPresent()) {
            Stage stage = currentStage.get();
            dto.setCurrentStageId(stage.getId());
            dto.setCurrentStageName(stage.getStageDefinition().getName());
            dto.setCurrentStageSequence(stage.getStageDefinition().getSequenceOrder());
        }
        
        // Get stage progress
        List<Stage> stages = stageRepository.findByCertificationIdOrderBySequence(certification.getId());
        List<StageProgressDto> stageProgress = stages.stream()
                .map(this::convertToStageProgressDto)
                .collect(Collectors.toList());
        dto.setStageProgress(stageProgress);
        
        return dto;
    }
    
    private StageProgressDto convertToStageProgressDto(Stage stage) {
        StageProgressDto dto = new StageProgressDto();
        dto.setId(stage.getId());
        dto.setStageDefinitionId(stage.getStageDefinition().getId());
        dto.setName(stage.getStageDefinition().getName());
        dto.setDescription(stage.getStageDefinition().getDescription());
        dto.setSequenceOrder(stage.getStageDefinition().getSequenceOrder());
        dto.setStatus(stage.getStatus());
        dto.setCompletionPercentage(stage.getCompletionPercentage());
        dto.setEstimatedDurationHours(stage.getStageDefinition().getEstimatedDurationHours());
        dto.setIsMandatory(stage.getStageDefinition().getIsMandatory());
        dto.setStartedAt(stage.getStartedAt());
        dto.setCompletedAt(stage.getCompletedAt());
        dto.setDueDate(stage.getDueDate());
        
        // Get task progress for this stage
        Long completedTasks = taskRepository.countCompletedTasksByStageId(stage.getId());
        Long totalTasks = taskRepository.countTotalTasksByStageId(stage.getId());
        dto.setCompletedTasks(completedTasks);
        dto.setTotalTasks(totalTasks);
        
        return dto;
    }
    
    private CertificationDefinitionDto convertToDto(CertificationDefinition certificationDefinition) {
        CertificationDefinitionDto dto = new CertificationDefinitionDto();
        dto.setId(certificationDefinition.getId());
        dto.setName(certificationDefinition.getName());
        dto.setDescription(certificationDefinition.getDescription());
        dto.setCategory(certificationDefinition.getCategory());
        dto.setTotalDurationHours(certificationDefinition.getTotalDurationHours());
        dto.setValidityPeriodMonths(certificationDefinition.getValidityPeriodMonths());
        dto.setIsActive(certificationDefinition.getIsActive());
        
        // Get enrollment count
        Long enrollmentCount = certificationRepository.countByCertificationDefinitionId(certificationDefinition.getId());
        dto.setEnrollmentCount(enrollmentCount);
        
        return dto;
    }
    
    private EmployeeDto convertToDto(Employee employee) {
        EmployeeDto dto = new EmployeeDto();
        dto.setId(employee.getId());
        dto.setFirstName(employee.getFirstName());
        dto.setLastName(employee.getLastName());
        dto.setEmail(employee.getEmail());
        dto.setDepartment(employee.getDepartment());
        dto.setPosition(employee.getPosition());
        dto.setHireDate(employee.getHireDate());
        return dto;
    }
    
    // Efficient chunked data retrieval for reporting
      /**
     * Get certification data for a chunk of employees with all related data in ONE OPTIMIZED query
     * This method replaces the previous 4-query + in-memory processing approach
     */    public List<CompleteReportDataDto> getCertificationDataChunk(List<String> employeeIds) {
        if (employeeIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Single comprehensive query - gets ALL data in one go
        List<Certification> certifications = certificationRepository.findCompleteReportDataByEmployeeIds(employeeIds);
        
        // Group certifications by employee (minimal in-memory processing)
        Map<String, List<Certification>> certificationsByEmployee = certifications.stream()
                .collect(Collectors.groupingBy(cert -> cert.getEmployee().getId()));
        
        // Convert to DTOs efficiently - all data is already loaded
        List<CompleteReportDataDto> result = new ArrayList<>();
        for (Map.Entry<String, List<Certification>> entry : certificationsByEmployee.entrySet()) {
            Employee employee = entry.getValue().get(0).getEmployee(); // Employee is already loaded
            
            // Convert employee to DTO
            EmployeeDto employeeDto = convertToDto(employee);
            
            // Convert certifications to DTOs - no additional queries needed since everything is pre-loaded
            List<CertificationDto> certificationDtos = entry.getValue().stream()
                    .map(this::convertToDetailedDtoWithPreloadedData)
                    .collect(Collectors.toList());
            
            result.add(new CompleteReportDataDto(employeeDto, certificationDtos));
        }
          // Handle employees with no certifications
        List<String> employeesWithCerts = result.stream()
                .map(data -> data.getEmployee().getId())
                .collect(Collectors.toList());
        
        List<String> employeesWithoutCerts = employeeIds.stream()
                .filter(id -> !employeesWithCerts.contains(id))
                .collect(Collectors.toList());
          if (!employeesWithoutCerts.isEmpty()) {
            List<Employee> employeesWithoutCertifications = employeeRepository.findAllById(employeesWithoutCerts);
            
            for (Employee employee : employeesWithoutCertifications) {
                EmployeeDto employeeDto = convertToDto(employee);
                result.add(new CompleteReportDataDto(employeeDto, new ArrayList<>()));
            }
        }
        return result;
    }
      /**
     * Get certification data in chunks for memory-efficient processing
     */
    public List<CompleteReportDataDto> getAllCertificationDataChunked(int chunkSize) {
        List<CompleteReportDataDto> allData = new ArrayList<>();
        int page = 0;
        List<String> employeeChunk;
        
        do {
            // Get employee IDs in chunks using pagination
            employeeChunk = employeeRepository.findEmployeeIdsChunked(page, chunkSize);
            
            if (!employeeChunk.isEmpty()) {
                List<CompleteReportDataDto> chunkData = getCertificationDataChunk(employeeChunk);
                allData.addAll(chunkData);
                page++;
            }
        } while (employeeChunk.size() == chunkSize); // Continue if we got a full chunk
        
        return allData;
    }    /**
     * Conversion using pre-loaded data from comprehensive query - ZERO additional queries
     * All data (employee, cert def, stages, stage defs, tasks, task defs) is already loaded
     */
    private CertificationDto convertToDetailedDtoWithPreloadedData(Certification certification) {
        CertificationDto dto = new CertificationDto();
        dto.setId(certification.getId());
        dto.setStatus(certification.getStatus());
        dto.setCompletionPercentage(certification.getCompletionPercentage());
        dto.setEnrolledAt(certification.getEnrolledAt());
        dto.setCompletedAt(certification.getCompletedAt());
        dto.setDueDate(certification.getDueDate());
        
        // Convert certification definition (already loaded via JOIN FETCH)
        if (certification.getCertificationDefinition() != null) {
            dto.setCertificationDefinition(convertToDto(certification.getCertificationDefinition()));
        }
        
        // Convert employee (already loaded via JOIN FETCH)
        if (certification.getEmployee() != null) {
            dto.setEmployee(convertToDto(certification.getEmployee()));
        }        // Convert stages with all related data (already loaded via LEFT JOIN FETCH)
        if (certification.getStages() != null && !certification.getStages().isEmpty()) {
            List<StageProgressDto> stageProgress = certification.getStages().stream()
                    .sorted(Comparator.comparing(stage -> stage.getStageDefinition().getSequenceOrder()))
                    .map(this::convertToStageProgressDtoWithPreloadedData)
                    .collect(Collectors.toList());
            dto.setStageProgress(stageProgress);
            
            // Set current stage information
            Optional<Stage> currentStage = certification.getStages().stream()
                    .filter(stage -> stage.getStatus() == Stage.StageStatus.IN_PROGRESS)
                    .findFirst();
            
            if (currentStage.isPresent()) {
                Stage stage = currentStage.get();
                dto.setCurrentStageId(stage.getId());
                dto.setCurrentStageName(stage.getStageDefinition().getName());
                dto.setCurrentStageSequence(stage.getStageDefinition().getSequenceOrder());
            }
        }
        
        return dto;
    }
    
    /**
     * Convert stage with pre-loaded task data - ZERO additional queries
     */
    private StageProgressDto convertToStageProgressDtoWithPreloadedData(Stage stage) {
        StageProgressDto dto = new StageProgressDto();
        dto.setId(stage.getId());
        dto.setStageDefinitionId(stage.getStageDefinition().getId());
        dto.setName(stage.getStageDefinition().getName());
        dto.setDescription(stage.getStageDefinition().getDescription());
        dto.setSequenceOrder(stage.getStageDefinition().getSequenceOrder());
        dto.setStatus(stage.getStatus());
        dto.setCompletionPercentage(stage.getCompletionPercentage());
        dto.setEstimatedDurationHours(stage.getStageDefinition().getEstimatedDurationHours());
        dto.setIsMandatory(stage.getStageDefinition().getIsMandatory());
        dto.setStartedAt(stage.getStartedAt());
        dto.setCompletedAt(stage.getCompletedAt());
        dto.setDueDate(stage.getDueDate());
          // Calculate task progress from pre-loaded task data (tasks are already sorted by sequenceOrder)
        if (stage.getTasks() != null) {
            long totalTasks = stage.getTasks().size();
            long completedTasks = stage.getTasks().stream()
                    .mapToLong(task -> task.getStatus() == Task.TaskStatus.COMPLETED ? 1 : 0)
                    .sum();
            
            dto.setCompletedTasks(completedTasks);
            dto.setTotalTasks(totalTasks);
        } else {
            dto.setCompletedTasks(0L);
            dto.setTotalTasks(0L);
        }
          return dto;
    }
}
