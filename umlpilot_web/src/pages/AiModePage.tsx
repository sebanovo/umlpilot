import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { ArrowLeft, Bot, FileText, ShieldAlert, Sparkles, Wifi, WifiOff } from 'lucide-react'
import { useAuth } from '../features/auth/useAuth'
import { useChatSocket } from '../features/ai-chat/model/useChatSocket'
import { useGroupChat } from '../features/ai-chat/model/useGroupChat'
import type { MentionOption } from '../features/ai-chat/model/mentions'
import { AiConversation } from '../features/ai-chat/ui/AiConversation'
import { DiagramSelector } from '../features/ai-chat/ui/DiagramSelector'
import { ParticipantsPanel } from '../features/ai-chat/ui/ParticipantsPanel'
import {
  diagramsApi,
  projectsApi,
  type Collaborator,
  type Diagram,
  type Project,
} from '../shared/api/client'

export function AiModePage() {
  const { id: projectId } = useParams<{ id: string }>()
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const { user } = useAuth()

  const [project, setProject] = useState<Project | null>(null)
  const [collaborators, setCollaborators] = useState<Collaborator[]>([])
  const [diagrams, setDiagrams] = useState<Diagram[]>([])
  const [selectedDiagramId, setSelectedDiagramId] = useState<string | null>(null)
  const [presence, setPresence] = useState<string[]>([])
  const [typingUsers, setTypingUsers] = useState<Record<string, string>>({})
  const [connected, setConnected] = useState(false)
  const [highlightedMessageId, setHighlightedMessageId] = useState<string | null>(null)
  const typingTimers = useRef<Record<string, number>>({})

  const currentRole = collaborators.find((collaborator) => collaborator.userId === user?.id)?.role
    ?? 'viewer'
  const currentUser = useMemo(() => ({
    id: user?.id ?? '',
    name: `${user?.firstName ?? ''} ${user?.lastName ?? ''}`.trim() || user?.email || 'Usuario',
    role: currentRole,
  }), [user, currentRole])

  const mentionOptions = useMemo<MentionOption[]>(() => [
    { id: 'assistant', label: 'Asistente IA', role: 'IA', assistant: true },
    ...collaborators.map((collaborator) => {
      const name = `${collaborator.firstName ?? ''} ${collaborator.lastName ?? ''}`.trim()
        || collaborator.email
      return { id: collaborator.userId, label: name, role: collaborator.role }
    }),
  ], [collaborators])
  const mentionNames = useMemo(() => mentionOptions.map((option) => option.label), [mentionOptions])

  const chat = useGroupChat(projectId, currentUser)

  const { sendTyping } = useChatSocket(projectId, {
    onMessage: chat.mergeMessage,
    onAiStatus: chat.applyAiStatus,
    onTyping: (userId, name, typing) => {
      if (userId === currentUser.id) return
      if (typingTimers.current[userId]) window.clearTimeout(typingTimers.current[userId])
      if (!typing) {
        setTypingUsers((prev) => {
          const next = { ...prev }
          delete next[userId]
          return next
        })
        return
      }
      setTypingUsers((prev) => ({ ...prev, [userId]: name }))
      typingTimers.current[userId] = window.setTimeout(() => {
        setTypingUsers((prev) => {
          const next = { ...prev }
          delete next[userId]
          return next
        })
      }, 2500)
    },
    onPresence: setPresence,
    onConnectionChange: setConnected,
    onReconnect: chat.refresh,
  })

  useEffect(() => {
    if (!projectId) return
    Promise.all([
      projectsApi.get(projectId),
      projectsApi.getCollaborators(projectId),
      diagramsApi.list(projectId),
    ]).then(([projectResult, collaboratorResult, diagramResult]) => {
      setProject(projectResult)
      setCollaborators(collaboratorResult)
      setDiagrams(diagramResult)
    }).catch(() => {
      // La página muestra el estado vacío; el detalle del error lo da el chat.
    })
  }, [projectId])

  useEffect(() => {
    if (!diagrams.length) return
    setSelectedDiagramId((previous) => {
      if (previous && diagrams.some((diagram) => diagram.id === previous)) return previous
      const queryDiagram = searchParams.get('diagramId')
      if (queryDiagram && diagrams.some((diagram) => diagram.id === queryDiagram)) return queryDiagram
      const focus = chat.conversation?.focusDiagramId
      if (focus && diagrams.some((diagram) => diagram.id === focus)) return focus
      return diagrams[0].id
    })
  }, [diagrams, searchParams, chat.conversation?.focusDiagramId])

  const canMutate = chat.conversation?.canMutate
    ?? (currentRole === 'owner' || currentRole === 'editor')
  const typingNames = Object.values(typingUsers)
  const selectedDiagram = diagrams.find((diagram) => diagram.id === selectedDiagramId)

  const handleCitationClick = async (messageId: string) => {
    const found = await chat.loadUntilMessage(messageId)
    if (!found) return
    setHighlightedMessageId(messageId)
    window.setTimeout(() => {
      document.getElementById(`chat-message-${messageId}`)?.scrollIntoView({
        behavior: 'smooth',
        block: 'center',
      })
    }, 50)
    window.setTimeout(() => setHighlightedMessageId(null), 3500)
  }

  const openDiagram = (diagramId: string) => {
    navigate(`/projects/${projectId}/diagrams/${diagramId}`)
  }

  if (!projectId) return null

  return (
    <div className="flex h-screen flex-col bg-gray-950 font-sans text-gray-100">
      <header className="flex shrink-0 items-center justify-between border-b border-gray-800 bg-gray-900 px-4 py-3">
        <div className="flex min-w-0 items-center gap-3">
          <Link
            to={`/projects/${projectId}`}
            className="rounded-lg p-1.5 text-gray-400 transition-colors hover:bg-gray-800 hover:text-gray-100"
            title="Volver al proyecto"
          >
            <ArrowLeft className="h-4 w-4" />
          </Link>
          <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-blue-950 text-blue-300">
            <Sparkles className="h-4 w-4" />
          </div>
          <div className="min-w-0">
            <h1 className="truncate text-sm font-semibold">Modo IA · {project?.name ?? 'Proyecto'}</h1>
            <p className="truncate text-[11px] text-gray-500">
              Chat grupal persistente con el contexto de todo el equipo
            </p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <span className={`inline-flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-[11px] ${
            connected
              ? 'border-emerald-900 bg-emerald-950/60 text-emerald-300'
              : 'border-amber-900 bg-amber-950/60 text-amber-300'
          }`}>
            {connected ? <Wifi className="h-3 w-3" /> : <WifiOff className="h-3 w-3" />}
            {connected ? 'Tiempo real' : 'Reconectando…'}
          </span>
          <span className="hidden rounded-full border border-gray-700 bg-gray-800 px-2.5 py-1 text-[11px] text-gray-300 sm:inline">
            {presence.length} conectado{presence.length === 1 ? '' : 's'}
          </span>
        </div>
      </header>

      <main className="grid min-h-0 flex-1 grid-cols-1 gap-3 overflow-hidden p-3 lg:grid-cols-[250px_minmax(0,1fr)_270px]">
        <aside className="hidden min-h-0 flex-col gap-4 overflow-y-auto rounded-2xl border border-gray-800 bg-gray-900/60 p-4 lg:flex">
          <DiagramSelector
            diagrams={diagrams}
            value={selectedDiagramId}
            onChange={setSelectedDiagramId}
          />
          {selectedDiagram && (
            <Link
              to={`/projects/${projectId}/diagrams/${selectedDiagram.id}`}
              className="flex items-center justify-center gap-1.5 rounded-lg border border-gray-700 bg-gray-800 px-2.5 py-2 text-xs text-gray-300 transition-colors hover:border-blue-500 hover:text-blue-200"
            >
              <FileText className="h-3.5 w-3.5" /> Abrir diagrama
            </Link>
          )}
          {!canMutate && (
            <div className="rounded-lg border border-amber-900 bg-amber-950/40 p-2.5 text-[11px] text-amber-200">
              <p className="mb-1 flex items-center gap-1 font-medium">
                <ShieldAlert className="h-3.5 w-3.5" /> Modo lectura
              </p>
              Puedes leer y escribir mensajes; la IA no modificará el diagrama.
            </div>
          )}
          <ParticipantsPanel
            collaborators={collaborators}
            presence={presence}
            currentUserId={currentUser.id}
          />
        </aside>

        <AiConversation
          messages={chat.messages}
          currentUserId={currentUser.id}
          loading={chat.loading}
          hasMore={chat.hasMore}
          loadingMore={chat.loadingMore}
          sending={chat.sending}
          canMutate={canMutate}
          typingUsers={typingNames}
          highlightedMessageId={highlightedMessageId}
          mentionOptions={mentionOptions}
          mentionNames={mentionNames}
          error={chat.error}
          onSend={(text) => chat.sendMessage(text, selectedDiagramId)}
          onRetry={chat.retryMessage}
          onLoadOlder={chat.loadOlder}
          onCitationClick={handleCitationClick}
          onOpenDiagram={openDiagram}
          onTyping={sendTyping}
          onClearError={chat.clearError}
        />

        <aside className="hidden min-h-0 flex-col gap-4 overflow-y-auto rounded-2xl border border-gray-800 bg-gray-900/60 p-4 lg:flex">
          <div>
            <p className="mb-1 flex items-center gap-1 text-[11px] font-medium uppercase tracking-wide text-gray-500">
              <Bot className="h-3.5 w-3.5" /> Contexto compartido
            </p>
            <p className="text-xs leading-relaxed text-gray-400">
              La IA recibe los mensajes recientes, el resumen de la conversación y referencias
              a mensajes antiguos relevantes. Todo el equipo comparte el mismo hilo.
            </p>
          </div>
          <div>
            <p className="mb-1 text-[11px] font-medium uppercase tracking-wide text-gray-500">
              Diagrama seleccionado
            </p>
            <p className="text-xs text-gray-300">{selectedDiagram?.name ?? 'Ninguno'}</p>
            <p className="mt-1 text-[11px] text-gray-500">
              La IA solo puede operar sobre este diagrama.
            </p>
          </div>
          <div>
            <p className="mb-1 text-[11px] font-medium uppercase tracking-wide text-gray-500">
              Resumen del historial
            </p>
            <p className="whitespace-pre-wrap text-xs leading-relaxed text-gray-400">
              {chat.conversation?.summary || 'Aún no hay un resumen. Se generará cuando la conversación crezca.'}
            </p>
          </div>
          <div className="rounded-lg border border-emerald-900/60 bg-emerald-950/30 p-2.5 text-[11px] text-emerald-200/80">
            Escribe <strong>@asistente</strong> para que la IA responda. Usa <strong>@nombre</strong> para mencionar a un compañero.
          </div>
          <div className="mt-auto rounded-lg border border-gray-800 bg-gray-950/60 p-2.5 text-[11px] text-gray-500">
            Consejo: menciona a un compañero o cita una decisión anterior para que la IA la recupere.
          </div>
        </aside>
      </main>
    </div>
  )
}
