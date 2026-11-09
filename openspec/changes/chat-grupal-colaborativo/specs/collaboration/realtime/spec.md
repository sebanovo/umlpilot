## ADDED Requirements

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
