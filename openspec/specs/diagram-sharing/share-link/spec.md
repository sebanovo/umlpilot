## Purpose

Compartir diagramas como en Figma, con dos vías independientes:
1. **Cualquiera con el enlace**: la persona puede previsualizar el diagrama (sin cuenta) y, al iniciar sesión, **unirse al proyecto** con el rol que defina el enlace (lector o editor). El enlace no añade miembros por sí solo.
2. **Invitar por correo**: el owner/editor invita por email a un usuario registrado; recibe la propuesta en su bandeja de notificaciones para aceptar o rechazar.

## Requirements

### Requirement: Enlace con permiso configurable
El sistema SHALL exponer `GET|POST|DELETE /api/v1/projects/{projectId}/diagrams/{diagramId}/share`. Solo `owner`/`editor` pueden gestionarlo. El enlace tiene un rol `viewer` (por defecto) o `editor`, configurable al activarlo o después.

#### Scenario: Activar
- **WHEN** un owner/editor activa el enlace con un rol
- **THEN** se crea (o reactiva) un token único y se devuelve `{ enabled, token, url, role }`

#### Scenario: Cambiar permiso
- **WHEN** el owner/editor cambia el rol del enlace
- **THEN** el token se mantiene y las siguientes uniones usan el nuevo rol

#### Scenario: Desactivar
- **WHEN** el owner/editor desactiva el enlace
- **THEN** el token deja de resolver la vista pública y de permitir uniones

### Requirement: Vista previa pública
El sistema SHALL exponer `GET /api/v1/public/diagrams/{token}` sin autenticación, devolviendo el proyecto, el diagrama, su rol de enlace y todo el contenido (elementos, relaciones, atributos, métodos, literales y multiplicidades). Es de solo lectura.

#### Scenario: Enlace válido
- **WHEN** alguien abre `/share/{token}`
- **THEN** ve el diagrama renderizado en modo vista previa

#### Scenario: Enlace inválido o desactivado
- **WHEN** el token no existe o fue desactivado
- **THEN** la vista indica que el enlace no está disponible

### Requirement: Unirse con el enlace
El sistema SHALL exponer `POST /api/v1/public/diagrams/{token}/join` (requiere autenticación). La unión es idempotente y crea una colaboración `accepted` con el rol del enlace.

#### Scenario: Usuario nuevo
- **WHEN** un usuario autenticado se une con el enlace
- **THEN** se crea la colaboración `accepted` con el rol del enlace y accede al proyecto

#### Scenario: Ya es miembro
- **WHEN** un miembro existente abre y usa el enlace
- **THEN** conserva su rol actual (no se degrada)

#### Scenario: Invitación pendiente
- **WHEN** el usuario tenía una invitación `pending`
- **THEN** se acepta manteniendo el rol de la invitación

#### Scenario: Invitación rechazada
- **WHEN** el usuario había rechazado la invitación y se une con el enlace
- **THEN** se reincorpora como `accepted` con el rol del enlace

### Requirement: Invitar por correo desde el diálogo
El diálogo de compartir SHALL permitir invitar por email a usuarios registrados (rol `editor` o `viewer`), creando una colaboración `pending` y una notificación no leída en la bandeja del invitado. Invitar no comparte el enlace.

### Requirement: Roles en el editor
El editor SHALL mostrarse en modo solo lectura para `viewer`: sin caja de herramientas, paneles de edición ni acciones de mutación; conserva la exportación XMI.

#### Scenario: Viewer abre el editor
- **WHEN** un `viewer` abre un diagrama tras unirse con un enlace de lector
- **THEN** ve la insignia "Solo lectura" y no puede modificar el contenido
