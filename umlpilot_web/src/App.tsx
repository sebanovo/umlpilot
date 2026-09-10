import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider, useAuth } from './features/auth/AuthContext'
import { LoginPage } from './pages/LoginPage'
import { RegisterPage } from './pages/RegisterPage'
import { HomePage } from './pages/HomePage'
import type { ReactNode } from 'react'

function ProtectedRoute({ children }: { children: ReactNode }) {
  const { isAuthenticated, isLoading } = useAuth()

  if (isLoading) return <p style={{ padding: '2rem' }}>Cargando...</p>
  if (!isAuthenticated) return <Navigate to="/login" replace />

  return <>{children}</>
}

function PublicRoute({ children }: { children: ReactNode }) {
  const { isAuthenticated, isLoading } = useAuth()

  if (isLoading) return <p style={{ padding: '2rem' }}>Cargando...</p>
  if (isAuthenticated) return <Navigate to="/" replace />

  return <>{children}</>
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<PublicRoute><LoginPage /></PublicRoute>} />
      <Route path="/register" element={<PublicRoute><RegisterPage /></PublicRoute>} />
      <Route path="/" element={<ProtectedRoute><HomePage /></ProtectedRoute>} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>
    </BrowserRouter>
  )
}

export default App
