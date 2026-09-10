import { useState, useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { projectsApi, type Project } from '../shared/api/client'
import { useAuth } from '../features/auth/useAuth'

export function DashboardPage() {
  const { logout } = useAuth()
  const [projects, setProjects] = useState<Project[]>([])
  const [loading, setLoading] = useState(true)
  const [showCreate, setShowCreate] = useState(false)
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const navigate = useNavigate()

  useEffect(() => {
    projectsApi.list().then(setProjects).finally(() => setLoading(false))
  }, [])

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault()
    try {
      const project = await projectsApi.create({ name, description })
      setProjects([...projects, project])
      setShowCreate(false)
      setName('')
      setDescription('')
      navigate(`/projects/${project.id}`)
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Error al crear proyecto')
    }
  }

  if (loading) return <p style={{ padding: '2rem' }}>Cargando proyectos...</p>

  return (
    <div style={{ maxWidth: '800px', margin: '2rem auto', padding: '0 2rem', fontFamily: 'sans-serif' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
        <h1>Mis Proyectos</h1>
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <button
            onClick={() => setShowCreate(true)}
            style={{ padding: '0.5rem 1rem', background: '#0066cc', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}
          >
            + Nuevo Proyecto
          </button>
          <button
            onClick={() => { logout(); navigate('/login') }}
            style={{ padding: '0.5rem 1rem', background: '#cc0000', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}
          >
            Cerrar sesión
          </button>
        </div>
      </div>

      {showCreate && (
        <form onSubmit={handleCreate} style={{ background: '#f5f5f5', padding: '1.5rem', borderRadius: '8px', marginBottom: '2rem' }}>
          <h3>Crear Proyecto</h3>
          <div style={{ marginBottom: '1rem' }}>
            <input
              type="text"
              placeholder="Nombre del proyecto"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
              style={{ width: '100%', padding: '0.5rem', boxSizing: 'border-box' }}
            />
          </div>
          <div style={{ marginBottom: '1rem' }}>
            <textarea
              placeholder="Descripción (opcional)"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              style={{ width: '100%', padding: '0.5rem', boxSizing: 'border-box', minHeight: '60px' }}
            />
          </div>
          <button type="submit" style={{ padding: '0.5rem 1rem', background: '#28a745', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', marginRight: '0.5rem' }}>
            Crear
          </button>
          <button type="button" onClick={() => setShowCreate(false)} style={{ padding: '0.5rem 1rem', background: '#6c757d', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>
            Cancelar
          </button>
        </form>
      )}

      {projects.length === 0 ? (
        <p>No tienes proyectos. Crea uno para empezar.</p>
      ) : (
        <div style={{ display: 'grid', gap: '1rem' }}>
          {projects.map((project) => (
            <Link
              key={project.id}
              to={`/projects/${project.id}`}
              style={{ display: 'block', padding: '1.5rem', background: '#f5f5f5', borderRadius: '8px', textDecoration: 'none', color: 'inherit' }}
            >
              <h3 style={{ margin: '0 0 0.5rem 0' }}>{project.name}</h3>
              <p style={{ margin: '0 0 0.5rem 0', color: '#666' }}>{project.description || 'Sin descripción'}</p>
              <small style={{ color: '#999' }}>Creado: {new Date(project.createdAt).toLocaleDateString()}</small>
            </Link>
          ))}
        </div>
      )}
    </div>
  )
}
