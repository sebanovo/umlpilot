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
    <div style={{ background: 'white', border: '2px solid #333', borderRadius: '4px', minWidth: 160, fontFamily: 'monospace', fontSize: 12 }}>
      <Handle type="target" position={Position.Top} style={{ background: '#555' }} />
      <div style={{ textAlign: 'center', fontStyle: 'italic', color: '#666', padding: '2px 8px', borderBottom: '1px solid #ccc' }}>{`<<enumeration>>`}</div>
      <div style={{ textAlign: 'center', fontWeight: 'bold', padding: '6px 8px', borderBottom: '1px solid #333' }}>
        {visSymbol[visibility] || '+'} {name}
      </div>
      <div style={{ padding: '4px 8px', minHeight: 20 }}>
        {literals.map((lit: UmlLiteral) => (
          <div key={lit.id}>{lit.name}{lit.value ? ` = ${lit.value}` : ''}</div>
        ))}
      </div>
      <Handle type="source" position={Position.Bottom} style={{ background: '#555' }} />
    </div>
  )
}
