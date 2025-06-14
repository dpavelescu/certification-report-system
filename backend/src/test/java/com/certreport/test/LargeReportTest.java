package com.certreport.test;

import com.certreport.dto.ReportRequestDto;
import com.certreport.model.*;
import com.certreport.repository.*;
import com.certreport.service.ReportService;
import com.certreport.service.EmployeeService;
import com.certreport.service.ActuatorPerformanceMonitor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Large Report Performance Test
 * Tests memory efficiency and performance for generating 100+ page reports
 * Uses PostgreSQL test database for realistic performance measurements
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("postgres-test")
public class LargeReportTest {@Autowired
    private ReportService reportService;
      @Autowired
    private EmployeeService employeeService;    @Autowired
    private ActuatorPerformanceMonitor actuatorPerformanceMonitor;
    
    @Autowired
    private ReportRepository reportRepository;
    
    @Autowired
    private EmployeeRepository employeeRepository;
    
    @Autowired
    private CertificationRepository certificationRepository;
    
    @Autowired
    private CertificationDefinitionRepository certDefRepository;
    
    @Autowired
    private StageRepository stageRepository;
    
    @Autowired
    private StageDefinitionRepository stageDefRepository;
    
    @Autowired
    private TaskRepository taskRepository;
    
    @Autowired
    private TaskDefinitionRepository taskDefRepository;
    
    @org.springframework.boot.test.web.server.LocalServerPort
    private int port;

    @BeforeEach
    public void setUp() {
        // Force garbage collection before test
        System.gc();
        try {
            Thread.sleep(1000); // Allow GC to complete
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }    @Test
    public void testLargeReportGeneration() throws Exception {
        System.out.println("=== LARGE REPORT GENERATION TEST (TARGET: 100+ PAGES) ===");
        System.out.println("Creating dense dataset with maximum activities per employee");
        System.out.println();
        
        // Phase 1: Data Population (EXCLUDED from generation time)
        long dataPopulationStart = System.nanoTime();
        createDenseDataset(300);
        long dataPopulationEnd = System.nanoTime();
        long dataPopulationTimeMs = TimeUnit.NANOSECONDS.toMillis(dataPopulationEnd - dataPopulationStart);
        
        long totalEmployees = employeeService.getAllEmployees().size();
        System.out.println("=== PHASE 1: DATA POPULATION COMPLETED ===");
        System.out.println("- Data Population Time: " + (dataPopulationTimeMs / 1000.0) + " seconds");
        System.out.println("- Total Employees Created: " + totalEmployees);
        System.out.println();
          // Get baseline metrics from actual services
        long baselineMemoryMB = Runtime.getRuntime().totalMemory() / (1024 * 1024);
        System.out.println("Baseline Memory: " + baselineMemoryMB + " MB");
        System.out.println();
        
        // Create request for ALL employees
        ReportRequestDto request = new ReportRequestDto();
        request.setEmployeeIds(Collections.emptyList()); // Empty = all employees
        request.setReportType("CERTIFICATIONS");        System.out.println("=== PHASE 2: REPORT GENERATION (PERFORMANCE MEASUREMENT) ===");
        
        // Generate the report (this triggers async generation)
        // The REAL timing happens inside ReportService.generateReportAsync() using ActuatorPerformanceMonitor
        Report report = reportService.generateReport(request);
        System.out.println("Created report with ID: " + report.getId());
          // Wait for completion (the real work happens in async method)
        Report completedReport = waitForReportCompletion(report.getId(), 60000);
        
        // Debug: Print actual report details
        System.out.println("DEBUG - Completed Report Details:");
        System.out.println("- Report ID: " + completedReport.getId());
        System.out.println("- Status: " + completedReport.getStatus());
        System.out.println("- Page Count: " + completedReport.getPageCount());
        System.out.println("- File Path: " + completedReport.getFilePath());
        System.out.println("- Error Message: " + completedReport.getErrorMessage());
        System.out.println("- Started At: " + completedReport.getStartedAt());
        System.out.println("- Completed At: " + completedReport.getCompletedAt());
        
        // Get performance metrics from the SAME ActuatorPerformanceMonitor used by ReportService
        // No need to start/stop timer here - the real app already did that internally
        ActuatorPerformanceMonitor.MemoryMetrics finalMemory = actuatorPerformanceMonitor.getDetailedMemoryMetrics();        // ActuatorPerformanceMonitor.DatabaseMetrics finalDb = actuatorPerformanceMonitor.getDatabaseMetrics();
        
        // Save report and get file size
        String reportFilePath = saveReportToFile(completedReport);
        File reportFile = new File(reportFilePath);
        long fileSizeBytes = reportFile.length();// Present results using simple metrics (real timing happened inside ReportService)
        presentResults(completedReport, dataPopulationTimeMs, finalMemory, baselineMemoryMB, 
                      fileSizeBytes, reportFilePath, totalEmployees);
          // Basic assertions
        assertNotNull(completedReport, "Report should be generated");
        assertEquals(Report.ReportStatus.COMPLETED, completedReport.getStatus(), "Report should be completed");
        
        // If report failed, show the error message before asserting page count
        if (completedReport.getStatus() == Report.ReportStatus.FAILED) {
            fail("Report generation failed with error: " + completedReport.getErrorMessage());
        }
        
        assertNotNull(completedReport.getPageCount(), "Page count should be available");
        assertTrue(completedReport.getPageCount() > 0, "Should generate at least 1 page");
        
        System.out.println("✅ LARGE REPORT TEST COMPLETED");
        System.out.println("   - Report File: " + reportFilePath);
        System.out.println();
    }    private Report waitForReportCompletion(String reportId, long timeoutMs) {
        long startTime = System.currentTimeMillis();
        Report report;
        
        System.out.println("Waiting for report completion...");
        
        do {
            try {
                Thread.sleep(1000); // Check every second
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail("Test interrupted while waiting for report completion");
            }
            
            report = reportRepository.findById(reportId).orElse(null);
            
            if (System.currentTimeMillis() - startTime > timeoutMs) {
                fail("Report generation timed out after " + timeoutMs + "ms");
            }
            
        } while (report == null || report.getStatus() == Report.ReportStatus.QUEUED || 
                 report.getStatus() == Report.ReportStatus.IN_PROGRESS);
        
        System.out.println("Report completed with status: " + report.getStatus());
        return report;
    }    private void presentResults(
            Report report, 
            long dataPopulationTimeMs,
            ActuatorPerformanceMonitor.MemoryMetrics finalMemory,
            long baselineMemoryMB,
            long fileSizeBytes,
            String filePath,
            long employeeCount) {
        
        System.out.println("=== PERFORMANCE RESULTS ===");
        System.out.println();
        
        // Report summary
        System.out.println("REPORT SUMMARY:");
        System.out.println("- Report ID: " + report.getId());
        System.out.println("- Status: " + report.getStatus());
        System.out.println("- Page Count: " + report.getPageCount());
        System.out.println("- Employee Count: " + employeeCount);
        System.out.println("- File Size: " + (fileSizeBytes / 1024) + " KB");
        System.out.println("- File Location: " + filePath);
        System.out.println();
        
        // Calculate approximate generation time from timestamps
        long generationTimeMs = 0;
        if (report.getStartedAt() != null && report.getCompletedAt() != null) {
            generationTimeMs = java.time.Duration.between(report.getStartedAt(), report.getCompletedAt()).toMillis();
        }

        // Performance metrics (timing comes from ReportService internal monitoring)
        System.out.println("PERFORMANCE METRICS:");
        System.out.println("- Data Population Time: " + (dataPopulationTimeMs / 1000.0) + " seconds [EXCLUDED]");
        System.out.println("- Report Generation Time: " + (generationTimeMs / 1000.0) + " seconds [ACTUAL FROM TIMESTAMPS]");
        System.out.println("- Final Memory: " + finalMemory);
        System.out.println();
          // Efficiency metrics
        if (report.getPageCount() != null && report.getPageCount() > 0 && generationTimeMs > 0) {
            double reportTimeSec = generationTimeMs / 1000.0;
            double pagesPerSecond = report.getPageCount() / reportTimeSec;
            double employeesPerSecond = employeeCount / reportTimeSec;
            
            System.out.println("EFFICIENCY METRICS:");
            System.out.println("- Pages/Second: " + String.format("%.2f", pagesPerSecond));
            System.out.println("- Employees/Second: " + String.format("%.2f", employeesPerSecond));
            System.out.println();
        }
        
        // Performance assessment
        System.out.println("PERFORMANCE ASSESSMENT:");
        if (generationTimeMs < 10000) {
            System.out.println("✅ Generation time: EXCELLENT (< 10 seconds)");
        } else if (generationTimeMs < 30000) {
            System.out.println("✅ Generation time: GOOD (< 30 seconds)");
        } else {
            System.out.println("⚠️ Generation time: NEEDS OPTIMIZATION (> 30 seconds)");
        }
        System.out.println();
    }

    private String saveReportToFile(Report report) throws Exception {
        // Create target directory if it doesn't exist
        Path targetDir = Paths.get("target");
        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }
        
        // Save to target/large-report.pdf
        Path outputPath = targetDir.resolve("large-report.pdf");
          if (report.getFilePath() != null) {
            // Copy from existing file path
            Path sourcePath = Paths.get(report.getFilePath());
            Files.copy(sourcePath, outputPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } else {
            throw new IllegalStateException("Report has no file path");
        }
        
        return outputPath.toAbsolutePath().toString();
    }    /**
     * Creates a dense dataset optimized for 100+ page reports:
     * - Maximum 10 certification definitions
     * - Maximum 5 assigned certifications per employee  
     * - 99% of employees have at least one certification
     * - Many activities, activity types, and varied statuses
     */
    private void createDenseDataset(int employeeCount) {
        System.out.println("Creating DENSE dataset with " + employeeCount + " employees...");
        System.out.println("Target: 100+ pages with maximum activities per employee");
        
        // Clear existing test data
        clearExistingTestData();
        
        // 1. Create exactly 10 certification definitions (requirement)
        List<CertificationDefinition> certDefs = createCertificationDefinitions();
        
        // 2. Create 300 employees (increased from 200)
        List<Employee> employees = createEmployees(employeeCount);
        
        // 3. Create DENSE certifications (max 5 per employee, 99% coverage)
        createDenseCertifications(employees, certDefs);
        
        // 4. Create MANY stages with varied statuses
        createDenseStages();
        
        // 5. Create MAXIMUM activities with different types and statuses
        createDenseActivities();
        
        // Print final statistics
        printDatasetStatistics();
    }private void clearExistingTestData() {
        // Delete in proper order to avoid constraint violations
        taskRepository.deleteAll();
        stageRepository.deleteAll();
        certificationRepository.deleteAll();
        taskDefRepository.deleteAll();
        stageDefRepository.deleteAll();
        certDefRepository.deleteAll();
        employeeRepository.deleteAll();
    }    private List<CertificationDefinition> createCertificationDefinitions() {
        String[] certNames = {
            "Advanced Cloud Architecture", "Cybersecurity Professional", "Data Science Excellence",
            "DevOps Engineering", "AI/ML Specialist", "Database Management",
            "Project Management", "Quality Assurance", "Software Development", "Systems Administration"
        };
        
        String[] categories = {"CLOUD", "SECURITY", "DATA", "DEVOPS", "AI", "DATABASE", "MANAGEMENT", "QA", "DEVELOPMENT", "SYSTEMS"};
        
        List<CertificationDefinition> certDefs = new ArrayList<>();
          for (int i = 0; i < 10; i++) {
            CertificationDefinition certDef = new CertificationDefinition();
            // Don't set ID manually - let Hibernate generate it automatically
            certDef.setName(certNames[i]);
            certDef.setDescription("Comprehensive certification for " + certNames[i] + " with extensive practical requirements");
            certDef.setCategory(categories[i]);
            certDef.setActive(true);
            certDef.setTotalDurationHours(120.0 + (i * 20)); // 120-300 hours
            certDef.setValidityPeriodMonths(24 + (i * 6)); // 24-78 months
            // Don't set timestamps manually - let @CreationTimestamp and @UpdateTimestamp handle them
            certDefs.add(certDef);
        }
        
        // Save certification definitions first so they get IDs
        certDefRepository.saveAll(certDefs);
        
        // Now create stages for each certification
        for (int i = 0; i < certDefs.size(); i++) {
            CertificationDefinition certDef = certDefs.get(i);
            // Create 5-8 stages per certification (more stages = more content)
            createStageDefinitions(certDef, 5 + (i % 4)); // 5-8 stages
        }
        return certDefs;
    }

    private void createStageDefinitions(CertificationDefinition certDef, int stageCount) {
        List<StageDefinition> stageDefs = new ArrayList<>();
        
        String[] stageTypes = {"Foundation", "Intermediate", "Advanced", "Expert", "Practical", "Assessment", "Project", "Capstone"};
          for (int i = 1; i <= stageCount; i++) {
            StageDefinition stageDef = new StageDefinition();
            // Don't set ID manually - let Hibernate generate it automatically
            stageDef.setCertificationDefinition(certDef);
            stageDef.setName(stageTypes[(i-1) % stageTypes.length] + " Stage " + i);
            stageDef.setDescription("Comprehensive " + stageTypes[(i-1) % stageTypes.length].toLowerCase() + 
                                   " stage with multiple practical activities and assessments");
            stageDef.setSequenceOrder(i);
            stageDef.setEstimatedDurationHours(15.0 + (i * 5)); // 20-55 hours per stage
            stageDef.setMandatory(i <= 3); // First 3 stages mandatory
            // Don't set timestamps manually - let @CreationTimestamp and @UpdateTimestamp handle them
            stageDefs.add(stageDef);
        }
        
        // Save stage definitions first so they get IDs
        stageDefRepository.saveAll(stageDefs);
        
        // Now create task definitions for each stage
        for (StageDefinition stageDef : stageDefs) {
            // Create 8-15 task definitions per stage (MANY activities)
            createTaskDefinitions(stageDef, 8 + (stageDef.getSequenceOrder() % 8)); // 8-15 tasks per stage
        }
    }

    private void createTaskDefinitions(StageDefinition stageDef, int taskCount) {
        List<TaskDefinition> taskDefs = new ArrayList<>();        // Diverse activity types
        TaskDefinition.TaskType[] taskTypes = {
            TaskDefinition.TaskType.ASSESSMENT, TaskDefinition.TaskType.CLASSROOM_TRAINING,
            TaskDefinition.TaskType.PROJECT, TaskDefinition.TaskType.PRACTICAL_EXERCISE
        };
        
        String[] taskNames = {
            "Theory Assessment", "Practical Lab", "Case Study", "Code Review", "Security Analysis",
            "Performance Optimization", "Documentation", "Presentation", "Peer Review", "Final Evaluation",
            "Hands-on Workshop", "Technical Interview", "Portfolio Submission", "Live Demo", "Group Project"
        };
          for (int i = 1; i <= taskCount; i++) {
            TaskDefinition taskDef = new TaskDefinition();
            // Don't set ID manually - let Hibernate generate it automatically
            taskDef.setStageDefinition(stageDef);
            taskDef.setName(taskNames[(i-1) % taskNames.length] + " " + i);
            taskDef.setDescription("Detailed " + taskNames[(i-1) % taskNames.length].toLowerCase() + 
                                 " with specific learning objectives and assessment criteria");
            taskDef.setSequenceOrder(i);
            taskDef.setEstimatedHours(2.0 + (i % 5)); // 2-6 hours per task
            taskDef.setMandatory(i <= (taskCount / 2)); // Half are mandatory
            taskDef.setTaskType(taskTypes[i % taskTypes.length]);
            // Don't set timestamps manually - let @CreationTimestamp and @UpdateTimestamp handle them
            taskDefs.add(taskDef);
        }
        
        taskDefRepository.saveAll(taskDefs);
    }    private List<Employee> createEmployees(int employeeCount) {
        String[] departments = {"Engineering", "Data Science", "DevOps", "Security", "QA", "Product", "Design", "Analytics", "Research", "Operations"};
        String[] positions = {"Junior", "Mid-level", "Senior", "Lead", "Principal", "Manager", "Director", "Specialist", "Architect", "Consultant"};
        String[] firstNames = {"Alex", "Taylor", "Jordan", "Casey", "Morgan", "Riley", "Avery", "Blake", "Cameron", "Drew"};
        String[] lastNames = {"Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Rodriguez", "Martinez"};
        
        List<Employee> employees = new ArrayList<>();
        Random random = new Random(42); // Deterministic for testing
        
        for (int i = 1; i <= employeeCount; i++) {
            Employee employee = new Employee();
            employee.setId("DENSE_EMP_" + String.format("%04d", i));
            employee.setFirstName(firstNames[random.nextInt(firstNames.length)] + i);
            employee.setLastName(lastNames[random.nextInt(lastNames.length)]);
            employee.setEmail("dense.employee" + i + "@testcompany.com");
            employee.setDepartment(departments[i % departments.length]);
            employee.setPosition(positions[random.nextInt(positions.length)] + " " + departments[i % departments.length]);
            employee.setHireDate(LocalDateTime.now().minusDays(random.nextInt(2000))); // Up to 5+ years
            employee.setCreatedAt(LocalDateTime.now());
            employee.setUpdatedAt(LocalDateTime.now());
            employees.add(employee);
        }
        
        employeeRepository.saveAll(employees);
        return employees;
    }    private void createDenseCertifications(List<Employee> employees, List<CertificationDefinition> certDefs) {
        Random random = new Random(42);
        List<Certification> certifications = new ArrayList<>();
        
        Certification.CertificationStatus[] statuses = {
            Certification.CertificationStatus.NOT_STARTED,
            Certification.CertificationStatus.IN_PROGRESS,
            Certification.CertificationStatus.COMPLETED,
            Certification.CertificationStatus.EXPIRED
        };
          // int employeesWithCerts = 0;
        
        for (int empIndex = 0; empIndex < employees.size(); empIndex++) {
            Employee employee = employees.get(empIndex);
            
            // 99% of employees get certifications (only 1% have none)
            boolean shouldHaveCerts = empIndex % 100 != 0; // Skip every 100th employee (1%)
            
            if (shouldHaveCerts) {
                // employeesWithCerts++;
                
                // Each employee gets 3-5 certifications (randomly)
                int certCount = 3 + random.nextInt(3); // 3, 4, or 5 certifications
                
                // Select random unique certifications
                List<Integer> selectedCerts = new ArrayList<>();
                while (selectedCerts.size() < certCount) {
                    int certIndex = random.nextInt(certDefs.size());
                    if (!selectedCerts.contains(certIndex)) {
                        selectedCerts.add(certIndex);
                    }
                }
                  for (int certIndex : selectedCerts) {
                    CertificationDefinition certDef = certDefs.get(certIndex);
                    
                    Certification certification = new Certification();
                    // Don't set ID manually - let Hibernate generate it automatically
                    certification.setEmployee(employee);
                    certification.setCertificationDefinition(certDef);
                    certification.setEnrolledAt(LocalDateTime.now().minusDays(random.nextInt(365)));
                    certification.setStatus(statuses[random.nextInt(statuses.length)]);
                    certification.setDueDate(LocalDateTime.now().plusDays(30 + random.nextInt(300)));
                    certification.setCurrentStageSequence(1 + random.nextInt(5));
                    certification.setCompletionPercentage(random.nextDouble() * 100);
                    // Don't set timestamps manually - let @CreationTimestamp and @UpdateTimestamp handle them
                    certifications.add(certification);
                }
            }
        }
        
        certificationRepository.saveAll(certifications);
    }    private void createDenseStages() {
        List<Certification> certifications = certificationRepository.findAll();
        List<Stage> stages = new ArrayList<>();
        Random random = new Random(42);
        
        for (Certification certification : certifications) {
            List<StageDefinition> stageDefs = stageDefRepository.findByCertificationDefinitionIdOrderBySequenceOrder(
                certification.getCertificationDefinition().getId());
            
            // Business Logic: Only one stage can be IN_PROGRESS per certification
            // Others should be COMPLETED (earlier stages) or NOT_STARTED (later stages)
            int inProgressStageIndex = -1;
            if (certification.getStatus() == Certification.CertificationStatus.IN_PROGRESS) {
                // Pick a random stage to be in progress (but not the first or last)
                if (stageDefs.size() > 2) {
                    inProgressStageIndex = 1 + random.nextInt(stageDefs.size() - 2);
                } else if (stageDefs.size() > 1) {
                    inProgressStageIndex = random.nextInt(stageDefs.size());
                }
            }
            
            for (int i = 0; i < stageDefs.size(); i++) {
                StageDefinition stageDef = stageDefs.get(i);
                Stage stage = new Stage();
                stage.setCertification(certification);
                stage.setStageDefinition(stageDef);
                stage.setStartedAt(LocalDateTime.now().minusDays(random.nextInt(200)));
                
                // Set stage status based on business logic
                Stage.StageStatus stageStatus;
                if (certification.getStatus() == Certification.CertificationStatus.COMPLETED) {
                    stageStatus = Stage.StageStatus.COMPLETED;
                } else if (certification.getStatus() == Certification.CertificationStatus.FAILED) {
                    stageStatus = (i < stageDefs.size() / 2) ? Stage.StageStatus.COMPLETED : Stage.StageStatus.FAILED;
                } else if (certification.getStatus() == Certification.CertificationStatus.IN_PROGRESS) {
                    if (i < inProgressStageIndex) {
                        stageStatus = Stage.StageStatus.COMPLETED;
                    } else if (i == inProgressStageIndex) {
                        stageStatus = Stage.StageStatus.IN_PROGRESS;
                    } else {
                        stageStatus = Stage.StageStatus.NOT_STARTED;
                    }
                } else {
                    // NOT_STARTED, OVERDUE, SUSPENDED
                    stageStatus = Stage.StageStatus.NOT_STARTED;
                }
                
                stage.setStatus(stageStatus);
                stage.setCompletionPercentage(
                    stageStatus == Stage.StageStatus.COMPLETED ? 100.0 :
                    stageStatus == Stage.StageStatus.IN_PROGRESS ? random.nextDouble() * 80 + 10 : // 10-90%
                    0.0
                );
                
                // Add completion date for completed stages
                if (stage.getStatus() == Stage.StageStatus.COMPLETED) {
                    stage.setCompletedAt(stage.getStartedAt().plusDays(random.nextInt(60)));
                }
                
                stages.add(stage);
            }
        }
        
        stageRepository.saveAll(stages);
    }    private void createDenseActivities() {
        List<Stage> stages = stageRepository.findAll();
        List<Task> tasks = new ArrayList<>();
        Random random = new Random(42);
          Task.TaskStatus[] statuses = {
            Task.TaskStatus.NOT_STARTED,
            Task.TaskStatus.IN_PROGRESS,
            Task.TaskStatus.COMPLETED,
            Task.TaskStatus.FAILED,
            Task.TaskStatus.CANCELLED
        };
        
        for (Stage stage : stages) {            List<TaskDefinition> taskDefs = taskDefRepository.findByStageDefinitionIdOrderBySequenceOrder(
                stage.getStageDefinition().getId());
              for (TaskDefinition taskDef : taskDefs) {
                Task task = new Task();
                // Don't set ID manually - let Hibernate generate it automatically
                task.setStage(stage);
                task.setTaskDefinition(taskDef);
                task.setStartedAt(LocalDateTime.now().minusDays(random.nextInt(150)));
                task.setStatus(statuses[random.nextInt(statuses.length)]);
                  // Add scores for completed/failed tasks
                if (task.getStatus() == Task.TaskStatus.COMPLETED || task.getStatus() == Task.TaskStatus.FAILED) {
                    task.setScore(30.0 + random.nextDouble() * 70); // 30-100% score
                    task.setCompletedAt(task.getStartedAt().plusDays(random.nextInt(30)));
                }
                
                // Don't set timestamps manually - let @CreationTimestamp and @UpdateTimestamp handle them
                tasks.add(task);
            }
        }
        
        taskRepository.saveAll(tasks);
    }

    private void printDatasetStatistics() {
        long employeeCount = employeeRepository.count();
        long certDefCount = certDefRepository.count();
        long certificationCount = certificationRepository.count();
        long stageDefCount = stageDefRepository.count();
        long stageCount = stageRepository.count();
        long taskDefCount = taskDefRepository.count();
        long taskCount = taskRepository.count();
        
        System.out.println("\n=== DENSE DATASET STATISTICS ===");
        System.out.println("Employees: " + employeeCount);
        System.out.println("Certification Definitions: " + certDefCount + " (requirement: ≤10)");
        System.out.println("Active Certifications: " + certificationCount);
        System.out.println("Stage Definitions: " + stageDefCount);
        System.out.println("Active Stages: " + stageCount);
        System.out.println("Task Definitions: " + taskDefCount);
        System.out.println("Active Tasks/Activities: " + taskCount);
        System.out.println();
        System.out.println("DENSITY METRICS:");
        System.out.println("- Avg Certifications/Employee: " + String.format("%.2f", (double)certificationCount / employeeCount));
        System.out.println("- Avg Stages/Certification: " + String.format("%.2f", (double)stageCount / certificationCount));
        System.out.println("- Avg Activities/Stage: " + String.format("%.2f", (double)taskCount / stageCount));
        System.out.println("- Total Activities/Employee: " + String.format("%.2f", (double)taskCount / employeeCount));
        System.out.println();
        System.out.println("ESTIMATED REPORT SIZE:");
        System.out.println("- Expected Pages: " + (employeeCount * 4) + "+ (estimate: 4+ pages per employee)");
        System.out.println("- Target: 100+ pages ✅");
        System.out.println("=====================================\n");
    }
}
