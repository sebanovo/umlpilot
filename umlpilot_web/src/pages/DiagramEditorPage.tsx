import { useParams, Link } from 'react-router-dom'
import { ReactFlowProvider } from '@xyflow/react'
import { useAuth } from '../features/auth/useAuth'
import { useDiagramState } from '../features/diagram-editor/hooks/useDiagramState'
import { DiagramCanvas } from '../features/diagram-editor/components/DiagramCanvas'
import { Toolbar } from '../features/diagram-editor/components/Toolbar'
import { PropertiesPanel } from '../features/diagram-editor/components/PropertiesPanel'
import { Wifi, WifiOff, Users, ArrowLeft } from 'lucide-react'

export function DiagramEditorPage() {
  const { id: projectId, diagramId } = useParams<{ id: string; diagramId: string }>()
  const { user } = useAuth()

  const {
    nodes, edges, elements, connected, presenceUsers,
    selectedElement, selectedAttributes, selectedMethods, loading,
    onNodesChange, onEdgesChange, onConnect, selectElement, createElement,
    updateElement, addAttribute, deleteAttribute, addMethod, deleteMethod,
  } = useDiagramState({ projectId: projectId!, diagramId: diagramId! })

  if (loading) return (
    <div className="h-screen flex items-center justify-center bg-gray-50">
      <div className="text-center">
        <div className="w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full animate-spin mx-auto mb-3" />
        <p className="text-sm text-gray-500">Cargando editor...</p>
      </div>
    </div>
  )

  return (
    <ReactFlowProvider>
      <div className="h-screen flex flex-col font-sans bg-white">
        <header className="flex items-center justify-between bg-blue-900 text-white px-4 py-2 text-sm shrink-0">
          <div className="flex items-center gap-4">
            <Link to={`/projects/${projectId}`} className="text-blue-300 hover:text-white transition-colors">
              <ArrowLeft className="w-4 h-4" />
            </Link>
            <strong>Editor de Diagrama</strong>
            <span className="text-blue-300 text-xs">{elements.length} elemento{elements.length !== 1 ? 's' : ''}</span>
          </div>
          <div className="flex items-center gap-4">
            {/* Presence */}
            {presenceUsers.length > 0 && (
              <div className="flex items-center gap-1.5 text-xs text-blue-200">
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
            <span className="text-blue-200 text-xs">{user?.firstName}</span>
          </div>
        </header>
        <Toolbar onAddElement={createElement} onAddRelationship={() => {}} />
        <div className="flex flex-1 overflow-hidden">
          <div className="flex-1">
            <DiagramCanvas
              nodes={nodes} edges={edges}
              onNodesChange={onNodesChange} onEdgesChange={onEdgesChange}
              onNodeClick={selectElement} onConnect={onConnect}
            />
          </div>
          <PropertiesPanel
            element={selectedElement ? { ...selectedElement, elementType: selectedElement.elementType as 'class' | 'interface' | 'enumeration' | 'note' } : null}
            attributes={selectedAttributes.map((a) => ({ ...a }))}
            methods={selectedMethods.map((m) => ({ ...m }))}
            onUpdate={updateElement} onAddAttribute={addAttribute} onAddMethod={addMethod}
            onDeleteAttribute={deleteAttribute} onDeleteMethod={deleteMethod}
          />
        </div>
      </div>
    </ReactFlowProvider>
  )
}
