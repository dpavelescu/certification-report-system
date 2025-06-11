import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import ReportList from '../components/ReportList'
import * as api from '../services/api'

// Mock the API module
vi.mock('../services/api')

const mockReports = {
  content: [
    {
      id: 'REP001',
      status: 'COMPLETED',
      reportType: 'EMPLOYEE_DEMOGRAPHICS',
      createdAt: '2024-06-01T10:00:00Z',
      completedAt: '2024-06-01T10:01:30Z'
    },
    {
      id: 'REP002',
      status: 'PROCESSING',
      reportType: 'EMPLOYEE_DEMOGRAPHICS',
      createdAt: '2024-06-01T11:00:00Z',
      completedAt: null
    },
    {
      id: 'REP003',
      status: 'FAILED',
      reportType: 'EMPLOYEE_DEMOGRAPHICS',
      createdAt: '2024-06-01T12:00:00Z',
      completedAt: null,
      errorMessage: 'Database connection failed'
    }
  ],
  totalElements: 3,
  totalPages: 1,
  number: 0,
  size: 10
}

describe('ReportList', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(api.fetchReports).mockResolvedValue(mockReports)
  })

  it('should render loading state initially', () => {
    render(<ReportList />)
    expect(screen.getByTestId('loading-spinner')).toBeInTheDocument()
  })

  it('should load and display reports', async () => {
    render(<ReportList />)
    
    await waitFor(() => {
      expect(screen.getByText('REP001')).toBeInTheDocument()
      expect(screen.getByText('REP002')).toBeInTheDocument()
      expect(screen.getByText('REP003')).toBeInTheDocument()
    })
    
    expect(api.fetchReports).toHaveBeenCalledWith(0, 10)
  })

  it('should display correct status styling', async () => {
    render(<ReportList />)
    
    await waitFor(() => {
      const completedStatus = screen.getByText('Completed')
      const processingStatus = screen.getByText('Processing')
      const failedStatus = screen.getByText('Failed')
      
      expect(completedStatus).toHaveClass('bg-green-100', 'text-green-800')
      expect(processingStatus).toHaveClass('bg-yellow-100', 'text-yellow-800')
      expect(failedStatus).toHaveClass('bg-red-100', 'text-red-800')
    })
  })

  it('should show download button for completed reports', async () => {
    render(<ReportList />)
    
    await waitFor(() => {
      const downloadButtons = screen.getAllByText(/download/i)
      // Should have download button only for completed reports
      expect(downloadButtons).toHaveLength(1)
    })
  })

  it('should show delete buttons for all reports', async () => {
    render(<ReportList />)
    
    await waitFor(() => {
      const deleteButtons = screen.getAllByText(/delete/i)
      // Should have delete button for all reports
      expect(deleteButtons).toHaveLength(3)
    })
  })

  it('should handle download action', async () => {
    const mockBlob = new Blob(['test'], { type: 'application/pdf' })
    vi.mocked(api.downloadReport).mockResolvedValue(mockBlob)
    
    // Mock URL.createObjectURL and URL.revokeObjectURL
    const mockCreateObjectURL = vi.fn().mockReturnValue('blob:mock-url')
    const mockRevokeObjectURL = vi.fn()
    global.URL.createObjectURL = mockCreateObjectURL
    global.URL.revokeObjectURL = mockRevokeObjectURL
    
    // Mock createElement and click for download
    const mockLink = {
      href: '',
      download: '',
      click: vi.fn()
    }
    vi.spyOn(document, 'createElement').mockReturnValue(mockLink as any)

    render(<ReportList />)
    
    await waitFor(() => {
      const downloadButton = screen.getByText(/download/i)
      fireEvent.click(downloadButton)
    })

    await waitFor(() => {
      expect(api.downloadReport).toHaveBeenCalledWith('REP001')
      expect(mockCreateObjectURL).toHaveBeenCalledWith(mockBlob)
      expect(mockLink.click).toHaveBeenCalled()
    })
  })

  it('should handle delete action with confirmation', async () => {
    vi.mocked(api.deleteReport).mockResolvedValue(undefined)
    
    // Mock window.confirm
    const mockConfirm = vi.fn().mockReturnValue(true)
    global.confirm = mockConfirm

    render(<ReportList />)
    
    await waitFor(() => {
      const deleteButtons = screen.getAllByText(/delete/i)
      fireEvent.click(deleteButtons[0])
    })

    await waitFor(() => {
      expect(mockConfirm).toHaveBeenCalledWith('Are you sure you want to delete this report?')
      expect(api.deleteReport).toHaveBeenCalledWith('REP001')
    })
  })

  it('should not delete when user cancels confirmation', async () => {
    // Mock window.confirm to return false
    const mockConfirm = vi.fn().mockReturnValue(false)
    global.confirm = mockConfirm

    render(<ReportList />)
    
    await waitFor(() => {
      const deleteButtons = screen.getAllByText(/delete/i)
      fireEvent.click(deleteButtons[0])
    })

    expect(mockConfirm).toHaveBeenCalled()
    expect(api.deleteReport).not.toHaveBeenCalled()
  })

  it('should handle pagination', async () => {
    const mockReportsPage2 = {
      ...mockReports,
      number: 1,
      totalPages: 2
    }
    
    vi.mocked(api.fetchReports).mockResolvedValueOnce(mockReports)
    vi.mocked(api.fetchReports).mockResolvedValueOnce(mockReportsPage2)

    render(<ReportList />)
    
    await waitFor(() => {
      expect(screen.getByText('REP001')).toBeInTheDocument()
    })

    // Should show pagination controls when there are multiple pages
    if (mockReports.totalPages > 1) {
      const nextButton = screen.getByText(/next/i)
      await userEvent.click(nextButton)
      
      expect(api.fetchReports).toHaveBeenCalledWith(1, 10)
    }
  })

  it('should show error message for failed reports', async () => {
    render(<ReportList />)
    
    await waitFor(() => {
      expect(screen.getByText('Database connection failed')).toBeInTheDocument()
    })
  })

  it('should refresh reports list', async () => {
    render(<ReportList />)
    
    await waitFor(() => {
      expect(screen.getByText('REP001')).toBeInTheDocument()
    })

    // Find and click refresh button
    const refreshButton = screen.getByText(/refresh/i)
    await userEvent.click(refreshButton)

    // Should call fetchReports again
    expect(api.fetchReports).toHaveBeenCalledTimes(2)
  })

  it('should handle API errors gracefully', async () => {
    vi.mocked(api.fetchReports).mockRejectedValue(new Error('API Error'))
    
    render(<ReportList />)
    
    await waitFor(() => {
      expect(screen.getByText(/error loading reports/i)).toBeInTheDocument()
    })
  })

  it('should show empty state when no reports', async () => {
    vi.mocked(api.fetchReports).mockResolvedValue({
      content: [],
      totalElements: 0,
      totalPages: 0,
      number: 0,
      size: 10
    })
    
    render(<ReportList />)
    
    await waitFor(() => {
      expect(screen.getByText(/no reports found/i)).toBeInTheDocument()
    })
  })

  it('should format dates correctly', async () => {
    render(<ReportList />)
    
    await waitFor(() => {
      // Check that dates are formatted properly
      expect(screen.getByText(/jun.*01.*2024/i)).toBeInTheDocument()
    })
  })
})
