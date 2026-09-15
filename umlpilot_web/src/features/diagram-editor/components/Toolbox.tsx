const SPRITE_URL = '/assets/icons-vector-elements.svg'

interface SpriteIconProps {
  id: string
  viewBox: string
  className?: string
}

function SpriteIcon({ id, viewBox, className }: SpriteIconProps) {
  return (
    <svg className={className} viewBox={viewBox} preserveAspectRatio="xMidYMid meet" aria-hidden="true">
      <use href={`${SPRITE_URL}#${id}`} />
    </svg>
  )
}

const ELEMENT_ICONS: Record<string, { id: string; viewBox: string }> = {
  class: { id: 'Class', viewBox: '33 18 14 13' },
  interface: { id: 'Interface', viewBox: '47 19 17 12' },
  enumeration: { id: 'Enumeration', viewBox: '66.5 20 11 9.5' },
  note: { id: 'Note', viewBox: '33 2 15 12' },
}

const RELATIONSHIP_ICONS: Record<string, { id: string; viewBox: string }> = {
  association: { id: 'Association', viewBox: '80.5 81 13 13.5' },
  generalization: { id: 'Generalization', viewBox: '112 80.5 16 14' },
  composition: { id: 'Composition', viewBox: '32.5 177 14 14' },
  aggregation: { id: 'Aggregation', viewBox: '16.5 177 14 14' },
  association_class: { id: 'AssociationClass', viewBox: '96 80.5 16 15' },
  realization: { id: 'Realization', viewBox: '96 96.5 15.5 14' },
  template_binding: { id: 'Dependency', viewBox: '65 80.5 15 14' },
  dependency: { id: 'Dependency', viewBox: '65 80.5 15 14' },
}

const elementTypes = [
  { type: 'class', label: 'Clase' },
  { type: 'interface', label: 'Interfaz' },
  { type: 'enumeration', label: 'Enum' },
  { type: 'note', label: 'Nota' },
]

const relationshipTypes = [
  { type: 'association', label: 'Associate' },
  { type: 'generalization', label: 'Generalize' },
  { type: 'composition', label: 'Compose' },
  { type: 'aggregation', label: 'Aggregate' },
  { type: 'association_class', label: 'Asoc. Class' },
  { type: 'realization', label: 'Realize' },
  { type: 'template_binding', label: 'Template' },
  { type: 'dependency', label: 'Dependency' },
]

interface ToolboxProps {
  onAddElement: (type: string) => void
  activeRelationship: string | null
  onSelectRelationship: (type: string | null) => void
}

export function Toolbox({ onAddElement, activeRelationship, onSelectRelationship }: ToolboxProps) {
  return (
    <aside className="w-44 shrink-0 bg-gray-900 border-r border-gray-800 flex flex-col overflow-y-auto">
      <div className="px-3 py-2 border-b border-gray-800">
        <h3 className="text-[11px] font-semibold text-gray-500 uppercase tracking-wider">Elementos</h3>
      </div>
      <div className="grid grid-cols-2 gap-1 p-2">
        {elementTypes.map((et) => {
          const icon = ELEMENT_ICONS[et.type]
          return (
            <button key={et.type} onClick={() => onAddElement(et.type)} title={et.label}
              className="flex flex-col items-center gap-1 rounded-md border border-gray-700 bg-gray-800 px-1 py-2 text-[10px] text-gray-300 hover:bg-gray-700 hover:border-gray-600 hover:text-gray-100 transition-colors cursor-pointer">
              {icon && <SpriteIcon id={icon.id} viewBox={icon.viewBox} className="w-6 h-6" />}
              {et.label}
            </button>
          )
        })}
      </div>

      <div className="px-3 py-2 border-y border-gray-800">
        <h3 className="text-[11px] font-semibold text-gray-500 uppercase tracking-wider">Relaciones</h3>
      </div>
      <div className="grid grid-cols-2 gap-1 p-2">
        {relationshipTypes.map((rt) => {
          const icon = RELATIONSHIP_ICONS[rt.type]
          const active = activeRelationship === rt.type
          return (
            <button key={rt.type} onClick={() => onSelectRelationship(active ? null : rt.type)}
              title={active ? 'Clic para desactivar' : 'Selecciona y conecta dos clases'}
              className={`flex flex-col items-center gap-1 rounded-md border px-1 py-2 text-[10px] transition-colors cursor-pointer ${
                active
                  ? 'border-blue-500 bg-blue-950 text-blue-300'
                  : 'border-gray-700 bg-gray-800 text-gray-300 hover:bg-gray-700 hover:border-gray-600 hover:text-gray-100'
              }`}>
              {icon && <SpriteIcon id={icon.id} viewBox={icon.viewBox} className="w-6 h-6" />}
              {rt.label}
            </button>
          )
        })}
      </div>

      <div className="mt-auto p-3 border-t border-gray-800 text-[10px] leading-relaxed text-gray-500">
        Pasa el cursor sobre un elemento para <span className="text-gray-300">editar</span> o <span className="text-gray-300">eliminar</span>.
      </div>
    </aside>
  )
}
