import { useEffect, useState } from 'react'
import { ImageOff, Loader2 } from 'lucide-react'
import { groupChatApi } from '../../../shared/api/client'

interface ChatImagePreviewProps {
  projectId: string
  messageId: string
  alt: string
}

export function ChatImagePreview({ projectId, messageId, alt }: ChatImagePreviewProps) {
  const [url, setUrl] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [failed, setFailed] = useState(false)

  useEffect(() => {
    let active = true
    let objectUrl: string | null = null
    setLoading(true)
    setFailed(false)
    setUrl(null)

    groupChatApi.getMessageImage(projectId, messageId)
      .then((blob) => {
        if (!active) return
        objectUrl = URL.createObjectURL(blob)
        setUrl(objectUrl)
      })
      .catch(() => {
        if (active) setFailed(true)
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => {
      active = false
      if (objectUrl) URL.revokeObjectURL(objectUrl)
    }
  }, [projectId, messageId])

  if (failed) {
    return (
      <div className="mb-2 flex h-24 items-center justify-center gap-2 rounded-lg border border-gray-700 bg-gray-900/60 text-[11px] text-gray-500">
        <ImageOff className="h-4 w-4" /> Imagen no disponible
      </div>
    )
  }

  if (loading || !url) {
    return (
      <div className="mb-2 flex h-24 items-center justify-center rounded-lg border border-gray-700 bg-gray-900/60">
        <Loader2 className="h-4 w-4 animate-spin text-blue-400" />
      </div>
    )
  }

  return (
    <img
      src={url}
      alt={alt}
      className="mb-2 max-h-64 w-full rounded-lg border border-white/10 object-contain"
    />
  )
}
