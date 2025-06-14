package com.certreport.test;

import com.certreport.model.*;
import com.certreport.repository.*;
// import com.certreport.service.CertificationDataSeedingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Test data setup for performance testing
 * Ensures we have enough data to generate 100+ page reports
 */
@Component
@ActiveProfiles("test")
public class PerformanceTestDataSetup {

    @Autowired
    private EmployeeRepository employeeRepository;
    
    @Autowired
    private CertificationDefinitionRepository certificationDefinitionRepository;
    
    @Autowired
    private CertificationRepository certificationRepository;
      // @Autowired
    // private StageDefinitionRepository stageDefinitionRepository;
    
    // @Autowired
    // private TaskDefinitionRepository taskDefinitionRepository;
    
    // @Autowired
    // private StageRepository stageRepository;
    
    // @Autowired
    // private TaskRepository taskRepository;

    private final Random random = new Random(12345); // Fixed seed for reproducible tests

    /**
     * Creates additional test data to ensure large reports (100+ pages)
     * Estimates: ~20-25 employees per page, so we need ~120+ employees for 100+ pages
     */
    public void setupLargeDataset() {
        System.out.println("Setting up large dataset for performance testing...");
        
        long currentEmployeeCount = employeeRepository.count();
        long currentCertificationCount = certificationRepository.count();
        
        System.out.println("Current data size:");
        System.out.println("- Employees: " + currentEmployeeCount);
        System.out.println("- Certifications: " + currentCertificationCount);
        
        // Target: 150 employees to ensure 100+ page report
        int targetEmployees = 150;
        if (currentEmployeeCount < targetEmployees) {
            createAdditionalEmployees((int)(targetEmployees - currentEmployeeCount));
        }
        
        // Ensure all employees have multiple certifications
        ensureEmployeesHaveCertifications();
        
        long finalEmployeeCount = employeeRepository.count();
        long finalCertificationCount = certificationRepository.count();
        
        System.out.println("Final data size:");
        System.out.println("- Employees: " + finalEmployeeCount);
        System.out.println("- Certifications: " + finalCertificationCount);
        System.out.println("- Estimated report pages: " + (finalEmployeeCount / 20));
        System.out.println("Large dataset setup completed.");
    }

    private void createAdditionalEmployees(int count) {
        System.out.println("Creating " + count + " additional employees...");
        
        String[] departments = {"Engineering", "Sales", "Marketing", "HR", "Finance", "Operations", "Legal", "IT"};
        String[] positions = {"Manager", "Senior", "Junior", "Lead", "Specialist", "Coordinator", "Analyst", "Director"};
        String[] firstNames = {"John", "Jane", "Michael", "Sarah", "David", "Lisa", "Robert", "Emily", "James", "Jessica"};
        String[] lastNames = {"Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Rodriguez", "Martinez"};
        
        List<Employee> employees = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            Employee employee = new Employee();
            employee.setFirstName(firstNames[random.nextInt(firstNames.length)]);
            employee.setLastName(lastNames[random.nextInt(lastNames.length)] + i); // Ensure uniqueness
            employee.setEmail(employee.getFirstName().toLowerCase() + "." + 
                            employee.getLastName().toLowerCase() + "@testcompany.com");
            employee.setDepartment(departments[random.nextInt(departments.length)]);            employee.setPosition(positions[random.nextInt(positions.length)] + " " + 
                                departments[random.nextInt(departments.length)]);
            employee.setHireDate(LocalDateTime.now().minusDays(random.nextInt(3650))); // Up to 10 years ago
            
            employees.add(employee);
        }
        
        employeeRepository.saveAll(employees);
        System.out.println("Created " + count + " additional employees.");
    }

    private void ensureEmployeesHaveCertifications() {
        System.out.println("Ensuring all employees have certifications...");
        
        List<Employee> allEmployees = employeeRepository.findAll();
        List<CertificationDefinition> certificationDefinitions = certificationDefinitionRepository.findAllActive();
        
        if (certificationDefinitions.isEmpty()) {
            System.out.println("No certification definitions found. Creating sample definitions...");
            createSampleCertificationDefinitions();
            certificationDefinitions = certificationDefinitionRepository.findAllActive();
        }
        
        int certificationsCreated = 0;
        
        for (Employee employee : allEmployees) {
            // Check if employee already has certifications
            long existingCount = certificationRepository.countByEmployeeId(employee.getId());
            
            if (existingCount < 2) { // Ensure each employee has at least 2 certifications
                int certificationsToCreate = 2 + random.nextInt(3); // 2-4 certifications per employee
                
                for (int i = 0; i < certificationsToCreate; i++) {
                    CertificationDefinition certDef = certificationDefinitions.get(
                        random.nextInt(certificationDefinitions.size()));
                    
                    // Check if employee already has this certification
                    boolean alreadyHas = certificationRepository
                        .findByEmployeeIdAndCertificationDefinitionId(employee.getId(), certDef.getId())
                        .isPresent();
                    
                    if (!alreadyHas) {
                        createCertificationForEmployee(employee, certDef);
                        certificationsCreated++;
                    }
                }
            }
        }
        
        System.out.println("Created " + certificationsCreated + " additional certifications.");
    }

    private void createSampleCertificationDefinitions() {
        List<CertificationDefinition> definitions = new ArrayList<>();
        
        // Create diverse certification types
        String[][] certTypes = {
            {"Safety Leadership Certification", "Advanced safety management and leadership", "Safety & Compliance"},
            {"Data Privacy Specialist", "GDPR and privacy regulations compliance", "Privacy & Security"},
            {"Project Management Professional", "Advanced project management methodologies", "Management"},
            {"Quality Assurance Expert", "Quality control and assurance processes", "Quality"},
            {"Digital Marketing Certification", "Modern digital marketing strategies", "Marketing"},
            {"Financial Analysis Specialist", "Advanced financial analysis and reporting", "Finance"},
            {"Cybersecurity Professional", "Information security and cyber defense", "Security"},
            {"Agile Methodology Expert", "Scrum and Agile development practices", "Development"}
        };
        
        for (String[] certType : certTypes) {
            CertificationDefinition cert = new CertificationDefinition();
            cert.setName(certType[0]);
            cert.setDescription(certType[1]);
            cert.setCategory(certType[2]);
            cert.setTotalDurationHours(20.0 + random.nextDouble() * 40.0); // 20-60 hours
            cert.setValidityPeriodMonths(12 + random.nextInt(24)); // 12-36 months
            cert.setActive(true);
            
            definitions.add(cert);
        }
        
        certificationDefinitionRepository.saveAll(definitions);
        System.out.println("Created " + definitions.size() + " sample certification definitions.");
    }

    private void createCertificationForEmployee(Employee employee, CertificationDefinition certDef) {
        Certification certification = new Certification();
        certification.setEmployee(employee);
        certification.setCertificationDefinition(certDef);
        certification.setEnrolledAt(LocalDateTime.now().minusDays(random.nextInt(365)));
        
        // Set random status
        Certification.CertificationStatus[] statuses = Certification.CertificationStatus.values();
        certification.setStatus(statuses[random.nextInt(statuses.length)]);
        
        // Set completion percentage based on status
        switch (certification.getStatus()) {
            case COMPLETED:
                certification.setCompletionPercentage(100.0);
                certification.setCompletedAt(certification.getEnrolledAt().plusDays(random.nextInt(90)));
                break;
            case IN_PROGRESS:
                certification.setCompletionPercentage(20.0 + random.nextDouble() * 60.0); // 20-80%
                certification.setStartedAt(certification.getEnrolledAt().plusDays(random.nextInt(30)));
                break;
            case FAILED:
                certification.setCompletionPercentage(random.nextDouble() * 50.0); // 0-50%
                certification.setCompletedAt(certification.getEnrolledAt().plusDays(random.nextInt(120)));
                break;
            default:
                certification.setCompletionPercentage(0.0);
                break;
        }
        
        // Set due date
        certification.setDueDate(certification.getEnrolledAt().plusMonths(3));
        
        // Set current stage
        certification.setCurrentStageSequence(1 + random.nextInt(3)); // Stage 1-3
        
        certificationRepository.save(certification);
    }

    /**
     * Returns statistics about the current dataset
     */
    public DatasetStatistics getDatasetStatistics() {
        DatasetStatistics stats = new DatasetStatistics();
        stats.employeeCount = employeeRepository.count();
        stats.certificationDefinitionCount = certificationDefinitionRepository.count();
        stats.certificationCount = certificationRepository.count();
        stats.estimatedReportPages = (int)(stats.employeeCount / 20); // Rough estimate
        return stats;
    }

    public static class DatasetStatistics {
        public long employeeCount;
        public long certificationDefinitionCount;
        public long certificationCount;
        public int estimatedReportPages;
        
        @Override
        public String toString() {
            return String.format("Dataset: %d employees, %d cert types, %d certifications, ~%d pages", 
                               employeeCount, certificationDefinitionCount, certificationCount, estimatedReportPages);
        }
    }
}
