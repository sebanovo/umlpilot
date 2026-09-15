import { EdgeLabelRenderer, getBezierPath, useInternalNode, type EdgeProps } from '@xyflow/react'

const EDGE_COLOR = '#9ca3af'
const EDGE_SELECTED = '#60a5fa'
const NODE_BG = '#111827'

type Kind =
  | 'association' | 'association_class' | 'generalization' | 'realization'
  | 'dependency' | 'aggregation' | 'composition' | 'template_binding'

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

// Direction the line takes leaving the source handle / entering the target handle
const SOURCE_DIR: Record<string, [number, number]> = {
  top: [0, -1], bottom: [0, 1], left: [-1, 0], right: [1, 0],
}
const TARGET_DIR: Record<string, [number, number]> = {
  top: [0, 1], bottom: [0, -1], left: [1, 0], right: [-1, 0],
}

function edgeGeometry(props: EdgeProps): { path: string; labelX: number; labelY: number } {
  const { source, target, sourceX, sourceY, targetX, targetY, sourcePosition, targetPosition } = props

  // Recursive relationship: orthogonal loop with extra room
  if (source === target) {
    const d = 170
    const path = `M ${sourceX},${sourceY} L ${sourceX + d},${sourceY} L ${sourceX + d},${targetY} L ${targetX},${targetY}`
    return { path, labelX: sourceX + d, labelY: (sourceY + targetY) / 2 }
  }

  const [path, labelX, labelY] = getBezierPath({ sourceX, sourceY, sourcePosition, targetX, targetY, targetPosition })
  return { path, labelX, labelY }
}

function trianglePath(x: number, y: number, dir: [number, number], len = 14, half = 7): string {
  const [dx, dy] = dir
  const px = -dy, py = dx
  const bx = x - dx * len, by = y - dy * len
  return `M ${x},${y} L ${bx + px * half},${by + py * half} L ${bx - px * half},${by - py * half} Z`
}

function openArrowPath(x: number, y: number, dir: [number, number], len = 12, half = 6): string {
  const [dx, dy] = dir
  const px = -dy, py = dx
  return `M ${x - dx * len + px * half},${y - dy * len + py * half} L ${x},${y} L ${x - dx * len - px * half},${y - dy * len - py * half}`
}

function diamondPath(x: number, y: number, dir: [number, number], len = 9, half = 5.5): string {
  const [dx, dy] = dir
  const px = -dy, py = dx
  return `M ${x},${y} L ${x + dx * len + px * half},${y + dy * len + py * half} L ${x + dx * len * 2},${y + dy * len * 2} L ${x + dx * len - px * half},${y + dy * len - py * half} Z`
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
  const { selected, data, sourceX, sourceY, targetX, targetY, sourcePosition, targetPosition } = props
  const { path, labelX, labelY } = edgeGeometry(props)
  const { name, sourceMultiplicity, targetMultiplicity, associationClassElementId, onDelete } = relInfo(data)

  const stroke = selected ? EDGE_SELECTED : EDGE_COLOR
  const dashed = kind === 'dependency' || kind === 'realization' || kind === 'template_binding'
  const thin = kind === 'dependency' || kind === 'template_binding'
  const targetIn = TARGET_DIR[targetPosition ?? 'top'] ?? [0, -1]
  const targetOut = SOURCE_DIR[targetPosition ?? 'top'] ?? [0, -1]
  const sourceOut = SOURCE_DIR[sourcePosition ?? 'bottom'] ?? [0, 1]

  // Multiplicity labels are offset away from the node along the handle normal
  const sPerp: [number, number] = [-sourceOut[1], sourceOut[0]]
  const sourceLabelX = sourceX + sourceOut[0] * 16 + sPerp[0] * 14
  const sourceLabelY = sourceY + sourceOut[1] * 16 + sPerp[1] * 14
  const tPerp: [number, number] = [-targetOut[1], -targetOut[0]]
  const targetLabelX = targetX + targetOut[0] * 16 + tPerp[0] * 14
  const targetLabelY = targetY + targetOut[1] * 16 + tPerp[1] * 14

  return (
    <>
      {kind === 'association_class' && associationClassElementId && (
        <AssociationClassConnector elementId={associationClassElementId} midX={labelX} midY={labelY} selected={selected} />
      )}
      <svg style={{ position: 'absolute', width: '100%', height: '100%', pointerEvents: 'none', overflow: 'visible' }}>
        <path d={path} fill="none" stroke={stroke} strokeWidth={thin ? 1.5 : 2}
          strokeDasharray={dashed ? '5,5' : undefined} />

        {(kind === 'generalization' || kind === 'realization') && (
          <path d={trianglePath(targetX, targetY, targetIn)} fill={NODE_BG} stroke={stroke} strokeWidth={1.5} />
        )}

        {(kind === 'dependency' || kind === 'template_binding') && (
          <path d={openArrowPath(targetX, targetY, targetIn)} fill="none" stroke={stroke} strokeWidth={1.5} />
        )}

        {(kind === 'aggregation' || kind === 'composition') && (
          <path d={diamondPath(targetX, targetY, targetOut)}
            fill={kind === 'composition' ? stroke : NODE_BG} stroke={stroke} strokeWidth={1.5} />
        )}

        <path d={path} fill="none" stroke="transparent" strokeWidth={20} style={{ pointerEvents: 'stroke' }} />
      </svg>
      <EndLabel x={sourceLabelX} y={sourceLabelY} text={sourceMultiplicity} />
      <EndLabel x={targetLabelX} y={targetLabelY} text={targetMultiplicity} />
      <EdgeMid x={labelX} y={labelY} name={kind === 'template_binding' ? (name || '«bind»') : name} selected={selected} onDelete={onDelete} />
    </>
  )
}

export function AssociationEdge(props: EdgeProps) { return <UmlEdge kind="association" props={props} /> }
export function AssociationClassEdge(props: EdgeProps) { return <UmlEdge kind="association_class" props={props} /> }
export function GeneralizationEdge(props: EdgeProps) { return <UmlEdge kind="generalization" props={props} /> }
export function RealizationEdge(props: EdgeProps) { return <UmlEdge kind="realization" props={props} /> }
export function DependencyEdge(props: EdgeProps) { return <UmlEdge kind="dependency" props={props} /> }
export function TemplateBindingEdge(props: EdgeProps) { return <UmlEdge kind="template_binding" props={props} /> }
export function AggregationEdge(props: EdgeProps) { return <UmlEdge kind="aggregation" props={props} /> }
export function CompositionEdge(props: EdgeProps) { return <UmlEdge kind="composition" props={props} /> }
