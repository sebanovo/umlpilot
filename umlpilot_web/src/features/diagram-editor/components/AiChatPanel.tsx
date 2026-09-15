import { useEffect, useRef, useState } from 'react'
import { Bot, Send, X, Sparkles } from 'lucide-react'
import type { ChatMessage } from '../hooks/useAiChat'

interface AiChatPanelProps {
  messages: ChatMessage[]
  loading: boolean
  error: string | null
  onSend: (text: string) => void
  onReset: () => void
  onClose: () => void
  width?: number
}

const ACTION_LABELS: Record<string, string> = {
  create: 'creó',
  update: 'actualizó',
  delete: 'eliminó',
  read: 'leyó',
}

function actionSummary(actions?: ChatMessage['actions']): string[] {
  if (!actions || actions.length === 0) return []
  const mutations = actions.filter((a) => a.action !== 'read')
  if (mutations.length === 0) return []
  return mutations.map((a) => `${ACTION_LABELS[a.action] || a.action} ${a.entity}`)
}

export function AiChatPanel({ messages, loading, error, onSend, onReset, onClose, width }: AiChatPanelProps) {
  const [input, setInput] = useState('')
  const listRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    listRef.current?.scrollTo({ top: listRef.current.scrollHeight, behavior: 'smooth' })
  }, [messages, loading])

  const submit = () => {
    const text = input.trim()
    if (!text || loading) return
    onSend(text)
    setInput('')
  }

  return (
    <div className="shrink-0 min-h-0 bg-gray-900 border-l border-gray-800 flex flex-col overflow-hidden" style={width ? { width } : undefined}>
      <div className="px-4 py-3 border-b border-gray-800 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Sparkles className="w-4 h-4 text-blue-400" />
          <h3 className="text-sm font-semibold text-gray-100">Asistente IA</h3>
        </div>
        <div className="flex items-center gap-1">
          <button onClick={onReset} title="Nueva conversación"
            className="text-xs text-gray-400 hover:text-gray-200 px-2 py-1 rounded cursor-pointer transition-colors">
            Limpiar
          </button>
          <button onClick={onClose} title="Cerrar"
            className="text-gray-400 hover:text-gray-200 p-1 rounded cursor-pointer transition-colors">
            <X className="w-4 h-4" />
          </button>
        </div>
      </div>

      <div ref={listRef} className="flex-1 min-h-0 overflow-y-auto p-4 space-y-3">
        {messages.length === 0 && (
          <div className="text-xs text-gray-500 space-y-1">
            <p>Pídele cambios sobre el diagrama, por ejemplo:</p>
            <p className="text-gray-400">· "crea una clase Cliente"</p>
            <p className="text-gray-400">· "agrega el atributo nombre:String a Cliente"</p>
            <p className="text-gray-400">· "relaciona Cliente con Pedido"</p>
          </div>
        )}

        {messages.map((m, i) => (
          <div key={i} className={m.role === 'user' ? 'flex justify-end' : 'flex justify-start'}>
            <div className={m.role === 'user'
              ? 'max-w-[85%] rounded-lg px-3 py-2 text-sm bg-blue-600 text-white whitespace-pre-wrap'
              : 'max-w-[85%] rounded-lg px-3 py-2 text-sm bg-gray-800 text-gray-100 whitespace-pre-wrap'}>
              {m.role === 'assistant' && (
                <span className="flex items-center gap-1 text-[10px] uppercase tracking-wide text-blue-400 mb-1">
                  <Bot className="w-3 h-3" /> Asistente
                </span>
              )}
              {m.content}
              {actionSummary(m.actions).length > 0 && (
                <ul className="mt-2 space-y-0.5">
                  {actionSummary(m.actions).map((s, j) => (
                    <li key={j} className="text-[11px] text-emerald-400">✓ {s}</li>
                  ))}
                </ul>
              )}
            </div>
          </div>
        ))}

        {loading && (
          <div className="flex justify-start">
            <div className="rounded-lg px-3 py-2 text-sm bg-gray-800 text-gray-400 flex items-center gap-2">
              <span className="w-1.5 h-1.5 rounded-full bg-blue-400 animate-pulse" />
              Pensando…
            </div>
          </div>
        )}

        {error && (
          <div className="rounded-md border border-red-900 bg-red-950/60 px-3 py-2 text-xs text-red-300">
            {error}
          </div>
        )}
      </div>

      <div className="border-t border-gray-800 p-3 flex gap-2">
        <textarea value={input} rows={2}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); submit() }
          }}
          placeholder="Escribe una instrucción…"
          className="flex-1 resize-none rounded-md border border-gray-700 bg-gray-800 px-2.5 py-1.5 text-sm text-gray-100 placeholder-gray-500 focus:border-blue-500 focus:ring-1 focus:ring-blue-500/40 outline-none" />
        <button onClick={submit} disabled={!input.trim() || loading} title="Enviar"
          className="self-end rounded-md bg-blue-600 px-2.5 py-2 text-white hover:bg-blue-500 disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer transition-colors">
          <Send className="w-4 h-4" />
        </button>
      </div>
    </div>
  )
}
