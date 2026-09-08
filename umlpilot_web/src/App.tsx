import { useEffect, useState } from 'react'
import './App.css'

interface HealthResponse {
  status: string
  message: string
}

function App() {
  const [health, setHealth] = useState<HealthResponse | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    fetch('/api/health')
      .then((res) => res.json())
      .then((data: HealthResponse) => setHealth(data))
      .catch((err) => setError(err.message))
  }, [])

  return (
    <div style={{ padding: '2rem', fontFamily: 'sans-serif' }}>
      <h1>{import.meta.env.VITE_APP_NAME}</h1>
      <h2>Health Check</h2>
      {error && <p style={{ color: 'red' }}>Error: {error}</p>}
      {health ? (
        <pre style={{ background: '#f4f4f4', padding: '1rem', borderRadius: '8px' }}>
{JSON.stringify(health, null, 2)}
        </pre>
      ) : (
        <p>Cargando...</p>
      )}
    </div>
  )
}

export default App
