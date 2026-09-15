import type { UmlLiteral } from '../types/uml'
import { NodeActions, NodeHandles, useInlineEdit, inlineInputClass } from './NodeControls'

interface EnumNodeData {
  name: string
  visibility: string
  literals: UmlLiteral[]
  onDeleteNode?: (id: string) => void
  onRenameNode?: (id: string, name: string) => void
}

export function EnumNode({ id, data, selected }: { id: string; data: EnumNodeData; selected?: boolean }) {
  const { name, literals, onDeleteNode, onRenameNode } = data as EnumNodeData
  const edit = useInlineEdit(name, onRenameNode ? (value) => onRenameNode(id, value) : undefined)

  return (
    <div className={`group relative bg-gray-900 border-2 rounded min-w-[160px] font-mono text-xs text-gray-100 transition-shadow ${selected ? 'border-blue-400 ring-2 ring-blue-400/60 shadow-lg shadow-blue-500/20' : 'border-gray-500'}`}>
      <NodeActions onEdit={onRenameNode ? edit.start : undefined} onDelete={onDeleteNode ? () => onDeleteNode(id) : undefined} />
      <NodeHandles />
      <div className="text-center italic text-gray-400 px-2 py-0.5 border-b border-gray-700">{`<<enumeration>>`}</div>
      <div className="text-center font-bold px-2 py-1.5 border-b border-gray-500">
        {edit.editing ? (
          <input autoFocus value={edit.value} onChange={(e) => edit.setValue(e.target.value)}
            onBlur={edit.commit}
            onKeyDown={(e) => { if (e.key === 'Enter') edit.commit(); if (e.key === 'Escape') edit.cancel() }}
            className={inlineInputClass} />
        ) : (
          <>{name}</>
        )}
      </div>
      <div className="px-2 py-1 min-h-[20px]">
        {literals.map((lit: UmlLiteral) => (
          <div key={lit.id}>{lit.name}{lit.value ? ` = ${lit.value}` : ''}</div>
        ))}
      </div>
    </div>
  )
}
