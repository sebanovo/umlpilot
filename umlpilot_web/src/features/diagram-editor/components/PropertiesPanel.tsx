import { useState, useEffect } from 'react'
import type { UmlElement, UmlAttribute, UmlMethod } from '../types/uml'
import { Plus, X, ChevronDown, ChevronRight, Pencil, Check, Trash2 } from 'lucide-react'

interface AttributeInput { name: string; scope: string; type: string; alias: string; stereotype: string; initialValue: string }
interface ParameterInput { name: string; type: string; direction: string; notes: string }

interface PropertiesPanelProps {
  element: UmlElement | null
  attributes: UmlAttribute[]
  methods: UmlMethod[]
  onUpdate: (data: Partial<UmlElement>) => void
  onAddAttribute: (data: AttributeInput) => void
  onAddMethod: (data: AttributeInput) => void
  onDeleteAttribute: (id: string) => void
  onDeleteMethod: (id: string) => void
  onAddParameter: (methodId: string, data: ParameterInput) => void
  onDeleteParameter: (methodId: string, paramId: string) => void
  onDelete?: () => void
  width?: number
}

const SCOPE_OPTIONS = [
  { value: 'public', label: 'público', symbol: '+' },
  { value: 'private', label: 'privado', symbol: '-' },
  { value: 'protected', label: 'protegido', symbol: '#' },
  { value: 'package', label: 'paquete', symbol: '~' },
]
const DIRECTIONS = ['in', 'inout', 'out', 'return']

const scopeSymbol = (scope: string) => SCOPE_OPTIONS.find((s) => s.value === scope)?.symbol || '+'
const emptyInput: AttributeInput = { name: '', scope: 'private', type: 'String', alias: '', stereotype: '', initialValue: '' }
const emptyParam: ParameterInput = { name: '', type: 'String', direction: 'in', notes: '' }

export function PropertiesPanel({ element, attributes, methods, onUpdate, onAddAttribute, onAddMethod, onDeleteAttribute, onDeleteMethod, onAddParameter, onDeleteParameter, onDelete, width }: PropertiesPanelProps) {
  const [newAttr, setNewAttr] = useState<AttributeInput>(emptyInput)
  const [newMethod, setNewMethod] = useState<AttributeInput>({ ...emptyInput, scope: 'public', type: 'void' })
  const [showAttrs, setShowAttrs] = useState(true)
  const [showMethods, setShowMethods] = useState(true)
  const [expandedMethod, setExpandedMethod] = useState<string | null>(null)
  const [newParam, setNewParam] = useState<ParameterInput>(emptyParam)

  const [isEditing, setIsEditing] = useState(false)
  const [editName, setEditName] = useState('')
  const [stereotypeDraft, setStereotypeDraft] = useState('')

  useEffect(() => {
    setIsEditing(false)
    setExpandedMethod(null)
    setStereotypeDraft(element?.stereotype ?? '')
  }, [element?.id, element?.stereotype])

  if (!element) {
    return (
      <div className="shrink-0 bg-gray-900 border-l border-gray-800 flex flex-col items-center justify-center text-gray-500 text-sm" style={width ? { width } : undefined}>
        <svg className="w-10 h-10 mb-3 text-gray-600" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
          <rect x="3" y="3" width="18" height="18" rx="2" />
          <line x1="3" y1="9" x2="21" y2="9" />
          <line x1="9" y1="21" x2="9" y2="9" />
        </svg>
        <p>Selecciona un elemento</p>
        <p className="text-xs text-gray-600 mt-1">para ver sus propiedades</p>
      </div>
    )
  }

  const inputClass = 'w-full rounded border border-gray-700 bg-gray-800 px-1.5 py-1 text-[11px] text-gray-100 placeholder-gray-500 focus:border-blue-500 outline-none'

  return (
    <div className="shrink-0 min-h-0 bg-gray-900 border-l border-gray-800 flex flex-col overflow-hidden" style={width ? { width } : undefined}>
      <div className="px-4 py-3 border-b border-gray-800 flex items-center justify-between">
        <h3 className="text-sm font-semibold text-gray-100">Propiedades</h3>
        {!isEditing && (
          <button onClick={() => { setEditName(element.name); setIsEditing(true) }}
            className="flex items-center gap-1 text-xs text-blue-400 hover:text-blue-300 cursor-pointer transition-colors">
            <Pencil className="w-3.5 h-3.5" /> Editar
          </button>
        )}
      </div>

      <div className="flex-1 min-h-0 overflow-y-auto p-4 space-y-4">
        <div>
          <label className="block text-xs font-medium text-gray-400 mb-1">Nombre</label>
          {isEditing ? (
            <div className="flex gap-2">
              <input autoFocus value={editName} onChange={(e) => setEditName(e.target.value)}
                onKeyDown={(e) => { if (e.key === 'Enter') { onUpdate({ name: editName.trim() || element.name }); setIsEditing(false) }; if (e.key === 'Escape') setIsEditing(false) }}
                className="flex-1 rounded-md border border-gray-700 bg-gray-800 px-2.5 py-1.5 text-sm text-gray-100 focus:border-blue-500 focus:ring-1 focus:ring-blue-500/40 outline-none" />
              <button onClick={() => { onUpdate({ name: editName.trim() || element.name }); setIsEditing(false) }}
                className="rounded-md bg-blue-600 px-2.5 text-white hover:bg-blue-500 cursor-pointer transition-colors">
                <Check className="w-4 h-4" />
              </button>
            </div>
          ) : (
            <p className="text-sm text-gray-100">{element.name}</p>
          )}
        </div>

        {element.elementType === 'class' && (
          <div>
            <label className="block text-xs font-medium text-gray-400 mb-1">Estereotipo</label>
            <div className="flex gap-2">
              <input value={stereotypeDraft} onChange={(e) => setStereotypeDraft(e.target.value)}
                onKeyDown={(e) => { if (e.key === 'Enter') onUpdate({ stereotype: stereotypeDraft.trim() }) }}
                placeholder="ej. entity" className={inputClass} />
              <button onClick={() => onUpdate({ stereotype: stereotypeDraft.trim() })}
                title="Guardar estereotipo"
                className="rounded-md bg-blue-600 px-2.5 text-white hover:bg-blue-500 cursor-pointer transition-colors">
                <Check className="w-4 h-4" />
              </button>
              {element.stereotype && (
                <button onClick={() => { setStereotypeDraft(''); onUpdate({ stereotype: '' }) }}
                  title="Quitar estereotipo"
                  className="rounded-md bg-gray-800 px-2.5 text-gray-300 hover:bg-red-600 hover:text-white cursor-pointer transition-colors">
                  <Trash2 className="w-4 h-4" />
                </button>
              )}
            </div>
            {element.stereotype && (
              <p className="mt-1 text-[11px] text-gray-500">{`<<${element.stereotype}>>`}</p>
            )}
          </div>
        )}

        {/* Attributes */}
        <div className="border-t border-gray-800 pt-3">
          <button onClick={() => setShowAttrs(!showAttrs)}
            className="flex items-center gap-1.5 text-xs font-semibold text-gray-300 mb-2 hover:text-gray-100 cursor-pointer w-full">
            {showAttrs ? <ChevronDown className="w-3.5 h-3.5" /> : <ChevronRight className="w-3.5 h-3.5" />}
            Atributos ({attributes.length})
          </button>
          {showAttrs && (
            <div className="space-y-1">
              {attributes.map((a) => (
                <div key={a.id} className="flex items-center justify-between py-1 px-2 rounded hover:bg-gray-800 group">
                  <span className="text-xs text-gray-300 font-mono truncate">
                    {scopeSymbol(a.visibility)} {a.name}: {a.dataType}
                    {a.defaultValue ? ` = ${a.defaultValue}` : ''}
                    {a.stereotype ? <span className="text-purple-300"> «{a.stereotype}»</span> : null}
                    {a.alias ? <span className="text-gray-500"> ({a.alias})</span> : null}
                  </span>
                  <button onClick={() => onDeleteAttribute(a.id)}
                    className="opacity-0 group-hover:opacity-100 text-gray-500 hover:text-red-400 transition-opacity cursor-pointer">
                    <X className="w-3 h-3" />
                  </button>
                </div>
              ))}

              <div className="grid grid-cols-2 gap-1 mt-2">
                <input placeholder="nombre" value={newAttr.name} onChange={(e) => setNewAttr({ ...newAttr, name: e.target.value })} className={inputClass} />
                <select value={newAttr.scope} onChange={(e) => setNewAttr({ ...newAttr, scope: e.target.value })} className={inputClass}>
                  {SCOPE_OPTIONS.map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}
                </select>
                <input placeholder="tipo" value={newAttr.type} onChange={(e) => setNewAttr({ ...newAttr, type: e.target.value })} className={inputClass} />
                <input placeholder="stereotype" value={newAttr.stereotype} onChange={(e) => setNewAttr({ ...newAttr, stereotype: e.target.value })} className={inputClass} />
                <input placeholder="alias" value={newAttr.alias} onChange={(e) => setNewAttr({ ...newAttr, alias: e.target.value })} className={inputClass} />
                <input placeholder="valor inicial" value={newAttr.initialValue} onChange={(e) => setNewAttr({ ...newAttr, initialValue: e.target.value })} className={inputClass} />
                <button onClick={() => { if (newAttr.name.trim()) { onAddAttribute(newAttr); setNewAttr({ ...emptyInput }) } }}
                  className="col-span-2 flex items-center justify-center gap-1 rounded bg-blue-600 px-2 py-1 text-[11px] text-white hover:bg-blue-500 cursor-pointer">
                  <Plus className="w-3 h-3" /> Agregar atributo
                </button>
              </div>
            </div>
          )}
        </div>

        {/* Methods */}
        <div className="border-t border-gray-800 pt-3">
          <button onClick={() => setShowMethods(!showMethods)}
            className="flex items-center gap-1.5 text-xs font-semibold text-gray-300 mb-2 hover:text-gray-100 cursor-pointer w-full">
            {showMethods ? <ChevronDown className="w-3.5 h-3.5" /> : <ChevronRight className="w-3.5 h-3.5" />}
            Métodos ({methods.length})
          </button>
          {showMethods && (
            <div className="space-y-1">
              {methods.map((m) => (
                <div key={m.id} className="rounded hover:bg-gray-800/50">
                  <div className="flex items-center justify-between py-1 px-2 group">
                    <button onClick={() => setExpandedMethod(expandedMethod === m.id ? null : m.id)}
                      className="flex items-center gap-1 text-xs text-gray-300 font-mono truncate cursor-pointer text-left">
                      {expandedMethod === m.id ? <ChevronDown className="w-3 h-3 shrink-0" /> : <ChevronRight className="w-3 h-3 shrink-0" />}
                      <span className="truncate">
                        {scopeSymbol(m.visibility)} {m.name}(): {m.returnType}
                        {m.stereotype ? <span className="text-purple-300"> «{m.stereotype}»</span> : null}
                        {(m.parameters?.length ? ` (${m.parameters.length})` : '')}
                      </span>
                    </button>
                    <button onClick={() => onDeleteMethod(m.id)}
                      className="opacity-0 group-hover:opacity-100 text-gray-500 hover:text-red-400 transition-opacity cursor-pointer">
                      <X className="w-3 h-3" />
                    </button>
                  </div>

                  {expandedMethod === m.id && (
                    <div className="ml-3 mt-1 mb-2 border-l border-gray-700 pl-2 space-y-1">
                      {m.parameters?.map((p) => (
                        <div key={p.id} className="flex items-center justify-between py-0.5 px-1 rounded hover:bg-gray-800 group">
                          <span className="text-[11px] text-gray-400 font-mono truncate">
                            <span className="text-gray-500">{p.direction || 'in'}</span> {p.name}: {p.dataType}
                            {p.notes ? <span className="text-gray-500"> — {p.notes}</span> : null}
                          </span>
                          <button onClick={() => onDeleteParameter(m.id, p.id)}
                            className="opacity-0 group-hover:opacity-100 text-gray-500 hover:text-red-400 transition-opacity cursor-pointer">
                            <X className="w-3 h-3" />
                          </button>
                        </div>
                      ))}
                      <div className="grid grid-cols-2 gap-1 pt-1">
                        <input placeholder="nombre" value={newParam.name} onChange={(e) => setNewParam({ ...newParam, name: e.target.value })} className={inputClass} />
                        <input placeholder="tipo" value={newParam.type} onChange={(e) => setNewParam({ ...newParam, type: e.target.value })} className={inputClass} />
                        <select value={newParam.direction} onChange={(e) => setNewParam({ ...newParam, direction: e.target.value })} className={inputClass}>
                          {DIRECTIONS.map((d) => <option key={d} value={d}>{d}</option>)}
                        </select>
                        <input placeholder="notes" value={newParam.notes} onChange={(e) => setNewParam({ ...newParam, notes: e.target.value })} className={inputClass} />
                        <button onClick={() => { if (newParam.name.trim()) { onAddParameter(m.id, newParam); setNewParam({ ...emptyParam }) } }}
                          className="col-span-2 flex items-center justify-center gap-1 rounded bg-gray-700 px-2 py-1 text-[11px] text-gray-200 hover:bg-gray-600 cursor-pointer">
                          <Plus className="w-3 h-3" /> Agregar parámetro
                        </button>
                      </div>
                    </div>
                  )}
                </div>
              ))}

              <div className="grid grid-cols-2 gap-1 mt-2">
                <input placeholder="nombre" value={newMethod.name} onChange={(e) => setNewMethod({ ...newMethod, name: e.target.value })} className={inputClass} />
                <select value={newMethod.scope} onChange={(e) => setNewMethod({ ...newMethod, scope: e.target.value })} className={inputClass}>
                  {SCOPE_OPTIONS.map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}
                </select>
                <input placeholder="tipo" value={newMethod.type} onChange={(e) => setNewMethod({ ...newMethod, type: e.target.value })} className={inputClass} />
                <input placeholder="stereotype" value={newMethod.stereotype} onChange={(e) => setNewMethod({ ...newMethod, stereotype: e.target.value })} className={inputClass} />
                <input placeholder="alias" value={newMethod.alias} onChange={(e) => setNewMethod({ ...newMethod, alias: e.target.value })} className={inputClass} />
                <input placeholder="valor inicial" value={newMethod.initialValue} onChange={(e) => setNewMethod({ ...newMethod, initialValue: e.target.value })} className={inputClass} />
                <button onClick={() => { if (newMethod.name.trim()) { onAddMethod(newMethod); setNewMethod({ ...emptyInput, scope: 'public', type: 'void' }) } }}
                  className="col-span-2 flex items-center justify-center gap-1 rounded bg-blue-600 px-2 py-1 text-[11px] text-white hover:bg-blue-500 cursor-pointer">
                  <Plus className="w-3 h-3" /> Agregar método
                </button>
              </div>
            </div>
          )}
        </div>

        {onDelete && (
          <div className="border-t border-gray-800 pt-3">
            <button onClick={onDelete}
              className="w-full flex items-center justify-center gap-1.5 rounded-md border border-red-900 bg-red-950/40 px-3 py-2 text-xs font-medium text-red-300 hover:bg-red-950 transition-colors cursor-pointer">
              <Trash2 className="w-3.5 h-3.5" /> Eliminar elemento
            </button>
          </div>
        )}
      </div>
    </div>
  )
}
