import { useCallback, useEffect, useRef, useState } from 'react'
import { Client, type IMessage } from '@stomp/stompjs'
import type { ChatAiStatus, ChatMessage } from '../api/groupChatApi'

export interface ChatSocketHandlers {
  onMessage: (message: ChatMessage) => void
  onAiStatus: (messageId: string, status: ChatAiStatus, error?: string) => void
  onTyping: (userId: string, name: string, typing: boolean) => void
  onPresence: (users: string[]) => void
  onConnectionChange?: (connected: boolean) => void
  onReconnect?: () => void
}

export function useChatSocket(projectId: string | undefined, handlers: ChatSocketHandlers) {
  const clientRef = useRef<Client | null>(null)
  const handlersRef = useRef(handlers)
  const connectedOnceRef = useRef(false)
  const [connected, setConnected] = useState(false)

  useEffect(() => {
    handlersRef.current = handlers
  }, [handlers])

  useEffect(() => {
    if (!projectId) return

    const token = localStorage.getItem('accessToken')
    const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
    const client = new Client({
      brokerURL: `${protocol}://${window.location.host}/ws`,
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      reconnectDelay: 3000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,

      onConnect: () => {
        setConnected(true)
        handlersRef.current.onConnectionChange?.(true)
        client.subscribe(`/topic/project/${projectId}/chat`, (frame: IMessage) => {
          try {
            const event = JSON.parse(frame.body) as Record<string, unknown>
            switch (event.type) {
              case 'chat_message_created':
                handlersRef.current.onMessage(event.message as ChatMessage)
                break
              case 'chat_ai_status':
                handlersRef.current.onAiStatus(
                  String(event.messageId ?? ''),
                  String(event.status ?? 'none') as ChatAiStatus,
                  event.error ? String(event.error) : undefined,
                )
                break
              case 'chat_typing':
                handlersRef.current.onTyping(
                  String(event.userId ?? ''),
                  String(event.name ?? 'Colaborador'),
                  Boolean(event.typing),
                )
                break
              case 'chat_presence':
                handlersRef.current.onPresence(
                  Array.isArray(event.users) ? event.users.map(String) : [],
                )
                break
              default:
                break
            }
          } catch {
            // Evento malformado: se ignora sin romper el chat.
          }
        })
        client.publish({
          destination: '/app/chat/presence',
          body: JSON.stringify({ projectId }),
        })
        if (connectedOnceRef.current) {
          handlersRef.current.onReconnect?.()
        }
        connectedOnceRef.current = true
      },

      onDisconnect: () => {
        setConnected(false)
        handlersRef.current.onConnectionChange?.(false)
      },

      onStompError: () => {
        setConnected(false)
        handlersRef.current.onConnectionChange?.(false)
      },
    })

    client.activate()
    clientRef.current = client

    return () => {
      client.deactivate()
      clientRef.current = null
    }
  }, [projectId])

  const sendTyping = useCallback((typing: boolean) => {
    if (!projectId || !clientRef.current?.connected) return
    clientRef.current.publish({
      destination: '/app/chat/typing',
      body: JSON.stringify({ projectId, typing }),
    })
  }, [projectId])

  return { connected, sendTyping }
}
