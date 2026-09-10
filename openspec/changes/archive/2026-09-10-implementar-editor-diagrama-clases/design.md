## Context

El backend ya tiene auth, proyectos, diagramas (como contenedores), colaboradores y WebSocket rooms. El frontend tiene auth, dashboard, y un placeholder de editor. La base de datos define tablas completas para elementos UML pero no hay código Java ni React que las implemente. El WebSocket usa raw WS en frontend pero STOMP en backend.

## Goals / Non-Goals

**Goals:**
- Editor visual de diagramas de clases con canvas interactivo
- CRUD de elementos UML (clases, interfaces, enumeraciones)
- Atributos, métodos y parámetros en cada elemento
- Relaciones entre elementos con multiplicidades
- Colaboración en tiempo real con sincronización de operaciones
- Cursor compartido y selección compartida

**Non-Goals:**
- Diagramas de secuencia, casos de uso, actividad, estado (futuro)
- AI commands / code generation (futuro)
- XMI export/import (futuro)
- Operational Transform / CRDT (usar locking + broadcast simple)
- Undo/redo (futuro)

## Decisions

### 1. Canvas: @xyflow/react (React Flow)
**Por qué**: Librería de canvas moderna, soporta nodos personalizados, edges personalizados, zoom/pan, drag and drop, TypeScript first. Comunidad activa.
**Alternativa**: JointJS — más pesada, menos flexible para custom rendering. Konva/Pixi — demasiado bajo nivel.

### 2. Elementos UML como nodos customizados de React Flow
**Por qué**: Cada tipo de elemento UML (clase, interfaz, enumeración) será un React Flow custom node con su propio renderizado SVG/HTML.
**Estructura**: Un `ClassNode`, `InterfaceNode`, `EnumNode` que renderizan cajas UML con 3 secciones.

### 3. Relaciones como edges customizados de React Flow
**Por qué**: React Flow permite edges personalizados con estilos SVG, etiquetas, y multiplicity labels.
**Tipos**: `AssociationEdge`, `GeneralizationEdge`, `DependencyEdge`, `AggregationEdge`, `CompositionEdge`, `RealizationEdge`.

### 4. Estado local + WebSocket broadcast
**Por qué**: El canvas mantiene estado local para responsividad. Las operaciones se envían al backend vía WebSocket y se broadcastean a otros usuarios.
**Flujo**: Usuario mueve elemento → estado local actualizado → POST al backend → WebSocket broadcast → otros usuarios actualizan su canvas.

### 5. Backend: Elementos como tablas separadas (no JSON blob)
**Por qué**: Las tablas ya existen en el esquema SQL. Permite queries eficientes, integridad referencial, y auditoría.
**Relación**: `element` (base) → `class`, `interface`, `enumeration` (herencia JPA con @OneToOne)

### 6. WebSocket: STOMP client en frontend
**Por qué**: El backend ya configura STOMP. Usar `@stomp/stompjs` o `sockjs-client` + `stompjs` para alinear protocolos.

### 7. Persistencia de canvas: React Flow → JSON → canvas_data
**Por qué**: React Flow serializa el estado del canvas a JSON. Se guarda como `canvas_data` en la tabla `diagram` para persistir posiciones.

## Risks / Trade-offs

- **[Performance con muchos elementos]** → Mitigación: virtualización del canvas, lazy loading de elementos
- **[Conflictos de edición simultánea]** → Mitigación: diagram locking + broadcast de operaciones
- **[Complejidad de UML rendering]** → Mitigación: empezar con clases simples, iterar sobre komplejidad
- **[Tamaño del bundle con React Flow]** → Mitigación: tree shaking, lazy loading del editor
