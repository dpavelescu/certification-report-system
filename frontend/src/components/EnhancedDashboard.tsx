import React, { useState, useEffect, useCallback } from 'react';
import { useReportContext } from '../context/ReportContext';
import type { CertificationDefinition } from '../types';
import LoadingSpinner from './LoadingSpinner';
import ReportList from './ReportList';
import { apiClient } from '../services/api';
import { createDateRange, getDaysBetween, formatDateDisplay } from '../utils/dateUtils';
import { calculatePagination } from '../utils/paginationUtils';

const EnhancedDashboard: React.FC = () => {
  const {
    employees,
    reports,
    loading,
    loadEmployees,
    loadReports,
    generateReport,
    refreshReportStatus,
    downloadReport,
    deleteReport,
    regenerateReport,
    cleanupStuckReports
  } = useReportContext();

  const [selectedEmployees, setSelectedEmployees] = useState<string[]>([]);
  const [selectedCertifications, setSelectedCertifications] = useState<string[]>([]);
  const [availableCertifications, setAvailableCertifications] = useState<CertificationDefinition[]>([]);
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');

  // Pagination state for certifications
  const [certificationPage, setCertificationPage] = useState(1);
  const [certificationsPerPage, setCertificationsPerPage] = useState(30);
  
  // Pagination state for employees
  const [employeePage, setEmployeePage] = useState(1);
  const [employeesPerPage, setEmployeesPerPage] = useState(30);
  
  // Pagination constants
  const EMPLOYEES_PER_PAGE_OPTIONS = [30, 50, 100];
  const CERTIFICATIONS_PER_PAGE_OPTIONS = [30, 50, 100];

  // Helper function to set date range using utility
  const setDateRange = (daysAgo: number) => {
    const { startDate, endDate } = createDateRange(daysAgo);
    setStartDate(startDate);
    setEndDate(endDate);
  };  // Calculate paginated data using utility
  const certificationPagination = calculatePagination(availableCertifications, certificationPage, certificationsPerPage);
  const employeePagination = calculatePagination(employees, employeePage, employeesPerPage);

  const {
    totalPages: totalCertificationPages,
    paginatedItems: paginatedCertifications
  } = certificationPagination;

  const {
    totalPages: totalPages,
    paginatedItems: paginatedEmployees
  } = employeePagination;

  const loadAvailableCertifications = useCallback(async () => {
    if (selectedEmployees.length === 0) {
      // Load all certifications when no employees selected
      try {
        const allCerts = await apiClient.getCertificationDefinitions();
        setAvailableCertifications(allCerts);
        setSelectedCertifications([]);
      } catch (error) {
        console.error('Failed to load certifications:', error);
      }
      return;
    }

    try {
      const availableCerts = await apiClient.getAvailableCertificationsForEmployees(selectedEmployees);
      setAvailableCertifications(availableCerts);
      // Remove selected certifications that are no longer available
      setSelectedCertifications(prev => 
        prev.filter(certId => availableCerts.some(cert => cert.id === certId))
      );
    } catch (error) {
      console.error('Failed to load available certifications:', error);
    }
  }, [selectedEmployees]);

  useEffect(() => {
    loadEmployees();
    loadReports();
    loadAvailableCertifications();
  }, [loadEmployees, loadReports, loadAvailableCertifications]);

  // Load available certifications when employees change
  useEffect(() => {
    loadAvailableCertifications();
    setCertificationPage(1); // Reset to first page when employees change
  }, [loadAvailableCertifications]);
  const handleEmployeeSelection = (employeeId: string) => {
    setSelectedEmployees(prev => 
      prev.includes(employeeId) 
        ? prev.filter(id => id !== employeeId)
        : [...prev, employeeId]
    );
  };

  const handleCertificationSelection = (certificationId: string) => {
    setSelectedCertifications(prev => 
      prev.includes(certificationId) 
        ? prev.filter(id => id !== certificationId)
        : [...prev, certificationId]
    );
  };

  const handleSelectAllEmployees = () => {
    if (selectedEmployees.length === employees.length) {
      setSelectedEmployees([]);
    } else {
      setSelectedEmployees(employees.map(emp => emp.id));
    }
  };

  const handleSelectAllCertifications = () => {
    if (selectedCertifications.length === availableCertifications.length) {
      setSelectedCertifications([]);
    } else {
      setSelectedCertifications(availableCertifications.map((cert: CertificationDefinition) => cert.id));
    }
  };

  const handleClearEmployees = () => {
    setSelectedEmployees([]);
  };

  const handleClearCertifications = () => {
    setSelectedCertifications([]);
  };
  const handleGenerateReport = async () => {
    await generateReport('CERTIFICATION', {
      employeeIds: selectedEmployees,
      certificationIds: selectedCertifications.length > 0 ? selectedCertifications : undefined,
      startDate: startDate || undefined,
      endDate: endDate || undefined,
    });
  };

  const hasValidSelection = selectedEmployees.length > 0;
  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-blue-50 to-indigo-50 p-4">
      <div className="max-w-7xl mx-auto space-y-4">
        {/* Date Range - Full Width Top Row */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
          <div className="bg-gradient-to-r from-purple-50 to-violet-50 px-4 py-3 border-b border-gray-200">
            <div className="flex justify-between items-center">
              <div>
                <h3 className="text-base font-semibold text-gray-900">Date Range Filter</h3>
                <p className="text-xs text-gray-600 mt-0.5">Optional: Filter by certification dates</p>
              </div>
              {(startDate || endDate) && (
                <button
                  onClick={() => {
                    setStartDate('');
                    setEndDate('');
                  }}
                  className="px-3 py-1 text-xs font-medium text-gray-700 bg-gray-100 rounded-md hover:bg-gray-200 transition-colors duration-200"
                >
                  Clear Dates
                </button>
              )}
            </div>
          </div>
            <div className="p-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Start Date
                </label>
                <input
                  type="date"
                  value={startDate}
                  onChange={(e) => setStartDate(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-purple-500 focus:border-transparent transition-colors"
                  placeholder="Select start date"
                />                {startDate && (
                  <p className="text-xs text-gray-500 mt-1">
                    From: {formatDateDisplay(startDate)}
                  </p>
                )}
              </div>
              
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  End Date
                </label>
                <input
                  type="date"
                  value={endDate}
                  onChange={(e) => setEndDate(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-purple-500 focus:border-transparent transition-colors"
                  placeholder="Select end date"
                />                {endDate && (
                  <p className="text-xs text-gray-500 mt-1">
                    Until: {formatDateDisplay(endDate)}
                  </p>
                )}
              </div>
            </div>

            {/* Quick Date Range Presets */}
            {(startDate || endDate) && (
              <div className="mt-4 pt-4 border-t border-gray-200">
                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-3">
                    <span className="text-sm font-medium text-gray-700">Quick actions:</span>                    <button
                      onClick={() => setDateRange(30)}
                      className="px-2 py-1 text-xs font-medium text-purple-700 bg-purple-100 rounded-md hover:bg-purple-200 transition-colors duration-200"
                    >
                      Last 30 days
                    </button>
                    <button
                      onClick={() => setDateRange(90)}
                      className="px-2 py-1 text-xs font-medium text-purple-700 bg-purple-100 rounded-md hover:bg-purple-200 transition-colors duration-200"
                    >
                      Last 90 days
                    </button>
                  </div>                  <div className="text-xs text-gray-600">
                    {startDate && endDate && (
                      <span className="font-medium text-purple-700">
                        {getDaysBetween(startDate, endDate)} days selected
                      </span>
                    )}
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>        {/* Employee and Certification Filters - Same Row */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          {/* Employee Selection */}
          <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden h-[450px] flex flex-col">
            <div className="bg-gradient-to-r from-blue-50 to-cyan-50 px-4 py-3 border-b border-gray-200 flex-shrink-0">
              <div className="flex justify-between items-center">
                <div>
                  <h3 className="text-base font-semibold text-gray-900">
                    Select Employees ({selectedEmployees.length} selected)
                  </h3>                  <p className="text-xs text-gray-600 mt-0.5">
                    Choose which employees to include in the report
                  </p>
                </div>
                <div className="flex space-x-2">
                  <button
                    onClick={handleSelectAllEmployees}
                    className="px-2 py-1 text-xs font-medium text-blue-700 bg-blue-100 rounded-md hover:bg-blue-200 transition-colors duration-200"
                  >
                    All ({employees.length})
                  </button>
                  <button
                    onClick={handleClearEmployees}
                    className="px-2 py-1 text-xs font-medium text-gray-700 bg-gray-100 rounded-md hover:bg-gray-200 transition-colors duration-200"
                  >
                    Clear
                  </button>
                </div>
              </div>
            </div>            {/* Employee List with Fixed Height for Exactly 5 Items */}
            <div className="flex-1 p-3 min-h-0">              <div className="h-[312px] overflow-y-auto space-y-2">
                {/* Show all items from current page, with minimum of 5 slots */}
                {paginatedEmployees.length > 0 ? (
                  paginatedEmployees.map((employee) => (
                    <div
                      key={employee.id}
                      onClick={() => handleEmployeeSelection(employee.id)}
                      className={`p-3 rounded-lg border transition-all duration-200 cursor-pointer hover:shadow-sm h-[56px] flex items-center ${
                        selectedEmployees.includes(employee.id)
                          ? 'border-blue-300 bg-blue-50 shadow-sm'
                          : 'border-gray-200 bg-white hover:border-gray-300'
                      }`}
                    >
                      <div className="flex items-center justify-between w-full">
                        <div className="flex-1 min-w-0">
                          <h4 className="font-semibold text-gray-900 text-sm truncate">
                            {employee.firstName} {employee.lastName}
                          </h4>
                          <p className="text-xs text-gray-600 truncate">{employee.department}</p>
                          <p className="text-xs text-gray-500 truncate">{employee.email}</p>
                        </div>
                        <div className={`w-4 h-4 rounded-full border-2 flex items-center justify-center transition-colors duration-200 flex-shrink-0 ml-3 ${
                          selectedEmployees.includes(employee.id)
                            ? 'border-blue-500 bg-blue-500'
                            : 'border-gray-300'
                        }`}>
                          {selectedEmployees.includes(employee.id) && (
                            <svg className="w-2.5 h-2.5 text-white" fill="currentColor" viewBox="0 0 20 20">
                              <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                            </svg>
                          )}
                        </div>
                      </div>
                    </div>
                  ))                ) : (
                  // Show placeholder when no employees
                  Array.from({ length: 5 }, (_, index) => (
                    <div
                      key={`placeholder-${index}`}
                      className="h-[56px] rounded-lg border border-dashed border-gray-200 bg-gray-50/50 flex items-center justify-center"
                    >
                      <span className="text-xs text-gray-400">—</span>
                    </div>
                  ))
                )}
              </div>
            </div>{/* Pagination Controls - Always show if there are employees */}
            {employees.length > 0 && (
              <div className="px-4 py-2 border-t border-gray-200 flex items-center justify-between bg-gray-50 flex-shrink-0">                <div className="flex items-center space-x-3">                  <div className="text-xs text-gray-600">
                    Page {employeePage} of {totalPages} • Showing {paginatedEmployees.length} items
                  </div>
                  <div className="flex items-center space-x-1">
                    <span className="text-xs text-gray-600">Show:</span>
                    <select
                      value={employeesPerPage}
                      onChange={(e) => {
                        setEmployeesPerPage(Number(e.target.value));
                        setEmployeePage(1); // Reset to first page when changing page size
                      }}
                      className="text-xs border border-gray-300 rounded px-1 py-0.5 bg-white focus:ring-1 focus:ring-blue-500 focus:border-blue-500"
                    >
                      {EMPLOYEES_PER_PAGE_OPTIONS.map(option => (
                        <option key={option} value={option}>{option}</option>
                      ))}
                    </select>
                  </div>
                </div>
                <div className="flex items-center space-x-1">
                  <button
                    onClick={() => setEmployeePage(Math.max(1, employeePage - 1))}
                    disabled={employeePage === 1}
                    className={`px-2 py-1 text-xs rounded ${
                      employeePage === 1 
                        ? 'text-gray-400 cursor-not-allowed' 
                        : 'text-blue-600 hover:bg-blue-50'
                    }`}
                  >
                    ‹
                  </button>
                  <span className="text-xs text-gray-600">
                    {employeePage} of {totalPages}
                  </span>
                  <button
                    onClick={() => setEmployeePage(Math.min(totalPages, employeePage + 1))}
                    disabled={employeePage === totalPages}
                    className={`px-2 py-1 text-xs rounded ${
                      employeePage === totalPages 
                        ? 'text-gray-400 cursor-not-allowed' 
                        : 'text-blue-600 hover:bg-blue-50'
                    }`}
                  >
                    ›
                  </button>
                </div>
              </div>
            )}
          </div>          {/* Certifications Selection */}
          <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden h-[450px] flex flex-col">
            <div className="bg-gradient-to-r from-green-50 to-emerald-50 px-4 py-3 border-b border-gray-200 flex-shrink-0">
              <div className="flex justify-between items-center">
                <div>
                  <h3 className="text-base font-semibold text-gray-900">
                    Certifications ({selectedCertifications.length})
                  </h3>
                  <p className="text-xs text-gray-600 mt-0.5">
                    {selectedEmployees.length > 0 ? 'Available for selected employees' : 'Select employees first'}
                  </p>
                </div>
                <div className="flex space-x-1">
                  <button
                    onClick={handleSelectAllCertifications}
                    disabled={availableCertifications.length === 0}
                    className={`px-2 py-1 text-xs font-medium rounded-md transition-colors duration-200 ${
                      availableCertifications.length > 0
                        ? 'text-green-700 bg-green-100 hover:bg-green-200'
                        : 'text-gray-400 bg-gray-100 cursor-not-allowed'
                    }`}
                  >
                    All ({availableCertifications.length})
                  </button>
                  <button
                    onClick={handleClearCertifications}
                    className="px-2 py-1 text-xs font-medium text-gray-700 bg-gray-100 rounded-md hover:bg-gray-200 transition-colors duration-200"
                  >
                    Clear
                  </button>
                </div>
              </div>
            </div>            {/* Certifications List with Fixed Height for Exactly 5 Items */}
            <div className="flex-1 p-3 min-h-0">              <div className="h-[312px] overflow-y-auto space-y-2">
                {/* Show all certifications from current page, with minimum display */}
                {paginatedCertifications.length > 0 ? (
                  paginatedCertifications.map((certification) => (
                    <div
                      key={certification.id}
                      onClick={() => handleCertificationSelection(certification.id)}
                      className={`p-3 rounded-lg border transition-all duration-200 cursor-pointer hover:shadow-sm h-[56px] flex items-center ${
                        selectedCertifications.includes(certification.id)
                          ? 'border-green-300 bg-green-50 shadow-sm'
                          : 'border-gray-200 bg-white hover:border-gray-300'
                      }`}
                    >
                      <div className="flex items-center justify-between w-full">
                        <div className="flex-1 min-w-0">
                          <h4 className="font-semibold text-gray-900 text-sm truncate">{certification.name}</h4>
                          <p className="text-xs text-gray-600 truncate">{certification.category}</p>
                          <p className="text-xs text-gray-500 truncate">Certification</p>
                        </div>
                        <div className={`w-4 h-4 rounded-full border-2 flex items-center justify-center transition-colors duration-200 flex-shrink-0 ml-3 ${
                          selectedCertifications.includes(certification.id)
                            ? 'border-green-500 bg-green-500'
                            : 'border-gray-300'
                        }`}>
                          {selectedCertifications.includes(certification.id) && (
                            <svg className="w-2.5 h-2.5 text-white" fill="currentColor" viewBox="0 0 20 20">
                              <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                            </svg>
                          )}
                        </div>
                      </div>
                    </div>
                  ))                ) : (
                  // Show placeholder when no certifications or no employees selected
                  Array.from({ length: 5 }, (_, index) => (
                    <div
                      key={`cert-placeholder-${index}`}
                      className="h-[56px] rounded-lg border border-dashed border-gray-200 bg-gray-50/50 flex items-center justify-center"
                    >
                      {selectedEmployees.length === 0 ? (
                        <span className="text-xs text-gray-400">Select employees first</span>
                      ) : (
                        <span className="text-xs text-gray-400">—</span>
                      )}
                    </div>
                  ))
                )}
              </div>
            </div>            {/* Pagination Controls for Certifications - Show if there are certifications */}
            {availableCertifications.length > 0 && (
              <div className="px-4 py-2 border-t border-gray-200 flex items-center justify-between bg-gray-50 flex-shrink-0">
                <div className="flex items-center space-x-3">                  <div className="text-xs text-gray-600">
                    Page {certificationPage} of {totalCertificationPages} • Showing {paginatedCertifications.length} items
                  </div>
                  <div className="flex items-center space-x-1">
                    <span className="text-xs text-gray-600">Show:</span>
                    <select
                      value={certificationsPerPage}
                      onChange={(e) => {
                        setCertificationsPerPage(Number(e.target.value));
                        setCertificationPage(1); // Reset to first page when changing page size
                      }}
                      className="text-xs border border-gray-300 rounded px-1 py-0.5 bg-white focus:ring-1 focus:ring-green-500 focus:border-green-500"
                    >
                      {CERTIFICATIONS_PER_PAGE_OPTIONS.map(option => (
                        <option key={option} value={option}>{option}</option>
                      ))}
                    </select>
                  </div>
                </div>
                <div className="flex items-center space-x-1">
                  <button
                    onClick={() => setCertificationPage(Math.max(1, certificationPage - 1))}
                    disabled={certificationPage === 1}
                    className={`px-2 py-1 text-xs rounded ${
                      certificationPage === 1 
                        ? 'text-gray-400 cursor-not-allowed' 
                        : 'text-green-600 hover:bg-green-50'
                    }`}
                  >
                    ‹
                  </button>
                  <span className="text-xs text-gray-600">
                    {certificationPage} of {totalCertificationPages}
                  </span>
                  <button
                    onClick={() => setCertificationPage(Math.min(totalCertificationPages, certificationPage + 1))}
                    disabled={certificationPage === totalCertificationPages}
                    className={`px-2 py-1 text-xs rounded ${
                      certificationPage === totalCertificationPages 
                        ? 'text-gray-400 cursor-not-allowed' 
                        : 'text-green-600 hover:bg-green-50'
                    }`}
                  >
                    ›
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>{/* Generate Report Button - Full Width */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
          <div className="bg-gradient-to-r from-indigo-50 to-blue-50 px-6 py-4 border-b border-gray-200">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="text-lg font-semibold text-gray-900">Generate Report</h3>
                <p className="text-sm text-gray-600 mt-1">
                  Create your certification report with the selected filters
                </p>
              </div>
              <div className="flex items-center space-x-4 text-sm text-gray-600">
                <span className="flex items-center">
                  <span className="w-2 h-2 bg-blue-500 rounded-full mr-2"></span>
                  {selectedEmployees.length} employee{selectedEmployees.length !== 1 ? 's' : ''} selected
                </span>
                <span className="flex items-center">
                  <span className="w-2 h-2 bg-green-500 rounded-full mr-2"></span>
                  {selectedCertifications.length} certification{selectedCertifications.length !== 1 ? 's' : ''} selected
                </span>
                {(startDate || endDate) && (
                  <span className="flex items-center">
                    <span className="w-2 h-2 bg-purple-500 rounded-full mr-2"></span>
                    Date filter active
                  </span>
                )}
              </div>
            </div>
          </div>
            <div className="p-6">
            <div className="flex items-center justify-center">
              <button
                onClick={handleGenerateReport}
                disabled={!hasValidSelection || loading}
                className={`inline-flex items-center justify-center px-8 py-4 border border-transparent rounded-lg text-base font-semibold transition-all duration-200 ${
                  hasValidSelection && !loading
                    ? 'text-white bg-gradient-to-r from-blue-600 to-blue-700 hover:from-blue-700 hover:to-blue-800 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 shadow-lg hover:shadow-xl transform hover:scale-105'
                    : 'text-gray-400 bg-gray-100 cursor-not-allowed'
                }`}
              >
                {loading ? (
                  <>
                    <LoadingSpinner size="sm" className="mr-3" />
                    Generating Report...
                  </>
                ) : (
                  <>
                    <svg className="w-5 h-5 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                    </svg>
                    Generate Certification Report
                  </>
                )}
              </button>
            </div>
          </div>
        </div>

        {/* Reports Management Section */}
        <div>
          <ReportList
            reports={reports}
            onRefreshStatus={refreshReportStatus}
            onDownload={downloadReport}
            onDelete={deleteReport}
            onRegenerate={regenerateReport}
            onCleanupStuckReports={cleanupStuckReports}
            onRefreshReports={loadReports}
            loading={loading}
          />
        </div>
      </div>
    </div>
  );
};

export default EnhancedDashboard;
