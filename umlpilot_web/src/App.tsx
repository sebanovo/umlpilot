import { BrowserRouter, Routes, Route, Navigate, useLocation } from 'react-router-dom'
import { AuthProvider } from './features/auth/AuthProvider'
import { useAuth } from './features/auth/useAuth'
import { ToastProvider } from './shared/ui/toast/ToastProvider'
import { LoginPage } from './pages/LoginPage'
import { RegisterPage } from './pages/RegisterPage'
import { DashboardPage } from './pages/DashboardPage'
import { ProjectDetailPage } from './pages/ProjectDetailPage'
import { DiagramEditorPage } from './pages/DiagramEditorPage'
import { LandingPage } from './pages/LandingPage'
import { NotFoundPage } from './pages/NotFoundPage'
import { RedirectPage } from './pages/RedirectPage'
import { Loader2 } from 'lucide-react'

function FullPageLoader() {
  return (
    <div className="min-h-screen flex items-center justify-center gap-2 bg-gray-950 text-gray-400">
      <Loader2 className="w-4 h-4 animate-spin" />
      Cargando...
    </div>
  )
}

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, isLoading } = useAuth()
  const location = useLocation()

  if (isLoading) return <FullPageLoader />
  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />
  }

  return <>{children}</>
}

function HomeRoute() {
  const { isAuthenticated, isLoading } = useAuth()

  if (isLoading) return <FullPageLoader />
  if (isAuthenticated) return <Navigate to="/projects" replace />

  return <LandingPage />
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/redirect" element={<RedirectPage />} />
      <Route path="/" element={<HomeRoute />} />
      <Route path="/projects" element={<ProtectedRoute><DashboardPage /></ProtectedRoute>} />
      <Route path="/projects/:id" element={<ProtectedRoute><ProjectDetailPage /></ProtectedRoute>} />
      <Route path="/projects/:id/diagrams/:diagramId" element={<ProtectedRoute><DiagramEditorPage /></ProtectedRoute>} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}

function App() {
  return (
    <BrowserRouter>
      <ToastProvider>
        <AuthProvider>
          <AppRoutes />
        </AuthProvider>
      </ToastProvider>
    </BrowserRouter>
  )
}

export default App
