## Purpose

Permite gestionar colaboradores en un proyecto — invitar usuarios, aceptar/rechazar invitaciones y cambiar roles.

## ADDED Requirements

### Requirement: Invitar colaborador
El sistema SHALL exponer un endpoint `POST /api/v1/projects/{id}/collaborators` que invite a un usuario por email con un rol (editor o viewer). Solo owner y editor pueden invitar.

#### Scenario: Invitación enviada
- **WHEN** se envía un POST con email de usuario existente y rol válido
- **THEN** el sistema retorna HTTP 201 con la invitación en estado "pending"

#### Scenario: Usuario ya es colaborador
- **WHEN** se invita a un usuario que ya es colaborador del proyecto
- **THEN** el sistema retorna HTTP 409 con error

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
El sistema SHALL exponer un endpoint `PUT /api/v1/projects/{id}/collaborators/{userId}` que cambie el rol. Solo el owner puede cambiar roles.

#### Scenario: Rol cambiado
- **WHEN** el owner cambia el rol de un colaborador
- **THEN** el sistema actualiza el rol y retorna HTTP 200

### Requirement: Eliminar colaborador
El sistema SHALL exponer un endpoint `DELETE /api/v1/projects/{id}/collaborators/{userId}` que elimine a un colaborador. El owner puede eliminar a cualquier usuario; un editor puede eliminarse a sí mismo.

#### Scenario: Colaborador eliminado
- **WHEN** se envía DELETE con permisos adecuados
- **THEN** el colaborador se elimina del proyecto
