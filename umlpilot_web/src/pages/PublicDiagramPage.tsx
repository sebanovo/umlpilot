import { useEffect, useMemo, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { ReactFlowProvider } from '@xyflow/react'
import type { Edge, Node } from '@xyflow/react'
import { Eye, Loader2, LogIn, UserPlus } from 'lucide-react'
import { useAuth } from '../features/auth/useAuth'
import { useToast } from '../shared/ui/toast/useToast'
import {
  publicDiagramApi,
  type MultiplicityApi,
  type PublicDiagramView,
  type UmlRelationshipApi,
} from '../shared/api/client'
import { DiagramCanvas } from '../features/diagram-editor/components/DiagramCanvas'

const EDGE_TYPES = new Set([
  'association', 'association_class', 'generalization', 'realization',
  'aggregation', 'composition',
])

type HandleSide = 'top' | 'right' | 'bottom' | 'left'
const SIDES: HandleSide[] = ['top', 'right', 'bottom', 'left']

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

function normalizeHandle(handle: string | null | undefined, type: 'source' | 'target', fallback: HandleSide): string {
  const prefix = type === 'source' ? 's-' : 't-'
  if (handle && handle.length > 2) {
    const side = handle.slice(handle.indexOf('-') + 1) as HandleSide
    if (SIDES.includes(side)) return `${prefix}${side}`
  }
  if (handle && SIDES.includes(handle as HandleSide)) return `${prefix}${handle}`
  return `${prefix}${fallback}`
}

function formatMultiplicity(m?: MultiplicityApi): string {
  if (!m || m.min == null || m.max == null) return ''
  const maxLabel = m.max < 0 ? '*' : String(m.max)
  if (m.min === m.max && m.max >= 0) return String(m.min)
  return `${m.min}..${maxLabel}`
}

function buildGraph(view: PublicDiagramView): { nodes: Node[]; edges: Edge[] } {
  const nodes: Node[] = view.elements.map((element) => ({
    id: element.id,
    type: nodeTypeFor(element.elementType),
    position: { x: element.positionX, y: element.positionY },
    data: {
      name: element.name,
      visibility: element.visibility,
      stereotype: element.stereotype,
      attributes: view.attributes[element.id] || [],
      methods: view.methods[element.id] || [],
      literals: view.literals[element.id] || [],
    },
    draggable: false,
  }))

  const edges: Edge[] = view.relationships.map((relationship: UmlRelationshipApi) => {
    const list = view.multiplicities[relationship.id] || []
    const source = list.find((m) => m.end === 'source')
    const target = list.find((m) => m.end === 'target')
    const isSelfLoop = relationship.sourceElementId === relationship.targetElementId
    return {
      id: relationship.id,
      source: relationship.sourceElementId,
      target: relationship.targetElementId,
      sourceHandle: isSelfLoop ? 's-right' : normalizeHandle(relationship.sourceHandle, 'source', 'bottom'),
      targetHandle: isSelfLoop ? 't-right' : normalizeHandle(relationship.targetHandle, 'target', 'top'),
      type: edgeTypeFor(relationship.relationshipType),
      data: {
        relationshipType: relationship.relationshipType,
        name: relationship.name || '',
        associationClassElementId: relationship.associationClassElementId || null,
        sourceMultiplicity: formatMultiplicity(source),
        targetMultiplicity: formatMultiplicity(target),
      },
    }
  })

  return { nodes, edges }
}

export function PublicDiagramPage() {
  const { token } = useParams<{ token: string }>()
  const { isAuthenticated } = useAuth()
  const { toast } = useToast()
  const navigate = useNavigate()
  const location = useLocation()
  const [view, setView] = useState<PublicDiagramView | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [joining, setJoining] = useState(false)

  useEffect(() => {
    if (!token) {
      setError('Enlace no válido')
      setLoading(false)
      return
    }
    let active = true
    publicDiagramApi.get(token)
      .then((data) => { if (active) setView(data) })
      .catch((err) => { if (active) setError(err instanceof Error ? err.message : 'Enlace no válido') })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [token])

  const graph = useMemo(() => (view ? buildGraph(view) : { nodes: [], edges: [] }), [view])

  const handleJoin = async () => {
    if (!token || joining) return
    setJoining(true)
    try {
      const result = await publicDiagramApi.join(token)
      navigate(`/projects/${result.projectId}/diagrams/${result.diagramId}`)
    } catch (err) {
      toast({
        title: 'No se pudo unir al proyecto',
        description: err instanceof Error ? err.message : 'Error inesperado',
        variant: 'error',
      })
    } finally {
      setJoining(false)
    }
  }

  if (loading) {
    return (
      <div className="flex h-screen items-center justify-center bg-gray-950 text-gray-400">
        <Loader2 className="h-5 w-5 animate-spin" />
      </div>
    )
  }

  if (error || !view) {
    return (
      <div className="flex h-screen flex-col items-center justify-center gap-3 bg-gray-950 px-6 text-center">
        <p className="text-lg font-semibold text-gray-100">Enlace no disponible</p>
        <p className="max-w-sm text-sm text-gray-400">{error ?? 'Este enlace no es válido o fue desactivado.'}</p>
        <Link to="/" className="text-sm font-medium text-blue-400 hover:text-blue-300">Volver a UMLPilot</Link>
      </div>
    )
  }

  return (
    <ReactFlowProvider>
      <div className="flex h-screen flex-col bg-gray-950">
        <header className="flex shrink-0 items-center justify-between gap-3 border-b border-gray-800 bg-gray-900 px-4 py-3">
          <div className="min-w-0">
            <p className="truncate text-xs text-gray-500">{view.projectName}</p>
            <h1 className="truncate text-sm font-semibold text-gray-100">{view.diagramName}</h1>
          </div>
          <div className="flex shrink-0 items-center gap-2">
            <span className="inline-flex items-center gap-1.5 rounded-full bg-gray-800 px-2.5 py-1 text-xs text-gray-300">
              <Eye className="h-3.5 w-3.5" />
              Vista previa
            </span>
            {isAuthenticated ? (
              <button
                type="button"
                onClick={handleJoin}
                disabled={joining}
                title={`Al unirte tendrás acceso como ${view.linkRole === 'editor' ? 'editor' : 'lector'}`}
                className="flex items-center gap-1.5 rounded-lg bg-blue-600 px-3 py-1.5 text-xs font-medium text-white transition-colors hover:bg-blue-500 disabled:opacity-50 cursor-pointer"
              >
                {joining ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : <UserPlus className="h-3.5 w-3.5" />}
                Unirme como {view.linkRole === 'editor' ? 'editor' : 'lector'}
              </button>
            ) : (
              <Link
                to="/login"
                state={{ from: location.pathname }}
                className="flex items-center gap-1.5 rounded-lg bg-blue-600 px-3 py-1.5 text-xs font-medium text-white transition-colors hover:bg-blue-500"
              >
                <LogIn className="h-3.5 w-3.5" />
                Inicia sesión para unirte
              </Link>
            )}
          </div>
        </header>
        <div className="min-h-0 flex-1">
          <DiagramCanvas
            nodes={graph.nodes}
            edges={graph.edges}
            onNodesChange={() => {}}
            onEdgesChange={() => {}}
            onNodeClick={() => {}}
            onConnect={() => {}}
            readOnly
          />
        </div>
      </div>
    </ReactFlowProvider>
  )
}
