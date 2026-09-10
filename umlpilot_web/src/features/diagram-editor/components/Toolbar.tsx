import { useState } from 'react'

interface ToolbarProps {
  onAddElement: (type: string) => void
  onAddRelationship: (type: string) => void
}

const elementTypes = [
  { type: 'class', label: 'Clase', icon: '□' },
  { type: 'interface', label: 'Interfaz', icon: '◇' },
  { type: 'enumeration', label: 'Enum', icon: '▽' },
  { type: 'note', label: 'Nota', icon: '📝' },
]

const relationshipTypes = [
  { type: 'association', label: 'Asociación', icon: '—>' },
  { type: 'generalization', label: 'Herencia', icon: '—▷' },
  { type: 'dependency', label: 'Dependencia', icon: '- ->' },
  { type: 'aggregation', label: 'Agregación', icon: '—◇' },
  { type: 'composition', label: 'Composición', icon: '—◆' },
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
    <div className="flex items-center gap-2 px-3 py-2 bg-gray-50 border-b border-gray-200 text-xs flex-wrap">
      <span className="font-semibold text-gray-600">Elementos:</span>
      {elementTypes.map((et) => (
        <button key={et.type} onClick={() => handleClick(et.type, false)}
          className="rounded-md border border-gray-300 bg-white px-2 py-1 hover:bg-gray-100 cursor-pointer transition-colors">
          {et.icon} {et.label}
        </button>
      ))}
      <span className="w-px h-4 bg-gray-300 mx-1" />
      <span className="font-semibold text-gray-600">Relaciones:</span>
      {relationshipTypes.map((rt) => (
        <button key={rt.type} onClick={() => handleClick(rt.type, true)}
          className={`rounded-md border px-2 py-1 cursor-pointer transition-colors ${activeTool === rt.type ? 'border-blue-600 bg-blue-50 text-blue-700' : 'border-gray-300 bg-white hover:bg-gray-100'}`}>
          {rt.icon} {rt.label}
        </button>
      ))}
    </div>
  )
}
