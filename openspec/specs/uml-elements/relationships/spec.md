## Purpose

Permite crear, editar y eliminar relaciones entre elementos UML con multiplicidades.

## ADDED Requirements

### Requirement: Crear relación
El sistema SHALL exponer un endpoint `POST /api/v1/projects/{projectId}/diagrams/{diagramId}/relationships` que cree una relación con tipo (association, generalization, dependency, aggregation, composition, realization), elemento origen, elemento destino, nombre y dirección.

#### Scenario: Relación creada
- **WHEN** se envía POST con sourceElementId, targetElementId y relationshipType válidos
- **THEN** el sistema retorna HTTP 201 con la relación creada

#### Scenario: Elemento origen o destino no existe
- **WHEN** se referencia un elemento que no existe en el diagrama
- **THEN** el sistema retorna HTTP 400

### Requirement: Listar relaciones del diagrama
El sistema SHALL exponer un endpoint `GET .../relationships` que retorne todas las relaciones del diagrama.

### Requirement: Actualizar relación
El sistema SHALL exponer un endpoint `PUT .../relationships/{relationshipId}` que actualice nombre, dirección o tipo.

### Requirement: Eliminar relación
El sistema SHALL exponer un endpoint `DELETE .../relationships/{relationshipId}` que elimine la relación y sus multiplicidades.

### Requirement: Gestionar multiplicidades
El sistema SHALL permitir gestionar multiplicidades en los extremos de una relación (source, target). Cada multiplicidad tiene min, max, isOrdered, isUnique.

#### Scenario: Multiplicidad 1..*
- **WHEN** se configura multiplicidad source min=1, max=*
- **THEN** la relación muestra "1..*" en el extremo origen

#### Scenario: Multiplicidad por defecto
- **WHEN** se crea una relación sin multiplicidades explícitas
- **THEN** se asigna multiplicidad 1..1 en ambos extremos
