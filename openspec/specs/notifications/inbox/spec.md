## Purpose

Bandeja de notificaciones persistente para avisar al usuario de propuestas de colaboración. No usa push ni websockets: el cliente consulta el contador por polling ligero y al enfocar la ventana. Cada notificación puede marcarse como leída y, si es una invitación, responderse (aceptar/rechazar) desde la propia bandeja.

## ADDED Requirements

### Requirement: Bandeja de notificaciones
El sistema SHALL exponer `GET /api/v1/notifications` que devuelva las notificaciones del usuario autenticado, más recientes primero, incluyendo `type`, `title`, `message`, `projectId`, `role`, `read`, `createdAt` e `invitationStatus`.

#### Scenario: Notificaciones listadas
- **WHEN** el usuario autenticado consulta su bandeja
- **THEN** recibe solo sus notificaciones, con el estado de lectura y el estado actual de la invitación

#### Scenario: Solo no leídas
- **WHEN** se consulta con `unreadOnly=true`
- **THEN** solo se devuelven las notificaciones sin leer

### Requirement: Contador de no leídas
El sistema SHALL exponer `GET /api/v1/notifications/unread-count` que devuelva `{ "count": n }`.

### Requirement: Marcar como leída
El sistema SHALL exponer `PUT /api/v1/notifications/{id}/read` y `PUT /api/v1/notifications/read-all`.

#### Scenario: Marcar una notificación
- **WHEN** el usuario marca una notificación propia
- **THEN** se registra `readAt` y deja de contar como no leída

#### Scenario: Notificación ajena
- **WHEN** se intenta marcar una notificación de otro usuario
- **THEN** responde con error y no la modifica

### Requirement: Responder invitación desde la bandeja
El sistema SHALL exponer `POST /api/v1/notifications/{id}/respond` con `{ "action": "accept" | "reject" }`. La acción actualiza el colaborador correspondiente y marca la notificación como leída.

#### Scenario: Aceptar
- **WHEN** el usuario acepta una invitación pendiente desde su bandeja
- **THEN** la colaboración pasa a `accepted`, el proyecto aparece en sus proyectos y la notificación queda leída

#### Scenario: Rechazar
- **WHEN** el usuario rechaza la invitación
- **THEN** la colaboración pasa a `rejected` y la notificación queda leída

### Requirement: La invitación por email crea una notificación
Al invitar por email a un usuario registrado, el sistema SHALL crear una notificación no leída en su bandeja con el nombre del proyecto, el rol y el nombre del invitador.

#### Scenario: Invitación notificada
- **WHEN** un owner/editor invita a un usuario registrado
- **THEN** la invitación queda en `pending` y el invitado recibe una notificación no leída

#### Scenario: Reinvitar tras rechazo
- **WHEN** se invita de nuevo a un usuario que había rechazado
- **THEN** la colaboración vuelve a `pending` con el nuevo rol y se crea una nueva notificación

#### Scenario: Invitación duplicada
- **WHEN** ya existe una invitación `pending` o una colaboración `accepted` para ese usuario
- **THEN** el sistema rechaza la operación con un error claro

### Requirement: Búsqueda de usuarios para invitar
El sistema SHALL exponer `GET /api/v1/users/search?q=` que busque por email o nombre (mínimo 2 caracteres), excluya al usuario actual y limite los resultados. Las credenciales y datos sensibles no se exponen.

#### Scenario: Búsqueda por coincidencia
- **WHEN** el usuario escribe al menos 2 caracteres
- **THEN** recibe hasta 10 usuarios con `id`, `email`, `firstName` y `lastName`
