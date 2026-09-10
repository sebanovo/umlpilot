import { useAuth } from '../features/auth/useAuth'

export function HomePage() {
  const { user, logout } = useAuth()

  return (
    <div style={{ maxWidth: '600px', margin: '4rem auto', padding: '2rem', fontFamily: 'sans-serif' }}>
      <h1>{import.meta.env.VITE_APP_NAME}</h1>
      <p>Hola, <strong>{user?.firstName} {user?.lastName}</strong> ({user?.email})</p>
      <p>Rol: <strong>{user?.role}</strong></p>
      <button
        onClick={logout}
        style={{ padding: '0.5rem 1rem', background: '#cc0000', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}
      >
        Cerrar sesión
      </button>
    </div>
  )
}
