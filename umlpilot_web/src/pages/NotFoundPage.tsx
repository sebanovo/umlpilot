import { Link } from 'react-router-dom'
import { useAuth } from '../features/auth/useAuth'
import { Compass, ArrowLeft, LogIn } from 'lucide-react'

export function NotFoundPage() {
  const { isAuthenticated } = useAuth()
  const home = isAuthenticated ? '/projects' : '/login'

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-950 px-4">
      <div className="w-full max-w-md text-center">
        <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-gray-900 border border-gray-800 mb-6">
          <Compass className="w-8 h-8 text-blue-400" />
        </div>
        <h1 className="text-6xl font-bold text-gray-100 tracking-tight">404</h1>
        <p className="text-lg text-gray-300 mt-3">Página no encontrada</p>
        <p className="text-sm text-gray-500 mt-2">
          La dirección a la que intentas acceder no existe o fue movida.
        </p>

        <div className="flex items-center justify-center gap-3 mt-8">
          <Link to={home}
            className="flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2.5 text-white font-medium hover:bg-blue-500 transition-colors">
            <ArrowLeft className="w-4 h-4" />
            {isAuthenticated ? 'Volver al inicio' : 'Ir al inicio'}
          </Link>
          {!isAuthenticated && (
            <Link to="/login"
              className="flex items-center gap-2 rounded-lg border border-gray-700 bg-gray-800 px-4 py-2.5 text-gray-200 hover:bg-gray-700 transition-colors">
              <LogIn className="w-4 h-4" />
              Iniciar sesión
            </Link>
          )}
        </div>
      </div>
    </div>
  )
}
