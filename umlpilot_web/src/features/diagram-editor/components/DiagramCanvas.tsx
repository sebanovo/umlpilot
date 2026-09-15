import { useCallback, useMemo, type MouseEvent } from 'react'
import { ReactFlow, Background, Controls, MiniMap, ViewportPortal, useReactFlow, type Node, type Edge, type OnNodesChange, type OnEdgesChange, type NodeTypes, type EdgeTypes, BackgroundVariant } from '@xyflow/react'
import '@xyflow/react/dist/style.css'
import { ClassNode } from './ClassNode'
import { InterfaceNode } from './InterfaceNode'
import { EnumNode } from './EnumNode'
import { NoteNode } from './NoteNode'
import { AssociationEdge, AssociationClassEdge, GeneralizationEdge, RealizationEdge, DependencyEdge, TemplateBindingEdge, AggregationEdge, CompositionEdge } from './UmlEdges'
import type { RemoteCursor } from '../../../shared/lib/useWebSocket'

interface DiagramCanvasProps {
  nodes: Node[]
  edges: Edge[]
  onNodesChange: OnNodesChange
  onEdgesChange: OnEdgesChange
  onNodeClick: (elementId: string) => void
  onConnect: (params: { source: string; target: string }) => void
  onEdgeClick?: (event: unknown, edge: Edge) => void
  onPaneClick?: () => void
  onCursorMove?: (x: number, y: number) => void
  cursors?: RemoteCursor[]
  connectSourceId?: string | null
}

const nodeTypes: NodeTypes = {
  class: ClassNode as any,
  interface: InterfaceNode as any,
  enumeration: EnumNode as any,
  note: NoteNode as any,
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

function CursorPointer({ cursor }: { cursor: RemoteCursor }) {
  return (
    <div style={{ position: 'absolute', left: cursor.x, top: cursor.y, pointerEvents: 'none', zIndex: 50, transform: 'translate(-2px, -2px)' }}>
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" style={{ filter: 'drop-shadow(0 1px 1px rgba(0,0,0,0.45))' }}>
        <path d="M4 2 L4 19 L8.5 14.5 L11.5 21 L14.5 19.5 L11.5 13.5 L18 13.5 Z"
          fill={cursor.color} stroke="#ffffff" strokeWidth="1.2" strokeLinejoin="round" />
      </svg>
      <span style={{ position: 'absolute', left: 18, top: 18, background: cursor.color, color: '#ffffff', fontSize: 11, lineHeight: 1, padding: '3px 6px', borderRadius: 6, whiteSpace: 'nowrap', fontWeight: 600 }}>
        {cursor.name}
      </span>
    </div>
  )
}

export function DiagramCanvas({ nodes, edges, onNodesChange, onEdgesChange, onNodeClick, onConnect, onEdgeClick, onPaneClick, onCursorMove, cursors, connectSourceId }: DiagramCanvasProps) {
  const { screenToFlowPosition } = useReactFlow()

  const handleNodeClick = useCallback((_: unknown, node: Node) => { onNodeClick(node.id) }, [onNodeClick])

  const handleMouseMove = useCallback((event: MouseEvent) => {
    if (!onCursorMove) return
    const position = screenToFlowPosition({ x: event.clientX, y: event.clientY })
    onCursorMove(position.x, position.y)
  }, [onCursorMove, screenToFlowPosition])

  const styledNodes = useMemo(
    () => connectSourceId
      ? nodes.map((n) => n.id === connectSourceId ? { ...n, className: 'ring-2 ring-blue-400 rounded' } : n)
      : nodes,
    [nodes, connectSourceId],
  )

  return (
    <div className="w-full h-full bg-gray-950" onMouseMove={handleMouseMove}>
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
        zoomOnDoubleClick={false}
      >
        <Background variant={BackgroundVariant.Dots} gap={15} size={1} color="#374151" />
        <Controls />
        <MiniMap />
        {cursors && cursors.length > 0 && (
          <ViewportPortal>
            {cursors.map((cursor) => <CursorPointer key={cursor.clientId} cursor={cursor} />)}
          </ViewportPortal>
        )}
      </ReactFlow>
    </div>
  )
}
