## Purpose

Permite crear, editar, mover y eliminar elementos UML (clases, interfaces, enumeraciones) dentro de un diagrama de clases.

## ADDED Requirements

### Requirement: Crear elemento UML
El sistema SHALL exponer un endpoint `POST /api/v1/projects/{projectId}/diagrams/{diagramId}/elements` que cree un elemento con tipo (class, interface, enumeration), nombre, posición (x, y) y dimensiones (width, height).

#### Scenario: Clase creada
- **WHEN** se envía POST con tipo "class", nombre y posición
- **THEN** el sistema retorna HTTP 201 con el elemento creado (id, type, name, x, y, width, height, visibility)

#### Scenario: Elemento en diagrama bloqueado por otro
- **WHEN** el diagrama está bloqueado por otro usuario
- **THEN** el sistema retorna HTTP 409

### Requirement: Listar elementos del diagrama
El sistema SHALL exponer un endpoint `GET /api/v1/projects/{projectId}/diagrams/{diagramId}/elements` que retorne todos los elementos del diagrama.

#### Scenario: Elementos listados
- **WHEN** se envía GET y el usuario tiene acceso
- **THEN** el sistema retorna HTTP 200 con array de elementos

### Requirement: Actualizar elemento UML
El sistema SHALL exponer un endpoint `PUT /api/v1/projects/{projectId}/diagrams/{diagramId}/elements/{elementId}` que actualice nombre, posición, dimensiones o visibilidad.

#### Scenario: Elemento actualizado
- **WHEN** se envía PUT con datos válidos
- **THEN** el sistema retorna HTTP 200 con el elemento actualizado

### Requirement: Eliminar elemento UML
El sistema SHALL exponer un endpoint `DELETE /api/v1/projects/{projectId}/diagrams/{diagramId}/elements/{elementId}` que elimine el elemento y sus hijos (atributos, métodos, relaciones asociadas).

#### Scenario: Elemento eliminado
- **WHEN** se envía DELETE
- **THEN** el sistema retorna HTTP 204 y elimina el elemento en cascada

### Requirement: Mover elemento
El sistema SHALL permitir actualizar la posición (x, y) de un elemento.

#### Scenario: Elemento movido
- **WHEN** se actualiza la posición de un elemento
- **THEN** la nueva posición se persiste y se notifica a otros usuarios vía WebSocket

### Requirement: Cambiar visibilidad
El sistema SHALL permitir cambiar la visibilidad de un elemento (public, private, protected, package).

#### Scenario: Visibilidad cambiada
- **WHEN** se actualiza la visibilidad
- **THEN** la nueva visibilidad se persiste
