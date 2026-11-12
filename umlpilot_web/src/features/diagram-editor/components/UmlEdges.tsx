import { EdgeLabelRenderer, getStraightPath, useInternalNode, useReactFlow, useStore, type EdgeProps } from '@xyflow/react'
import { useState, type MouseEvent as ReactMouseEvent, type PointerEvent as ReactPointerEvent } from 'react'
import { clampAnchorPercent, sideFromPosition, type AnchorEnd, type AnchorSide, type EdgeAnchor } from '../lib/anchors'
import type { EdgeWaypoint } from '../lib/waypoints'

const EDGE_COLOR = '#111827'
const EDGE_SELECTED = '#2563eb'
const NODE_BG = '#ffffff'

type Kind =
  | 'association' | 'association_class' | 'generalization' | 'realization'
  | 'aggregation' | 'composition'

function relInfo(data: unknown) {
  const d = (data || {}) as {
    name?: string
    sourceMultiplicity?: string
    targetMultiplicity?: string
    associationClassElementId?: string | null
    sourceAnchor?: EdgeAnchor
    targetAnchor?: EdgeAnchor
    waypoints?: EdgeWaypoint[]
    onAnchorChange?: (relationshipId: string, end: AnchorEnd, anchor: EdgeAnchor) => void
    onWaypointsChange?: (relationshipId: string, waypoints: EdgeWaypoint[]) => void
    onDelete?: () => void
  }
  return {
    name: d.name || '',
    sourceMultiplicity: d.sourceMultiplicity || '',
    targetMultiplicity: d.targetMultiplicity || '',
    associationClassElementId: d.associationClassElementId || null,
    sourceAnchor: d.sourceAnchor,
    targetAnchor: d.targetAnchor,
    waypoints: d.waypoints || [],
    onAnchorChange: d.onAnchorChange,
    onWaypointsChange: d.onWaypointsChange,
    onDelete: d.onDelete,
  }
}

// Direction the line takes leaving the source handle / entering the target handle
const SOURCE_DIR: Record<string, [number, number]> = {
  top: [0, -1], bottom: [0, 1], left: [-1, 0], right: [1, 0],
}
const TARGET_DIR: Record<string, [number, number]> = {
  top: [0, 1], bottom: [0, -1], left: [1, 0], right: [-1, 0],
}

type NodeGeometry = {
  internals: { positionAbsolute: { x: number; y: number } }
  measured?: { width?: number; height?: number }
}

function anchorPoint(node: NodeGeometry | undefined, anchor: EdgeAnchor, fallbackX: number, fallbackY: number) {
  const width = node?.measured?.width ?? 0
  const height = node?.measured?.height ?? 0
  if (!node || !width || !height) return { x: fallbackX, y: fallbackY }

  const x = node.internals.positionAbsolute.x
  const y = node.internals.positionAbsolute.y
  const offset = anchor.percent / 100

  if (anchor.side === 'top') return { x: x + width * offset, y }
  if (anchor.side === 'bottom') return { x: x + width * offset, y: y + height }
  if (anchor.side === 'left') return { x, y: y + height * offset }
  return { x: x + width, y: y + height * offset }
}

function nodeCenter(node: NodeGeometry | undefined) {
  const width = node?.measured?.width ?? 0
  const height = node?.measured?.height ?? 0
  if (!node || !width || !height) return null
  return {
    x: node.internals.positionAbsolute.x + width / 2,
    y: node.internals.positionAbsolute.y + height / 2,
  }
}

function autoSides(sourceNode: NodeGeometry | undefined, targetNode: NodeGeometry | undefined): { source: AnchorSide; target: AnchorSide } | null {
  const source = nodeCenter(sourceNode)
  const target = nodeCenter(targetNode)
  if (!source || !target) return null

  const dx = target.x - source.x
  const dy = target.y - source.y
  if (Math.abs(dx) >= Math.abs(dy)) {
    return dx >= 0 ? { source: 'right', target: 'left' } : { source: 'left', target: 'right' }
  }
  return dy >= 0 ? { source: 'bottom', target: 'top' } : { source: 'top', target: 'bottom' }
}

function projectAnchor(node: NodeGeometry, side: AnchorSide, point: { x: number; y: number }): EdgeAnchor {
  const width = node.measured?.width ?? 0
  const height = node.measured?.height ?? 0
  const x = node.internals.positionAbsolute.x
  const y = node.internals.positionAbsolute.y
  const percent = side === 'top' || side === 'bottom'
    ? (width ? ((point.x - x) / width) * 100 : 50)
    : (height ? ((point.y - y) / height) * 100 : 50)
  return { side, percent: clampAnchorPercent(percent) }
}

function hashString(value: string): number {
  let hash = 0
  for (let i = 0; i < value.length; i += 1) {
    hash = (hash * 31 + value.charCodeAt(i)) >>> 0
  }
  return hash
}

function pathFromPoints(points: { x: number; y: number }[]): string {
  return points.map((point, index) => `${index === 0 ? 'M' : 'L'} ${point.x},${point.y}`).join(' ')
}

function polylineLabel(points: { x: number; y: number }[]) {
  let total = 0
  const lengths: number[] = []
  for (let i = 1; i < points.length; i += 1) {
    const length = Math.hypot(points[i].x - points[i - 1].x, points[i].y - points[i - 1].y)
    lengths.push(length)
    total += length
  }

  let remaining = total / 2
  let segment = 0
  while (segment < lengths.length - 1 && remaining > lengths[segment]) {
    remaining -= lengths[segment]
    segment += 1
  }

  const a = points[segment]
  const b = points[segment + 1]
  const length = lengths[segment] || 1
  const t = remaining / length
  const midX = a.x + (b.x - a.x) * t
  const midY = a.y + (b.y - a.y) * t
  let normalX = -(b.y - a.y) / length
  let normalY = (b.x - a.x) / length
  const dx = b.x - a.x
  const dy = b.y - a.y

  if (Math.abs(dx) >= Math.abs(dy)) {
    if (normalY > 0) { normalX = -normalX; normalY = -normalY }
  } else if (normalX < 0) {
    normalX = -normalX
    normalY = -normalY
  }

  return { midX, midY, labelX: midX + normalX * 14, labelY: midY + normalY * 14 }
}

function distanceToSegment(point: { x: number; y: number }, a: { x: number; y: number }, b: { x: number; y: number }) {
  const dx = b.x - a.x
  const dy = b.y - a.y
  const lengthSq = dx * dx + dy * dy
  if (!lengthSq) return Math.hypot(point.x - a.x, point.y - a.y)
  const t = Math.max(0, Math.min(1, ((point.x - a.x) * dx + (point.y - a.y) * dy) / lengthSq))
  return Math.hypot(point.x - (a.x + dx * t), point.y - (a.y + dy * t))
}

function edgeGeometry(props: EdgeProps, loopSide?: AnchorSide, waypoints: EdgeWaypoint[] = []): {
  path: string
  midX: number
  midY: number
  labelX: number
  labelY: number
  sourceX: number
  sourceY: number
  targetX: number
  targetY: number
} {
  const { id, source, target, sourceX, sourceY, targetX, targetY, sourcePosition, targetPosition } = props
  const sx = sourceX
  const sy = sourceY
  const tx = targetX
  const ty = targetY

  if (waypoints.length > 0) {
    const points = [{ x: sx, y: sy }, ...waypoints, { x: tx, y: ty }]
    return {
      path: pathFromPoints(points),
      ...polylineLabel(points),
      sourceX: sx,
      sourceY: sy,
      targetX: tx,
      targetY: ty,
    }
  }

  // Recursive relationship: rectangular loop made only of straight segments.
  // Each self-relation uses a different external lane so multiple loops do not overlap.
  if (source === target) {
    const side = loopSide ?? sourcePosition ?? targetPosition ?? 'right'
    const lane = 110 + (hashString(id) % 4) * 28
    const gap = 14

    if (side === 'left') {
      const x = Math.min(sx, tx) - lane
      const path = `M ${sx},${sy} L ${x},${sy} L ${x},${ty} L ${tx},${ty}`
      return {
        path,
        midX: x,
        midY: (sy + ty) / 2,
        labelX: x - gap,
        labelY: (sy + ty) / 2,
        sourceX: sx,
        sourceY: sy,
        targetX: tx,
        targetY: ty,
      }
    }
    if (side === 'top') {
      const y = Math.min(sy, ty) - lane
      const path = `M ${sx},${sy} L ${sx},${y} L ${tx},${y} L ${tx},${ty}`
      return {
        path,
        midX: (sx + tx) / 2,
        midY: y,
        labelX: (sx + tx) / 2,
        labelY: y - gap,
        sourceX: sx,
        sourceY: sy,
        targetX: tx,
        targetY: ty,
      }
    }
    if (side === 'bottom') {
      const y = Math.max(sy, ty) + lane
      const path = `M ${sx},${sy} L ${sx},${y} L ${tx},${y} L ${tx},${ty}`
      return {
        path,
        midX: (sx + tx) / 2,
        midY: y,
        labelX: (sx + tx) / 2,
        labelY: y + gap,
        sourceX: sx,
        sourceY: sy,
        targetX: tx,
        targetY: ty,
      }
    }

    const x = Math.max(sx, tx) + lane
    const path = `M ${sx},${sy} L ${x},${sy} L ${x},${ty} L ${tx},${ty}`
    return {
      path,
      midX: x,
      midY: (sy + ty) / 2,
      labelX: x + gap,
      labelY: (sy + ty) / 2,
      sourceX: sx,
      sourceY: sy,
      targetX: tx,
      targetY: ty,
    }
  }

  const [path] = getStraightPath({ sourceX: sx, sourceY: sy, targetX: tx, targetY: ty })
  const midX = (sx + tx) / 2
  const midY = (sy + ty) / 2
  const dx = tx - sx
  const dy = ty - sy
  const length = Math.hypot(dx, dy) || 1
  let normalX = -dy / length
  let normalY = dx / length

  // Horizontal lines: label above. Vertical lines: label to the right.
  if (Math.abs(dx) >= Math.abs(dy)) {
    if (normalY > 0) {
      normalX = -normalX
      normalY = -normalY
    }
  } else if (normalX < 0) {
    normalX = -normalX
    normalY = -normalY
  }

  const gap = 14
  return {
    path,
    midX,
    midY,
    labelX: midX + normalX * gap,
    labelY: midY + normalY * gap,
    sourceX: sx,
    sourceY: sy,
    targetX: tx,
    targetY: ty,
  }
}

function trianglePath(x: number, y: number, dir: [number, number], len = 14, half = 7): string {
  const [dx, dy] = dir
  const px = -dy, py = dx
  const bx = x - dx * len, by = y - dy * len
  return `M ${x},${y} L ${bx + px * half},${by + py * half} L ${bx - px * half},${by - py * half} Z`
}

function diamondPath(x: number, y: number, dir: [number, number], len = 9, half = 5.5): string {
  const [dx, dy] = dir
  const px = -dy, py = dx
  return `M ${x},${y} L ${x + dx * len + px * half},${y + dy * len + py * half} L ${x + dx * len * 2},${y + dy * len * 2} L ${x + dx * len - px * half},${y + dy * len - py * half} Z`
}

function EdgeMid({ x, y, name }: { x: number; y: number; name?: string }) {
  if (!name) return null
  return (
    <EdgeLabelRenderer>
      <div className="nodrag nopan"
        style={{ position: 'absolute', transform: `translate(-50%, -50%) translate(${x}px,${y}px)`, pointerEvents: 'none' }}>
        <div style={{ fontSize: 10, color: '#111827' }}>{name}</div>
      </div>
    </EdgeLabelRenderer>
  )
}

function EdgeEndpoint({ x, y, color, onPointerDown }: {
  x: number
  y: number
  color: string
  onPointerDown: (event: ReactPointerEvent<HTMLDivElement>) => void
}) {
  return (
    <EdgeLabelRenderer>
      <div
        className="nodrag nopan"
        onPointerDown={onPointerDown}
        onClick={(event) => event.stopPropagation()}
        style={{
          position: 'absolute', transform: `translate(-50%, -50%) translate(${x}px,${y}px)`,
          width: 14, height: 14, borderRadius: '50%', background: '#ffffff',
          border: `1.5px solid ${color}`, pointerEvents: 'all', cursor: 'grab', zIndex: 9999,
        }}
      />
    </EdgeLabelRenderer>
  )
}

function EdgeWaypointHandle({ x, y, color, onPointerDown, onContextMenu }: {
  x: number
  y: number
  color: string
  onPointerDown: (event: ReactPointerEvent<HTMLDivElement>) => void
  onContextMenu: (event: ReactMouseEvent<HTMLDivElement>) => void
}) {
  return (
    <EdgeLabelRenderer>
      <div
        className="nodrag nopan"
        onPointerDown={onPointerDown}
        onContextMenu={onContextMenu}
        onClick={(event) => event.stopPropagation()}
        style={{
          position: 'absolute', transform: `translate(-50%, -50%) translate(${x}px,${y}px)`,
          width: 10, height: 10, borderRadius: 2, background: '#ffffff',
          border: `1.5px solid ${color}`, pointerEvents: 'all', cursor: 'move', zIndex: 9999,
        }}
      />
    </EdgeLabelRenderer>
  )
}

function EndLabel({ x, y, text }: { x: number; y: number; text?: string }) {
  if (!text) return null
  return (
    <EdgeLabelRenderer>
      <div style={{ position: 'absolute', transform: `translate(-50%, -50%) translate(${x}px,${y}px)`, fontSize: 10, fontWeight: 600, color: '#111827', background: '#ffffff', padding: '0 3px', borderRadius: 0, pointerEvents: 'none' }}>
        {text}
      </div>
    </EdgeLabelRenderer>
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

function UmlEdge({ kind, props }: { kind: Kind; props: EdgeProps }) {
  const { id, selected, data, source, target, sourcePosition, targetPosition } = props
  const sourceNode = useInternalNode(source)
  const targetNode = useInternalNode(target)
  const { screenToFlowPosition } = useReactFlow()
  const canEdit = useStore((store) => store.nodesConnectable)
  const [dragAnchor, setDragAnchor] = useState<{ end: AnchorEnd; anchor: EdgeAnchor } | null>(null)
  const [dragWaypoint, setDragWaypoint] = useState<{ index: number; point: EdgeWaypoint } | null>(null)
  const {
    name, sourceMultiplicity, targetMultiplicity, associationClassElementId,
    sourceAnchor: sourceAnchorData, targetAnchor: targetAnchorData,
    waypoints: waypointsData, onAnchorChange, onWaypointsChange,
  } = relInfo(data)

  const baseSourceAnchor: EdgeAnchor = dragAnchor?.end === 'source'
    ? dragAnchor.anchor
    : (sourceAnchorData ?? { side: sideFromPosition(sourcePosition, 'bottom'), percent: 50 })
  const baseTargetAnchor: EdgeAnchor = dragAnchor?.end === 'target'
    ? dragAnchor.anchor
    : (targetAnchorData ?? { side: sideFromPosition(targetPosition, 'top'), percent: 50 })
  const auto = source === target ? null : autoSides(sourceNode, targetNode)
  const sourceAnchor: EdgeAnchor = auto ? { ...baseSourceAnchor, side: auto.source } : baseSourceAnchor
  const targetAnchor: EdgeAnchor = auto ? { ...baseTargetAnchor, side: auto.target } : baseTargetAnchor

  const waypoints = (waypointsData ?? []).map((point, index) => dragWaypoint?.index === index ? dragWaypoint.point : point)
  const sourcePoint = anchorPoint(sourceNode, sourceAnchor, props.sourceX, props.sourceY)
  const targetPoint = anchorPoint(targetNode, targetAnchor, props.targetX, props.targetY)
  const { path, midX, midY, labelX, labelY } = edgeGeometry({
    ...props,
    sourceX: sourcePoint.x,
    sourceY: sourcePoint.y,
    targetX: targetPoint.x,
    targetY: targetPoint.y,
  }, sourceAnchor.side, waypoints)

  const stroke = selected ? EDGE_SELECTED : EDGE_COLOR
  const dashed = kind === 'realization'
  const lastPoint = waypoints.length ? waypoints[waypoints.length - 1] : sourcePoint
  const lineX = targetPoint.x - lastPoint.x
  const lineY = targetPoint.y - lastPoint.y
  const lineLength = Math.hypot(lineX, lineY) || 1
  const targetIn: [number, number] = source === target && waypoints.length === 0
    ? (TARGET_DIR[targetAnchor.side] ?? [0, -1])
    : [lineX / lineLength, lineY / lineLength]
  const targetOut: [number, number] = [-targetIn[0], -targetIn[1]]
  const sourceOut = SOURCE_DIR[sourceAnchor.side] ?? [0, 1]

  // Multiplicity labels are offset away from the node along the handle normal
  const sPerp: [number, number] = [-sourceOut[1], sourceOut[0]]
  const sourceLabelX = sourcePoint.x + sourceOut[0] * 16 + sPerp[0] * 14
  const sourceLabelY = sourcePoint.y + sourceOut[1] * 16 + sPerp[1] * 14
  const tPerp: [number, number] = [-targetOut[1], -targetOut[0]]
  const targetLabelX = targetPoint.x + targetOut[0] * 16 + tPerp[0] * 14
  const targetLabelY = targetPoint.y + targetOut[1] * 16 + tPerp[1] * 14

  const startAnchorDrag = (end: AnchorEnd) => (event: ReactPointerEvent<HTMLDivElement>) => {
    if (!selected || !onAnchorChange || !canEdit) return
    const node = end === 'source' ? sourceNode : targetNode
    if (!node) return

    event.stopPropagation()
    event.preventDefault()
    const side = end === 'source' ? sourceAnchor.side : targetAnchor.side

    const onMove = (moveEvent: PointerEvent) => {
      const point = screenToFlowPosition({ x: moveEvent.clientX, y: moveEvent.clientY })
      setDragAnchor({ end, anchor: projectAnchor(node, side, point) })
    }
    const onUp = (upEvent: PointerEvent) => {
      window.removeEventListener('pointermove', onMove)
      window.removeEventListener('pointerup', onUp)
      const point = screenToFlowPosition({ x: upEvent.clientX, y: upEvent.clientY })
      const anchor = projectAnchor(node, side, point)
      setDragAnchor(null)
      onAnchorChange(id, end, anchor)
    }

    window.addEventListener('pointermove', onMove)
    window.addEventListener('pointerup', onUp)
  }

  const startWaypointDrag = (index: number) => (event: ReactPointerEvent<HTMLDivElement>) => {
    if (!selected || !onWaypointsChange || !canEdit) return

    event.stopPropagation()
    event.preventDefault()
    const onMove = (moveEvent: PointerEvent) => {
      const point = screenToFlowPosition({ x: moveEvent.clientX, y: moveEvent.clientY })
      setDragWaypoint({ index, point: { x: Math.round(point.x), y: Math.round(point.y) } })
    }
    const onUp = (upEvent: PointerEvent) => {
      window.removeEventListener('pointermove', onMove)
      window.removeEventListener('pointerup', onUp)
      const point = screenToFlowPosition({ x: upEvent.clientX, y: upEvent.clientY })
      const next = waypoints.map((current, currentIndex) => currentIndex === index
        ? { x: Math.round(point.x), y: Math.round(point.y) }
        : current)
      setDragWaypoint(null)
      onWaypointsChange(id, next)
    }

    window.addEventListener('pointermove', onMove)
    window.addEventListener('pointerup', onUp)
  }

  const addWaypoint = (event: ReactMouseEvent<SVGPathElement>) => {
    event.preventDefault()
    event.stopPropagation()
    if (!canEdit || !onWaypointsChange) return

    const point = screenToFlowPosition({ x: event.clientX, y: event.clientY })
    const points = [
      { x: sourcePoint.x, y: sourcePoint.y },
      ...waypoints,
      { x: targetPoint.x, y: targetPoint.y },
    ]
    let insertAt = 0
    let bestDistance = Infinity
    for (let index = 0; index < points.length - 1; index += 1) {
      const distance = distanceToSegment(point, points[index], points[index + 1])
      if (distance < bestDistance) {
        bestDistance = distance
        insertAt = index
      }
    }

    const next = [...waypoints]
    next.splice(insertAt, 0, { x: Math.round(point.x), y: Math.round(point.y) })
    onWaypointsChange(id, next)
  }

  const removeWaypoint = (index: number) => (event: ReactMouseEvent<HTMLDivElement>) => {
    event.preventDefault()
    event.stopPropagation()
    if (!onWaypointsChange) return
    onWaypointsChange(id, waypoints.filter((_, currentIndex) => currentIndex !== index))
  }

  return (
    <>
      {kind === 'association_class' && associationClassElementId && (
        <AssociationClassConnector elementId={associationClassElementId} midX={midX} midY={midY} selected={selected} />
      )}
      <svg style={{ position: 'absolute', width: '100%', height: '100%', pointerEvents: 'none', overflow: 'visible' }}>
        <path d={path} fill="none" stroke={stroke} strokeWidth={2}
          strokeDasharray={dashed ? '5,5' : undefined} />

        {(kind === 'generalization' || kind === 'realization') && (
          <path d={trianglePath(targetPoint.x, targetPoint.y, targetIn)} fill={NODE_BG} stroke={stroke} strokeWidth={1.5} />
        )}

        {(kind === 'aggregation' || kind === 'composition') && (
          <path d={diamondPath(targetPoint.x, targetPoint.y, targetOut)}
            fill={kind === 'composition' ? stroke : NODE_BG} stroke={stroke} strokeWidth={1.5} />
        )}
        <path d={path} fill="none" stroke="transparent" strokeWidth={20}
          style={{ pointerEvents: 'stroke' }} onContextMenu={addWaypoint} />

      </svg>
      <EndLabel x={sourceLabelX} y={sourceLabelY} text={sourceMultiplicity} />
      <EndLabel x={targetLabelX} y={targetLabelY} text={targetMultiplicity} />
      <EdgeMid x={labelX} y={labelY} name={name} />
      {selected && canEdit && (
        <>
          <EdgeEndpoint x={sourcePoint.x} y={sourcePoint.y} color={stroke} onPointerDown={startAnchorDrag('source')} />
          <EdgeEndpoint x={targetPoint.x} y={targetPoint.y} color={stroke} onPointerDown={startAnchorDrag('target')} />
          {waypoints.map((point, index) => (
            <EdgeWaypointHandle key={`waypoint-${index}`} x={point.x} y={point.y} color={stroke}
              onPointerDown={startWaypointDrag(index)} onContextMenu={removeWaypoint(index)} />
          ))}
        </>
      )}
    </>
  )
}

export function AssociationEdge(props: EdgeProps) { return <UmlEdge kind="association" props={props} /> }
export function AssociationClassEdge(props: EdgeProps) { return <UmlEdge kind="association_class" props={props} /> }
export function GeneralizationEdge(props: EdgeProps) { return <UmlEdge kind="generalization" props={props} /> }
export function RealizationEdge(props: EdgeProps) { return <UmlEdge kind="realization" props={props} /> }
export function AggregationEdge(props: EdgeProps) { return <UmlEdge kind="aggregation" props={props} /> }
export function CompositionEdge(props: EdgeProps) { return <UmlEdge kind="composition" props={props} /> }
