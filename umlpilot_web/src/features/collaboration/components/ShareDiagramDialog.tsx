import { useEffect, useState } from 'react'
import { Check, Copy, Globe, Loader2, Lock, X } from 'lucide-react'
import { diagramShareApi, type SharedLinkRole, type ShareLinkState } from '../../../shared/api/client'
import { useToast } from '../../../shared/ui/toast/useToast'
import { InviteCollaboratorForm } from './InviteCollaboratorForm'

interface ShareDiagramDialogProps {
  projectId: string
  diagramId: string
  open: boolean
  onClose: () => void
}

/** Compartir diagrama: enlace público de solo lectura o invitación por email. */
export function ShareDiagramDialog({ projectId, diagramId, open, onClose }: ShareDiagramDialogProps) {
  const { toast } = useToast()
  const [status, setStatus] = useState<ShareLinkState | null>(null)
  const [loading, setLoading] = useState(false)
  const [busy, setBusy] = useState(false)
  const [copied, setCopied] = useState(false)

  useEffect(() => {
    if (!open) return
    setLoading(true)
    setCopied(false)
    diagramShareApi.get(projectId, diagramId)
      .then(setStatus)
      .catch((err) => {
        toast({
          title: 'No se pudo cargar el enlace',
          description: err instanceof Error ? err.message : 'Error inesperado',
          variant: 'error',
        })
      })
      .finally(() => setLoading(false))
  }, [open, projectId, diagramId, toast])

  useEffect(() => {
    if (!open) return
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && !busy) onClose()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [open, busy, onClose])

  if (!open) return null

  const absoluteUrl = status?.url ? `${window.location.origin}${status.url}` : ''

  const enable = async () => {
    setBusy(true)
    try {
      setStatus(await diagramShareApi.enable(projectId, diagramId, status?.role ?? 'viewer'))
      toast({ title: 'Enlace público activado', variant: 'success' })
    } catch (err) {
      toast({
        title: 'No se pudo activar el enlace',
        description: err instanceof Error ? err.message : 'Error inesperado',
        variant: 'error',
      })
    } finally {
      setBusy(false)
    }
  }

  const disable = async () => {
    setBusy(true)
    try {
      await diagramShareApi.disable(projectId, diagramId)
      setStatus((current) => ({
        enabled: false,
        token: null,
        url: null,
        role: current?.role ?? 'viewer',
      }))
      toast({ title: 'Enlace público desactivado', variant: 'info' })
    } catch (err) {
      toast({
        title: 'No se pudo desactivar el enlace',
        description: err instanceof Error ? err.message : 'Error inesperado',
        variant: 'error',
      })
    } finally {
      setBusy(false)
    }
  }

  const changeRole = async (next: SharedLinkRole) => {
    setStatus((current) => (current ? { ...current, role: next } : current))
    if (!status?.enabled) return
    setBusy(true)
    try {
      setStatus(await diagramShareApi.enable(projectId, diagramId, next))
    } catch (err) {
      toast({
        title: 'No se pudo cambiar el permiso del enlace',
        description: err instanceof Error ? err.message : 'Error inesperado',
        variant: 'error',
      })
      diagramShareApi.get(projectId, diagramId).then(setStatus).catch(() => {})
    } finally {
      setBusy(false)
    }
  }

  const copy = async () => {
    if (!absoluteUrl) return
    try {
      await navigator.clipboard.writeText(absoluteUrl)
      setCopied(true)
      toast({ title: 'Enlace copiado', variant: 'success' })
      window.setTimeout(() => setCopied(false), 2000)
    } catch {
      toast({ title: 'No se pudo copiar', description: absoluteUrl, variant: 'warning' })
    }
  }

  return (
    <div className="fixed inset-0 z-[80] flex items-center justify-center p-4">
      <div
        className="absolute inset-0 bg-black/60 backdrop-blur-sm"
        onClick={() => { if (!busy) onClose() }}
        aria-hidden="true"
      />
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="share-dialog-title"
        className="relative w-full max-w-md overflow-hidden rounded-xl border border-gray-800 bg-gray-900 shadow-2xl shadow-black/60"
      >
        <div className="flex items-start justify-between border-b border-gray-800 px-5 py-4">
          <div>
            <h2 id="share-dialog-title" className="text-base font-semibold text-gray-100">Compartir diagrama</h2>
            <p className="mt-0.5 text-xs text-gray-500">Invita a tu equipo o comparte un enlace de solo lectura.</p>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Cerrar"
            className="rounded-lg p-1 text-gray-400 hover:bg-gray-800 hover:text-gray-200 cursor-pointer"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        <div className="max-h-[70vh] space-y-5 overflow-y-auto px-5 py-4">
          <section>
            <div className="flex items-center justify-between gap-3">
              <div className="flex items-center gap-2">
                {status?.enabled ? <Globe className="h-4 w-4 text-green-400" /> : <Lock className="h-4 w-4 text-gray-500" />}
                <div>
                  <p className="text-sm font-medium text-gray-100">Cualquiera con el enlace</p>
                  <p className="text-xs text-gray-500">
                    {status?.enabled
                      ? 'Puede previsualizarlo e unirse al proyecto. Sin invitación.'
                      : 'El enlace público está desactivado.'}
                  </p>
                </div>
              </div>
              {loading ? (
                <Loader2 className="h-4 w-4 animate-spin text-gray-500" />
              ) : status?.enabled ? (
                <button
                  type="button"
                  onClick={disable}
                  disabled={busy}
                  className="shrink-0 rounded-lg border border-gray-700 bg-gray-800 px-3 py-1.5 text-xs text-gray-300 hover:bg-gray-700 disabled:opacity-50 cursor-pointer"
                >
                  Desactivar
                </button>
              ) : (
                <button
                  type="button"
                  onClick={enable}
                  disabled={busy || loading}
                  className="shrink-0 rounded-lg bg-blue-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-blue-500 disabled:opacity-50 cursor-pointer"
                >
                  Activar enlace
                </button>
              )}
            </div>

            <div className="mt-3 flex items-center gap-2">
              <label htmlFor="share-role" className="text-xs text-gray-400">Al unirse, podrá</label>
              <select
                id="share-role"
                value={status?.role ?? 'viewer'}
                onChange={(event) => changeRole(event.target.value as SharedLinkRole)}
                disabled={busy || loading}
                className="rounded-lg border border-gray-700 bg-gray-800 px-2 py-1 text-xs text-gray-100 outline-none focus:border-blue-500 disabled:opacity-50 cursor-pointer"
              >
                <option value="viewer">Ver (lector)</option>
                <option value="editor">Editar</option>
              </select>
            </div>

            {status?.enabled && absoluteUrl && (
              <div className="mt-3 flex gap-2">
                <input
                  readOnly
                  value={absoluteUrl}
                  aria-label="Enlace público"
                  className="min-w-0 flex-1 rounded-lg border border-gray-700 bg-gray-800 px-3 py-2 text-xs text-gray-300 outline-none"
                />
                <button
                  type="button"
                  onClick={copy}
                  className="flex shrink-0 items-center gap-1.5 rounded-lg bg-gray-800 px-3 py-2 text-xs font-medium text-gray-200 hover:bg-gray-700 cursor-pointer"
                >
                  {copied ? <Check className="h-3.5 w-3.5 text-green-400" /> : <Copy className="h-3.5 w-3.5" />}
                  {copied ? 'Copiado' : 'Copiar'}
                </button>
              </div>
            )}
          </section>

          <div className="border-t border-gray-800 pt-4">
            <InviteCollaboratorForm projectId={projectId} onInvited={() => {}} />
          </div>
        </div>
      </div>
    </div>
  )
}
