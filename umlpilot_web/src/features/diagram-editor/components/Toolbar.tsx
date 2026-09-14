import { Square, Diamond, Triangle, StickyNote, ArrowRight, GitBranch, Link2, Circle } from 'lucide-react'

interface ToolbarProps {
  onAddElement: (type: string) => void
  activeRelationship: string | null
  onSelectRelationship: (type: string | null) => void
}

const elementTypes = [
  { type: 'class', label: 'Clase', icon: Square },
  { type: 'interface', label: 'Interfaz', icon: Diamond },
  { type: 'enumeration', label: 'Enum', icon: Triangle },
  { type: 'note', label: 'Nota', icon: StickyNote },
]

const relationshipTypes = [
  { type: 'association', label: 'Associate', icon: ArrowRight },
  { type: 'generalization', label: 'Generalize', icon: GitBranch },
  { type: 'composition', label: 'Compose', icon: Diamond },
  { type: 'aggregation', label: 'Aggregate', icon: Circle },
  { type: 'association_class', label: 'Association Class', icon: Square },
  { type: 'realization', label: 'Realize', icon: Triangle },
  { type: 'template_binding', label: 'Template Binding', icon: Link2 },
  { type: 'dependency', label: 'Dependency', icon: Link2 },
]

export function Toolbar({ onAddElement, activeRelationship, onSelectRelationship }: ToolbarProps) {
  return (
    <div className="flex flex-wrap items-center gap-1 px-3 py-2 bg-gray-900 border-b border-gray-800 text-xs">
      <span className="text-[11px] font-semibold text-gray-500 uppercase tracking-wider mr-2">Elementos</span>
      {elementTypes.map((et) => {
        const Icon = et.icon
        return (
          <button key={et.type} onClick={() => onAddElement(et.type)}
            className="flex items-center gap-1.5 rounded-md border border-gray-700 bg-gray-800 px-2.5 py-1.5 text-gray-300 hover:bg-gray-700 hover:border-gray-600 hover:text-gray-100 transition-all cursor-pointer">
            <Icon className="w-3.5 h-3.5" />
            {et.label}
          </button>
        )
      })}
      <div className="w-px h-5 bg-gray-700 mx-2" />
      <span className="text-[11px] font-semibold text-gray-500 uppercase tracking-wider mr-2">Relaciones</span>
      {relationshipTypes.map((rt) => {
        const Icon = rt.icon
        const active = activeRelationship === rt.type
        return (
          <button key={rt.type} onClick={() => onSelectRelationship(active ? null : rt.type)}
            title={active ? 'Clic para desactivar' : 'Selecciona y arrastra entre dos clases'}
            className={`flex items-center gap-1.5 rounded-md border px-2.5 py-1.5 transition-all cursor-pointer ${
              active
                ? 'border-blue-500 bg-blue-950 text-blue-300 shadow-sm'
                : 'border-gray-700 bg-gray-800 text-gray-300 hover:bg-gray-700 hover:border-gray-600 hover:text-gray-100'
            }`}>
            <Icon className="w-3.5 h-3.5" />
            {rt.label}
          </button>
        )
      })}
    </div>
  )
}
