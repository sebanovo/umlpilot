import { useState, useEffect } from 'react'
import { useParams, Link } from 'react-router-dom'
import { projectsApi, diagramsApi, type Project, type Collaborator, type Diagram } from '../shared/api/client'

export function ProjectDetailPage() {
  const { id } = useParams<{ id: string }>()
  const [project, setProject] = useState<Project | null>(null)
  const [collaborators, setCollaborators] = useState<Collaborator[]>([])
  const [diagrams, setDiagrams] = useState<Diagram[]>([])
  const [loading, setLoading] = useState(true)
  const [showInvite, setShowInvite] = useState(false)
  const [inviteEmail, setInviteEmail] = useState('')
  const [showCreateDiagram, setShowCreateDiagram] = useState(false)
  const [diagramName, setDiagramName] = useState('')
  const [diagramType, setDiagramType] = useState('class')

  useEffect(() => {
    if (!id) return
    Promise.all([projectsApi.get(id), projectsApi.getCollaborators(id), diagramsApi.list(id)])
      .then(([p, c, d]) => { setProject(p); setCollaborators(c); setDiagrams(d) })
      .finally(() => setLoading(false))
  }, [id])

  const handleInvite = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!id) return
    try {
      const collab = await projectsApi.inviteCollaborator(id, { email: inviteEmail, role: 'editor' })
      setCollaborators([...collaborators, collab])
      setShowInvite(false); setInviteEmail('')
    } catch (err) { alert(err instanceof Error ? err.message : 'Error al invitar') }
  }

  const handleCreateDiagram = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!id) return
    try {
      const diagram = await diagramsApi.create(id, { name: diagramName, type: diagramType })
      setDiagrams([...diagrams, diagram])
      setShowCreateDiagram(false); setDiagramName('')
    } catch (err) { alert(err instanceof Error ? err.message : 'Error al crear diagrama') }
  }

  const handleDeleteDiagram = async (diagramId: string) => {
    if (!id || !confirm('¿Eliminar este diagrama?')) return
    try { await diagramsApi.delete(id, diagramId); setDiagrams(diagrams.filter(d => d.id !== diagramId)) }
    catch (err) { alert(err instanceof Error ? err.message : 'Error al eliminar') }
  }

  if (loading) return <p className="p-8 text-gray-500">Cargando proyecto...</p>
  if (!project) return <p className="p-8 text-gray-500">Proyecto no encontrado</p>

  return (
    <div className="mx-auto max-w-4xl px-8 py-8 font-sans">
      <Link to="/projects" className="text-blue-600 hover:underline text-sm">&larr; Volver</Link>
      <h1 className="text-2xl font-bold text-gray-900 mt-4 mb-2">{project.name}</h1>
      <p className="text-gray-500 mb-8">{project.description}</p>

      <section className="mb-8">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-semibold text-gray-900">Colaboradores</h2>
          <button onClick={() => setShowInvite(true)} className="text-sm text-blue-600 hover:underline cursor-pointer">+ Invitar</button>
        </div>
        {showInvite && (
          <form onSubmit={handleInvite} className="flex gap-2 mb-4">
            <input type="email" placeholder="Email del usuario" value={inviteEmail} onChange={(e) => setInviteEmail(e.target.value)} required
              className="flex-1 rounded-lg border border-gray-300 px-3 py-2 text-sm text-gray-900 focus:border-blue-500 focus:ring-2 focus:ring-blue-500 outline-none" />
            <button type="submit" className="rounded-lg bg-green-600 px-4 py-2 text-sm font-medium text-white hover:bg-green-700 cursor-pointer">Invitar</button>
            <button type="button" onClick={() => setShowInvite(false)} className="rounded-lg bg-gray-400 px-4 py-2 text-sm font-medium text-white hover:bg-gray-500 cursor-pointer">Cancelar</button>
          </form>
        )}
        <ul className="space-y-2">
          {collaborators.map((c) => (
            <li key={c.userId} className="flex justify-between items-center rounded-lg bg-gray-50 border border-gray-200 px-4 py-2">
              <span className="text-sm text-gray-700">{c.firstName} {c.lastName} ({c.email})</span>
              <span className="text-sm font-medium text-blue-600">{c.role}</span>
            </li>
          ))}
        </ul>
      </section>

      <section>
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-semibold text-gray-900">Diagramas</h2>
          <button onClick={() => setShowCreateDiagram(true)} className="text-sm text-blue-600 hover:underline cursor-pointer">+ Nuevo Diagrama</button>
        </div>
        {showCreateDiagram && (
          <form onSubmit={handleCreateDiagram} className="bg-gray-50 rounded-xl p-4 mb-4 border border-gray-200">
            <input type="text" placeholder="Nombre del diagrama" value={diagramName} onChange={(e) => setDiagramName(e.target.value)} required
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm text-gray-900 mb-3 focus:border-blue-500 outline-none" />
            <select value={diagramType} onChange={(e) => setDiagramType(e.target.value)}
              className="rounded-lg border border-gray-300 px-3 py-2 text-sm text-gray-900 mb-3">
              <option value="class">Clase</option>
              <option value="sequence">Secuencia</option>
              <option value="use_case">Caso de Uso</option>
              <option value="activity">Actividad</option>
              <option value="state">Estado</option>
            </select>
            <div className="flex gap-2">
              <button type="submit" className="rounded-lg bg-green-600 px-4 py-2 text-sm font-medium text-white hover:bg-green-700 cursor-pointer">Crear</button>
              <button type="button" onClick={() => setShowCreateDiagram(false)} className="rounded-lg bg-gray-400 px-4 py-2 text-sm font-medium text-white hover:bg-gray-500 cursor-pointer">Cancelar</button>
            </div>
          </form>
        )}
        {diagrams.length === 0 ? (
          <p className="text-gray-500">No hay diagramas. Crea uno para empezar.</p>
        ) : (
          <div className="grid gap-2">
            {diagrams.map((diagram) => (
              <div key={diagram.id} className="flex items-center justify-between rounded-lg bg-gray-50 border border-gray-200 px-4 py-3">
                <Link to={`/projects/${id}/diagrams/${diagram.id}`} className="flex-1 hover:underline">
                  <strong className="text-gray-900">{diagram.name}</strong>
                  <span className="ml-2 text-sm text-gray-500">({diagram.type})</span>
                  {diagram.isLocked && <span className="ml-2 text-xs text-red-600">🔒 Bloqueado</span>}
                </Link>
                <button onClick={() => handleDeleteDiagram(diagram.id)}
                  className="rounded-lg bg-red-600 px-3 py-1 text-xs font-medium text-white hover:bg-red-700 cursor-pointer">Eliminar</button>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  )
}
