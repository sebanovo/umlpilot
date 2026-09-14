import { useEffect, useRef, useState, useCallback } from 'react'
import { Client, type IMessage } from '@stomp/stompjs'
import { v4 as uuidv4 } from 'uuid'

const CLIENT_ID = uuidv4()

interface PresenceMessage {
  userId: string
  users?: string[]
}

interface DiagramUpdateMessage {
  userId: string
  type: string
  data?: Record<string, unknown>
}

export function useWebSocket(diagramId: string, userId: string) {
  const clientRef = useRef<Client | null>(null)
  const [connected, setConnected] = useState(false)
  const [presenceUsers, setPresenceUsers] = useState<string[]>([])
  const [remoteChanges, setRemoteChanges] = useState<DiagramUpdateMessage[]>([])

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
            const senderClientId = (body.data as { clientId?: string } | undefined)?.clientId
            if (senderClientId !== CLIENT_ID) {
              setRemoteChanges((prev) => [...prev, body])
            }
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

  const broadcastCursor = useCallback((x: number, y: number) => {
    publish('/app/diagram/update', { diagramId, type: 'user_cursor', data: { x, y, clientId: CLIENT_ID } })
  }, [publish, diagramId])

  const clearRemoteChanges = useCallback(() => {
    setRemoteChanges([])
  }, [])

  return {
    connected,
    presenceUsers,
    remoteChanges,
    publish,
    broadcastElementChange,
    broadcastCursor,
    clearRemoteChanges,
  }
}
