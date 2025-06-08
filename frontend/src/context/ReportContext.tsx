import React, { createContext, useContext, useState, useCallback } from 'react';
import type { ReactNode } from 'react';
import type { Employee, Report, ReportRequest } from '../types';
import { apiClient } from '../services/api';

interface ReportContextType {
  employees: Employee[];
  reports: Report[];
  selectedEmployees: Employee[];
  loading: boolean;
  error: string | null;
  
  // Actions
  loadEmployees: () => Promise<void>;
  loadReports: () => Promise<void>;  selectEmployee: (employee: Employee) => void;
  deselectEmployee: (employeeId: string) => void;
  selectAllEmployees: () => void;
  clearSelection: () => void;
  generateReport: (reportType: string) => Promise<void>;
  refreshReportStatus: (reportId: string) => Promise<void>;
  downloadReport: (reportId: string) => Promise<void>;
  clearError: () => void;
}

const ReportContext = createContext<ReportContextType | undefined>(undefined);

export const useReportContext = () => {
  const context = useContext(ReportContext);
  if (!context) {
    throw new Error('useReportContext must be used within a ReportProvider');
  }
  return context;
};

interface ReportProviderProps {
  children: ReactNode;
}

export const ReportProvider: React.FC<ReportProviderProps> = ({ children }) => {
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [reports, setReports] = useState<Report[]>([]);
  const [selectedEmployees, setSelectedEmployees] = useState<Employee[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const clearError = useCallback(() => {
    setError(null);
  }, []);

  const loadEmployees = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await apiClient.getEmployees();
      setEmployees(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load employees');
    } finally {
      setLoading(false);
    }
  }, []);

  const loadReports = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await apiClient.getReports();
      setReports(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load reports');
    } finally {
      setLoading(false);
    }
  }, []);

  const selectEmployee = useCallback((employee: Employee) => {
    setSelectedEmployees(prev => {
      if (prev.find(emp => emp.id === employee.id)) {
        return prev;
      }
      return [...prev, employee];
    });
  }, []);
  const deselectEmployee = useCallback((employeeId: string) => {
    setSelectedEmployees(prev => prev.filter(emp => emp.id !== employeeId));
  }, []);

  const selectAllEmployees = useCallback(() => {
    setSelectedEmployees(employees);
  }, [employees]);

  const clearSelection = useCallback(() => {
    setSelectedEmployees([]);
  }, []);

  const generateReport = useCallback(async (reportType: string) => {
    if (selectedEmployees.length === 0) {
      setError('Please select at least one employee');
      return;
    }

    try {
      setLoading(true);
      setError(null);
      const request: ReportRequest = {
        employeeIds: selectedEmployees.map(emp => emp.id),
        reportType
      };
      const newReport = await apiClient.generateReport(request);
      setReports(prev => [newReport, ...prev]);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to generate report');
    } finally {
      setLoading(false);
    }
  }, [selectedEmployees]);

  const refreshReportStatus = useCallback(async (reportId: string) => {
    try {
      const updatedReport = await apiClient.getReport(reportId);
      setReports(prev => prev.map(report => 
        report.id === reportId ? updatedReport : report
      ));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to refresh report status');
    }
  }, []);

  const downloadReport = useCallback(async (reportId: string) => {
    try {
      setLoading(true);
      const blob = await apiClient.downloadReport(reportId);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `report-${reportId}.pdf`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to download report');
    } finally {
      setLoading(false);
    }
  }, []);

  const value: ReportContextType = {
    employees,
    reports,
    selectedEmployees,
    loading,
    error,
    loadEmployees,
    loadReports,
    selectEmployee,
    deselectEmployee,
    selectAllEmployees,
    clearSelection,
    generateReport,
    refreshReportStatus,
    downloadReport,
    clearError,
  };

  return (
    <ReportContext.Provider value={value}>
      {children}
    </ReportContext.Provider>
  );
};
