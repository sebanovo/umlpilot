import { useState } from 'react'
import type { UmlElement, UmlAttribute, UmlMethod } from '../types/uml'
import { Plus, X, ChevronDown, ChevronRight, Eye, EyeOff, Hash, AtSign } from 'lucide-react'

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

const visOptions = [
  { value: 'public', label: '+ Público', icon: Eye },
  { value: 'private', label: '- Privado', icon: EyeOff },
  { value: 'protected', label: '# Protegido', icon: Hash },
  { value: 'package', label: '~ Paquete', icon: AtSign },
]

export function PropertiesPanel({ element, attributes, methods, onUpdate, onAddAttribute, onAddMethod, onDeleteAttribute, onDeleteMethod }: PropertiesPanelProps) {
  const [newAttrName, setNewAttrName] = useState('')
  const [newAttrType, setNewAttrType] = useState('String')
  const [newMethodName, setNewMethodName] = useState('')
  const [newMethodReturn, setNewMethodReturn] = useState('void')
  const [showAttrs, setShowAttrs] = useState(true)
  const [showMethods, setShowMethods] = useState(true)

  if (!element) {
    return (
      <div className="w-72 bg-gray-50 border-l border-gray-200 flex flex-col items-center justify-center text-gray-400 text-sm">
        <svg className="w-10 h-10 mb-3 text-gray-300" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
          <rect x="3" y="3" width="18" height="18" rx="2" />
          <line x1="3" y1="9" x2="21" y2="9" />
          <line x1="9" y1="21" x2="9" y2="9" />
        </svg>
        <p>Selecciona un elemento</p>
        <p className="text-xs text-gray-300 mt-1">para ver sus propiedades</p>
      </div>
    )
  }

  return (
    <div className="w-72 bg-white border-l border-gray-200 flex flex-col overflow-hidden">
      <div className="px-4 py-3 border-b border-gray-200 bg-gray-50">
        <h3 className="text-sm font-semibold text-gray-900">Propiedades</h3>
      </div>

      <div className="flex-1 overflow-y-auto p-4 space-y-4">
        <div>
          <label className="block text-xs font-medium text-gray-500 mb-1">Nombre</label>
          <input value={element.name} onChange={(e) => onUpdate({ name: e.target.value })}
            className="w-full rounded-md border border-gray-300 px-2.5 py-1.5 text-sm text-gray-900 focus:border-blue-500 focus:ring-1 focus:ring-blue-500 outline-none transition-colors" />
        </div>
        <div>
          <label className="block text-xs font-medium text-gray-500 mb-1">Visibilidad</label>
          <select value={element.visibility} onChange={(e) => onUpdate({ visibility: e.target.value })}
            className="w-full rounded-md border border-gray-300 px-2.5 py-1.5 text-sm text-gray-900 focus:border-blue-500 focus:ring-1 focus:ring-blue-500 outline-none">
            {visOptions.map((v) => (
              <option key={v.value} value={v.value}>{v.label}</option>
            ))}
          </select>
        </div>

        <div className="border-t border-gray-100 pt-3">
          <button onClick={() => setShowAttrs(!showAttrs)}
            className="flex items-center gap-1.5 text-xs font-semibold text-gray-700 mb-2 hover:text-gray-900 cursor-pointer w-full">
            {showAttrs ? <ChevronDown className="w-3.5 h-3.5" /> : <ChevronRight className="w-3.5 h-3.5" />}
            Atributos ({attributes.length})
          </button>
          {showAttrs && (
            <div className="space-y-1">
              {attributes.map((a) => (
                <div key={a.id} className="flex items-center justify-between py-1 px-2 rounded hover:bg-gray-50 group">
                  <span className="text-xs text-gray-600 font-mono truncate">
                    {a.visibility === 'private' ? '-' : a.visibility === 'protected' ? '#' : '+'} {a.name}: {a.dataType}
                  </span>
                  <button onClick={() => onDeleteAttribute(a.id)}
                    className="opacity-0 group-hover:opacity-100 text-gray-400 hover:text-red-500 transition-opacity cursor-pointer">
                    <X className="w-3 h-3" />
                  </button>
                </div>
              ))}
              <div className="flex gap-1 mt-2">
                <input placeholder="nombre" value={newAttrName} onChange={(e) => setNewAttrName(e.target.value)}
                  className="flex-1 rounded border border-gray-200 px-1.5 py-1 text-[11px] focus:border-blue-400 outline-none" />
                <input placeholder="tipo" value={newAttrType} onChange={(e) => setNewAttrType(e.target.value)}
                  className="flex-1 rounded border border-gray-200 px-1.5 py-1 text-[11px] focus:border-blue-400 outline-none" />
                <button onClick={() => { if (newAttrName) { onAddAttribute(newAttrName, newAttrType); setNewAttrName(''); setNewAttrType('String') } }}
                  className="rounded bg-blue-600 p-1 text-white hover:bg-blue-700 cursor-pointer">
                  <Plus className="w-3 h-3" />
                </button>
              </div>
            </div>
          )}
        </div>

        <div className="border-t border-gray-100 pt-3">
          <button onClick={() => setShowMethods(!showMethods)}
            className="flex items-center gap-1.5 text-xs font-semibold text-gray-700 mb-2 hover:text-gray-900 cursor-pointer w-full">
            {showMethods ? <ChevronDown className="w-3.5 h-3.5" /> : <ChevronRight className="w-3.5 h-3.5" />}
            Métodos ({methods.length})
          </button>
          {showMethods && (
            <div className="space-y-1">
              {methods.map((m) => (
                <div key={m.id} className="flex items-center justify-between py-1 px-2 rounded hover:bg-gray-50 group">
                  <span className="text-xs text-gray-600 font-mono truncate">
                    {m.visibility === 'private' ? '-' : m.visibility === 'protected' ? '#' : '+'} {m.name}(): {m.returnType}
                  </span>
                  <button onClick={() => onDeleteMethod(m.id)}
                    className="opacity-0 group-hover:opacity-100 text-gray-400 hover:text-red-500 transition-opacity cursor-pointer">
                    <X className="w-3 h-3" />
                  </button>
                </div>
              ))}
              <div className="flex gap-1 mt-2">
                <input placeholder="nombre" value={newMethodName} onChange={(e) => setNewMethodName(e.target.value)}
                  className="flex-1 rounded border border-gray-200 px-1.5 py-1 text-[11px] focus:border-blue-400 outline-none" />
                <input placeholder="retorno" value={newMethodReturn} onChange={(e) => setNewMethodReturn(e.target.value)}
                  className="flex-1 rounded border border-gray-200 px-1.5 py-1 text-[11px] focus:border-blue-400 outline-none" />
                <button onClick={() => { if (newMethodName) { onAddMethod(newMethodName, newMethodReturn); setNewMethodName(''); setNewMethodReturn('void') } }}
                  className="rounded bg-blue-600 p-1 text-white hover:bg-blue-700 cursor-pointer">
                  <Plus className="w-3 h-3" />
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
