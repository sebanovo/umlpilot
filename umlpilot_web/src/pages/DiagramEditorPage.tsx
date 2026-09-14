import { useParams, Link } from 'react-router-dom'
import { ReactFlowProvider } from '@xyflow/react'
import { useAuth } from '../features/auth/useAuth'
import { useDiagramState } from '../features/diagram-editor/hooks/useDiagramState'
import { DiagramCanvas } from '../features/diagram-editor/components/DiagramCanvas'
import { Toolbar } from '../features/diagram-editor/components/Toolbar'
import { PropertiesPanel } from '../features/diagram-editor/components/PropertiesPanel'
import { RelationshipPanel } from '../features/diagram-editor/components/RelationshipPanel'
import { Wifi, WifiOff, Users, ArrowLeft } from 'lucide-react'

export function DiagramEditorPage() {
  const { id: projectId, diagramId } = useParams<{ id: string; diagramId: string }>()
  const { user } = useAuth()

  const {
    nodes, edges, elements, connected, presenceUsers,
    selectedElement, selectedAttributes, selectedMethods, loading,
    activeRelationship, setActiveRelationship, connectSource, handleNodeClick,
    selectedRelationship, relationshipMultiplicities, onEdgeClick, clearRelationshipSelection,
    updateRelationshipName, updateMultiplicity,
    onNodesChange, onEdgesChange, onConnect, createElement,
    updateElement, addAttribute, deleteAttribute, addMethod, deleteMethod,
  } = useDiagramState({ projectId: projectId!, diagramId: diagramId! })

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
                {connectSource ? 'Ahora haz clic en la clase destino' : 'Haz clic en la clase origen'}
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
            <span className="text-gray-400 text-xs">{user?.firstName}</span>
          </div>
        </header>
        <Toolbar onAddElement={createElement} activeRelationship={activeRelationship} onSelectRelationship={setActiveRelationship} />
        <div className="flex flex-1 overflow-hidden">
          <div className="flex-1">
            <DiagramCanvas
              nodes={nodes} edges={edges}
              onNodesChange={onNodesChange} onEdgesChange={onEdgesChange}
              onNodeClick={handleNodeClick} onConnect={onConnect}
              onEdgeClick={onEdgeClick} onPaneClick={clearRelationshipSelection}
              connectSourceId={connectSource}
            />
          </div>
          {selectedRelationship ? (
            <RelationshipPanel
              relationship={selectedRelationship}
              multiplicities={relationshipMultiplicities}
              onUpdateName={updateRelationshipName}
              onUpdateMultiplicity={updateMultiplicity}
            />
          ) : (
            <PropertiesPanel
              element={selectedElement ? { ...selectedElement, elementType: selectedElement.elementType as 'class' | 'interface' | 'enumeration' | 'note' } : null}
              attributes={selectedAttributes.map((a) => ({ ...a }))}
              methods={selectedMethods.map((m) => ({ ...m }))}
              onUpdate={updateElement} onAddAttribute={addAttribute} onAddMethod={addMethod}
              onDeleteAttribute={deleteAttribute} onDeleteMethod={deleteMethod}
            />
          )}
        </div>
      </div>
    </ReactFlowProvider>
  )
}
