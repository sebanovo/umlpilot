import type { UmlElementApi, UmlAttributeApi, UmlMethodApi, UmlRelationshipApi } from '../../../shared/api/client'

export interface XmiPosition { x: number; y: number; width?: number; height?: number }

interface XmiInput {
  modelName: string
  diagramId: string
  elements: UmlElementApi[]
  attributes: Record<string, UmlAttributeApi[]>
  methods: Record<string, UmlMethodApi[]>
  relationships: UmlRelationshipApi[]
  positions: Record<string, XmiPosition>
  multiplicities?: Record<string, { source?: string; target?: string }>
}

const NS_XMI = 'http://www.omg.org/spec/XMI/20131001'
const NS_UML = 'http://www.omg.org/spec/UML/20131001'
const NS_UMLDI = 'http://www.omg.org/spec/UML/20131001/UMLDI'
const NS_DC = 'http://www.omg.org/spec/UML/20131001/UMLDC'

const STYLE1 = 'ShowPrivate=1;ShowProtected=1;ShowPublic=1;HideRelationships=0;Locked=0;Border=1;HighlightForeign=1;PackageContents=1;SequenceNotes=0;ScalePrintImage=0;PPgs.cx=0;PPgs.cy=0;DocSize.cx=799;DocSize.cy=1067;ShowDetails=0;Orientation=P;Zoom=100;ShowTags=0;OpParams=1;VisibleAttributeDetail=0;ShowOpRetType=1;ShowIcons=1;CollabNums=0;HideProps=0;ShowReqs=0;ShowCons=0;PaperSize=1;HideParents=0;UseAlias=0;HideAtts=0;HideOps=0;HideStereo=0;HideElemStereo=0;ShowTests=0;ShowMaint=0;ConnectorNotation=UML 2.1;ExplicitNavigability=0;ShowShape=1;AllDockable=0;AdvancedElementProps=1;AdvancedFeatureProps=1;AdvancedConnectorProps=1;m_bElementClassifier=1;SPT=1;ShowNotes=0;SuppressBrackets=0;SuppConnectorLabels=0;PrintPageHeadFoot=0;ShowAsList=0;'
const STYLE2 = 'ExcludeRTF=0;DocAll=0;HideQuals=0;AttPkg=1;ShowTests=0;ShowMaint=0;SuppressFOC=1;MatrixActive=0;SwimlanesActive=1;KanbanActive=0;MatrixLineWidth=1;MatrixLineClr=0;MatrixLocked=0;TConnectorNotation=UML 2.1;TExplicitNavigability=0;AdvancedElementProps=1;AdvancedFeatureProps=1;AdvancedConnectorProps=1;m_bElementClassifier=1;SPT=1;MDGDgm=;STBLDgm=;ShowNotes=0;VisibleAttributeDetail=0;ShowOpRetType=1;SuppressBrackets=0;SuppConnectorLabels=0;PrintPageHeadFoot=0;ShowAsList=0;SuppressedCompartments=;Theme=:119;'
const STYLE_SWIMLANES = 'locked=false;orientation=0;width=0;inbar=false;names=false;color=-1;bold=false;fcol=0;tcol=-1;ofCol=-1;ufCol=-1;hl=0;ufh=0;hh=0;cls=0;bw=0;hli=0;SwimlaneFont=lfh:-13,lfw:0,lfi:0,lfu:0,lfs:0,lfface=Calibri,lfe:0,lfo:0,lfchar:1,lfop:0,lfcp:0,lfq:0,lfpf=0,lfWidth=0;'
const STYLE_MATRIX = 'locked=false;matrixactive=false;swimlanesactive=true;kanbanactive=false;width=1;clrLine=0;'
const CONN_LABELS = '$LLB=CX=17:CY=14:OX=0:OY=0:HDN=0:BLD=0:ITA=0:UND=0:CLR=-1:ALN=1:DIR=0:ROT=0;LLT=;LMT=CX=32:CY=14:OX=0:OY=0:HDN=0:BLD=0:ITA=0:UND=0:CLR=-1:ALN=1:DIR=0:ROT=0;LMB=;LRT=;LRB=CX=17:CY=14:OX=0:OY=0:HDN=0:BLD=0:ITA=0:UND=0:CLR=-1:ALN=1:DIR=0:ROT=0;IRHS=;ILHS=;Path=;'

// EA serialises its proprietary GUIDs as uppercase hex groups joined by "_".
function guid(raw: string): string {
  return raw.toUpperCase().replace(/[^A-Z0-9]/g, '_').replace(/_+/g, '_').replace(/^_+|_+$/g, '')
}
function esc(v: string | null | undefined): string {
  return (v ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&apos;')
}
function xid(raw: string): string {
  return 'EAID_' + guid(raw)
}
function duid(raw: string): string {
  let h = 2166136261
  for (let i = 0; i < raw.length; i++) { h ^= raw.charCodeAt(i); h = Math.imul(h, 16777619) }
  return (h >>> 0).toString(16).toUpperCase().padStart(8, '0').slice(0, 8)
}
function scope(v: string | undefined): string {
  const s = (v || 'public').toLowerCase()
  return s.charAt(0).toUpperCase() + s.slice(1)
}
// "0..*" / "1" / "*" -> { lower, upper } for LiteralInteger/LiteralUnlimitedNatural
function bounds(m?: string): { lower: string; upper: string } {
  const s = (m || '').trim()
  if (!s) return { lower: '1', upper: '1' }
  if (s.includes('..')) {
    const [a, b] = s.split('..')
    return { lower: (a.trim() || '0'), upper: (b.trim() || '*') }
  }
  if (s === '*') return { lower: '0', upper: '*' }
  return { lower: s, upper: s }
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
function elementStype(t: string): string {
  switch (t) {
    case 'interface': return 'Interface'
    case 'enumeration': return 'Enumeration'
    case 'dataType': return 'DataType'
    default: return 'Class'
  }
}

export function buildXmi(input: XmiInput): string {
  const { modelName, diagramId, elements, attributes, methods, relationships, positions, multiplicities } = input
  const primitives = new Map<string, string>() // xmi:id -> display name (deduped)
  let localId = 100

  // EA identifies the package and the diagram by real, distinct GUIDs derived from the id.
  const PKG_ID = 'EAPK_' + guid(diagramId)
  const DIAGRAM_ID = 'EAID_' + guid(diagramId + '_diagram')

  const diagramNodes = elements.filter((e) => elementTag(e.elementType))
  const emitted = new Set(diagramNodes.map((e) => e.id))

  // An association class is modelled as a normal Class element plus a relationship
  // of type association_class that points at it. In UML/EA it must be a *single*
  // uml:AssociationClass, so we merge both into one element and hide the plain class.
  const assocClassRel = new Map<string, UmlRelationshipApi>()
  for (const rel of relationships) {
    if (rel.relationshipType === 'association_class' && rel.associationClassElementId) {
      assocClassRel.set(rel.associationClassElementId, rel)
    }
  }
  const assocClassIds = new Set(assocClassRel.keys())

  const typeId = (typeName?: string): string => {
    const t = (typeName || 'void').trim() || 'void'
    const el = elements.find((e) => e.name === t && emitted.has(e.id))
    if (el) return xid(el.id)
    const clean = t.toLowerCase().replace(/[^a-z0-9]/g, '') || 'void'
    const id = 'EAJava_' + clean
    if (!primitives.has(id)) primitives.set(id, clean)
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
  const w = (id: string) => Math.round((pos(id).width && pos(id).width! > 0) ? pos(id).width! : 160)
  const h = (id: string) => Math.round((pos(id).height && pos(id).height! > 0) ? pos(id).height! : 80)
  const shapeId = (id: string) => xid(id + '_shape')

  // Only keep relationships whose both ends are real, emittable elements so no
  // xmi:idref ever dangles (a dangling ref makes EA drop the whole relationship).
  const validRel = (rel: UmlRelationshipApi) => emitted.has(rel.sourceElementId) && emitted.has(rel.targetElementId)

  // Relationship types that we actually materialise in the UML model; anything
  // else would leave a dangling modelElement reference in the diagram.
  const MODELED_TYPES = new Set(['association', 'aggregation', 'composition', 'association_class', 'dependency', 'template_binding', 'generalization', 'realization'])
  const modeledRel = (rel: UmlRelationshipApi) => validRel(rel) && MODELED_TYPES.has(rel.relationshipType)

  const out: string[] = []
  const p = (n: number) => '\t'.repeat(n)

  const emitAttributes = (ownerId: string, indent: number) => {
    for (const attr of attributes[ownerId] || []) {
      out.push(`${p(indent)}<ownedAttribute xmi:type="uml:Property" xmi:id="${xid(attr.id)}" name="${esc(attr.name)}" visibility="${(attr.visibility || 'private').toLowerCase()}">`)
      out.push(`${p(indent + 1)}<lowerValue xmi:type="uml:LiteralInteger" xmi:id="${xid(attr.id + '_lv')}" value="1"/>`)
      out.push(`${p(indent + 1)}<upperValue xmi:type="uml:LiteralUnlimitedNatural" xmi:id="${xid(attr.id + '_uv')}" value="1"/>`)
      out.push(`${p(indent + 1)}<type xmi:idref="${typeId(attr.dataType)}"/>`)
      out.push(`${p(indent)}</ownedAttribute>`)
    }
  }
  const emitMethods = (ownerId: string, indent: number) => {
    for (const method of methods[ownerId] || []) {
      out.push(`${p(indent)}<ownedOperation xmi:id="${xid(method.id)}" name="${esc(method.name)}">`)
      for (const param of method.parameters || []) {
        out.push(`${p(indent + 1)}<ownedParameter xmi:id="${xid(param.id)}" name="${esc(param.name)}" type="${typeId(param.dataType)}"/>`)
      }
      out.push(`${p(indent + 1)}<ownedParameter xmi:id="${xid(method.id + '_rt')}" name="return" direction="return" type="${typeId(method.returnType)}"/>`)
      out.push(`${p(indent)}</ownedOperation>`)
    }
  }

  out.push('<?xml version="1.0" encoding="UTF-8"?>')
  out.push(`<xmi:XMI xmlns:uml="${NS_UML}" xmlns:xmi="${NS_XMI}" xmlns:umldi="${NS_UMLDI}" xmlns:dc="${NS_DC}">`)
  out.push(`${p(1)}<xmi:Documentation exporter="Enterprise Architect" exporterVersion="6.5"/>`)
  out.push(`${p(1)}<uml:Model xmi:type="uml:Model" name="EA_Model">`)
  out.push(`${p(2)}<packagedElement xmi:type="uml:Package" xmi:id="${PKG_ID}" name="${esc(modelName)}">`)

  for (const el of elements) {
    const tag = elementTag(el.elementType)
    if (!tag) continue
    if (assocClassIds.has(el.id)) continue // emitted later as uml:AssociationClass
    out.push(`${p(3)}<packagedElement xmi:type="${tag}" xmi:id="${xid(el.id)}" name="${esc(el.name)}">`)
    for (const gen of generalizations.get(el.id) || []) {
      if (!validRel(gen)) continue
      out.push(`${p(4)}<generalization xmi:type="uml:Generalization" xmi:id="${xid(gen.id)}" general="${xid(gen.targetElementId)}"/>`)
    }
    for (const real of realizations.get(el.id) || []) {
      if (!validRel(real)) continue
      out.push(`${p(4)}<interfaceRealization xmi:type="uml:InterfaceRealization" xmi:id="${xid(real.id)}" name="${esc(real.name)}" client="${xid(real.sourceElementId)}" supplier="${xid(real.targetElementId)}" contract="${xid(real.targetElementId)}"/>`)
    }
    emitAttributes(el.id, 4)
    emitMethods(el.id, 4)
    out.push(`${p(3)}</packagedElement>`)
  }

  for (const rel of relationships) {
    const t = rel.relationshipType
    if (t !== 'association' && t !== 'aggregation' && t !== 'composition' && t !== 'association_class') continue
    if (!validRel(rel)) continue
    const isAssocClass = t === 'association_class'
    const clsEl = isAssocClass ? elements.find((e) => e.id === rel.associationClassElementId) : undefined
    // The AssociationClass is the class element (it holds the attributes); the connector
    // id stays on the relationship so the diagram edge and the extension connector line up.
    const relId = isAssocClass ? xid(clsEl?.id || rel.id) : xid(rel.id)
    const src = xid(rel.id + '_src')
    const dst = xid(rel.id + '_dst')
    const tag = isAssocClass ? 'uml:AssociationClass' : 'uml:Association'
    const agg = t === 'aggregation' ? 'shared' : t === 'composition' ? 'composite' : 'none'
    const name = isAssocClass ? (clsEl?.name || rel.name) : rel.name
    const sb = bounds(multiplicities?.[rel.id]?.source)
    const tb = bounds(multiplicities?.[rel.id]?.target)
    out.push(`${p(3)}<packagedElement xmi:type="${tag}" xmi:id="${relId}" name="${esc(name)}">`)
    out.push(`${p(4)}<memberEnd xmi:idref="${dst}"/>`)
    out.push(`${p(4)}<memberEnd xmi:idref="${src}"/>`)
    out.push(`${p(4)}<ownedEnd xmi:type="uml:Property" xmi:id="${src}" association="${relId}"${agg !== 'none' ? ` aggregation="${agg}"` : ''}>`)
    out.push(`${p(5)}<type xmi:idref="${xid(rel.sourceElementId)}"/>`)
    if (!isAssocClass) {
      out.push(`${p(5)}<lowerValue xmi:type="uml:LiteralInteger" xmi:id="${xid(rel.id + '_slv')}" value="${sb.lower}"/>`)
      out.push(`${p(5)}<upperValue xmi:type="uml:LiteralUnlimitedNatural" xmi:id="${xid(rel.id + '_suv')}" value="${sb.upper}"/>`)
    }
    out.push(`${p(4)}</ownedEnd>`)
    out.push(`${p(4)}<ownedEnd xmi:type="uml:Property" xmi:id="${dst}" association="${relId}">`)
    out.push(`${p(5)}<type xmi:idref="${xid(rel.targetElementId)}"/>`)
    if (!isAssocClass) {
      out.push(`${p(5)}<lowerValue xmi:type="uml:LiteralInteger" xmi:id="${xid(rel.id + '_tlv')}" value="${tb.lower}"/>`)
      out.push(`${p(5)}<upperValue xmi:type="uml:LiteralUnlimitedNatural" xmi:id="${xid(rel.id + '_tuv')}" value="${tb.upper}"/>`)
    }
    out.push(`${p(4)}</ownedEnd>`)
    if (isAssocClass && clsEl) {
      emitAttributes(clsEl.id, 4)
      emitMethods(clsEl.id, 4)
    }
    out.push(`${p(3)}</packagedElement>`)
  }
  for (const rel of relationships) {
    if (rel.relationshipType !== 'dependency' && rel.relationshipType !== 'template_binding') continue
    if (!validRel(rel)) continue
    out.push(`${p(3)}<packagedElement xmi:type="uml:Dependency" xmi:id="${xid(rel.id)}" name="${esc(rel.name)}" client="${xid(rel.sourceElementId)}" supplier="${xid(rel.targetElementId)}"/>`)
  }

  out.push(`${p(2)}</packagedElement>`)

  // Standard diagram interchange
  out.push(`${p(2)}<umldi:Diagram xmi:type="umldi:UMLClassDiagram" xmi:id="${DIAGRAM_ID}" isFrame="false" modelElement="${PKG_ID}">`)
  for (const el of diagramNodes) {
    const q = pos(el.id)
    out.push(`${p(3)}<ownedElement xmi:type="umldi:UMLClassifierShape" xmi:id="${shapeId(el.id)}" modelElement="${xid(el.id)}">`)
    out.push(`${p(4)}<ownedElement xmi:type="umldi:UMLNameLabel" xmi:id="${xid(el.id + '_nl')}" text="${esc(el.name)}"/>`)
    out.push(`${p(4)}<bounds xmi:type="dc:bounds" xmi:id="${xid(el.id + '_db')}" x="${Math.round(q.x)}" y="${Math.round(q.y)}" width="${w(el.id)}" height="${h(el.id)}"/>`)
    out.push(`${p(3)}</ownedElement>`)
  }
  for (const rel of relationships) {
    if (!modeledRel(rel)) continue
    const s = pos(rel.sourceElementId)
    const t = pos(rel.targetElementId)
    out.push(`${p(3)}<ownedElement xmi:type="umldi:UMLEdge" xmi:id="${xid(rel.id + '_edge')}" source="${shapeId(rel.sourceElementId)}" target="${shapeId(rel.targetElementId)}" modelElement="${xid(rel.id)}">`)
    out.push(`${p(4)}<waypoint xmi:type="dc:waypoint" xmi:id="${xid(rel.id + '_wp0')}" x="${Math.round(s.x + w(rel.sourceElementId) / 2)}" y="${Math.round(s.y + h(rel.sourceElementId) / 2)}"/>`)
    out.push(`${p(4)}<waypoint xmi:type="dc:waypoint" xmi:id="${xid(rel.id + '_wp1')}" x="${Math.round(t.x + w(rel.targetElementId) / 2)}" y="${Math.round(t.y + h(rel.targetElementId) / 2)}"/>`)
    out.push(`${p(3)}</ownedElement>`)
  }
  out.push(`${p(2)}</umldi:Diagram>`)
  out.push(`${p(1)}</uml:Model>`)

  // ===== xmi:Extension =====
  out.push(`${p(1)}<xmi:Extension extender="Enterprise Architect" extenderID="6.5">`)

  out.push(`${p(2)}<elements>`)
  localId++
  out.push(`${p(3)}<element xmi:idref="${PKG_ID}" xmi:type="uml:Package" name="${esc(modelName)}" scope="public">`)
  out.push(`${p(4)}<model package2="${xid(diagramId)}" package="${PKG_ID}" tpos="0" ea_localid="${localId}" ea_eleType="package"/>`)
  out.push(`${p(4)}<properties isSpecification="false" sType="Package" nType="0" scope="public"/>`)
  out.push(`${p(4)}<project author="UMLPilot" version="1.0" phase="1.0" complexity="1" status="Proposed"/>`)
  out.push(`${p(4)}<code gentype="Java"/>`)
  out.push(`${p(4)}<style appearance="BackColor=-1;BorderColor=-1;BorderWidth=-1;FontColor=-1;VSwimLanes=1;HSwimLanes=1;BorderStyle=0;"/>`)
  out.push(`${p(4)}<tags/>`)
  out.push(`${p(4)}<xrefs/>`)
  out.push(`${p(4)}<extendedProperties tagged="0" package_name="Model"/>`)
  out.push(`${p(4)}<packageproperties version="1.0"/>`)
  out.push(`${p(4)}<paths/>`)
  out.push(`${p(4)}<flags iscontrolled="FALSE" isprotected="FALSE" usedtd="FALSE" logxml="FALSE"/>`)
  out.push(`${p(3)}</element>`)

  const linksFor = (id: string) => relationships.filter((r) => validRel(r) && (r.sourceElementId === id || r.targetElementId === id))
  const localIds = new Map<string, number>()

  for (const el of elements) {
    const tag = elementTag(el.elementType)
    if (!tag) continue
    localId++
    localIds.set(el.id, localId)
    const isAssocClass = assocClassIds.has(el.id)
    const assocRel = assocClassRel.get(el.id)
    out.push(`${p(3)}<element xmi:idref="${xid(el.id)}" xmi:type="${tag}" name="${esc(el.name)}" scope="public">`)
    out.push(`${p(4)}<model package="${PKG_ID}" tpos="0" ea_localid="${localId}" ea_eleType="element"/>`)
    out.push(`${p(4)}<properties isSpecification="false" sType="${elementStype(el.elementType)}" nType="${isAssocClass ? 17 : 0}" scope="public" isRoot="false" isLeaf="false" isAbstract="false" isActive="false"/>`)
    out.push(`${p(4)}<project author="UMLPilot" version="1.0" phase="1.0" complexity="1" status="Proposed"/>`)
    out.push(`${p(4)}<code gentype="Java"/>`)
    out.push(`${p(4)}<style appearance="BackColor=-1;BorderColor=-1;BorderWidth=-1;FontColor=-1;VSwimLanes=1;HSwimLanes=1;BorderStyle=0;"/>`)
    out.push(`${p(4)}<tags/>`)
    out.push(`${p(4)}<xrefs/>`)
    out.push(`${p(4)}<extendedProperties tagged="0" package_name="${esc(modelName)}"${isAssocClass && assocRel ? ` conID="${xid(assocRel.id)}"` : ''}/>`)

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

    const links = linksFor(el.id)
    if (links.length) {
      out.push(`${p(4)}<links>`)
      for (const link of links) {
        const kind = link.relationshipType === 'generalization' ? 'Generalization' : 'Association'
        out.push(`${p(5)}<${kind} xmi:id="${xid(link.id + '_link')}" start="${xid(link.sourceElementId)}" end="${xid(link.targetElementId)}"/>`)
      }
      out.push(`${p(4)}</links>`)
    }
    out.push(`${p(3)}</element>`)
  }
  out.push(`${p(2)}</elements>`)

  // connectors
  const connRels = relationships.filter((r) => validRel(r) && ['association', 'aggregation', 'composition', 'association_class', 'generalization', 'realization', 'dependency', 'template_binding'].includes(r.relationshipType))
  out.push(`${p(2)}<connectors>`)
  for (const rel of connRels) {
    const isGen = rel.relationshipType === 'generalization'
    const isReal = rel.relationshipType === 'realization'
    const isAssocClass = rel.relationshipType === 'association_class'
    const agg = rel.relationshipType === 'aggregation' ? 'shared' : rel.relationshipType === 'composition' ? 'composite' : 'none'
    const clsId = isAssocClass ? (rel.associationClassElementId || '') : ''
    const sMul = (multiplicities?.[rel.id]?.source || '').trim() || '1'
    const tMul = (multiplicities?.[rel.id]?.target || '').trim() || '1'
    const srcEl = elements.find((e) => e.id === rel.sourceElementId)
    const tgtEl = elements.find((e) => e.id === rel.targetElementId)
    const srcLocal = localIds.get(rel.sourceElementId)
    const tgtLocal = localIds.get(rel.targetElementId)
    out.push(`${p(3)}<connector xmi:idref="${xid(rel.id)}" name="${esc(rel.name)}">`)
    out.push(`${p(4)}<source xmi:idref="${xid(rel.sourceElementId)}">`)
    out.push(`${p(5)}<model${srcLocal ? ` ea_localid="${srcLocal}"` : ''} type="Class" name="${esc(srcEl?.name || '')}"/>`)
    out.push(`${p(5)}<role visibility="Public"/>`)
    if (isGen || isReal) out.push(`${p(5)}<type aggregation="none"/>`)
    else out.push(`${p(5)}<type multiplicity="${esc(sMul)}" aggregation="${agg}"/>`)
    out.push(`${p(5)}<constraints/>`)
    out.push(`${p(5)}<modifiers isOrdered="false" isNavigable="false"/>`)
    out.push(`${p(5)}<documentation/>`)
    out.push(`${p(4)}</source>`)
    out.push(`${p(4)}<target xmi:idref="${xid(rel.targetElementId)}">`)
    out.push(`${p(5)}<model${tgtLocal ? ` ea_localid="${tgtLocal}"` : ''} type="Class" name="${esc(tgtEl?.name || '')}"/>`)
    out.push(`${p(5)}<role visibility="Public"/>`)
    if (isGen || isReal) out.push(`${p(5)}<type aggregation="none"/>`)
    else out.push(`${p(5)}<type multiplicity="${esc(tMul)}" aggregation="none"/>`)
    out.push(`${p(5)}<constraints/>`)
    out.push(`${p(5)}<modifiers isOrdered="false" isNavigable="true"/>`)
    out.push(`${p(5)}<documentation/>`)
    out.push(`${p(4)}</target>`)
    const eaType = isGen ? 'Generalization' : isReal ? 'Realization' : rel.relationshipType === 'dependency' ? 'Dependency' : 'Association'
    out.push(`${p(4)}<properties ea_type="${eaType}"${isAssocClass ? ' subtype="Class"' : ''} direction="${isGen ? 'Source -&gt; Destination' : 'Unspecified'}"/>`)
    out.push(`${p(4)}<modifiers isRoot="false" isLeaf="false"/>`)
    out.push(`${p(4)}<appearance linemode="3" linecolor="-1" linewidth="0" seqno="0" headStyle="0" lineStyle="0"/>`)
    if (isAssocClass) out.push(`${p(4)}<labels/>`)
    else if (isGen || isReal) out.push(`${p(4)}<labels mt="${esc(rel.name)}"/>`)
    else out.push(`${p(4)}<labels lb="${esc(sMul)}" mt="${esc(rel.name)}" rb="${esc(tMul)}"/>`)
    if (isAssocClass && clsId) {
      const priv = localIds.get(clsId)
      out.push(`${p(4)}<extendedProperties virtualInheritance="0" associationclass="${xid(clsId)}"${priv ? ` privatedata1="${priv}"` : ''}/>`)
    } else {
      out.push(`${p(4)}<extendedProperties virtualInheritance="0"/>`)
    }
    out.push(`${p(3)}</connector>`)
  }
  out.push(`${p(2)}</connectors>`)

  // primitives
  primitives.set('EAJava_void', 'void')
  out.push(`${p(2)}<primitivetypes>`)
  out.push(`${p(3)}<packagedElement xmi:type="uml:Package" xmi:id="EAPrimitiveTypesPackage" name="EA_PrimitiveTypes_Package">`)
  out.push(`${p(4)}<packagedElement xmi:type="uml:Package" xmi:id="EAJavaTypesPackage" name="EA_Java_Types_Package">`)
  for (const [id, name] of primitives) {
    out.push(`${p(5)}<packagedElement xmi:type="uml:PrimitiveType" xmi:id="${id}" name="${esc(name)}"/>`)
  }
  out.push(`${p(4)}</packagedElement>`)
  out.push(`${p(3)}</packagedElement>`)
  out.push(`${p(2)}</primitivetypes>`)
  out.push(`${p(2)}<profiles/>`)

  // diagrams (EA geometry)
  out.push(`${p(2)}<diagrams>`)
  out.push(`${p(3)}<diagram xmi:id="${DIAGRAM_ID}">`)
  out.push(`${p(4)}<model package="${PKG_ID}" localID="1" owner="${PKG_ID}"/>`)
  out.push(`${p(4)}<properties name="${esc(modelName)}" type="Logical"/>`)
  out.push(`${p(4)}<project author="UMLPilot" version="1.0"/>`)
  out.push(`${p(4)}<style1 value="${STYLE1}"/>`)
  out.push(`${p(4)}<style2 value="${STYLE2}"/>`)
  out.push(`${p(4)}<swimlanes value="${STYLE_SWIMLANES}"/>`)
  out.push(`${p(4)}<matrixitems value="${STYLE_MATRIX}"/>`)
  out.push(`${p(4)}<extendedProperties/>`)
  out.push(`${p(4)}<elements>`)
  diagramNodes.forEach((el, i) => {
    const q = pos(el.id)
    const left = Math.round(q.x), top = Math.round(q.y)
    out.push(`${p(5)}<element geometry="Left=${left};Top=${top};Right=${left + w(el.id)};Bottom=${top + h(el.id)};" subject="${xid(el.id)}" seqno="${i + 1}" style="DUID=${duid(el.id)};"/>`)
  })
  for (const rel of relationships) {
    if (!modeledRel(rel)) continue
    const sDuid = duid(rel.sourceElementId)
    const tDuid = duid(rel.targetElementId)
    out.push(`${p(5)}<element geometry="EDGE=2;SX=0;SY=0;EX=0;EY=0;${CONN_LABELS}" subject="${xid(rel.id)}" style="Mode=3;EOID=${tDuid};SOID=${sDuid};Color=-1;LWidth=0;Hidden=0;"/>`)
  }
  out.push(`${p(4)}</elements>`)
  out.push(`${p(3)}</diagram>`)
  out.push(`${p(2)}</diagrams>`)

  out.push(`${p(1)}</xmi:Extension>`)
  out.push('</xmi:XMI>')

  return out.join('\n')
}
