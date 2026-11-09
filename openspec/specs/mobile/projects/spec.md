## Purpose

Cubre la gestión de proyectos, sus colaboradores y roles, y la bandeja de notificaciones con invitaciones que el usuario puede aceptar o rechazar desde el móvil.

## Requirements

### Requirement: Listar proyectos
La app SHALL listar los proyectos donde el usuario es creador o colaborador aceptado, mostrando nombre, descripción y fecha de creación.

#### Scenario: Proyectos disponibles
- **WHEN** el usuario entra a la pantalla de proyectos
- **THEN** ve solo sus proyectos accesibles, ordenados por fecha

#### Scenario: Sin proyectos
- **WHEN** el usuario no tiene proyectos
- **THEN** ve un estado vacío con la acción "Nuevo proyecto"

### Requirement: Crear proyecto
La app SHALL crear proyectos con nombre obligatorio y descripción opcional, y abrir su detalle al terminar.

#### Scenario: Creación correcta
- **WHEN** el usuario envía un nombre válido
- **THEN** se crea el proyecto y se abre su detalle

#### Scenario: Nombre vacío
- **WHEN** el usuario intenta crear sin nombre
- **THEN** se muestra la validación y no se envía la petición

### Requirement: Detalle de proyecto
La app SHALL mostrar el proyecto con sus diagramas y colaboradores, e indicar el rol del usuario actual.

#### Scenario: Detalle accesible
- **WHEN** el usuario abre un proyecto del que es miembro
- **THEN** ve diagramas, colaboradores y las acciones permitidas por su rol

#### Scenario: Sin acceso
- **WHEN** un usuario intenta abrir un proyecto del que no es miembro
- **THEN** se muestra que el proyecto no está disponible y no se expone su contenido

### Requirement: Editar y eliminar proyecto
La app SHALL permitir editar nombre/descripción a owner/editor y eliminar el proyecto solo al owner, con confirmación.

#### Scenario: Edición permitida
- **WHEN** un owner/editor guarda cambios válidos
- **THEN** el proyecto se actualiza

#### Scenario: Eliminación
- **WHEN** el owner confirma eliminar el proyecto
- **THEN** el proyecto se elimina y se vuelve al listado

#### Scenario: Viewer intenta editar
- **WHEN** un viewer intenta editar o eliminar
- **THEN** la acción no está disponible y el backend la rechaza

### Requirement: Listar colaboradores
La app SHALL mostrar los colaboradores con nombre, email, rol y estado de invitación (`pending`, `accepted`, `rejected`).

#### Scenario: Colaboradores listados
- **WHEN** el usuario abre la sección de colaboradores
- **THEN** ve cada miembro con su rol y estado

### Requirement: Invitar por email
La app SHALL buscar usuarios registrados por email o nombre (mínimo 2 caracteres) y enviar una invitación con rol `editor` o `viewer`.

#### Scenario: Búsqueda de usuarios
- **WHEN** el usuario escribe al menos 2 caracteres
- **THEN** ve hasta 10 coincidencias con nombre y email

#### Scenario: Invitación enviada
- **WHEN** se invita a un usuario registrado
- **THEN** la invitación queda pendiente y el invitado recibe una notificación no leída

#### Scenario: Usuario no registrado
- **WHEN** el email no corresponde a una cuenta
- **THEN** se muestra el error del backend y no se crea la invitación

### Requirement: Gestionar roles y eliminar colaboradores
La app SHALL permitir al owner cambiar el rol entre editor y lector, y eliminar colaboradores; el rol del owner MUST ser inmutable y un editor solo puede salir por sí mismo.

#### Scenario: Cambio de rol
- **WHEN** el owner cambia el rol de un miembro
- **THEN** el cambio se refleja y se aplica en el backend

#### Scenario: Eliminar colaborador
- **WHEN** el owner confirma eliminar a un colaborador
- **THEN** el colaborador pierde el acceso de inmediato

#### Scenario: Editor intenta gestionar a otro
- **WHEN** un editor intenta cambiar el rol o eliminar a otro miembro
- **THEN** la acción no está disponible y el backend la rechaza

### Requirement: Bandeja de notificaciones
La app SHALL mostrar una bandeja con las notificaciones del usuario, un contador de no leídas y actualización periódica o al enfocar la app.

#### Scenario: Nueva invitación
- **WHEN** el usuario recibe una invitación
- **THEN** el contador de no leídas se incrementa sin recargar la pantalla

#### Scenario: Bandeja vacía
- **WHEN** el usuario no tiene notificaciones
- **THEN** ve un estado vacío

### Requirement: Marcar notificaciones como leídas
La app SHALL permitir marcar una notificación o todas como leídas y actualizar el contador.

#### Scenario: Marcar todas
- **WHEN** el usuario pulsa "Marcar todas como leídas"
- **THEN** el contador queda en cero y las notificaciones se muestran leídas

### Requirement: Responder invitaciones desde la bandeja
La app SHALL permitir aceptar o rechazar una invitación pendiente desde la notificación; al aceptar, el proyecto aparece en el listado.

#### Scenario: Aceptar invitación
- **WHEN** el usuario acepta una invitación pendiente
- **THEN** la colaboración pasa a aceptada y el proyecto queda disponible

#### Scenario: Rechazar invitación
- **WHEN** el usuario rechaza una invitación
- **THEN** la invitación queda rechazada y no se concede acceso
