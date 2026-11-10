# Casos de uso — Parte 2

Actores
- Usuario (Principal)
- Colaborador / Integrante del equipo
- Usuario móvil
- IA (Sistema)
- Sistema / Plataforma (servicios de chat, WebSocket y persistencia NoSQL)
Casos de uso nuevos
- CU11: Interactuar con IA multifuncional
- CU12: Colaborar en chat grupal de IA
- CU13: Visualizar diagramas en móvil en tiempo real
- CU14: Usar el modo IA dedicado
Relación actor–caso de uso
Caso de uso	Actores
CU11	Usuario, IA (Sistema)
CU12	Colaborador / Integrante, IA (Sistema), Sistema / Plataforma
CU13	Usuario móvil, Sistema / Plataforma
CU14	Usuario, Colaborador / Integrante, IA (Sistema)
Total: 4 casos de uso nuevos (< 6).  
NoSQL no es un caso de uso ni un actor: es una decisión de arquitectura. IA local es una variante opcional de CU11.

## Alcance

Este documento detalla los casos de uso nuevos de la segunda parte de UMLPilot:

- **CU11:** Interactuar con IA multifuncional.
- **CU12:** Colaborar en chat grupal de IA.
- **CU13:** Visualizar diagramas en móvil en tiempo real.
- **CU14:** Usar el modo IA dedicado.

No se incluyen como casos de uso nuevos:
- **NoSQL:** decisión de arquitectura (MongoDB para el chat; PostgreSQL sigue siendo la fuente de verdad).
- **IA local:** variante opcional de CU11.
- **App móvil:** se documenta dentro de CU13.

## Actores

| Actor | Descripción |
|---|---|
| Usuario | Usuario autenticado de UMLPilot. Puede tener rol `owner`, `editor` o `viewer` en un proyecto. |
| Integrante del equipo | Miembro aceptado de un proyecto que participa en el chat grupal. |
| Usuario móvil | Integrante que consulta un diagrama desde la app móvil. |
| Asistente IA | Servicio de IA que interpreta consultas, genera respuestas y ejecuta acciones UML cuando tiene permisos. |
| Servicio IA | Componente que selecciona proveedor/modelo, construye contexto y aplica tools. |
| Proyecto / Diagrama | Datos y modelo UML sobre los que opera la IA. |
| Chat Service | Servicio que persiste, pagina y difunde los mensajes del chat. |
| MongoDB | Almacén NoSQL de conversaciones, mensajes y resúmenes. |
| Servidor WebSocket | Canal STOMP que difunde cambios de diagrama y eventos del chat. |

## Resumen

| Código | Caso de uso | Actor iniciador | Prioridad |
|---|---|---|---|
| CU11 | Interactuar con IA multifuncional | Usuario | Alta |
| CU12 | Colaborar en chat grupal de IA | Integrante del equipo | Alta |
| CU13 | Visualizar diagramas en móvil en tiempo real | Usuario móvil | Media-Alta |
| CU14 | Usar el modo IA dedicado | Usuario | Alta |

---

# CU11 — Interactuar con IA multifuncional

| Campo | Detalle |
|---|---|
| **Código** | CU11 |
| **Nombre** | Interactuar con IA multifuncional |
| **Descripción** | Permite al usuario realizar consultas generales, revisiones, generación de proyectos, cambios y modificaciones usando IA por texto o voz. |
| **Propósito** | Que la IA actúe como un asistente versátil sobre el proyecto y el diagrama, no solo como un generador de diagramas. |
| **Actores** | Usuario, Asistente IA, Servicio IA, Proyecto/Diagrama. IA local es una variante opcional. |
| **Actor iniciador** | Usuario |
| **Precondiciones** | Usuario autenticado. Debe ser miembro aceptado del proyecto. Para cambios en el diagrama debe ser `owner` o `editor`. Si se van a aplicar cambios, debe existir un diagrama seleccionado. |
| **Postcondiciones** | El usuario recibe una respuesta. Si correspondía, las acciones UML quedan aplicadas y registradas. El proveedor/modelo usado queda registrado. |
| **Frecuencia** | Alta |
| **Criticidad** | Alta |

## Flujo principal

1. El usuario abre el modo IA o el editor y elige la opción de consultar/generar con IA.
2. El sistema valida la sesión y la membresía del usuario en el proyecto.
3. El sistema carga el proyecto, los diagramas disponibles y el diagrama seleccionado.
4. El usuario elige el tipo de solicitud: consulta, revisión, generación de proyecto, cambio o modificación.
5. El usuario escribe la instrucción o la dicta por voz.
6. Si la entrada es voz, el sistema la transcribe a texto.
7. El sistema construye el contexto: proyecto, diagrama, rol del usuario, historial/acciones previas y reglas del asistente.
8. El sistema selecciona un proveedor de IA según `AI_PROVIDER_ORDER` y rota si el proveedor falla o se queda sin cuota.
9. La IA interpreta la intención y genera una respuesta o un conjunto de acciones UML.
10. Si la respuesta requiere cambios, el sistema valida que el usuario sea `owner`/`editor`, ejecuta las tools UML y publica los cambios por STOMP.
11. El sistema muestra la respuesta, las acciones ejecutadas y el resultado al usuario.
12. El usuario puede continuar la conversación, abrir el diagrama o cerrar la vista.

## Flujos alternativos

- **A1. Sin diagrama seleccionado:** la IA responde consultas generales, pero no ejecuta cambios.
- **A2. Usuario `viewer`:** la IA usa herramientas de solo lectura y explica que no puede modificar el diagrama.
- **A3. Voz no disponible o transcripción fallida:** el sistema permite continuar con texto.
- **A4. IA local no configurada:** el sistema usa el proveedor en la nube configurado.
- **A5. Proveedor habitual saturado:** el sistema rota al siguiente proveedor/modelo configurado.
- **A6. La IA no entiende la intención:** pide aclaración antes de ejecutar acciones.

## Excepciones

- **Sin proveedor de IA disponible:** HTTP 503 y mensaje legible; no se aplican cambios parciales.
- **Diagrama inexistente o de otro proyecto:** HTTP 404/400 y no se ejecuta ninguna acción.
- **Usuario sin permisos de edición:** HTTP 403 o respuesta de solo lectura según el canal.
- **Timeout del proveedor:** se informa el fallo y se permite reintentar.
- **Argumentos inválidos de una tool:** la tool devuelve el error al modelo; no se corrompe el diagrama.
- **Conflicto con cambios concurrentes:** la tool relee el estado actual antes de aplicar el cambio.

## Reglas de negocio

- La IA opera siempre sobre el diagrama seleccionado del proyecto; nunca sobre otro diagrama.
- Solo `owner` y `editor` pueden ejecutar acciones que mutan el diagrama.
- La rotación de proveedores es automática y no expone claves ni credenciales.
- Las acciones ejecutadas se registran para mostrarlas al usuario y para el contexto posterior.
- La IA no inventa ids: resuelve nombres contra el estado real del diagrama.

## Datos involucrados

- Proyecto y diagrama seleccionado.
- Instrucción del usuario (texto o transcripción).
- Rol y membresía del usuario.
- Contexto del proyecto y acciones previas.
- Respuesta, acciones, proveedor y modelo usados.

---

# CU12 — Colaborar en chat grupal de IA

| Campo | Detalle |
|---|---|
| **Código** | CU12 |
| **Nombre** | Colaborar en chat grupal de IA |
| **Descripción** | Chat persistente por proyecto, compartido por todos los integrantes aceptados, con contexto de todos los autores, menciones y referencias a conversaciones antiguas. |
| **Propósito** | Convertir la IA en un miembro más del equipo: todos ven el mismo hilo, la IA recuerda decisiones y puede citar mensajes anteriores. |
| **Actores** | Integrante del equipo, Asistente IA, Chat Service, MongoDB, Servicio IA, Equipo. |
| **Actor iniciador** | Integrante del equipo |
| **Precondiciones** | Usuario autenticado y miembro aceptado del proyecto. El proyecto debe existir. Para mutaciones, el usuario debe ser `owner` o `editor`. |
| **Postcondiciones** | El mensaje queda persistido y compartido. Si mencionó a la IA, se guarda la respuesta con acciones/citas y se actualiza el estado. El historial queda disponible para todos. |
| **Frecuencia** | Alta |
| **Criticidad** | Alta |

## Flujo principal

1. El integrante abre el chat grupal del proyecto.
2. El sistema valida la membresía en PostgreSQL.
3. El sistema obtiene o crea la conversación principal del proyecto de forma idempotente.
4. El sistema carga el bloque más reciente de mensajes y el resumen acumulado.
5. El integrante escribe un mensaje y puede mencionar con autocompletado a `@Asistente IA` o a otro integrante.
6. El sistema valida que el contenido no esté vacío, que el diagrama (si se indicó) pertenezca al proyecto y que el `clientMessageId` no esté repetido.
7. El sistema persiste el mensaje en MongoDB con autor, nombre, rol, fecha y estado.
8. El sistema difunde el mensaje por STOMP a todos los integrantes conectados.
9. Si el mensaje menciona a la IA (`@asistente`, `@ai` o `@ia`), el sistema construye el contexto compartido: resumen, mensajes recientes, mensajes antiguos relevantes y autoría de cada mensaje.
10. El sistema consulta al proveedor de IA con las tools permitidas según el rol.
11. El sistema persiste la respuesta del asistente con acciones, citas verificadas, proveedor y modelo.
12. El sistema actualiza el estado del mensaje del usuario y difunde la respuesta y el estado por STOMP.
13. Si la conversación supera el umbral, un proceso de resumen actualiza el resumen acumulado.
14. Los integrantes pueden seguir conversando, cargar mensajes anteriores o seleccionar una cita para localizar el mensaje original.

## Flujos alternativos

- **A1. Mensaje sin `@asistente`:** se guarda y se difunde como chat grupal; no se invoca a la IA.
- **A2. Mención solo a una persona:** se guarda y se difunde; no se invoca a la IA.
- **A3. Mensaje duplicado:** si el `clientMessageId` ya existe, se devuelve el mensaje existente sin duplicarlo ni volver a invocar a la IA.
- **A4. Usuario `viewer`:** puede leer y escribir en el chat, pero la IA se ejecuta con tools de solo lectura.
- **A5. Cargar historial:** el cliente solicita mensajes anteriores por cursor.
- **A6. Reconexión:** el cliente vuelve a suscribirse al tópico y recupera por REST los mensajes perdidos.
- **A7. Respuesta de IA fallida:** el mensaje del usuario permanece guardado, se marca como fallido y se ofrece reintentar.
- **A8. Conversación larga:** se usa el resumen más reciente junto con los mensajes no resumidos y la recuperación de mensajes antiguos.

## Excepciones

- **MongoDB no disponible:** HTTP 503; no se simulan mensajes persistidos.
- **Usuario sin membresía o colaborador eliminado:** HTTP 403.
- **Proyecto eliminado:** la conversación deja de ser accesible.
- **Mensaje vacío:** HTTP 400 y no se persiste ni difunde.
- **Diagrama de otro proyecto:** HTTP 400/404 y no se ejecuta ninguna acción.
- **Ningún proveedor de IA disponible:** el mensaje queda guardado como fallido y se permite reintentar.
- **Fallo al generar el resumen:** el chat sigue funcionando con los mensajes recientes y se registra el fallo.

## Reglas de negocio

- Existe una conversación principal por proyecto.
- Todos los integrantes aceptados ven el mismo historial; no hay chats privados 1:1.
- Todos los integrantes pueden leer y escribir; solo `owner`/`editor` pueden mutar el diagrama.
- La IA responde solo cuando se la menciona explícitamente.
- Un mismo `clientMessageId` no genera mensajes duplicados.
- Las citas de la IA se validan contra mensajes reales antes de mostrarse.
- El chat se almacena en MongoDB; PostgreSQL sigue siendo la fuente de verdad de usuarios, proyectos, permisos y diagramas.
- La eliminación de un proyecto archiva/limpia su conversación.

## Datos involucrados

- Conversación: proyecto, título, estado, resumen y foco de diagrama.
- Mensaje: autor, rol, contenido, fecha, estado de IA, acciones, citas, proveedor/modelo.
- Resumen: rango de mensajes, texto y modelo.
- Contexto recuperado: mensajes antiguos relevantes y hechos del proyecto.

---

# CU13 — Visualizar diagramas en móvil en tiempo real

| Campo | Detalle |
|---|---|
| **Código** | CU13 |
| **Nombre** | Visualizar diagramas en móvil en tiempo real |
| **Descripción** | App móvil de solo lectura que muestra un diagrama UML y refleja en vivo los cambios realizados por otros usuarios o por la IA. |
| **Propósito** | Permitir que los integrantes consulten el estado actual del proyecto desde el móvil sin editar ni generar contenido. |
| **Actores** | Usuario móvil, App móvil, Servidor WebSocket, Diagrama, Backend. |
| **Actor iniciador** | Usuario móvil |
| **Precondiciones** | Usuario autenticado y miembro aceptado del proyecto. El diagrama debe existir y pertenecer al proyecto. La app móvil debe estar instalada y con conexión. |
| **Postcondiciones** | El diagrama se muestra actualizado. La suscripción en tiempo real queda cerrada al salir. No se modifica ningún dato. |
| **Frecuencia** | Media |
| **Criticidad** | Media-Alta |

## Flujo principal

1. El usuario abre la app móvil e inicia sesión.
2. El sistema valida el JWT y carga los proyectos accesibles.
3. El usuario selecciona un proyecto y un diagrama.
4. El backend valida la membresía y devuelve el snapshot del diagrama: elementos, relaciones, atributos, métodos y layout.
5. La app muestra el diagrama en modo solo lectura con pan y zoom.
6. La app conecta el WebSocket con JWT y se suscribe al canal de cambios del diagrama.
7. Otro usuario o la IA modifica el diagrama y el servidor publica el evento de cambio.
8. La app recibe el evento y actualiza la vista sin recargar la pantalla.
9. El usuario puede seguir consultando el diagrama y los detalles de los elementos.
10. Al salir de la vista, la app se desuscribe y cierra la conexión.

## Flujos alternativos

- **A1. Conexión perdida:** la app muestra el estado desconectado, reintenta y recarga el snapshot al reconectar.
- **A2. Token expirado:** la app intenta refrescar la sesión; si falla, redirige al login.
- **A3. Evento de otro diagrama:** se ignora.
- **A4. Evento malformado:** se registra y se ignora sin romper la vista.
- **A5. Muchos cambios seguidos:** la app agrupa/refresca para evitar parpadeos.
- **A6. Usuario sin acceso:** se muestra error y no se carga el diagrama.

## Excepciones

- **Servidor o WebSocket no disponible:** error de conexión y reintento.
- **Diagrama eliminado:** se cierra la vista y se informa.
- **Tipo de diagrama no soportado:** se muestra un mensaje claro.
- **Sesión revocada o colaborador eliminado:** se cierra la sesión.
- **Error al cargar el snapshot:** se permite reintentar.

## Reglas de negocio

- La app móvil es de solo lectura: no edita, no usa IA, no importa/exporta XMI y no genera código.
- La membresía se valida en cada carga y al suscribirse al canal.
- Solo se reciben cambios del diagrama seleccionado.
- El layout se conserva y el canvas se adapta al tamaño de pantalla.
- Al salir se liberan la suscripción y los recursos.

## Datos involucrados

- Proyecto y diagrama.
- Elementos, relaciones, atributos, métodos y multiplicidades.
- Posiciones y dimensiones del layout.
- Eventos de cambio en tiempo real.

---

# CU14 — Usar el modo IA dedicado

| Campo | Detalle |
|---|---|
| **Código** | CU14 |
| **Nombre** | Usar el modo IA dedicado |
| **Descripción** | Nueva vista y nuevo enfoque para interactuar con la IA: chat grupal central, historial, selector de diagrama, participantes, acciones y citas. |
| **Propósito** | Dar a la IA un espacio propio y cómodo, en lugar de un panel efímero dentro del editor. |
| **Actores** | Usuario, Integrantes del equipo, Asistente IA, Chat Service, Diagrama. |
| **Actor iniciador** | Usuario |
| **Precondiciones** | Usuario autenticado y miembro aceptado del proyecto. El proyecto debe existir. |
| **Postcondiciones** | La conversación queda persistida y compartida. Si la IA ejecutó acciones, el diagrama queda actualizado y visible para el equipo. |
| **Frecuencia** | Alta |
| **Criticidad** | Alta |

## Flujo principal

1. El usuario entra al modo IA desde el proyecto o desde el editor de un diagrama.
2. El sistema abre una vista dedicada con el chat grupal como elemento central.
3. El sistema carga el proyecto, los diagramas, los colaboradores y la conversación principal.
4. Si el usuario entró desde un diagrama, ese diagrama queda preseleccionado; si no, se usa el último foco o el primero disponible.
5. El sistema muestra el historial persistido, el resumen y los participantes.
6. El usuario selecciona el diagrama de contexto y escribe un mensaje con `@asistente` o menciona a otro integrante.
7. El sistema procesa el mensaje mediante CU12 y, si corresponde, CU11.
8. El sistema muestra la respuesta, las acciones ejecutadas y las citas de mensajes antiguos.
9. El usuario puede abrir el mensaje citado, cambiar de diagrama, abrir el editor del diagrama afectado o cargar historial anterior.
10. Los cambios del diagrama se reflejan en el editor abierto mediante el canal de tiempo real.

## Flujos alternativos

- **A1. Proyecto sin diagramas:** se permite conversar en modo general, sin mutaciones.
- **A2. Usuario `viewer`:** puede leer y escribir en el chat, pero la interfaz muestra que la IA no mutará el diagrama.
- **A3. Proyecto sin conversación:** se muestra un estado vacío y se crea la conversación al enviar el primer mensaje.
- **A4. Entrada desde el editor:** se preselecciona el diagrama actual.
- **A5. Reconexión:** se recupera el historial por REST y se vuelve a suscribir al chat.
- **A6. Cita de un mensaje no cargado:** se cargan páginas anteriores hasta localizarlo.

## Excepciones

- **Proyecto inexistente o sin acceso:** HTTP 404/403.
- **MongoDB no disponible:** HTTP 503 y mensaje de error.
- **Proveedor de IA no disponible:** el mensaje queda guardado como fallido y se puede reintentar.
- **Diagrama inválido o de otro proyecto:** no se ejecuta ninguna acción.
- **Error al enviar un mensaje:** se muestra el error y se permite reintentar.
- **Cita no disponible:** se muestra como no disponible sin romper la conversación.

## Reglas de negocio

- El modo IA es una vista dedicada, no un panel efímero del editor.
- La conversación es grupal y persistente por proyecto.
- La IA solo responde cuando se la menciona.
- Los viewers pueden conversar, pero no mutar el diagrama.
- El historial, las citas y las acciones son compartidos por todo el equipo.
- El chat usa MongoDB; los permisos y el proyecto viven en PostgreSQL.
- La interfaz debe ser cómoda: burbujas, fechas, menciones resaltadas, scroll y compositor autoajustable.

## Datos involucrados

- Proyecto, diagramas y colaboradores.
- Conversación, mensajes, resumen y citas.
- Acciones de IA y diagrama afectado.
- Estado de conexión y presencia.

---

## Relación con los scripts de Enterprise Architect

Los scripts generados en `.ignore/scripts EA casos de uso parte 2/` implementan estos casos de uso:

| Caso de uso | Script de actividad | Script de secuencia |
|---|---|---|
| CU11 | `CU11 - IA Multifuncional (Actividad).js` | `SD CU11 - IA Multifuncional.js` |
| CU12 | `CU12 - Chat Grupal de IA (Actividad).js` | `SD CU12 - Chat Grupal de IA.js` |
| CU13 | `CU13 - Visualizacion Movil en Tiempo Real (Actividad).js` | — |
| CU14 | `CU14 - Modo IA Dedicado (Actividad).js` | — |
| Todos | `00 - Casos de Uso Parte 2.js` | — |
