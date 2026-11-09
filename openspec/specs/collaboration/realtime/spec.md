## Purpose

Permite colaboración en tiempo real mediante WebSocket — presencia de usuarios, sincronización de cambios y bloqueo de diagrama.

## Requirements

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

### Requirement: Sincronización de cambios originados por el asistente
El sistema SHALL transmitir a todos los usuarios de la sala los cambios de elementos y relaciones aplicados por el asistente de IA, usando los mismos eventos y el mismo canal que los cambios originados por un usuario.

#### Scenario: Elemento modificado por el asistente
- **WHEN** una tool del asistente crea, actualiza o elimina un elemento
- **THEN** el evento `element_created`, `element_updated` o `element_deleted` se publica en el canal de cambios del diagrama

#### Scenario: Relación modificada por el asistente
- **WHEN** una tool del asistente crea, actualiza o elimina una relación
- **THEN** el evento `relationship_created`, `relationship_updated` o `relationship_deleted` se publica en el canal de cambios del diagrama

#### Scenario: Refresco de colaboradores
- **WHEN** un usuario distinto recibe un evento originado por el asistente
- **THEN** su editor reconstruye el diagrama y muestra el cambio en tiempo real

#### Scenario: Refresco del solicitante
- **WHEN** el usuario que solicitó la acción del asistente recibe el evento resultante
- **THEN** su editor refleja el cambio sin recargar la página

### Requirement: Tópico de chat por proyecto
El sistema SHALL exponer un tópico STOMP de chat por proyecto, separado del tópico de cambios de diagrama, para difundir mensajes, actividad de la IA y presencia.

#### Scenario: Suscripción al chat
- **WHEN** un integrante aceptado se suscribe al tópico de chat de su proyecto
- **THEN** recibe los eventos de mensajes y actividad de ese proyecto

#### Scenario: Aislamiento entre proyectos
- **WHEN** un integrante está suscrito al chat de un proyecto
- **THEN** no recibe eventos de chat de otros proyectos

### Requirement: Autorización de suscripción al chat
El sistema SHALL validar la membresía aceptada en PostgreSQL al procesar la suscripción STOMP al tópico de chat y SHALL rechazar la suscripción de usuarios sin acceso.

#### Scenario: Suscripción autorizada
- **WHEN** un miembro aceptado se suscribe al tópico de chat de su proyecto
- **THEN** la suscripción se acepta y el usuario recibe eventos

#### Scenario: Suscripción sin acceso
- **WHEN** un usuario sin membresía intenta suscribirse al tópico de chat de un proyecto
- **THEN** la suscripción se rechaza y no recibe mensajes ni presencia

#### Scenario: Colaborador eliminado
- **WHEN** un colaborador eliminado intenta suscribirse o enviar al chat
- **THEN** la operación se rechaza

### Requirement: Eventos de mensajes de chat en tiempo real
El sistema SHALL publicar en el tópico de chat un evento por cada mensaje persistido, con identificador, conversación, autor, rol, contenido, fecha y estado.

#### Scenario: Mensaje persistido
- **WHEN** un mensaje se guarda correctamente
- **THEN** se publica un evento `chat_message_created` en el tópico del proyecto

#### Scenario: Mensaje de la IA
- **WHEN** la IA termina una respuesta
- **THEN** se publica el mensaje del asistente con sus acciones, citas y proveedor/modelo

#### Scenario: Error de la IA
- **WHEN** la IA no puede responder
- **THEN** se publica un evento de estado de error asociado al mensaje de usuario que originó la petición

### Requirement: Estado de generación de la IA
El sistema SHALL publicar eventos de estado de la IA para que todos los integrantes sepan cuándo se está generando una respuesta y cuándo terminó o falló.

#### Scenario: Inicio de generación
- **WHEN** un mensaje de usuario dispara una respuesta de IA
- **THEN** se publica un evento de estado `generating` con el identificador del mensaje de usuario

#### Scenario: Fin de generación
- **WHEN** la IA termina o falla
- **THEN** se publica un evento de estado `completed` o `failed` con el identificador correspondiente

### Requirement: Actividad de escritura y presencia en el chat
El sistema SHALL permitir difundir la actividad de escritura y la presencia de integrantes en el chat del proyecto, sin persistir esos eventos.

#### Scenario: Integrante escribiendo
- **WHEN** un integrante está escribiendo
- **THEN** los demás pueden recibir un evento de actividad de escritura asociado al proyecto

#### Scenario: Integrante se desconecta
- **WHEN** un integrante se desconecta del chat
- **THEN** los demás reciben la actualización de presencia

### Requirement: Recuperación tras reconexión del chat
El sistema SHALL permitir que un cliente que se reconecta recupere por REST los mensajes perdidos y vuelva a suscribirse al tópico de chat.

#### Scenario: Reconexión exitosa
- **WHEN** el cliente restablece la conexión STOMP
- **THEN** vuelve a suscribirse al tópico del proyecto y recibe los eventos nuevos

#### Scenario: Mensajes durante la desconexión
- **WHEN** se publicaron mensajes mientras el cliente estaba desconectado
- **THEN** el cliente los obtiene al cargar el historial por REST y no los duplica con los eventos nuevos
