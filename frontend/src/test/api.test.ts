import { describe, it, expect, beforeEach, vi } from 'vitest'
import { 
  fetchEmployees, 
  fetchCertifications, 
  generateReport, 
  fetchReports, 
  getReportStatus,
  downloadReport,
  deleteReport
} from '../services/api'

// Mock fetch
const mockFetch = vi.fn()
global.fetch = mockFetch

describe('API Service', () => {
  beforeEach(() => {
    mockFetch.mockClear()
  })

  describe('fetchEmployees', () => {
    it('should fetch employees successfully', async () => {
      const mockEmployees = [
        { id: 'EMP001', name: 'John Doe', department: 'Engineering' },
        { id: 'EMP002', name: 'Jane Smith', department: 'HR' }
      ]

      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => mockEmployees
      })

      const result = await fetchEmployees()
      
      expect(mockFetch).toHaveBeenCalledWith('http://localhost:8080/api/employees')
      expect(result).toEqual(mockEmployees)
    })

    it('should throw error on fetch failure', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 500
      })

      await expect(fetchEmployees()).rejects.toThrow('Failed to fetch employees')
    })
  })

  describe('fetchCertifications', () => {
    it('should fetch certifications for employee', async () => {
      const employeeId = 'EMP001'
      const mockCertifications = [
        { id: 1, name: 'AWS Certified', status: 'Valid' },
        { id: 2, name: 'Java Certification', status: 'Expired' }
      ]

      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => mockCertifications
      })

      const result = await fetchCertifications(employeeId)
      
      expect(mockFetch).toHaveBeenCalledWith(`http://localhost:8080/api/employees/${employeeId}/certifications`)
      expect(result).toEqual(mockCertifications)
    })
  })

  describe('generateReport', () => {
    it('should generate report successfully', async () => {
      const reportRequest = {
        employeeIds: ['EMP001', 'EMP002'],
        reportType: 'EMPLOYEE_DEMOGRAPHICS'
      }
      const mockReport = { id: 'REP001', status: 'QUEUED' }

      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => mockReport
      })

      const result = await generateReport(reportRequest)
      
      expect(mockFetch).toHaveBeenCalledWith('http://localhost:8080/api/reports/generate', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(reportRequest)
      })
      expect(result).toEqual(mockReport)
    })

    it('should handle validation errors', async () => {
      const reportRequest = {
        employeeIds: [],
        reportType: 'INVALID'
      }

      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 400,
        text: async () => 'Validation failed'
      })

      await expect(generateReport(reportRequest)).rejects.toThrow('Failed to generate report: Validation failed')
    })
  })

  describe('fetchReports', () => {
    it('should fetch reports with default pagination', async () => {
      const mockReports = {
        content: [
          { id: 'REP001', status: 'COMPLETED' },
          { id: 'REP002', status: 'PROCESSING' }
        ],
        totalElements: 2,
        totalPages: 1
      }

      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => mockReports
      })

      const result = await fetchReports()
      
      expect(mockFetch).toHaveBeenCalledWith('http://localhost:8080/api/reports?page=0&size=10')
      expect(result).toEqual(mockReports)
    })

    it('should fetch reports with custom pagination', async () => {
      await fetchReports(2, 5)
      
      expect(mockFetch).toHaveBeenCalledWith('http://localhost:8080/api/reports?page=2&size=5')
    })
  })

  describe('getReportStatus', () => {
    it('should get report status', async () => {
      const reportId = 'REP001'
      const mockStatus = { id: reportId, status: 'COMPLETED' }

      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => mockStatus
      })

      const result = await getReportStatus(reportId)
      
      expect(mockFetch).toHaveBeenCalledWith(`http://localhost:8080/api/reports/${reportId}/status`)
      expect(result).toEqual(mockStatus)
    })
  })

  describe('downloadReport', () => {
    it('should download report as blob', async () => {
      const reportId = 'REP001'
      const mockBlob = new Blob(['test'], { type: 'application/pdf' })

      mockFetch.mockResolvedValueOnce({
        ok: true,
        blob: async () => mockBlob
      })

      const result = await downloadReport(reportId)
      
      expect(mockFetch).toHaveBeenCalledWith(`http://localhost:8080/api/reports/${reportId}/download`)
      expect(result).toEqual(mockBlob)
    })
  })

  describe('deleteReport', () => {
    it('should delete report successfully', async () => {
      const reportId = 'REP001'

      mockFetch.mockResolvedValueOnce({
        ok: true
      })

      await deleteReport(reportId)
      
      expect(mockFetch).toHaveBeenCalledWith(`http://localhost:8080/api/reports/${reportId}`, {
        method: 'DELETE'
      })
    })

    it('should throw error on delete failure', async () => {
      const reportId = 'REP001'

      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 404
      })

      await expect(deleteReport(reportId)).rejects.toThrow('Failed to delete report')
    })
  })
})
