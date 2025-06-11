package com.certreport.controller;

import com.certreport.dto.*;
import com.certreport.service.CertificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/certifications")
public class CertificationController {
    
    private final CertificationService certificationService;
    
    public CertificationController(CertificationService certificationService) {
        this.certificationService = certificationService;
    }
    
    @PostMapping("/filter")
    public ResponseEntity<CertificationFilterResponseDto> filterCertifications(
            @RequestBody CertificationFilterRequestDto filterRequest) {
        CertificationFilterResponseDto response = certificationService.filterCertifications(filterRequest);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/definitions")
    public ResponseEntity<List<CertificationDefinitionDto>> getAllCertificationDefinitions() {
        List<CertificationDefinitionDto> definitions = certificationService.getAllCertificationDefinitions();
        return ResponseEntity.ok(definitions);
    }
    
    @PostMapping("/definitions/for-employees")
    public ResponseEntity<List<CertificationDefinitionDto>> getCertificationDefinitionsForEmployees(
            @RequestBody List<String> employeeIds) {
        List<CertificationDefinitionDto> definitions = certificationService.getCertificationDefinitionsForEmployees(employeeIds);
        return ResponseEntity.ok(definitions);
    }
    
    // Alternative GET endpoint for frontend compatibility
    @GetMapping("/available")
    public ResponseEntity<List<CertificationDefinitionDto>> getAvailableCertificationsForEmployees(
            @RequestParam List<String> employeeIds) {
        List<CertificationDefinitionDto> definitions = certificationService.getCertificationDefinitionsForEmployees(employeeIds);
        return ResponseEntity.ok(definitions);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<CertificationDto> getCertificationById(@PathVariable String id) {
        Optional<CertificationDto> certification = certificationService.getCertificationById(id);
        return certification.map(ResponseEntity::ok)
                           .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<CertificationDto>> getCertificationsByEmployeeId(@PathVariable String employeeId) {
        List<CertificationDto> certifications = certificationService.getCertificationsByEmployeeId(employeeId);
        return ResponseEntity.ok(certifications);
    }
    
    @GetMapping("/definition/{certificationDefinitionId}")
    public ResponseEntity<List<CertificationDto>> getCertificationsByCertificationDefinitionId(
            @PathVariable String certificationDefinitionId) {
        List<CertificationDto> certifications = certificationService.getCertificationsByCertificationDefinitionId(certificationDefinitionId);
        return ResponseEntity.ok(certifications);
    }
    
    // Preview endpoints for filter interface
    @PostMapping("/preview/employee-count")
    public ResponseEntity<PreviewCountDto> getEmployeeCountForCertifications(
            @RequestBody List<String> certificationDefinitionIds) {
        Long count = certificationService.getEmployeeCountForCertifications(certificationDefinitionIds);
        return ResponseEntity.ok(new PreviewCountDto(count, "employees"));
    }
    
    @PostMapping("/preview/certification-count")
    public ResponseEntity<PreviewCountDto> getCertificationCountForEmployees(
            @RequestBody List<String> employeeIds) {
        Long count = certificationService.getCertificationCountForEmployees(employeeIds);
        return ResponseEntity.ok(new PreviewCountDto(count, "certifications"));
    }
    
    // Helper DTO for preview counts
    public static class PreviewCountDto {
        private Long count;
        private String type;
        
        public PreviewCountDto(Long count, String type) {
            this.count = count;
            this.type = type;
        }
        
        public Long getCount() {
            return count;
        }
        
        public void setCount(Long count) {
            this.count = count;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
    }
}
