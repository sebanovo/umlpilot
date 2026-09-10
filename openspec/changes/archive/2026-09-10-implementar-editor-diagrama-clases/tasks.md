## 1. Backend - Dominio UML

- [x] 1.1 Crear entidad de dominio `Element` con campos: id, diagramId, parentId, creatorId, name, visibility, stereotype, elementType, positionX, positionY, width, height, properties. Verificar que compila
- [x] 1.2 Crear entidades de dominio `ClassElement`, `InterfaceElement`, `EnumerationElement` con campos específicos (isAbstract, isFinal, baseType, etc.). Verificar que compila
- [x] 1.3 Crear entidades de dominio `Attribute`, `Method`, `Parameter`, `Literal` con campos según esquema SQL. Verificar que compila
- [x] 1.4 Crear entidad de dominio `Relationship` con campos: id, diagramId, sourceElementId, targetElementId, relationshipType, name, direction. Verificar que compila
- [x] 1.5 Crear entidad de dominio `Multiplicity` con campos: id, relationshipId, end (source/target), min, max, isOrdered, isUnique. Verificar que compila
- [x] 1.6 Crear Value Objects: `ElementId`, `RelationshipId`, `MultiplicityId`. Verificar que compila

## 2. Backend - Repositorios y Application Layer

- [x] 2.1 Crear interfaces de repositorio en domain: `ElementRepository`, `RelationshipRepository`, `MultiplicityRepository`. Verificar que compila
- [x] 2.2 Crear JPA entities en infrastructure: `ElementJpaEntity`, `ClassElementJpaEntity`, `InterfaceElementJpaEntity`, `EnumerationElementJpaEntity`, `AttributeJpaEntity`, `MethodJpaEntity`, `ParameterJpaEntity`, `LiteralJpaEntity`, `RelationshipJpaEntity`, `MultiplicityJpaEntity`. Verificar que compila
- [x] 2.3 Crear JPA repositories: `ElementJpaRepository`, `RelationshipJpaRepository`, `MultiplicityJpaRepository`. Verificar que compila
- [x] 2.4 Crear persistence adapters: `ElementPersistenceAdapter`, `RelationshipPersistenceAdapter`, `MultiplicityPersistenceAdapter`. Verificar que compila
- [x] 2.5 Crear DTOs de aplicación: `CreateElementCommand`, `UpdateElementCommand`, `ElementResult`, `CreateRelationshipCommand`, `RelationshipResult`, etc. Verificar que compila

## 3. Backend - Servicios y Controladores

- [x] 3.1 Crear servicio `ElementService` con CRUD de elementos y validación. Verificar que compila
- [x] 3.2 Crear servicio `AttributeService` con CRUD de atributos. Verificar que compila
- [x] 3.3 Crear servicio `MethodService` con CRUD de métodos y parámetros. Verificar que compila
- [x] 3.4 Crear servicio `RelationshipService` con CRUD de relaciones y multiplicidades. Verificar que compila
- [x] 3.5 Crear `ElementController` con endpoints REST bajo `/api/v1/projects/{projectId}/diagrams/{diagramId}/elements`. Verificar que compila
- [x] 3.6 Crear `AttributeController` y `MethodController` con endpoints REST. Verificar que compila
- [x] 3.7 Crear `RelationshipController` con endpoints REST. Verificar que compila

## 4. Backend - WebSocket Events

- [x] 4.1 Extender `WebSocketHandler` con eventos: `element_created`, `element_moved`, `element_updated`, `element_deleted`, `relationship_created`, `relationship_updated`, `relationship_deleted`. Verificar que compila
- [x] 4.2 Añadir eventos de presencia: `user_cursor`, `user_selection`. Verificar que compila

## 5. Frontend - Dependencias y Estructura

- [x] 5.1 Instalar `@xyflow/react`, `@stomp/stompjs`, `sockjs-client` en `umlpilot_web`. Verificar que `pnpm run build` compila
- [x] 5.2 Crear estructura de directorios: `features/diagram-editor/` con subcarpetas `components/`, `hooks/`, `types/`. Verificar estructura
- [x] 5.3 Definir tipos TypeScript: `UmlElement`, `UmlClass`, `UmlInterface`, `UmlAttribute`, `UmlMethod`, `UmlRelationship`, `UmlMultiplicity`. Verificar que compila

## 6. Frontend - Componentes UML

- [x] 6.1 Crear componente `ClassNode` que renderice una caja UML con 3 secciones (nombre, atributos, métodos). Verificar que renderiza
- [x] 6.2 Crear componente `InterfaceNode` con estereotipo «interface». Verificar que renderiza
- [x] 6.3 Crear componente `EnumNode` con estereotipo «enumeration» y lista de literales. Verificar que renderiza
- [x] 6.4 Crear edges customizados: `AssociationEdge`, `GeneralizationEdge`, `DependencyEdge` con estilos SVG. Verificar que renderiza

## 7. Frontend - Editor y Canvas

- [x] 7.1 Crear componente `DiagramCanvas` que integre React Flow con los nodos y edges UML. Verificar que renderiza
- [x] 7.2 Crear `Toolbar` con opciones: Clase, Interfaz, Enumeración, Nota, tipos de relación. Verificar que renderiza
- [x] 7.3 Crear `PropertiesPanel` que muestre propiedades del elemento seleccionado (nombre, visibilidad, atributos, métodos). Verificar que renderiza
- [x] 7.4 Implementar drag & drop desde toolbar para crear elementos. Verificar que funciona

## 8. Frontend - WebSocket y Colaboración

- [x] 8.1 Migrar `useWebSocket` a STOMP client con `@stomp/stompjs`. Verificar que conecta
- [x] 8.2 Implementar sincronización de operaciones: enviar creación/movimiento/edición vía WebSocket. Verificar que sincroniza
- [x] 8.3 Implementar recepción de operaciones de otros usuarios y actualización del canvas. Verificar que sincroniza
- [x] 8.4 Implementar cursor compartido: enviar posición del cursor y mostrar cursores de otros usuarios. Verificar que muestra

## 9. Frontend - Integración

- [x] 9.1 Actualizar `DiagramEditorPage` para usar el nuevo editor con React Flow, toolbar, panel de propiedades. Verificar que renderiza
- [x] 9.2 Conectar editor con REST API para cargar/guardar elementos y relaciones. Verificar flujo completo
- [x] 9.3 Conectar editor con WebSocket para colaboración en tiempo real. Verificar que funciona
