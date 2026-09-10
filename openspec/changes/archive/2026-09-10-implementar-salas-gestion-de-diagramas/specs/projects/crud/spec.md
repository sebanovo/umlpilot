## Purpose

Permite a los usuarios crear, listar, actualizar y eliminar proyectos que contienen diagramas UML.

## ADDED Requirements

### Requirement: Crear proyecto
El sistema SHALL exponer un endpoint `POST /api/v1/projects` que cree un proyecto nuevo con nombre, descripción. El usuario autenticado será el creador y owner del proyecto.

#### Scenario: Proyecto creado exitosamente
- **WHEN** se envía un POST a `/api/v1/projects` con nombre y descripción válidos
- **THEN** el sistema retorna HTTP 201 con los datos del proyecto creado (id, name, description, status, creatorId, createdAt)

#### Scenario: Nombre vacío
- **WHEN** se envía un POST sin nombre
- **THEN** el sistema retorna HTTP 400 con error de validación

### Requirement: Listar proyectos del usuario
El sistema SHALL exponer un endpoint `GET /api/v1/projects` que retorne todos los proyectos donde el usuario autenticado es colaborador (owner, editor o viewer).

#### Scenario: Proyectos listados
- **WHEN** se envía un GET a `/api/v1/projects`
- **THEN** el sistema retorna HTTP 200 con un array de proyectos

### Requirement: Obtener proyecto por ID
El sistema SHALL exponer un endpoint `GET /api/v1/projects/{id}` que retorne un proyecto específico con sus colaboradores.

#### Scenario: Proyecto encontrado
- **WHEN** se envía un GET a `/api/v1/projects/{id}` y el usuario es colaborador
- **THEN** el sistema retorna HTTP 200 con el proyecto y lista de colaboradores

#### Scenario: Proyecto no encontrado
- **WHEN** el proyecto no existe o el usuario no es colaborador
- **THEN** el sistema retorna HTTP 404

### Requirement: Actualizar proyecto
El sistema SHALL exponer un endpoint `PUT /api/v1/projects/{id}` que actualice nombre y descripción. Solo el owner o editor pueden actualizar.

#### Scenario: Proyecto actualizado
- **WHEN** se envía un PUT con datos válidos y el usuario tiene permisos
- **THEN** el sistema retorna HTTP 200 con el proyecto actualizado

### Requirement: Eliminar proyecto
El sistema SHALL exponer un endpoint `DELETE /api/v1/projects/{id}` que elimine el proyecto. Solo el owner puede eliminar.

#### Scenario: Proyecto eliminado
- **WHEN** se envía un DELETE y el usuario es owner
- **THEN** el sistema retorna HTTP 204
