import { useEffect, useRef, useState } from 'react'

interface WebSocketMessage {
  type?: string
  userId?: string
  users?: string[]
  data?: unknown
}

const WS_URL = import.meta.env.VITE_API_URL?.replace('http', 'ws') || 'ws://localhost:8080'

export function useWebSocket(diagramId: string, userId: string) {
  const wsRef = useRef<WebSocket | null>(null)
  const reconnectTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const [connected, setConnected] = useState(false)
  const [messages, setMessages] = useState<WebSocketMessage[]>([])

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
          const data = JSON.parse(event.data) as WebSocketMessage
          setMessages(prev => [...prev, data])
        } catch {
          // ignore
        }
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
      if (reconnectTimeoutRef.current) {
        clearTimeout(reconnectTimeoutRef.current)
      }
      wsRef.current?.close()
    }
  }, [diagramId, userId])

  const sendMessage = (_destination: string, _body: unknown) => {
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify(_body))
    }
  }

  return { connected, messages, sendMessage }
}
