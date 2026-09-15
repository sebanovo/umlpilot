import { Handle, Position } from '@xyflow/react'
import type { UmlLiteral } from '../types/uml'
import { NodeActions, useInlineEdit, inlineInputClass } from './NodeControls'

interface EnumNodeData {
  name: string
  visibility: string
  literals: UmlLiteral[]
  onDeleteNode?: (id: string) => void
  onRenameNode?: (id: string, name: string) => void
}

const visSymbol: Record<string, string> = { public: '+', private: '-', protected: '#', package: '~' }

export function EnumNode({ id, data }: { id: string; data: EnumNodeData }) {
  const { name, visibility, literals, onDeleteNode, onRenameNode } = data as EnumNodeData
  const edit = useInlineEdit(name, onRenameNode ? (value) => onRenameNode(id, value) : undefined)

  return (
    <div className="group relative bg-gray-900 border-2 border-gray-500 rounded min-w-[160px] font-mono text-xs text-gray-100">
      <NodeActions onEdit={onRenameNode ? edit.start : undefined} onDelete={onDeleteNode ? () => onDeleteNode(id) : undefined} />
      <Handle type="target" position={Position.Top} className="!w-3 !h-3 !bg-blue-400" />
      <div className="text-center italic text-gray-400 px-2 py-0.5 border-b border-gray-700">{`<<enumeration>>`}</div>
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
      <div className="px-2 py-1 min-h-[20px]">
        {literals.map((lit: UmlLiteral) => (
          <div key={lit.id}>{lit.name}{lit.value ? ` = ${lit.value}` : ''}</div>
        ))}
      </div>
      <Handle type="source" position={Position.Bottom} className="!w-3 !h-3 !bg-blue-400" />
    </div>
  )
}
