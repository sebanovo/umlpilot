import { useCallback, useState } from 'react'
import { aiChatApi, type AiChatAction, type AiChatTurn } from '../../../shared/api/client'

export interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  actions?: AiChatAction[]
}

/**
 * Conversación con el asistente de IA. El historial vive solo en memoria durante la
 * sesión del editor (no se persiste en el servidor ni en localStorage).
 */
export function useAiChat(projectId: string, diagramId: string, onActions?: () => void) {
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const send = useCallback(async (text: string) => {
    const trimmed = text.trim()
    if (!trimmed || loading) return

    const history: AiChatTurn[] = messages.map((m) => ({ role: m.role, content: m.content }))
    setMessages((prev) => [...prev, { role: 'user', content: trimmed }])
    setLoading(true)
    setError(null)

    try {
      const result = await aiChatApi.chat(projectId, diagramId, trimmed, history)
      setMessages((prev) => [...prev, { role: 'assistant', content: result.reply, actions: result.actions }])
      if (result.actions?.some((a) => a.action !== 'read')) onActions?.()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Error al contactar al asistente')
    } finally {
      setLoading(false)
    }
  }, [projectId, diagramId, messages, loading, onActions])

  const reset = useCallback(() => {
    setMessages([])
    setError(null)
  }, [])

  return { messages, loading, error, send, reset }
}
