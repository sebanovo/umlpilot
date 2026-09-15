import type { UmlElementApi, UmlAttributeApi, UmlMethodApi, UmlRelationshipApi } from '../../../shared/api/client'

export interface XmiPosition { x: number; y: number; width?: number; height?: number }

interface XmiInput {
  modelName: string
  elements: UmlElementApi[]
  attributes: Record<string, UmlAttributeApi[]>
  methods: Record<string, UmlMethodApi[]>
  relationships: UmlRelationshipApi[]
  positions: Record<string, XmiPosition>
}

const NS_XMI = 'http://www.omg.org/spec/XMI/20131001'
const NS_UML = 'http://www.omg.org/spec/UML/20131001'
const NS_UMLDI = 'http://www.omg.org/spec/UML/20131001/UMLDI'
const NS_DC = 'http://www.omg.org/spec/UML/20131001/UMLDC'

const PKG_ID = 'EAPK_MAIN'
const DIAGRAM_ID = 'EAID_DIAGRAM'

function esc(v: string | null | undefined): string {
  return (v ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&apos;')
}
function xid(raw: string): string {
  return 'EAID_' + raw.replace(/[^A-Za-z0-9_-]/g, '_')
}
function scope(v: string | undefined): string {
  const s = (v || 'public').toLowerCase()
  return s.charAt(0).toUpperCase() + s.slice(1)
}
function elementTag(t: string): string | null {
  switch (t) {
    case 'class': return 'uml:Class'
    case 'interface': return 'uml:Interface'
    case 'enumeration': return 'uml:Enumeration'
    case 'dataType': return 'uml:DataType'
    default: return null
  }
}

export function buildXmi(input: XmiInput): string {
  const { modelName, elements, attributes, methods, relationships, positions } = input
  const primitives = new Map<string, string>() // javaTypeName -> EAJava_ id

  const typeId = (typeName?: string): string => {
    const t = (typeName || 'void').trim() || 'void'
    const el = elements.find((e) => e.name === t && e.elementType !== 'note')
    if (el) return xid(el.id)
    const id = 'EAJava_' + t.toLowerCase().replace(/[^a-z0-9]/g, '')
    primitives.set(t, id)
    return id
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

  const pos = (id: string): XmiPosition => positions[id] || { x: 0, y: 0 }
  const w = (id: string) => { const p = pos(id); return Math.round(p.width && p.width > 0 ? p.width : 160) }
  const h = (id: string) => { const p = pos(id); return Math.round(p.height && p.height > 0 ? p.height : 80) }

  const out: string[] = []
  const p = (n: number) => '\t'.repeat(n)

  out.push('<?xml version="1.0" encoding="windows-1252"?>')
  out.push(`<xmi:XMI xmlns:uml="${NS_UML}" xmlns:xmi="${NS_XMI}" xmlns:umldi="${NS_UMLDI}" xmlns:dc="${NS_DC}">`)
  out.push(`${p(1)}<xmi:Documentation exporter="UMLPilot" exporterVersion="1.0"/>`)
  out.push(`${p(1)}<uml:Model xmi:type="uml:Model" name="EA_Model">`)
  out.push(`${p(2)}<packagedElement xmi:type="uml:Package" xmi:id="${PKG_ID}" name="${esc(modelName)}">`)

  // Classes / interfaces / enumerations
  for (const el of elements) {
    const tag = elementTag(el.elementType)
    if (!tag) continue
    out.push(`${p(3)}<packagedElement xmi:type="${tag}" xmi:id="${xid(el.id)}" name="${esc(el.name)}">`)

    for (const gen of generalizations.get(el.id) || []) {
      out.push(`${p(4)}<generalization xmi:type="uml:Generalization" xmi:id="${xid(gen.id)}" general="${xid(gen.targetElementId)}"/>`)
    }
    for (const real of realizations.get(el.id) || []) {
      out.push(`${p(4)}<interfaceRealization xmi:type="uml:InterfaceRealization" xmi:id="${xid(real.id)}" name="${esc(real.name)}" client="${xid(real.sourceElementId)}" supplier="${xid(real.targetElementId)}" contract="${xid(real.targetElementId)}"/>`)
    }

    for (const attr of attributes[el.id] || []) {
      out.push(`${p(4)}<ownedAttribute xmi:type="uml:Property" xmi:id="${xid(attr.id)}" name="${esc(attr.name)}" visibility="${(attr.visibility || 'private').toLowerCase()}">`)
      out.push(`${p(5)}<lowerValue xmi:type="uml:LiteralInteger" xmi:id="${xid(attr.id + '_lv')}" value="1"/>`)
      out.push(`${p(5)}<upperValue xmi:type="uml:LiteralUnlimitedNatural" xmi:id="${xid(attr.id + '_uv')}" value="1"/>`)
      out.push(`${p(5)}<type xmi:idref="${typeId(attr.dataType)}"/>`)
      out.push(`${p(4)}</ownedAttribute>`)
    }

    for (const method of methods[el.id] || []) {
      out.push(`${p(4)}<ownedOperation xmi:id="${xid(method.id)}" name="${esc(method.name)}">`)
      for (const param of method.parameters || []) {
        out.push(`${p(5)}<ownedParameter xmi:id="${xid(param.id)}" name="${esc(param.name)}" type="${typeId(param.dataType)}"/>`)
      }
      out.push(`${p(5)}<ownedParameter xmi:id="${xid(method.id + '_rt')}" name="return" direction="return" type="${typeId(method.returnType)}"/>`)
      out.push(`${p(4)}</ownedOperation>`)
    }

    out.push(`${p(3)}</packagedElement>`)
  }

  // Associations
  for (const rel of relationships) {
    const t = rel.relationshipType
    if (t !== 'association' && t !== 'aggregation' && t !== 'composition' && t !== 'association_class') continue
    const relId = xid(rel.id)
    const src = xid(rel.id + '_src')
    const dst = xid(rel.id + '_dst')
    const agg = t === 'aggregation' ? 'shared' : t === 'composition' ? 'composite' : 'none'
    out.push(`${p(3)}<packagedElement xmi:type="uml:Association" xmi:id="${relId}" name="${esc(rel.name)}">`)
    out.push(`${p(4)}<memberEnd xmi:idref="${dst}"/>`)
    out.push(`${p(4)}<memberEnd xmi:idref="${src}"/>`)
    out.push(`${p(4)}<ownedEnd xmi:type="uml:Property" xmi:id="${src}" association="${relId}">`)
    out.push(`${p(5)}<type xmi:idref="${xid(rel.sourceElementId)}"/>`)
    out.push(`${p(5)}<lowerValue xmi:type="uml:LiteralInteger" xmi:id="${xid(rel.id + '_slv')}" value="1"/>`)
    out.push(`${p(5)}<upperValue xmi:type="uml:LiteralUnlimitedNatural" xmi:id="${xid(rel.id + '_suv')}" value="1"/>`)
    out.push(`${p(4)}</ownedEnd>`)
    out.push(`${p(4)}<ownedEnd xmi:type="uml:Property" xmi:id="${dst}" association="${relId}" aggregation="${agg}">`)
    out.push(`${p(5)}<type xmi:idref="${xid(rel.targetElementId)}"/>`)
    out.push(`${p(5)}<lowerValue xmi:type="uml:LiteralInteger" xmi:id="${xid(rel.id + '_tlv')}" value="1"/>`)
    out.push(`${p(5)}<upperValue xmi:type="uml:LiteralUnlimitedNatural" xmi:id="${xid(rel.id + '_tuv')}" value="1"/>`)
    out.push(`${p(4)}</ownedEnd>`)
    out.push(`${p(3)}</packagedElement>`)
  }

  // Dependencies
  for (const rel of relationships) {
    if (rel.relationshipType !== 'dependency' && rel.relationshipType !== 'template_binding') continue
    out.push(`${p(3)}<packagedElement xmi:type="uml:Dependency" xmi:id="${xid(rel.id)}" name="${esc(rel.name)}" client="${xid(rel.sourceElementId)}" supplier="${xid(rel.targetElementId)}"/>`)
  }

  out.push(`${p(2)}</packagedElement>`)

  // ===== Diagram interchange (standard) =====
  const diagramNodes = elements.filter((e) => elementTag(e.elementType))
  const shapeId = (id: string) => xid(id + '_shape')
  out.push(`${p(2)}<umldi:Diagram xmi:type="umldi:UMLClassDiagram" xmi:id="${DIAGRAM_ID}" isFrame="false" modelElement="${PKG_ID}">`)
  for (const el of diagramNodes) {
    const q = pos(el.id)
    out.push(`${p(3)}<ownedElement xmi:type="umldi:UMLClassifierShape" xmi:id="${shapeId(el.id)}" modelElement="${xid(el.id)}">`)
    out.push(`${p(4)}<ownedElement xmi:type="umldi:UMLNameLabel" xmi:id="${xid(el.id + '_nl')}" text="${esc(el.name)}"/>`)
    out.push(`${p(4)}<bounds xmi:type="dc:bounds" xmi:id="${xid(el.id + '_db')}" x="${Math.round(q.x)}" y="${Math.round(q.y)}" width="${w(el.id)}" height="${h(el.id)}"/>`)
    out.push(`${p(3)}</ownedElement>`)
  }
  const hasShape = (id: string) => diagramNodes.some((e) => e.id === id)
  for (const rel of relationships) {
    if (!hasShape(rel.sourceElementId) || !hasShape(rel.targetElementId)) continue
    const s = pos(rel.sourceElementId)
    const t = pos(rel.targetElementId)
    out.push(`${p(3)}<ownedElement xmi:type="umldi:UMLEdge" xmi:id="${xid(rel.id + '_edge')}" source="${shapeId(rel.sourceElementId)}" target="${shapeId(rel.targetElementId)}" modelElement="${xid(rel.id)}">`)
    out.push(`${p(4)}<waypoint xmi:type="dc:waypoint" xmi:id="${xid(rel.id + '_wp0')}" x="${Math.round(s.x + w(rel.sourceElementId) / 2)}" y="${Math.round(s.y + h(rel.sourceElementId) / 2)}"/>`)
    out.push(`${p(4)}<waypoint xmi:type="dc:waypoint" xmi:id="${xid(rel.id + '_wp1')}" x="${Math.round(t.x + w(rel.targetElementId) / 2)}" y="${Math.round(t.y + h(rel.targetElementId) / 2)}"/>`)
    out.push(`${p(3)}</ownedElement>`)
  }
  out.push(`${p(2)}</umldi:Diagram>`)
  out.push(`${p(1)}</uml:Model>`)

  // ===== EA Extension =====
  out.push(`${p(1)}<xmi:Extension extender="Enterprise Architect" extenderID="6.5">`)

  // elements
  out.push(`${p(2)}<elements>`)
  out.push(`${p(3)}<element xmi:idref="${PKG_ID}" xmi:type="uml:Package" name="${esc(modelName)}" scope="public">`)
  out.push(`${p(4)}<model package2="EAID_MAIN" package="EAID_ROOT" tpos="0" ea_localid="1" ea_eleType="package"/>`)
  out.push(`${p(4)}<properties isSpecification="false" sType="Package" nType="0" scope="public"/>`)
  out.push(`${p(4)}<project author="UMLPilot" version="1.0" phase="1.0" complexity="1" status="Proposed"/>`)
  out.push(`${p(4)}<code gentype="Java"/>`)
  out.push(`${p(4)}<extendedProperties tagged="0" package_name="Model"/>`)
  out.push(`${p(3)}</element>`)

  for (const el of elements) {
    const tag = elementTag(el.elementType)
    if (!tag) continue
    out.push(`${p(3)}<element xmi:idref="${xid(el.id)}" xmi:type="${tag}" name="${esc(el.name)}" scope="public">`)
    out.push(`${p(4)}<model package="${PKG_ID}" tpos="0" ea_localid="0" ea_eleType="element"/>`)
    out.push(`${p(4)}<properties isSpecification="false" sType="${tag.replace('uml:', '')}" nType="0" scope="public" isRoot="false" isLeaf="false" isAbstract="false" isActive="false"/>`)
    out.push(`${p(4)}<project author="UMLPilot" version="1.0" phase="1.0" complexity="1" status="Proposed"/>`)
    out.push(`${p(4)}<code gentype="Java"/>`)
    out.push(`${p(4)}<extendedProperties tagged="0" package_name="${esc(modelName)}"/>`)

    const elementAttrs = attributes[el.id] || []
    if (elementAttrs.length) {
      out.push(`${p(4)}<attributes>`)
      elementAttrs.forEach((attr, i) => {
        out.push(`${p(5)}<attribute xmi:idref="${xid(attr.id)}" name="${esc(attr.name)}" scope="${scope(attr.visibility)}">`)
        out.push(`${p(6)}<initial${attr.defaultValue ? ` value="${esc(attr.defaultValue)}"` : ''}/>`)
        out.push(`${p(6)}<documentation/>`)
        out.push(`${p(6)}<properties type="${esc(attr.dataType || '')}" collection="false" static="${attr.isStatic ? '1' : '0'}" duplicates="0" changeability="changeable"/>`)
        out.push(`${p(6)}<containment containment="Not Specified" position="${i}"/>`)
        out.push(`${p(6)}<stereotype${attr.stereotype ? ` stereotype="${esc(attr.stereotype)}"` : ''}/>`)
        out.push(`${p(6)}<bounds lower="1" upper="1"/>`)
        out.push(`${p(5)}</attribute>`)
      })
      out.push(`${p(4)}</attributes>`)
    }

    const elementMethods = methods[el.id] || []
    if (elementMethods.length) {
      out.push(`${p(4)}<operations>`)
      elementMethods.forEach((method, i) => {
        out.push(`${p(5)}<operation xmi:idref="${xid(method.id)}" name="${esc(method.name)}" scope="${scope(method.visibility)}">`)
        out.push(`${p(6)}<properties position="${i}"/>`)
        out.push(`${p(6)}<stereotype${method.stereotype ? ` stereotype="${esc(method.stereotype)}"` : ''}/>`)
        out.push(`${p(6)}<type type="${esc(method.returnType || 'void')}" const="false" static="${method.isStatic ? 'true' : 'false'}" isAbstract="false" synchronised="0" concurrency="Sequential" pure="0" isQuery="false"/>`)
        out.push(`${p(6)}<parameters>`)
        out.push(`${p(7)}<parameter xmi:idref="${xid(method.id + '_rt')}" visibility="public">`)
        out.push(`${p(8)}<properties pos="0" type="${esc(method.returnType || 'void')}" const="false"/>`)
        out.push(`${p(7)}</parameter>`)
        for (const param of method.parameters || []) {
          out.push(`${p(7)}<parameter xmi:idref="${xid(param.id)}" visibility="public">`)
          out.push(`${p(8)}<properties pos="0" type="${esc(param.dataType || '')}" const="false"/>`)
          out.push(`${p(8)}<documentation${param.notes ? ` value="${esc(param.notes)}"` : ''}/>`)
          out.push(`${p(7)}</parameter>`)
        }
        out.push(`${p(6)}</parameters>`)
        out.push(`${p(5)}</operation>`)
      })
      out.push(`${p(4)}</operations>`)
    }

    out.push(`${p(3)}</element>`)
  }
  out.push(`${p(2)}</elements>`)

  // connectors
  const connRels = relationships.filter((r) =>
    ['association', 'aggregation', 'composition', 'association_class', 'generalization', 'realization', 'dependency', 'template_binding'].includes(r.relationshipType))
  out.push(`${p(2)}<connectors>`)
  for (const rel of connRels) {
    out.push(`${p(3)}<connector xmi:idref="${xid(rel.id)}" name="${esc(rel.name)}">`)
    out.push(`${p(4)}<source xmi:idref="${xid(rel.sourceElementId)}"><model type="Class"/><type multiplicity="1" aggregation="none" containment="Unspecified"/></source>`)
    out.push(`${p(4)}<target xmi:idref="${xid(rel.targetElementId)}"><model type="Class"/><type multiplicity="1" aggregation="none" containment="Unspecified"/></target>`)
    out.push(`${p(4)}<properties ea_type="${esc(rel.relationshipType)}" direction="Unspecified"/>`)
    out.push(`${p(4)}<modifiers isRoot="false" isLeaf="false"/>`)
    out.push(`${p(3)}</connector>`)
  }
  out.push(`${p(2)}</connectors>`)

  // primitives
  primitives.set('void', 'EAJava_void')
  out.push(`${p(2)}<primitivetypes>`)
  out.push(`${p(3)}<packagedElement xmi:type="uml:Package" xmi:id="EAPrimitiveTypesPackage" name="EA_PrimitiveTypes_Package">`)
  out.push(`${p(4)}<packagedElement xmi:type="uml:Package" xmi:id="EAJavaTypesPackage" name="EA_Java_Types_Package">`)
  for (const [name, id] of primitives) {
    out.push(`${p(5)}<packagedElement xmi:type="uml:PrimitiveType" xmi:id="${id}" name="${esc(name)}"/>`)
  }
  out.push(`${p(4)}</packagedElement>`)
  out.push(`${p(3)}</packagedElement>`)
  out.push(`${p(2)}</primitivetypes>`)

  // diagrams (EA geometry)
  out.push(`${p(2)}<diagrams>`)
  out.push(`${p(3)}<diagram xmi:id="${DIAGRAM_ID}">`)
  out.push(`${p(4)}<model package="${PKG_ID}" localID="1" owner="${PKG_ID}"/>`)
  out.push(`${p(4)}<properties name="${esc(modelName)}" type="Logical"/>`)
  out.push(`${p(4)}<project author="UMLPilot" version="1.0"/>`)
  out.push(`${p(4)}<extendedProperties/>`)
  out.push(`${p(4)}<elements>`)
  diagramNodes.forEach((el, i) => {
    const q = pos(el.id)
    const left = Math.round(q.x), top = Math.round(q.y)
    out.push(`${p(5)}<element geometry="Left=${left};Top=${top};Right=${left + w(el.id)};Bottom=${top + h(el.id)};" subject="${xid(el.id)}" seqno="${i + 1}"/>`)
  })
  for (const rel of relationships) {
    if (!hasShape(rel.sourceElementId) || !hasShape(rel.targetElementId)) continue
    out.push(`${p(5)}<element geometry="SX=0;SY=0;EX=0;EY=0;EDGE=1;" subject="${xid(rel.id)}" style="Mode=3;EOID=${xid(rel.targetElementId)};SOID=${xid(rel.sourceElementId)};Color=-1;LWidth=0;Hidden=0;"/>`)
  }
  out.push(`${p(4)}</elements>`)
  out.push(`${p(3)}</diagram>`)
  out.push(`${p(2)}</diagrams>`)

  out.push(`${p(1)}</xmi:Extension>`)
  out.push('</xmi:XMI>')

  return out.join('\n')
}
