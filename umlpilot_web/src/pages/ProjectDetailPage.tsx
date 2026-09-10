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
    Promise.all([
      projectsApi.get(id),
      projectsApi.getCollaborators(id),
      diagramsApi.list(id),
    ]).then(([p, c, d]) => {
      setProject(p)
      setCollaborators(c)
      setDiagrams(d)
    }).finally(() => setLoading(false))
  }, [id])

  const handleInvite = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!id) return
    try {
      const collab = await projectsApi.inviteCollaborator(id, { email: inviteEmail, role: 'editor' })
      setCollaborators([...collaborators, collab])
      setShowInvite(false)
      setInviteEmail('')
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Error al invitar')
    }
  }

  const handleCreateDiagram = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!id) return
    try {
      const diagram = await diagramsApi.create(id, { name: diagramName, type: diagramType })
      setDiagrams([...diagrams, diagram])
      setShowCreateDiagram(false)
      setDiagramName('')
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Error al crear diagrama')
    }
  }

  const handleDeleteDiagram = async (diagramId: string) => {
    if (!id || !confirm('¿Eliminar este diagrama?')) return
    try {
      await diagramsApi.delete(id, diagramId)
      setDiagrams(diagrams.filter(d => d.id !== diagramId))
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Error al eliminar')
    }
  }

  if (loading) return <p style={{ padding: '2rem' }}>Cargando proyecto...</p>
  if (!project) return <p style={{ padding: '2rem' }}>Proyecto no encontrado</p>

  return (
    <div style={{ maxWidth: '800px', margin: '2rem auto', padding: '0 2rem', fontFamily: 'sans-serif' }}>
      <Link to="/projects" style={{ color: '#0066cc', textDecoration: 'none' }}>&larr; Volver</Link>
      <h1>{project.name}</h1>
      <p style={{ color: '#666' }}>{project.description}</p>

      <section style={{ marginBottom: '2rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
          <h2>Colaboradores</h2>
          <button onClick={() => setShowInvite(true)} style={{ padding: '0.4rem 0.8rem', background: '#0066cc', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>
            + Invitar
          </button>
        </div>
        {showInvite && (
          <form onSubmit={handleInvite} style={{ display: 'flex', gap: '0.5rem', marginBottom: '1rem' }}>
            <input type="email" placeholder="Email del usuario" value={inviteEmail} onChange={(e) => setInviteEmail(e.target.value)} required style={{ flex: 1, padding: '0.5rem' }} />
            <button type="submit" style={{ padding: '0.5rem 1rem', background: '#28a745', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>Invitar</button>
            <button type="button" onClick={() => setShowInvite(false)} style={{ padding: '0.5rem 1rem', background: '#6c757d', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>Cancelar</button>
          </form>
        )}
        <ul style={{ listStyle: 'none', padding: 0 }}>
          {collaborators.map((c) => (
            <li key={c.userId} style={{ padding: '0.5rem', background: '#f5f5f5', borderRadius: '4px', marginBottom: '0.5rem', display: 'flex', justifyContent: 'space-between' }}>
              <span>{c.firstName} {c.lastName} ({c.email})</span>
              <span style={{ fontWeight: 'bold' }}>{c.role}</span>
            </li>
          ))}
        </ul>
      </section>

      <section>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
          <h2>Diagramas</h2>
          <button onClick={() => setShowCreateDiagram(true)} style={{ padding: '0.4rem 0.8rem', background: '#0066cc', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>
            + Nuevo Diagrama
          </button>
        </div>
        {showCreateDiagram && (
          <form onSubmit={handleCreateDiagram} style={{ background: '#f5f5f5', padding: '1rem', borderRadius: '8px', marginBottom: '1rem' }}>
            <input type="text" placeholder="Nombre del diagrama" value={diagramName} onChange={(e) => setDiagramName(e.target.value)} required style={{ width: '100%', padding: '0.5rem', boxSizing: 'border-box', marginBottom: '0.5rem' }} />
            <select value={diagramType} onChange={(e) => setDiagramType(e.target.value)} style={{ padding: '0.5rem', marginBottom: '0.5rem' }}>
              <option value="class">Clase</option>
              <option value="sequence">Secuencia</option>
              <option value="use_case">Caso de Uso</option>
              <option value="activity">Actividad</option>
              <option value="state">Estado</option>
            </select>
            <div>
              <button type="submit" style={{ padding: '0.4rem 0.8rem', background: '#28a745', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', marginRight: '0.5rem' }}>Crear</button>
              <button type="button" onClick={() => setShowCreateDiagram(false)} style={{ padding: '0.4rem 0.8rem', background: '#6c757d', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>Cancelar</button>
            </div>
          </form>
        )}
        {diagrams.length === 0 ? (
          <p>No hay diagramas. Crea uno para empezar.</p>
        ) : (
          <div style={{ display: 'grid', gap: '0.75rem' }}>
            {diagrams.map((diagram) => (
              <div key={diagram.id} style={{ padding: '1rem', background: '#f5f5f5', borderRadius: '8px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Link to={`/projects/${id}/diagrams/${diagram.id}`} style={{ textDecoration: 'none', color: 'inherit', flex: 1 }}>
                  <strong>{diagram.name}</strong>
                  <span style={{ marginLeft: '0.5rem', color: '#666', fontSize: '0.9em' }}>({diagram.type})</span>
                  {diagram.isLocked && <span style={{ marginLeft: '0.5rem', color: '#cc0000', fontSize: '0.85em' }}>🔒 Bloqueado</span>}
                </Link>
                <button onClick={() => handleDeleteDiagram(diagram.id)} style={{ padding: '0.3rem 0.6rem', background: '#cc0000', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', fontSize: '0.85em' }}>
                  Eliminar
                </button>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  )
}
