// API Client using native fetch - no Axios as per specifications

import type { Employee, Report, ReportRequest, CertificationDefinition, CertificationFilter, CertificationFilterResponse } from '../types';

const API_BASE_URL = 'http://localhost:8080/api';

class ApiClient {
  private async fetchWithErrorHandling<T>(url: string, options?: RequestInit): Promise<T> {
    try {
      const response = await fetch(`${API_BASE_URL}${url}`, {
        headers: {
          'Content-Type': 'application/json',
          ...options?.headers,
        },
        ...options,
      });

      if (!response.ok) {
        const errorData = await response.text();
        throw new Error(`HTTP ${response.status}: ${errorData}`);
      }

      return await response.json();
    } catch (error) {
      console.error(`API Error for ${url}:`, error);
      throw error;
    }
  }
  // Employee endpoints
  async getEmployees(): Promise<Employee[]> {
    return this.fetchWithErrorHandling<Employee[]>('/employees');
  }

  // Report endpoints
  async getReports(): Promise<Report[]> {
    return this.fetchWithErrorHandling<Report[]>('/reports');
  }
  async generateReport(request: ReportRequest): Promise<Report> {
    return this.fetchWithErrorHandling<Report>('/reports/generate', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }  async getReport(id: string): Promise<Report> {
    return this.fetchWithErrorHandling<Report>(`/reports/${id}`);
  }async downloadReport(id: string): Promise<Blob> {
    const response = await fetch(`${API_BASE_URL}/reports/${id}/download`);    if (!response.ok) {
      // Try to parse error response as JSON
      try {
        const errorData = await response.json();
        const errorMessage = errorData.error || `Download failed: ${response.statusText}`;
        throw new Error(errorMessage);
      } catch {
        // If JSON parsing fails, use the status text
        throw new Error(`Download failed: ${response.statusText}`);
      }
    }
    return response.blob();
  }

  async deleteReport(id: string): Promise<void> {
    return this.fetchWithErrorHandling<void>(`/reports/${id}`, {
      method: 'DELETE',
    });
  }

  async cleanupStuckReports(): Promise<{ message: string; cleanedReports: number }> {
    return this.fetchWithErrorHandling<{ message: string; cleanedReports: number }>('/reports/cleanup/stuck', {
      method: 'POST',
    });
  }
  async getReportStats(): Promise<Record<string, unknown>> {
    return this.fetchWithErrorHandling<Record<string, unknown>>('/reports/stats');
  }

  async getPerformanceMetrics(): Promise<Record<string, unknown>> {
    return this.fetchWithErrorHandling<Record<string, unknown>>('/metrics/performance');
  }

  async getHealthStatus(): Promise<Record<string, unknown>> {
    return this.fetchWithErrorHandling<Record<string, unknown>>('/metrics/health');
  }

  // Certification endpoints
  async getCertificationDefinitions(): Promise<CertificationDefinition[]> {
    return this.fetchWithErrorHandling<CertificationDefinition[]>('/certifications/definitions');
  }

  async filterCertifications(filter: CertificationFilter): Promise<CertificationFilterResponse> {
    return this.fetchWithErrorHandling<CertificationFilterResponse>('/certifications/filter', {
      method: 'POST',
      body: JSON.stringify(filter),
    });
  }

  async getAvailableCertificationsForEmployees(employeeIds: string[]): Promise<CertificationDefinition[]> {
    const queryParams = employeeIds.map(id => `employeeIds=${id}`).join('&');
    return this.fetchWithErrorHandling<CertificationDefinition[]>(`/certifications/available?${queryParams}`);
  }

  async getEmployeesByDepartment(department: string): Promise<Employee[]> {
    return this.fetchWithErrorHandling<Employee[]>(`/employees/department/${encodeURIComponent(department)}`);
  }

  async searchEmployees(searchTerm: string): Promise<Employee[]> {
    return this.fetchWithErrorHandling<Employee[]>(`/employees/search?q=${encodeURIComponent(searchTerm)}`);
  }

  async getDepartments(): Promise<string[]> {
    return this.fetchWithErrorHandling<string[]>('/employees/departments');
  }
}

export const apiClient = new ApiClient();
