// API Client using native fetch - no Axios as per specifications

import type { Employee, Report, ReportRequest } from '../types';

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

  async getEmployee(id: number): Promise<Employee> {
    return this.fetchWithErrorHandling<Employee>(`/employees/${id}`);
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
  }

  async getReport(id: string): Promise<Report> {
    return this.fetchWithErrorHandling<Report>(`/reports/${id}`);
  }

  async downloadReport(id: string): Promise<Blob> {
    const response = await fetch(`${API_BASE_URL}/reports/${id}/download`);
    if (!response.ok) {
      throw new Error(`Download failed: ${response.statusText}`);
    }
    return response.blob();
  }
}

export const apiClient = new ApiClient();
