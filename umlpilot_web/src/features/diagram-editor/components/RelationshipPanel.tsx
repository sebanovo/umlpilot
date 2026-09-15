import { useEffect, useState } from 'react'
import type { UmlRelationshipApi, MultiplicityApi } from '../../../shared/api/client'
import { Check } from 'lucide-react'

interface RelationshipPanelProps {
  relationship: UmlRelationshipApi | null
  multiplicities: MultiplicityApi[]
  onUpdateName: (name: string) => void
  onUpdateMultiplicity: (multiplicityId: string, min: number, max: number) => void
  width?: number
}

const TYPE_LABELS: Record<string, string> = {
  association: 'Associate',
  generalization: 'Generalize',
  composition: 'Compose',
  aggregation: 'Aggregate',
  association_class: 'Association Class',
  realization: 'Realize',
  template_binding: 'Template Binding',
  dependency: 'Dependency',
}

const PRESETS = [
  { label: '1', min: 1, max: 1 },
  { label: '0..1', min: 0, max: 1 },
  { label: '1..*', min: 1, max: -1 },
  { label: '0..*', min: 0, max: -1 },
]

function multLabel(min: number | null, max: number | null): string {
  if (min == null || max == null) return ''
  if (min === max && max >= 0) return String(min)
  return `${min}..${max < 0 ? '*' : max}`
}

export function RelationshipPanel({ relationship, multiplicities, onUpdateName, onUpdateMultiplicity, width }: RelationshipPanelProps) {
  const [name, setName] = useState('')

  useEffect(() => {
    setName(relationship?.name || '')
  }, [relationship?.id, relationship?.name])

  if (!relationship) return null

  const source = multiplicities.find((m) => m.end === 'source')
  const target = multiplicities.find((m) => m.end === 'target')

  const renderMultiplicity = (label: string, mult?: MultiplicityApi) => {
    if (!mult) return null
    const current = multLabel(mult.min, mult.max)
    const options = PRESETS.some((p) => p.label === current)
      ? PRESETS
      : [{ label: current, min: mult.min ?? 0, max: mult.max ?? 0 }, ...PRESETS]
    return (
      <div>
        <label className="block text-xs font-medium text-gray-400 mb-1">{label}</label>
        <select value={current}
          onChange={(e) => {
            const p = options.find((o) => o.label === e.target.value)
            if (p) onUpdateMultiplicity(mult.id, p.min, p.max)
          }}
          className="w-full rounded-md border border-gray-700 bg-gray-800 px-2.5 py-1.5 text-sm text-gray-100 focus:border-blue-500 focus:ring-1 focus:ring-blue-500/40 outline-none">
          {options.map((o) => <option key={o.label} value={o.label}>{o.label}</option>)}
        </select>
      </div>
    )
  }

  return (
    <div className="shrink-0 min-h-0 bg-gray-900 border-l border-gray-800 flex flex-col overflow-hidden" style={width ? { width } : undefined}>
      <div className="px-4 py-3 border-b border-gray-800">
        <h3 className="text-sm font-semibold text-gray-100">Relación</h3>
        <p className="text-xs text-blue-400 mt-0.5">{TYPE_LABELS[relationship.relationshipType] || relationship.relationshipType}</p>
      </div>

      <div className="flex-1 min-h-0 overflow-y-auto p-4 space-y-4">
        <div>
          <label className="block text-xs font-medium text-gray-400 mb-1">Nombre / texto</label>
          <div className="flex gap-2">
            <input value={name} onChange={(e) => setName(e.target.value)}
              onKeyDown={(e) => { if (e.key === 'Enter') onUpdateName(name) }}
              placeholder="p. ej. contiene"
              className="flex-1 rounded-md border border-gray-700 bg-gray-800 px-2.5 py-1.5 text-sm text-gray-100 placeholder-gray-500 focus:border-blue-500 focus:ring-1 focus:ring-blue-500/40 outline-none" />
            <button onClick={() => onUpdateName(name)} title="Guardar nombre"
              className="rounded-md bg-blue-600 px-2.5 text-white hover:bg-blue-500 cursor-pointer transition-colors">
              <Check className="w-4 h-4" />
            </button>
          </div>
        </div>

        <div className="border-t border-gray-800 pt-3 space-y-3">
          {renderMultiplicity('Multiplicidad origen', source)}
          {renderMultiplicity('Multiplicidad destino', target)}
          {!source && !target && (
            <p className="text-xs text-gray-500">Sin multiplicidades registradas.</p>
          )}
        </div>
      </div>
    </div>
  )
}
