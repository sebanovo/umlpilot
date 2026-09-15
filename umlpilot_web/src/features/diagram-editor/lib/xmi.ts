import type { UmlElementApi, UmlAttributeApi, UmlMethodApi, UmlRelationshipApi } from '../../../shared/api/client'

interface XmiInput {
  modelName: string
  elements: UmlElementApi[]
  attributes: Record<string, UmlAttributeApi[]>
  methods: Record<string, UmlMethodApi[]>
  relationships: UmlRelationshipApi[]
}

const XMLNS_XMI = 'http://www.omg.org/spec/XMI/20131001'
const XMLNS_UML = 'http://www.omg.org/spec/UML/20131001'

function esc(value: string | null | undefined): string {
  return (value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;')
}

function xid(raw: string): string {
  return 'EAID_' + raw.replace(/[^A-Za-z0-9_-]/g, '_')
}

function visibility(v: string | undefined): string {
  return v === 'private' || v === 'protected' || v === 'package' ? v : 'public'
}

function elementTag(elementType: string): string | null {
  switch (elementType) {
    case 'class': return 'uml:Class'
    case 'interface': return 'uml:Interface'
    case 'enumeration': return 'uml:Enumeration'
    case 'dataType': return 'uml:DataType'
    default: return null
  }
}

export function buildXmi(input: XmiInput): string {
  const { modelName, elements, attributes, methods, relationships } = input
  const primitiveIds = new Map<string, string>()

  const typeRef = (typeName?: string): string | null => {
    if (!typeName || !typeName.trim()) return null
    const name = typeName.trim()
    const el = elements.find((e) => e.name === name && e.elementType !== 'note')
    if (el) return xid(el.id)
    if (!primitiveIds.has(name)) primitiveIds.set(name, xid('pt_' + name))
    return primitiveIds.get(name)!
  }

  const bySource = (type: string) => {
    const map = new Map<string, UmlRelationshipApi[]>()
    for (const rel of relationships) {
      if (rel.relationshipType !== type) continue
      const list = map.get(rel.sourceElementId) || []
      list.push(rel)
      map.set(rel.sourceElementId, list)
    }
    return map
  }
  const generalizations = bySource('generalization')
  const realizations = bySource('realization')

  const lines: string[] = []
  const pad = (n: number) => '  '.repeat(n)

  lines.push('<?xml version="1.0" encoding="UTF-8"?>')
  lines.push(`<xmi:XMI xmi:version="2.5.1" xmlns:xmi="${XMLNS_XMI}" xmlns:uml="${XMLNS_UML}">`)
  lines.push(`${pad(1)}<xmi:Documentation exporter="UMLPilot" exporterVersion="1.0"/>`)
  lines.push(`${pad(1)}<uml:Model xmi:id="EAID_MODEL" name="${esc(modelName)}">`)
  lines.push(`${pad(2)}<packagedElement xmi:type="uml:Package" xmi:id="EAID_PACKAGE" name="Model">`)

  // Classes / interfaces / enumerations
  for (const el of elements) {
    const tag = elementTag(el.elementType)
    if (!tag) continue
    lines.push(`${pad(3)}<packagedElement xmi:type="${tag}" xmi:id="${xid(el.id)}" name="${esc(el.name)}">`)

    for (const gen of generalizations.get(el.id) || []) {
      lines.push(`${pad(4)}<generalization xmi:type="uml:Generalization" xmi:id="${xid(gen.id)}" general="${xid(gen.targetElementId)}"/>`)
    }
    for (const real of realizations.get(el.id) || []) {
      lines.push(`${pad(4)}<interfaceRealization xmi:type="uml:InterfaceRealization" xmi:id="${xid(real.id)}" name="${esc(real.name)}" client="${xid(real.sourceElementId)}" supplier="${xid(real.targetElementId)}" contract="${xid(real.targetElementId)}"/>`)
    }

    for (const attr of attributes[el.id] || []) {
      const typeId = typeRef(attr.dataType)
      const typeAttr = typeId ? ` type="${typeId}"` : ''
      const staticAttr = attr.isStatic ? ' isStatic="true"' : ''
      lines.push(`${pad(4)}<ownedAttribute xmi:type="uml:Property" xmi:id="${xid(attr.id)}" name="${esc(attr.name)}" visibility="${visibility(attr.visibility)}"${typeAttr}${staticAttr}>`)
      if (attr.defaultValue) {
        lines.push(`${pad(5)}<defaultValue xmi:type="uml:LiteralString" xmi:id="${xid(attr.id + '_default')}" value="${esc(attr.defaultValue)}"/>`)
      }
      lines.push(`${pad(4)}</ownedAttribute>`)
    }

    for (const method of methods[el.id] || []) {
      const staticAttr = method.isStatic ? ' isStatic="true"' : ''
      lines.push(`${pad(4)}<ownedOperation xmi:type="uml:Operation" xmi:id="${xid(method.id)}" name="${esc(method.name)}" visibility="${visibility(method.visibility)}"${staticAttr}>`)
      const retId = typeRef(method.returnType)
      if (retId) {
        lines.push(`${pad(5)}<ownedParameter xmi:type="uml:Parameter" xmi:id="${xid(method.id + '_return')}" name="return" direction="return" type="${retId}"/>`)
      }
      for (const param of method.parameters || []) {
        const pId = typeRef(param.dataType)
        const pType = pId ? ` type="${pId}"` : ''
        lines.push(`${pad(5)}<ownedParameter xmi:type="uml:Parameter" xmi:id="${xid(param.id)}" name="${esc(param.name)}" direction="in"${pType}/>`)
      }
      if (method.defaultValue) {
        lines.push(`${pad(5)}<defaultValue xmi:type="uml:LiteralString" xmi:id="${xid(method.id + '_default')}" value="${esc(method.defaultValue)}"/>`)
      }
      lines.push(`${pad(4)}</ownedOperation>`)
    }

    lines.push(`${pad(3)}</packagedElement>`)
  }

  // Associations (association / aggregation / composition / association class)
  for (const rel of relationships) {
    const type = rel.relationshipType
    if (type !== 'association' && type !== 'aggregation' && type !== 'composition' && type !== 'association_class') continue
    const relId = xid(rel.id)
    const srcEnd = xid(rel.id + '_src')
    const tgtEnd = xid(rel.id + '_tgt')
    const aggregation = type === 'aggregation' ? ' aggregation="shared"' : type === 'composition' ? ' aggregation="composite"' : ''
    lines.push(`${pad(3)}<packagedElement xmi:type="uml:Association" xmi:id="${relId}" name="${esc(rel.name)}">`)
    lines.push(`${pad(4)}<memberEnd xmi:idref="${srcEnd}"/>`)
    lines.push(`${pad(4)}<memberEnd xmi:idref="${tgtEnd}"/>`)
    lines.push(`${pad(4)}<ownedEnd xmi:type="uml:Property" xmi:id="${srcEnd}" type="${xid(rel.sourceElementId)}" association="${relId}"/>`)
    lines.push(`${pad(4)}<ownedEnd xmi:type="uml:Property" xmi:id="${tgtEnd}" type="${xid(rel.targetElementId)}" association="${relId}"${aggregation}/>`)
    lines.push(`${pad(3)}</packagedElement>`)
  }

  // Dependencies (dependency / template binding)
  for (const rel of relationships) {
    const type = rel.relationshipType
    if (type !== 'dependency' && type !== 'template_binding') continue
    lines.push(`${pad(3)}<packagedElement xmi:type="uml:Dependency" xmi:id="${xid(rel.id)}" name="${esc(rel.name)}" client="${xid(rel.sourceElementId)}" supplier="${xid(rel.targetElementId)}"/>`)
  }

  // Primitive types used by attributes / operations / parameters
  if (primitiveIds.size > 0) {
    lines.push(`${pad(3)}<packagedElement xmi:type="uml:Package" xmi:id="EAID_PRIMITIVE_TYPES" name="PrimitiveTypes">`)
    for (const [name, id] of primitiveIds) {
      lines.push(`${pad(4)}<packagedElement xmi:type="uml:PrimitiveType" xmi:id="${id}" name="${esc(name)}"/>`)
    }
    lines.push(`${pad(3)}</packagedElement>`)
  }

  lines.push(`${pad(2)}</packagedElement>`)
  lines.push(`${pad(1)}</uml:Model>`)
  lines.push('</xmi:XMI>')

  return lines.join('\n')
}
