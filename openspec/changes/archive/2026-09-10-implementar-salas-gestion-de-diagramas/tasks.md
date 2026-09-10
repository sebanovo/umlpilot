## 1. Backend - Dependencias y entidades

- [x] 1.1 Agregar dependencia `spring-boot-starter-websockets` al `pom.xml` y verificar que `mvnw compile` compila
- [x] 1.2 Crear entidad `Project` con campos: id (UUID), name, description, creatorId, status, createdAt, updatedAt. Verificar que compila
- [x] 1.3 Crear entidad `Collaborator` con campos compuestos (projectId, userId), role, invitationStatus, timestamps. Verificar que compila
- [x] 1.4 Crear entidad `Diagram` con campos: id (UUID), projectId, name, type, description, canvasData (JSON), version, isLocked, lockedByUserId, timestamps. Verificar que compila

## 2. Backend - Repositorios y DTOs

- [x] 2.1 Crear repositorios: `ProjectRepository`, `CollaboratorRepository`, `DiagramRepository` con queries necesarias. Verificar que compila
- [x] 2.2 Crear DTOs de request/response para Projects, Collaborators y Diagrams. Verificar que compila

## 3. Backend - Servicios

- [x] 3.1 Crear `ProjectService` con CRUD de proyectos y validación de permisos. Verificar que compila
- [x] 3.2 Crear `CollaboratorService` con invitar, aceptar, rechazar, cambiar rol, eliminar. Verificar que compila
- [x] 3.3 Crear `DiagramService` con CRUD, bloqueo/desbloqueo de diagramas. Verificar que compila

## 4. Backend - Controladores REST

- [x] 4.1 Crear `ProjectController` con endpoints CRUD bajo `/api/v1/projects`. Verificar con curl
- [x] 4.2 Crear `CollaboratorController` con endpoints bajo `/api/v1/projects/{id}/collaborators`. Verificar con curl
- [x] 4.3 Crear `DiagramController` con endpoints CRUD + lock/unlock bajo `/api/v1/projects/{id}/diagrams`. Verificar con curl

## 5. Backend - WebSocket

- [x] 5.1 Crear configuración WebSocket con STOMP, handler de conexiones y autenticación JWT. Verificar que compila
- [x] 5.2 Crear servicio de salas (rooms) que maneje join/leave, presencia y broadcasting de eventos. Verificar que compila
- [x] 5.3 Actualizar SecurityConfig para permitir conexiones WebSocket. Verificar que compila

## 6. Frontend - API client y páginas base

- [x] 6.1 Crear API client para proyectos (fetch, create, update, delete). Verificar que compila
- [x] 6.2 Crear `DashboardPage` que liste proyectos del usuario con botón crear. Verificar que renderiza
- [x] 6.3 Crear `ProjectDetailPage` que muestre info del proyecto, colaboradores y diagramas. Verificar que renderiza

## 7. Frontend - Editor y WebSocket

- [x] 7.1 Crear `DiagramEditorPage` con canvas básico y panel de elementos UML. Verificar que renderiza
- [x] 7.2 Implementar WebSocket client para conexión a `/ws`, join/leave de salas. Verificar que conecta
- [x] 7.3 Conectar editor con WebSocket para recibir/enviar cambios de elementos en tiempo real. Verificar que sincroniza

## 8. Frontend - Integración

- [x] 8.1 Actualizar App.tsx con rutas: `/projects`, `/projects/:id`, `/projects/:id/diagrams/:id`. Verificar que compila
- [x] 8.2 Conectar todas las páginas con API client y WebSocket. Verificar flujo completo: crear proyecto → crear diagrama → abrir editor → colaborar
