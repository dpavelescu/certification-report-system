import React, { useState, useEffect } from 'react';
import type { Report } from '../types';
import LoadingSpinner from './LoadingSpinner';

interface ReportListProps {
  reports: Report[];
  onRefreshStatus: (reportId: string) => void;
  onDownload: (reportId: string) => void;
  onDelete?: (reportId: string) => void;
  onRegenerate?: (reportId: string) => void;
  onCleanupStuckReports?: () => void;
  onRefreshReports?: () => void;
  loading?: boolean;
}

// Professional SVG Icons with larger default size
const DownloadIcon = ({ className = "w-5 h-5" }: { className?: string }) => (
  <svg className={className} fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
  </svg>
);

const DeleteIcon = ({ className = "w-5 h-5" }: { className?: string }) => (
  <svg className={className} fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
  </svg>
);

const RefreshIcon = ({ className = "w-4 h-4" }: { className?: string }) => (
  <svg className={className} fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
  </svg>
);

const RegenerateIcon = ({ className = "w-4 h-4" }: { className?: string }) => (
  <svg className={className} fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12" />
  </svg>
);

const DocumentIcon = ({ className = "w-5 h-5" }: { className?: string }) => (
  <svg className={className} fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
  </svg>
);

const ReportList: React.FC<ReportListProps> = ({
  reports,
  onRefreshStatus,
  onDownload,
  onDelete,
  onRegenerate,
  onCleanupStuckReports,
  onRefreshReports,
  loading: systemLoading
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
  const paginatedReports = reports.slice(startIndex, endIndex);
  const getStatusBadge = (status: Report['status']) => {
    // Simple, direct status styling without complex config objects
    let bgClass, textClass, icon;
    
    switch (status) {
      case 'QUEUED':
        bgClass = 'bg-amber-50 border-amber-200';
        textClass = 'text-amber-700';
        icon = '⏳';
        break;
      case 'IN_PROGRESS':
        bgClass = 'bg-blue-50 border-blue-200';
        textClass = 'text-blue-700';
        icon = '🔄';
        break;
      case 'COMPLETED':
        bgClass = 'bg-emerald-50 border-emerald-200';
        textClass = 'text-emerald-700';
        icon = '✅';
        break;
      case 'FAILED':
        bgClass = 'bg-red-50 border-red-200';
        textClass = 'text-red-700';
        icon = '❌';
        break;
      default:
        bgClass = 'bg-gray-50 border-gray-200';
        textClass = 'text-gray-700';
        icon = '❓';
    }    
    return (
      <div className={`inline-flex items-center px-3 py-1 rounded-full text-xs font-medium border ${bgClass} ${textClass}`}>
        <span className="mr-2">{icon}</span>
        {(status === 'QUEUED' || status === 'IN_PROGRESS') && <LoadingSpinner size="sm" className="mr-2" />}
        <span className="capitalize">{status.toLowerCase().replace('_', ' ')}</span>
      </div>
    );
  };
  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    const now = new Date();
    const diffInMs = now.getTime() - date.getTime();
    const diffInMinutes = Math.floor(diffInMs / (1000 * 60));
    const diffInHours = Math.floor(diffInMs / (1000 * 60 * 60));
    
    if (diffInMinutes < 1) {
      return 'Just now';
    } else if (diffInMinutes < 60) {
      return `${diffInMinutes}m ago`;
    } else if (diffInHours < 24) {
      return `${diffInHours}h ago`;
    } else if (diffInHours < 48) {
      return 'Yesterday';
    } else {
      return date.toLocaleDateString('en-US', {
        month: 'short',
        day: 'numeric',
        year: date.getFullYear() !== now.getFullYear() ? 'numeric' : undefined
      });
    }
  };

  const formatFileSize = (pageCount?: number) => {
    if (!pageCount) return '';
    const estimatedSizeKB = pageCount * 150; // Rough estimate
    if (estimatedSizeKB < 1024) {
      return `~${estimatedSizeKB}KB`;
    } else {
      return `~${(estimatedSizeKB / 1024).toFixed(1)}MB`;
    }
  };
  const ActionButton = ({ 
    onClick, 
    icon, 
    color = 'gray',
    disabled = false,
    loading = false,
    tooltip 
  }: {
    onClick: () => void;
    icon: React.ReactNode;
    color?: 'blue' | 'red' | 'green' | 'gray';
    disabled?: boolean;
    loading?: boolean;
    tooltip: string;
  }) => {
    const colorClasses = {
      blue: 'hover:bg-blue-50 hover:text-blue-600 text-blue-500',
      red: 'hover:bg-red-50 hover:text-red-600 text-red-500',
      green: 'hover:bg-green-50 hover:text-green-600 text-green-500',
      gray: 'hover:bg-gray-50 hover:text-gray-600 text-gray-500'
    };

    return (
      <button
        onClick={onClick}
        disabled={disabled || loading}
        className={`p-2.5 rounded-lg transition-all duration-200 ${colorClasses[color]} ${
          disabled ? 'opacity-50 cursor-not-allowed' : 'hover:shadow-sm'
        }`}
        title={tooltip}
      >
        {loading ? <LoadingSpinner size="sm" /> : icon}
      </button>
    );
  };

  if (reports.length === 0) {
    return (
      <div className="bg-gradient-to-br from-gray-50 to-white rounded-xl shadow-sm border border-gray-200 p-8">
        <div className="text-center">
          <div className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-4">
            <DocumentIcon className="w-8 h-8 text-gray-400" />
          </div>
          <h3 className="text-lg font-semibold text-gray-900 mb-2">No Reports Yet</h3>
          <p className="text-gray-500 max-w-sm mx-auto">
            Select employees and generate your first certification report to get started.
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">      {/* Header */}
      <div className="bg-gradient-to-r from-blue-50 to-indigo-50 px-6 py-4 border-b border-gray-200">
        <div className="flex justify-between items-center">
          <div>
            <h3 className="text-lg font-semibold text-gray-900">
              Reports Dashboard
            </h3>
            <p className="text-sm text-gray-600 mt-1">
              {reports.length} report{reports.length !== 1 ? 's' : ''} generated
            </p>
          </div>
          <div className="flex items-center space-x-3">
            {/* System Management Buttons */}
            {onCleanupStuckReports && (
              <button
                onClick={onCleanupStuckReports}
                disabled={systemLoading}
                className="inline-flex items-center px-3 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-colors duration-200 disabled:opacity-50 disabled:cursor-not-allowed"
                title="Cleanup reports that are stuck in processing"
              >
                <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                </svg>
                Cleanup Stuck
              </button>
            )}
            {onRefreshReports && (
              <button
                onClick={onRefreshReports}
                disabled={systemLoading}
                className="inline-flex items-center px-3 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-colors duration-200 disabled:opacity-50 disabled:cursor-not-allowed"
                title="Refresh the reports list"
              >
                <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                </svg>
                Refresh Reports
              </button>
            )}
            {reports.length > 5 && (
              <div className="flex items-center space-x-3 border-l border-gray-300 pl-3">
                <label className="text-sm font-medium text-gray-700">Show:</label>
                <select
                  value={pageSize}
                  onChange={(e) => setPageSize(Number(e.target.value))}
                  className="text-sm border border-gray-300 rounded-lg px-3 py-1.5 bg-white focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                >
                  <option value={5}>5 per page</option>
                  <option value={10}>10 per page</option>
                  <option value={20}>20 per page</option>
                </select>
              </div>
            )}
          </div>
        </div>
      </div>
      
      {/* Reports List */}
      <div className="divide-y divide-gray-100">
        {paginatedReports.map((report, index) => (
          <div 
            key={report.id} 
            className={`p-6 hover:bg-gray-50 transition-colors duration-200 ${
              index === 0 ? 'bg-blue-50/30' : ''
            }`}
          >
            <div className="flex items-start justify-between">
              <div className="flex-1 min-w-0">
                <div className="flex items-center space-x-3 mb-3">
                  <div className="flex-shrink-0">
                    <div className="w-10 h-10 bg-gradient-to-br from-blue-500 to-blue-600 rounded-lg flex items-center justify-center">
                      <DocumentIcon className="w-5 h-5 text-white" />
                    </div>
                  </div>
                  <div className="flex-1 min-w-0">
                    <h4 className="text-sm font-semibold text-gray-900 truncate">
                      {report.name}
                    </h4>
                    <div className="flex items-center space-x-2 mt-1">
                      {getStatusBadge(report.status)}
                      {index === 0 && (
                        <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                          Latest
                        </span>
                      )}
                    </div>
                  </div>
                </div>
                
                <div className="flex items-center space-x-4 text-sm text-gray-500">
                  <span className="flex items-center">
                    <span className="w-2 h-2 bg-gray-300 rounded-full mr-2"></span>
                    {report.type.replace('_', ' ')}
                  </span>
                  <span>•</span>
                  <span>{formatDate(report.createdAt)}</span>
                  {report.pageCount && (
                    <>
                      <span>•</span>
                      <span>{report.pageCount} page{report.pageCount !== 1 ? 's' : ''}</span>
                    </>
                  )}
                  {formatFileSize(report.pageCount) && (
                    <>
                      <span>•</span>
                      <span>{formatFileSize(report.pageCount)}</span>
                    </>
                  )}
                </div>
                
                {report.errorMessage && (
                  <div className="mt-2 p-3 bg-red-50 border border-red-200 rounded-lg">
                    <p className="text-sm text-red-700">
                      <span className="font-medium">Error:</span> {report.errorMessage}
                    </p>
                  </div>
                )}
              </div>
                {/* Action Buttons */}
              <div className="flex items-center space-x-2 ml-6">
                {(report.status === 'QUEUED' || report.status === 'IN_PROGRESS') && (
                  <ActionButton
                    onClick={() => onRefreshStatus(report.id)}
                    icon={<RefreshIcon />}
                    color="gray"
                    tooltip="Refresh report status"
                  />
                )}
                
                {report.status === 'COMPLETED' && (
                  <>
                    <ActionButton
                      onClick={() => onDownload(report.id)}
                      icon={<DownloadIcon />}
                      color="green"
                      tooltip="Download PDF report"
                    />
                    {onDelete && (
                      <ActionButton
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
                        icon={<DeleteIcon />}
                        color="red"
                        loading={deletingReports.has(report.id)}
                        tooltip="Delete this report permanently"
                      />
                    )}
                  </>
                )}
                
                {(report.status === 'FAILED' || report.status === 'QUEUED') && onDelete && (
                  <ActionButton
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
                    icon={<DeleteIcon />}
                    color="red"
                    loading={deletingReports.has(report.id)}
                    tooltip="Remove failed report"
                  />
                )}
                
                {report.status === 'FAILED' && onRegenerate && (
                  <ActionButton
                    onClick={() => onRegenerate(report.id)}
                    icon={<RegenerateIcon />}
                    color="blue"
                    tooltip="Try generating this report again"
                  />
                )}
              </div>
            </div>
          </div>
        ))}
      </div>
      
      {/* Pagination */}
      {totalPages > 1 && (
        <div className="bg-gray-50 px-6 py-4 border-t border-gray-200">
          <div className="flex justify-between items-center">
            <div className="text-sm text-gray-700">
              Showing <span className="font-medium">{startIndex + 1}</span> to{' '}
              <span className="font-medium">{Math.min(endIndex, reports.length)}</span> of{' '}
              <span className="font-medium">{reports.length}</span> reports
            </div>
            <div className="flex items-center space-x-2">
              <button
                onClick={() => setCurrentPage(Math.max(1, currentPage - 1))}
                disabled={currentPage === 1}
                className="inline-flex items-center px-3 py-2 text-sm font-medium text-gray-500 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 hover:text-gray-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors duration-200"
              >
                ← Previous
              </button>
              <div className="flex items-center space-x-1">
                {Array.from({ length: totalPages }, (_, i) => i + 1).map((page) => (
                  <button
                    key={page}
                    onClick={() => setCurrentPage(page)}
                    className={`inline-flex items-center px-3 py-2 text-sm font-medium rounded-lg transition-colors duration-200 ${
                      currentPage === page
                        ? 'bg-blue-600 text-white'
                        : 'text-gray-500 bg-white border border-gray-300 hover:bg-gray-50 hover:text-gray-700'
                    }`}
                  >
                    {page}
                  </button>
                ))}
              </div>
              <button
                onClick={() => setCurrentPage(Math.min(totalPages, currentPage + 1))}
                disabled={currentPage === totalPages}
                className="inline-flex items-center px-3 py-2 text-sm font-medium text-gray-500 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 hover:text-gray-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors duration-200"
              >
                Next →
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default ReportList;
