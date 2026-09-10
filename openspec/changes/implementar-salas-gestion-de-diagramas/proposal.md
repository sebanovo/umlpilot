## Why

UMLPilot necesita un sistema de salas para que los usuarios puedan crear proyectos, invitar colaboradores y editar diagramas UML en tiempo real. Actualmente solo existe autenticación de usuarios pero no hay gestión de proyectos ni colaboración. Este cambio establece la base para todo el editor colaborativo.

## What Changes

- **Backend**: Se añaden entidades Project, Collaborator y Diagram con sus repositorios, servicios y controladores REST. Se configura WebSocket para colaboración en tiempo real.
- **Frontend**: Se añaden páginas de dashboard (listar proyectos), detalle de proyecto (listar diagramas) y editor de diagramas. Se implementa conexión WebSocket para presencia y cambios en tiempo real.
- **BREAKING**: Los endpoints de proyecto, colaborador y diagrama requieren autenticación JWT.

## Capabilities

### New Capabilities

- `projects/crud`: Crear, listar, actualizar y eliminar proyectos. Cada proyecto tiene nombre, descripción, estado y un creador.
- `projects/collaborators`: Gestionar colaboradores de un proyecto — invitar usuarios, aceptar/rechazar invitaciones, cambiar roles (owner, editor, viewer).
- `diagrams/crud`: Crear, listar, actualizar y eliminar diagramas dentro de un proyecto. Cada diagrama tiene nombre, tipo (clase, secuencia, etc.), datos del canvas y control de bloqueo.
- `collaboration/realtime`: WebSocket para colaboración en tiempo real — presencia de usuarios, sincronización de cambios de diagrama, bloqueo de diagrama.

### Modified Capabilities

<!-- Ninguna capability existente cambia requisitos -->

## Impact

- **Backend**: Nuevas entidades JPA (Project, Collaborator, Diagram), repositorios, servicios, controladores. Nuevo dependencia `spring-boot-starter-websockets`. Configuración WebSocket en SecurityConfig.
- **Frontend**: Nuevas páginas (DashboardPage, ProjectDetailPage, DiagramEditorPage). Nuevo WebSocket client. Nuevos componentes UI.
- **Base de datos**: Tablas `project`, `collaborator`, `diagram` ya definidas en el esquema SQL.
- **API**: Nuevos endpoints REST bajo `/api/v1/projects/**` y `/api/v1/diagrams/**`. WebSocket en `/ws`.
