// Types for the Certification Report System

export interface Employee {
  id: string; // UUID as string
  firstName: string;
  lastName: string;
  email: string;
  department: string;
  position: string;
  hireDate: string;
}

// Certification-related types
export interface CertificationDefinition {
  id: string;
  name: string;
  description: string;
  category: string;
  totalDurationHours: number;
  validityPeriodMonths: number;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface Certification {
  id: string;
  employee: Employee;
  certificationDefinition: CertificationDefinition;
  status: 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED' | 'OVERDUE' | 'SUSPENDED' | 'EXPIRED';
  completionPercentage: number;
  currentStageId?: string;
  currentStageName?: string;
  currentStageSequence?: number;
  enrolledAt: string;
  completedAt?: string;
  dueDate: string;
  stageProgress?: StageProgress[];
}

export interface StageProgress {
  id: string;
  stageDefinitionId: string;
  name: string;
  description: string;
  sequenceOrder: number;
  status: 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED' | 'SKIPPED' | 'OVERDUE' | 'SUSPENDED';
  completionPercentage: number;
  estimatedDurationHours: number;
  isMandatory: boolean;
  startedAt?: string;
  completedAt?: string;
  dueDate: string;
}

export interface CertificationFilter {
  employeeIds?: string[];
  certificationDefinitionIds?: string[];
  statuses?: ('NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED' | 'OVERDUE' | 'SUSPENDED' | 'EXPIRED')[];
  startDate?: string;
  endDate?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: 'ASC' | 'DESC';
}

export interface CertificationFilterResponse {
  certifications: Certification[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
}

export interface ReportRequest {
  employeeIds: string[]; // UUIDs as strings
  reportType: string;
  certificationIds?: string[];
  startDate?: string;
  endDate?: string;
}

export interface Report {
  id: string;
  name: string; // Changed from fileName to match backend
  type: string;
  status: 'QUEUED' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED'; // Updated to match backend
  parameters?: string;
  filePath?: string;
  pageCount?: number;
  createdAt: string;
  startedAt?: string;
  completedAt?: string;
  errorMessage?: string;
}

export interface ApiResponse<T> {
  data: T;
  message?: string;
  error?: string;
}
