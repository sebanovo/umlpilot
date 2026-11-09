/* eslint-disable react-refresh/only-export-components */
import { useEffect, useState } from 'react'
import { Handle, Position } from '@xyflow/react'
import { Pencil, Trash2 } from 'lucide-react'

export function NodeHandles() {
  return (
    <>
      <Handle id="t-top" type="target" position={Position.Top} className="!w-3 !h-3 !bg-emerald-400" style={{ left: '38%' }} />
      <Handle id="s-top" type="source" position={Position.Top} className="!w-3 !h-3 !bg-blue-400" style={{ left: '62%' }} />
      <Handle id="t-bottom" type="target" position={Position.Bottom} className="!w-3 !h-3 !bg-emerald-400" style={{ left: '62%' }} />
      <Handle id="s-bottom" type="source" position={Position.Bottom} className="!w-3 !h-3 !bg-blue-400" style={{ left: '38%' }} />
      <Handle id="t-left" type="target" position={Position.Left} className="!w-3 !h-3 !bg-emerald-400" style={{ top: '38%' }} />
      <Handle id="s-left" type="source" position={Position.Left} className="!w-3 !h-3 !bg-blue-400" style={{ top: '62%' }} />
      <Handle id="t-right" type="target" position={Position.Right} className="!w-3 !h-3 !bg-emerald-400" style={{ top: '62%' }} />
      <Handle id="s-right" type="source" position={Position.Right} className="!w-3 !h-3 !bg-blue-400" style={{ top: '38%' }} />
    </>
  )
}

interface NodeActionsProps {
  onEdit?: () => void
  onDelete?: () => void
}

export function NodeActions({ onEdit, onDelete }: NodeActionsProps) {
  if (!onEdit && !onDelete) return null
  return (
    <div className="nodrag nopan absolute -top-3 -right-2 z-10 flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
      {onEdit && (
        <button type="button" title="Editar nombre"
          onPointerDown={(e) => e.stopPropagation()}
          onClick={(e) => { e.stopPropagation(); onEdit() }}
          className="flex items-center justify-center w-5 h-5 rounded bg-gray-700 text-gray-200 shadow hover:bg-blue-600 hover:text-white cursor-pointer">
          <Pencil className="w-3 h-3" />
        </button>
      )}
      {onDelete && (
        <button type="button" title="Eliminar"
          onPointerDown={(e) => e.stopPropagation()}
          onClick={(e) => { e.stopPropagation(); onDelete() }}
          className="flex items-center justify-center w-5 h-5 rounded bg-gray-700 text-gray-200 shadow hover:bg-red-600 hover:text-white cursor-pointer">
          <Trash2 className="w-3 h-3" />
        </button>
      )}
    </div>
  )
}

export function useInlineEdit(name: string, onRename?: (name: string) => void) {
  const [editing, setEditing] = useState(false)
  const [value, setValue] = useState(name)

  useEffect(() => { setValue(name) }, [name])

  const start = () => { setValue(name); setEditing(true) }
  const commit = () => {
    setEditing(false)
    if (onRename && value !== name) onRename(value)
  }
  const cancel = () => { setValue(name); setEditing(false) }

  return { editing, value, setValue, start, commit, cancel }
}

export const inlineInputClass =
  'nodrag nopan w-full bg-gray-800 text-center outline-none rounded px-1 text-gray-100'
