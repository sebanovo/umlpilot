## Purpose

Chat de IA grupal, persistente y colaborativo por proyecto, con contexto compartido de todos los integrantes, historial consultable y capacidad de la IA de referenciar conversaciones antiguas.

## ADDED Requirements

### Requirement: Conversación grupal única por proyecto
El sistema SHALL mantener una conversación principal por proyecto, compartida por todos los integrantes aceptados, y SHALL crearla de forma idempotente en el primer acceso.

#### Scenario: Primer acceso al chat del proyecto
- **WHEN** un miembro aceptado abre el chat de un proyecto que aún no tiene conversación
- **THEN** el sistema crea la conversación del proyecto y la devuelve con su identificador e historial vacío

#### Scenario: Accesos posteriores
- **WHEN** cualquier miembro aceptado vuelve a abrir el chat del mismo proyecto
- **THEN** el sistema devuelve la misma conversación, sin crear duplicados

#### Scenario: Proyecto inexistente
- **WHEN** se solicita el chat de un proyecto que no existe
- **THEN** el sistema responde 404

#### Scenario: Usuario sin membresía
- **WHEN** un usuario autenticado que no es miembro aceptado solicita el chat del proyecto
- **THEN** el sistema responde 403 y no revela mensajes ni participantes

### Requirement: Persistencia de mensajes en almacén NoSQL
El sistema SHALL persistir las conversaciones, mensajes y resúmenes del chat en un almacén NoSQL documental, de forma que sobrevivan a reinicios del backend, recargas del navegador y cambios de sesión.

#### Scenario: Recarga del navegador
- **WHEN** un usuario recarga la vista de modo IA
- **THEN** vuelve a ver el historial persistido de la conversación del proyecto

#### Scenario: Reinicio del backend
- **WHEN** el backend se reinicia y un miembro vuelve a abrir el chat
- **THEN** los mensajes anteriores siguen disponibles

#### Scenario: PostgreSQL como fuente de verdad
- **WHEN** se valida el acceso al chat
- **THEN** la membresía y el rol se consultan en PostgreSQL, no en el almacén NoSQL del chat

### Requirement: Mensajes compartidos con autor y rol
El sistema SHALL registrar cada mensaje con identificador, conversación, proyecto, contenido, autor, nombre visible, rol, marca temporal y un `clientMessageId` de idempotencia; todos los integrantes aceptados SHALL ver el mismo historial.

#### Scenario: Mensaje de otro integrante
- **WHEN** un integrante envía un mensaje
- **THEN** los demás integrantes ven el mismo mensaje con el nombre y rol del autor y la fecha de envío

#### Scenario: Historial común
- **WHEN** dos integrantes consultan el historial del mismo proyecto
- **THEN** ambos reciben la misma secuencia de mensajes, sin conversaciones privadas por usuario

#### Scenario: Mensaje vacío
- **WHEN** se intenta enviar un mensaje nulo, vacío o solo con espacios
- **THEN** el sistema responde 400 y no persiste ni difunde el mensaje

### Requirement: Idempotencia y orden de mensajes
El sistema SHALL garantizar que un mismo `clientMessageId` no cree mensajes duplicados en una conversación y SHALL devolver los mensajes en un orden estable por fecha del servidor e identificador.

#### Scenario: Reintento del mismo mensaje
- **WHEN** el cliente reenvía un mensaje con un `clientMessageId` ya persistido
- **THEN** el sistema devuelve el mensaje existente y no crea otro ni vuelve a invocar a la IA

#### Scenario: Dos mensajes simultáneos
- **WHEN** dos integrantes envían mensajes casi al mismo tiempo
- **THEN** ambos se persisten y la paginación los devuelve en un orden estable, sin omitirlos ni duplicarlos

### Requirement: Mensajes en tiempo real
El sistema SHALL difundir por STOMP cada mensaje nuevo a todos los integrantes conectados al chat del proyecto, sin que tengan que recargar la página.

#### Scenario: Mensaje difundido
- **WHEN** un integrante envía un mensaje correctamente
- **THEN** los demás integrantes conectados lo reciben en el tópico de chat del proyecto y lo muestran en la conversación

#### Scenario: Autor del mensaje
- **WHEN** el autor recibe la confirmación del servidor
- **THEN** su mensaje optimista se reconcilia con el mensaje persistido, sin duplicarse en pantalla

#### Scenario: Reconexión
- **WHEN** un integrante pierde la conexión STOMP y se reconecta
- **THEN** el sistema vuelve a suscribirlo al chat y el cliente recupera los mensajes perdidos por REST

### Requirement: Contexto compartido de todos los integrantes
El sistema SHALL construir el contexto de la IA con la conversación compartida del proyecto, incluyendo el nombre y rol de cada autor, y MUST NOT basarse únicamente en el último mensaje ni en un historial privado del usuario.

#### Scenario: Pregunta sobre lo dicho por otro integrante
- **WHEN** un integrante pregunta por una decisión que tomó otro integrante en la misma conversación
- **THEN** la IA dispone del mensaje del otro integrante, con su autor y rol, para responder

#### Scenario: Conversación larga
- **WHEN** la conversación supera el número de mensajes recientes configurado
- **THEN** la IA sigue recibiendo el contexto resumido y los mensajes recientes, sin perder el hilo colectivo

### Requirement: Referencia a mensajes y conversaciones antiguas
El sistema SHALL permitir que la IA recupere fragmentos relevantes de mensajes anteriores y SHALL devolver en su respuesta referencias verificables a los mensajes citados.

#### Scenario: Cita de una decisión antigua
- **WHEN** la IA usa un mensaje antiguo para responder
- **THEN** la respuesta incluye una cita con el identificador del mensaje, un fragmento, el autor y la fecha

#### Scenario: Mensaje citado accesible
- **WHEN** el usuario selecciona una cita
- **THEN** el cliente muestra o carga el mensaje original y lo resalta en el historial

#### Scenario: Sin coincidencias relevantes
- **WHEN** no hay mensajes antiguos relevantes para la consulta
- **THEN** la IA responde sin inventar citas ni referencias

### Requirement: Resumen rodante del historial
El sistema SHALL generar y almacenar resúmenes del historial cuando la conversación supere el umbral configurado, y SHALL usar el resumen más reciente junto con los mensajes aún no resumidos para construir el contexto.

#### Scenario: Umbral alcanzado
- **WHEN** la conversación supera el número de mensajes o tokens configurado
- **THEN** el sistema resume el bloque más antiguo y conserva los mensajes recientes sin resumir

#### Scenario: Resumen disponible
- **WHEN** la IA responde después de un resumen
- **THEN** recibe el resumen acumulado y los mensajes recientes, manteniendo la continuidad

#### Scenario: Fallo al resumir
- **WHEN** el proveedor de IA no puede generar el resumen
- **THEN** el chat sigue funcionando con los últimos mensajes y el fallo se registra sin perder mensajes

### Requirement: Permisos de lectura, escritura y acciones de IA
El sistema SHALL permitir leer y enviar mensajes a todo integrante aceptado y SHALL permitir acciones de IA que mutan el diagrama solo a `owner` y `editor`; un `viewer` SHALL poder conversar en modo lectura, sin mutaciones.

#### Scenario: Viewer envía un mensaje
- **WHEN** un `viewer` escribe en el chat
- **THEN** el mensaje se persiste y se difunde, y la IA responde sin ejecutar tools que mutan el diagrama

#### Scenario: Viewer intenta una acción de mutación
- **WHEN** un `viewer` pide a la IA crear o modificar elementos del diagrama
- **THEN** la IA no muta el diagrama y explica que no tiene permisos de edición

#### Scenario: Editor solicita una mutación
- **WHEN** un `owner` o `editor` pide a la IA modificar el diagrama
- **THEN** la IA puede ejecutar las tools de mutación sobre el diagrama seleccionado

#### Scenario: Colaborador eliminado
- **WHEN** un colaborador es eliminado del proyecto
- **THEN** pierde el acceso al chat en la siguiente lectura, escritura o suscripción STOMP

### Requirement: Diagrama de contexto para acciones
El sistema SHALL asociar cada mensaje con un diagrama del proyecto cuando el usuario seleccione uno y SHALL validar que el diagrama pertenece al proyecto antes de permitir acciones de IA sobre él.

#### Scenario: Diagrama seleccionado
- **WHEN** el usuario envía un mensaje con un diagrama seleccionado
- **THEN** la IA ejecuta las tools de lectura o mutación únicamente sobre ese diagrama

#### Scenario: Diagrama de otro proyecto
- **WHEN** el mensaje referencia un diagrama que no pertenece al proyecto
- **THEN** el sistema responde 400 o 404 y no ejecuta ninguna acción

#### Scenario: Sin diagrama seleccionado
- **WHEN** el usuario conversa sin seleccionar un diagrama
- **THEN** la IA puede responder consultas generales, pero no muta ningún diagrama

### Requirement: Metadatos del proveedor de IA
El sistema SHALL reutilizar la rotación de proveedores configurada y SHALL registrar en cada respuesta el proveedor y modelo que respondieron, sin exponer claves ni credenciales.

#### Scenario: Respuesta con proveedor
- **WHEN** la IA responde
- **THEN** el mensaje del asistente incluye el proveedor y modelo usados, visibles para diagnóstico

#### Scenario: Rotación de proveedor
- **WHEN** el proveedor habitual falla o se agota su cuota
- **THEN** el sistema usa el siguiente proveedor configurado y registra el que finalmente respondió

### Requirement: Paginación del historial
El sistema SHALL exponer el historial paginado por cursor, comenzando por los mensajes más recientes y permitiendo cargar bloques anteriores.

#### Scenario: Carga inicial
- **WHEN** un usuario abre el chat
- **THEN** recibe el bloque más reciente de mensajes y un cursor para continuar

#### Scenario: Cargar mensajes anteriores
- **WHEN** el usuario solicita mensajes anteriores usando el cursor
- **THEN** el sistema devuelve el bloque inmediatamente anterior, sin duplicar ni omitir mensajes

#### Scenario: Fin del historial
- **WHEN** ya no existen mensajes anteriores
- **THEN** el sistema indica que no hay más páginas

### Requirement: Gestión del ciclo de vida de la conversación
El sistema SHALL archivar o eliminar las conversaciones de un proyecto cuando el proyecto se elimina, y SHALL impedir el acceso a conversaciones de proyectos eliminados.

#### Scenario: Proyecto eliminado
- **WHEN** el owner elimina un proyecto
- **THEN** sus conversaciones dejan de ser accesibles y se archivan o eliminan del almacén NoSQL

#### Scenario: Limpieza fallida
- **WHEN** la limpieza del chat falla al eliminar el proyecto
- **THEN** el proyecto igualmente se elimina y la conversación queda inaccesible por la validación de membresía, con una limpieza posterior pendiente

### Requirement: Manejo de fallos del chat
El sistema SHALL persistir el mensaje del usuario aunque falle la generación de la respuesta de la IA y SHALL permitir reintentar la respuesta sin duplicar el mensaje original.

#### Scenario: Proveedor de IA no disponible
- **WHEN** ningún proveedor de IA puede responder
- **THEN** el mensaje del usuario permanece guardado y el chat muestra un estado de error con opción de reintentar

#### Scenario: Reintento de respuesta
- **WHEN** el usuario reintenta una respuesta fallida
- **THEN** el sistema genera una nueva respuesta para el mismo mensaje de usuario, sin crear otro mensaje de usuario

#### Scenario: Almacén de chat no disponible
- **WHEN** el almacén NoSQL no está disponible
- **THEN** el sistema responde 503 y no simula mensajes persistidos
