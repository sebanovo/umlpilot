import { Loader2, Check, AlertTriangle } from 'lucide-react'
import type { ChatAiStatus } from '../api/groupChatApi'

export function AiStatusBadge({ status }: { status: ChatAiStatus }) {
  if (status === 'generating') {
    return (
      <span className="inline-flex items-center gap-1 text-[11px] text-blue-300">
        <Loader2 className="w-3 h-3 animate-spin" /> IA generando…
      </span>
    )
  }
  if (status === 'failed') {
    return (
      <span className="inline-flex items-center gap-1 text-[11px] text-red-300">
        <AlertTriangle className="w-3 h-3" /> Error de IA
      </span>
    )
  }
  if (status === 'completed') {
    return (
      <span className="inline-flex items-center gap-1 text-[11px] text-emerald-300">
        <Check className="w-3 h-3" /> Respondido
      </span>
    )
  }
  return null
}
