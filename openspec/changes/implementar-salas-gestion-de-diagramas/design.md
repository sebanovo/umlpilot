## Context

El backend tiene solo User entity + JWT auth. Las tablas project, collaborator, diagram están definidas en el esquema SQL pero no implementadas. El frontend tiene Login, Register y HomePage. No hay WebSocket ni colaboración.

## Goals / Non-Goals

**Goals:**
- CRUD completo de proyectos, colaboradores y diagramas
- Colaboración en tiempo real con WebSocket
- Control de bloqueo de diagramas
- Presencia de usuarios conectados

**Non-Goals:**
- Operational Transform / CRDT (usar bloqueo simple)
- Chat en tiempo real (futuro)
- AI commands (futuro)
- Code generation (futuro)
- XMI export/import (futuro)

## Decisions

### 1. Layout plano existente (no hexagonal)
**Por qué**: Migrar a hexagonal ahora bloquearía la funcionalidad. Se migrará después.
**Alternativa**: Migrar ahora — demasiado trabajo para poco valor funcional.

### 2. IDs como String (UUID)
**Por qué**: El esquema SQL usa VARCHAR(36). JPA con `@GeneratedValue(strategy = GenerationType.UUID)` genera UUIDs automáticamente.

### 3. WebSocket con Spring WebSocket + STOMP
**Por qué**: Integración nativa con Spring, soporte para salas (topics), fácil autenticación.
**Alternativa**: Socket.io — requiere librería adicional y adaptador Node.js.

### 4. Canvas data como JSON en diagrama
**Por qué**: El estado del canvas se almacena como JSON en `canvas_data`. Los elementos y relaciones son tablas separadas para consultas eficientes.

### 5. Diagram locking para concurrencia
**Por qué**: Sin OT/CRDT, el bloqueo previene conflictos. Un usuario bloquea, edita, desbloquea.
**Alternativa**: CRDT — complejidad excesiva para esta fase.

### 6. DTOs separados de entidades JPA
**Por qué**: Separa el contrato de API del modelo de persistencia.

## Risks / Trade-offs

- **[Bloqueo puede causar espera]** → Mitigación: timeout de bloqueo automático después de 30 min
- **[WebSocket sin auth inicial]** → Mitigación: JWT en query param del handshake
- **[Canvas data puede crecer mucho]** → Mitigación: límite de tamaño, compresión futura
- **[Sin history de cambios]** → Mitigación: change_history se implementará después
