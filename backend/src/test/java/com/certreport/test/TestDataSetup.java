package com.certreport.test;

import com.certreport.model.*;
import com.certreport.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Reusable test data setup for performance testing.
 * Extracted from LargeReportTest to avoid cross-test dependencies.
 * 
 * This class creates realistic test data that can be used by multiple test classes
 * without requiring them to depend on each other.
 */
@Component
public class TestDataSetup {
    
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private CertificationDefinitionRepository certDefRepository;
    @Autowired private StageDefinitionRepository stageDefRepository;
    @Autowired private TaskDefinitionRepository taskDefRepository;
    @Autowired private CertificationRepository certificationRepository;
    @Autowired private StageRepository stageRepository;
    @Autowired private TaskRepository taskRepository;
    
    private final Random random = new Random(42); // Deterministic for reproducible tests
    
    /**
     * Creates a complete test dataset with the specified number of employees.
     * This includes all related certification data for realistic performance testing.
     */
    @Transactional
    public void createTestDataset(int employeeCount) {
        System.out.println("🔧 Creating test dataset with " + employeeCount + " employees...");
        long startTime = System.currentTimeMillis();
        
        // Clear existing data first
        clearAllData();
        
        // Create the data structure
        List<CertificationDefinition> certDefs = createCertificationDefinitions();
        List<Employee> employees = createEmployees(employeeCount);
        createCertifications(employees, certDefs);
        createStages();
        createTasks();
        
        long duration = System.currentTimeMillis() - startTime;
        System.out.println("✅ Test dataset created in " + (duration / 1000.0) + " seconds");
        
        printDatasetStatistics();
    }
    
    /**
     * Clears all test data in the correct order to avoid constraint violations
     */
    @Transactional
    public void clearAllData() {
        taskRepository.deleteAll();
        stageRepository.deleteAll();
        certificationRepository.deleteAll();
        taskDefRepository.deleteAll();
        stageDefRepository.deleteAll();
        certDefRepository.deleteAll();
        employeeRepository.deleteAll();
    }
    
    /**
     * Gets all employee IDs from the current dataset
     */
    public List<String> getEmployeeIds() {
        return employeeRepository.findAll().stream()
                .map(Employee::getId)
                .sorted()
                .toList();
    }
    
    /**
     * Creates certification definitions (10 total as per requirements)
     */
    private List<CertificationDefinition> createCertificationDefinitions() {
        String[] certNames = {
            "Java Enterprise Development", "Cloud Architecture", "DevOps Engineering",
            "Data Science Fundamentals", "Cybersecurity Essentials", "UI/UX Design",
            "Machine Learning Operations", "Agile Project Management", "API Design",
            "Database Administration"
        };
        
        String[] categories = {"Technical", "Leadership", "Security", "Design"};
        
        List<CertificationDefinition> certDefs = new ArrayList<>();
        for (int i = 0; i < certNames.length; i++) {            CertificationDefinition certDef = new CertificationDefinition();
            certDef.setName(certNames[i]);
            certDef.setCategory(categories[i % categories.length]);
            certDef.setDescription("Professional certification in " + certNames[i].toLowerCase());
            certDef.setValidityPeriodMonths(24);
            certDef.setTotalDurationHours(100.0 + (i * 20)); // Vary duration hours
            certDef.setCreatedAt(LocalDateTime.now());
            certDef.setUpdatedAt(LocalDateTime.now());
            certDefs.add(certDef);
        }
        
        List<CertificationDefinition> saved = certDefRepository.saveAll(certDefs);
        
        // Create stage definitions for each certification
        createStageDefinitions(saved);
        
        return saved;
    }
    
    /**
     * Creates stage definitions for certification definitions
     */
    private void createStageDefinitions(List<CertificationDefinition> certDefs) {
        String[] stageNames = {"Foundation", "Intermediate", "Advanced", "Mastery", "Certification"};
        List<StageDefinition> stageDefs = new ArrayList<>();
        
        for (CertificationDefinition certDef : certDefs) {
            for (int i = 0; i < stageNames.length; i++) {
                StageDefinition stageDef = new StageDefinition();
                stageDef.setCertificationDefinition(certDef);                stageDef.setName(stageNames[i]);
                stageDef.setDescription(stageNames[i] + " level for " + certDef.getName());
                stageDef.setSequenceOrder(i + 1);
                stageDef.setEstimatedDurationHours(20.0 + (i * 10)); // 20, 30, 40, 50, 60 hours
                stageDef.setIsMandatory(true);
                stageDef.setCreatedAt(LocalDateTime.now());
                stageDef.setUpdatedAt(LocalDateTime.now());
                stageDefs.add(stageDef);
            }
        }
        
        List<StageDefinition> savedStageDefs = stageDefRepository.saveAll(stageDefs);
        
        // Create task definitions for each stage
        createTaskDefinitions(savedStageDefs);
    }
    
    /**
     * Creates task definitions for stage definitions
     */
    private void createTaskDefinitions(List<StageDefinition> stageDefs) {
        String[] taskNames = {"Theory Assessment", "Practical Lab", "Case Study", "Code Review", "Final Evaluation"};
        TaskDefinition.TaskType[] taskTypes = {
            TaskDefinition.TaskType.ASSESSMENT, TaskDefinition.TaskType.PRACTICAL_EXERCISE,
            TaskDefinition.TaskType.PROJECT, TaskDefinition.TaskType.REVIEW, TaskDefinition.TaskType.PRESENTATION
        };
        
        List<TaskDefinition> taskDefs = new ArrayList<>();
        
        for (StageDefinition stageDef : stageDefs) {
            for (int i = 0; i < taskNames.length; i++) {
                TaskDefinition taskDef = new TaskDefinition();
                taskDef.setStageDefinition(stageDef);
                taskDef.setName(taskNames[i]);
                taskDef.setDescription("Complete " + taskNames[i].toLowerCase() + " for " + stageDef.getName());
                taskDef.setSequenceOrder(i + 1);
                taskDef.setEstimatedHours(4.0 + (i * 2)); // 4, 6, 8, 10, 12 hours
                taskDef.setMandatory(i < 3); // First 3 tasks are mandatory
                taskDef.setTaskType(taskTypes[i]);
                taskDef.setCreatedAt(LocalDateTime.now());
                taskDef.setUpdatedAt(LocalDateTime.now());
                taskDefs.add(taskDef);
            }
        }
        
        taskDefRepository.saveAll(taskDefs);
    }
    
    /**
     * Creates employees with realistic data
     */
    private List<Employee> createEmployees(int employeeCount) {
        String[] departments = {"Engineering", "Data Science", "DevOps", "Security", "QA"};
        String[] positions = {"Junior", "Senior", "Lead", "Principal", "Manager"};
        String[] firstNames = {"Alex", "Taylor", "Jordan", "Casey", "Morgan"};
        String[] lastNames = {"Smith", "Johnson", "Williams", "Brown", "Jones"};
        
        List<Employee> employees = new ArrayList<>();
        LocalDateTime baseDate = LocalDateTime.now();
        
        for (int i = 1; i <= employeeCount; i++) {
            Employee employee = new Employee();
            employee.setId("TEST_EMP_" + String.format("%05d", i));
            employee.setFirstName(firstNames[i % firstNames.length] + i);
            employee.setLastName(lastNames[i % lastNames.length]);
            employee.setEmail("test.employee" + i + "@testcompany.com");
            employee.setDepartment(departments[i % departments.length]);
            employee.setPosition(positions[i % positions.length] + " " + departments[i % departments.length]);
            employee.setHireDate(baseDate.minusDays(random.nextInt(1000)));
            employee.setCreatedAt(baseDate);
            employee.setUpdatedAt(baseDate);
            employees.add(employee);
        }
        
        return employeeRepository.saveAll(employees);
    }
    
    /**
     * Creates certifications for employees
     */
    private void createCertifications(List<Employee> employees, List<CertificationDefinition> certDefs) {
        List<Certification> certifications = new ArrayList<>();
        Certification.CertificationStatus[] statuses = {
            Certification.CertificationStatus.NOT_STARTED,
            Certification.CertificationStatus.IN_PROGRESS,
            Certification.CertificationStatus.COMPLETED,
            Certification.CertificationStatus.EXPIRED
        };
        
        LocalDateTime baseDate = LocalDateTime.now();
        
        // Each employee gets 3-4 certifications for performance testing
        for (Employee employee : employees) {
            // Skip every 20th employee to have some without certifications (5%)
            if (employee.getId().endsWith("0")) continue;
            
            int certCount = 3 + (employee.getId().hashCode() % 2); // 3 or 4 certifications
            
            for (int i = 0; i < certCount && i < certDefs.size(); i++) {
                CertificationDefinition certDef = certDefs.get(i % certDefs.size());
                
                Certification certification = new Certification();
                certification.setEmployee(employee);
                certification.setCertificationDefinition(certDef);
                certification.setEnrolledAt(baseDate.minusDays(random.nextInt(200)));
                certification.setStatus(statuses[i % statuses.length]);
                certification.setDueDate(baseDate.plusDays(30 + random.nextInt(200)));
                certification.setCurrentStageSequence(1 + (i % 3));
                certification.setCompletionPercentage(random.nextDouble() * 100);
                certification.setCreatedAt(baseDate);
                certification.setUpdatedAt(baseDate);
                certifications.add(certification);
            }
        }
        
        certificationRepository.saveAll(certifications);
    }
    
    /**
     * Creates stages for all certifications
     */
    private void createStages() {
        List<Certification> certifications = certificationRepository.findAll();
        List<Stage> stages = new ArrayList<>();
        
        for (Certification certification : certifications) {
            List<StageDefinition> stageDefs = stageDefRepository.findByCertificationDefinitionIdOrderBySequenceOrder(
                certification.getCertificationDefinition().getId());
            
            for (int i = 0; i < stageDefs.size(); i++) {
                StageDefinition stageDef = stageDefs.get(i);
                Stage stage = new Stage();
                stage.setCertification(certification);
                stage.setStageDefinition(stageDef);
                stage.setStartedAt(LocalDateTime.now().minusDays(random.nextInt(100)));
                
                // Simple status assignment
                if (i < 2) {
                    stage.setStatus(Stage.StageStatus.COMPLETED);
                    stage.setCompletionPercentage(100.0);
                    stage.setCompletedAt(stage.getStartedAt().plusDays(random.nextInt(30)));
                } else if (i == 2) {
                    stage.setStatus(Stage.StageStatus.IN_PROGRESS);
                    stage.setCompletionPercentage(30.0 + random.nextDouble() * 50);
                } else {
                    stage.setStatus(Stage.StageStatus.NOT_STARTED);
                    stage.setCompletionPercentage(0.0);
                }
                
                stage.setCreatedAt(LocalDateTime.now());
                stage.setUpdatedAt(LocalDateTime.now());
                stages.add(stage);
            }
        }
        
        stageRepository.saveAll(stages);
    }
    
    /**
     * Creates tasks for all stages
     */
    private void createTasks() {
        List<Stage> stages = stageRepository.findAll();
        List<Task> tasks = new ArrayList<>();
        Task.TaskStatus[] statuses = {
            Task.TaskStatus.NOT_STARTED, Task.TaskStatus.IN_PROGRESS,
            Task.TaskStatus.COMPLETED, Task.TaskStatus.FAILED
        };
        
        LocalDateTime baseDate = LocalDateTime.now();
        
        for (Stage stage : stages) {
            List<TaskDefinition> taskDefs = taskDefRepository.findByStageDefinitionIdOrderBySequenceOrder(
                stage.getStageDefinition().getId());
            
            for (int i = 0; i < taskDefs.size(); i++) {
                TaskDefinition taskDef = taskDefs.get(i);
                Task task = new Task();
                task.setStage(stage);
                task.setTaskDefinition(taskDef);
                task.setStartedAt(baseDate.minusDays(random.nextInt(60)));
                task.setStatus(statuses[i % statuses.length]);
                
                if (task.getStatus() == Task.TaskStatus.COMPLETED || task.getStatus() == Task.TaskStatus.FAILED) {
                    task.setScore(50.0 + random.nextDouble() * 50); // 50-100% score
                    task.setCompletedAt(task.getStartedAt().plusDays(random.nextInt(14)));
                }
                
                task.setCreatedAt(baseDate);
                task.setUpdatedAt(baseDate);
                tasks.add(task);
            }
        }
        
        taskRepository.saveAll(tasks);
    }
    
    /**
     * Prints statistics about the created dataset
     */
    private void printDatasetStatistics() {
        long employeeCount = employeeRepository.count();
        long certDefCount = certDefRepository.count();
        long certificationCount = certificationRepository.count();
        long stageCount = stageRepository.count();
        long taskCount = taskRepository.count();
        
        System.out.println("\n=== TEST DATASET STATISTICS ===");
        System.out.println("📊 Employees: " + employeeCount);
        System.out.println("📚 Certification Definitions: " + certDefCount);
        System.out.println("🎓 Active Certifications: " + certificationCount);
        System.out.println("📋 Active Stages: " + stageCount);
        System.out.println("✅ Active Tasks: " + taskCount);
        
        if (employeeCount > 0) {
            System.out.println("\n📈 METRICS:");
            System.out.println("   Certifications/Employee: " + String.format("%.1f", (double)certificationCount / employeeCount));
            System.out.println("   Tasks/Employee: " + String.format("%.0f", (double)taskCount / employeeCount));
            System.out.println("   Expected Report Pages: " + (employeeCount * 3) + "+");
        }
        System.out.println("=====================================\n");
    }
}
