import { useState, useEffect } from 'react'
import { useParams, Link } from 'react-router-dom'
import { diagramsApi, type Diagram } from '../shared/api/client'
import { useWebSocket } from '../shared/lib/useWebSocket'
import { useAuth } from '../features/auth/useAuth'

export function DiagramEditorPage() {
  const { id: projectId, diagramId } = useParams<{ id: string; diagramId: string }>()
  const { user } = useAuth()
  const [diagram, setDiagram] = useState<Diagram | null>(null)
  const [loading, setLoading] = useState(true)
  const { connected, sendMessage } = useWebSocket(diagramId || '', String(user?.id || ''))

  useEffect(() => {
    if (!projectId || !diagramId) return
    diagramsApi.get(projectId, diagramId).then(setDiagram).finally(() => setLoading(false))
  }, [projectId, diagramId])

  useEffect(() => {
    if (!diagramId || !connected) return
    sendMessage('/app/joinDiagram', { diagramId })
  }, [diagramId, connected, sendMessage])

  const handleLock = async () => {
    if (!projectId || !diagramId) return
    try {
      const updated = await diagramsApi.lock(projectId, diagramId)
      setDiagram(updated)
      sendMessage('/app/diagram/update', { diagramId, type: 'diagram_locked', data: { userId: user?.id } })
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Error al bloquear')
    }
  }

  const handleUnlock = async () => {
    if (!projectId || !diagramId) return
    try {
      const updated = await diagramsApi.unlock(projectId, diagramId)
      setDiagram(updated)
      sendMessage('/app/diagram/update', { diagramId, type: 'diagram_unlocked', data: { userId: user?.id } })
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Error al desbloquear')
    }
  }

  if (loading) return <p style={{ padding: '2rem' }}>Cargando diagrama...</p>
  if (!diagram) return <p style={{ padding: '2rem' }}>Diagrama no encontrado</p>

  return (
    <div style={{ height: '100vh', display: 'flex', flexDirection: 'column', fontFamily: 'sans-serif' }}>
      <header style={{ padding: '0.75rem 1.5rem', background: '#1a1a2e', color: 'white', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <Link to={`/projects/${projectId}`} style={{ color: '#88ccff', textDecoration: 'none', marginRight: '1rem' }}>&larr; Volver</Link>
          <strong>{diagram.name}</strong>
          <span style={{ marginLeft: '0.5rem', color: '#aaa' }}>({diagram.type} v{diagram.version})</span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <span style={{ fontSize: '0.85em', color: connected ? '#4caf50' : '#f44336' }}>
            {connected ? '● Conectado' : '○ Desconectado'}
          </span>
          {diagram.isLocked ? (
            <button onClick={handleUnlock} style={{ padding: '0.4rem 0.8rem', background: '#ff9800', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>
              🔓 Desbloquear
            </button>
          ) : (
            <button onClick={handleLock} style={{ padding: '0.4rem 0.8rem', background: '#4caf50', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>
              🔒 Bloquear para editar
            </button>
          )}
        </div>
      </header>
      <main style={{ flex: 1, background: '#f0f0f0', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <div style={{ background: 'white', width: '80%', height: '80%', borderRadius: '8px', boxShadow: '0 2px 8px rgba(0,0,0,0.1)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <p style={{ color: '#999' }}>Canvas del diagrama —Aquí irá el editor UML</p>
        </div>
      </main>
    </div>
  )
}
