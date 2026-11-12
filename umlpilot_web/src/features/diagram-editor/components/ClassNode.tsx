import type { UmlAttribute, UmlMethod } from '../types/uml'
import { NodeHandles, useInlineEdit, inlineInputClass } from './NodeControls'

interface ClassNodeData {
  name: string
  stereotype?: string
  visibility: string
  attributes: UmlAttribute[]
  methods: UmlMethod[]
  onDeleteNode?: (id: string) => void
  onRenameNode?: (id: string, name: string) => void
}

const visSymbol: Record<string, string> = { public: '+', private: '-', protected: '#', package: '~' }

export function ClassNode({ id, data, selected }: { id: string; data: ClassNodeData; selected?: boolean }) {
  const { name, stereotype, attributes, methods, onRenameNode } = data as ClassNodeData
  const edit = useInlineEdit(name, onRenameNode ? (value) => onRenameNode(id, value) : undefined)

  return (
    <div className={`group relative bg-white border-2 rounded-none min-w-45 font-mono text-xs text-black transition-shadow ${selected ? 'border-blue-600 ring-2 ring-blue-400/60 shadow-lg shadow-blue-500/20' : 'border-black'}`}>
      <NodeHandles />
      {stereotype && <div className="text-center italic text-gray-600 px-2 py-0.5">{`<<${stereotype}>>`}</div>}
      <div className="text-center font-bold px-2 py-1.5 border-b border-black">
        {edit.editing ? (
          <input autoFocus value={edit.value} onChange={(e) => edit.setValue(e.target.value)}
            onBlur={edit.commit}
            onKeyDown={(e) => { if (e.key === 'Enter') edit.commit(); if (e.key === 'Escape') edit.cancel() }}
            className={inlineInputClass} />
        ) : (
          <>{name}</>
        )}
      </div>
      <div className="px-2 py-1 border-b border-black min-h-5">
        {attributes.length === 0 && <div className="text-gray-400">&nbsp;</div>}
        {attributes.map((attr: UmlAttribute) => (
          <div key={attr.id}>{visSymbol[attr.visibility] || '+'} {attr.name}: {attr.dataType}{attr.stereotype ? ` «${attr.stereotype}»` : ''}{attr.defaultValue ? ` = ${attr.defaultValue}` : ''}</div>
        ))}
      </div>
      <div className="px-2 py-1 min-h-5">
        {methods.length === 0 && <div className="text-gray-400">&nbsp;</div>}
        {methods.map((m: UmlMethod) => (
          <div key={m.id}>{visSymbol[m.visibility] || '+'} {m.name}({m.parameters.map((p) => p.name).join(', ')}): {m.returnType}{m.stereotype ? ` «${m.stereotype}»` : ''}{m.defaultValue ? ` = ${m.defaultValue}` : ''}</div>
        ))}
      </div>
    </div>
  )
}
