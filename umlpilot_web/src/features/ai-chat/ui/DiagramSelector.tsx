import type { Diagram } from '../../../shared/api/client'

interface DiagramSelectorProps {
  diagrams: Diagram[]
  value: string | null
  onChange: (diagramId: string | null) => void
}

export function DiagramSelector({ diagrams, value, onChange }: DiagramSelectorProps) {
  return (
    <label className="block">
      <span className="mb-1 block text-[11px] font-medium uppercase tracking-wide text-gray-500">
        Diagrama de contexto
      </span>
      <select
        value={value ?? ''}
        onChange={(event) => onChange(event.target.value || null)}
        className="w-full rounded-lg border border-gray-700 bg-gray-900 px-2.5 py-2 text-sm text-gray-100 outline-none transition-colors focus:border-blue-500 focus:ring-1 focus:ring-blue-500/40"
      >
        <option value="">Sin diagrama (solo consultas)</option>
        {diagrams.map((diagram) => (
          <option key={diagram.id} value={diagram.id}>
            {diagram.name}
          </option>
        ))}
      </select>
    </label>
  )
}
