import type { ChatMessage } from '../api/groupChatApi'
import type { MentionOption } from '../model/mentions'
import { ChatComposer } from './ChatComposer'
import { MessageList } from './MessageList'

interface AiConversationProps {
  messages: ChatMessage[]
  currentUserId: string
  loading: boolean
  hasMore: boolean
  loadingMore: boolean
  sending: boolean
  canMutate: boolean
  typingUsers: string[]
  highlightedMessageId: string | null
  mentionOptions: MentionOption[]
  mentionNames: string[]
  error: string | null
  onSend: (content: string) => void
  onRetry: (messageId: string) => void
  onLoadOlder: () => void
  onCitationClick: (messageId: string) => void
  onOpenDiagram: (diagramId: string) => void
  onTyping: (typing: boolean) => void
  onClearError: () => void
}

export function AiConversation({
  messages,
  currentUserId,
  loading,
  hasMore,
  loadingMore,
  sending,
  canMutate,
  typingUsers,
  highlightedMessageId,
  mentionOptions,
  mentionNames,
  error,
  onSend,
  onRetry,
  onLoadOlder,
  onCitationClick,
  onOpenDiagram,
  onTyping,
  onClearError,
}: AiConversationProps) {
  return (
    <section className="flex min-h-0 flex-1 flex-col overflow-hidden rounded-2xl border border-gray-800 bg-gray-900/60">
      <MessageList
        messages={messages}
        currentUserId={currentUserId}
        loading={loading}
        hasMore={hasMore}
        loadingMore={loadingMore}
        highlightedMessageId={highlightedMessageId}
        mentionNames={mentionNames}
        onLoadOlder={onLoadOlder}
        onCitationClick={onCitationClick}
        onRetry={onRetry}
        onOpenDiagram={onOpenDiagram}
      />
      {error && (
        <div className="mx-4 mb-2 flex items-center justify-between rounded-lg border border-red-900 bg-red-950/60 px-3 py-2 text-xs text-red-200">
          <span>{error}</span>
          <button type="button" onClick={onClearError} className="text-red-300 hover:text-white">
            Cerrar
          </button>
        </div>
      )}
      {typingUsers.length > 0 && (
        <p className="mx-4 mb-1 text-[11px] text-gray-500">
          {typingUsers.join(', ')} {typingUsers.length === 1 ? 'está escribiendo…' : 'están escribiendo…'}
        </p>
      )}
      <ChatComposer
        sending={sending}
        canMutate={canMutate}
        mentionOptions={mentionOptions}
        onSend={onSend}
        onTyping={onTyping}
      />
    </section>
  )
}
