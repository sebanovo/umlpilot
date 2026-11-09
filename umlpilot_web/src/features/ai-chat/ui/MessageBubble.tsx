import { Bot, ExternalLink, RotateCcw, UserRound } from 'lucide-react'
import type { ChatMessage } from '../api/groupChatApi'
import { AiStatusBadge } from './AiStatusBadge'
import { CitationList } from './CitationList'
import { MentionText } from './MentionText'

interface MessageBubbleProps {
  message: ChatMessage
  isOwn: boolean
  highlighted?: boolean
  mentionNames: string[]
  onCitationClick: (messageId: string) => void
  onRetry: (messageId: string) => void
  onOpenDiagram: (diagramId: string) => void
}

const ACTION_LABELS: Record<string, string> = {
  create: 'creó',
  update: 'actualizó',
  delete: 'eliminó',
  read: 'leyó',
}

function formatTime(value: string): string {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

export function MessageBubble({
  message,
  isOwn,
  highlighted,
  mentionNames,
  onCitationClick,
  onRetry,
  onOpenDiagram,
}: MessageBubbleProps) {
  if (message.authorType === 'system') {
    return (
      <div className="flex justify-center">
        <p className="max-w-[85%] rounded-full bg-gray-900/80 px-3 py-1 text-center text-[11px] text-gray-500">
          {message.content}
        </p>
      </div>
    )
  }

  const assistant = message.authorType === 'assistant'
  const mutations = message.actions.filter((action) => action.action !== 'read')
  const canOpenDiagram = Boolean(message.diagramId && mutations.length > 0)

  return (
    <div id={`chat-message-${message.id}`} className={`flex ${isOwn ? 'justify-end' : 'justify-start'}`}>
      <div
        className={`max-w-[82%] px-3.5 py-2.5 text-sm shadow-sm transition-colors ${
          isOwn
            ? 'rounded-2xl rounded-br-md bg-[#005c4b] text-white'
            : 'rounded-2xl rounded-bl-md border border-gray-700/70 bg-gray-800 text-gray-100'
        } ${highlighted ? 'ring-2 ring-amber-400 ring-offset-2 ring-offset-gray-950' : ''}`}
      >
        <div className={`mb-1 flex items-center gap-1.5 text-[10px] uppercase tracking-wide ${isOwn ? 'text-blue-100/80' : 'text-gray-400'}`}>
          {assistant ? <Bot className="w-3 h-3" /> : <UserRound className="w-3 h-3" />}
          <span>{message.authorName}</span>
          <span>·</span>
          <span>{message.authorRole}</span>
          <span>·</span>
          <span>{formatTime(message.createdAt)}</span>
          {message.provider && (
            <span className="ml-auto rounded bg-black/20 px-1.5 py-0.5 normal-case tracking-normal text-[10px]">
              {message.provider}{message.model ? ` / ${message.model}` : ''}
            </span>
          )}
        </div>

        <p className="whitespace-pre-wrap break-words">
          <MentionText content={message.content} labels={mentionNames} />
        </p>

        {message.citations.length > 0 && (
          <CitationList citations={message.citations} onCitationClick={onCitationClick} />
        )}

        {mutations.length > 0 && (
          <ul className="mt-2 space-y-0.5 border-t border-white/10 pt-2">
            {mutations.map((action, index) => (
              <li key={`${action.entity}-${index}`} className="text-[11px] text-emerald-300">
                ✓ {ACTION_LABELS[action.action] || action.action} {action.entity}
              </li>
            ))}
          </ul>
        )}

        {canOpenDiagram && message.diagramId && (
          <button
            type="button"
            onClick={() => onOpenDiagram(message.diagramId as string)}
            className="mt-2 inline-flex items-center gap-1 text-[11px] text-blue-200 hover:text-white"
          >
            <ExternalLink className="w-3 h-3" /> Abrir diagrama
          </button>
        )}

        {!assistant && message.aiStatus !== 'none' && (
          <div className="mt-1.5 flex items-center gap-2">
            <AiStatusBadge status={message.aiStatus} />
            {message.failed && (
              <button
                type="button"
                onClick={() => onRetry(message.id)}
                className="inline-flex items-center gap-1 text-[11px] text-red-100 hover:text-white"
              >
                <RotateCcw className="w-3 h-3" /> Reintentar
              </button>
            )}
          </div>
        )}
      </div>
    </div>
  )
}
