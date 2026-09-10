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

  if (!element) return <div className="w-72 bg-gray-50 border-l border-gray-200 p-4 text-sm text-gray-400">Selecciona un elemento</div>

  return (
    <div className="w-72 bg-gray-50 border-l border-gray-200 p-4 text-sm overflow-y-auto">
      <h3 className="font-semibold text-gray-900 mb-3">Propiedades</h3>
      <div className="mb-3">
        <label className="block text-xs font-medium text-gray-600 mb-1">Nombre</label>
        <input value={element.name} onChange={(e) => onUpdate({ name: e.target.value })}
          className="w-full rounded border border-gray-300 px-2 py-1 text-gray-900 text-xs focus:border-blue-500 outline-none" />
      </div>
      <div className="mb-3">
        <label className="block text-xs font-medium text-gray-600 mb-1">Visibilidad</label>
        <select value={element.visibility} onChange={(e) => onUpdate({ visibility: e.target.value })}
          className="w-full rounded border border-gray-300 px-2 py-1 text-gray-900 text-xs">
          <option value="public">+</option>
          <option value="private">-</option>
          <option value="protected">#</option>
          <option value="package">~</option>
        </select>
      </div>

      <h4 className="font-medium text-gray-700 mt-4 mb-2">Atributos</h4>
      {attributes.map((a) => (
        <div key={a.id} className="flex justify-between items-center py-1 border-b border-gray-200">
          <span className="text-xs text-gray-700">{a.name}: {a.dataType}</span>
          <button onClick={() => onDeleteAttribute(a.id)} className="text-red-500 hover:text-red-700 text-xs cursor-pointer">&times;</button>
        </div>
      ))}
      <div className="flex gap-1 mt-2">
        <input placeholder="nombre" value={newAttrName} onChange={(e) => setNewAttrName(e.target.value)}
          className="flex-1 rounded border border-gray-300 px-1 py-0.5 text-[11px] outline-none" />
        <input placeholder="tipo" value={newAttrType} onChange={(e) => setNewAttrType(e.target.value)}
          className="flex-1 rounded border border-gray-300 px-1 py-0.5 text-[11px] outline-none" />
        <button onClick={() => { if (newAttrName) { onAddAttribute(newAttrName, newAttrType); setNewAttrName(''); setNewAttrType('String') } }}
          className="rounded bg-blue-600 px-1.5 py-0.5 text-white text-[11px] cursor-pointer">+</button>
      </div>

      <h4 className="font-medium text-gray-700 mt-4 mb-2">Métodos</h4>
      {methods.map((m) => (
        <div key={m.id} className="flex justify-between items-center py-1 border-b border-gray-200">
          <span className="text-xs text-gray-700">{m.name}(): {m.returnType}</span>
          <button onClick={() => onDeleteMethod(m.id)} className="text-red-500 hover:text-red-700 text-xs cursor-pointer">&times;</button>
        </div>
      ))}
      <div className="flex gap-1 mt-2">
        <input placeholder="nombre" value={newMethodName} onChange={(e) => setNewMethodName(e.target.value)}
          className="flex-1 rounded border border-gray-300 px-1 py-0.5 text-[11px] outline-none" />
        <input placeholder="retorno" value={newMethodReturn} onChange={(e) => setNewMethodReturn(e.target.value)}
          className="flex-1 rounded border border-gray-300 px-1 py-0.5 text-[11px] outline-none" />
        <button onClick={() => { if (newMethodName) { onAddMethod(newMethodName, newMethodReturn); setNewMethodName(''); setNewMethodReturn('void') } }}
          className="rounded bg-blue-600 px-1.5 py-0.5 text-white text-[11px] cursor-pointer">+</button>
      </div>
    </div>
  )
}
