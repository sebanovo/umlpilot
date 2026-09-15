import { BaseEdge, EdgeLabelRenderer, getSmoothStepPath, useInternalNode, type EdgeProps } from '@xyflow/react'
import type { ReactNode } from 'react'

const EDGE_COLOR = '#9ca3af'
const EDGE_SELECTED = '#60a5fa'
const NODE_BG = '#111827'

function relInfo(data: unknown) {
  const d = (data || {}) as { name?: string; sourceMultiplicity?: string; targetMultiplicity?: string; associationClassElementId?: string | null; onDelete?: () => void }
  return {
    name: d.name || '',
    sourceMultiplicity: d.sourceMultiplicity || '',
    targetMultiplicity: d.targetMultiplicity || '',
    associationClassElementId: d.associationClassElementId || null,
    onDelete: d.onDelete,
  }
}

function edgeGeometry(props: EdgeProps): { path: string; labelX: number; labelY: number } {
  const { source, target, sourceX, sourceY, targetX, targetY, sourcePosition, targetPosition } = props

  if (source === target) {
    const d = 120
    const path = `M ${sourceX},${sourceY} L ${sourceX + d},${sourceY} L ${sourceX + d},${targetY} L ${targetX},${targetY}`
    return { path, labelX: sourceX + d, labelY: (sourceY + targetY) / 2 }
  }

  const [path, labelX, labelY] = getSmoothStepPath({
    sourceX, sourceY, sourcePosition, targetX, targetY, targetPosition,
    borderRadius: 0, offset: 20,
  })
  return { path, labelX, labelY }
}

function EdgeMid({ x, y, name, selected, onDelete }: { x: number; y: number; name?: string; selected?: boolean; onDelete?: () => void }) {
  return (
    <EdgeLabelRenderer>
      <div className="nodrag nopan"
        style={{ position: 'absolute', transform: `translate(-50%, -50%) translate(${x}px,${y}px)`, display: 'flex', alignItems: 'center', gap: 4, pointerEvents: 'all' }}>
        {name && (
          <div style={{ fontSize: 10, background: '#1f2937', color: '#e5e7eb', padding: '1px 4px', borderRadius: 2 }}>{name}</div>
        )}
        {selected && onDelete && (
          <button onClick={(e) => { e.stopPropagation(); onDelete() }} title="Eliminar relación"
            style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', width: 18, height: 18, borderRadius: 9999, background: '#7f1d1d', color: '#fecaca', border: '1px solid #b91c1c', cursor: 'pointer', fontSize: 13, lineHeight: 1, padding: 0 }}>
            ×
          </button>
        )}
      </div>
    </EdgeLabelRenderer>
  )
}

function EndLabel({ x, y, text }: { x: number; y: number; text?: string }) {
  if (!text) return null
  return (
    <EdgeLabelRenderer>
      <div style={{ position: 'absolute', transform: `translate(-50%, -50%) translate(${x}px,${y}px)`, fontSize: 10, fontWeight: 600, color: '#cbd5e1', background: '#111827', padding: '0 3px', borderRadius: 2, pointerEvents: 'none' }}>
        {text}
      </div>
    </EdgeLabelRenderer>
  )
}

interface MarkerPathProps {
  path: string
  selected?: boolean
  dashed?: boolean
  strokeWidth?: number
  markerStart?: string
  markerEnd?: string
  defs: ReactNode
}

function MarkerPath({ path, selected, dashed, strokeWidth = 2, markerStart, markerEnd, defs }: MarkerPathProps) {
  return (
    <svg style={{ position: 'absolute', width: '100%', height: '100%', pointerEvents: 'none', overflow: 'visible' }}>
      <defs>{defs}</defs>
      <path d={path} fill="none" stroke={selected ? EDGE_SELECTED : EDGE_COLOR} strokeWidth={strokeWidth}
        strokeDasharray={dashed ? '5,5' : undefined} markerStart={markerStart} markerEnd={markerEnd} />
      <path d={path} fill="none" stroke="transparent" strokeWidth={20} style={{ pointerEvents: 'stroke' }} />
    </svg>
  )
}

function HollowTriangle(id: string) {
  return (
    <marker id={`uml-tri-${id}`} viewBox="0 0 12 12" refX="11" refY="6" markerWidth="12" markerHeight="12" orient="auto" markerUnits="userSpaceOnUse">
      <path d="M 0 0 L 12 6 L 0 12 Z" fill={NODE_BG} stroke={EDGE_COLOR} strokeWidth="1" />
    </marker>
  )
}

function OpenArrow(id: string) {
  return (
    <marker id={`uml-open-${id}`} viewBox="0 0 12 12" refX="11" refY="6" markerWidth="12" markerHeight="12" orient="auto" markerUnits="userSpaceOnUse">
      <path d="M 0 0 L 12 6 L 0 12" fill="none" stroke={EDGE_COLOR} strokeWidth="1.5" />
    </marker>
  )
}

function Diamond(id: string, filled: boolean) {
  return (
    <marker id={`uml-dia-${filled ? 'f' : 'h'}-${id}`} viewBox="0 0 16 12" refX="0" refY="6" markerWidth="16" markerHeight="12" orient="auto" markerUnits="userSpaceOnUse">
      <path d="M 0 6 L 8 0 L 16 6 L 8 12 Z" fill={filled ? EDGE_COLOR : NODE_BG} stroke={EDGE_COLOR} strokeWidth="1" />
    </marker>
  )
}

export function AssociationEdge(props: EdgeProps) {
  const { id, selected, data, sourceX, sourceY, targetX, targetY } = props
  const { path, labelX, labelY } = edgeGeometry(props)
  const { name, sourceMultiplicity, targetMultiplicity, onDelete } = relInfo(data)
  return (
    <>
      <BaseEdge id={id} path={path} style={{ strokeWidth: 2, stroke: selected ? EDGE_SELECTED : EDGE_COLOR }} />
      <EndLabel x={sourceX + 18} y={sourceY + 18} text={sourceMultiplicity} />
      <EndLabel x={targetX + 18} y={targetY - 18} text={targetMultiplicity} />
      <EdgeMid x={labelX} y={labelY} name={name} selected={selected} onDelete={onDelete} />
    </>
  )
}

function AssociationClassConnector({ elementId, midX, midY, selected }: { elementId: string; midX: number; midY: number; selected?: boolean }) {
  const node = useInternalNode(elementId)
  if (!node) return null

  const width = node.measured?.width ?? 180
  const height = node.measured?.height ?? 80
  const nx = node.internals.positionAbsolute.x
  const ny = node.internals.positionAbsolute.y

  const inside = midX >= nx && midX <= nx + width && midY >= ny && midY <= ny + height
  const tx = inside ? nx + width / 2 : Math.max(nx, Math.min(midX, nx + width))
  const ty = inside ? ny : Math.max(ny, Math.min(midY, ny + height))

  return (
    <svg style={{ position: 'absolute', width: '100%', height: '100%', pointerEvents: 'none', overflow: 'visible' }}>
      <path d={`M ${midX},${midY} L ${tx},${ty}`} fill="none"
        stroke={selected ? EDGE_SELECTED : EDGE_COLOR} strokeWidth={1.5} strokeDasharray="5,5" />
    </svg>
  )
}

export function AssociationClassEdge(props: EdgeProps) {
  const { id, selected, data, sourceX, sourceY, targetX, targetY } = props
  const { path, labelX, labelY } = edgeGeometry(props)
  const { name, sourceMultiplicity, targetMultiplicity, associationClassElementId, onDelete } = relInfo(data)
  return (
    <>
      <BaseEdge id={id} path={path} style={{ strokeWidth: 2, stroke: selected ? EDGE_SELECTED : EDGE_COLOR }} />
      {associationClassElementId && (
        <AssociationClassConnector elementId={associationClassElementId} midX={labelX} midY={labelY} selected={selected} />
      )}
      <EndLabel x={sourceX + 18} y={sourceY + 18} text={sourceMultiplicity} />
      <EndLabel x={targetX + 18} y={targetY - 18} text={targetMultiplicity} />
      <EdgeMid x={labelX} y={labelY} name={name} selected={selected} onDelete={onDelete} />
    </>
  )
}

export function GeneralizationEdge(props: EdgeProps) {
  const { id, selected, data, sourceX, sourceY, targetX, targetY } = props
  const { path, labelX, labelY } = edgeGeometry(props)
  const { name, sourceMultiplicity, targetMultiplicity, onDelete } = relInfo(data)
  return (
    <>
      <MarkerPath path={path} selected={selected} markerEnd={`url(#uml-tri-${id})`} defs={<HollowTriangle id={id} />} />
      <EndLabel x={sourceX + 18} y={sourceY + 18} text={sourceMultiplicity} />
      <EndLabel x={targetX + 18} y={targetY - 18} text={targetMultiplicity} />
      <EdgeMid x={labelX} y={labelY} name={name} selected={selected} onDelete={onDelete} />
    </>
  )
}

export function RealizationEdge(props: EdgeProps) {
  const { id, selected, data, sourceX, sourceY, targetX, targetY } = props
  const { path, labelX, labelY } = edgeGeometry(props)
  const { name, sourceMultiplicity, targetMultiplicity, onDelete } = relInfo(data)
  return (
    <>
      <MarkerPath path={path} selected={selected} dashed markerEnd={`url(#uml-tri-${id})`} defs={<HollowTriangle id={id} />} />
      <EndLabel x={sourceX + 18} y={sourceY + 18} text={sourceMultiplicity} />
      <EndLabel x={targetX + 18} y={targetY - 18} text={targetMultiplicity} />
      <EdgeMid x={labelX} y={labelY} name={name} selected={selected} onDelete={onDelete} />
    </>
  )
}

export function DependencyEdge(props: EdgeProps) {
  const { id, selected, data, sourceX, sourceY, targetX, targetY } = props
  const { path, labelX, labelY } = edgeGeometry(props)
  const { name, sourceMultiplicity, targetMultiplicity, onDelete } = relInfo(data)
  return (
    <>
      <MarkerPath path={path} selected={selected} dashed strokeWidth={1.5} markerEnd={`url(#uml-open-${id})`} defs={<OpenArrow id={id} />} />
      <EndLabel x={sourceX + 18} y={sourceY + 18} text={sourceMultiplicity} />
      <EndLabel x={targetX + 18} y={targetY - 18} text={targetMultiplicity} />
      <EdgeMid x={labelX} y={labelY} name={name} selected={selected} onDelete={onDelete} />
    </>
  )
}

export function TemplateBindingEdge(props: EdgeProps) {
  const { id, selected, data, sourceX, sourceY, targetX, targetY } = props
  const { path, labelX, labelY } = edgeGeometry(props)
  const { name, sourceMultiplicity, targetMultiplicity, onDelete } = relInfo(data)
  return (
    <>
      <MarkerPath path={path} selected={selected} dashed strokeWidth={1.5} markerEnd={`url(#uml-open-${id})`} defs={<OpenArrow id={id} />} />
      <EndLabel x={sourceX + 18} y={sourceY + 18} text={sourceMultiplicity} />
      <EndLabel x={targetX + 18} y={targetY - 18} text={targetMultiplicity} />
      <EdgeMid x={labelX} y={labelY} name={name || '«bind»'} selected={selected} onDelete={onDelete} />
    </>
  )
}

export function AggregationEdge(props: EdgeProps) {
  const { id, selected, data, sourceX, sourceY, targetX, targetY } = props
  const { path, labelX, labelY } = edgeGeometry(props)
  const { name, sourceMultiplicity, targetMultiplicity, onDelete } = relInfo(data)
  return (
    <>
      <MarkerPath path={path} selected={selected} markerStart={`url(#uml-dia-h-${id})`} defs={<Diamond id={id} filled={false} />} />
      <EndLabel x={sourceX + 18} y={sourceY + 18} text={sourceMultiplicity} />
      <EndLabel x={targetX + 18} y={targetY - 18} text={targetMultiplicity} />
      <EdgeMid x={labelX} y={labelY} name={name} selected={selected} onDelete={onDelete} />
    </>
  )
}

export function CompositionEdge(props: EdgeProps) {
  const { id, selected, data, sourceX, sourceY, targetX, targetY } = props
  const { path, labelX, labelY } = edgeGeometry(props)
  const { name, sourceMultiplicity, targetMultiplicity, onDelete } = relInfo(data)
  return (
    <>
      <MarkerPath path={path} selected={selected} markerStart={`url(#uml-dia-f-${id})`} defs={<Diamond id={id} filled />} />
      <EndLabel x={sourceX + 18} y={sourceY + 18} text={sourceMultiplicity} />
      <EndLabel x={targetX + 18} y={targetY - 18} text={targetMultiplicity} />
      <EdgeMid x={labelX} y={labelY} name={name} selected={selected} onDelete={onDelete} />
    </>
  )
}
