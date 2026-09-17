import { Users } from 'lucide-react'

export interface PresenceUser {
  id: string
  name: string
  color: string
  isSelf?: boolean
}

function initials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean)
  if (parts.length === 0) return '?'
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase()
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase()
}

/**
 * Barra de presencia que se muestra en la parte superior del lienzo con las
 * personas conectadas al diagrama y el estado de la conexión en tiempo real.
 */
export function PresenceBar({ users, connected }: { users: PresenceUser[]; connected: boolean }) {
  const visible = users.slice(0, 6)
  const extra = users.length - visible.length
  const label = users.length <= 1 ? 'Solo tú' : `${users.length} en línea`

  return (
    <div
      role="region"
      aria-label="Usuarios conectados al diagrama"
      className="flex items-center gap-3 rounded-full border border-gray-700 bg-gray-800/70 px-3 py-1"
    >
      <span className="flex items-center gap-1.5 text-[11px] font-medium text-gray-300">
        <Users className="h-3.5 w-3.5 text-gray-400" aria-hidden="true" />
        {label}
      </span>

      <div className="flex items-center -space-x-2">
        {visible.map((user) => (
          <div
            key={user.id}
            title={user.isSelf ? `${user.name} (tú)` : user.name}
            className="flex h-7 w-7 items-center justify-center rounded-full text-[10px] font-bold text-gray-950 ring-2 ring-gray-800"
            style={{ backgroundColor: user.color }}
          >
            <span className="sr-only">{user.isSelf ? `${user.name} (tú)` : user.name}</span>
            <span aria-hidden="true">{initials(user.name)}</span>
          </div>
        ))}
        {extra > 0 && (
          <div
            className="flex h-7 w-7 items-center justify-center rounded-full bg-gray-700 text-[10px] font-bold text-gray-100 ring-2 ring-gray-800"
            title={`${extra} usuario${extra !== 1 ? 's' : ''} más`}
          >
            +{extra}
          </div>
        )}
      </div>

      <span className="flex items-center gap-1.5" title={connected ? 'Conectado en tiempo real' : 'Sin conexión en tiempo real'}>
        <span className={`h-2 w-2 rounded-full ${connected ? 'bg-green-500' : 'bg-red-500'}`} aria-hidden="true" />
        <span className="sr-only">{connected ? 'Conectado en tiempo real' : 'Sin conexión en tiempo real'}</span>
      </span>
    </div>
  )
}
