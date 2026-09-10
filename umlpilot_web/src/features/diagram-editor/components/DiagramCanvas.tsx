import { useCallback, useMemo } from 'react'
import { ReactFlow, Background, Controls, MiniMap, type Node, type Edge, type OnNodesChange, type OnEdgesChange, type NodeTypes, type EdgeTypes, BackgroundVariant } from '@xyflow/react'
import '@xyflow/react/dist/style.css'
import { ClassNode } from './ClassNode'
import { InterfaceNode } from './InterfaceNode'
import { EnumNode } from './EnumNode'
import { AssociationEdge, GeneralizationEdge, DependencyEdge } from './UmlEdges'
import type { UmlElement, UmlAttribute, UmlMethod, UmlLiteral, UmlRelationship } from '../types/uml'

interface DiagramCanvasProps {
  elements: UmlElement[]
  attributes: Record<string, UmlAttribute[]>
  methods: Record<string, UmlMethod[]>
  literals: Record<string, UmlLiteral[]>
  relationships: UmlRelationship[]
  onNodesChange: OnNodesChange
  onEdgesChange: OnEdgesChange
  onNodeClick: (elementId: string) => void
  onConnect: (params: { source: string; target: string }) => void
}

export function DiagramCanvas({ elements, attributes, methods, literals, relationships, onNodesChange, onEdgesChange, onNodeClick, onConnect }: DiagramCanvasProps) {
  const nodeTypes: NodeTypes = useMemo(() => ({
    class: ClassNode as any,
    interface: InterfaceNode as any,
    enumeration: EnumNode as any,
  }), [])

  const edgeTypes: EdgeTypes = useMemo(() => ({
    association: AssociationEdge as any,
    generalization: GeneralizationEdge as any,
    dependency: DependencyEdge as any,
    aggregation: AssociationEdge as any,
    composition: AssociationEdge as any,
    realization: DependencyEdge as any,
  }), [])

  const nodes: Node[] = useMemo(() => elements.map((el) => ({
    id: el.id,
    type: el.elementType === 'enumeration' ? 'enumeration' : el.elementType === 'interface' ? 'interface' : 'class',
    position: { x: el.positionX, y: el.positionY },
    data: {
      name: el.name,
      visibility: el.visibility,
      stereotype: el.stereotype,
      attributes: attributes[el.id] || [],
      methods: methods[el.id] || [],
      literals: literals[el.id] || [],
    },
  })), [elements, attributes, methods, literals])

  const edges: Edge[] = useMemo(() => relationships.map((rel) => ({
    id: rel.id,
    source: rel.sourceElementId,
    target: rel.targetElementId,
    type: rel.relationshipType === 'generalization' ? 'generalization' : rel.relationshipType === 'dependency' || rel.relationshipType === 'realization' ? 'dependency' : 'association',
    label: rel.name || '',
    animated: rel.relationshipType === 'dependency',
  })), [relationships])

  const handleNodeClick = useCallback((_: any, node: Node) => { onNodeClick(node.id) }, [onNodeClick])

  return (
    <div style={{ width: '100%', height: '100%' }}>
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
