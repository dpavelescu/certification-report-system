import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import LoadingSpinner from '../components/LoadingSpinner'

describe('LoadingSpinner', () => {
  it('should render with default props', () => {
    render(<LoadingSpinner />)
    
    const spinner = screen.getByTestId('loading-spinner')
    expect(spinner).toBeInTheDocument()
    expect(spinner).toHaveClass('w-8', 'h-8') // Default size
  })

  it('should render with custom size', () => {
    render(<LoadingSpinner size="large" />)
    
    const spinner = screen.getByTestId('loading-spinner')
    expect(spinner).toHaveClass('w-12', 'h-12') // Large size
  })

  it('should render with small size', () => {
    render(<LoadingSpinner size="small" />)
    
    const spinner = screen.getByTestId('loading-spinner')
    expect(spinner).toHaveClass('w-4', 'h-4') // Small size
  })

  it('should render with custom text', () => {
    render(<LoadingSpinner text="Loading data..." />)
    
    expect(screen.getByText('Loading data...')).toBeInTheDocument()
  })

  it('should render without text when not provided', () => {
    render(<LoadingSpinner />)
    
    // Should not render any text
    const textElement = screen.queryByText(/loading/i)
    expect(textElement).not.toBeInTheDocument()
  })

  it('should have proper accessibility attributes', () => {
    render(<LoadingSpinner />)
    
    const spinner = screen.getByTestId('loading-spinner')
    expect(spinner).toHaveAttribute('aria-label', 'Loading')
  })

  it('should apply animation classes', () => {
    render(<LoadingSpinner />)
    
    const spinner = screen.getByTestId('loading-spinner')
    expect(spinner).toHaveClass('animate-spin')
  })
})
