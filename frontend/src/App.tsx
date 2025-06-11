import EnhancedDashboard from './components/EnhancedDashboard'
import { ReportProvider } from './context/ReportContext'

function App() {
  return (
    <ReportProvider>
      <div className="min-h-screen bg-gray-50">
        <header className="bg-white shadow-sm border-b">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="flex justify-between items-center py-6">
              <h1 className="text-3xl font-bold text-gray-900">
                Certification Report System
              </h1>
              <div className="text-sm text-gray-500">
                Simplified Filtering Interface
              </div>
            </div>
          </div>
        </header>
        
        <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <EnhancedDashboard />
        </main>
      </div>
    </ReportProvider>
  )
}

export default App
