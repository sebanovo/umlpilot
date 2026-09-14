import { Handle, Position } from '@xyflow/react'
import type { UmlAttribute, UmlMethod } from '../types/uml'

interface InterfaceNodeData {
  name: string
  visibility: string
  attributes: UmlAttribute[]
  methods: UmlMethod[]
}

const visSymbol: Record<string, string> = { public: '+', private: '-', protected: '#', package: '~' }

export function InterfaceNode({ data }: { data: InterfaceNodeData }) {
  const { name, visibility, attributes, methods } = data as InterfaceNodeData
  return (
    <div className="bg-gray-900 border-2 border-gray-500 rounded min-w-[180px] font-mono text-xs text-gray-100">
      <Handle type="target" position={Position.Top} className="!w-3 !h-3 !bg-blue-400" />
      <div className="text-center italic text-gray-400 px-2 py-0.5 border-b border-gray-700">{`<<interface>>`}</div>
      <div className="text-center font-bold px-2 py-1.5 border-b border-gray-500">
        {visSymbol[visibility] || '+'} {name}
      </div>
      <div className="px-2 py-1 border-b border-gray-700 min-h-[20px]">
        {attributes.map((attr: UmlAttribute) => (
          <div key={attr.id}>{visSymbol[attr.visibility] || '+'} {attr.name}: {attr.dataType}</div>
        ))}
      </div>
      <div className="px-2 py-1 min-h-[20px]">
        {methods.map((m: UmlMethod) => (
          <div key={m.id}>{visSymbol[m.visibility] || '+'} {m.name}({m.parameters.map((p) => p.name).join(', ')}): {m.returnType}</div>
        ))}
      </div>
      <Handle type="source" position={Position.Bottom} className="!w-3 !h-3 !bg-blue-400" />
    </div>
  )
}
