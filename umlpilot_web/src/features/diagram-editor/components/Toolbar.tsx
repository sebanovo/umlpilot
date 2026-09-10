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
    <div style={{ display: 'flex', gap: '8px', padding: '8px', background: '#f5f5f5', borderBottom: '1px solid #ddd', flexWrap: 'wrap' }}>
      <span style={{ fontWeight: 'bold', fontSize: 12, alignSelf: 'center' }}>Elementos:</span>
      {elementTypes.map((et) => (
        <button key={et.type} onClick={() => handleClick(et.type, false)} style={{ padding: '4px 8px', border: '1px solid #ccc', borderRadius: '4px', cursor: 'pointer', background: 'white', fontSize: 12 }}>
          {et.icon} {et.label}
        </button>
      ))}
      <span style={{ borderLeft: '1px solid #ccc', margin: '0 4px' }} />
      <span style={{ fontWeight: 'bold', fontSize: 12, alignSelf: 'center' }}>Relaciones:</span>
      {relationshipTypes.map((rt) => (
        <button key={rt.type} onClick={() => handleClick(rt.type, true)} style={{ padding: '4px 8px', border: `1px solid ${activeTool === rt.type ? '#0066cc' : '#ccc'}`, borderRadius: '4px', cursor: 'pointer', background: activeTool === rt.type ? '#e6f0ff' : 'white', fontSize: 12 }}>
          {rt.icon} {rt.label}
        </button>
      ))}
    </div>
  )
}
