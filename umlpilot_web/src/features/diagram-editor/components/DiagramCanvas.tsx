import { useCallback } from 'react'
import { ReactFlow, Background, Controls, MiniMap, type Node, type Edge, type OnNodesChange, type OnEdgesChange, type NodeTypes, type EdgeTypes, BackgroundVariant } from '@xyflow/react'
import '@xyflow/react/dist/style.css'
import { ClassNode } from './ClassNode'
import { InterfaceNode } from './InterfaceNode'
import { EnumNode } from './EnumNode'
import { AssociationEdge, GeneralizationEdge, DependencyEdge } from './UmlEdges'

interface DiagramCanvasProps {
  nodes: Node[]
  edges: Edge[]
  onNodesChange: OnNodesChange
  onEdgesChange: OnEdgesChange
  onNodeClick: (elementId: string) => void
  onConnect: (params: { source: string; target: string }) => void
}

const nodeTypes: NodeTypes = {
  class: ClassNode as any,
  interface: InterfaceNode as any,
  enumeration: EnumNode as any,
}

const edgeTypes: EdgeTypes = {
  association: AssociationEdge as any,
  generalization: GeneralizationEdge as any,
  dependency: DependencyEdge as any,
  aggregation: AssociationEdge as any,
  composition: AssociationEdge as any,
  realization: DependencyEdge as any,
}

export function DiagramCanvas({ nodes, edges, onNodesChange, onEdgesChange, onNodeClick, onConnect }: DiagramCanvasProps) {
  const handleNodeClick = useCallback((_: unknown, node: Node) => { onNodeClick(node.id) }, [onNodeClick])

  return (
    <div className="w-full h-full">
      <ReactFlow
        nodes={nodes}
        edges={edges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        onNodeClick={handleNodeClick}
        onConnect={onConnect}
        nodeTypes={nodeTypes}
        edgeTypes={edgeTypes}
        fitView
        snapToGrid
        snapGrid={[15, 15]}
        deleteKeyCode="Delete"
      >
        <Background variant={BackgroundVariant.Dots} gap={15} size={1} color="#e0e0e0" />
        <Controls />
        <MiniMap />
      </ReactFlow>
    </div>
  )
}
