import { useEffect, useState } from 'react'
import { Handle, Position } from '@xyflow/react'
import { NodeActions } from './NodeControls'

interface NoteNodeData {
  name?: string
  onDeleteNode?: (id: string) => void
  onRenameNode?: (id: string, text: string) => void
}

export function NoteNode({ id, data }: { id: string; data: NoteNodeData }) {
  const { name = '', onDeleteNode, onRenameNode } = data
  const [editing, setEditing] = useState(false)
  const [value, setValue] = useState(name)

  useEffect(() => { setValue(name) }, [name])

  const commit = () => {
    setEditing(false)
    if (value !== name) onRenameNode?.(id, value)
  }

  return (
    <div
      className="group relative bg-amber-100 border-2 border-amber-300 rounded shadow-md min-w-[160px] max-w-[260px] text-xs text-gray-900"
      onDoubleClick={() => { if (onRenameNode) { setValue(name); setEditing(true) } }}
    >
      <NodeActions onEdit={onRenameNode ? () => { setValue(name); setEditing(true) } : undefined} onDelete={onDeleteNode ? () => onDeleteNode(id) : undefined} />
      <Handle type="target" position={Position.Top} className="!w-3 !h-3 !bg-blue-400" />
      {editing ? (
        <textarea autoFocus value={value} onChange={(e) => setValue(e.target.value)}
          onBlur={commit}
          onKeyDown={(e) => { if (e.key === 'Escape') { setValue(name); setEditing(false) } }}
          className="nodrag nopan w-full bg-transparent outline-none resize-none p-2 text-gray-900"
          rows={4} />
      ) : (
        <div className="p-2 whitespace-pre-wrap min-h-[44px]">
          {name || <span className="text-gray-400 italic">Doble clic para escribir…</span>}
        </div>
      )}
      <Handle type="source" position={Position.Bottom} className="!w-3 !h-3 !bg-blue-400" />
    </div>
  )
}
