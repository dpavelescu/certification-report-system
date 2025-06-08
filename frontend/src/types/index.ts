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

export interface ReportRequest {
  employeeIds: string[]; // UUIDs as strings
  reportType: string;
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
