## Purpose

Permite crear, listar, actualizar y eliminar diagramas UML dentro de un proyecto, con control de bloqueo para edición.

## ADDED Requirements

### Requirement: Crear diagrama
El sistema SHALL exponer un endpoint `POST /api/v1/projects/{projectId}/diagrams` que cree un diagrama con nombre y tipo (class, sequence, use_case, activity, state). Solo owner y editor pueden crear.

#### Scenario: Diagrama creado
- **WHEN** se envía POST con nombre y tipo válidos
- **THEN** el sistema retorna HTTP 201 con el diagrama (id, name, type, version, canvasData: {}, createdAt)

#### Scenario: Tipo inválido
- **WHEN** se envía un tipo no soportado
- **THEN** el sistema retorna HTTP 400

### Requirement: Listar diagramas del proyecto
El sistema SHALL exponer un endpoint `GET /api/v1/projects/{projectId}/diagrams` que retorne todos los diagramas del proyecto.

#### Scenario: Diagramas listados
- **WHEN** se envía GET y el usuario es colaborador
- **THEN** el sistema retorna HTTP 200 con array de diagramas

### Requirement: Obtener diagrama por ID
El sistema SHALL exponer un endpoint `GET /api/v1/projects/{projectId}/diagrams/{id}` que retorne el diagrama completo con sus elementos y relaciones.

#### Scenario: Diagrama encontrado
- **WHEN** se envía GET y el usuario es colaborador
- **THEN** el sistema retorna HTTP 200 con el diagrama, elementos y relaciones

### Requirement: Actualizar diagrama
El sistema SHALL exponer un endpoint `PUT /api/v1/projects/{projectId}/diagrams/{id}` que actualice nombre, descripción y canvasData. El diagrama debe estar desbloqueado o bloqueado por el usuario que actualiza.

#### Scenario: Diagrama actualizado
- **WHEN** se envía PUT con datos válidos y el diagrama no está bloqueado por otro usuario
- **THEN** el sistema retorna HTTP 200 con el diagrama actualizado y versión incrementada

#### Scenario: Diagrama bloqueado por otro
- **WHEN** otro usuario tiene el diagrama bloqueado
- **THEN** el sistema retorna HTTP 409 con error de conflicto

### Requirement: Bloquear diagrama para edición
El sistema SHALL exponer un endpoint `POST /api/v1/projects/{projectId}/diagrams/{id}/lock` que bloquee el diagrama para el usuario autenticado.

#### Scenario: Diagrama bloqueado
- **WHEN** el diagrama no está bloqueado y se solicita bloqueo
- **THEN** el sistema marca el diagrama como bloqueado por el usuario

#### Scenario: Ya bloqueado por otro
- **WHEN** otro usuario ya lo tiene bloqueado
- **THEN** el sistema retorna HTTP 409

### Requirement: Desbloquear diagrama
El sistema SHALL exponer un endpoint `POST /api/v1/projects/{projectId}/diagrams/{id}/unlock` que desbloquee el diagrama.

#### Scenario: Diagrama desbloqueado
- **WHEN** el diagrama está bloqueado por el usuario autenticado
- **THEN** el sistema desbloquea el diagrama

### Requirement: Eliminar diagrama
El sistema SHALL exponer un endpoint `DELETE /api/v1/projects/{projectId}/diagrams/{id}`. Solo owner y editor pueden eliminar.

#### Scenario: Diagrama eliminado
- **WHEN** se envía DELETE con permisos adecuados
- **THEN** el sistema retorna HTTP 204
