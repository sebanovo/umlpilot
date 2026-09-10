## Purpose

Permite协作ación en tiempo real en el editor de diagramas — sincronización de operaciones de elementos, presencia de usuarios, y resolución de conflictos.

## ADDED Requirements

### Requirement: Sincronización de creación de elementos
Cuando un usuario crea un elemento, el evento `element_created` se envía a todos los usuarios de la sala excepto al remitente, y el elemento aparece en el canvas de todos.

#### Scenario: Usuario crea clase
- **WHEN** el usuario A crea una clase
- **THEN** los usuarios B, C ven la nueva clase aparecer en su canvas

### Requirement: Sincronización de movimiento
Cuando un usuario mueve un elemento, el evento `element_moved` se envía a todos con la nueva posición, y el elemento se actualiza en el canvas de todos.

#### Scenario: Movimiento en tiempo real
- **WHEN** el usuario A mueve una clase
- **THEN** los usuarios B, C ven la clase moverse suavemente en su canvas

### Requirement: Sincronización de edición de atributos/métodos
Cuando un usuario añade, edita o elimina un atributo o método, los cambios se reflejan en el canvas de todos.

#### Scenario: Añadir atributo colaborativo
- **WHEN** el usuario A añade un atributo a una clase
- **THEN** los usuarios B ven el atributo aparecer en la sección de atributos de esa clase

### Requirement: Sincronización de relaciones
Cuando se crea, actualiza o elimina una relación, los cambios se reflejan en el canvas de todos.

### Requirement: Cursor compartido
El frontend SHALL mostrar la posición del cursor de cada usuario conectado en el canvas.

#### Scenario: Ver cursor de otro usuario
- **WHEN** el usuario B se mueve por el canvas
- **THEN** el usuario A ve el cursor de B con su nombre

### Requirement: Selección compartida
Cuando un usuario selecciona un elemento, se muestra un borde del color del usuario en el canvas de todos.

### Requirement: Migración WebSocket a STOMP
El frontend SHALL usar un cliente STOMP (no raw WebSocket) para comunicarse con el backend, alineándose con la configuración STOMP del servidor.

#### Scenario: Conexión STOMP exitosa
- **WHEN** el frontend conecta vía STOMP a `/ws`
- **THEN** la conexión se establece correctamente y puede enviar/recibir mensajes
