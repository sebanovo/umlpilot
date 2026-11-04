import { useEffect, useRef, useState } from 'react'
import { Loader2, Mail, Search, UserPlus } from 'lucide-react'
import {
  projectsApi,
  usersApi,
  type Collaborator,
  type UserSearchResult,
} from '../../../shared/api/client'
import { useToast } from '../../../shared/ui/toast/useToast'

interface InviteCollaboratorFormProps {
  projectId: string
  onInvited: (collaborator: Collaborator) => void
}

const SEARCH_DEBOUNCE_MS = 300
const MIN_QUERY_LENGTH = 2

/** Formulario para buscar un usuario por email/nombre e invitarlo a un proyecto. */
export function InviteCollaboratorForm({ projectId, onInvited }: InviteCollaboratorFormProps) {
  const { toast } = useToast()
  const [email, setEmail] = useState('')
  const [role, setRole] = useState<'editor' | 'viewer'>('editor')
  const [results, setResults] = useState<UserSearchResult[]>([])
  const [showResults, setShowResults] = useState(false)
  const [searching, setSearching] = useState(false)
  const [inviting, setInviting] = useState(false)
  const containerRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const query = email.trim()
    if (query.length < MIN_QUERY_LENGTH) {
      setResults([])
      setSearching(false)
      return
    }

    let active = true
    const timer = window.setTimeout(async () => {
      setSearching(true)
      try {
        const found = await usersApi.search(query)
        if (active) {
          setResults(found)
          setShowResults(true)
        }
      } catch {
        if (active) setResults([])
      } finally {
        if (active) setSearching(false)
      }
    }, SEARCH_DEBOUNCE_MS)

    return () => {
      active = false
      window.clearTimeout(timer)
    }
  }, [email])

  useEffect(() => {
    const onPointerDown = (event: MouseEvent) => {
      if (!containerRef.current?.contains(event.target as Node)) setShowResults(false)
    }
    document.addEventListener('mousedown', onPointerDown)
    return () => document.removeEventListener('mousedown', onPointerDown)
  }, [])

  const invite = async (targetEmail: string) => {
    const value = targetEmail.trim()
    if (!value) return
    setInviting(true)
    try {
      const collaborator = await projectsApi.inviteCollaborator(projectId, { email: value, role })
      onInvited(collaborator)
      setEmail('')
      setResults([])
      setShowResults(false)
      toast({
        title: 'Invitación enviada',
        description: `La propuesta llegó a la bandeja de ${collaborator.firstName} ${collaborator.lastName}`,
        variant: 'success',
      })
    } catch (err) {
      toast({
        title: 'No se pudo invitar',
        description: err instanceof Error ? err.message : 'Error inesperado',
        variant: 'error',
      })
    } finally {
      setInviting(false)
    }
  }

  return (
    <form
      onSubmit={(event) => {
        event.preventDefault()
        void invite(email)
      }}
      className="rounded-xl border border-gray-800 bg-gray-900 p-4"
    >
      <p className="mb-2 flex items-center gap-1.5 text-sm font-medium text-gray-200">
        <UserPlus className="h-4 w-4 text-blue-400" />
        Invitar por email
      </p>

      <div className="relative" ref={containerRef}>
        <div className="relative">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-gray-500" />
          <input
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            onFocus={() => { if (results.length > 0) setShowResults(true) }}
            placeholder="nombre@correo.com"
            aria-label="Email del usuario a invitar"
            className="w-full rounded-lg border border-gray-700 bg-gray-800 py-2 pl-9 pr-3 text-sm text-gray-100 placeholder-gray-500 outline-none transition-colors focus:border-blue-500 focus:ring-2 focus:ring-blue-500/40"
          />
          {searching && (
            <Loader2 className="absolute right-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 animate-spin text-gray-500" />
          )}
        </div>

        {showResults && results.length > 0 && (
          <ul className="absolute left-0 right-0 top-full z-20 mt-1 max-h-52 overflow-y-auto rounded-lg border border-gray-700 bg-gray-900 py-1 shadow-xl shadow-black/50">
            {results.map((user) => (
              <li key={user.id}>
                <button
                  type="button"
                  onClick={() => {
                    setEmail(user.email)
                    setShowResults(false)
                  }}
                  className="flex w-full items-center gap-2 px-3 py-2 text-left transition-colors hover:bg-gray-800 cursor-pointer"
                >
                  <Mail className="h-3.5 w-3.5 shrink-0 text-gray-500" />
                  <span className="min-w-0">
                    <span className="block truncate text-sm text-gray-100">
                      {user.firstName} {user.lastName}
                    </span>
                    <span className="block truncate text-xs text-gray-500">{user.email}</span>
                  </span>
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>

      <div className="mt-2 flex gap-2">
        <select
          value={role}
          onChange={(event) => setRole(event.target.value as 'editor' | 'viewer')}
          aria-label="Rol del invitado"
          className="w-28 rounded-lg border border-gray-700 bg-gray-800 px-2 py-2 text-sm text-gray-100 outline-none transition-colors focus:border-blue-500 focus:ring-2 focus:ring-blue-500/40"
        >
          <option value="editor">Editor</option>
          <option value="viewer">Lector</option>
        </select>
        <button
          type="submit"
          disabled={inviting || email.trim().length === 0}
          className="flex flex-1 items-center justify-center gap-1.5 rounded-lg bg-blue-600 px-3 py-2 text-sm font-medium text-white transition-colors hover:bg-blue-500 disabled:opacity-50 cursor-pointer"
        >
          {inviting ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : <UserPlus className="h-3.5 w-3.5" />}
          Invitar
        </button>
      </div>

      <p className="mt-2 text-[11px] leading-relaxed text-gray-500">
        El invitado debe tener cuenta en UMLPilot. Recibirá la propuesta en su bandeja de notificaciones.
      </p>
    </form>
  )
}
