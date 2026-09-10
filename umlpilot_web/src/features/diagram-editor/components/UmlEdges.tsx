import { BaseEdge, EdgeLabelRenderer, getBezierPath, type EdgeProps } from '@xyflow/react'

export function AssociationEdge({ id, sourceX, sourceY, targetX, targetY, sourcePosition, targetPosition, markerEnd }: EdgeProps) {
  const [edgePath, labelX, labelY] = getBezierPath({ sourceX, sourceY, targetX, targetY, sourcePosition, targetPosition })
  return (
    <>
      <BaseEdge id={id} path={edgePath} markerEnd={markerEnd} style={{ strokeWidth: 2 }} />
      <EdgeLabelRenderer>
        <div style={{ position: 'absolute', transform: `translate(-50%, -50%) translate(${labelX}px,${labelY}px)`, fontSize: 10, background: 'white', padding: '1px 4px', borderRadius: 2, pointerEvents: 'all' }}>&nbsp;</div>
      </EdgeLabelRenderer>
    </>
  )
}

export function GeneralizationEdge({ id, sourceX, sourceY, targetX, targetY, sourcePosition, targetPosition }: EdgeProps) {
  const [edgePath] = getBezierPath({ sourceX, sourceY, targetX, targetY, sourcePosition, targetPosition })
  return (
    <svg style={{ position: 'absolute', width: '100%', height: '100%', pointerEvents: 'none' }}>
      <defs>
        <marker id={`arrow-hollow-${id}`} viewBox="0 0 10 10" refX="10" refY="5" markerWidth="8" markerHeight="8" orient="auto-start-reverse">
          <path d="M 0 0 L 10 5 L 0 10 Z" fill="white" stroke="#333" strokeWidth="1" />
        </marker>
      </defs>
      <path d={edgePath} fill="none" stroke="#333" strokeWidth="2" markerEnd={`url(#arrow-hollow-${id})`} />
    </svg>
  )
}

export function DependencyEdge({ id, sourceX, sourceY, targetX, targetY, sourcePosition, targetPosition }: EdgeProps) {
  const [edgePath] = getBezierPath({ sourceX, sourceY, targetX, targetY, sourcePosition, targetPosition })
  return (
    <svg style={{ position: 'absolute', width: '100%', height: '100%', pointerEvents: 'none' }}>
      <defs>
        <marker id={`arrow-open-${id}`} viewBox="0 0 10 10" refX="10" refY="5" markerWidth="8" markerHeight="8" orient="auto-start-reverse">
          <path d="M 0 0 L 10 5 L 0 10" fill="none" stroke="#333" strokeWidth="1.5" />
        </marker>
      </defs>
      <path d={edgePath} fill="none" stroke="#333" strokeWidth="1.5" strokeDasharray="5,5" markerEnd={`url(#arrow-open-${id})`} />
    </svg>
  )
}
