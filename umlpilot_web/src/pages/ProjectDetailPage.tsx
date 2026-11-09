import { useState, useEffect } from 'react'
import { useParams, Link } from 'react-router-dom'
import { projectsApi, diagramsApi, type Project, type Collaborator, type Diagram } from '../shared/api/client'
import { useToast } from '../shared/ui/toast/useToast'
import { useAuth } from '../features/auth/useAuth'
import { ConfirmDialog } from '../shared/ui/ConfirmDialog'
import { NotificationBell } from '../features/notifications/components/NotificationBell'
import { InviteCollaboratorForm } from '../features/collaboration/components/InviteCollaboratorForm'
import { ArrowLeft, Plus, Trash2, Users, FileText, Lock, Loader2, ChevronDown, ChevronUp, Sparkles } from 'lucide-react'

export function ProjectDetailPage() {
  const { id } = useParams<{ id: string }>()
  const { toast } = useToast()
  const { user } = useAuth()
  const [project, setProject] = useState<Project | null>(null)
  const [collaborators, setCollaborators] = useState<Collaborator[]>([])
  const [diagrams, setDiagrams] = useState<Diagram[]>([])
  const [loading, setLoading] = useState(true)
  const [showCreateDiagram, setShowCreateDiagram] = useState(false)
  const [diagramName, setDiagramName] = useState('')
  const [diagramType, setDiagramType] = useState('class')
  const [creatingDiagram, setCreatingDiagram] = useState(false)
  const [showCollaborators, setShowCollaborators] = useState(true)
  const [changingRoleFor, setChangingRoleFor] = useState<string | null>(null)
  const [removingCollaborator, setRemovingCollaborator] = useState<Collaborator | null>(null)
  const [removing, setRemoving] = useState(false)

  useEffect(() => {
    if (!id) return
    Promise.all([projectsApi.get(id), projectsApi.getCollaborators(id), diagramsApi.list(id)])
      .then(([p, c, d]) => { setProject(p); setCollaborators(c); setDiagrams(d) })
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [id])

  const handleCreateDiagram = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!id) return
    setCreatingDiagram(true)
    try {
      const diagram = await diagramsApi.create(id, { name: diagramName, type: diagramType })
      setDiagrams([...diagrams, diagram])
      setShowCreateDiagram(false); setDiagramName('')
      toast({ title: 'Diagrama creado', description: diagram.name, variant: 'success' })
    } catch (err) {
      toast({
        title: 'No se pudo crear el diagrama',
        description: err instanceof Error ? err.message : 'Error inesperado',
        variant: 'error',
      })
    } finally { setCreatingDiagram(false) }
  }

  const handleInvited = (collaborator: Collaborator) => {
    setCollaborators((current) => [
      ...current.filter((item) => item.userId !== collaborator.userId),
      collaborator,
    ])
  }

  const handleRemoveCollaborator = async () => {
    if (!id || !removingCollaborator) return
    const target = removingCollaborator
    setRemoving(true)
    try {
      await projectsApi.removeCollaborator(id, target.userId)
      setCollaborators((current) => current.filter((item) => item.userId !== target.userId))
      setRemovingCollaborator(null)
      toast({
        title: target.invitationStatus === 'pending' ? 'Invitación cancelada' : 'Colaborador eliminado',
        description: `${target.firstName} ${target.lastName} ya no tiene acceso al proyecto`,
        variant: 'success',
      })
    } catch (err) {
      toast({
        title: 'No se pudo eliminar al colaborador',
        description: err instanceof Error ? err.message : 'Error inesperado',
        variant: 'error',
      })
    } finally {
      setRemoving(false)
    }
  }

  const handleRoleChange = async (userId: string, role: 'editor' | 'viewer') => {
    if (!id) return
    const previous = collaborators
    setCollaborators((current) =>
      current.map((item) => (item.userId === userId ? { ...item, role } : item)),
    )
    setChangingRoleFor(userId)
    try {
      await projectsApi.updateCollaboratorRole(id, userId, role)
      toast({
        title: 'Rol actualizado',
        description: role === 'editor' ? 'Ahora puede editar el proyecto' : 'Ahora solo puede leerlo',
        variant: 'success',
      })
    } catch (err) {
      setCollaborators(previous)
      toast({
        title: 'No se pudo cambiar el rol',
        description: err instanceof Error ? err.message : 'Error inesperado',
        variant: 'error',
      })
    } finally {
      setChangingRoleFor(null)
    }
  }

  const handleDeleteDiagram = async (diagramId: string) => {
    if (!id || !confirm('¿Eliminar este diagrama?')) return
    try {
      await diagramsApi.delete(id, diagramId)
      setDiagrams(diagrams.filter(d => d.id !== diagramId))
      toast({ title: 'Diagrama eliminado', variant: 'success' })
    } catch (err) {
      toast({
        title: 'No se pudo eliminar el diagrama',
        description: err instanceof Error ? err.message : 'Error inesperado',
        variant: 'error',
      })
    }
  }

  if (loading) return (
    <div className="min-h-screen bg-gray-950 flex items-center justify-center">
      <Loader2 className="w-6 h-6 animate-spin text-blue-400" />
    </div>
  )
  if (!project) return <div className="min-h-screen bg-gray-950 flex items-center justify-center text-gray-400">Proyecto no encontrado</div>

  const currentRole = collaborators.find((item) => item.userId === user?.id)?.role
  const canInvite = currentRole === 'owner' || currentRole === 'editor'
  const canManageRoles = currentRole === 'owner'

  return (
    <div className="min-h-screen bg-gray-950">
      <header className="bg-gray-900 border-b border-gray-800 sticky top-0 z-10">
        <div className="max-w-6xl mx-auto px-6 py-3 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Link to="/projects" className="text-gray-400 hover:text-gray-200 transition-colors">
              <ArrowLeft className="w-5 h-5" />
            </Link>
            <div>
              <h1 className="text-lg font-bold text-gray-100">{project.name}</h1>
              {project.description && <p className="text-xs text-gray-500 truncate max-w-md">{project.description}</p>}
            </div>
          </div>
          <div className="flex items-center gap-2">
            <Link
              to={`/projects/${id}/ai`}
              className="flex items-center gap-1.5 rounded-lg border border-blue-900 bg-blue-950/60 px-3 py-1.5 text-xs font-medium text-blue-200 transition-colors hover:border-blue-500 hover:text-blue-100"
            >
              <Sparkles className="w-3.5 h-3.5" />
              Modo IA
            </Link>
            <NotificationBell />
          </div>
        </div>
      </header>

      <main className="max-w-6xl mx-auto px-6 py-8">
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          <div className="lg:col-span-2">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-base font-semibold text-gray-100 flex items-center gap-2">
                <FileText className="w-4 h-4 text-gray-400" />
                Diagramas ({diagrams.length})
              </h2>
              <button onClick={() => setShowCreateDiagram(true)}
                className="flex items-center gap-1.5 rounded-lg bg-blue-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-blue-500 transition-colors cursor-pointer">
                <Plus className="w-3.5 h-3.5" />
                Nuevo
              </button>
            </div>

            {showCreateDiagram && (
              <form onSubmit={handleCreateDiagram} className="bg-gray-900 rounded-xl border border-gray-800 shadow-sm p-5 mb-4">
                <div className="grid grid-cols-2 gap-3 mb-4">
                  <input type="text" placeholder="Nombre del diagrama" value={diagramName} onChange={(e) => setDiagramName(e.target.value)} required
                    className="rounded-lg border border-gray-700 bg-gray-800 px-3 py-2 text-sm text-gray-100 placeholder-gray-500 focus:border-blue-500 focus:ring-2 focus:ring-blue-500/40 outline-none transition-colors" />
                  <select value={diagramType} onChange={(e) => setDiagramType(e.target.value)}
                    className="rounded-lg border border-gray-700 bg-gray-800 px-3 py-2 text-sm text-gray-100 focus:border-blue-500 focus:ring-2 focus:ring-blue-500/40 outline-none transition-colors">
                    <option value="class">Clase</option>
                    <option value="sequence">Secuencia</option>
                    <option value="use_case">Caso de Uso</option>
                    <option value="activity">Actividad</option>
                    <option value="state">Estado</option>
                  </select>
                </div>
                <div className="flex gap-2">
                  <button type="submit" disabled={creatingDiagram}
                    className="flex items-center gap-1.5 rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-500 disabled:opacity-50 transition-colors cursor-pointer">
                    {creatingDiagram ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Plus className="w-3.5 h-3.5" />}
                    Crear
                  </button>
                  <button type="button" onClick={() => setShowCreateDiagram(false)}
                    className="rounded-lg bg-gray-800 px-4 py-2 text-sm font-medium text-gray-300 hover:bg-gray-700 transition-colors cursor-pointer">
                    Cancelar
                  </button>
                </div>
              </form>
            )}

            {diagrams.length === 0 ? (
              <div className="text-center py-12 bg-gray-900 rounded-xl border border-gray-800">
                <FileText className="w-10 h-10 text-gray-600 mx-auto mb-3" />
                <p className="text-gray-400 text-sm mb-3">No hay diagramas aún</p>
                <button onClick={() => setShowCreateDiagram(true)}
                  className="inline-flex items-center gap-1.5 rounded-lg bg-blue-600 px-3 py-2 text-sm font-medium text-white hover:bg-blue-500 transition-colors cursor-pointer">
                  <Plus className="w-3.5 h-3.5" />
                  Crear primer diagrama
                </button>
              </div>
            ) : (
              <div className="space-y-2">
                {diagrams.map((diagram) => (
                  <Link key={diagram.id} to={`/projects/${id}/diagrams/${diagram.id}`}
                    className="group flex items-center justify-between bg-gray-900 rounded-xl border border-gray-800 px-5 py-3.5 hover:border-blue-500 hover:shadow-sm transition-all">
                    <div className="flex items-center gap-3">
                      <div className="w-9 h-9 rounded-lg bg-blue-950 flex items-center justify-center group-hover:bg-blue-900 transition-colors">
                        <FileText className="w-4 h-4 text-blue-400" />
                      </div>
                      <div>
                        <strong className="text-sm font-medium text-gray-100 group-hover:text-blue-400 transition-colors">{diagram.name}</strong>
                        <div className="flex items-center gap-2 mt-0.5">
                          <span className="text-xs text-gray-500">{diagram.type}</span>
                          {diagram.isLocked && (
                            <span className="inline-flex items-center gap-1 text-xs text-amber-400 bg-amber-950 px-1.5 py-0.5 rounded-full">
                              <Lock className="w-3 h-3" />
                              Bloqueado
                            </span>
                          )}
                        </div>
                      </div>
                    </div>
                    <button onClick={(e) => { e.preventDefault(); handleDeleteDiagram(diagram.id) }}
                      className="opacity-0 group-hover:opacity-100 p-1.5 rounded-lg text-gray-400 hover:text-red-400 hover:bg-red-950/50 transition-all cursor-pointer">
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </Link>
                ))}
              </div>
            )}
          </div>

          <div>
            <button onClick={() => setShowCollaborators(!showCollaborators)}
              className="flex items-center justify-between w-full text-base font-semibold text-gray-100 mb-4 cursor-pointer">
              <span className="flex items-center gap-2">
                <Users className="w-4 h-4 text-gray-400" />
                Colaboradores ({collaborators.length})
              </span>
              {showCollaborators ? <ChevronUp className="w-4 h-4 text-gray-400" /> : <ChevronDown className="w-4 h-4 text-gray-400" />}
            </button>
            {showCollaborators && (
              <div className="space-y-2">
                {collaborators.map((c) => (
                  <div key={c.userId} className="flex items-center gap-3 bg-gray-900 rounded-xl border border-gray-800 px-4 py-3">
                    <div className="w-8 h-8 rounded-full bg-blue-900 flex items-center justify-center text-xs font-medium text-blue-200">
                      {c.firstName?.[0]}{c.lastName?.[0]}
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium text-gray-100 truncate">{c.firstName} {c.lastName}</p>
                      <p className="text-xs text-gray-500 truncate">{c.email}</p>
                    </div>
                    <div className="flex shrink-0 items-center gap-1.5">
                      {c.invitationStatus === 'pending' && (
                        <span className="text-xs font-medium px-2 py-0.5 rounded-full bg-amber-950 text-amber-300">
                          Pendiente
                        </span>
                      )}
                      {canManageRoles && c.role !== 'owner' ? (
                        <select
                          value={c.role}
                          disabled={changingRoleFor === c.userId}
                          onChange={(event) =>
                            handleRoleChange(c.userId, event.target.value as 'editor' | 'viewer')
                          }
                          aria-label={`Rol de ${c.firstName} ${c.lastName}`}
                          className="rounded-lg border border-gray-700 bg-gray-800 px-2 py-1 text-xs text-gray-100 outline-none transition-colors focus:border-blue-500 focus:ring-2 focus:ring-blue-500/40 disabled:opacity-50 cursor-pointer"
                        >
                          <option value="editor">Editor</option>
                          <option value="viewer">Lector</option>
                        </select>
                      ) : (
                        <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${
                          c.role === 'owner' ? 'bg-purple-950 text-purple-300' : 'bg-blue-950 text-blue-300'
                        }`}>
                          {c.role}
                        </span>
                      )}
                    </div>
                    {canManageRoles && c.role !== 'owner' && (
                      <button
                        type="button"
                        onClick={() => setRemovingCollaborator(c)}
                        title={c.invitationStatus === 'pending' ? 'Cancelar invitación' : 'Eliminar colaborador'}
                        aria-label={c.invitationStatus === 'pending'
                          ? `Cancelar invitación de ${c.firstName} ${c.lastName}`
                          : `Eliminar a ${c.firstName} ${c.lastName}`}
                        className="shrink-0 rounded-lg p-1.5 text-gray-500 transition-colors hover:bg-red-950/50 hover:text-red-400 cursor-pointer"
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    )}
                  </div>
                ))}
                {id && canInvite && (
                  <InviteCollaboratorForm projectId={id} onInvited={handleInvited} />
                )}
              </div>
            )}
          </div>
        </div>
      </main>

      <ConfirmDialog
        open={removingCollaborator !== null}
        title={removingCollaborator?.invitationStatus === 'pending' ? 'Cancelar invitación' : 'Eliminar colaborador'}
        description={
          removingCollaborator
            ? removingCollaborator.invitationStatus === 'pending'
              ? `${removingCollaborator.firstName} ${removingCollaborator.lastName} ya no podrá aceptar la invitación.`
              : `${removingCollaborator.firstName} ${removingCollaborator.lastName} perderá el acceso al proyecto.`
            : undefined
        }
        confirmLabel={removingCollaborator?.invitationStatus === 'pending' ? 'Cancelar invitación' : 'Eliminar'}
        destructive
        loading={removing}
        onConfirm={handleRemoveCollaborator}
        onCancel={() => { if (!removing) setRemovingCollaborator(null) }}
      />
    </div>
  )
}
