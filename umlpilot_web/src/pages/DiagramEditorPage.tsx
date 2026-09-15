import { useEffect, useState, type PointerEvent as ReactPointerEvent } from 'react'
import { useParams, Link } from 'react-router-dom'
import { ReactFlowProvider } from '@xyflow/react'
import { useAuth } from '../features/auth/useAuth'
import { useDiagramState } from '../features/diagram-editor/hooks/useDiagramState'
import { DiagramCanvas } from '../features/diagram-editor/components/DiagramCanvas'
import { Toolbox } from '../features/diagram-editor/components/Toolbox'
import { PropertiesPanel } from '../features/diagram-editor/components/PropertiesPanel'
import { RelationshipPanel } from '../features/diagram-editor/components/RelationshipPanel'
import { buildXmi } from '../features/diagram-editor/lib/xmi'
import { Wifi, WifiOff, Users, ArrowLeft, Download } from 'lucide-react'

export function DiagramEditorPage() {
  const { id: projectId, diagramId } = useParams<{ id: string; diagramId: string }>()
  const { user } = useAuth()

  const {
    nodes, edges, elements, connected, presenceUsers,
    selectedElement, selectedAttributes, selectedMethods, loading,
    activeRelationship, setActiveRelationship, createRelationship, reconnectRelationship, handleNodeClick,
    selectedRelationship, relationshipMultiplicities, onEdgeClick, clearRelationshipSelection,
    updateRelationshipName, updateMultiplicity, remoteCursors, handleCursorMove,
    onNodesChange, onEdgesChange, onConnect, createElement, deleteElement,
    updateElement, addAttribute, deleteAttribute, addMethod, deleteMethod,
    attributes, methods, relationships,
  } = useDiagramState({ projectId: projectId!, diagramId: diagramId! })

  const [selectMode, setSelectMode] = useState(false)
  const [handTool, setHandTool] = useState(false)
  const [toolboxWidth, setToolboxWidth] = useState(176)
  const [panelWidth, setPanelWidth] = useState(288)

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
    const xml = buildXmi({
      modelName: `Diagram_${diagramId}`,
      elements,
      attributes,
      methods,
      relationships,
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
            {/* Presence */}
            {presenceUsers.length > 0 && (
              <div className="flex items-center gap-1.5 text-xs text-gray-400">
                <Users className="w-3.5 h-3.5" />
                {presenceUsers.length} en línea
              </div>
            )}
            {/* Connection status */}
            <div className="flex items-center gap-1.5">
              {connected ? (
                <>
                  <Wifi className="w-3.5 h-3.5 text-green-400" />
                  <span className="text-xs text-green-400">Conectado</span>
                </>
              ) : (
                <>
                  <WifiOff className="w-3.5 h-3.5 text-red-400" />
                  <span className="text-xs text-red-400">Desconectado</span>
                </>
              )}
            </div>
            <button onClick={handleExportXmi} title="Exportar a XMI 2.5.1 (UML)"
              className="flex items-center gap-1.5 rounded-md border border-gray-700 bg-gray-800 px-2.5 py-1 text-xs text-gray-300 hover:bg-gray-700 hover:text-gray-100 transition-colors cursor-pointer">
              <Download className="w-3.5 h-3.5" />
              Exportar XMI
            </button>
            <span className="text-gray-400 text-xs">{user?.firstName}</span>
          </div>
        </header>
        <div className="flex flex-1 min-h-0 overflow-hidden">
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
          <div onPointerDown={startResize('toolbox')} title="Redimensionar"
            className="w-1.5 shrink-0 cursor-col-resize bg-gray-800 hover:bg-blue-500 active:bg-blue-500 transition-colors" />
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
            />
          </div>
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
              onDelete={selectedElement ? () => deleteElement(selectedElement.id) : undefined}
              width={panelWidth}
            />
          )}
        </div>
      </div>
    </ReactFlowProvider>
  )
}
