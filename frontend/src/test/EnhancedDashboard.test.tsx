import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import EnhancedDashboard from '../components/EnhancedDashboard'
import * as api from '../services/api'

// Mock the API module
vi.mock('../services/api')

const mockEmployees = [
  { id: 'EMP001', name: 'John Doe', department: 'Engineering', title: 'Software Engineer' },
  { id: 'EMP002', name: 'Jane Smith', department: 'HR', title: 'HR Manager' },
  { id: 'EMP003', name: 'Bob Johnson', department: 'Engineering', title: 'Senior Developer' }
]

const mockCertifications = [
  { id: 1, name: 'AWS Certified', status: 'Valid', expiryDate: '2025-12-31' },
  { id: 2, name: 'Java Certification', status: 'Expired', expiryDate: '2024-01-01' }
]

describe('EnhancedDashboard', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(api.fetchEmployees).mockResolvedValue(mockEmployees)
    vi.mocked(api.fetchCertifications).mockResolvedValue(mockCertifications)
  })

  it('should render loading state initially', () => {
    render(<EnhancedDashboard />)
    expect(screen.getByTestId('loading-spinner')).toBeInTheDocument()
  })

  it('should load and display employees', async () => {
    render(<EnhancedDashboard />)
    
    await waitFor(() => {
      expect(screen.getByText('John Doe')).toBeInTheDocument()
      expect(screen.getByText('Jane Smith')).toBeInTheDocument()
      expect(screen.getByText('Bob Johnson')).toBeInTheDocument()
    })
    
    expect(api.fetchEmployees).toHaveBeenCalledTimes(1)
  })

  it('should filter employees by department', async () => {
    render(<EnhancedDashboard />)
    
    await waitFor(() => {
      expect(screen.getByText('John Doe')).toBeInTheDocument()
    })

    // Filter by Engineering department
    const departmentFilter = screen.getByDisplayValue('All Departments')
    await userEvent.selectOptions(departmentFilter, 'Engineering')

    // Should show only Engineering employees
    expect(screen.getByText('John Doe')).toBeInTheDocument()
    expect(screen.getByText('Bob Johnson')).toBeInTheDocument()
    expect(screen.queryByText('Jane Smith')).not.toBeInTheDocument()
  })

  it('should search employees by name', async () => {
    render(<EnhancedDashboard />)
    
    await waitFor(() => {
      expect(screen.getByText('John Doe')).toBeInTheDocument()
    })

    // Search for "John"
    const searchInput = screen.getByPlaceholderText(/search employees/i)
    await userEvent.type(searchInput, 'John')

    // Should show only John Doe and Bob Johnson
    expect(screen.getByText('John Doe')).toBeInTheDocument()
    expect(screen.getByText('Bob Johnson')).toBeInTheDocument()
    expect(screen.queryByText('Jane Smith')).not.toBeInTheDocument()
  })

  it('should handle employee selection', async () => {
    render(<EnhancedDashboard />)
    
    await waitFor(() => {
      expect(screen.getByText('John Doe')).toBeInTheDocument()
    })

    // Select an employee
    const checkbox = screen.getByLabelText('Select John Doe')
    await userEvent.click(checkbox)

    // Check that certifications are loaded
    await waitFor(() => {
      expect(api.fetchCertifications).toHaveBeenCalledWith('EMP001')
    })

    // Generate button should be enabled
    const generateButton = screen.getByText(/generate report/i)
    expect(generateButton).not.toBeDisabled()
  })

  it('should disable generate button when no employees selected', async () => {
    render(<EnhancedDashboard />)
    
    await waitFor(() => {
      expect(screen.getByText('John Doe')).toBeInTheDocument()
    })

    const generateButton = screen.getByText(/generate report/i)
    expect(generateButton).toBeDisabled()
  })

  it('should handle pagination', async () => {
    render(<EnhancedDashboard />)
    
    await waitFor(() => {
      expect(screen.getByText('John Doe')).toBeInTheDocument()
    })

    // Check if pagination controls are present
    const nextButton = screen.queryByText(/next/i)
    const prevButton = screen.queryByText(/previous/i)
    
    // With only 3 employees and page size of 10, pagination shouldn't be needed
    expect(nextButton).not.toBeInTheDocument()
    expect(prevButton).not.toBeInTheDocument()
  })

  it('should handle API errors gracefully', async () => {
    vi.mocked(api.fetchEmployees).mockRejectedValue(new Error('API Error'))
    
    render(<EnhancedDashboard />)
    
    await waitFor(() => {
      expect(screen.getByText(/error loading employees/i)).toBeInTheDocument()
    })
  })

  it('should handle date range selection', async () => {
    render(<EnhancedDashboard />)
    
    await waitFor(() => {
      expect(screen.getByText('John Doe')).toBeInTheDocument()
    })

    // Set date range
    const startDateInput = screen.getByLabelText(/start date/i)
    const endDateInput = screen.getByLabelText(/end date/i)
    
    await userEvent.type(startDateInput, '2024-01-01')
    await userEvent.type(endDateInput, '2024-12-31')

    // Check that dates are set
    expect(startDateInput).toHaveValue('2024-01-01')
    expect(endDateInput).toHaveValue('2024-12-31')
  })

  it('should generate report with selected employees', async () => {
    const mockReport = { id: 'REP001', status: 'QUEUED' }
    vi.mocked(api.generateReport).mockResolvedValue(mockReport)
    
    render(<EnhancedDashboard />)
    
    await waitFor(() => {
      expect(screen.getByText('John Doe')).toBeInTheDocument()
    })

    // Select an employee
    const checkbox = screen.getByLabelText('Select John Doe')
    await userEvent.click(checkbox)

    // Wait for certifications to load
    await waitFor(() => {
      expect(api.fetchCertifications).toHaveBeenCalledWith('EMP001')
    })

    // Click generate report
    const generateButton = screen.getByText(/generate report/i)
    await userEvent.click(generateButton)

    await waitFor(() => {
      expect(api.generateReport).toHaveBeenCalledWith({
        employeeIds: ['EMP001'],
        reportType: 'EMPLOYEE_DEMOGRAPHICS'
      })
    })
  })

  it('should show certification details for selected employees', async () => {
    render(<EnhancedDashboard />)
    
    await waitFor(() => {
      expect(screen.getByText('John Doe')).toBeInTheDocument()
    })

    // Select an employee
    const checkbox = screen.getByLabelText('Select John Doe')
    await userEvent.click(checkbox)

    // Wait for certifications to load and be displayed
    await waitFor(() => {
      expect(screen.getByText('AWS Certified')).toBeInTheDocument()
      expect(screen.getByText('Java Certification')).toBeInTheDocument()
    })

    // Check certification statuses
    expect(screen.getByText('Valid')).toBeInTheDocument()
    expect(screen.getByText('Expired')).toBeInTheDocument()
  })
})
