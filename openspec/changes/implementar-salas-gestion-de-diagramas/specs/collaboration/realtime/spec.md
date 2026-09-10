## Purpose

Permite colaboración en tiempo real mediante WebSocket — presencia de usuarios, sincronización de cambios y bloqueo de diagrama.

## ADDED Requirements

### Requirement: Conexión WebSocket autenticada
El sistema SHALL soportar conexiones WebSocket en `/ws` con autenticación JWT via query param `token`.

#### Scenario: Conexión exitosa
- **WHEN** un usuario se conecta a `ws://host/ws?token=<jwt>`
- **THEN** el sistema acepta la conexión y agrega al usuario a la sala del diagrama

#### Scenario: Token inválido
- **WHEN** se envía un token inválido o expirado
- **THEN** el sistema cierra la conexión con código 4001

### Requirement: Unirse a sala de diagrama
El sistema SHALL permitir a un usuario unirse a la "sala" de un diagrama específico para recibir actualizaciones en tiempo real.

#### Scenario: Usuario se une a sala
- **WHEN** el usuario envía el evento `join_diagram` con `diagramId`
- **THEN** el sistema agrega al usuario a la sala y notifica a los demás usuarios conectados

### Requirement: Salir de sala de diagrama
El sistema SHALL permitir a un usuario salir de la sala cuando cierra el editor.

#### Scenario: Usuario sale de sala
- **WHEN** el usuario envía `leave_diagram` o se desconecta
- **THEN** el sistema remueve al usuario de la sala y notifica a los demás

### Requirement: Presencia de usuarios
El sistema SHALL mantener y transmitir la presencia de usuarios conectados a un diagrama.

#### Scenario: Lista de usuarios en sala
- **WHEN** un usuario se une a una sala
- **THEN** recibe la lista de usuarios actualmente conectados al diagrama

#### Scenario: Notificación de nueva presencia
- **WHEN** un nuevo usuario se une a la sala
- **THEN** todos los usuarios conectados reciben el evento `user_joined` con userId, name

#### Scenario: Notificación de salida
- **WHEN** un usuario sale de la sala
- **THEN** todos reciben el evento `user_left` con userId

### Requirement: Sincronización de cambios de diagrama
El sistema SHALL transmitir cambios de elementos y relaciones en tiempo real a todos los usuarios de la sala.

#### Scenario: Elemento creado
- **WHEN** un usuario crea un elemento (clase, atributo, método, etc.)
- **THEN** el evento `element_created` se envía a todos los usuarios de la sala excepto al remitente

#### Scenario: Elemento actualizado
- **WHEN** un usuario mueve o edita un elemento
- **THEN** el evento `element_updated` se envía a todos excepto al remitente

#### Scenario: Elemento eliminado
- **WHEN** un usuario elimina un elemento
- **THEN** el evento `element_deleted` se envía a todos excepto al remitente

#### Scenario: Relación creada/actualizada/eliminada
- **WHEN** un usuario crea, actualiza o elimina una relación
- **THEN** los eventos `relationship_created`, `relationship_updated`, `relationship_deleted` se envían a todos excepto al remitente

### Requirement: Bloqueo de diagrama en tiempo real
El sistema SHALL notificar a todos los usuarios cuando un diagrama se bloquea o desbloquea.

#### Scenario: Diagrama bloqueado
- **WHEN** un usuario bloquea un diagrama
- **THEN** todos reciben el evento `diagram_locked` con userId y nombre

#### Scenario: Diagrama desbloqueado
- **WHEN** un usuario desbloquea un diagrama
- **THEN** todos reciben el evento `diagram_unlocked`
