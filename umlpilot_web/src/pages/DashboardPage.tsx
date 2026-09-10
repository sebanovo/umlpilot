import { useState, useEffect } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { projectsApi, type Project } from '../shared/api/client'
import { useAuth } from '../features/auth/useAuth'

export function DashboardPage() {
  const { logout, user } = useAuth()
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

  return (
    <div className="mx-auto max-w-4xl px-8 py-8 font-sans">
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Mis Proyectos</h1>
          {user && <p className="text-sm text-gray-500 mt-1">{user.firstName} {user.lastName}</p>}
        </div>
        <div className="flex gap-2">
          <button onClick={() => setShowCreate(true)}
            className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 transition-colors cursor-pointer">
            + Nuevo Proyecto
          </button>
          <button onClick={() => { logout(); navigate('/login') }}
            className="rounded-lg bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700 transition-colors cursor-pointer">
            Cerrar sesión
          </button>
        </div>
      </div>

      {showCreate && (
        <form onSubmit={handleCreate} className="bg-gray-50 rounded-xl p-6 mb-8 border border-gray-200">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Crear Proyecto</h3>
          <input type="text" placeholder="Nombre del proyecto" value={name} onChange={(e) => setName(e.target.value)} required
            className="w-full rounded-lg border border-gray-300 px-3 py-2 text-gray-900 mb-3 focus:border-blue-500 focus:ring-2 focus:ring-blue-500 outline-none" />
          <textarea placeholder="Descripción (opcional)" value={description} onChange={(e) => setDescription(e.target.value)}
            className="w-full rounded-lg border border-gray-300 px-3 py-2 text-gray-900 mb-4 min-h-16 focus:border-blue-500 focus:ring-2 focus:ring-blue-500 outline-none" />
          <div className="flex gap-2">
            <button type="submit" className="rounded-lg bg-green-600 px-4 py-2 text-sm font-medium text-white hover:bg-green-700 cursor-pointer">Crear</button>
            <button type="button" onClick={() => setShowCreate(false)} className="rounded-lg bg-gray-400 px-4 py-2 text-sm font-medium text-white hover:bg-gray-500 cursor-pointer">Cancelar</button>
          </div>
        </form>
      )}

      {loading ? (
        <p className="text-gray-500">Cargando proyectos...</p>
      ) : projects.length === 0 ? (
        <p className="text-gray-500">No tienes proyectos. Crea uno para empezar.</p>
      ) : (
        <div className="grid gap-3">
          {projects.map((project) => (
            <Link key={project.id} to={`/projects/${project.id}`}
              className="block rounded-xl bg-gray-50 border border-gray-200 p-5 hover:border-blue-300 hover:shadow-sm transition-all">
              <h3 className="font-semibold text-gray-900 mb-1">{project.name}</h3>
              <p className="text-sm text-gray-500 mb-2">{project.description || 'Sin descripción'}</p>
              <small className="text-xs text-gray-400">Creado: {new Date(project.createdAt).toLocaleDateString()}</small>
            </Link>
          ))}
        </div>
      )}
    </div>
  )
}
