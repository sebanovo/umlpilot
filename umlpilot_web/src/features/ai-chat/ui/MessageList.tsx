import { useEffect, useRef, useState } from 'react'
import { ArrowDown, Loader2, MessageSquare } from 'lucide-react'
import type { ChatMessage } from '../api/groupChatApi'
import { MessageBubble } from './MessageBubble'

function dayKey(value: string): string {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return `${date.getFullYear()}-${date.getMonth()}-${date.getDate()}`
}

function dayLabel(value: string): string {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const today = new Date()
  const yesterday = new Date()
  yesterday.setDate(today.getDate() - 1)
  if (dayKey(value) === dayKey(today.toISOString())) return 'Hoy'
  if (dayKey(value) === dayKey(yesterday.toISOString())) return 'Ayer'
  return date.toLocaleDateString([], { day: '2-digit', month: 'short', year: 'numeric' })
}

interface MessageListProps {
  messages: ChatMessage[]
  currentUserId: string
  loading: boolean
  hasMore: boolean
  loadingMore: boolean
  highlightedMessageId: string | null
  mentionNames: string[]
  onLoadOlder: () => void
  onCitationClick: (messageId: string) => void
  onRetry: (messageId: string) => void
  onOpenDiagram: (diagramId: string) => void
}

export function MessageList({
  messages,
  currentUserId,
  loading,
  hasMore,
  loadingMore,
  highlightedMessageId,
  mentionNames,
  onLoadOlder,
  onCitationClick,
  onRetry,
  onOpenDiagram,
}: MessageListProps) {
  const containerRef = useRef<HTMLDivElement>(null)
  const bottomRef = useRef<HTMLDivElement>(null)
  const atBottomRef = useRef(true)
  const [showJump, setShowJump] = useState(false)

  useEffect(() => {
    if (atBottomRef.current) {
      bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
    } else {
      setShowJump(true)
    }
  }, [messages.length])

  const onScroll = () => {
    const element = containerRef.current
    if (!element) return
    const distance = element.scrollHeight - element.scrollTop - element.clientHeight
    atBottomRef.current = distance < 80
    setShowJump(distance >= 80)
  }

  const jumpToBottom = () => {
    atBottomRef.current = true
    setShowJump(false)
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }

  if (loading) {
    return (
      <div className="flex flex-1 items-center justify-center text-sm text-gray-500">
        <Loader2 className="mr-2 h-4 w-4 animate-spin" /> Cargando conversación…
      </div>
    )
  }

  return (
    <div
      ref={containerRef}
      onScroll={onScroll}
      className="relative flex-1 space-y-1.5 overflow-y-auto px-4 py-4"
    >
      {hasMore && (
        <div className="flex justify-center">
          <button
            type="button"
            onClick={onLoadOlder}
            disabled={loadingMore}
            className="rounded-full border border-gray-700 bg-gray-900 px-3 py-1 text-xs text-gray-300 transition-colors hover:border-blue-500 hover:text-blue-200 disabled:opacity-50"
          >
            {loadingMore ? 'Cargando…' : 'Cargar mensajes anteriores'}
          </button>
        </div>
      )}

      {messages.length === 0 && (
        <div className="flex flex-1 flex-col items-center justify-center gap-2 py-16 text-center text-gray-500">
          <MessageSquare className="h-8 w-8" />
          <p className="text-sm font-medium text-gray-300">Aún no hay mensajes</p>
          <p className="max-w-sm text-xs">
            Pregunta al asistente o comparte una decisión con el equipo. La conversación es compartida y persistente.
          </p>
        </div>
      )}

      {messages.map((message, index) => {
        const previous = messages[index - 1]
        const showDay = !previous || dayKey(previous.createdAt) !== dayKey(message.createdAt)
        return (
          <div key={message.id} className="space-y-1.5">
            {showDay && (
              <div className="flex justify-center py-2">
                <span className="rounded-full bg-gray-900/90 px-2.5 py-0.5 text-[10px] font-medium uppercase tracking-wide text-gray-500">
                  {dayLabel(message.createdAt)}
                </span>
              </div>
            )}
            <MessageBubble
              message={message}
              isOwn={message.authorId === currentUserId}
              highlighted={highlightedMessageId === message.id}
              mentionNames={mentionNames}
              onCitationClick={onCitationClick}
              onRetry={onRetry}
              onOpenDiagram={onOpenDiagram}
            />
          </div>
        )
      })}
      <div ref={bottomRef} />
      {showJump && (
        <button
          type="button"
          onClick={jumpToBottom}
          className="sticky bottom-2 left-1/2 z-10 flex -translate-x-1/2 items-center gap-1 rounded-full border border-gray-700 bg-gray-900 px-3 py-1.5 text-xs text-gray-200 shadow-lg transition-colors hover:border-blue-500 hover:text-blue-200"
        >
          <ArrowDown className="h-3.5 w-3.5" /> Ir al final
        </button>
      )}
    </div>
  )
}
