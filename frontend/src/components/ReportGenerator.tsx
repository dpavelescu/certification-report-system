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

  return (    <div className="bg-white shadow-sm rounded-lg p-4">
      <h3 className="text-lg font-medium text-gray-900 mb-3">
        Generate Report
      </h3>
      
      <div className="space-y-3">
        {/* Report Type - Simplified */}
        <div>
          <div className="text-sm font-medium text-gray-700 mb-1">Report Type</div>
          <div className="text-sm text-gray-600 bg-gray-50 px-3 py-2 rounded border">
            Employee Demographics
          </div>
        </div>

        {/* Selected Employees - Compact */}
        <div>
          <div className="text-sm font-medium text-gray-700 mb-1">Selected Employees</div>
          <div className="text-sm text-gray-600 bg-gray-50 px-3 py-2 rounded border">
            {selectedEmployees.length === 0 ? (
              <span className="text-gray-400">No employees selected</span>
            ) : (
              <span className="text-green-600 font-medium">
                {selectedEmployees.length} employee{selectedEmployees.length !== 1 ? 's' : ''} selected
              </span>
            )}
          </div>
        </div>

        {/* Generate Button */}
        <div className="pt-2">
          <Button
            onClick={handleGenerateReport}
            disabled={isDisabled}
            className="w-full flex items-center justify-center"
          >
            {loading ? (
              <>
                <LoadingSpinner size="sm" className="mr-2" />
                Generating...
              </>
            ) : (
              'Generate PDF Report'
            )}
          </Button>
          
          {selectedEmployees.length === 0 && (
            <p className="mt-2 text-xs text-gray-500 text-center">
              Select employees above to generate a report
            </p>
          )}
        </div>
      </div>
    </div>
  );
};

export default ReportGenerator;
