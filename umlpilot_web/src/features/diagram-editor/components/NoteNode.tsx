import { useEffect, useState } from 'react'
import { NodeActions, NodeHandles } from './NodeControls'

interface NoteNodeData {
  name?: string
  onDeleteNode?: (id: string) => void
  onRenameNode?: (id: string, text: string) => void
}

export function NoteNode({ id, data, selected }: { id: string; data: NoteNodeData; selected?: boolean }) {
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
      className={`group relative bg-amber-100 border-2 rounded shadow-md min-w-[160px] max-w-[260px] text-xs text-gray-900 transition-shadow ${selected ? 'border-blue-500 ring-2 ring-blue-400/60 shadow-lg shadow-blue-500/20' : 'border-amber-300'}`}
      onDoubleClick={() => { if (onRenameNode) { setValue(name); setEditing(true) } }}
    >
      <NodeActions onEdit={onRenameNode ? () => { setValue(name); setEditing(true) } : undefined} onDelete={onDeleteNode ? () => onDeleteNode(id) : undefined} />
      <NodeHandles />
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
    </div>
  )
}
