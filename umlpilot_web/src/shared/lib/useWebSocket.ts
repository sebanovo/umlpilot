import { useEffect, useRef, useState, useCallback } from 'react'

interface WebSocketMessage {
  type?: string
  userId?: string
  users?: string[]
  data?: unknown
  elementId?: string
  position?: { x: number; y: number }
}

const WS_URL = import.meta.env.VITE_API_URL?.replace('http', 'ws') || 'ws://localhost:8080'

export function useWebSocket(diagramId: string, userId: string) {
  const wsRef = useRef<WebSocket | null>(null)
  const reconnectTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const [connected, setConnected] = useState(false)
  const [presenceUsers, setPresenceUsers] = useState<string[]>([])
  const [remoteChanges, setRemoteChanges] = useState<WebSocketMessage[]>([])

  useEffect(() => {
    if (!diagramId || !userId) return
    let cancelled = false

    function connect() {
      if (cancelled) return
      const token = localStorage.getItem('accessToken')
      const ws = new WebSocket(`${WS_URL}/ws?token=${token}`)

      ws.onopen = () => {
        if (cancelled) return
        setConnected(true)
        ws.send(JSON.stringify({ destination: '/app/joinDiagram', body: { diagramId } }))
      }

      ws.onmessage = (event) => {
        try {
          const msg = JSON.parse(event.data) as WebSocketMessage
          if (msg.type) {
            setRemoteChanges(prev => [...prev, msg])
          }
          if (msg.users) {
            setPresenceUsers(msg.users.filter((u) => u !== userId))
          }
        } catch { /* ignore */ }
      }

      ws.onclose = () => {
        if (cancelled) return
        setConnected(false)
        reconnectTimeoutRef.current = setTimeout(() => connect(), 3000)
      }

      wsRef.current = ws
    }

    connect()
    return () => {
      cancelled = true
      if (reconnectTimeoutRef.current) clearTimeout(reconnectTimeoutRef.current)
      wsRef.current?.close()
    }
  }, [diagramId, userId])

  const sendMessage = useCallback((destination: string, body: unknown) => {
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify({ destination, body }))
    }
  }, [])

  const broadcastElementChange = useCallback((type: string, elementId: string, data?: Record<string, unknown>) => {
    const body = { diagramId, type, data: { elementId, ...data } }
    sendMessage('/app/diagram/update', body)
  }, [sendMessage, diagramId])

  const broadcastCursor = useCallback((x: number, y: number) => {
    const body = { diagramId, type: 'user_cursor', data: { x, y } }
    sendMessage('/app/diagram/update', body)
  }, [sendMessage, diagramId])

  const clearRemoteChanges = useCallback(() => {
    setRemoteChanges([])
  }, [])

  return {
    connected,
    presenceUsers,
    remoteChanges,
    sendMessage,
    broadcastElementChange,
    broadcastCursor,
    clearRemoteChanges,
  }
}
