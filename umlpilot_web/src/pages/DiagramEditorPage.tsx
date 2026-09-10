import { useParams, Link } from 'react-router-dom'
import { ReactFlowProvider } from '@xyflow/react'
import { useAuth } from '../features/auth/useAuth'
import { useDiagramState } from '../features/diagram-editor/hooks/useDiagramState'
import { DiagramCanvas } from '../features/diagram-editor/components/DiagramCanvas'
import { Toolbar } from '../features/diagram-editor/components/Toolbar'
import { PropertiesPanel } from '../features/diagram-editor/components/PropertiesPanel'

export function DiagramEditorPage() {
  const { id: projectId, diagramId } = useParams<{ id: string; diagramId: string }>()
  const { user } = useAuth()

  const {
    nodes, edges, elements,
    selectedElement, selectedAttributes, selectedMethods, loading,
    onNodesChange, onEdgesChange, onConnect, selectElement, createElement,
    updateElement, addAttribute, deleteAttribute, addMethod, deleteMethod,
  } = useDiagramState({ projectId: projectId!, diagramId: diagramId! })

  if (loading) return <p className="p-8 text-gray-500">Cargando editor...</p>

  return (
    <ReactFlowProvider>
      <div className="h-screen flex flex-col font-sans">
        <header className="flex items-center justify-between bg-blue-900 text-white px-4 py-2 text-sm">
          <div className="flex items-center gap-4">
            <Link to={`/projects/${projectId}`} className="text-blue-300 hover:text-white hover:underline">&larr; Volver</Link>
            <strong>Editor de Diagrama</strong>
            <span className="text-blue-300 text-xs">{elements.length} elementos</span>
          </div>
          <span className="text-blue-200">{user?.firstName} {user?.lastName}</span>
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
