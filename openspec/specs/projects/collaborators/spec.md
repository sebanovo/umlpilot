## Purpose

Permite gestionar colaboradores en un proyecto — invitar usuarios, aceptar/rechazar invitaciones y cambiar roles.

## ADDED Requirements

### Requirement: Invitar colaborador
El sistema SHALL exponer un endpoint `POST /api/v1/projects/{id}/collaborators` que invite a un usuario por email con un rol (editor o viewer). Solo owner y editor pueden invitar.

#### Scenario: Invitación enviada
- **WHEN** se envía un POST con email de usuario existente y rol válido
- **THEN** el sistema retorna HTTP 201 con la invitación en estado "pending"

#### Scenario: Invitación notificada
- **WHEN** se invita por email a un usuario registrado
- **THEN** se crea además una notificación no leída en su bandeja (ver `notifications/inbox`)

#### Scenario: Usuario ya es colaborador
- **WHEN** se invita a un usuario que ya es colaborador del proyecto
- **THEN** el sistema retorna HTTP 409 con error

#### Scenario: Reinvitar tras rechazo
- **WHEN** se invita a un usuario que había rechazado la invitación
- **THEN** la colaboración vuelve a `pending` y se crea una nueva notificación

### Requirement: Listar colaboradores
El sistema SHALL exponer un endpoint `GET /api/v1/projects/{id}/collaborators` que retorne todos los colaboradores del proyecto.

#### Scenario: Colaboradores listados
- **WHEN** se envía un GET y el usuario es colaborador
- **THEN** el sistema retorna HTTP 200 con array de colaboradores (userId, email, name, role, status)

### Requirement: Aceptar invitación
El sistema SHALL exponer un endpoint `PUT /api/v1/projects/{id}/collaborators/accept` que acepte la invitación pendiente del usuario autenticado.

#### Scenario: Invitación aceptada
- **WHEN** el usuario tiene una invitación pendiente y la acepta
- **THEN** el estado cambia a "accepted" y el usuario accede al proyecto

### Requirement: Rechazar invitación
El sistema SHALL exponer un endpoint `PUT /api/v1/projects/{id}/collaborators/reject` que rechace la invitación.

#### Scenario: Invitación rechazada
- **WHEN** el usuario rechaza la invitación
- **THEN** el estado cambia a "rejected"

### Requirement: Cambiar rol de colaborador
El sistema SHALL exponer un endpoint `PUT /api/v1/projects/{id}/collaborators/{userId}` que cambie el rol de un miembro entre `editor` y `viewer`. Solo el owner puede cambiar roles.

#### Scenario: Rol cambiado
- **WHEN** el owner cambia el rol de un colaborador
- **THEN** el sistema actualiza el rol y retorna HTTP 200

#### Scenario: Editor intenta cambiar roles
- **WHEN** un colaborador con rol `editor` intenta cambiar el rol de otro miembro
- **THEN** el sistema rechaza la operación

#### Scenario: Rol del owner inmutable
- **WHEN** se intenta cambiar el rol del owner
- **THEN** el sistema rechaza la operación

#### Scenario: Invitar con rol owner
- **WHEN** se intenta invitar a un usuario con rol `owner`
- **THEN** el sistema rechaza la operación (solo `editor` o `viewer`)

### Requirement: Eliminar colaborador
El sistema SHALL exponer un endpoint `DELETE /api/v1/projects/{id}/collaborators/{userId}` que elimine a un colaborador. El owner puede eliminar a cualquier usuario; un editor puede eliminarse a sí mismo.

#### Scenario: Colaborador eliminado
- **WHEN** se envía DELETE con permisos adecuados
- **THEN** el colaborador se elimina del proyecto
