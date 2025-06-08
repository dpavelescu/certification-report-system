import React, { useState } from 'react';
import { useReportContext } from '../context/ReportContext';
import Button from './Button';
import LoadingSpinner from './LoadingSpinner';

const ReportGenerator: React.FC = () => {
  const { selectedEmployees, generateReport, loading } = useReportContext();
  const [reportType] = useState('employee_demographics'); // Fixed for Iteration 1

  const handleGenerateReport = () => {
    generateReport(reportType);
  };

  const isDisabled = selectedEmployees.length === 0 || loading;

  return (
    <div className="bg-white shadow-sm rounded-lg p-6">
      <h3 className="text-lg font-medium text-gray-900 mb-4">
        Generate Report
      </h3>
      
      <div className="space-y-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Report Type
          </label>
          <div className="form-select-wrapper">
            <select 
              value={reportType}
              disabled={true}
              className="form-select opacity-60 cursor-not-allowed"
            >
              <option value="employee_demographics">Employee Demographics</option>
            </select>
          </div>
          <p className="mt-1 text-xs text-gray-500">
            Only Employee Demographics available in Iteration 1
          </p>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Selected Employees
          </label>
          <div className="text-sm text-gray-600">
            {selectedEmployees.length === 0 ? (
              <span className="text-gray-400">No employees selected</span>
            ) : (
              <span>
                {selectedEmployees.length} employee{selectedEmployees.length !== 1 ? 's' : ''} selected
              </span>
            )}
          </div>
          {selectedEmployees.length > 0 && (
            <div className="mt-2 max-h-32 overflow-y-auto">
              <div className="space-y-1">
                {selectedEmployees.map((employee) => (
                  <div key={employee.id} className="text-xs text-gray-600 bg-gray-50 px-2 py-1 rounded">
                    {employee.firstName} {employee.lastName} ({employee.department})
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        <div className="pt-4 border-t border-gray-200">
          <Button
            onClick={handleGenerateReport}
            disabled={isDisabled}
            className="w-full flex items-center justify-center"
          >
            {loading ? (
              <>
                <LoadingSpinner size="sm" className="mr-2" />
                Generating Report...
              </>
            ) : (
              'Generate PDF Report'
            )}
          </Button>
          
          {selectedEmployees.length === 0 && (
            <p className="mt-2 text-xs text-gray-500 text-center">
              Please select at least one employee to generate a report
            </p>
          )}
        </div>
      </div>
    </div>
  );
};

export default ReportGenerator;
