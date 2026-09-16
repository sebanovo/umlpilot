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

  /** Transcribe un audio a texto (Groq/Whisper). No agrega mensajes a la conversación. */
  const transcribeAudio = useCallback(async (audio: Blob): Promise<string> => {
    const ext = audio.type.includes('mp4') ? 'mp4' : audio.type.includes('ogg') ? 'ogg' : 'webm'
    const result = await aiChatApi.transcribe(projectId, diagramId, audio, `audio.${ext}`)
    return result.text
  }, [projectId, diagramId])

  /** Construye el diagrama a partir de una imagen (solo creación). */
  const buildFromImage = useCallback(async (image: File) => {
    if (loading) return
    setMessages((prev) => [...prev, { role: 'user', content: `[Imagen] ${image.name}` }])
    setLoading(true)
    setError(null)
    try {
      const result = await aiChatApi.diagramFromImage(projectId, diagramId, image)
      setMessages((prev) => [...prev, { role: 'assistant', content: result.reply, actions: result.actions }])
      if (result.actions?.some((a) => a.action !== 'read')) onActions?.()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Error al construir desde la imagen')
    } finally {
      setLoading(false)
    }
  }, [projectId, diagramId, loading, onActions])

  /** Construye el diagrama completo a partir de una descripción de texto. */
  const buildFromText = useCallback(async (text: string) => {
    const prompt = text.trim()
    if (!prompt || loading) return
    setMessages((prev) => [...prev, { role: 'user', content: prompt }])
    setLoading(true)
    setError(null)
    try {
      const result = await aiChatApi.diagramFromText(projectId, diagramId, prompt)
      setMessages((prev) => [...prev, { role: 'assistant', content: result.reply, actions: result.actions }])
      if (result.actions?.some((a) => a.action !== 'read')) onActions?.()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Error al construir desde el texto')
    } finally {
      setLoading(false)
    }
  }, [projectId, diagramId, loading, onActions])

  return { messages, loading, error, send, reset, transcribeAudio, buildFromImage, buildFromText }
}
