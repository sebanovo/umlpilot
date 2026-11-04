import { useCallback, useEffect, useRef, useState } from 'react'
import { Bell, Check, CheckCheck, Loader2, X } from 'lucide-react'
import { notificationsApi, type AppNotification } from '../../../shared/api/client'
import { useToast } from '../../../shared/ui/toast/useToast'

interface NotificationBellProps {
  /** Se llama cuando el usuario acepta una invitación (para refrescar proyectos). */
  onInvitationAccepted?: () => void
}

const POLL_INTERVAL_MS = 30_000

function formatDate(value: string): string {
  return new Date(value).toLocaleString('es-ES', {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  })
}

/**
 * Campana de notificaciones con bandeja persistente.
 * Sin websockets ni push: refresca el contador por polling ligero y al enfocar la ventana.
 */
export function NotificationBell({ onInvitationAccepted }: NotificationBellProps) {
  const { toast } = useToast()
  const [open, setOpen] = useState(false)
  const [notifications, setNotifications] = useState<AppNotification[]>([])
  const [unread, setUnread] = useState(0)
  const [loading, setLoading] = useState(false)
  const [busyId, setBusyId] = useState<string | null>(null)
  const containerRef = useRef<HTMLDivElement>(null)

  const refreshCount = useCallback(async () => {
    try {
      const { count } = await notificationsApi.unreadCount()
      setUnread(count)
    } catch {
      // La campana no debe romper la página si el contador falla.
    }
  }, [])

  useEffect(() => {
    void refreshCount()
    const interval = window.setInterval(() => { void refreshCount() }, POLL_INTERVAL_MS)
    const onFocus = () => { void refreshCount() }
    window.addEventListener('focus', onFocus)
    return () => {
      window.clearInterval(interval)
      window.removeEventListener('focus', onFocus)
    }
  }, [refreshCount])

  useEffect(() => {
    if (!open) return
    const onPointerDown = (event: MouseEvent) => {
      if (!containerRef.current?.contains(event.target as Node)) setOpen(false)
    }
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setOpen(false)
    }
    document.addEventListener('mousedown', onPointerDown)
    document.addEventListener('keydown', onKeyDown)
    return () => {
      document.removeEventListener('mousedown', onPointerDown)
      document.removeEventListener('keydown', onKeyDown)
    }
  }, [open])

  const loadNotifications = useCallback(async () => {
    setLoading(true)
    try {
      const items = await notificationsApi.list()
      setNotifications(items)
      setUnread(items.filter((item) => !item.read).length)
    } catch (err) {
      toast({
        title: 'No se pudieron cargar las notificaciones',
        description: err instanceof Error ? err.message : 'Error inesperado',
        variant: 'error',
      })
    } finally {
      setLoading(false)
    }
  }, [toast])

  const toggle = () => {
    const next = !open
    setOpen(next)
    if (next) void loadNotifications()
  }

  const markAsRead = async (notification: AppNotification) => {
    if (notification.read) return
    try {
      await notificationsApi.markAsRead(notification.id)
      setNotifications((current) =>
        current.map((item) => (item.id === notification.id ? { ...item, read: true } : item)),
      )
      setUnread((current) => Math.max(0, current - 1))
    } catch (err) {
      toast({
        title: 'No se pudo marcar como leída',
        description: err instanceof Error ? err.message : 'Error inesperado',
        variant: 'error',
      })
    }
  }

  const markAllAsRead = async () => {
    try {
      await notificationsApi.markAllAsRead()
      setNotifications((current) => current.map((item) => ({ ...item, read: true })))
      setUnread(0)
    } catch (err) {
      toast({
        title: 'No se pudieron marcar como leídas',
        description: err instanceof Error ? err.message : 'Error inesperado',
        variant: 'error',
      })
    }
  }

  const respond = async (notification: AppNotification, action: 'accept' | 'reject') => {
    setBusyId(notification.id)
    try {
      await notificationsApi.respond(notification.id, action)
      const accepted = action === 'accept'
      setNotifications((current) =>
        current.map((item) =>
          item.id === notification.id
            ? { ...item, read: true, invitationStatus: accepted ? 'accepted' : 'rejected' }
            : item,
        ),
      )
      void refreshCount()
      toast({
        title: accepted ? 'Invitación aceptada' : 'Invitación rechazada',
        description: accepted
          ? 'Ya tienes acceso al proyecto'
          : 'La propuesta fue descartada',
        variant: accepted ? 'success' : 'info',
      })
      if (accepted) onInvitationAccepted?.()
    } catch (err) {
      toast({
        title: 'No se pudo registrar la respuesta',
        description: err instanceof Error ? err.message : 'Error inesperado',
        variant: 'error',
      })
    } finally {
      setBusyId(null)
    }
  }

  return (
    <div className="relative" ref={containerRef}>
      <button
        type="button"
        onClick={toggle}
        aria-label={unread > 0 ? `Notificaciones (${unread} sin leer)` : 'Notificaciones'}
        aria-expanded={open}
        className="relative flex h-9 w-9 items-center justify-center rounded-lg border border-gray-700 bg-gray-800 text-gray-300 transition-colors hover:bg-gray-700 hover:text-gray-100 cursor-pointer"
      >
        <Bell className="h-4 w-4" />
        {unread > 0 && (
          <span className="absolute -right-1 -top-1 flex h-4 min-w-4 items-center justify-center rounded-full bg-blue-500 px-1 text-[10px] font-bold leading-none text-white">
            {unread > 9 ? '9+' : unread}
          </span>
        )}
      </button>

      {open && (
        <div
          role="dialog"
          aria-label="Bandeja de notificaciones"
          className="absolute right-0 z-40 mt-2 w-80 overflow-hidden rounded-xl border border-gray-800 bg-gray-900 shadow-2xl shadow-black/60 sm:w-96"
        >
          <div className="flex items-center justify-between border-b border-gray-800 px-4 py-3">
            <div>
              <h2 className="text-sm font-semibold text-gray-100">Notificaciones</h2>
              <p className="text-xs text-gray-500">
                {unread > 0 ? `${unread} sin leer` : 'Todo al día'}
              </p>
            </div>
            {unread > 0 && (
              <button
                type="button"
                onClick={markAllAsRead}
                className="flex items-center gap-1 text-xs font-medium text-blue-400 hover:text-blue-300 cursor-pointer"
              >
                <CheckCheck className="h-3.5 w-3.5" />
                Marcar todas
              </button>
            )}
          </div>

          <div className="max-h-96 overflow-y-auto">
            {loading ? (
              <div className="flex items-center justify-center py-10 text-gray-400">
                <Loader2 className="h-5 w-5 animate-spin" />
              </div>
            ) : notifications.length === 0 ? (
              <div className="px-4 py-10 text-center">
                <Bell className="mx-auto mb-2 h-8 w-8 text-gray-700" />
                <p className="text-sm text-gray-400">No tienes notificaciones</p>
              </div>
            ) : (
              <ul className="divide-y divide-gray-800">
                {notifications.map((notification) => {
                  const pending = notification.invitationStatus === 'pending'
                  const busy = busyId === notification.id
                  return (
                    <li
                      key={notification.id}
                      className={`px-4 py-3 ${notification.read ? '' : 'bg-blue-950/20'}`}
                    >
                      <div className="flex items-start gap-2">
                        <span
                          className={`mt-1.5 h-2 w-2 shrink-0 rounded-full ${
                            notification.read ? 'bg-gray-700' : 'bg-blue-400'
                          }`}
                          aria-hidden="true"
                        />
                        <div className="min-w-0 flex-1">
                          <div className="flex items-start justify-between gap-2">
                            <p className="text-sm font-medium text-gray-100">{notification.title}</p>
                            <span className="shrink-0 text-[11px] text-gray-500">
                              {formatDate(notification.createdAt)}
                            </span>
                          </div>
                          <p className="mt-0.5 text-xs leading-relaxed text-gray-400">
                            {notification.message}
                          </p>

                          <div className="mt-2 flex flex-wrap items-center gap-2">
                            {notification.role && (
                              <span className="rounded-full bg-gray-800 px-2 py-0.5 text-[11px] text-gray-300">
                                {notification.role}
                              </span>
                            )}
                            {notification.invitationStatus === 'accepted' && (
                              <span className="rounded-full bg-green-950 px-2 py-0.5 text-[11px] text-green-300">
                                Aceptada
                              </span>
                            )}
                            {notification.invitationStatus === 'rejected' && (
                              <span className="rounded-full bg-red-950 px-2 py-0.5 text-[11px] text-red-300">
                                Rechazada
                              </span>
                            )}
                            {!notification.read && (
                              <button
                                type="button"
                                onClick={() => markAsRead(notification)}
                                className="flex items-center gap-1 text-[11px] font-medium text-gray-400 hover:text-gray-200 cursor-pointer"
                              >
                                <Check className="h-3 w-3" />
                                Marcar leída
                              </button>
                            )}
                          </div>

                          {pending && (
                            <div className="mt-2 flex gap-2">
                              <button
                                type="button"
                                disabled={busy}
                                onClick={() => respond(notification, 'accept')}
                                className="flex items-center gap-1.5 rounded-lg bg-blue-600 px-3 py-1.5 text-xs font-semibold text-white transition-colors hover:bg-blue-500 disabled:opacity-50 cursor-pointer"
                              >
                                {busy ? <Loader2 className="h-3 w-3 animate-spin" /> : <Check className="h-3 w-3" />}
                                Aceptar
                              </button>
                              <button
                                type="button"
                                disabled={busy}
                                onClick={() => respond(notification, 'reject')}
                                className="flex items-center gap-1.5 rounded-lg border border-gray-700 bg-gray-800 px-3 py-1.5 text-xs font-medium text-gray-300 transition-colors hover:bg-gray-700 disabled:opacity-50 cursor-pointer"
                              >
                                <X className="h-3 w-3" />
                                Rechazar
                              </button>
                            </div>
                          )}
                        </div>
                      </div>
                    </li>
                  )
                })}
              </ul>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
