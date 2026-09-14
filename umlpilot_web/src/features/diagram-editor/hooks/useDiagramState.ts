import { useState, useCallback, useRef, useEffect } from 'react'
import type { Node, Edge, OnNodesChange, OnEdgesChange } from '@xyflow/react'
import { applyNodeChanges, applyEdgeChanges } from '@xyflow/react'
import { elementsApi, relationshipsApi } from '../../../shared/api/client'
import type { UmlElementApi, UmlAttributeApi, UmlMethodApi, UmlRelationshipApi } from '../../../shared/api/client'
import { useWebSocket } from '../../../shared/lib/useWebSocket'

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
}

export function useDiagramState({ projectId, diagramId }: UseDiagramStateProps) {
  const [state, setState] = useState<DiagramState>({
    nodes: [], edges: [], elements: [], attributes: {}, methods: {},
    relationships: [], selectedElement: null, selectedAttributes: [], selectedMethods: [], loading: true,
  })

  const pendingChanges = useRef<Map<string, Record<string, unknown>>>(new Map())
  const syncTimer = useRef<ReturnType<typeof setTimeout> | null>(null)

  // Get current user ID from localStorage
  const userId = JSON.parse(localStorage.getItem('user') || '{}')?.id || ''

  // Connect WebSocket
  const { connected, presenceUsers, remoteChanges, broadcastElementChange, clearRemoteChanges } = useWebSocket(diagramId, userId)

  // Load initial data
  useEffect(() => {
    if (!projectId || !diagramId) return
    Promise.all([
      elementsApi.list(projectId, diagramId),
      relationshipsApi.list(projectId, diagramId),
    ]).then(([els, rels]) => {
      const attrMap: Record<string, UmlAttributeApi[]> = {}
      const methMap: Record<string, UmlMethodApi[]> = {}

      return Promise.all(els.map(async (el: UmlElementApi) => {
        const [attrs, meths] = await Promise.all([
          elementsApi.getAttributes(projectId, diagramId, el.id),
          elementsApi.getMethods(projectId, diagramId, el.id),
        ])
        attrMap[el.id] = attrs
        methMap[el.id] = meths
      })).then(() => {
        const nodes: Node[] = els.map((el: UmlElementApi) => ({
          id: el.id,
          type: el.elementType === 'enumeration' ? 'enumeration' : el.elementType === 'interface' ? 'interface' : 'class',
          position: { x: el.positionX, y: el.positionY },
          data: { name: el.name, visibility: el.visibility, stereotype: el.stereotype, attributes: attrMap[el.id] || [], methods: methMap[el.id] || [], literals: [] },
        }))

        const edges: Edge[] = rels.map((rel: UmlRelationshipApi) => ({
          id: rel.id,
          source: rel.sourceElementId,
          target: rel.targetElementId,
          type: rel.relationshipType === 'generalization' ? 'generalization' : rel.relationshipType === 'dependency' || rel.relationshipType === 'realization' ? 'dependency' : 'association',
          label: rel.name || '',
          animated: rel.relationshipType === 'dependency',
        }))

        setState(prev => ({
          ...prev, nodes, edges, elements: els, attributes: attrMap, methods: methMap, relationships: rels, loading: false,
        }))
      })
    }).catch(() => setState(prev => ({ ...prev, loading: false })))
  }, [projectId, diagramId])

  // Handle remote changes from WebSocket
  useEffect(() => {
    if (remoteChanges.length === 0) return

    remoteChanges.forEach((change) => {
      switch (change.type) {
        case 'element_created':
        case 'element_moved':
        case 'element_updated': {
          const data = change.data as Record<string, unknown>
          if (data?.elementId) {
            // Refetch element from server
            elementsApi.getAttributes(projectId, diagramId, data.elementId as string).catch(() => {})
            // For simplicity, refetch all elements to stay in sync
            elementsApi.list(projectId, diagramId).then((els) => {
              setState(prev => ({ ...prev, elements: els }))
            }).catch(() => {})
          }
          break
        }
        case 'element_deleted': {
          const data = change.data as Record<string, unknown>
          if (data?.elementId) {
            setState(prev => ({
              ...prev,
              nodes: prev.nodes.filter((n) => n.id !== data.elementId),
              elements: prev.elements.filter((e) => e.id !== data.elementId),
            }))
          }
          break
        }
        case 'relationship_created':
        case 'relationship_updated': {
          relationshipsApi.list(projectId, diagramId).then((rels) => {
            const edges: Edge[] = rels.map((rel: UmlRelationshipApi) => ({
              id: rel.id,
              source: rel.sourceElementId,
              target: rel.targetElementId,
              type: rel.relationshipType === 'generalization' ? 'generalization' : rel.relationshipType === 'dependency' || rel.relationshipType === 'realization' ? 'dependency' : 'association',
              label: rel.name || '',
              animated: rel.relationshipType === 'dependency',
            }))
            setState(prev => ({ ...prev, edges, relationships: rels }))
          }).catch(() => {})
          break
        }
        case 'relationship_deleted': {
          const data = change.data as Record<string, unknown>
          if (data?.relationshipId) {
            setState(prev => ({
              ...prev,
              edges: prev.edges.filter((e) => e.id !== data.relationshipId),
              relationships: prev.relationships.filter((r) => r.id !== data.relationshipId),
            }))
          }
          break
        }
      }
    })

    clearRemoteChanges()
  }, [remoteChanges, projectId, diagramId, clearRemoteChanges])

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
        pendingChanges.current.set(change.id as string, {
          ...(pendingChanges.current.get(change.id as string) || {}),
          positionX: change.position.x,
          positionY: change.position.y,
        })
        syncToServer()
      }
    })
  }, [syncToServer])

  const onEdgesChange: OnEdgesChange = useCallback((changes) => {
    setState(prev => ({ ...prev, edges: applyEdgeChanges(changes, prev.edges) }))
  }, [])

  // Create element — optimistic + broadcast
  const createElement = useCallback(async (type: string) => {
    const count = state.elements.filter((e) => e.elementType === type).length
    const name = type === 'class' ? `Class${count + 1}` : type === 'interface' ? `Interface${count + 1}` : `Enum${count + 1}`
    const x = 200 + count * 50, y = 200 + count * 50

    const tempId = `temp-${Date.now()}`
    const newNode: Node = {
      id: tempId, type, position: { x, y },
      data: { name, visibility: 'public', stereotype: undefined, attributes: [], methods: [], literals: [] },
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

  // Connect elements — optimistic + broadcast
  const onConnect = useCallback(async (params: { source: string; target: string }) => {
    if (!params.source || !params.target) return

    const tempId = `edge-${Date.now()}`
    const newEdge: Edge = { id: tempId, source: params.source, target: params.target, type: 'association' }
    setState(prev => ({ ...prev, edges: [...prev.edges, newEdge] }))

    try {
      const rel = await relationshipsApi.create(projectId, diagramId, {
        sourceElementId: params.source, targetElementId: params.target, relationshipType: 'association',
      })
      setState(prev => ({
        ...prev,
        edges: prev.edges.map((e) => e.id === tempId ? { ...e, id: rel.id } : e),
        relationships: [...prev.relationships, rel],
      }))
      broadcastElementChange('relationship_created', rel.id)
    } catch {
      setState(prev => ({ ...prev, edges: prev.edges.filter((e) => e.id !== tempId) }))
    }
  }, [projectId, diagramId, broadcastElementChange])

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
  const addAttribute = useCallback(async (name: string, dataType: string) => {
    if (!state.selectedElement || !projectId || !diagramId) return
    const tempId = `attr-${Date.now()}`
    const newAttr: UmlAttributeApi = { id: tempId, elementId: state.selectedElement.id, name, dataType, visibility: 'private', isStatic: false, isFinal: false, orderIndex: state.selectedAttributes.length }

    setState(prev => ({
      ...prev, selectedAttributes: [...prev.selectedAttributes, newAttr],
      attributes: { ...prev.attributes, [state.selectedElement!.id]: [...(prev.attributes[state.selectedElement!.id] || []), newAttr] },
      nodes: prev.nodes.map((n) => n.id === state.selectedElement!.id ? { ...n, data: { ...n.data, attributes: [...(n.data as Record<string, unknown>).attributes as UmlAttributeApi[], newAttr] } } : n),
    }))

    try {
      const saved = await elementsApi.addAttribute(projectId, diagramId, state.selectedElement.id, { name, dataType, visibility: 'private', orderIndex: state.selectedAttributes.length })
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
    setState(prev => ({
      ...prev, selectedAttributes: prev.selectedAttributes.filter((a) => a.id !== attrId),
      attributes: { ...prev.attributes, [state.selectedElement!.id]: (prev.attributes[state.selectedElement!.id] || []).map((a) => a).filter((a) => a.id !== attrId) },
    }))
    broadcastElementChange('element_updated', state.selectedElement.id)
  }, [state.selectedElement, projectId, diagramId, broadcastElementChange])

  // Method operations — optimistic + broadcast
  const addMethod = useCallback(async (name: string, returnType: string) => {
    if (!state.selectedElement || !projectId || !diagramId) return
    const tempId = `meth-${Date.now()}`
    const newMeth: UmlMethodApi = { id: tempId, elementId: state.selectedElement.id, name, returnType, visibility: 'public', isStatic: false, isAbstract: false, isFinal: false, isConstructor: false, orderIndex: state.selectedMethods.length, parameters: [] }

    setState(prev => ({
      ...prev, selectedMethods: [...prev.selectedMethods, newMeth],
      methods: { ...prev.methods, [state.selectedElement!.id]: [...(prev.methods[state.selectedElement!.id] || []), newMeth] },
    }))

    try {
      const saved = await elementsApi.addMethod(projectId, diagramId, state.selectedElement.id, { name, returnType, visibility: 'public', orderIndex: state.selectedMethods.length })
      setState(prev => ({
        ...prev,
        selectedMethods: prev.selectedMethods.map((m) => m.id === tempId ? saved : m),
        methods: { ...prev.methods, [state.selectedElement!.id]: (prev.methods[state.selectedElement!.id] || []).map((m) => m.id === tempId ? saved : m) },
      }))
      broadcastElementChange('element_updated', state.selectedElement.id)
    } catch {
      setState(prev => ({ ...prev, selectedMethods: prev.selectedMethods.filter((m) => m.id !== tempId) }))
    }
  }, [state.selectedElement, state.selectedMethods, projectId, diagramId, broadcastElementChange])

  const deleteMethod = useCallback(async (methId: string) => {
    if (!state.selectedElement || !projectId || !diagramId) return
    await elementsApi.deleteMethod(projectId, diagramId, methId)
    setState(prev => ({
      ...prev, selectedMethods: prev.selectedMethods.filter((m) => m.id !== methId),
      methods: { ...prev.methods, [state.selectedElement!.id]: (prev.methods[state.selectedElement!.id] || []).filter((m) => m.id !== methId) },
    }))
    broadcastElementChange('element_updated', state.selectedElement.id)
  }, [state.selectedElement, projectId, diagramId, broadcastElementChange])

  return {
    ...state,
    connected,
    presenceUsers,
    onNodesChange,
    onEdgesChange,
    onConnect,
    selectElement,
    createElement,
    updateElement,
    addAttribute,
    deleteAttribute,
    addMethod,
    deleteMethod,
  }
}
