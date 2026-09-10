export interface UmlElement {
  id: string
  diagramId: string
  name: string
  visibility: string
  stereotype?: string
  elementType: 'class' | 'interface' | 'enumeration' | 'note'
  positionX: number
  positionY: number
  width: number
  height: number
}

export interface UmlAttribute {
  id: string
  elementId: string
  name: string
  dataType: string
  visibility: string
  defaultValue?: string
  isStatic: boolean
  isFinal: boolean
  orderIndex: number
}

export interface UmlMethod {
  id: string
  elementId: string
  name: string
  returnType: string
  visibility: string
  isStatic: boolean
  isAbstract: boolean
  isFinal: boolean
  isConstructor: boolean
  orderIndex: number
  parameters: UmlParameter[]
}

export interface UmlParameter {
  id: string
  methodId: string
  name: string
  dataType: string
  orderIndex: number
}

export interface UmlLiteral {
  id: string
  elementId: string
  name: string
  value?: string
  orderIndex: number
}

export interface UmlRelationship {
  id: string
  diagramId: string
  sourceElementId: string
  targetElementId: string
  relationshipType: 'association' | 'generalization' | 'dependency' | 'aggregation' | 'composition' | 'realization'
  name?: string
  direction: string
}
