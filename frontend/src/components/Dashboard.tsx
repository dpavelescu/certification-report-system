import React, { useEffect } from 'react';
import { useReportContext } from '../context/ReportContext';
import EmployeeTable from './EmployeeTable';
import ReportGenerator from './ReportGenerator';
import ReportList from './ReportList';
import LoadingSpinner from './LoadingSpinner';

const Dashboard: React.FC = () => {
  const {
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
    refreshReportStatus,
    downloadReport,
    clearError
  } = useReportContext();

  useEffect(() => {
    loadEmployees();
    loadReports();
  }, [loadEmployees, loadReports]);

  if (loading && employees.length === 0) {
    return (
      <div className="flex items-center justify-center py-12">
        <div className="text-center">
          <LoadingSpinner size="lg" className="mx-auto mb-4" />
          <div className="text-gray-600">Loading employee data...</div>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {error && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4">
          <div className="flex items-center justify-between">
            <div className="flex">
              <div className="text-red-800">
                <svg className="w-5 h-5" viewBox="0 0 20 20" fill="currentColor">
                  <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
                </svg>
              </div>
              <div className="ml-3">
                <h3 className="text-sm font-medium text-red-800">Error</h3>
                <div className="mt-1 text-sm text-red-700">{error}</div>
              </div>
            </div>
            <button
              onClick={clearError}
              className="text-red-400 hover:text-red-600"
            >
              <svg className="w-5 h-5" viewBox="0 0 20 20" fill="currentColor">
                <path fillRule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clipRule="evenodd" />
              </svg>
            </button>
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Employee Selection - Left Column */}
        <div className="lg:col-span-2">
          <EmployeeTable
            employees={employees}
            selectedEmployees={selectedEmployees}
            onEmployeeSelect={selectEmployee}
            onEmployeeDeselect={deselectEmployee}
            onSelectAll={selectAllEmployees}
            onClearSelection={clearSelection}
          />
        </div>

        {/* Report Generation - Right Column */}
        <div className="space-y-6">
          <ReportGenerator />
          
          <ReportList
            reports={reports}
            onRefreshStatus={refreshReportStatus}
            onDownload={downloadReport}
          />
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
