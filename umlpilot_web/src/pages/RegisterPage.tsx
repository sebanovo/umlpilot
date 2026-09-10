import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useAuth } from '../features/auth/AuthContext'

export function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [firstName, setFirstName] = useState('')
  const [lastName, setLastName] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)

    try {
      await register(email, password, firstName, lastName)
      navigate('/')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error al registrarse')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{ maxWidth: '400px', margin: '4rem auto', padding: '2rem', fontFamily: 'sans-serif' }}>
      <h1>Registrarse</h1>
      <form onSubmit={handleSubmit}>
        <div style={{ marginBottom: '1rem' }}>
          <label style={{ display: 'block', marginBottom: '0.25rem' }}>Nombre</label>
          <input
            type="text"
            value={firstName}
            onChange={(e) => setFirstName(e.target.value)}
            required
            style={{ width: '100%', padding: '0.5rem', boxSizing: 'border-box' }}
          />
        </div>
        <div style={{ marginBottom: '1rem' }}>
          <label style={{ display: 'block', marginBottom: '0.25rem' }}>Apellido</label>
          <input
            type="text"
            value={lastName}
            onChange={(e) => setLastName(e.target.value)}
            required
            style={{ width: '100%', padding: '0.5rem', boxSizing: 'border-box' }}
          />
        </div>
        <div style={{ marginBottom: '1rem' }}>
          <label style={{ display: 'block', marginBottom: '0.25rem' }}>Email</label>
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            style={{ width: '100%', padding: '0.5rem', boxSizing: 'border-box' }}
          />
        </div>
        <div style={{ marginBottom: '1rem' }}>
          <label style={{ display: 'block', marginBottom: '0.25rem' }}>Password (mínimo 8 caracteres)</label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            minLength={8}
            style={{ width: '100%', padding: '0.5rem', boxSizing: 'border-box' }}
          />
        </div>
        {error && <p style={{ color: 'red' }}>{error}</p>}
        <button
          type="submit"
          disabled={loading}
          style={{ width: '100%', padding: '0.75rem', background: '#0066cc', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}
        >
          {loading ? 'Registrando...' : 'Registrarse'}
        </button>
      </form>
      <p style={{ marginTop: '1rem', textAlign: 'center' }}>
        ¿Ya tienes cuenta? <Link to="/login">Inicia sesión</Link>
      </p>
    </div>
  )
}
