import { Quote } from 'lucide-react'
import type { ChatCitation } from '../api/groupChatApi'

interface CitationListProps {
  citations: ChatCitation[]
  onCitationClick: (messageId: string) => void
}

export function CitationList({ citations, onCitationClick }: CitationListProps) {
  if (!citations.length) return null
  return (
    <div className="mt-2 space-y-1 border-t border-white/10 pt-2">
      <p className="flex items-center gap-1 text-[10px] uppercase tracking-wide text-gray-400">
        <Quote className="w-3 h-3" /> Referencias
      </p>
      {citations.map((citation) => (
        <button
          key={citation.messageId}
          type="button"
          onClick={() => onCitationClick(citation.messageId)}
          className="block w-full rounded-md border border-gray-700/70 bg-gray-900/40 px-2 py-1 text-left text-[11px] text-gray-300 transition-colors hover:border-blue-500 hover:text-blue-200"
        >
          <span className="font-medium text-blue-300">{citation.authorName}</span>
          <span className="text-gray-500"> · </span>
          <span>{citation.snippet}</span>
        </button>
      ))}
    </div>
  )
}
