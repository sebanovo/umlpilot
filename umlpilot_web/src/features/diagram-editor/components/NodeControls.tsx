/* eslint-disable react-refresh/only-export-components */
import { Fragment, useEffect, useState } from 'react'
import { Handle, Position } from '@xyflow/react'

const HANDLE_SIDES = [
  { side: 'top', position: Position.Top },
  { side: 'right', position: Position.Right },
  { side: 'bottom', position: Position.Bottom },
  { side: 'left', position: Position.Left },
]

export function NodeHandles() {
  return (
    <>
      {HANDLE_SIDES.map(({ side, position }) => (
        <Fragment key={side}>
          <Handle id={`t-${side}-center`} type="target" position={position} />
          <Handle id={`s-${side}-center`} type="source" position={position} />
        </Fragment>
      ))}
    </>
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
  'nodrag nopan w-full bg-white text-center outline-none rounded-none border border-black px-1 text-black'
