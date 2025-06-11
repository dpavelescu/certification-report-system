import '@testing-library/jest-dom'

// Mock fetch for API tests
global.fetch = vi.fn()

// Mock environment variables
Object.defineProperty(window, 'ENV', {
  writable: true,
  value: {
    API_BASE_URL: 'http://localhost:8080/api'
  }
})
