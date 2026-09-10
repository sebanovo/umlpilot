import { useState } from 'react'
import type { UmlElement, UmlAttribute, UmlMethod } from '../types/uml'

interface PropertiesPanelProps {
  element: UmlElement | null
  attributes: UmlAttribute[]
  methods: UmlMethod[]
  onUpdate: (data: Partial<UmlElement>) => void
  onAddAttribute: (name: string, dataType: string) => void
  onAddMethod: (name: string, returnType: string) => void
  onDeleteAttribute: (id: string) => void
  onDeleteMethod: (id: string) => void
}

export function PropertiesPanel({ element, attributes, methods, onUpdate, onAddAttribute, onAddMethod, onDeleteAttribute, onDeleteMethod }: PropertiesPanelProps) {
  const [newAttrName, setNewAttrName] = useState('')
  const [newAttrType, setNewAttrType] = useState('String')
  const [newMethodName, setNewMethodName] = useState('')
  const [newMethodReturn, setNewMethodReturn] = useState('void')

  if (!element) return <div style={{ width: 280, background: '#f9f9f9', borderLeft: '1px solid #ddd', padding: '1rem', fontSize: 13, color: '#999' }}>Selecciona un elemento</div>

  return (
    <div style={{ width: 280, background: '#f9f9f9', borderLeft: '1px solid #ddd', padding: '1rem', fontSize: 13, overflowY: 'auto' }}>
      <h3 style={{ margin: '0 0 0.5rem 0' }}>Propiedades</h3>
      <div style={{ marginBottom: '0.5rem' }}>
        <label>Nombre</label>
        <input value={element.name} onChange={(e) => onUpdate({ name: e.target.value })} style={{ width: '100%', padding: '4px', boxSizing: 'border-box' }} />
      </div>
      <div style={{ marginBottom: '0.5rem' }}>
        <label>Visibilidad</label>
        <select value={element.visibility} onChange={(e) => onUpdate({ visibility: e.target.value })} style={{ width: '100%', padding: '4px' }}>
          <option value="public">+</option>
          <option value="private">-</option>
          <option value="protected">#</option>
          <option value="package">~</option>
        </select>
      </div>

      <h4>Atributos</h4>
      {attributes.map((a) => (
        <div key={a.id} style={{ display: 'flex', justifyContent: 'space-between', padding: '2px 0', borderBottom: '1px solid #eee' }}>
          <span>{a.name}: {a.dataType}</span>
          <button onClick={() => onDeleteAttribute(a.id)} style={{ border: 'none', color: 'red', cursor: 'pointer', background: 'none', fontSize: 11 }}>×</button>
        </div>
      ))}
      <div style={{ display: 'flex', gap: '4px', marginTop: '4px' }}>
        <input placeholder="nombre" value={newAttrName} onChange={(e) => setNewAttrName(e.target.value)} style={{ flex: 1, padding: '3px', fontSize: 11 }} />
        <input placeholder="tipo" value={newAttrType} onChange={(e) => setNewAttrType(e.target.value)} style={{ flex: 1, padding: '3px', fontSize: 11 }} />
        <button onClick={() => { if (newAttrName) { onAddAttribute(newAttrName, newAttrType); setNewAttrName(''); setNewAttrType('String') } }} style={{ padding: '3px 6px', fontSize: 11 }}>+</button>
      </div>

      <h4>Métodos</h4>
      {methods.map((m) => (
        <div key={m.id} style={{ display: 'flex', justifyContent: 'space-between', padding: '2px 0', borderBottom: '1px solid #eee' }}>
          <span>{m.name}(): {m.returnType}</span>
          <button onClick={() => onDeleteMethod(m.id)} style={{ border: 'none', color: 'red', cursor: 'pointer', background: 'none', fontSize: 11 }}>×</button>
        </div>
      ))}
      <div style={{ display: 'flex', gap: '4px', marginTop: '4px' }}>
        <input placeholder="nombre" value={newMethodName} onChange={(e) => setNewMethodName(e.target.value)} style={{ flex: 1, padding: '3px', fontSize: 11 }} />
        <input placeholder="retorno" value={newMethodReturn} onChange={(e) => setNewMethodReturn(e.target.value)} style={{ flex: 1, padding: '3px', fontSize: 11 }} />
        <button onClick={() => { if (newMethodName) { onAddMethod(newMethodName, newMethodReturn); setNewMethodName(''); setNewMethodReturn('void') } }} style={{ padding: '3px 6px', fontSize: 11 }}>+</button>
      </div>
    </div>
  )
}
