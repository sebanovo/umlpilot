import { useEffect, useRef, useState, useCallback } from 'react'
import { Client, type IMessage } from '@stomp/stompjs'
import { v4 as uuidv4 } from 'uuid'

const CLIENT_ID = uuidv4()

const CURSOR_TTL_MS = 5000

export interface RemoteCursor {
  clientId: string
  x: number
  y: number
  name: string
  color: string
  ts: number
}

interface PresenceMessage {
  userId: string
  users?: string[]
}

interface DiagramUpdateMessage {
  userId: string
  type: string
  data?: Record<string, unknown>
}

export function colorFor(id: string): string {
  let hash = 0
  for (let i = 0; i < id.length; i++) {
    hash = (hash * 31 + id.charCodeAt(i)) >>> 0
  }
  return `hsl(${hash % 360} 85% 62%)`
}

export function useWebSocket(diagramId: string, userId: string) {
  const clientRef = useRef<Client | null>(null)
  const [connected, setConnected] = useState(false)
  const [presenceUsers, setPresenceUsers] = useState<string[]>([])
  const [remoteChanges, setRemoteChanges] = useState<DiagramUpdateMessage[]>([])
  const [remoteCursors, setRemoteCursors] = useState<Record<string, RemoteCursor>>({})

  useEffect(() => {
    if (!diagramId || !userId) return

    const token = localStorage.getItem('accessToken')

    const client = new Client({
      brokerURL: `ws://${window.location.host}/ws`,
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      reconnectDelay: 3000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,

      onConnect: () => {
        setConnected(true)

        client.publish({
          destination: '/app/joinDiagram',
          body: JSON.stringify({ diagramId }),
        })

        client.subscribe(`/topic/diagram/${diagramId}/presence`, (message: IMessage) => {
          try {
            const body = JSON.parse(message.body) as PresenceMessage
            if (body.users) {
              setPresenceUsers(body.users.filter((u) => u !== userId))
            }
          } catch { /* ignore */ }
        })

        client.subscribe(`/topic/diagram/${diagramId}/changes`, (message: IMessage) => {
          try {
            const body = JSON.parse(message.body) as DiagramUpdateMessage
            const data = body.data as { clientId?: string; x?: number; y?: number; name?: string } | undefined
            const senderClientId = data?.clientId

            if (senderClientId === CLIENT_ID) return

            if (body.type === 'user_cursor') {
              if (senderClientId && typeof data?.x === 'number' && typeof data?.y === 'number') {
                setRemoteCursors((prev) => ({
                  ...prev,
                  [senderClientId]: {
                    clientId: senderClientId,
                    x: data.x as number,
                    y: data.y as number,
                    name: data.name || 'Colaborador',
                    color: colorFor(body.userId || senderClientId),
                    ts: Date.now(),
                  },
                }))
              }
              return
            }

            setRemoteChanges((prev) => [...prev, body])
          } catch { /* ignore */ }
        })
      },

      onDisconnect: () => {
        setConnected(false)
      },

      onStompError: (frame) => {
        console.error('STOMP error:', frame.headers['message'])
        setConnected(false)
      },
    })

    client.activate()
    clientRef.current = client

    return () => {
      client.deactivate()
      clientRef.current = null
    }
  }, [diagramId, userId])

  // Remove cursors that stopped moving
  useEffect(() => {
    const timer = setInterval(() => {
      setRemoteCursors((prev) => {
        const now = Date.now()
        const next: Record<string, RemoteCursor> = {}
        let changed = false
        for (const [key, cursor] of Object.entries(prev)) {
          if (now - cursor.ts < CURSOR_TTL_MS) next[key] = cursor
          else changed = true
        }
        return changed ? next : prev
      })
    }, 2000)
    return () => clearInterval(timer)
  }, [])

  const publish = useCallback((destination: string, body: Record<string, unknown>) => {
    if (clientRef.current?.connected) {
      clientRef.current.publish({
        destination,
        body: JSON.stringify(body),
      })
    }
  }, [])

  const broadcastElementChange = useCallback((type: string, elementId: string, data?: Record<string, unknown>) => {
    publish('/app/diagram/update', { diagramId, type, data: { elementId, clientId: CLIENT_ID, ...data } })
  }, [publish, diagramId])

  const broadcastCursor = useCallback((x: number, y: number, name?: string) => {
    publish('/app/diagram/update', { diagramId, type: 'user_cursor', data: { x, y, name: name || '', clientId: CLIENT_ID } })
  }, [publish, diagramId])

  const clearRemoteChanges = useCallback(() => {
    setRemoteChanges([])
  }, [])

  return {
    connected,
    presenceUsers,
    remoteChanges,
    remoteCursors,
    publish,
    broadcastElementChange,
    broadcastCursor,
    clearRemoteChanges,
  }
}
