import { Handle, Position } from '@xyflow/react'
import type { UmlLiteral } from '../types/uml'

interface EnumNodeData {
  name: string
  visibility: string
  literals: UmlLiteral[]
}

const visSymbol: Record<string, string> = { public: '+', private: '-', protected: '#', package: '~' }

export function EnumNode({ data }: { data: EnumNodeData }) {
  const { name, visibility, literals } = data as EnumNodeData
  return (
    <div className="bg-gray-900 border-2 border-gray-500 rounded min-w-[160px] font-mono text-xs text-gray-100">
      <Handle type="target" position={Position.Top} className="!w-3 !h-3 !bg-blue-400" />
      <div className="text-center italic text-gray-400 px-2 py-0.5 border-b border-gray-700">{`<<enumeration>>`}</div>
      <div className="text-center font-bold px-2 py-1.5 border-b border-gray-500">
        {visSymbol[visibility] || '+'} {name}
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
