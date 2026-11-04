## Purpose

Reglas de acceso a proyectos y diagramas. El acceso solo se obtiene por ser creador o colaborador con invitación aceptada; conocer el enlace de un proyecto no concede acceso. Los permisos se validan en todos los canales (REST, WebSocket y enlaces compartidos).

## ADDED Requirements

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
