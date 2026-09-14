import { BaseEdge, EdgeLabelRenderer, getBezierPath, type EdgeProps } from '@xyflow/react'
import type { ReactNode } from 'react'

const EDGE_COLOR = '#9ca3af'
const NODE_BG = '#111827'

function relInfo(data: unknown) {
  const d = (data || {}) as { name?: string; onDelete?: () => void }
  return { name: d.name || '', onDelete: d.onDelete }
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

interface MarkerPathProps {
  path: string
  labelX: number
  labelY: number
  name?: string
  selected?: boolean
  onDelete?: () => void
  dashed?: boolean
  strokeWidth?: number
  markerStart?: string
  markerEnd?: string
  defs: ReactNode
}

function MarkerPath({ path, labelX, labelY, name, selected, onDelete, dashed, strokeWidth = 2, markerStart, markerEnd, defs }: MarkerPathProps) {
  return (
    <>
      <svg style={{ position: 'absolute', width: '100%', height: '100%', pointerEvents: 'none', overflow: 'visible' }}>
        <defs>{defs}</defs>
        <path d={path} fill="none" stroke={selected ? '#60a5fa' : EDGE_COLOR} strokeWidth={strokeWidth}
          strokeDasharray={dashed ? '5,5' : undefined} markerStart={markerStart} markerEnd={markerEnd} />
        <path d={path} fill="none" stroke="transparent" strokeWidth={20} style={{ pointerEvents: 'stroke' }} />
      </svg>
      <EdgeMid x={labelX} y={labelY} name={name} selected={selected} onDelete={onDelete} />
    </>
  )
}

export function AssociationEdge({ id, sourceX, sourceY, targetX, targetY, sourcePosition, targetPosition, data, selected }: EdgeProps) {
  const [edgePath, labelX, labelY] = getBezierPath({ sourceX, sourceY, targetX, targetY, sourcePosition, targetPosition })
  const { name, onDelete } = relInfo(data)
  return (
    <>
      <BaseEdge id={id} path={edgePath} style={{ strokeWidth: 2, stroke: selected ? '#60a5fa' : EDGE_COLOR }} />
      <EdgeMid x={labelX} y={labelY} name={name} selected={selected} onDelete={onDelete} />
    </>
  )
}

export function AssociationClassEdge({ id, sourceX, sourceY, targetX, targetY, sourcePosition, targetPosition, data, selected }: EdgeProps) {
  const [edgePath, labelX, labelY] = getBezierPath({ sourceX, sourceY, targetX, targetY, sourcePosition, targetPosition })
  const { name, onDelete } = relInfo(data)
  return (
    <>
      <BaseEdge id={id} path={edgePath} style={{ strokeWidth: 2, stroke: selected ? '#60a5fa' : EDGE_COLOR }} />
      <EdgeMid x={labelX} y={labelY} name={name || '«association class»'} selected={selected} onDelete={onDelete} />
    </>
  )
}

export function GeneralizationEdge({ id, sourceX, sourceY, targetX, targetY, sourcePosition, targetPosition, data, selected }: EdgeProps) {
  const [edgePath, labelX, labelY] = getBezierPath({ sourceX, sourceY, targetX, targetY, sourcePosition, targetPosition })
  const { name, onDelete } = relInfo(data)
  return (
    <MarkerPath path={edgePath} labelX={labelX} labelY={labelY} name={name} selected={selected} onDelete={onDelete}
      markerEnd={`url(#uml-tri-${id})`}
      defs={
        <marker id={`uml-tri-${id}`} viewBox="0 0 12 12" refX="11" refY="6" markerWidth="12" markerHeight="12" orient="auto" markerUnits="userSpaceOnUse">
          <path d="M 0 0 L 12 6 L 0 12 Z" fill={NODE_BG} stroke={EDGE_COLOR} strokeWidth="1" />
        </marker>
      }
    />
  )
}

export function RealizationEdge({ id, sourceX, sourceY, targetX, targetY, sourcePosition, targetPosition, data, selected }: EdgeProps) {
  const [edgePath, labelX, labelY] = getBezierPath({ sourceX, sourceY, targetX, targetY, sourcePosition, targetPosition })
  const { name, onDelete } = relInfo(data)
  return (
    <MarkerPath path={edgePath} labelX={labelX} labelY={labelY} name={name} selected={selected} onDelete={onDelete} dashed
      markerEnd={`url(#uml-tri-${id})`}
      defs={
        <marker id={`uml-tri-${id}`} viewBox="0 0 12 12" refX="11" refY="6" markerWidth="12" markerHeight="12" orient="auto" markerUnits="userSpaceOnUse">
          <path d="M 0 0 L 12 6 L 0 12 Z" fill={NODE_BG} stroke={EDGE_COLOR} strokeWidth="1" />
        </marker>
      }
    />
  )
}

export function DependencyEdge({ id, sourceX, sourceY, targetX, targetY, sourcePosition, targetPosition, data, selected }: EdgeProps) {
  const [edgePath, labelX, labelY] = getBezierPath({ sourceX, sourceY, targetX, targetY, sourcePosition, targetPosition })
  const { name, onDelete } = relInfo(data)
  return (
    <MarkerPath path={edgePath} labelX={labelX} labelY={labelY} name={name} selected={selected} onDelete={onDelete} dashed strokeWidth={1.5}
      markerEnd={`url(#uml-open-${id})`}
      defs={
        <marker id={`uml-open-${id}`} viewBox="0 0 12 12" refX="11" refY="6" markerWidth="12" markerHeight="12" orient="auto" markerUnits="userSpaceOnUse">
          <path d="M 0 0 L 12 6 L 0 12" fill="none" stroke={EDGE_COLOR} strokeWidth="1.5" />
        </marker>
      }
    />
  )
}

export function TemplateBindingEdge({ id, sourceX, sourceY, targetX, targetY, sourcePosition, targetPosition, data, selected }: EdgeProps) {
  const [edgePath, labelX, labelY] = getBezierPath({ sourceX, sourceY, targetX, targetY, sourcePosition, targetPosition })
  const { name, onDelete } = relInfo(data)
  return (
    <MarkerPath path={edgePath} labelX={labelX} labelY={labelY} name={name || '«bind»'} selected={selected} onDelete={onDelete} dashed strokeWidth={1.5}
      markerEnd={`url(#uml-open-${id})`}
      defs={
        <marker id={`uml-open-${id}`} viewBox="0 0 12 12" refX="11" refY="6" markerWidth="12" markerHeight="12" orient="auto" markerUnits="userSpaceOnUse">
          <path d="M 0 0 L 12 6 L 0 12" fill="none" stroke={EDGE_COLOR} strokeWidth="1.5" />
        </marker>
      }
    />
  )
}

export function AggregationEdge({ id, sourceX, sourceY, targetX, targetY, sourcePosition, targetPosition, data, selected }: EdgeProps) {
  const [edgePath, labelX, labelY] = getBezierPath({ sourceX, sourceY, targetX, targetY, sourcePosition, targetPosition })
  const { name, onDelete } = relInfo(data)
  return (
    <MarkerPath path={edgePath} labelX={labelX} labelY={labelY} name={name} selected={selected} onDelete={onDelete}
      markerStart={`url(#uml-dia-h-${id})`}
      defs={
        <marker id={`uml-dia-h-${id}`} viewBox="0 0 16 12" refX="0" refY="6" markerWidth="16" markerHeight="12" orient="auto" markerUnits="userSpaceOnUse">
          <path d="M 0 6 L 8 0 L 16 6 L 8 12 Z" fill={NODE_BG} stroke={EDGE_COLOR} strokeWidth="1" />
        </marker>
      }
    />
  )
}

export function CompositionEdge({ id, sourceX, sourceY, targetX, targetY, sourcePosition, targetPosition, data, selected }: EdgeProps) {
  const [edgePath, labelX, labelY] = getBezierPath({ sourceX, sourceY, targetX, targetY, sourcePosition, targetPosition })
  const { name, onDelete } = relInfo(data)
  return (
    <MarkerPath path={edgePath} labelX={labelX} labelY={labelY} name={name} selected={selected} onDelete={onDelete}
      markerStart={`url(#uml-dia-f-${id})`}
      defs={
        <marker id={`uml-dia-f-${id}`} viewBox="0 0 16 12" refX="0" refY="6" markerWidth="16" markerHeight="12" orient="auto" markerUnits="userSpaceOnUse">
          <path d="M 0 6 L 8 0 L 16 6 L 8 12 Z" fill={EDGE_COLOR} stroke={EDGE_COLOR} strokeWidth="1" />
        </marker>
      }
    />
  )
}
