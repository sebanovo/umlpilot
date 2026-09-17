import { useState, useEffect } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { projectsApi, type Project } from '../shared/api/client'
import { useAuth } from '../features/auth/useAuth'
import { useToast } from '../shared/ui/toast/useToast'
import { Plus, LogOut, FolderOpen, Calendar, Loader2 } from 'lucide-react'

export function DashboardPage() {
  const { logout, user } = useAuth()
  const { toast } = useToast()
  const [projects, setProjects] = useState<Project[]>([])
  const [loading, setLoading] = useState(true)
  const [showCreate, setShowCreate] = useState(false)
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [creating, setCreating] = useState(false)
  const navigate = useNavigate()

  useEffect(() => {
    projectsApi.list().then(setProjects).finally(() => setLoading(false))
  }, [])

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault()
    setCreating(true)
    try {
      const project = await projectsApi.create({ name, description })
      setProjects([...projects, project])
      setShowCreate(false)
      setName('')
      setDescription('')
      toast({ title: 'Proyecto creado', description: project.name, variant: 'success' })
      navigate(`/projects/${project.id}`)
    } catch (err) {
      toast({
        title: 'No se pudo crear el proyecto',
        description: err instanceof Error ? err.message : 'Error inesperado',
        variant: 'error',
      })
    } finally {
      setCreating(false)
    }
  }

  return (
    <div className="min-h-screen bg-gray-950">
      <header className="bg-gray-900 border-b border-gray-800 sticky top-0 z-10">
        <div className="max-w-6xl mx-auto px-6 py-4 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-lg bg-blue-600 flex items-center justify-center">
              <svg className="w-5 h-5 text-white" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <rect x="3" y="3" width="7" height="7" rx="1" />
                <rect x="14" y="3" width="7" height="7" rx="1" />
                <rect x="3" y="14" width="7" height="7" rx="1" />
                <rect x="14" y="14" width="7" height="7" rx="1" />
              </svg>
            </div>
            <div>
              <h1 className="text-lg font-bold text-gray-100">UMLPilot</h1>
              <p className="text-xs text-gray-500">Mis Proyectos</p>
            </div>
          </div>
          <div className="flex items-center gap-3">
            {user && (
              <span className="text-sm text-gray-400">{user.firstName} {user.lastName}</span>
            )}
            <button onClick={() => { logout(); navigate('/login') }}
              className="flex items-center gap-1.5 text-sm text-gray-400 hover:text-red-400 transition-colors cursor-pointer">
              <LogOut className="w-4 h-4" />
              Salir
            </button>
          </div>
        </div>
      </header>

      <main className="max-w-6xl mx-auto px-6 py-8">
        <div className="flex items-center justify-between mb-8">
          <div>
            <h2 className="text-xl font-bold text-gray-100">Proyectos</h2>
            <p className="text-sm text-gray-400 mt-1">{projects.length} proyecto{projects.length !== 1 ? 's' : ''}</p>
          </div>
          <button onClick={() => setShowCreate(true)}
            className="flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2.5 text-sm font-medium text-white hover:bg-blue-500 transition-colors shadow-sm cursor-pointer">
            <Plus className="w-4 h-4" />
            Nuevo Proyecto
          </button>
        </div>

        {showCreate && (
          <div className="bg-gray-900 rounded-xl border border-gray-800 shadow-sm p-6 mb-8">
            <h3 className="text-base font-semibold text-gray-100 mb-4">Crear Proyecto</h3>
            <form onSubmit={handleCreate} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-1.5">Nombre</label>
                <input type="text" placeholder="Mi proyecto UML" value={name} onChange={(e) => setName(e.target.value)} required
                  className="w-full rounded-lg border border-gray-700 bg-gray-800 px-3 py-2.5 text-gray-100 placeholder-gray-500 focus:border-blue-500 focus:ring-2 focus:ring-blue-500/40 outline-none transition-colors" />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-1.5">Descripción</label>
                <textarea placeholder="Descripción opcional del proyecto..." value={description} onChange={(e) => setDescription(e.target.value)}
                  className="w-full rounded-lg border border-gray-700 bg-gray-800 px-3 py-2.5 text-gray-100 placeholder-gray-500 min-h-20 focus:border-blue-500 focus:ring-2 focus:ring-blue-500/40 outline-none transition-colors resize-none" />
              </div>
              <div className="flex gap-3">
                <button type="submit" disabled={creating}
                  className="flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2.5 text-sm font-medium text-white hover:bg-blue-500 disabled:opacity-50 transition-colors cursor-pointer">
                  {creating ? <Loader2 className="w-4 h-4 animate-spin" /> : <Plus className="w-4 h-4" />}
                  {creating ? 'Creando...' : 'Crear'}
                </button>
                <button type="button" onClick={() => setShowCreate(false)}
                  className="rounded-lg bg-gray-800 px-4 py-2.5 text-sm font-medium text-gray-300 hover:bg-gray-700 transition-colors cursor-pointer">
                  Cancelar
                </button>
              </div>
            </form>
          </div>
        )}

        {loading ? (
          <div className="flex items-center justify-center py-20">
            <Loader2 className="w-6 h-6 animate-spin text-blue-400" />
          </div>
        ) : projects.length === 0 ? (
          <div className="text-center py-20">
            <FolderOpen className="w-12 h-12 text-gray-600 mx-auto mb-4" />
            <p className="text-gray-400 mb-4">No tienes proyectos aún</p>
            <button onClick={() => setShowCreate(true)}
              className="inline-flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2.5 text-sm font-medium text-white hover:bg-blue-500 transition-colors cursor-pointer">
              <Plus className="w-4 h-4" />
              Crear primer proyecto
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {projects.map((project) => (
              <Link key={project.id} to={`/projects/${project.id}`}
                className="group bg-gray-900 rounded-xl border border-gray-800 p-5 hover:border-blue-500 hover:shadow-md hover:shadow-blue-950/40 transition-all cursor-pointer">
                <div className="flex items-start justify-between mb-3">
                  <div className="w-10 h-10 rounded-lg bg-blue-950 flex items-center justify-center group-hover:bg-blue-900 transition-colors">
                    <FolderOpen className="w-5 h-5 text-blue-400" />
                  </div>
                  <span className="text-xs font-medium text-green-400 bg-green-950 px-2 py-0.5 rounded-full">
                    Activo
                  </span>
                </div>
                <h3 className="font-semibold text-gray-100 mb-1 group-hover:text-blue-400 transition-colors">
                  {project.name}
                </h3>
                <p className="text-sm text-gray-400 line-clamp-2 mb-3">
                  {project.description || 'Sin descripción'}
                </p>
                <div className="flex items-center gap-1.5 text-xs text-gray-500">
                  <Calendar className="w-3.5 h-3.5" />
                  {new Date(project.createdAt).toLocaleDateString('es-ES', { day: 'numeric', month: 'short', year: 'numeric' })}
                </div>
              </Link>
            ))}
          </div>
        )}
      </main>
    </div>
  )
}
