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
    <div style={{ background: 'white', border: '2px solid #333', borderRadius: '4px', minWidth: 180, fontFamily: 'monospace', fontSize: 12 }}>
      <Handle type="target" position={Position.Top} style={{ background: '#555' }} />
      <div style={{ textAlign: 'center', fontStyle: 'italic', color: '#666', padding: '2px 8px', borderBottom: '1px solid #ccc' }}>{`<<interface>>`}</div>
      <div style={{ textAlign: 'center', fontWeight: 'bold', padding: '6px 8px', borderBottom: '1px solid #333' }}>
        {visSymbol[visibility] || '+'} {name}
      </div>
      <div style={{ padding: '4px 8px', borderBottom: '1px solid #ccc', minHeight: 20 }}>
        {attributes.map((attr: UmlAttribute) => (
          <div key={attr.id}>{visSymbol[attr.visibility] || '+'} {attr.name}: {attr.dataType}</div>
        ))}
      </div>
      <div style={{ padding: '4px 8px', minHeight: 20 }}>
        {methods.map((m: UmlMethod) => (
          <div key={m.id}>{visSymbol[m.visibility] || '+'} {m.name}({m.parameters.map((p) => p.name).join(', ')}): {m.returnType}</div>
        ))}
      </div>
      <Handle type="source" position={Position.Bottom} style={{ background: '#555' }} />
    </div>
  )
}
