import { useState, useEffect, useCallback } from 'react'
import { useParams, Link } from 'react-router-dom'
import { ReactFlowProvider } from '@xyflow/react'
import { elementsApi, relationshipsApi, type UmlElementApi, type UmlAttributeApi, type UmlMethodApi, type UmlRelationshipApi } from '../shared/api/client'
import { useAuth } from '../features/auth/useAuth'
import { DiagramCanvas } from '../features/diagram-editor/components/DiagramCanvas'
import { Toolbar } from '../features/diagram-editor/components/Toolbar'
import { PropertiesPanel } from '../features/diagram-editor/components/PropertiesPanel'
import type { OnNodesChange, OnEdgesChange } from '@xyflow/react'
import type { UmlElement } from '../features/diagram-editor/types/uml'

export function DiagramEditorPage() {
  const { id: projectId, diagramId } = useParams<{ id: string; diagramId: string }>()
  const { user } = useAuth()
  const [elements, setElements] = useState<UmlElementApi[]>([])
  const [attributes, setAttributes] = useState<Record<string, UmlAttributeApi[]>>({})
  const [methods, setMethods] = useState<Record<string, UmlMethodApi[]>>({})
  const [relationships, setRelationships] = useState<UmlRelationshipApi[]>([])
  const [selectedElement, setSelectedElement] = useState<UmlElementApi | null>(null)
  const [selectedAttributes, setSelectedAttributes] = useState<UmlAttributeApi[]>([])
  const [selectedMethods, setSelectedMethods] = useState<UmlMethodApi[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!projectId || !diagramId) return
    Promise.all([
      elementsApi.list(projectId, diagramId),
      relationshipsApi.list(projectId, diagramId),
    ]).then(([els, rels]) => {
      setElements(els)
      setRelationships(rels)
      const attrMap: Record<string, UmlAttributeApi[]> = {}
      const methMap: Record<string, UmlMethodApi[]> = {}
      els.forEach((el) => {
        elementsApi.getAttributes(projectId, diagramId, el.id).then((attrs) => { attrMap[el.id] = attrs; setAttributes({ ...attrMap }) })
        elementsApi.getMethods(projectId, diagramId, el.id).then((meths) => { methMap[el.id] = meths; setMethods({ ...methMap }) })
      })
    }).finally(() => setLoading(false))
  }, [projectId, diagramId])

  const onNodesChange: OnNodesChange = useCallback((changes) => {
    changes.forEach((change) => {
      if (change.type === 'position' && change.position) {
        const pos = change.position
        setElements((prev) => prev.map((el) => el.id === change.id ? { ...el, positionX: pos.x, positionY: pos.y } : el))
        if (projectId && diagramId && change.id) {
          elementsApi.update(projectId, diagramId, change.id as string, { positionX: pos.x, positionY: pos.y })
        }
      }
    })
  }, [projectId, diagramId])

  const onEdgesChange: OnEdgesChange = useCallback(() => {}, [])

  const onConnect = useCallback((params: { source: string; target: string }) => {
    if (!projectId || !diagramId) return
    relationshipsApi.create(projectId, diagramId, { sourceElementId: params.source, targetElementId: params.target, relationshipType: 'association' })
      .then((rel) => setRelationships((prev) => [...prev, rel]))
  }, [projectId, diagramId])

  const handleAddElement = async (type: string) => {
    if (!projectId || !diagramId) return
    const count = elements.filter((e) => e.elementType === type).length
    const name = type === 'class' ? `Class${count + 1}` : type === 'interface' ? `Interface${count + 1}` : `Enum${count + 1}`
    const el = await elementsApi.create(projectId, diagramId, { name, elementType: type, positionX: 200 + count * 50, positionY: 200 + count * 50 })
    setElements((prev) => [...prev, el])
    setAttributes((prev) => ({ ...prev, [el.id]: [] }))
    setMethods((prev) => ({ ...prev, [el.id]: [] }))
  }

  const handleNodeClick = async (elementId: string) => {
    if (!projectId || !diagramId) return
    const el = elements.find((e) => e.id === elementId)
    if (el) {
      setSelectedElement(el)
      const [attrs, meths] = await Promise.all([
        elementsApi.getAttributes(projectId, diagramId, elementId),
        elementsApi.getMethods(projectId, diagramId, elementId),
      ])
      setSelectedAttributes(attrs)
      setSelectedMethods(meths)
    }
  }

  const handleUpdateElement = async (data: Partial<UmlElement>) => {
    if (!projectId || !diagramId || !selectedElement) return
    const updated = await elementsApi.update(projectId, diagramId, selectedElement.id, data)
    setSelectedElement(updated)
    setElements((prev) => prev.map((e) => e.id === updated.id ? updated : e))
  }

  const handleAddAttribute = async (name: string, dataType: string) => {
    if (!projectId || !diagramId || !selectedElement) return
    const attr = await elementsApi.addAttribute(projectId, diagramId, selectedElement.id, { name, dataType, visibility: 'private', orderIndex: selectedAttributes.length })
    setSelectedAttributes((prev) => [...prev, attr])
    setAttributes((prev) => ({ ...prev, [selectedElement.id]: [...(prev[selectedElement.id] || []), attr] }))
  }

  const handleDeleteAttribute = async (attrId: string) => {
    if (!projectId || !diagramId || !selectedElement) return
    await elementsApi.deleteAttribute(projectId, diagramId, attrId)
    setSelectedAttributes((prev) => prev.filter((a) => a.id !== attrId))
    setAttributes((prev) => ({ ...prev, [selectedElement.id]: (prev[selectedElement.id] || []).filter((a) => a.id !== attrId) }))
  }

  const handleAddMethod = async (name: string, returnType: string) => {
    if (!projectId || !diagramId || !selectedElement) return
    const meth = await elementsApi.addMethod(projectId, diagramId, selectedElement.id, { name, returnType, visibility: 'public', orderIndex: selectedMethods.length })
    setSelectedMethods((prev) => [...prev, meth])
    setMethods((prev) => ({ ...prev, [selectedElement.id]: [...(prev[selectedElement.id] || []), meth] }))
  }

  const handleDeleteMethod = async (methId: string) => {
    if (!projectId || !diagramId || !selectedElement) return
    await elementsApi.deleteMethod(projectId, diagramId, methId)
    setSelectedMethods((prev) => prev.filter((m) => m.id !== methId))
    setMethods((prev) => ({ ...prev, [selectedElement.id]: (prev[selectedElement.id] || []).filter((m) => m.id !== methId) }))
  }

  if (loading) return <p style={{ padding: '2rem' }}>Cargando editor...</p>

  return (
    <ReactFlowProvider>
      <div style={{ height: '100vh', display: 'flex', flexDirection: 'column', fontFamily: 'sans-serif' }}>
        <header style={{ padding: '0.5rem 1rem', background: '#1a1a2e', color: 'white', display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: 13 }}>
          <div>
            <Link to={`/projects/${projectId}`} style={{ color: '#88ccff', textDecoration: 'none', marginRight: '1rem' }}>&larr; Volver</Link>
            <strong>Editor de Diagrama</strong>
          </div>
          <span style={{ color: user?.firstName ? '#4caf50' : '#999' }}>{user?.firstName} {user?.lastName}</span>
        </header>
        <Toolbar onAddElement={handleAddElement} onAddRelationship={() => {}} />
        <div style={{ flex: 1, display: 'flex', overflow: 'hidden' }}>
          <div style={{ flex: 1 }}>
            <DiagramCanvas
              elements={elements.map((e) => ({ ...e, diagramId: e.diagramId, elementType: e.elementType as 'class' | 'interface' | 'enumeration' | 'note' }))}
              attributes={attributes}
              methods={methods}
              literals={{}}
              relationships={relationships.map((r) => ({ ...r, relationshipType: r.relationshipType as 'association' | 'generalization' | 'dependency' | 'aggregation' | 'composition' | 'realization' }))}
              onNodesChange={onNodesChange}
              onEdgesChange={onEdgesChange}
              onNodeClick={handleNodeClick}
              onConnect={onConnect}
            />
          </div>
          <PropertiesPanel
            element={selectedElement ? { ...selectedElement, elementType: selectedElement.elementType as 'class' | 'interface' | 'enumeration' | 'note' } : null}
            attributes={selectedAttributes.map((a) => ({ ...a }))}
            methods={selectedMethods.map((m) => ({ ...m }))}
            onUpdate={handleUpdateElement}
            onAddAttribute={handleAddAttribute}
            onAddMethod={handleAddMethod}
            onDeleteAttribute={handleDeleteAttribute}
            onDeleteMethod={handleDeleteMethod}
          />
        </div>
      </div>
    </ReactFlowProvider>
  )
}
