import { useState } from 'react'
import { Square, Diamond, Triangle, StickyNote, ArrowRight, GitBranch, Link2, Circle } from 'lucide-react'

interface ToolbarProps {
  onAddElement: (type: string) => void
  onAddRelationship: (type: string) => void
}

const elementTypes = [
  { type: 'class', label: 'Clase', icon: Square },
  { type: 'interface', label: 'Interfaz', icon: Diamond },
  { type: 'enumeration', label: 'Enum', icon: Triangle },
  { type: 'note', label: 'Nota', icon: StickyNote },
]

const relationshipTypes = [
  { type: 'association', label: 'Asociación', icon: ArrowRight },
  { type: 'generalization', label: 'Herencia', icon: GitBranch },
  { type: 'dependency', label: 'Dependencia', icon: Link2 },
  { type: 'aggregation', label: 'Agregación', icon: Circle },
  { type: 'composition', label: 'Composición', icon: Circle },
]

export function Toolbar({ onAddElement, onAddRelationship }: ToolbarProps) {
  const [activeTool, setActiveTool] = useState<string | null>(null)

  const handleClick = (type: string, isRelationship: boolean) => {
    if (isRelationship) {
      setActiveTool(activeTool === type ? null : type)
      onAddRelationship(type)
    } else {
      setActiveTool(null)
      onAddElement(type)
    }
  }

  return (
    <div className="flex items-center gap-1 px-3 py-2 bg-white border-b border-gray-200 text-xs">
      <span className="text-[11px] font-semibold text-gray-400 uppercase tracking-wider mr-2">Elementos</span>
      {elementTypes.map((et) => {
        const Icon = et.icon
        return (
          <button key={et.type} onClick={() => handleClick(et.type, false)}
            className="flex items-center gap-1.5 rounded-md border border-gray-200 bg-white px-2.5 py-1.5 text-gray-600 hover:bg-gray-50 hover:border-gray-300 hover:text-gray-900 transition-all cursor-pointer">
            <Icon className="w-3.5 h-3.5" />
            {et.label}
          </button>
        )
      })}
      <div className="w-px h-5 bg-gray-200 mx-2" />
      <span className="text-[11px] font-semibold text-gray-400 uppercase tracking-wider mr-2">Relaciones</span>
      {relationshipTypes.map((rt) => {
        const Icon = rt.icon
        return (
          <button key={rt.type} onClick={() => handleClick(rt.type, true)}
            className={`flex items-center gap-1.5 rounded-md border px-2.5 py-1.5 transition-all cursor-pointer ${
              activeTool === rt.type
                ? 'border-blue-500 bg-blue-50 text-blue-700 shadow-sm'
                : 'border-gray-200 bg-white text-gray-600 hover:bg-gray-50 hover:border-gray-300 hover:text-gray-900'
            }`}>
            <Icon className="w-3.5 h-3.5" />
            {rt.label}
          </button>
        )
      })}
    </div>
  )
}
