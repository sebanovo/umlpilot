## MODIFIED Requirements

### Requirement: Contexto del diagrama en la conversación
El sistema SHALL cargar y enviar los mensajes al endpoint del asistente usando la conversación persistente del proyecto y el diagrama seleccionado; el cliente MUST NOT depender de un historial local como fuente de verdad.

#### Scenario: Continuidad de la conversación
- **WHEN** el usuario envía un segundo mensaje que depende del primero
- **THEN** el asistente recibe el historial persistido y compartido y responde en contexto

#### Scenario: Continuidad entre sesiones
- **WHEN** un usuario envía un mensaje después de recargar la página
- **THEN** el asistente recibe el historial persistido del proyecto y responde en contexto

#### Scenario: Contexto de otro integrante
- **WHEN** un usuario responde a algo dicho por otro integrante
- **THEN** la petición incluye el mensaje del otro integrante con su autor y rol

#### Scenario: Cambio de diagrama
- **WHEN** el usuario cambia el diagrama seleccionado en el modo IA
- **THEN** los mensajes siguientes se asocian al nuevo diagrama sin perder el historial del proyecto

### Requirement: Indicador de actividad y errores
El sistema SHALL indicar el estado de generación de la IA para todos los integrantes y SHALL mostrar errores legibles con opción de reintento, sin bloquear el envío de mensajes de otros usuarios.

#### Scenario: Petición en curso
- **WHEN** el usuario envía un mensaje y la IA aún responde
- **THEN** el chat muestra un indicador de actividad asociado al mensaje

#### Scenario: Error del asistente
- **WHEN** la respuesta de la IA falla (por ejemplo, proveedor no disponible)
- **THEN** el chat muestra un mensaje de error legible y permite reintentar

#### Scenario: IA generando
- **WHEN** un mensaje fue enviado y la IA aún no responde
- **THEN** todos los integrantes ven un indicador de que la IA está generando una respuesta

#### Scenario: Error de IA
- **WHEN** la respuesta de la IA falla
- **THEN** el chat muestra el error y permite reintentar la respuesta

#### Scenario: Otros usuarios siguen escribiendo
- **WHEN** la IA está generando una respuesta
- **THEN** otros integrantes pueden seguir leyendo y enviando mensajes

### Requirement: Reflejo de las acciones en el lienzo
El sistema SHALL actualizar el lienzo del editor para reflejar las mutaciones de la IA originadas en el chat grupal y SHALL mostrar en el modo IA las acciones ejecutadas con enlaces al diagrama afectado.

#### Scenario: Acción del asistente aplicada
- **WHEN** el asistente crea, edita o elimina un elemento o relación
- **THEN** el editor reconstruye el diagrama y muestra el cambio

#### Scenario: Acción de IA aplicada
- **WHEN** la IA crea, edita o elimina un elemento o relación desde el chat grupal
- **THEN** el editor abierto reconstruye el diagrama y muestra el cambio

#### Scenario: Acciones en modo IA
- **WHEN** la IA ejecuta acciones sobre el diagrama
- **THEN** el mensaje del asistente lista las acciones y permite abrir el diagrama afectado

#### Scenario: Cambio recibido por otros colaboradores
- **WHEN** otro usuario tiene el mismo diagrama abierto
- **THEN** su lienzo también refleja el cambio por el canal de tiempo real

#### Scenario: Cambio para otros colaboradores
- **WHEN** otro usuario tiene el mismo diagrama abierto
- **THEN** su lienzo también refleja el cambio por el canal de tiempo real

## ADDED Requirements

### Requirement: Acceso al modo IA desde el editor
El sistema SHALL mostrar en el editor de diagramas una acción para abrir el modo IA del proyecto, sin abrir un panel de chat efímero como flujo principal.

#### Scenario: Abrir el modo IA
- **WHEN** el usuario pulsa la acción de IA en el editor
- **THEN** navega a la vista dedicada de modo IA del proyecto, conservando el diagrama actual como contexto

#### Scenario: Diagrama preseleccionado
- **WHEN** el usuario abre el modo IA desde un diagrama
- **THEN** la vista preselecciona ese diagrama para las acciones de la IA

#### Scenario: Viewer abre el editor
- **WHEN** un usuario con rol `viewer` abre el editor
- **THEN** puede abrir el modo IA en modo lectura y sin acciones de mutación

### Requirement: Vista dedicada de modo IA
El sistema SHALL ofrecer una vista dedicada de modo IA por proyecto con la conversación grupal como elemento central, historial, campo de entrada, selector de diagrama y panel de contexto.

#### Scenario: Entrar al modo IA
- **WHEN** el usuario navega al modo IA de un proyecto
- **THEN** ve el historial del chat grupal, el selector de diagrama y el campo de entrada

#### Scenario: Proyecto sin conversación
- **WHEN** el usuario entra al modo IA de un proyecto sin mensajes
- **THEN** ve un estado vacío con acciones sugeridas y el campo de entrada habilitado

#### Scenario: Navegación al editor
- **WHEN** el usuario selecciona un diagrama o una acción de la IA
- **THEN** puede abrir el editor del diagrama correspondiente sin perder el estado del chat

### Requirement: Historial persistente y paginado
El sistema SHALL mostrar el historial persistido del proyecto, cargar primero el bloque más reciente y permitir cargar mensajes anteriores.

#### Scenario: Historial al abrir
- **WHEN** el usuario abre el modo IA
- **THEN** se muestran los mensajes más recientes de la conversación persistida

#### Scenario: Cargar anteriores
- **WHEN** el usuario llega al inicio de la lista visible
- **THEN** puede cargar el bloque anterior de mensajes sin duplicados

#### Scenario: Sin más mensajes
- **WHEN** ya no hay mensajes anteriores
- **THEN** la interfaz indica que se alcanzó el inicio de la conversación

### Requirement: Conversación grupal en tiempo real
El sistema SHALL mostrar los mensajes de todos los integrantes a medida que llegan por STOMP, identificando autor, rol y fecha, y SHALL reconciliar el mensaje propio con su confirmación del servidor.

#### Scenario: Mensaje de otro integrante
- **WHEN** otro integrante envía un mensaje
- **THEN** aparece en la conversación sin recargar la página

#### Scenario: Mensaje propio optimista
- **WHEN** el usuario envía un mensaje
- **THEN** se muestra de inmediato como pendiente y se reconcilia con el mensaje persistido confirmado

#### Scenario: Mensaje duplicado
- **WHEN** llega un evento de un mensaje ya mostrado
- **THEN** la interfaz no lo duplica

### Requirement: Referencias a mensajes antiguos en la interfaz
El sistema SHALL mostrar las citas de la IA como referencias seleccionables que permiten localizar y resaltar el mensaje original.

#### Scenario: Cita visible
- **WHEN** la IA responde con referencias a mensajes antiguos
- **THEN** la respuesta muestra las citas con autor y fragmento

#### Scenario: Ir al mensaje citado
- **WHEN** el usuario selecciona una cita
- **THEN** la interfaz carga, si hace falta, el bloque que contiene el mensaje y lo resalta

#### Scenario: Cita no disponible
- **WHEN** el mensaje citado ya no existe o fue eliminado
- **THEN** la interfaz muestra la cita como no disponible sin romper la conversación

### Requirement: Estado de participantes y actividad
El sistema SHALL mostrar los integrantes del proyecto y SHALL indicar quién está conectado y cuándo la IA está generando una respuesta.

#### Scenario: Participantes visibles
- **WHEN** el usuario abre el modo IA
- **THEN** ve la lista de integrantes aceptados del proyecto con su rol

#### Scenario: Presencia
- **WHEN** un integrante se conecta o desconecta del chat
- **THEN** el estado de presencia se actualiza para los demás integrantes

#### Scenario: IA generando
- **WHEN** la IA está procesando una respuesta
- **THEN** la interfaz muestra un indicador de generación asociado al mensaje que la originó

### Requirement: Reconexión del chat
El sistema SHALL reconectar el canal STOMP y SHALL recuperar por REST los mensajes perdidos durante una desconexión.

#### Scenario: Pérdida de conexión
- **WHEN** se pierde la conexión STOMP
- **THEN** la interfaz muestra el estado desconectado y reintenta la conexión

#### Scenario: Recuperación tras reconexión
- **WHEN** la conexión se restablece
- **THEN** el cliente se vuelve a suscribir y carga los mensajes publicados durante la desconexión

### Requirement: Modo lectura para viewer
El sistema SHALL permitir a un `viewer` leer y escribir mensajes en el chat grupal, y SHALL indicar que la IA no ejecutará acciones que mutan el diagrama.

#### Scenario: Viewer escribe
- **WHEN** un `viewer` envía un mensaje
- **THEN** el mensaje se muestra en la conversación compartida

#### Scenario: Viewer pide una mutación
- **WHEN** un `viewer` pide a la IA modificar el diagrama
- **THEN** la interfaz muestra la explicación de que no tiene permisos de edición

## REMOVED Requirements

### Requirement: Panel de chat en el editor
**Reason**: El flujo principal pasa a ser una vista dedicada de modo IA con conversación grupal persistente; el panel efímero dentro del editor ya no representa el comportamiento deseado.

**Migration**: El botón de IA del editor navega al modo IA del proyecto. Las conversaciones existentes del panel no se migran porque no se persistían.

### Requirement: Historial efímero
**Reason**: Reemplazado por una conversación grupal persistente por proyecto; el historial efímero impedía la colaboración y la continuidad entre sesiones.

**Migration**: La conversación se carga desde el backend al abrir el modo IA. Los mensajes anteriores del panel efímero no se migran porque no se persistían.
