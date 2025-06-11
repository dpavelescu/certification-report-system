package com.certreport.service;

import com.certreport.model.*;
import com.certreport.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class CertificationDataSeedingService implements CommandLineRunner {
    
    private final CertificationDefinitionRepository certificationDefinitionRepository;
    private final StageDefinitionRepository stageDefinitionRepository;
    private final TaskDefinitionRepository taskDefinitionRepository;
    private final CertificationRepository certificationRepository;
    private final StageRepository stageRepository;
    private final TaskRepository taskRepository;
    private final EmployeeRepository employeeRepository;
    
    private final Random random = new Random();
    
    public CertificationDataSeedingService(
            CertificationDefinitionRepository certificationDefinitionRepository,
            StageDefinitionRepository stageDefinitionRepository,
            TaskDefinitionRepository taskDefinitionRepository,
            CertificationRepository certificationRepository,
            StageRepository stageRepository,
            TaskRepository taskRepository,
            EmployeeRepository employeeRepository) {
        this.certificationDefinitionRepository = certificationDefinitionRepository;
        this.stageDefinitionRepository = stageDefinitionRepository;
        this.taskDefinitionRepository = taskDefinitionRepository;
        this.certificationRepository = certificationRepository;
        this.stageRepository = stageRepository;
        this.taskRepository = taskRepository;
        this.employeeRepository = employeeRepository;
    }
    
    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (certificationDefinitionRepository.count() == 0) {
            System.out.println("Seeding certification data...");
            seedCertificationData();
            System.out.println("Certification data seeding completed.");
        }
    }
    
    private void seedCertificationData() {
        List<CertificationDefinition> certificationDefinitions = createCertificationDefinitions();
        List<Employee> employees = employeeRepository.findAll();
        
        if (employees.isEmpty()) {
            System.out.println("No employees found. Please seed employee data first.");
            return;
        }
        
        // Enroll 80% of employees in random certifications
        enrollEmployees(certificationDefinitions, employees);
    }
    
    private List<CertificationDefinition> createCertificationDefinitions() {
        List<CertificationDefinition> definitions = new ArrayList<>();
        
        // 1. Workplace Safety Certification
        CertificationDefinition safety = createSafetyCertification();
        definitions.add(safety);
        
        // 2. Data Privacy and Security
        CertificationDefinition privacy = createPrivacyCertification();
        definitions.add(privacy);
        
        return definitions;
    }
    
    private CertificationDefinition createSafetyCertification() {
        CertificationDefinition cert = new CertificationDefinition();
        cert.setName("Workplace Safety Certification");
        cert.setDescription("Comprehensive workplace safety training covering OSHA guidelines, emergency procedures, and risk assessment.");
        cert.setCategory("Safety & Compliance");
        cert.setTotalDurationHours(40.0);
        cert.setValidityPeriodMonths(12);
        cert.setActive(true);
        cert = certificationDefinitionRepository.save(cert);
        
        // Stage 1: Foundation
        StageDefinition stage1 = createStageDefinition(cert, 1, "Safety Fundamentals", "Basic safety principles and regulations", 16.0, true);
        createTaskDefinition(stage1, 1, "OSHA Regulations Overview", TaskDefinition.TaskType.ELEARNING, 4.0, false, true);
        createTaskDefinition(stage1, 2, "Workplace Hazard Identification", TaskDefinition.TaskType.ELEARNING, 6.0, false, true);
        createTaskDefinition(stage1, 3, "Emergency Procedures", TaskDefinition.TaskType.CLASSROOM_TRAINING, 4.0, false, true);
        createTaskDefinition(stage1, 4, "Safety Equipment Training", TaskDefinition.TaskType.PRACTICAL_EXERCISE, 2.0, true, true);
        
        // Stage 2: Advanced
        StageDefinition stage2 = createStageDefinition(cert, 2, "Advanced Safety Practices", "Advanced safety procedures and leadership", 16.0, true);
        createTaskDefinition(stage2, 1, "Risk Assessment Methods", TaskDefinition.TaskType.CLASSROOM_TRAINING, 8.0, false, true);
        createTaskDefinition(stage2, 2, "Safety Incident Investigation", TaskDefinition.TaskType.CASE_STUDY, 4.0, false, true);
        createTaskDefinition(stage2, 3, "Safety Leadership", TaskDefinition.TaskType.WORKSHOP, 4.0, false, true);
        
        // Stage 3: Certification
        StageDefinition stage3 = createStageDefinition(cert, 3, "Certification Assessment", "Final assessment and certification", 8.0, true);
        createTaskDefinition(stage3, 1, "Written Examination", TaskDefinition.TaskType.ASSESSMENT, 2.0, false, true);
        createTaskDefinition(stage3, 2, "Practical Assessment", TaskDefinition.TaskType.PRACTICAL_EXERCISE, 4.0, true, true);
        createTaskDefinition(stage3, 3, "Certification Review", TaskDefinition.TaskType.REVIEW, 2.0, true, true);
        
        return cert;
    }
    
    private CertificationDefinition createPrivacyCertification() {
        CertificationDefinition cert = new CertificationDefinition();
        cert.setName("Data Privacy and Security");
        cert.setDescription("Comprehensive training on data protection, GDPR compliance, and cybersecurity best practices.");
        cert.setCategory("Security & Compliance");
        cert.setTotalDurationHours(24.0);
        cert.setValidityPeriodMonths(24);
        cert.setActive(true);
        cert = certificationDefinitionRepository.save(cert);
        
        // Stage 1: Foundations
        StageDefinition stage1 = createStageDefinition(cert, 1, "Privacy Fundamentals", "Basic privacy principles and regulations", 8.0, true);
        createTaskDefinition(stage1, 1, "GDPR Overview", TaskDefinition.TaskType.ELEARNING, 3.0, false, true);
        createTaskDefinition(stage1, 2, "Data Classification", TaskDefinition.TaskType.ELEARNING, 2.0, false, true);
        createTaskDefinition(stage1, 3, "Privacy by Design", TaskDefinition.TaskType.CLASSROOM_TRAINING, 3.0, false, true);
        
        // Stage 2: Implementation
        StageDefinition stage2 = createStageDefinition(cert, 2, "Security Implementation", "Implementing security measures and procedures", 12.0, true);
        createTaskDefinition(stage2, 1, "Cybersecurity Best Practices", TaskDefinition.TaskType.ELEARNING, 4.0, false, true);
        createTaskDefinition(stage2, 2, "Incident Response Planning", TaskDefinition.TaskType.WORKSHOP, 4.0, false, true);
        createTaskDefinition(stage2, 3, "Security Tools Training", TaskDefinition.TaskType.PRACTICAL_EXERCISE, 4.0, true, true);
        
        // Stage 3: Assessment
        StageDefinition stage3 = createStageDefinition(cert, 3, "Compliance Assessment", "Final assessment and certification", 4.0, true);
        createTaskDefinition(stage3, 1, "Compliance Audit", TaskDefinition.TaskType.ASSESSMENT, 2.0, true, true);
        createTaskDefinition(stage3, 2, "Final Review", TaskDefinition.TaskType.REVIEW, 2.0, true, true);
        
        return cert;
    }
    
    private StageDefinition createStageDefinition(CertificationDefinition cert, int sequence, String name, String description, double duration, boolean mandatory) {
        StageDefinition stage = new StageDefinition();
        stage.setCertificationDefinition(cert);
        stage.setSequenceOrder(sequence);
        stage.setName(name);
        stage.setDescription(description);
        stage.setEstimatedDurationHours(duration);
        stage.setMandatory(mandatory);
        return stageDefinitionRepository.save(stage);
    }
    
    private TaskDefinition createTaskDefinition(StageDefinition stage, int sequence, String name, TaskDefinition.TaskType taskType, double duration, boolean supervisorApproval, boolean mandatory) {
        TaskDefinition task = new TaskDefinition();
        task.setStageDefinition(stage);
        task.setSequenceOrder(sequence);
        task.setName(name);
        task.setTaskType(taskType);
        task.setEstimatedHours(duration);
        task.setRequiresSupervisorApproval(supervisorApproval);
        task.setMandatory(mandatory);
        return taskDefinitionRepository.save(task);
    }
    
    private void enrollEmployees(List<CertificationDefinition> certificationDefinitions, List<Employee> employees) {
        int targetEnrollments = (int) (employees.size() * 0.8); // 80% enrollment rate
        
        for (CertificationDefinition certDef : certificationDefinitions) {
            // Randomly select employees for this certification
            List<Employee> selectedEmployees = selectRandomEmployees(employees, targetEnrollments);
            
            for (Employee employee : selectedEmployees) {
                createCertificationEnrollment(employee, certDef);
            }
        }
    }
    
    private List<Employee> selectRandomEmployees(List<Employee> allEmployees, int count) {
        List<Employee> shuffled = new ArrayList<>(allEmployees);
        Collections.shuffle(shuffled, random);
        return shuffled.subList(0, Math.min(count, shuffled.size()));
    }
    
    private void createCertificationEnrollment(Employee employee, CertificationDefinition certDef) {
        // Create certification enrollment
        Certification certification = new Certification();
        certification.setEmployee(employee);
        certification.setCertificationDefinition(certDef);
        certification.setEnrolledAt(generateRandomEnrollmentDate());
        certification.setDueDate(certification.getEnrolledAt().plusMonths(certDef.getValidityPeriodMonths()));
        
        // Randomly assign status based on realistic distribution
        Certification.CertificationStatus status = generateRandomCertificationStatus();
        certification.setStatus(status);
        
        if (status == Certification.CertificationStatus.COMPLETED) {
            certification.setCompletedAt(certification.getEnrolledAt().plusDays(random.nextInt(90)));
            certification.setCompletionPercentage(100.0);
        } else if (status == Certification.CertificationStatus.IN_PROGRESS) {
            certification.setCompletionPercentage(random.nextDouble() * 80 + 10); // 10-90%
        } else {
            certification.setCompletionPercentage(0.0);
        }
        
        certification = certificationRepository.save(certification);
        
        // Create stages and tasks for this certification
        createStagesAndTasks(certification);
    }
    
    private LocalDateTime generateRandomEnrollmentDate() {
        // Generate dates within the last 12 months
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneYearAgo = now.minusMonths(12);
        long daysBetween = ChronoUnit.DAYS.between(oneYearAgo, now);
        return oneYearAgo.plusDays(random.nextInt((int) daysBetween));
    }
    
    private Certification.CertificationStatus generateRandomCertificationStatus() {
        int rand = random.nextInt(100);
        if (rand < 25) return Certification.CertificationStatus.COMPLETED;
        if (rand < 60) return Certification.CertificationStatus.IN_PROGRESS;
        if (rand < 75) return Certification.CertificationStatus.NOT_STARTED;
        if (rand < 85) return Certification.CertificationStatus.OVERDUE;
        if (rand < 95) return Certification.CertificationStatus.SUSPENDED;
        return Certification.CertificationStatus.FAILED;
    }
    
    private void createStagesAndTasks(Certification certification) {
        List<StageDefinition> stageDefinitions = stageDefinitionRepository
                .findByCertificationDefinitionIdOrderBySequenceOrder(certification.getCertificationDefinition().getId());
        
        for (StageDefinition stageDef : stageDefinitions) {
            Stage stage = new Stage();
            stage.setCertification(certification);
            stage.setStageDefinition(stageDef);
            
            // Set stage status based on certification progress
            Stage.StageStatus stageStatus = generateStageStatus(certification.getStatus(), stageDef.getSequenceOrder(), stageDefinitions.size());
            stage.setStatus(stageStatus);
            
            if (stageStatus == Stage.StageStatus.COMPLETED) {
                stage.setStartedAt(certification.getEnrolledAt().plusDays(random.nextInt(30)));
                stage.setCompletedAt(stage.getStartedAt().plusDays(random.nextInt(21)));
                stage.setCompletionPercentage(100.0);
            } else if (stageStatus == Stage.StageStatus.IN_PROGRESS) {
                stage.setStartedAt(certification.getEnrolledAt().plusDays(random.nextInt(30)));
                stage.setCompletionPercentage(random.nextDouble() * 80 + 10);
            } else {
                stage.setCompletionPercentage(0.0);
            }
            
            stage.setDueDate(certification.getEnrolledAt().plusDays(30L * stageDef.getSequenceOrder()));
            stage = stageRepository.save(stage);
            
            // Create tasks for this stage
            createTasksForStage(stage);
        }
    }
    
    private Stage.StageStatus generateStageStatus(Certification.CertificationStatus certStatus, int stageSequence, int totalStages) {
        if (certStatus == Certification.CertificationStatus.COMPLETED) {
            return Stage.StageStatus.COMPLETED;
        } else if (certStatus == Certification.CertificationStatus.IN_PROGRESS) {
            // Realistic progression: earlier stages more likely to be completed
            double progressRatio = 1.0 - (double) stageSequence / totalStages;
            int rand = random.nextInt(100);
            
            if (progressRatio > 0.7 && rand < 80) return Stage.StageStatus.COMPLETED;
            if (progressRatio > 0.3 && rand < 60) return Stage.StageStatus.IN_PROGRESS;
            return Stage.StageStatus.NOT_STARTED;
        } else if (certStatus == Certification.CertificationStatus.OVERDUE) {
            return random.nextBoolean() ? Stage.StageStatus.OVERDUE : Stage.StageStatus.IN_PROGRESS;
        } else if (certStatus == Certification.CertificationStatus.FAILED) {
            return random.nextBoolean() ? Stage.StageStatus.FAILED : Stage.StageStatus.NOT_STARTED;
        } else if (certStatus == Certification.CertificationStatus.SUSPENDED) {
            return Stage.StageStatus.SUSPENDED;
        }
        
        return Stage.StageStatus.NOT_STARTED;
    }
    
    private void createTasksForStage(Stage stage) {
        List<TaskDefinition> taskDefinitions = taskDefinitionRepository
                .findByStageDefinitionIdOrderBySequenceOrder(stage.getStageDefinition().getId());
        
        for (TaskDefinition taskDef : taskDefinitions) {
            Task task = new Task();
            task.setStage(stage);
            task.setTaskDefinition(taskDef);
              // Set task status based on stage progress
            Task.TaskStatus taskStatus = generateTaskStatus(stage.getStatus(), taskDef.getSequenceOrder(), taskDefinitions.size());
            task.setStatus(taskStatus);
            
            if (taskStatus == Task.TaskStatus.COMPLETED) {
                task.setStartedAt(stage.getStartedAt() != null ? stage.getStartedAt().plusDays(random.nextInt(7)) : null);
                task.setCompletedAt(task.getStartedAt() != null ? task.getStartedAt().plusHours(random.nextInt(8) + 1) : null);
                task.setActualHours(taskDef.getEstimatedHours() + (random.nextDouble() - 0.5) * 2); // +/- 1 hour variance
                task.setScore(70.0 + random.nextDouble() * 30); // 70-100 score
                
                if (taskDef.getRequiresSupervisorApproval()) {
                    task.setSupervisorApproved(random.nextBoolean());
                    if (task.getSupervisorApproved()) {
                        task.setSupervisorId("supervisor-" + random.nextInt(10));
                    }                }
            } else if (taskStatus == Task.TaskStatus.IN_PROGRESS) {
                task.setStartedAt(stage.getStartedAt() != null ? stage.getStartedAt().plusDays(random.nextInt(7)) : null);
                task.setActualHours(random.nextDouble() * taskDef.getEstimatedHours());
            }
            
            task.setDueDate(stage.getDueDate() != null ? stage.getDueDate().minusDays(taskDefinitions.size() - taskDef.getSequenceOrder()) : null);
            taskRepository.save(task);
        }
    }
      private Task.TaskStatus generateTaskStatus(Stage.StageStatus stageStatus, int taskSequence, int totalTasks) {
        if (stageStatus == Stage.StageStatus.COMPLETED) {
            return Task.TaskStatus.COMPLETED;
        } else if (stageStatus == Stage.StageStatus.IN_PROGRESS) {
            // Tasks progress sequentially within a stage
            double progressRatio = 1.0 - (double) taskSequence / totalTasks;
            int rand = random.nextInt(100);
            
            if (progressRatio > 0.8 && rand < 90) return Task.TaskStatus.COMPLETED;
            if (progressRatio > 0.5 && rand < 70) return Task.TaskStatus.IN_PROGRESS;
            return Task.TaskStatus.NOT_STARTED;
        } else if (stageStatus == Stage.StageStatus.OVERDUE) {
            return random.nextBoolean() ? Task.TaskStatus.OVERDUE : Task.TaskStatus.IN_PROGRESS;
        } else if (stageStatus == Stage.StageStatus.FAILED) {
            return random.nextBoolean() ? Task.TaskStatus.FAILED : Task.TaskStatus.NOT_STARTED;
        } else if (stageStatus == Stage.StageStatus.SUSPENDED) {
            // Map SUSPENDED to CANCELLED since Task.TaskStatus doesn't have SUSPENDED
            return Task.TaskStatus.CANCELLED;
        }
        
        return Task.TaskStatus.NOT_STARTED;
    }
}
