## Purpose

Permite gestionar atributos y métodos dentro de un elemento UML (clase, interfaz, enumeración).

## ADDED Requirements

### Requirement: Crear atributo
El sistema SHALL exponer un endpoint `POST /api/v1/projects/{projectId}/diagrams/{diagramId}/elements/{elementId}/attributes` que cree un atributo con nombre, tipo de dato, visibilidad, valor por defecto, y modifiers (static, final, transient, volatile).

#### Scenario: Atributo creado
- **WHEN** se envía POST con datos válidos
- **THEN** el sistema retorna HTTP 201 con el atributo creado (id, name, dataType, visibility, isStatic, isFinal, orderIndex)

#### Scenario: Nombre duplicado en mismo elemento
- **WHEN** se crea un atributo con nombre que ya existe en el elemento
- **THEN** el sistema retorna HTTP 400 con error de duplicado

### Requirement: Listar atributos
El sistema SHALL exponer un endpoint `GET /api/v1/projects/{projectId}/diagrams/{diagramId}/elements/{elementId}/attributes` que retorne los atributos ordenados por `orderIndex`.

### Requirement: Actualizar atributo
El sistema SHALL exponer un endpoint `PUT .../attributes/{attributeId}` que actualice cualquier campo del atributo.

### Requirement: Eliminar atributo
El sistema SHALL exponer un endpoint `DELETE .../attributes/{attributeId}` que elimine el atributo.

### Requirement: Crear método
El sistema SHALL exponer un endpoint `POST .../elements/{elementId}/methods` que cree un método con nombre, tipo de retorno, visibilidad, modifiers (static, abstract, final, constructor, synchronized, native), y cuerpo.

#### Scenario: Método creado
- **WHEN** se envía POST con datos válidos
- **THEN** el sistema retorna HTTP 201 con el método creado

### Requirement: Listar métodos
El sistema SHALL exponer un endpoint `GET .../methods` que retorne los métodos ordenados.

### Requirement: Actualizar método
El sistema SHALL exponer un endpoint `PUT .../methods/{methodId}` que actualice el método.

### Requirement: Eliminar método
El sistema SHALL exponer un endpoint `DELETE .../methods/{methodId}` que elimine el método y sus parámetros.

### Requirement: Gestionar parámetros de método
El sistema SHALL permitir crear, listar, actualizar y eliminar parámetros dentro de un método. Cada parámetro tiene nombre, tipo de dato, orden, y si es varargs, final, o tiene valor por defecto.

#### Scenario: Parámetro agregado a método
- **WHEN** se crea un parámetro en un método
- **THEN** el parámetro se agrega al final de la lista de parámetros del método
