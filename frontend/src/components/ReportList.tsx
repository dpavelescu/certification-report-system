import React, { useState, useEffect } from 'react';
import type { Report } from '../types';
import Button from './Button';
import LoadingSpinner from './LoadingSpinner';

interface ReportListProps {
  reports: Report[];
  onRefreshStatus: (reportId: string) => void;
  onDownload: (reportId: string) => void;
  onDelete?: (reportId: string) => void;
  onRegenerate?: (reportId: string) => void;
}

const ReportList: React.FC<ReportListProps> = ({
  reports,
  onRefreshStatus,
  onDownload,
  onDelete,
  onRegenerate
}) => {
  const [deletingReports, setDeletingReports] = useState<Set<string>>(new Set());
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(5);
  // Reset to first page when reports change
  useEffect(() => {
    setCurrentPage(1);
  }, [reports.length]);

  // Calculate pagination
  const totalPages = Math.ceil(reports.length / pageSize);
  const startIndex = (currentPage - 1) * pageSize;
  const endIndex = startIndex + pageSize;
  const paginatedReports = reports.slice(startIndex, endIndex);const getStatusBadge = (status: Report['status']) => {
    const statusClasses = {
      QUEUED: 'bg-yellow-100 text-yellow-800',
      IN_PROGRESS: 'bg-blue-100 text-blue-800',
      COMPLETED: 'bg-green-100 text-green-800',
      FAILED: 'bg-red-100 text-red-800'
    };

    return (
      <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${statusClasses[status]}`}>
        {(status === 'QUEUED' || status === 'IN_PROGRESS') && <LoadingSpinner size="sm" className="mr-1" />}
        {status}
      </span>
    );
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString();
  };

  if (reports.length === 0) {
    return (
      <div className="bg-white shadow-sm rounded-lg p-6">
        <h3 className="text-lg font-medium text-gray-900 mb-4">Recent Reports</h3>
        <div className="text-center py-8">
          <div className="text-gray-500">No reports generated yet</div>
          <div className="text-sm text-gray-400 mt-1">
            Select employees and generate your first report
          </div>
        </div>
      </div>
    );
  }

  return (    <div className="bg-white shadow-sm rounded-lg overflow-hidden">
      <div className="px-4 py-3 border-b border-gray-200 flex justify-between items-center">
        <h3 className="text-lg font-medium text-gray-900">
          Recent Reports ({reports.length})
        </h3>
        {reports.length > 5 && (
          <div className="flex items-center space-x-2">
            <select
              value={pageSize}
              onChange={(e) => setPageSize(Number(e.target.value))}
              className="text-sm border border-gray-300 rounded-md px-2 py-1"
            >
              <option value={5}>5 per page</option>
              <option value={10}>10 per page</option>
              <option value={20}>20 per page</option>
            </select>
          </div>
        )}
      </div>
      
      <div className="divide-y divide-gray-200">
        {paginatedReports.map((report) => (
          <div key={report.id} className="px-4 py-4">
            <div className="flex items-center justify-between">
              <div className="flex-1 min-w-0">                <div className="flex items-center space-x-3">
                  <h4 className="text-sm font-medium text-gray-900 truncate">
                    {report.name}
                  </h4>
                  {getStatusBadge(report.status)}
                </div>
                <div className="mt-1 flex items-center space-x-4 text-sm text-gray-500">
                  <span>{report.type}</span>
                  <span>•</span>
                  <span>{formatDate(report.createdAt)}</span>
                  {report.errorMessage && (
                    <>
                      <span>•</span>
                      <span className="text-red-600">{report.errorMessage}</span>
                    </>
                  )}
                </div>
              </div>              <div className="flex items-center space-x-2 ml-4">
                {(report.status === 'QUEUED' || report.status === 'IN_PROGRESS') && (
                  <Button
                    variant="secondary"
                    onClick={() => onRefreshStatus(report.id)}
                    className="text-xs px-3 py-1 min-w-[80px]"
                  >
                    Refresh
                  </Button>
                )}
                
                {report.status === 'COMPLETED' && (
                  <Button
                    variant="primary"
                    onClick={() => onDownload(report.id)}
                    className="text-xs px-3 py-1 min-w-[80px]"
                  >
                    Download PDF
                  </Button>
                )}
                  {(report.status === 'FAILED' || report.status === 'QUEUED') && onDelete && (
                  <Button
                    variant="secondary"
                    onClick={async () => {
                      setDeletingReports(prev => new Set(prev).add(report.id));
                      try {
                        await onDelete(report.id);
                      } finally {
                        setDeletingReports(prev => {
                          const newSet = new Set(prev);
                          newSet.delete(report.id);
                          return newSet;
                        });
                      }
                    }}
                    disabled={deletingReports.has(report.id)}
                    className="text-xs px-3 py-1 min-w-[80px] text-red-600 hover:text-red-800"
                  >
                    {deletingReports.has(report.id) ? 'Deleting...' : 'Delete'}
                  </Button>
                )}
                
                {report.status === 'FAILED' && onRegenerate && (
                  <Button
                    variant="primary"
                    onClick={() => onRegenerate(report.id)}
                    className="text-xs px-3 py-1 min-w-[90px]"
                  >
                    Regenerate
                  </Button>
                )}
                
                {report.status === 'FAILED' && !onDelete && !onRegenerate && (
                  <span className="text-sm text-red-600">
                    Generation failed
                  </span>
                )}
              </div>
            </div>
          </div>        ))}
      </div>
      
      {/* Pagination Controls */}
      {totalPages > 1 && (
        <div className="px-4 py-3 border-t border-gray-200 flex justify-between items-center">
          <div className="text-sm text-gray-700">
            Showing {startIndex + 1} to {Math.min(endIndex, reports.length)} of {reports.length} reports
          </div>          <div className="flex items-center space-x-2">
            <Button
              variant="secondary"
              onClick={() => setCurrentPage(Math.max(1, currentPage - 1))}
              disabled={currentPage === 1}
              className="text-xs px-3 py-1 min-w-[70px]"
            >
              Previous
            </Button>
            <span className="text-sm text-gray-700">
              Page {currentPage} of {totalPages}
            </span>
            <Button
              variant="secondary"
              onClick={() => setCurrentPage(Math.min(totalPages, currentPage + 1))}
              disabled={currentPage === totalPages}
              className="text-xs px-3 py-1 min-w-[70px]"
            >
              Next
            </Button>
          </div>
        </div>
      )}
    </div>
  );
};

export default ReportList;
