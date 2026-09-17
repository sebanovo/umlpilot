import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../features/auth/useAuth'
import { Loader2 } from 'lucide-react'

/**
 * Página de redirección: lleva al usuario al sitio correcto según su sesión.
 * Se usa en `/` y `/redirect` para no mostrar una pantalla vacía.
 */
export function RedirectPage() {
  const { isAuthenticated, isLoading } = useAuth()
  const location = useLocation()
  const from = (location.state as { from?: string } | null)?.from

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center gap-2 bg-gray-950 text-gray-400">
        <Loader2 className="w-4 h-4 animate-spin" />
        Redirigiendo...
      </div>
    )
  }

  return <Navigate to={isAuthenticated ? (from || '/projects') : '/login'} replace />
}
