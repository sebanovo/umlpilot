import { useAuth } from '../features/auth/useAuth'

export function HomePage() {
  const { user, logout } = useAuth()

  return (
    <div className="mx-auto max-w-lg mt-16 p-8 font-sans">
      <h1 className="text-2xl font-bold text-gray-900 mb-4">{import.meta.env.VITE_APP_NAME}</h1>
      <p className="text-gray-700 mb-2">Hola, <strong>{user?.firstName} {user?.lastName}</strong> ({user?.email})</p>
      <p className="text-gray-500 mb-6">Rol: <strong>{user?.role}</strong></p>
      <button onClick={logout}
        className="rounded-lg bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700 transition-colors cursor-pointer">
        Cerrar sesión
      </button>
    </div>
  )
}
