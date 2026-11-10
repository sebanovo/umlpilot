import { useCallback, useEffect, useState } from 'react'
import { groupChatApi, type ChatAiStatus, type ChatConversation, type ChatMessage } from '../api/groupChatApi'
import { mentionsAssistant } from './mentions'

export interface ChatUser {
  id: string
  name: string
  role: string
}

const PAGE_SIZE = 50

function sortMessages(messages: ChatMessage[]): ChatMessage[] {
  return [...messages].sort((a, b) => {
    const byDate = new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
    if (byDate !== 0) return byDate
    return a.id.localeCompare(b.id)
  })
}

function newClientMessageId(): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) return crypto.randomUUID()
  return `chat-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

export function useGroupChat(projectId: string | undefined, currentUser: ChatUser) {
  const [conversation, setConversation] = useState<ChatConversation | null>(null)
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [loading, setLoading] = useState(true)
  const [loadingMore, setLoadingMore] = useState(false)
  const [sending, setSending] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [nextCursor, setNextCursor] = useState<string | null>(null)
  const [hasMore, setHasMore] = useState(false)

  const mergeMessage = useCallback((incoming: ChatMessage) => {
    setMessages((prev) => {
      const existing = prev.findIndex((message) =>
        message.id === incoming.id
        || (incoming.clientMessageId && message.clientMessageId === incoming.clientMessageId))
      if (existing >= 0) {
        const next = [...prev]
        next[existing] = { ...incoming, pending: false, failed: false }
        return sortMessages(next)
      }
      return sortMessages([...prev, incoming])
    })
  }, [])

  const loadInitial = useCallback(async () => {
    if (!projectId) return
    setLoading(true)
    setError(null)
    try {
      const [conversationResult, page] = await Promise.all([
        groupChatApi.getConversation(projectId),
        groupChatApi.getMessages(projectId, undefined, PAGE_SIZE),
      ])
      setConversation(conversationResult)
      setMessages(sortMessages(page.messages))
      setNextCursor(page.nextCursor)
      setHasMore(page.hasMore)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo cargar el chat')
    } finally {
      setLoading(false)
    }
  }, [projectId])

  useEffect(() => {
    void loadInitial()
  }, [loadInitial])

  const refresh = useCallback(async () => {
    if (!projectId) return
    try {
      const [conversationResult, page] = await Promise.all([
        groupChatApi.getConversation(projectId),
        groupChatApi.getMessages(projectId, undefined, PAGE_SIZE),
      ])
      setConversation(conversationResult)
      for (const message of page.messages) mergeMessage(message)
    } catch {
      // La reconexión no debe borrar el historial ya visible.
    }
  }, [projectId, mergeMessage])

  const loadOlder = useCallback(async () => {
    if (!projectId || !nextCursor || loadingMore) return
    setLoadingMore(true)
    try {
      const page = await groupChatApi.getMessages(projectId, nextCursor, PAGE_SIZE)
      setMessages((prev) => sortMessages([...page.messages, ...prev]))
      setNextCursor(page.nextCursor)
      setHasMore(page.hasMore)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudieron cargar los mensajes anteriores')
    } finally {
      setLoadingMore(false)
    }
  }, [projectId, nextCursor, loadingMore])

  const loadUntilMessage = useCallback(async (messageId: string) => {
    if (!projectId) return false
    if (messages.some((message) => message.id === messageId)) return true
    let cursor = nextCursor
    let guard = 0
    while (cursor && guard < 20) {
      guard += 1
      const page = await groupChatApi.getMessages(projectId, cursor, PAGE_SIZE)
      setMessages((prev) => sortMessages([...page.messages, ...prev]))
      setNextCursor(page.nextCursor)
      setHasMore(page.hasMore)
      if (page.messages.some((message) => message.id === messageId)) return true
      cursor = page.nextCursor
    }
    return false
  }, [projectId, messages, nextCursor])

  const sendMessage = useCallback(async (content: string, diagramId: string | null) => {
    const trimmed = content.trim()
    if (!projectId || !trimmed || sending) return
    const clientMessageId = newClientMessageId()
    const optimistic: ChatMessage = {
      id: `pending-${clientMessageId}`,
      conversationId: conversation?.conversationId ?? '',
      projectId,
      diagramId,
      authorType: 'user',
      authorId: currentUser.id,
      authorName: currentUser.name,
      authorRole: currentUser.role,
      content: trimmed,
      clientMessageId,
      replyToId: null,
      citations: [],
      actions: [],
      provider: null,
      model: null,
      aiStatus: mentionsAssistant(trimmed) ? 'generating' : 'none',
      createdAt: new Date().toISOString(),
      pending: true,
    }
    setMessages((prev) => sortMessages([...prev, optimistic]))
    setSending(true)
    setError(null)
    try {
      const result = await groupChatApi.sendMessage(projectId, {
        clientMessageId,
        content: trimmed,
        diagramId,
      })
      mergeMessage(result.message)
    } catch (e) {
      setMessages((prev) => prev.map((message) =>
        message.clientMessageId === clientMessageId
          ? { ...message, pending: false, failed: true, aiStatus: 'failed' }
          : message))
      setError(e instanceof Error ? e.message : 'No se pudo enviar el mensaje')
    } finally {
      setSending(false)
    }
  }, [projectId, sending, conversation, currentUser, mergeMessage])

  const retryMessage = useCallback(async (messageId: string) => {
    if (!projectId) return
    setError(null)
    setMessages((prev) => prev.map((message) =>
      message.id === messageId ? { ...message, failed: false, aiStatus: 'generating' } : message))
    try {
      await groupChatApi.retry(projectId, messageId)
    } catch (e) {
      setMessages((prev) => prev.map((message) =>
        message.id === messageId ? { ...message, failed: true, aiStatus: 'failed' } : message))
      setError(e instanceof Error ? e.message : 'No se pudo reintentar la respuesta')
    }
  }, [projectId])

  const transcribeAudio = useCallback(async (audio: Blob): Promise<string> => {
    if (!projectId) throw new Error('Proyecto no disponible')
    const ext = audio.type.includes('mp4') ? 'mp4' : audio.type.includes('ogg') ? 'ogg' : 'webm'
    const result = await groupChatApi.transcribe(projectId, audio, `audio.${ext}`)
    return result.text
  }, [projectId])

  const applyAiStatus = useCallback((messageId: string, status: ChatAiStatus, statusError?: string) => {
    setMessages((prev) => prev.map((message) =>
      message.id === messageId ? { ...message, aiStatus: status, failed: status === 'failed' } : message))
    if (status === 'failed' && statusError) setError(statusError)
  }, [])

  return {
    conversation,
    messages,
    loading,
    loadingMore,
    sending,
    error,
    hasMore,
    mergeMessage,
    applyAiStatus,
    loadOlder,
    loadUntilMessage,
    refresh,
    sendMessage,
    retryMessage,
    transcribeAudio,
    clearError: () => setError(null),
  }
}
