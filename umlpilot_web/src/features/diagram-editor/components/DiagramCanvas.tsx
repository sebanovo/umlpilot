import { useCallback, useEffect, useMemo, useState, type DragEvent, type MouseEvent, type PointerEvent as ReactPointerEvent } from 'react'
import { ReactFlow, Background, Controls, ViewportPortal, useReactFlow, useInternalNode, type Node, type Edge, type Connection, type OnNodesChange, type OnEdgesChange, type NodeTypes, type EdgeTypes, BackgroundVariant } from '@xyflow/react'
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
  onConnect: (params: { source: string; target: string; sourceHandle?: string | null; targetHandle?: string | null }) => void
  onReconnectRelationship?: (relationshipId: string, connection: { source?: string | null; target?: string | null; sourceHandle?: string | null; targetHandle?: string | null }) => void
  onEdgeClick?: (event: unknown, edge: Edge) => void
  onPaneClick?: () => void
  onCursorMove?: (x: number, y: number) => void
  cursors?: RemoteCursor[]
  activeRelationship?: string | null
  selectMode?: boolean
  handTool?: boolean
  onCreateRelationship?: (sourceId: string, targetId: string, type: string) => void
  onActivateRelationship?: (type: string) => void
  onDropElement?: (type: string, x: number, y: number) => void
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

function ConnectionPreview({ sourceId, x, y }: { sourceId: string; x: number; y: number }) {
  const node = useInternalNode(sourceId)
  if (!node) return null
  const cx = node.internals.positionAbsolute.x + (node.measured?.width ?? 180) / 2
  const cy = node.internals.positionAbsolute.y + (node.measured?.height ?? 80) / 2
  return (
    <svg style={{ position: 'absolute', width: '100%', height: '100%', pointerEvents: 'none', overflow: 'visible' }}>
      <path d={`M ${cx},${cy} L ${x},${y}`} fill="none" stroke="#60a5fa" strokeWidth={2} strokeDasharray="6,4" />
      <circle cx={x} cy={y} r={3.5} fill="#60a5fa" />
    </svg>
  )
}

function nodeIdFromTarget(target: EventTarget | null): string | null {
  const el = target as HTMLElement | null
  const nodeEl = el?.closest?.('.react-flow__node') as HTMLElement | null
  return nodeEl?.getAttribute('data-id') ?? null
}

export function DiagramCanvas({
  nodes, edges, onNodesChange, onEdgesChange, onNodeClick, onConnect, onReconnectRelationship,
  onEdgeClick, onPaneClick, onCursorMove, cursors,
  activeRelationship, selectMode, handTool, onCreateRelationship, onActivateRelationship, onDropElement,
}: DiagramCanvasProps) {
  const { screenToFlowPosition } = useReactFlow()
  const [pendingSource, setPendingSource] = useState<string | null>(null)
  const [previewCursor, setPreviewCursor] = useState<{ x: number; y: number } | null>(null)
  const [showHandles, setShowHandles] = useState(false)

  const displayNodes = useMemo(
    () => nodes.map((n) => ({ ...n, className: showHandles ? 'show-handles' : undefined })),
    [nodes, showHandles],
  )

  const handleNodeClick = useCallback((_: unknown, node: Node) => { onNodeClick(node.id) }, [onNodeClick])

  const handleReconnect = useCallback((oldEdge: Edge, connection: Connection) => {
    onReconnectRelationship?.(oldEdge.id, connection)
  }, [onReconnectRelationship])

  const handleMouseMove = useCallback((event: MouseEvent) => {
    if (!onCursorMove) return
    const position = screenToFlowPosition({ x: event.clientX, y: event.clientY })
    onCursorMove(position.x, position.y)
  }, [onCursorMove, screenToFlowPosition])

  const handlePointerDown = useCallback((event: ReactPointerEvent) => {
    if (!activeRelationship || !onCreateRelationship) return
    const target = event.target as HTMLElement
    if (target.closest('.react-flow__handle, button, input, textarea, select, .nodrag')) return
    const id = nodeIdFromTarget(event.target)
    if (!id) return
    event.preventDefault()
    setPendingSource(id)
    setShowHandles(true)
    setPreviewCursor(screenToFlowPosition({ x: event.clientX, y: event.clientY }))
  }, [activeRelationship, onCreateRelationship, screenToFlowPosition])

  useEffect(() => {
    if (!pendingSource) return

    const onMove = (event: PointerEvent) => {
      setPreviewCursor(screenToFlowPosition({ x: event.clientX, y: event.clientY }))
    }
    const onUp = (event: PointerEvent) => {
      const el = document.elementFromPoint(event.clientX, event.clientY) as HTMLElement | null
      const targetId = el?.closest('.react-flow__node')?.getAttribute('data-id') ?? null
      if (targetId && activeRelationship && onCreateRelationship) {
        onCreateRelationship(pendingSource, targetId, activeRelationship)
      }
      setPendingSource(null)
      setPreviewCursor(null)
      setShowHandles(false)
    }

    window.addEventListener('pointermove', onMove)
    window.addEventListener('pointerup', onUp)
    return () => {
      window.removeEventListener('pointermove', onMove)
      window.removeEventListener('pointerup', onUp)
    }
  }, [pendingSource, activeRelationship, onCreateRelationship, screenToFlowPosition])

  const handleDragOver = useCallback((event: DragEvent) => {
    event.preventDefault()
    event.dataTransfer.dropEffect = 'copy'
  }, [])

  const handleDrop = useCallback((event: DragEvent) => {
    event.preventDefault()
    const nodeType = event.dataTransfer.getData('application/uml-node')
    const relType = event.dataTransfer.getData('application/uml-relationship')
    if (nodeType && onDropElement) {
      const position = screenToFlowPosition({ x: event.clientX, y: event.clientY })
      onDropElement(nodeType, Math.round(position.x), Math.round(position.y))
    } else if (relType && onActivateRelationship) {
      onActivateRelationship(relType)
    }
  }, [screenToFlowPosition, onDropElement, onActivateRelationship])

  return (
    <div
      className={`w-full h-full bg-gray-950 ${handTool ? 'cursor-grab' : activeRelationship ? 'cursor-crosshair' : ''}`}
      onMouseMove={handleMouseMove}
      onPointerDown={handlePointerDown}
      onDragOver={handleDragOver}
      onDrop={handleDrop}
    >
      <ReactFlow
        nodes={displayNodes}
        edges={edges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        onNodeClick={handleNodeClick}
        onEdgeClick={onEdgeClick}
        onPaneClick={onPaneClick}
        onConnect={onConnect}
        onConnectStart={() => setShowHandles(true)}
        onConnectEnd={() => setShowHandles(false)}
        onReconnect={handleReconnect}
        onReconnectStart={() => setShowHandles(true)}
        onReconnectEnd={() => setShowHandles(false)}
        edgesReconnectable
        nodeTypes={nodeTypes}
        edgeTypes={edgeTypes}
        colorMode="dark"
        fitView
        snapToGrid
        snapGrid={[15, 15]}
        deleteKeyCode="Delete"
        zoomOnDoubleClick={false}
        nodesDraggable={handTool ? false : !activeRelationship}
        panOnDrag={handTool ? true : (activeRelationship ? [1, 2] : (selectMode ? [1, 2] : true))}
        selectionOnDrag={!handTool && !!selectMode}
      >
        <Background variant={BackgroundVariant.Dots} gap={15} size={1} color="#374151" />
        <Controls />
        {/* <MiniMap /> */}
        <ViewportPortal>
          {cursors?.map((cursor) => <CursorPointer key={cursor.clientId} cursor={cursor} />)}
          {pendingSource && previewCursor && <ConnectionPreview sourceId={pendingSource} x={previewCursor.x} y={previewCursor.y} />}
        </ViewportPortal>
      </ReactFlow>
    </div>
  )
}
