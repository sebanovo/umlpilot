import type { Collaborator } from '../../../shared/api/client'
import { colorFor } from '../../../shared/lib/useWebSocket'

interface ParticipantsPanelProps {
  collaborators: Collaborator[]
  presence: string[]
  currentUserId: string
}

export function ParticipantsPanel({ collaborators, presence, currentUserId }: ParticipantsPanelProps) {
  return (
    <div>
      <p className="mb-2 text-[11px] font-medium uppercase tracking-wide text-gray-500">
        Participantes
      </p>
      <div className="space-y-1.5">
        {collaborators.map((collaborator) => {
          const online = presence.includes(collaborator.userId) || collaborator.userId === currentUserId
          const name = `${collaborator.firstName ?? ''} ${collaborator.lastName ?? ''}`.trim()
            || collaborator.email
          return (
            <div key={collaborator.userId} className="flex items-center gap-2 rounded-lg px-1.5 py-1">
              <span
                className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full text-[10px] font-semibold text-gray-950"
                style={{ backgroundColor: colorFor(collaborator.userId) }}
              >
                {(name[0] || '?').toUpperCase()}
              </span>
              <div className="min-w-0 flex-1">
                <p className="truncate text-xs text-gray-200">{name}</p>
                <p className="text-[10px] uppercase tracking-wide text-gray-500">{collaborator.role}</p>
              </div>
              <span
                className={`h-2 w-2 shrink-0 rounded-full ${online ? 'bg-emerald-400' : 'bg-gray-600'}`}
                title={online ? 'Conectado' : 'Desconectado'}
              />
            </div>
          )
        })}
      </div>
    </div>
  )
}
