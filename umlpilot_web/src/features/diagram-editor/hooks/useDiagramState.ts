import { useState, useCallback, useRef, useEffect } from 'react'
import type { Node, Edge, OnNodesChange, OnEdgesChange } from '@xyflow/react'
import { applyNodeChanges, applyEdgeChanges } from '@xyflow/react'
import { elementsApi, relationshipsApi } from '../../../shared/api/client'
import type { UmlElementApi, UmlAttributeApi, UmlMethodApi, UmlRelationshipApi, MultiplicityApi, UmlParameterApi } from '../../../shared/api/client'
import { useWebSocket } from '../../../shared/lib/useWebSocket'
import { anchorStorageValue, edgeHandleFor, parseEdgeAnchor, type AnchorEnd, type AnchorSide, type EdgeAnchor } from '../lib/anchors'
import { parseWaypoints, serializeWaypoints, type EdgeWaypoint } from '../lib/waypoints'

const EDGE_TYPES = new Set([
  'association', 'association_class', 'generalization', 'realization',
  'aggregation', 'composition',
])

function edgeTypeFor(relType?: string | null): string {
  const t = (relType || 'association').toLowerCase()
  return EDGE_TYPES.has(t) ? t : 'association'
}

function nodeTypeFor(elementType?: string): string {
  if (elementType === 'enumeration') return 'enumeration'
  if (elementType === 'interface') return 'interface'
  if (elementType === 'note') return 'note'
  return 'class'
}

type AnchorChangeHandler = (relationshipId: string, end: AnchorEnd, anchor: EdgeAnchor) => void
type WaypointChangeHandler = (relationshipId: string, waypoints: EdgeWaypoint[]) => void

function bestSides(source?: { position: { x: number; y: number } }, target?: { position: { x: number; y: number } }): { source: AnchorSide; target: AnchorSide } {
  const dx = (target?.position.x ?? 0) - (source?.position.x ?? 0)
  const dy = (target?.position.y ?? 0) - (source?.position.y ?? 0)
  if (Math.abs(dx) >= Math.abs(dy)) {
    return dx >= 0 ? { source: 'right', target: 'left' } : { source: 'left', target: 'right' }
  }
  return dy >= 0 ? { source: 'bottom', target: 'top' } : { source: 'top', target: 'bottom' }
}

function formatMultiplicity(m?: MultiplicityApi): string {
  if (!m || m.min == null || m.max == null) return ''
  const maxLabel = m.max < 0 ? '*' : String(m.max)
  if (m.min === m.max && m.max >= 0) return String(m.min)
  return `${m.min}..${maxLabel}`
}

async function buildEdges(projectId: string, diagramId: string, rels: UmlRelationshipApi[], onDelete: (id: string) => void, onAnchorChange: AnchorChangeHandler, onWaypointsChange: WaypointChangeHandler): Promise<Edge[]> {
  const entries = await Promise.all(rels.map(async (rel) => {
    try {
      const list = await relationshipsApi.getMultiplicities(projectId, diagramId, rel.id)
      return [rel.id, list] as const
    } catch {
      return [rel.id, [] as MultiplicityApi[]] as const
    }
  }))
  const byRel = new Map<string, MultiplicityApi[]>(entries)

  return rels.map((rel) => {
    const list = byRel.get(rel.id) || []
    const source = list.find((m) => m.end === 'source')
    const target = list.find((m) => m.end === 'target')
    const isSelfLoop = rel.sourceElementId === rel.targetElementId
    let sourceAnchor: EdgeAnchor = parseEdgeAnchor(rel.sourceHandle, isSelfLoop ? 'right' : 'bottom')
    let targetAnchor: EdgeAnchor = parseEdgeAnchor(rel.targetHandle, isSelfLoop ? 'right' : 'top')
    if (isSelfLoop && sourceAnchor.side === targetAnchor.side && sourceAnchor.percent === targetAnchor.percent) {
      sourceAnchor = { side: 'right', percent: 10 }
      targetAnchor = { side: 'right', percent: 90 }
    }
    return {
      id: rel.id,
      source: rel.sourceElementId,
      target: rel.targetElementId,
      sourceHandle: edgeHandleFor(sourceAnchor, 'source'),
      targetHandle: edgeHandleFor(targetAnchor, 'target'),
      type: edgeTypeFor(rel.relationshipType),
      data: {
        relationshipType: rel.relationshipType,
        name: rel.name || '',
        associationClassElementId: rel.associationClassElementId || null,
        sourceMultiplicity: formatMultiplicity(source),
        targetMultiplicity: formatMultiplicity(target),
        sourceAnchor,
        targetAnchor,
        waypoints: parseWaypoints(rel.waypoints),
        onAnchorChange,
        onWaypointsChange,
        onDelete: () => onDelete(rel.id),
      },
    }
  })
}

interface UseDiagramStateProps {
  projectId: string
  diagramId: string
}

interface DiagramState {
  nodes: Node[]
  edges: Edge[]
  elements: UmlElementApi[]
  attributes: Record<string, UmlAttributeApi[]>
  methods: Record<string, UmlMethodApi[]>
  relationships: UmlRelationshipApi[]
  selectedElement: UmlElementApi | null
  selectedAttributes: UmlAttributeApi[]
  selectedMethods: UmlMethodApi[]
  loading: boolean
  loadError: string | null
}

export function useDiagramState({ projectId, diagramId }: UseDiagramStateProps) {
  const [state, setState] = useState<DiagramState>({
    nodes: [], edges: [], elements: [], attributes: {}, methods: {},
    relationships: [], selectedElement: null, selectedAttributes: [], selectedMethods: [],
    loading: true, loadError: null,
  })

  const pendingChanges = useRef<Map<string, Record<string, unknown>>>(new Map())
  const syncTimer = useRef<ReturnType<typeof setTimeout> | null>(null)
  const lastMoveBroadcast = useRef<Map<string, number>>(new Map())
  const deleteRelationshipRef = useRef<(id: string) => void>(() => {})

  const handleDeleteEdge = useCallback((id: string) => deleteRelationshipRef.current(id), [])

  const deleteElementRef = useRef<(id: string) => void>(() => {})
  const handleDeleteNode = useCallback((id: string) => deleteElementRef.current(id), [])

  const patchElementRef = useRef<(id: string, data: Partial<UmlElementApi>) => void>(() => {})
  const handleNoteText = useCallback((id: string, text: string) => patchElementRef.current(id, { name: text }), [])

  const [activeRelationship, setActiveRelationship] = useState<string | null>(null)
  const [selectedRelationship, setSelectedRelationship] = useState<UmlRelationshipApi | null>(null)
  const [relationshipMultiplicities, setRelationshipMultiplicities] = useState<MultiplicityApi[]>([])

  // Get current user from localStorage
  const storedUser = JSON.parse(localStorage.getItem('user') || '{}')
  const userId = storedUser?.id || ''
  const userName = [storedUser?.firstName, storedUser?.lastName].filter(Boolean).join(' ')
    || storedUser?.email || 'Colaborador'

  // Connect WebSocket
  const {
    connected, presenceUsers, remoteChanges, remoteCursors,
    broadcastElementChange, broadcastCursor, clearRemoteChanges,
  } = useWebSocket(diagramId, userId)

  // Broadcast local pointer position (throttled)
  const lastCursorSent = useRef(0)
  const handleCursorMove = useCallback((x: number, y: number) => {
    const now = Date.now()
    if (now - lastCursorSent.current < 50) return
    lastCursorSent.current = now
    broadcastCursor(x, y, userName)
  }, [broadcastCursor, userName])

  const rebuildDiagramRef = useRef<() => Promise<void>>(async () => {})

  const updateRelationshipAnchor = useCallback(async (relationshipId: string, end: AnchorEnd, anchor: EdgeAnchor) => {
    const handle = anchorStorageValue(anchor, end)
    const anchorKey = end === 'source' ? 'sourceAnchor' : 'targetAnchor'

    setState(prev => ({
      ...prev,
      relationships: prev.relationships.map((r) => r.id === relationshipId
        ? (end === 'source' ? { ...r, sourceHandle: handle } : { ...r, targetHandle: handle })
        : r),
      edges: prev.edges.map((e) => e.id === relationshipId
        ? { ...e, data: { ...(e.data || {}), [anchorKey]: anchor } }
        : e),
    }))

    try {
      const payload = end === 'source' ? { sourceHandle: handle } : { targetHandle: handle }
      await relationshipsApi.update(projectId, diagramId, relationshipId, payload)
      broadcastElementChange('relationship_updated', relationshipId)
    } catch {
      rebuildDiagramRef.current().catch(() => {})
    }
  }, [projectId, diagramId, broadcastElementChange])

  const updateRelationshipWaypoints = useCallback(async (relationshipId: string, waypoints: EdgeWaypoint[]) => {
    const value = serializeWaypoints(waypoints)

    setState(prev => ({
      ...prev,
      relationships: prev.relationships.map((r) => r.id === relationshipId ? { ...r, waypoints: value } : r),
      edges: prev.edges.map((e) => e.id === relationshipId
        ? { ...e, data: { ...(e.data || {}), waypoints } }
        : e),
    }))

    try {
      await relationshipsApi.updateWaypoints(projectId, diagramId, relationshipId, value)
      broadcastElementChange('relationship_updated', relationshipId)
    } catch {
      rebuildDiagramRef.current().catch(() => {})
    }
  }, [projectId, diagramId, broadcastElementChange])

  // Load initial data
  useEffect(() => {
    if (!projectId || !diagramId) return
    Promise.all([
      elementsApi.list(projectId, diagramId),
      relationshipsApi.list(projectId, diagramId),
    ]).then(async ([els, rels]) => {
      const attrMap: Record<string, UmlAttributeApi[]> = {}
      const methMap: Record<string, UmlMethodApi[]> = {}

      await Promise.all(els.map(async (el: UmlElementApi) => {
        const [attrs, meths] = await Promise.all([
          elementsApi.getAttributes(projectId, diagramId, el.id),
          elementsApi.getMethods(projectId, diagramId, el.id),
        ])
        attrMap[el.id] = attrs
        methMap[el.id] = meths
      }))

      const nodes: Node[] = els.map((el: UmlElementApi) => ({
        id: el.id,
        type: nodeTypeFor(el.elementType),
        position: { x: el.positionX, y: el.positionY },
        data: { name: el.name, visibility: el.visibility, stereotype: el.stereotype, attributes: attrMap[el.id] || [], methods: methMap[el.id] || [], literals: [], onDeleteNode: handleDeleteNode, onRenameNode: handleNoteText },
      }))

      const edges: Edge[] = await buildEdges(projectId, diagramId, rels, handleDeleteEdge, updateRelationshipAnchor, updateRelationshipWaypoints)

      setState(prev => ({
        ...prev, nodes, edges, elements: els, attributes: attrMap, methods: methMap, relationships: rels, loading: false,
      }))
    }).catch((error) => setState(prev => ({
      ...prev,
      loading: false,
      loadError: error instanceof Error ? error.message : 'No tienes acceso a este diagrama',
    })))
  }, [projectId, diagramId, handleDeleteEdge, handleNoteText, handleDeleteNode, updateRelationshipAnchor, updateRelationshipWaypoints])

  // Rebuild nodes/edges from server (used when a remote change arrives)
  const rebuildDiagram = useCallback(async () => {
    const [els, rels] = await Promise.all([
      elementsApi.list(projectId, diagramId),
      relationshipsApi.list(projectId, diagramId),
    ])
    const attrMap: Record<string, UmlAttributeApi[]> = {}
    const methMap: Record<string, UmlMethodApi[]> = {}

    await Promise.all(els.map(async (el: UmlElementApi) => {
      const [attrs, meths] = await Promise.all([
        elementsApi.getAttributes(projectId, diagramId, el.id),
        elementsApi.getMethods(projectId, diagramId, el.id),
      ])
      attrMap[el.id] = attrs
      methMap[el.id] = meths
    }))

    const nodes: Node[] = els.map((el: UmlElementApi) => ({
      id: el.id,
      type: nodeTypeFor(el.elementType),
      position: { x: el.positionX, y: el.positionY },
      data: { name: el.name, visibility: el.visibility, stereotype: el.stereotype, attributes: attrMap[el.id] || [], methods: methMap[el.id] || [], literals: [], onDeleteNode: handleDeleteNode, onRenameNode: handleNoteText },
    }))

    const edges: Edge[] = await buildEdges(projectId, diagramId, rels, handleDeleteEdge, updateRelationshipAnchor, updateRelationshipWaypoints)

    setState(prev => ({
      ...prev, nodes, edges, elements: els, attributes: attrMap, methods: methMap, relationships: rels,
      selectedElement: prev.selectedElement ? els.find((e) => e.id === prev.selectedElement!.id) || prev.selectedElement : null,
    }))
  }, [projectId, diagramId, handleDeleteEdge, handleNoteText, handleDeleteNode, updateRelationshipAnchor, updateRelationshipWaypoints])

  useEffect(() => {
    rebuildDiagramRef.current = rebuildDiagram
  }, [rebuildDiagram])

  // Delete relationship — optimistic + broadcast
  const deleteRelationship = useCallback(async (relationshipId: string) => {
    const rel = state.relationships.find((r) => r.id === relationshipId)
    const assocElementId = rel?.associationClassElementId || null

    setState(prev => {
      const next = {
        ...prev,
        edges: prev.edges.filter((e) => e.id !== relationshipId),
        relationships: prev.relationships.filter((r) => r.id !== relationshipId),
      }
      if (assocElementId) {
        next.nodes = prev.nodes.filter((n) => n.id !== assocElementId)
        next.elements = prev.elements.filter((e) => e.id !== assocElementId)
        next.edges = next.edges.filter((e) => e.source !== assocElementId && e.target !== assocElementId)
        if (next.selectedElement?.id === assocElementId) next.selectedElement = null
      }
      return next
    })
    try {
      await relationshipsApi.delete(projectId, diagramId, relationshipId)
      setSelectedRelationship((sr) => sr?.id === relationshipId ? null : sr)
      broadcastElementChange('relationship_deleted', relationshipId, { relationshipId })
      if (assocElementId) {
        await elementsApi.delete(projectId, diagramId, assocElementId).catch(() => {})
        broadcastElementChange('element_deleted', assocElementId, { elementId: assocElementId })
      }
    } catch {
      rebuildDiagram().catch(() => {})
    }
  }, [projectId, diagramId, broadcastElementChange, rebuildDiagram, state.relationships])

  useEffect(() => {
    deleteRelationshipRef.current = deleteRelationship
  }, [deleteRelationship])

  // Delete element — optimistic + broadcast
  const deleteElement = useCallback(async (elementId: string) => {
    setState(prev => ({
      ...prev,
      nodes: prev.nodes.filter((n) => n.id !== elementId),
      elements: prev.elements.filter((e) => e.id !== elementId),
      edges: prev.edges.filter((e) => e.source !== elementId && e.target !== elementId),
      selectedElement: prev.selectedElement?.id === elementId ? null : prev.selectedElement,
      selectedAttributes: prev.selectedElement?.id === elementId ? [] : prev.selectedAttributes,
      selectedMethods: prev.selectedElement?.id === elementId ? [] : prev.selectedMethods,
    }))
    try {
      await elementsApi.delete(projectId, diagramId, elementId)
      broadcastElementChange('element_deleted', elementId, { elementId })
    } catch {
      rebuildDiagram().catch(() => {})
    }
  }, [projectId, diagramId, broadcastElementChange, rebuildDiagram])

  useEffect(() => {
    deleteElementRef.current = deleteElement
  }, [deleteElement])

  // Delete all elements + relationships of the diagram
  const deleteAllElements = useCallback(async (): Promise<number> => {
    const count = state.elements.length
    await elementsApi.deleteAll(projectId, diagramId)
    await rebuildDiagram()
    return count
  }, [projectId, diagramId, state.elements.length, rebuildDiagram])

  // Patch element (e.g. note text) — optimistic + broadcast
  const patchElement = useCallback(async (elementId: string, data: Partial<UmlElementApi>) => {
    setState(prev => ({
      ...prev,
      elements: prev.elements.map((e) => e.id === elementId ? { ...e, ...data } : e),
      nodes: prev.nodes.map((n) => n.id === elementId ? { ...n, data: { ...n.data, ...data } } : n),
      selectedElement: prev.selectedElement?.id === elementId ? { ...prev.selectedElement, ...data } : prev.selectedElement,
    }))
    try {
      await elementsApi.update(projectId, diagramId, elementId, data)
      broadcastElementChange('element_updated', elementId)
    } catch {
      rebuildDiagram().catch(() => {})
    }
  }, [projectId, diagramId, broadcastElementChange, rebuildDiagram])

  useEffect(() => {
    patchElementRef.current = patchElement
  }, [patchElement])

  // Handle remote changes from WebSocket
  useEffect(() => {
    if (remoteChanges.length === 0) return

    remoteChanges.forEach((change) => {
      switch (change.type) {
        case 'element_moved': {
          const d = change.data as { elementId?: string; positionX?: number; positionY?: number }
          if (d?.elementId && typeof d.positionX === 'number' && typeof d.positionY === 'number') {
            setState(prev => ({
              ...prev,
              nodes: prev.nodes.map((n) => n.id === d.elementId ? { ...n, position: { x: d.positionX as number, y: d.positionY as number } } : n),
              elements: prev.elements.map((e) => e.id === d.elementId ? { ...e, positionX: d.positionX as number, positionY: d.positionY as number } : e),
            }))
          } else {
            rebuildDiagram().catch(() => {})
          }
          break
        }
        case 'element_created':
        case 'element_updated': {
          rebuildDiagram().catch(() => {})
          break
        }
        case 'element_deleted': {
          const data = change.data as Record<string, unknown>
          if (data?.elementId) {
            setState(prev => ({
              ...prev,
              nodes: prev.nodes.filter((n) => n.id !== data.elementId),
              elements: prev.elements.filter((e) => e.id !== data.elementId),
              edges: prev.edges.filter((e) => e.source !== data.elementId && e.target !== data.elementId),
              relationships: prev.relationships.filter((r) => r.sourceElementId !== data.elementId && r.targetElementId !== data.elementId),
              selectedElement: prev.selectedElement?.id === data.elementId ? null : prev.selectedElement,
            }))
          }
          break
        }
        case 'relationship_created':
        case 'relationship_updated':
        case 'relationship_deleted':
        case 'diagram_changed': {
          rebuildDiagram().catch(() => {})
          break
        }
      }
    })

    clearRemoteChanges()
  }, [remoteChanges, rebuildDiagram, clearRemoteChanges])

  // Debounced sync to server + broadcast via WebSocket
  const syncToServer = useCallback(() => {
    if (syncTimer.current) clearTimeout(syncTimer.current)
    syncTimer.current = setTimeout(() => {
      pendingChanges.current.forEach((data, elementId) => {
        elementsApi.update(projectId, diagramId, elementId, data).catch(() => {})
        broadcastElementChange('element_moved', elementId, data)
      })
      pendingChanges.current.clear()
    }, 500)
  }, [projectId, diagramId, broadcastElementChange])

  // Local-only node changes (drag) — debounced sync + broadcast
  const onNodesChange: OnNodesChange = useCallback((changes) => {
    setState(prev => ({ ...prev, nodes: applyNodeChanges(changes, prev.nodes) }))

    changes.forEach((change) => {
      if (change.type === 'position' && change.position && change.id) {
        const id = change.id as string
        pendingChanges.current.set(id, {
          ...(pendingChanges.current.get(id) || {}),
          positionX: change.position.x,
          positionY: change.position.y,
        })
        syncToServer()
        const now = Date.now()
        if (now - (lastMoveBroadcast.current.get(id) || 0) >= 80) {
          lastMoveBroadcast.current.set(id, now)
          broadcastElementChange('element_moved', id, { positionX: change.position.x, positionY: change.position.y })
        }
      }
      if (change.type === 'remove' && change.id && !String(change.id).startsWith('temp-')) {
        handleDeleteNode(String(change.id))
      }
    })
  }, [syncToServer, broadcastElementChange, handleDeleteNode])

  const onEdgesChange: OnEdgesChange = useCallback((changes) => {
    setState(prev => ({ ...prev, edges: applyEdgeChanges(changes, prev.edges) }))

    changes.forEach((change) => {
      if (change.type === 'remove' && change.id && !String(change.id).startsWith('edge-')) {
        handleDeleteEdge(String(change.id))
      }
    })
  }, [handleDeleteEdge])

  // Create element — optimistic + broadcast
  const createElement = useCallback(async (type: string, px?: number, py?: number) => {
    const count = state.elements.filter((e) => e.elementType === type).length
    const name = type === 'class' ? `Class${count + 1}`
      : type === 'interface' ? `Interface${count + 1}`
      : type === 'note' ? `Nota ${count + 1}`
      : `Enum${count + 1}`
    const x = px ?? (200 + count * 50)
    const y = py ?? (200 + count * 50)

    const tempId = `temp-${Date.now()}`
    const nodeType = nodeTypeFor(type)
    const newNode: Node = {
      id: tempId, type: nodeType, position: { x, y },
      data: { name, visibility: 'public', stereotype: undefined, attributes: [], methods: [], literals: [], onDeleteNode: handleDeleteNode, onRenameNode: handleNoteText },
    }
    setState(prev => ({
      ...prev, nodes: [...prev.nodes, newNode], attributes: { ...prev.attributes, [tempId]: [] }, methods: { ...prev.methods, [tempId]: [] },
    }))

    try {
      const el = await elementsApi.create(projectId, diagramId, { name, elementType: type, positionX: x, positionY: y })
      setState(prev => ({
        ...prev,
        nodes: prev.nodes.map((n) => n.id === tempId ? { ...n, id: el.id, data: { ...n.data } } : n),
        elements: [...prev.elements, el],
      }))
      broadcastElementChange('element_created', el.id)
    } catch {
      setState(prev => ({ ...prev, nodes: prev.nodes.filter((n) => n.id !== tempId) }))
    }
  }, [projectId, diagramId, state.elements.length, broadcastElementChange])

  // Create the third class that represents an association class (like Enterprise Architect)
  const createAssociationClassElement = useCallback(async (sourceId: string, targetId: string): Promise<string | null> => {
    const sourceNode = state.nodes.find((n) => n.id === sourceId)
    const targetNode = state.nodes.find((n) => n.id === targetId)
    const sx = sourceNode?.position.x ?? 0
    const sy = sourceNode?.position.y ?? 0
    const tx = targetNode?.position.x ?? 0
    const ty = targetNode?.position.y ?? 0
    const x = Math.round((sx + tx) / 2)
    const y = Math.round((sy + ty) / 2) + 90

    const sourceName = state.elements.find((e) => e.id === sourceId)?.name || 'A'
    const targetName = state.elements.find((e) => e.id === targetId)?.name || 'B'
    const name = `${sourceName}_${targetName}`

    try {
      const el = await elementsApi.create(projectId, diagramId, { name, elementType: 'class', positionX: x, positionY: y })
      setState(prev => ({
        ...prev,
        elements: [...prev.elements, el],
        nodes: [...prev.nodes, {
          id: el.id,
          type: nodeTypeFor(el.elementType),
          position: { x: el.positionX, y: el.positionY },
          data: { name: el.name, visibility: el.visibility, stereotype: el.stereotype, attributes: [], methods: [], literals: [], onDeleteNode: handleDeleteNode, onRenameNode: handleNoteText },
        }],
      }))
      broadcastElementChange('element_created', el.id)
      return el.id
    } catch {
      return null
    }
  }, [projectId, diagramId, state.nodes, state.elements, broadcastElementChange, handleDeleteNode, handleNoteText])

  // Create relationship — optimistic + broadcast
  const createRelationship = useCallback(async (sourceId: string, targetId: string, relationshipType: string, sourceHandle?: string, targetHandle?: string) => {
    if (!sourceId || !targetId) return

    const isSelf = sourceId === targetId
    const sides = isSelf
      ? { source: 'right' as AnchorSide, target: 'right' as AnchorSide }
      : bestSides(
          state.nodes.find((n) => n.id === sourceId),
          state.nodes.find((n) => n.id === targetId),
        )
    const sourceAnchor: EdgeAnchor = isSelf
      ? { side: 'right', percent: 10 }
      : parseEdgeAnchor(sourceHandle, sides.source)
    const targetAnchor: EdgeAnchor = isSelf
      ? { side: 'right', percent: 90 }
      : parseEdgeAnchor(targetHandle, sides.target)
    const srcHandle = edgeHandleFor(sourceAnchor, 'source')
    const tgtHandle = edgeHandleFor(targetAnchor, 'target')
    const srcAnchorValue = anchorStorageValue(sourceAnchor, 'source')
    const tgtAnchorValue = anchorStorageValue(targetAnchor, 'target')

    let associationClassElementId: string | null = null
    if (relationshipType === 'association_class') {
      associationClassElementId = await createAssociationClassElement(sourceId, targetId)
    }

    const tempId = `edge-${Date.now()}`
    const newEdge: Edge = {
      id: tempId, source: sourceId, target: targetId,
      sourceHandle: srcHandle, targetHandle: tgtHandle,
      type: edgeTypeFor(relationshipType),
      data: {
        relationshipType, name: '', associationClassElementId, sourceMultiplicity: '', targetMultiplicity: '',
        sourceAnchor, targetAnchor, waypoints: [],
        onAnchorChange: updateRelationshipAnchor, onWaypointsChange: updateRelationshipWaypoints,
      },
    }
    setState(prev => ({ ...prev, edges: [...prev.edges, newEdge] }))

    try {
      const rel = await relationshipsApi.create(projectId, diagramId, {
        sourceElementId: sourceId, targetElementId: targetId, relationshipType,
        associationClassElementId: associationClassElementId || undefined,
        sourceHandle: srcAnchorValue, targetHandle: tgtAnchorValue,
      })
      const savedSourceAnchor = parseEdgeAnchor(rel.sourceHandle, sourceAnchor.side)
      const savedTargetAnchor = parseEdgeAnchor(rel.targetHandle, targetAnchor.side)
      setState(prev => ({
        ...prev,
        edges: prev.edges.map((e) => e.id === tempId
          ? {
              ...e,
              id: rel.id,
              sourceHandle: edgeHandleFor(savedSourceAnchor, 'source'),
              targetHandle: edgeHandleFor(savedTargetAnchor, 'target'),
              type: edgeTypeFor(rel.relationshipType),
              data: {
                relationshipType: rel.relationshipType, name: rel.name || '',
                associationClassElementId: rel.associationClassElementId || null,
                sourceMultiplicity: '', targetMultiplicity: '',
                sourceAnchor: savedSourceAnchor, targetAnchor: savedTargetAnchor,
                waypoints: parseWaypoints(rel.waypoints),
                onAnchorChange: updateRelationshipAnchor, onWaypointsChange: updateRelationshipWaypoints,
                onDelete: () => handleDeleteEdge(rel.id),
              },
            }
          : e),
        relationships: [...prev.relationships, rel],
      }))
      broadcastElementChange('relationship_created', rel.id)
      setActiveRelationship(null)
    } catch {
      setState(prev => ({ ...prev, edges: prev.edges.filter((e) => e.id !== tempId) }))
    }
  }, [projectId, diagramId, broadcastElementChange, handleDeleteEdge, createAssociationClassElement, state.nodes, updateRelationshipAnchor, updateRelationshipWaypoints])

  // Connect via drag between handles (only while a relationship tool is selected)
  const onConnect = useCallback((params: { source: string; target: string; sourceHandle?: string | null; targetHandle?: string | null }) => {
    if (!activeRelationship) return
    createRelationship(params.source, params.target, activeRelationship, params.sourceHandle ?? undefined, params.targetHandle ?? undefined)
  }, [createRelationship, activeRelationship])

  // Reconnect (move) a relationship endpoint
  const reconnectRelationship = useCallback(async (relationshipId: string, connection: { source?: string | null; target?: string | null; sourceHandle?: string | null; targetHandle?: string | null }) => {
    const newSource = connection.source || undefined
    const newTarget = connection.target || undefined
    const sourceAnchor = connection.sourceHandle ? parseEdgeAnchor(connection.sourceHandle, 'right') : undefined
    const targetAnchor = connection.targetHandle ? parseEdgeAnchor(connection.targetHandle, 'left') : undefined
    const newSourceHandle = sourceAnchor ? anchorStorageValue(sourceAnchor, 'source') : undefined
    const newTargetHandle = targetAnchor ? anchorStorageValue(targetAnchor, 'target') : undefined

    setState(prev => ({
      ...prev,
      edges: prev.edges.map((e) => e.id === relationshipId ? {
        ...e,
        source: newSource ?? e.source,
        target: newTarget ?? e.target,
        sourceHandle: sourceAnchor ? edgeHandleFor(sourceAnchor, 'source') : e.sourceHandle,
        targetHandle: targetAnchor ? edgeHandleFor(targetAnchor, 'target') : e.targetHandle,
        data: {
          ...(e.data || {}),
          ...(sourceAnchor ? { sourceAnchor } : {}),
          ...(targetAnchor ? { targetAnchor } : {}),
        },
      } : e),
      relationships: prev.relationships.map((r) => r.id === relationshipId ? {
        ...r,
        sourceElementId: newSource ?? r.sourceElementId,
        targetElementId: newTarget ?? r.targetElementId,
        sourceHandle: newSourceHandle ?? r.sourceHandle,
        targetHandle: newTargetHandle ?? r.targetHandle,
      } : r),
    }))

    try {
      await relationshipsApi.update(projectId, diagramId, relationshipId, {
        sourceElementId: newSource,
        targetElementId: newTarget,
        sourceHandle: newSourceHandle,
        targetHandle: newTargetHandle,
      })
      broadcastElementChange('relationship_updated', relationshipId)
    } catch {
      rebuildDiagram().catch(() => {})
    }
  }, [projectId, diagramId, broadcastElementChange, rebuildDiagram])

  // Select element
  const selectElement = useCallback(async (elementId: string) => {
    const el = state.elements.find((e) => e.id === elementId)
    if (!el || !projectId || !diagramId) return

    const [attrs, meths] = await Promise.all([
      elementsApi.getAttributes(projectId, diagramId, elementId),
      elementsApi.getMethods(projectId, diagramId, elementId),
    ])
    setState(prev => ({ ...prev, selectedElement: el, selectedAttributes: attrs, selectedMethods: meths }))
  }, [state.elements, projectId, diagramId])

  // Node click: when a relationship tool is active, pick source then target
  const selectRelationship = useCallback(async (relationshipId: string) => {
    const rel = state.relationships.find((r) => r.id === relationshipId) || null
    setState(prev => ({ ...prev, selectedElement: null, selectedAttributes: [], selectedMethods: [] }))
    setSelectedRelationship(rel)
    if (!rel) { setRelationshipMultiplicities([]); return }
    try {
      const mults = await relationshipsApi.getMultiplicities(projectId, diagramId, relationshipId)
      setRelationshipMultiplicities(mults)
    } catch {
      setRelationshipMultiplicities([])
    }
  }, [state.relationships, projectId, diagramId])

  const handleNodeClick = useCallback((elementId: string) => {
    if (activeRelationship) return
    setSelectedRelationship(null)
    setRelationshipMultiplicities([])
    selectElement(elementId)
  }, [activeRelationship, selectElement])

  const onEdgeClick = useCallback((_event: unknown, edge: Edge) => {
    setActiveRelationship(null)
    selectRelationship(edge.id)
  }, [selectRelationship, setActiveRelationship])

  const clearRelationshipSelection = useCallback(() => {
    setSelectedRelationship(null)
    setRelationshipMultiplicities([])
  }, [])

  const updateRelationshipName = useCallback(async (name: string) => {
    const rel = selectedRelationship
    if (!rel) return
    setState(prev => ({
      ...prev,
      relationships: prev.relationships.map((r) => r.id === rel.id ? { ...r, name } : r),
      edges: prev.edges.map((e) => e.id === rel.id ? { ...e, data: { ...(e.data as Record<string, unknown>), name } } : e),
    }))
    setSelectedRelationship({ ...rel, name })
    try {
      await relationshipsApi.update(projectId, diagramId, rel.id, { name })
      broadcastElementChange('relationship_updated', rel.id)
    } catch {
      rebuildDiagram().catch(() => {})
    }
  }, [selectedRelationship, projectId, diagramId, broadcastElementChange, rebuildDiagram])

  const updateMultiplicity = useCallback(async (multiplicityId: string, min: number | null, max: number | null) => {
    const rel = selectedRelationship
    if (!rel) return
    const updated = relationshipMultiplicities.map((m) => m.id === multiplicityId ? { ...m, min, max } : m)
    setRelationshipMultiplicities(updated)
    const source = updated.find((m) => m.end === 'source')
    const target = updated.find((m) => m.end === 'target')
    setState(prev => ({
      ...prev,
      edges: prev.edges.map((e) => e.id === rel.id
        ? { ...e, data: { ...(e.data as Record<string, unknown>), sourceMultiplicity: formatMultiplicity(source), targetMultiplicity: formatMultiplicity(target) } }
        : e),
    }))
    try {
      await relationshipsApi.updateMultiplicity(projectId, diagramId, multiplicityId, { min, max })
      broadcastElementChange('relationship_updated', rel.id)
    } catch {
      rebuildDiagram().catch(() => {})
    }
  }, [selectedRelationship, relationshipMultiplicities, projectId, diagramId, broadcastElementChange, rebuildDiagram])

  // Update element — optimistic + broadcast
  const updateElement = useCallback(async (data: Partial<UmlElementApi>) => {
    if (!state.selectedElement) return
    const el = state.selectedElement

    setState(prev => ({
      ...prev,
      selectedElement: { ...el, ...data },
      nodes: prev.nodes.map((n) => n.id === el.id ? { ...n, data: { ...n.data, ...data } } : n),
    }))

    pendingChanges.current.set(el.id, { ...(pendingChanges.current.get(el.id) || {}), ...data })
    syncToServer()
    broadcastElementChange('element_updated', el.id, data as Record<string, unknown>)
  }, [state.selectedElement, syncToServer, broadcastElementChange])

  // Attribute operations — optimistic + broadcast
  const addAttribute = useCallback(async (data: { name: string; scope: string; type: string; alias: string; stereotype: string; initialValue: string }) => {
    if (!state.selectedElement || !projectId || !diagramId) return
    const name = data.name.trim()
    if (!name) return
    const tempId = `attr-${Date.now()}`
    const newAttr: UmlAttributeApi = { id: tempId, elementId: state.selectedElement.id, name, dataType: data.type, visibility: data.scope, alias: data.alias, stereotype: data.stereotype, defaultValue: data.initialValue, isStatic: false, isFinal: false, orderIndex: state.selectedAttributes.length }

    setState(prev => ({
      ...prev, selectedAttributes: [...prev.selectedAttributes, newAttr],
      attributes: { ...prev.attributes, [state.selectedElement!.id]: [...(prev.attributes[state.selectedElement!.id] || []), newAttr] },
      nodes: prev.nodes.map((n) => n.id === state.selectedElement!.id ? { ...n, data: { ...n.data, attributes: [...(n.data as Record<string, unknown>).attributes as UmlAttributeApi[], newAttr] } } : n),
    }))

    try {
      const saved = await elementsApi.addAttribute(projectId, diagramId, state.selectedElement.id, { name, dataType: data.type, visibility: data.scope, alias: data.alias || undefined, stereotype: data.stereotype || undefined, defaultValue: data.initialValue || undefined, orderIndex: state.selectedAttributes.length })
      setState(prev => ({
        ...prev,
        selectedAttributes: prev.selectedAttributes.map((a) => a.id === tempId ? saved : a),
        attributes: { ...prev.attributes, [state.selectedElement!.id]: (prev.attributes[state.selectedElement!.id] || []).map((a) => a.id === tempId ? saved : a) },
      }))
      broadcastElementChange('element_updated', state.selectedElement.id)
    } catch {
      setState(prev => ({
        ...prev, selectedAttributes: prev.selectedAttributes.filter((a) => a.id !== tempId),
      }))
    }
  }, [state.selectedElement, state.selectedAttributes, projectId, diagramId, broadcastElementChange])

  const deleteAttribute = useCallback(async (attrId: string) => {
    if (!state.selectedElement || !projectId || !diagramId) return
    await elementsApi.deleteAttribute(projectId, diagramId, attrId)
    const elementId = state.selectedElement.id
    setState(prev => {
      const nextAttrs = (prev.attributes[elementId] || []).filter((a) => a.id !== attrId)
      return {
        ...prev,
        selectedAttributes: prev.selectedAttributes.filter((a) => a.id !== attrId),
        attributes: { ...prev.attributes, [elementId]: nextAttrs },
        nodes: prev.nodes.map((n) => n.id === elementId ? { ...n, data: { ...n.data, attributes: nextAttrs } } : n),
      }
    })
    broadcastElementChange('element_updated', elementId)
  }, [state.selectedElement, projectId, diagramId, broadcastElementChange])

  // Method operations — optimistic + broadcast
  const addMethod = useCallback(async (data: { name: string; scope: string; type: string; alias: string; stereotype: string; initialValue: string }) => {
    if (!state.selectedElement || !projectId || !diagramId) return
    const name = data.name.trim()
    if (!name) return
    const elementId = state.selectedElement.id
    const tempId = `meth-${Date.now()}`
    const newMeth: UmlMethodApi = { id: tempId, elementId, name, returnType: data.type, visibility: data.scope, alias: data.alias, stereotype: data.stereotype, defaultValue: data.initialValue, isStatic: false, isAbstract: false, isFinal: false, isConstructor: false, orderIndex: state.selectedMethods.length, parameters: [] }

    setState(prev => {
      const nextMethods = [...(prev.methods[elementId] || []), newMeth]
      return {
        ...prev,
        selectedMethods: [...prev.selectedMethods, newMeth],
        methods: { ...prev.methods, [elementId]: nextMethods },
        nodes: prev.nodes.map((n) => n.id === elementId ? { ...n, data: { ...n.data, methods: nextMethods } } : n),
      }
    })

    try {
      const saved = await elementsApi.addMethod(projectId, diagramId, elementId, { name, returnType: data.type, visibility: data.scope, alias: data.alias || undefined, stereotype: data.stereotype || undefined, defaultValue: data.initialValue || undefined, orderIndex: state.selectedMethods.length })
      setState(prev => {
        const nextMethods = (prev.methods[elementId] || []).map((m) => m.id === tempId ? saved : m)
        return {
          ...prev,
          selectedMethods: prev.selectedMethods.map((m) => m.id === tempId ? saved : m),
          methods: { ...prev.methods, [elementId]: nextMethods },
          nodes: prev.nodes.map((n) => n.id === elementId ? { ...n, data: { ...n.data, methods: nextMethods } } : n),
        }
      })
      broadcastElementChange('element_updated', elementId)
    } catch {
      setState(prev => {
        const nextMethods = (prev.methods[elementId] || []).filter((m) => m.id !== tempId)
        return {
          ...prev,
          selectedMethods: prev.selectedMethods.filter((m) => m.id !== tempId),
          methods: { ...prev.methods, [elementId]: nextMethods },
          nodes: prev.nodes.map((n) => n.id === elementId ? { ...n, data: { ...n.data, methods: nextMethods } } : n),
        }
      })
    }
  }, [state.selectedElement, state.selectedMethods, projectId, diagramId, broadcastElementChange])

  const deleteMethod = useCallback(async (methId: string) => {
    if (!state.selectedElement || !projectId || !diagramId) return
    await elementsApi.deleteMethod(projectId, diagramId, methId)
    const elementId = state.selectedElement.id
    setState(prev => {
      const nextMethods = (prev.methods[elementId] || []).filter((m) => m.id !== methId)
      return {
        ...prev,
        selectedMethods: prev.selectedMethods.filter((m) => m.id !== methId),
        methods: { ...prev.methods, [elementId]: nextMethods },
        nodes: prev.nodes.map((n) => n.id === elementId ? { ...n, data: { ...n.data, methods: nextMethods } } : n),
      }
    })
    broadcastElementChange('element_updated', elementId)
  }, [state.selectedElement, projectId, diagramId, broadcastElementChange])

  // Parameter operations — optimistic + broadcast
  const addParameter = useCallback(async (methodId: string, data: { name: string; type: string; direction: string; notes: string }) => {
    if (!state.selectedElement || !projectId || !diagramId) return
    const name = data.name.trim()
    if (!name) return
    const elementId = state.selectedElement.id
    const tempId = `param-${Date.now()}`
    const newParam: UmlParameterApi = { id: tempId, methodId, name, dataType: data.type, direction: data.direction, notes: data.notes, orderIndex: 0 }

    const withParam = (list: UmlMethodApi[]) => list.map((m) => m.id === methodId ? { ...m, parameters: [...(m.parameters || []), newParam] } : m)
    setState(prev => {
      const nextMethods = withParam(prev.methods[elementId] || [])
      return {
        ...prev,
        selectedMethods: withParam(prev.selectedMethods),
        methods: { ...prev.methods, [elementId]: nextMethods },
        nodes: prev.nodes.map((n) => n.id === elementId ? { ...n, data: { ...n.data, methods: nextMethods } } : n),
      }
    })

    try {
      const saved = await elementsApi.addParameter(projectId, diagramId, methodId, { name, dataType: data.type, direction: data.direction, notes: data.notes || undefined })
      const replace = (list: UmlMethodApi[]) => list.map((m) => m.id === methodId ? { ...m, parameters: (m.parameters || []).map((p) => p.id === tempId ? saved : p) } : m)
      setState(prev => {
        const nextMethods = replace(prev.methods[elementId] || [])
        return {
          ...prev,
          selectedMethods: replace(prev.selectedMethods),
          methods: { ...prev.methods, [elementId]: nextMethods },
          nodes: prev.nodes.map((n) => n.id === elementId ? { ...n, data: { ...n.data, methods: nextMethods } } : n),
        }
      })
      broadcastElementChange('element_updated', elementId)
    } catch {
      const drop = (list: UmlMethodApi[]) => list.map((m) => m.id === methodId ? { ...m, parameters: (m.parameters || []).filter((p) => p.id !== tempId) } : m)
      setState(prev => {
        const nextMethods = drop(prev.methods[elementId] || [])
        return {
          ...prev,
          selectedMethods: drop(prev.selectedMethods),
          methods: { ...prev.methods, [elementId]: nextMethods },
          nodes: prev.nodes.map((n) => n.id === elementId ? { ...n, data: { ...n.data, methods: nextMethods } } : n),
        }
      })
    }
  }, [state.selectedElement, projectId, diagramId, broadcastElementChange])

  const deleteParameter = useCallback(async (methodId: string, paramId: string) => {
    if (!state.selectedElement || !projectId || !diagramId) return
    const elementId = state.selectedElement.id
    await elementsApi.deleteParameter(projectId, diagramId, paramId)
    const drop = (list: UmlMethodApi[]) => list.map((m) => m.id === methodId ? { ...m, parameters: (m.parameters || []).filter((p) => p.id !== paramId) } : m)
    setState(prev => {
      const nextMethods = drop(prev.methods[elementId] || [])
      return {
        ...prev,
        selectedMethods: drop(prev.selectedMethods),
        methods: { ...prev.methods, [elementId]: nextMethods },
        nodes: prev.nodes.map((n) => n.id === elementId ? { ...n, data: { ...n.data, methods: nextMethods } } : n),
      }
    })
    broadcastElementChange('element_updated', elementId)
  }, [state.selectedElement, projectId, diagramId, broadcastElementChange])

  return {
    ...state,
    connected,
    presenceUsers,
    rebuildDiagram,
    activeRelationship,
    setActiveRelationship,
    handleNodeClick,
    createRelationship,
    reconnectRelationship,
    selectedRelationship,
    relationshipMultiplicities,
    onEdgeClick,
    clearRelationshipSelection,
    deleteRelationship,
    updateRelationshipName,
    updateMultiplicity,
    remoteCursors,
    handleCursorMove,
    onNodesChange,
    onEdgesChange,
    onConnect,
    selectElement,
    createElement,
    deleteElement,
    deleteAllElements,
    updateElement,
    addAttribute,
    deleteAttribute,
    addMethod,
    deleteMethod,
    addParameter,
    deleteParameter,
  }
}
