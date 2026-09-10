## Why

UMLPilot necesita un editor de diagramas de clases funcional que permita a los usuarios crear, editar y colaborar en diagramas UML en tiempo real. Actualmente el editor es un placeholder sin canvas, sin elementos UML ni renderizado. La base de datos ya tiene el esquema completo para elementos, atributos, métodos y relaciones pero no hay código que lo implemente.

## What Changes

- **Backend**: Se añaden entidades de dominio para elementos UML (Element, Class, Interface, Enumeration, Attribute, Method, Parameter, Literal), relaciones (Relationship, Multiplicity), y sus repositorios/servicios/controladores. Se implementan eventos WebSocket para operaciones de elementos en tiempo real.
- **Frontend**: Se instala `@xyflow/react` para el canvas, se crean componentes UML (cajas de clase, interfaz, enumeración), panel de propiedades, toolbar de elementos, y se conecta con WebSocket STOMP para colaboración.
- **BREAKING**: Los endpoints de elementos requieren autenticación. El WebSocket se migra de raw a STOMP.

## Capabilities

### New Capabilities

- `uml-elements/crud`: CRUD de elementos UML (clases, interfaces, enumeraciones) dentro de un diagrama — crear, mover, editar propiedades, eliminar.
- `uml-elements/attributes-methods`: Gestionar atributos y métodos dentro de un elemento — añadir, editar, eliminar atributos, métodos y parámetros.
- `uml-elements/relationships`: CRUD de relaciones entre elementos — asociación, herencia, dependencia, agregación, composición, con multiplicidades.
- `diagram-editor/canvas`: Editor visual con canvas interactivo — arrastrar elementos, conectar con líneas, panel de propiedades, toolbar de herramientas.
- `diagram-editor/collaboration`: Colaboración en tiempo real — presencia de usuarios, sincronización de operaciones de elementos, conflictos.

### Modified Capabilities

- `collaboration/realtime`: Se extiende con eventos para operaciones de elementos (create, move, update, delete element/relationship).

## Impact

- **Backend**: 15+ nuevas entidades de dominio, repositorios, servicios, controladores. Nuevos eventos WebSocket. Refactorización del WebSocketHandler.
- **Frontend**: Nueva dependencia `@xyflow/react`. 10+ componentes React nuevos. Migración del WebSocket hook a STOMP client.
- **Base de datos**: Tablas `element`, `class`, `interface`, `enumeration`, `attribute`, `method`, `parameter`, `literal`, `relationship`, `multiplicity` ya definidas en el esquema SQL.
- **API**: Nuevos endpoints REST para elementos y relaciones. WebSocket events extendidos.
