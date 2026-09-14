import { useCallback, useMemo } from 'react'
import { ReactFlow, Background, Controls, MiniMap, type Node, type Edge, type OnNodesChange, type OnEdgesChange, type NodeTypes, type EdgeTypes, BackgroundVariant } from '@xyflow/react'
import '@xyflow/react/dist/style.css'
import { ClassNode } from './ClassNode'
import { InterfaceNode } from './InterfaceNode'
import { EnumNode } from './EnumNode'
import { AssociationEdge, AssociationClassEdge, GeneralizationEdge, RealizationEdge, DependencyEdge, TemplateBindingEdge, AggregationEdge, CompositionEdge } from './UmlEdges'

interface DiagramCanvasProps {
  nodes: Node[]
  edges: Edge[]
  onNodesChange: OnNodesChange
  onEdgesChange: OnEdgesChange
  onNodeClick: (elementId: string) => void
  onConnect: (params: { source: string; target: string }) => void
  onEdgeClick?: (event: unknown, edge: Edge) => void
  onPaneClick?: () => void
  connectSourceId?: string | null
}

const nodeTypes: NodeTypes = {
  class: ClassNode as any,
  interface: InterfaceNode as any,
  enumeration: EnumNode as any,
}

const edgeTypes: EdgeTypes = {
  association: AssociationEdge as any,
  association_class: AssociationClassEdge as any,
  generalization: GeneralizationEdge as any,
  realization: RealizationEdge as any,
  dependency: DependencyEdge as any,
  template_binding: TemplateBindingEdge as any,
  aggregation: AggregationEdge as any,
  composition: CompositionEdge as any,
}

export function DiagramCanvas({ nodes, edges, onNodesChange, onEdgesChange, onNodeClick, onConnect, onEdgeClick, onPaneClick, connectSourceId }: DiagramCanvasProps) {
  const handleNodeClick = useCallback((_: unknown, node: Node) => { onNodeClick(node.id) }, [onNodeClick])

  const styledNodes = useMemo(
    () => connectSourceId
      ? nodes.map((n) => n.id === connectSourceId ? { ...n, className: 'ring-2 ring-blue-400 rounded' } : n)
      : nodes,
    [nodes, connectSourceId],
  )

  return (
    <div className="w-full h-full bg-gray-950">
      <ReactFlow
        nodes={styledNodes}
        edges={edges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        onNodeClick={handleNodeClick}
        onEdgeClick={onEdgeClick}
        onPaneClick={onPaneClick}
        onConnect={onConnect}
        nodeTypes={nodeTypes}
        edgeTypes={edgeTypes}
        colorMode="dark"
        fitView
        snapToGrid
        snapGrid={[15, 15]}
        deleteKeyCode="Delete"
      >
        <Background variant={BackgroundVariant.Dots} gap={15} size={1} color="#374151" />
        <Controls />
        <MiniMap />
      </ReactFlow>
    </div>
  )
}
