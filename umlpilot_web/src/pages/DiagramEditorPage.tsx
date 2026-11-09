import { useEffect, useMemo, useRef, useState, type PointerEvent as ReactPointerEvent } from 'react'
import { useParams, Link, useNavigate } from 'react-router-dom'
import { ReactFlowProvider } from '@xyflow/react'
import { useAuth } from '../features/auth/useAuth'
import { useDiagramState } from '../features/diagram-editor/hooks/useDiagramState'
import { DiagramCanvas } from '../features/diagram-editor/components/DiagramCanvas'
import { PresenceBar, type PresenceUser } from '../features/diagram-editor/components/PresenceBar'
import { Toolbox } from '../features/diagram-editor/components/Toolbox'
import { PropertiesPanel } from '../features/diagram-editor/components/PropertiesPanel'
import { RelationshipPanel } from '../features/diagram-editor/components/RelationshipPanel'
import { buildXmi } from '../features/diagram-editor/lib/xmi'
import { ShareDiagramDialog } from '../features/collaboration/components/ShareDiagramDialog'
import { backendGenerationApi, projectsApi, xmiImportApi, type Collaborator } from '../shared/api/client'
import { colorFor } from '../shared/lib/useWebSocket'
import { useToast } from '../shared/ui/toast/useToast'
import { ConfirmDialog } from '../shared/ui/ConfirmDialog'
import { ArrowLeft, Download, Upload, Sparkles, Package, Trash2, Share2 } from 'lucide-react'

export function DiagramEditorPage() {
  const { id: projectId, diagramId } = useParams<{ id: string; diagramId: string }>()
  const { user } = useAuth()
  const navigate = useNavigate()

  const {
    nodes, edges, elements, connected, presenceUsers,
    selectedElement, selectedAttributes, selectedMethods, loading,
    activeRelationship, setActiveRelationship, createRelationship, reconnectRelationship, handleNodeClick,
    selectedRelationship, relationshipMultiplicities, onEdgeClick, clearRelationshipSelection,
    updateRelationshipName, updateMultiplicity, remoteCursors, handleCursorMove,
    onNodesChange, onEdgesChange, onConnect, createElement, deleteElement,
    updateElement, addAttribute, deleteAttribute, addMethod, deleteMethod,
    addParameter, deleteParameter,
    attributes, methods, relationships, rebuildDiagram, deleteAllElements, loadError,
  } = useDiagramState({ projectId: projectId!, diagramId: diagramId! })

  const { toast } = useToast()

  const [selectMode, setSelectMode] = useState(false)
  const [handTool, setHandTool] = useState(false)
  const [toolboxWidth, setToolboxWidth] = useState(176)
  const [panelWidth, setPanelWidth] = useState(288)
  const [generatingBackend, setGeneratingBackend] = useState(false)
  const [importingXmi, setImportingXmi] = useState(false)
  const [confirmClearOpen, setConfirmClearOpen] = useState(false)
  const [clearing, setClearing] = useState(false)
  const [shareOpen, setShareOpen] = useState(false)
  const [collaborators, setCollaborators] = useState<Collaborator[]>([])
  const xmiFileInputRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    if (!projectId) return
    projectsApi.getCollaborators(projectId).then(setCollaborators).catch(() => {})
  }, [projectId])

  const nameById = useMemo(() => {
    const map = new Map<string, string>()
    for (const collaborator of collaborators) {
      const name = `${collaborator.firstName ?? ''} ${collaborator.lastName ?? ''}`.trim()
      map.set(collaborator.userId, name || collaborator.email || 'Colaborador')
    }
    return map
  }, [collaborators])

  const nameByIdRef = useRef(nameById)
  useEffect(() => { nameByIdRef.current = nameById }, [nameById])

  const currentRole = collaborators.find((collaborator) => collaborator.userId === user?.id)?.role
  const isViewer = currentRole === 'viewer'

  const presenceList = useMemo<PresenceUser[]>(() => {
    const self: PresenceUser[] = user
      ? [{ id: user.id, name: `${user.firstName} ${user.lastName}`.trim() || user.email, color: colorFor(user.id), isSelf: true }]
      : []
    const others: PresenceUser[] = presenceUsers.map((id) => ({
      id,
      name: nameById.get(id) || 'Colaborador',
      color: colorFor(id),
    }))
    return [...self, ...others]
  }, [user, presenceUsers, nameById])

  // Avisa cuando un colaborador entra o sale del diagrama
  const prevPresenceRef = useRef<Set<string> | null>(null)
  useEffect(() => {
    const next = new Set(presenceUsers)
    if (prevPresenceRef.current === null) {
      prevPresenceRef.current = next
      return
    }
    const prev = prevPresenceRef.current
    const display = (id: string) => nameByIdRef.current.get(id) || 'Un colaborador'
    for (const id of next) {
      if (!prev.has(id)) toast({ title: `${display(id)} se unió al diagrama`, variant: 'info' })
    }
    for (const id of prev) {
      if (!next.has(id)) toast({ title: `${display(id)} salió del diagrama`, variant: 'info' })
    }
    prevPresenceRef.current = next
  }, [presenceUsers, toast])

  const startResize = (which: 'toolbox' | 'panel') => (e: ReactPointerEvent) => {
    e.preventDefault()
    const startX = e.clientX
    const startWidth = which === 'toolbox' ? toolboxWidth : panelWidth
    const onMove = (ev: PointerEvent) => {
      const dx = ev.clientX - startX
      const clamp = (v: number, min: number, max: number) => Math.min(max, Math.max(min, v))
      if (which === 'toolbox') setToolboxWidth(clamp(startWidth + dx, 140, 400))
      else setPanelWidth(clamp(startWidth - dx, 220, 600))
    }
    const onUp = () => {
      window.removeEventListener('pointermove', onMove)
      window.removeEventListener('pointerup', onUp)
    }
    window.addEventListener('pointermove', onMove)
    window.addEventListener('pointerup', onUp)
  }

  const handleExportXmi = () => {
    const positions: Record<string, { x: number; y: number; width?: number; height?: number }> = {}
    for (const n of nodes) {
      positions[n.id] = {
        x: n.position.x,
        y: n.position.y,
        width: n.measured?.width,
        height: n.measured?.height,
      }
    }
    const multiplicities: Record<string, { source?: string; target?: string }> = {}
    for (const e of edges) {
      const d = (e.data || {}) as { sourceMultiplicity?: string; targetMultiplicity?: string }
      multiplicities[e.id] = { source: d.sourceMultiplicity, target: d.targetMultiplicity }
    }
    const xml = buildXmi({
      modelName: `Diagram_${diagramId}`,
      diagramId: diagramId!,
      elements,
      attributes,
      methods,
      relationships,
      positions,
      multiplicities,
    })
    const blob = new Blob([xml], { type: 'application/xml' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `diagram_${diagramId}.xmi`
    document.body.appendChild(link)
    link.click()
    link.remove()
    URL.revokeObjectURL(url)
    toast({ title: 'Diagrama exportado', description: 'Se descargó el archivo .xmi', variant: 'success' })
  }

  const handleGenerateBackend = async () => {
    if (generatingBackend) return
    setGeneratingBackend(true)
    try {
      const { blob, filename } = await backendGenerationApi.download(projectId!, diagramId!)
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = filename
      document.body.appendChild(link)
      link.click()
      link.remove()
      URL.revokeObjectURL(url)
      toast({ title: 'Backend generado', description: `Se descargó ${filename}`, variant: 'success' })
    } catch (e) {
      toast({
        title: 'No se pudo generar el backend',
        description: e instanceof Error ? e.message : 'Error inesperado',
        variant: 'error',
      })
    } finally {
      setGeneratingBackend(false)
    }
  }

  const handleDeleteAll = async () => {
    if (clearing) return
    setClearing(true)
    try {
      const count = await deleteAllElements()
      setConfirmClearOpen(false)
      toast({
        title: 'Diagrama vaciado',
        description: `Se eliminaron ${count} elemento${count !== 1 ? 's' : ''} y sus relaciones`,
        variant: 'success',
      })
    } catch (e) {
      toast({
        title: 'No se pudieron eliminar los elementos',
        description: e instanceof Error ? e.message : 'Error inesperado',
        variant: 'error',
      })
    } finally {
      setClearing(false)
    }
  }

  const handleImportXmi = async (file: File) => {
    if (importingXmi) return
    setImportingXmi(true)
    try {
      const result = await xmiImportApi.importXmi(projectId!, diagramId!, file)
      await rebuildDiagram()
      const summary = `${result.elements} elemento${result.elements !== 1 ? 's' : ''} y ${result.relationships} relación${result.relationships !== 1 ? 'es' : ''} añadidos`
      if (result.warnings?.length) {
        toast({
          title: 'XMI importado con advertencias',
          description: `${summary}. ${result.warnings.length} advertencia${result.warnings.length !== 1 ? 's' : ''}: ${result.warnings[0]}`,
          variant: 'warning',
        })
      } else {
        toast({ title: 'XMI importado', description: summary, variant: 'success' })
      }
    } catch (e) {
      toast({
        title: 'No se pudo importar el XMI',
        description: e instanceof Error ? e.message : 'Error inesperado',
        variant: 'error',
      })
    } finally {
      setImportingXmi(false)
    }
  }

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      const target = e.target as HTMLElement | null
      if (target && (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA' || target.isContentEditable)) return
      if (e.key === 'v' || e.key === 'V') {
        setSelectMode(true); setHandTool(false); setActiveRelationship(null)
      } else if (e.key === 'h' || e.key === 'H') {
        setHandTool(true); setSelectMode(false); setActiveRelationship(null)
      } else if (e.key === 'Escape') {
        setSelectMode(false); setHandTool(false); setActiveRelationship(null)
      }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [setActiveRelationship])

  if (loading) return (
    <div className="h-screen flex items-center justify-center bg-gray-950">
      <div className="text-center">
        <div className="w-8 h-8 border-4 border-blue-500 border-t-transparent rounded-full animate-spin mx-auto mb-3" />
        <p className="text-sm text-gray-400">Cargando editor...</p>
      </div>
    </div>
  )

  if (loadError) return (
    <div className="h-screen flex flex-col items-center justify-center gap-3 bg-gray-950 px-6 text-center">
      <p className="text-lg font-semibold text-gray-100">No puedes abrir este diagrama</p>
      <p className="max-w-sm text-sm text-gray-400">{loadError}</p>
      <Link to="/projects" className="text-sm font-medium text-blue-400 hover:text-blue-300">
        Volver a mis proyectos
      </Link>
    </div>
  )

  return (
    <ReactFlowProvider>
      <div className="h-screen flex flex-col font-sans bg-gray-950">
        <header className="flex items-center justify-between bg-gray-900 border-b border-gray-800 text-gray-100 px-4 py-2 text-sm shrink-0">
          <div className="flex items-center gap-4">
            <Link to={`/projects/${projectId}`} className="text-blue-400 hover:text-blue-300 transition-colors">
              <ArrowLeft className="w-4 h-4" />
            </Link>
            <strong>Editor de Diagrama</strong>
            <span className="text-gray-400 text-xs">{elements.length} elemento{elements.length !== 1 ? 's' : ''}</span>
            {activeRelationship && (
              <span className="text-xs text-blue-300 bg-blue-950/60 border border-blue-800 rounded-full px-2 py-0.5">
                Arrastra de una clase a otra para crear la relación
              </span>
            )}
          </div>
          <div className="flex items-center gap-4">
            <PresenceBar users={presenceList} connected={connected} />
            {isViewer && (
              <span className="rounded-full border border-gray-700 bg-gray-800 px-2 py-0.5 text-xs text-gray-300">
                Solo lectura
              </span>
            )}
            {!isViewer && (
              <button
                onClick={() => navigate(`/projects/${projectId}/ai?diagramId=${diagramId}`)}
                title="Abrir modo IA del proyecto"
                className="flex items-center gap-1.5 rounded-md border border-gray-700 bg-gray-800 px-2.5 py-1 text-xs text-gray-300 transition-colors hover:border-blue-500 hover:bg-blue-950/60 hover:text-blue-300 cursor-pointer">
                <Sparkles className="w-3.5 h-3.5" />
                Modo IA
              </button>
            )}
            {!isViewer && (
              <button onClick={() => setShareOpen(true)} title="Compartir diagrama"
                className="flex items-center gap-1.5 rounded-md border border-gray-700 bg-gray-800 px-2.5 py-1 text-xs text-gray-300 hover:bg-gray-700 hover:text-gray-100 transition-colors cursor-pointer">
                <Share2 className="w-3.5 h-3.5" />
                Compartir
              </button>
            )}
            <button onClick={handleExportXmi} title="Exportar a XMI 2.5.1 (UML)"
              className="flex items-center gap-1.5 rounded-md border border-gray-700 bg-gray-800 px-2.5 py-1 text-xs text-gray-300 hover:bg-gray-700 hover:text-gray-100 transition-colors cursor-pointer">
              <Download className="w-3.5 h-3.5" />
              Exportar XMI
            </button>
            {!isViewer && (
              <>
                <button onClick={() => xmiFileInputRef.current?.click()} disabled={importingXmi}
                  title="Importar un diagrama desde un archivo XMI 2.5.1 (se añade al diagrama actual)"
                  className={`flex items-center gap-1.5 rounded-md border border-gray-700 bg-gray-800 px-2.5 py-1 text-xs transition-colors ${importingXmi ? 'cursor-wait text-gray-500' : 'cursor-pointer text-gray-300 hover:bg-gray-700 hover:text-gray-100'}`}>
                  <Upload className="w-3.5 h-3.5" />
                  {importingXmi ? 'Importando…' : 'Importar XMI'}
                </button>
                <input ref={xmiFileInputRef} type="file" accept=".xmi,.xml" className="hidden"
                  onChange={(e) => {
                    const file = e.target.files?.[0]
                    e.target.value = ''
                    if (file) handleImportXmi(file)
                  }} />
                <button onClick={handleGenerateBackend} disabled={generatingBackend}
                  title="Generar backend Spring Boot (.zip) a partir del diagrama"
                  className={`flex items-center gap-1.5 rounded-md border border-gray-700 bg-gray-800 px-2.5 py-1 text-xs transition-colors ${generatingBackend ? 'cursor-wait text-gray-500' : 'cursor-pointer text-gray-300 hover:bg-gray-700 hover:text-gray-100'}`}>
                  <Package className="w-3.5 h-3.5" />
                  {generatingBackend ? 'Generando…' : 'Generar backend'}
                </button>
                <button onClick={() => setConfirmClearOpen(true)} disabled={elements.length === 0}
                  title="Elimina todos los elementos y relaciones del diagrama"
                  className={`flex items-center gap-1.5 rounded-md border px-2.5 py-1 text-xs transition-colors ${elements.length === 0
                    ? 'cursor-not-allowed border-gray-800 bg-gray-900 text-gray-600'
                    : 'cursor-pointer border-red-900 bg-red-950/40 text-red-300 hover:bg-red-900/40 hover:text-red-200'}`}>
                  <Trash2 className="w-3.5 h-3.5" />
                  Eliminar todo
                </button>
              </>
            )}
            <span className="text-gray-400 text-xs">{user?.firstName}</span>
          </div>
        </header>
        <div className="flex flex-1 min-h-0 overflow-hidden">
          {!isViewer && (
            <Toolbox
              onAddElement={createElement}
              activeRelationship={activeRelationship}
              onSelectRelationship={(type) => { setActiveRelationship(type); if (type) { setSelectMode(false); setHandTool(false) } }}
              selectMode={selectMode}
              onToggleSelectMode={() => { setSelectMode((v) => !v); setHandTool(false); setActiveRelationship(null) }}
              handTool={handTool}
              onToggleHandTool={() => { setHandTool((v) => !v); setSelectMode(false); setActiveRelationship(null) }}
              width={toolboxWidth}
            />
          )}
          {!isViewer && (
            <div onPointerDown={startResize('toolbox')} title="Redimensionar"
              className="w-1.5 shrink-0 cursor-col-resize bg-gray-800 hover:bg-blue-500 active:bg-blue-500 transition-colors" />
          )}
          <div className="flex-1 min-w-0">
            <DiagramCanvas
              nodes={nodes} edges={edges}
              onNodesChange={onNodesChange} onEdgesChange={onEdgesChange}
              onNodeClick={handleNodeClick} onConnect={onConnect}
              onEdgeClick={onEdgeClick} onPaneClick={clearRelationshipSelection}
              onCursorMove={handleCursorMove} cursors={Object.values(remoteCursors)}
              activeRelationship={activeRelationship}
              selectMode={selectMode}
              handTool={handTool}
              onCreateRelationship={createRelationship}
              onReconnectRelationship={reconnectRelationship}
              onActivateRelationship={setActiveRelationship}
              onDropElement={createElement}
              readOnly={isViewer}
            />
          </div>
          {!isViewer && (
            <>
              <div onPointerDown={startResize('panel')} title="Redimensionar"
                className="w-1.5 shrink-0 cursor-col-resize bg-gray-800 hover:bg-blue-500 active:bg-blue-500 transition-colors" />
              {selectedRelationship ? (
                <RelationshipPanel
                  relationship={selectedRelationship}
                  multiplicities={relationshipMultiplicities}
                  onUpdateName={updateRelationshipName}
                  onUpdateMultiplicity={updateMultiplicity}
                  width={panelWidth}
                />
              ) : (
                <PropertiesPanel
                  element={selectedElement ? { ...selectedElement, elementType: selectedElement.elementType as 'class' | 'interface' | 'enumeration' | 'note' } : null}
                  attributes={selectedAttributes.map((a) => ({ ...a }))}
                  methods={selectedMethods.map((m) => ({ ...m }))}
                  onUpdate={updateElement} onAddAttribute={addAttribute} onAddMethod={addMethod}
                  onDeleteAttribute={deleteAttribute} onDeleteMethod={deleteMethod}
                  onAddParameter={addParameter} onDeleteParameter={deleteParameter}
                  onDelete={selectedElement ? () => deleteElement(selectedElement.id) : undefined}
                  width={panelWidth}
                />
              )}
            </>
          )}
        </div>
      </div>

      {!isViewer && projectId && diagramId && (
        <ShareDiagramDialog
          projectId={projectId}
          diagramId={diagramId}
          open={shareOpen}
          onClose={() => setShareOpen(false)}
        />
      )}

      <ConfirmDialog
        open={confirmClearOpen}
        title="¿Eliminar todo el contenido del diagrama?"
        description={`Se eliminarán ${elements.length} elemento${elements.length !== 1 ? 's' : ''} y todas sus relaciones. Esta acción no se puede deshacer.`}
        confirmLabel="Eliminar todo"
        destructive
        loading={clearing}
        onConfirm={handleDeleteAll}
        onCancel={() => setConfirmClearOpen(false)}
      />
    </ReactFlowProvider>
  )
}
