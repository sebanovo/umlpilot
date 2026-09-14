import { BaseEdge, EdgeLabelRenderer, getBezierPath, type EdgeProps } from '@xyflow/react'
import type { ReactNode } from 'react'

const EDGE_COLOR = '#9ca3af'
const NODE_BG = '#111827'

function relInfo(data: unknown) {
  const d = (data || {}) as { name?: string }
  return { name: d.name || '' }
}

function EdgeLabel({ x, y, text }: { x: number; y: number; text?: string }) {
  if (!text) return null
  return (
    <EdgeLabelRenderer>
      <div style={{ position: 'absolute', transform: `translate(-50%, -50%) translate(${x}px,${y}px)`, fontSize: 10, background: '#1f2937', color: '#e5e7eb', padding: '1px 4px', borderRadius: 2, pointerEvents: 'all' }}>{text}</div>
    </EdgeLabelRenderer>
  )
}

interface MarkerPathProps {
  path: string
  labelX: number
  labelY: number
  name?: string
  dashed?: boolean
  strokeWidth?: number
  markerStart?: string
  markerEnd?: string
  defs: ReactNode
}

function MarkerPath({ path, labelX, labelY, name, dashed, strokeWidth = 2, markerStart, markerEnd, defs }: MarkerPathProps) {
  return (
    <>
      <svg style={{ position: 'absolute', width: '100%', height: '100%', pointerEvents: 'none', overflow: 'visible' }}>
        <defs>{defs}</defs>
        <path d={path} fill="none" stroke={EDGE_COLOR} strokeWidth={strokeWidth}
          strokeDasharray={dashed ? '5,5' : undefined} markerStart={markerStart} markerEnd={markerEnd} />
      </svg>
      <EdgeLabel x={labelX} y={labelY} text={name} />
    </>
  )
}

export function AssociationEdge({ id, sourceX, sourceY, targetX, targetY, sourcePosition, targetPosition, data }: EdgeProps) {
  const [edgePath, labelX, labelY] = getBezierPath({ sourceX, sourceY, targetX, targetY, sourcePosition, targetPosition })
  const { name } = relInfo(data)
  return (
    <>
      <BaseEdge id={id} path={edgePath} style={{ strokeWidth: 2, stroke: EDGE_COLOR }} />
      <EdgeLabel x={labelX} y={labelY} text={name} />
    </>
  )
}

export function AssociationClassEdge({ id, sourceX, sourceY, targetX, targetY, sourcePosition, targetPosition, data }: EdgeProps) {
  const [edgePath, labelX, labelY] = getBezierPath({ sourceX, sourceY, targetX, targetY, sourcePosition, targetPosition })
  const { name } = relInfo(data)
  return (
    <>
      <BaseEdge id={id} path={edgePath} style={{ strokeWidth: 2, stroke: EDGE_COLOR }} />
      <EdgeLabel x={labelX} y={labelY} text={name || '«association class»'} />
    </>
  )
}

export function GeneralizationEdge({ id, sourceX, sourceY, targetX, targetY, sourcePosition, targetPosition, data }: EdgeProps) {
  const [edgePath, labelX, labelY] = getBezierPath({ sourceX, sourceY, targetX, targetY, sourcePosition, targetPosition })
  const { name } = relInfo(data)
  return (
    <MarkerPath path={edgePath} labelX={labelX} labelY={labelY} name={name}
      markerEnd={`url(#uml-tri-${id})`}
      defs={
        <marker id={`uml-tri-${id}`} viewBox="0 0 12 12" refX="11" refY="6" markerWidth="12" markerHeight="12" orient="auto" markerUnits="userSpaceOnUse">
          <path d="M 0 0 L 12 6 L 0 12 Z" fill={NODE_BG} stroke={EDGE_COLOR} strokeWidth="1" />
        </marker>
      }
    />
  )
}

export function RealizationEdge({ id, sourceX, sourceY, targetX, targetY, sourcePosition, targetPosition, data }: EdgeProps) {
  const [edgePath, labelX, labelY] = getBezierPath({ sourceX, sourceY, targetX, targetY, sourcePosition, targetPosition })
  const { name } = relInfo(data)
  return (
    <MarkerPath path={edgePath} labelX={labelX} labelY={labelY} name={name} dashed
      markerEnd={`url(#uml-tri-${id})`}
      defs={
        <marker id={`uml-tri-${id}`} viewBox="0 0 12 12" refX="11" refY="6" markerWidth="12" markerHeight="12" orient="auto" markerUnits="userSpaceOnUse">
          <path d="M 0 0 L 12 6 L 0 12 Z" fill={NODE_BG} stroke={EDGE_COLOR} strokeWidth="1" />
        </marker>
      }
    />
  )
}

export function DependencyEdge({ id, sourceX, sourceY, targetX, targetY, sourcePosition, targetPosition, data }: EdgeProps) {
  const [edgePath, labelX, labelY] = getBezierPath({ sourceX, sourceY, targetX, targetY, sourcePosition, targetPosition })
  const { name } = relInfo(data)
  return (
    <MarkerPath path={edgePath} labelX={labelX} labelY={labelY} name={name} dashed strokeWidth={1.5}
      markerEnd={`url(#uml-open-${id})`}
      defs={
        <marker id={`uml-open-${id}`} viewBox="0 0 12 12" refX="11" refY="6" markerWidth="12" markerHeight="12" orient="auto" markerUnits="userSpaceOnUse">
          <path d="M 0 0 L 12 6 L 0 12" fill="none" stroke={EDGE_COLOR} strokeWidth="1.5" />
        </marker>
      }
    />
  )
}

export function TemplateBindingEdge({ id, sourceX, sourceY, targetX, targetY, sourcePosition, targetPosition, data }: EdgeProps) {
  const [edgePath, labelX, labelY] = getBezierPath({ sourceX, sourceY, targetX, targetY, sourcePosition, targetPosition })
  const { name } = relInfo(data)
  return (
    <MarkerPath path={edgePath} labelX={labelX} labelY={labelY} name={name || '«bind»'} dashed strokeWidth={1.5}
      markerEnd={`url(#uml-open-${id})`}
      defs={
        <marker id={`uml-open-${id}`} viewBox="0 0 12 12" refX="11" refY="6" markerWidth="12" markerHeight="12" orient="auto" markerUnits="userSpaceOnUse">
          <path d="M 0 0 L 12 6 L 0 12" fill="none" stroke={EDGE_COLOR} strokeWidth="1.5" />
        </marker>
      }
    />
  )
}

export function AggregationEdge({ id, sourceX, sourceY, targetX, targetY, sourcePosition, targetPosition, data }: EdgeProps) {
  const [edgePath, labelX, labelY] = getBezierPath({ sourceX, sourceY, targetX, targetY, sourcePosition, targetPosition })
  const { name } = relInfo(data)
  return (
    <MarkerPath path={edgePath} labelX={labelX} labelY={labelY} name={name}
      markerStart={`url(#uml-dia-h-${id})`}
      defs={
        <marker id={`uml-dia-h-${id}`} viewBox="0 0 16 12" refX="0" refY="6" markerWidth="16" markerHeight="12" orient="auto" markerUnits="userSpaceOnUse">
          <path d="M 0 6 L 8 0 L 16 6 L 8 12 Z" fill={NODE_BG} stroke={EDGE_COLOR} strokeWidth="1" />
        </marker>
      }
    />
  )
}

export function CompositionEdge({ id, sourceX, sourceY, targetX, targetY, sourcePosition, targetPosition, data }: EdgeProps) {
  const [edgePath, labelX, labelY] = getBezierPath({ sourceX, sourceY, targetX, targetY, sourcePosition, targetPosition })
  const { name } = relInfo(data)
  return (
    <MarkerPath path={edgePath} labelX={labelX} labelY={labelY} name={name}
      markerStart={`url(#uml-dia-f-${id})`}
      defs={
        <marker id={`uml-dia-f-${id}`} viewBox="0 0 16 12" refX="0" refY="6" markerWidth="16" markerHeight="12" orient="auto" markerUnits="userSpaceOnUse">
          <path d="M 0 6 L 8 0 L 16 6 L 8 12 Z" fill={EDGE_COLOR} stroke={EDGE_COLOR} strokeWidth="1" />
        </marker>
      }
    />
  )
}
