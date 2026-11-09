## Purpose

Permite compartir un diagrama con las dos opciones de Figma: un enlace con rol configurable que permite previsualizar y unirse al proyecto, o una invitación por email con notificación para aceptar o rechazar.

## ADDED Requirements

### Requirement: Consultar el estado del enlace
La app SHALL mostrar si el enlace público está activo, su rol (`viewer` o `editor`) y la URL compartible. Solo owner/editor pueden consultarlo.

#### Scenario: Enlace activo
- **WHEN** un owner/editor abre el diálogo de compartir
- **THEN** ve la URL, el rol y la acción para desactivarlo

#### Scenario: Enlace inactivo
- **WHEN** el enlace no está activo
- **THEN** ve la acción para activarlo con el rol por defecto (lector)

### Requirement: Activar, desactivar y cambiar el rol del enlace
La app SHALL permitir activar, desactivar y cambiar el rol del enlace entre lector y editor. El token MUST mantenerse al cambiar el rol y dejar de funcionar al desactivarlo.

#### Scenario: Activar
- **WHEN** el owner/editor activa el enlace
- **THEN** se genera una URL única y se muestra para copiar o compartir

#### Scenario: Cambiar rol
- **WHEN** el owner/editor cambia el rol del enlace
- **THEN** las siguientes uniones usan el nuevo rol y la URL no cambia

#### Scenario: Desactivar
- **WHEN** el owner/editor desactiva el enlace
- **THEN** la vista previa y la unión dejan de funcionar con ese token

### Requirement: Compartir el enlace
La app SHALL permitir copiar la URL y usar el diálogo nativo de compartir del sistema.

#### Scenario: Compartir
- **WHEN** el usuario pulsa "Compartir"
- **THEN** se abre el selector nativo con la URL del enlace

### Requirement: Vista previa pública
La app SHALL renderizar `/share/{token}` sin autenticación en modo solo lectura, mostrando el nombre del proyecto/diagrama y el rol que otorga el enlace.

#### Scenario: Enlace válido
- **WHEN** alguien abre un enlace activo
- **THEN** ve el diagrama en vista previa y las opciones de unirse o iniciar sesión

#### Scenario: Enlace inválido
- **WHEN** el token no existe o fue desactivado
- **THEN** se muestra que el enlace no está disponible

### Requirement: Unirse con el enlace
La app SHALL permitir que un usuario autenticado se una al proyecto con el rol del enlace mediante una acción explícita. La unión MUST ser idempotente y MUST NOT degradar el rol de un miembro existente.

#### Scenario: Usuario nuevo
- **WHEN** un usuario autenticado pulsa "Unirme"
- **THEN** se crea su membresía con el rol del enlace y se abre el diagrama

#### Scenario: Ya es miembro
- **WHEN** un miembro existente usa el enlace
- **THEN** conserva su rol actual

#### Scenario: Invitación pendiente
- **WHEN** el usuario tenía una invitación pendiente
- **THEN** se acepta manteniendo el rol de la invitación

#### Scenario: Invitación rechazada
- **WHEN** el usuario había rechazado previamente
- **THEN** se reincorpora con el rol del enlace

#### Scenario: Sin sesión
- **WHEN** un usuario sin sesión pulsa "Inicia sesión para unirte"
- **THEN** se abre el login y, al volver, puede unirse al enlace

### Requirement: El enlace no agrega miembros por sí solo
Abrir o previsualizar el enlace MUST NOT crear una membresía; solo la acción de unirse lo hace.

#### Scenario: Solo vista previa
- **WHEN** un usuario abre el enlace y no pulsa "Unirme"
- **THEN** no se crea ninguna colaboración

### Requirement: Invitar por correo desde compartir
La app SHALL permitir invitar por email desde el diálogo de compartir, con búsqueda de usuarios y rol editor/lector, creando una invitación pendiente y una notificación.

#### Scenario: Invitación por email
- **WHEN** el owner/editor invita a un usuario desde el diálogo
- **THEN** la invitación queda pendiente y el invitado recibe la notificación

### Requirement: Rol conservado para la web
La app móvil SHALL mostrar siempre el diagrama en modo solo lectura. El rol obtenido con el enlace (`viewer` o `editor`) MUST conservarse en la membresía para que aplique en la web.

#### Scenario: Unión como lector
- **WHEN** el usuario se une con un enlace de lector
- **THEN** el visor móvil abre el diagrama en solo lectura y el rol queda guardado como lector

#### Scenario: Unión como editor
- **WHEN** el usuario se une con un enlace de editor
- **THEN** el rol queda guardado como editor para la web y el visor móvil sigue en solo lectura
