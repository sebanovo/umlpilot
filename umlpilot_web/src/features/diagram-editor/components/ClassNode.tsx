import { Handle, Position } from '@xyflow/react'
import type { UmlAttribute, UmlMethod } from '../types/uml'
import { NodeActions, useInlineEdit, inlineInputClass } from './NodeControls'

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

export function ClassNode({ id, data }: { id: string; data: ClassNodeData }) {
  const { name, stereotype, visibility, attributes, methods, onDeleteNode, onRenameNode } = data as ClassNodeData
  const edit = useInlineEdit(name, onRenameNode ? (value) => onRenameNode(id, value) : undefined)

  return (
    <div className="group relative bg-gray-900 border-2 border-gray-500 rounded min-w-45 font-mono text-xs text-gray-100">
      <NodeActions onEdit={onRenameNode ? edit.start : undefined} onDelete={onDeleteNode ? () => onDeleteNode(id) : undefined} />
      <Handle type="target" position={Position.Top} className="!w-3 !h-3 !bg-blue-400" />
      {stereotype && <div className="text-center italic text-gray-400 px-2 py-0.5 border-b border-gray-700">{`<<${stereotype}>>`}</div>}
      <div className="text-center font-bold px-2 py-1.5 border-b border-gray-500">
        {edit.editing ? (
          <input autoFocus value={edit.value} onChange={(e) => edit.setValue(e.target.value)}
            onBlur={edit.commit}
            onKeyDown={(e) => { if (e.key === 'Enter') edit.commit(); if (e.key === 'Escape') edit.cancel() }}
            className={inlineInputClass} />
        ) : (
          <>{visSymbol[visibility] || '+'} {name}</>
        )}
      </div>
      <div className="px-2 py-1 border-b border-gray-700 min-h-5">
        {attributes.length === 0 && <div className="text-gray-600">&nbsp;</div>}
        {attributes.map((attr: UmlAttribute) => (
          <div key={attr.id}>{visSymbol[attr.visibility] || '+'} {attr.name}: {attr.dataType}</div>
        ))}
      </div>
      <div className="px-2 py-1 min-h-5">
        {methods.length === 0 && <div className="text-gray-600">&nbsp;</div>}
        {methods.map((m: UmlMethod) => (
          <div key={m.id}>{visSymbol[m.visibility] || '+'} {m.name}({m.parameters.map((p) => p.name).join(', ')}): {m.returnType}</div>
        ))}
      </div>
      <Handle type="source" position={Position.Bottom} className="!w-3 !h-3 !bg-blue-400" />
    </div>
  )
}
