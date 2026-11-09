## Purpose

Reglas de acceso a proyectos y diagramas. El acceso solo se obtiene por ser creador o colaborador con invitación aceptada; conocer el enlace de un proyecto no concede acceso. Los permisos se validan en todos los canales (REST, WebSocket y enlaces compartidos).

## Requirements

### Requirement: Acceso solo por membresía aceptada
El sistema SHALL permitir leer un proyecto o diagrama únicamente al creador o a un colaborador con `invitation_status = accepted`.

#### Scenario: Usuario eliminado
- **WHEN** un colaborador es eliminado del proyecto e intenta abrir el proyecto o el diagrama
- **THEN** el sistema rechaza la petición (no lo re-agrega automáticamente)

#### Scenario: Usuario con el enlace sin invitación
- **WHEN** un usuario autenticado que no es miembro abre el proyecto por su URL
- **THEN** el sistema rechaza la petición

### Requirement: Roles de lectura y edición
El sistema SHALL permitir editar solo a `owner` y `editor`. Un `viewer` SHALL poder leer el proyecto y sus diagramas, pero no crear, actualizar ni eliminar contenido ni usar funciones que mutan el diagrama.

#### Scenario: Viewer intenta editar
- **WHEN** un `viewer` intenta crear/editar/eliminar elementos, relaciones, diagramas o importar/construir con IA
- **THEN** el sistema responde 403

### Requirement: Validación de pertenencia del diagrama
El sistema SHALL comprobar que el diagrama pertenece al proyecto indicado en la ruta antes de operar sobre él.

#### Scenario: IDs de proyectos distintos
- **WHEN** se envía un `diagramId` que pertenece a otro proyecto
- **THEN** el sistema rechaza la operación

### Requirement: Acceso en tiempo real
El canal WebSocket SHALL validar la membresía al unirse a un diagrama y no emitir eventos de usuarios que no pertenecen a la sala.

#### Scenario: Unirse sin acceso
- **WHEN** un usuario sin acceso intenta unirse a la sala del diagrama
- **THEN** no se agrega a la sala ni recibe presencia/cambios

### Requirement: Acceso al chat por membresía aceptada
El sistema SHALL permitir leer y escribir en el chat grupal de un proyecto únicamente al creador o a un colaborador con `invitation_status = accepted`, validando la membresía en PostgreSQL en cada operación.

#### Scenario: Miembro aceptado
- **WHEN** un miembro aceptado lee o envía un mensaje en el chat del proyecto
- **THEN** la operación se permite

#### Scenario: Usuario sin membresía
- **WHEN** un usuario autenticado sin membresía intenta leer o enviar en el chat
- **THEN** el sistema responde 403 y no revela mensajes

#### Scenario: Invitación pendiente
- **WHEN** un usuario con invitación pendiente intenta acceder al chat
- **THEN** el sistema rechaza la operación hasta que acepte la invitación

### Requirement: Acciones de IA sujetas a rol
El sistema SHALL permitir que la IA ejecute acciones que mutan el diagrama solo cuando el usuario que originó el mensaje sea `owner` o `editor`; un `viewer` SHALL poder conversar sin mutar el diagrama.

#### Scenario: Owner o editor
- **WHEN** un `owner` o `editor` pide a la IA modificar el diagrama desde el chat
- **THEN** la IA puede ejecutar las tools de mutación permitidas

#### Scenario: Viewer
- **WHEN** un `viewer` pide a la IA modificar el diagrama desde el chat
- **THEN** la IA no ejecuta mutaciones y responde explicando la restricción

### Requirement: Autorización del canal de chat en tiempo real
El canal STOMP SHALL validar la membresía al suscribirse y al enviar eventos del chat, y no SHALL emitir mensajes de un proyecto a usuarios que no pertenecen a él.

#### Scenario: Suscripción sin acceso
- **WHEN** un usuario sin acceso intenta suscribirse al chat de un proyecto
- **THEN** no se agrega al tópico ni recibe mensajes

#### Scenario: Envío sin acceso
- **WHEN** un usuario sin acceso intenta publicar un mensaje de chat
- **THEN** el sistema descarta la operación y no persiste ni difunde el mensaje

### Requirement: Revocación de acceso al chat
El sistema SHALL revocar el acceso al chat de inmediato cuando un colaborador es eliminado o cuando el proyecto se elimina.

#### Scenario: Colaborador eliminado
- **WHEN** un colaborador es eliminado del proyecto
- **THEN** sus siguientes lecturas, escrituras y suscripciones al chat son rechazadas

#### Scenario: Proyecto eliminado
- **WHEN** el owner elimina el proyecto
- **THEN** el chat del proyecto deja de ser accesible para todos los usuarios
