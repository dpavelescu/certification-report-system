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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CertificationService {
    
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
}
