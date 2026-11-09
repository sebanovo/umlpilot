## Context

Ver `proposal.md - Why`. El estado actual relevante:

- El asistente vive en `AiChatController` y `AiChatService`; el historial llega desde el cliente (`UmlAiChatRequest.history`) y se descarta al terminar la petición.
- La rotación de proveedores ya existe en `AiChatProviderRegistry`/`AiChatConfiguration` y las tools UML en `UmlTools`; los cambios de diagrama se difunden por `DiagramEventPublisher`.
- La autorización se centraliza en `ProjectAccessService` (PostgreSQL). El canal STOMP ya valida membresía al entrar a una sala de diagrama, pero no al suscribirse a tópicos.
- El frontend usa React + FSD, `@stomp/stompjs` y un cliente REST propio; el panel `AiChatPanel` es efímero y se abre dentro del editor.
- PostgreSQL sigue siendo la fuente de verdad de proyectos, diagramas, usuarios y permisos. MongoDB se añade solo para el dominio del chat.
- Los tests de backend corren con el perfil H2 y no deben requerir un MongoDB real.

## Goals / Non-Goals

**Goals:**

- Un chat grupal persistente por proyecto, con historial compartido y autoría visible.
- Contexto de IA colectivo, con resumen rodante y referencias a mensajes antiguos.
- Tiempo real por STOMP con autorización de suscripción y recuperación tras reconexión.
- Modo IA como vista dedicada, con historial paginado, selector de diagrama y citas.
- Mantener PostgreSQL como fuente de verdad y aislar MongoDB detrás de puertos/adaptadores.
- No romper los tests actuales ni el arranque sin MongoDB.

**Non-Goals:**

- Migrar diagramas, proyectos o permisos a NoSQL.
- Chat privado 1:1, adjuntos, notificaciones push o app móvil.
- Streaming token por token en el MVP.
- Búsqueda vectorial/Atlas en el MVP.
- Varias conversaciones por proyecto en el MVP (el modelo queda preparado).
- Cambiar la semántica de las tools UML existentes.

## Decisions

### 1. Almacenamiento híbrido: PostgreSQL + MongoDB

**Decisión:** PostgreSQL conserva identidad, proyectos, diagramas, colaboradores y permisos. MongoDB almacena conversaciones, mensajes, resúmenes y, en fase 2, embeddings.

**Por qué:** el chat es append-only, con esquema variable y lecturas por conversación; el núcleo necesita integridad referencial y transacciones. MongoDB cumple el requisito NoSQL sin degradar el dominio existente.

**Alternativas:** todo en PostgreSQL (se pierde el requisito NoSQL y la búsqueda documental); todo en MongoDB (se pierden FKs, joins y la lógica de acceso actual); base de grafos (no aporta para el chat).

### 2. Una conversación principal por proyecto

**Decisión:** `conversations` tiene un índice único por `projectId`. El primer acceso crea la conversación de forma idempotente; el modelo incluye `conversationId` para admitir varios hilos en el futuro.

**Por qué:** el requisito es un chat grupal por proyecto y una sola fuente de contexto compartido. Varios hilos fragmentarían el conocimiento y complicarían la UI.

**Alternativas:** una conversación por diagrama (fragmenta el contexto del proyecto); múltiples hilos desde el inicio (más UI y decisiones de contexto sin necesidad actual).

### 3. Todos los miembros leen y escriben; solo owner/editor mutan el diagrama

**Decisión:** cualquier miembro aceptado puede leer y enviar mensajes. Las tools que mutan el diagrama se habilitan solo para `owner`/`editor`; un `viewer` recibe una variante de la IA con tools de solo lectura.

**Por qué:** el chat es comunicación del equipo y debe incluir a los viewers; las mutaciones siguen la regla existente de `projects/access-control`.

**Alternativas:** solo owner/editor pueden chatear (excluye colaboradores); viewers solo leen (poco colaborativo); validar el rol dentro de cada tool (más frágil y difícil de explicar al modelo).

### 4. Persistencia en Mongo con un documento por mensaje

**Decisión:** cada mensaje es un documento independiente. La conversación guarda metadatos y un puntero al resumen; los resúmenes viven en su propia colección.

**Por qué:** evita documentos que crecen sin límite y el límite de 16 MB; permite paginación por cursor e índices por conversación/fecha.

**Alternativas:** array de mensajes dentro de la conversación (crecimiento y contención de escritura); una colección por conversación (multiplica índices y operaciones).

### 5. REST para escrituras y STOMP para eventos

**Decisión:** el cliente envía mensajes por REST; el backend persiste, responde `202` con el mensaje y publica el evento por STOMP. La respuesta de la IA se genera en segundo plano y se publica al terminar.

**Por qué:** REST facilita validación, idempotencia, reintentos y códigos de error; STOMP mantiene la colaboración en tiempo real. Bloquear el POST hasta que responda la IA acopla la latencia del proveedor al envío y complica la concurrencia.

**Alternativas:** enviar por STOMP (más difícil de validar/reintentar); POST bloqueante (peor UX y timeouts); WebSocket por usuario (no cumple el modelo grupal).

### 6. Respuesta completa en el MVP; streaming en fase 2

**Decisión:** el MVP publica el mensaje del asistente cuando está completo y muestra `generating`/`completed`/`failed`. El diseño deja el tópico preparado para añadir deltas de texto.

**Por qué:** reduce complejidad y permite reutilizar `AiChatService`; el requisito observable es la conversación en tiempo real, no el streaming token a token.

**Alternativas:** streaming por STOMP/SSE desde el inicio (más estados, reconexión y pruebas); sin indicador de generación (peor experiencia).

### 7. Contexto: resumen + últimos N + recuperación textual; citas verificables

**Decisión:** el contexto se compone de:

1. System prompt con proyecto, diagrama seleccionado, reglas y formato de autoría.
2. Resumen acumulado más reciente.
3. Últimos `N` mensajes (por defecto 40).
4. Hasta `K` mensajes antiguos relevantes (por defecto 5) recuperados por búsqueda textual en MongoDB, excluyendo los últimos `N`.
5. Instrucción de citar con `[msg:<id>]`; el backend valida los IDs y devuelve `citations`.

**Por qué:** enviar meses de historial es caro e inviable; el resumen mantiene continuidad y la recuperación permite referenciar decisiones antiguas. Las citas se validan contra IDs reales para no inventar referencias.

**Alternativas:** historial completo (coste y límite de contexto); solo resumen (pierde detalles); embeddings/Atlas desde el MVP (dependencia adicional); sin citas (no cumple “referenciar chats antiguos” de forma verificable).

### 8. Resúmenes con job periódico e idempotente

**Decisión:** `ChatSummaryScheduler` revisa cada minuto las conversaciones cuyo contador de mensajes no resumidos supera el umbral (por defecto 60), resume el bloque más antiguo y actualiza el puntero/contador. Si falla, reintenta en la siguiente ejecución.

**Por qué:** desacopla la latencia del resumen de la respuesta al usuario y evita carreras entre peticiones. El contador se actualiza de forma atómica en MongoDB.

**Alternativas:** resumir en la misma petición (añade latencia); resumen en memoria (se pierde al reiniciar); un lock en memoria (no sirve con varias réplicas).

### 9. STOMP con tópico por proyecto y autorización en SUBSCRIBE

**Decisión:** el tópico es `/topic/project/{projectId}/chat`. Un interceptor valida `ProjectAccessService.requireMember` al procesar `SUBSCRIBE` y los handlers validan al enviar. Eventos: `chat_message_created`, `chat_ai_status`, `chat_typing`, `chat_presence`.

**Por qué:** el tópico por proyecto coincide con la unidad de permisos y con la conversación principal; validar en SUBSCRIBE evita fugas de mensajes.

**Alternativas:** tópico por conversación (requiere lookup extra); tópico global con filtrado en cliente (inseguro); colas por usuario (no es difusión grupal).

### 10. Adaptador Mongo detrás de puertos de dominio

**Decisión:** el dominio define `GroupConversation`, `ConversationMessage`, `ConversationSummary` y puertos como `GroupChatRepository`, `ChatContextRetriever` y `ChatSummaryRepository`. La infraestructura implementa documentos `@Document`, repositorios Spring Data y mappers.

**Por qué:** mantiene la regla de dependencia de la arquitectura hexagonal; el dominio no conoce Mongo ni Spring.

**Alternativas:** usar `MongoTemplate` directamente en el servicio de aplicación (acopla la infraestructura); entidades JPA con JSON (no cumple NoSQL y complica consultas).

### 11. Metadatos del proveedor en la respuesta

**Decisión:** extender el resultado de la ejecución de IA con `provider` y `model` (nuevo record o campos adicionales), manteniendo `AiChatOutcome` para el endpoint existente. El chat grupal persiste esos metadatos en el mensaje del asistente.

**Por qué:** el requisito pide saber qué proveedor respondió; la rotación actual no expone esa información.

**Alternativas:** registrar solo en logs (no visible para el usuario); crear un servicio de IA paralelo (duplica la rotación y las tools).

### 12. Limpieza al eliminar proyecto: evento after-commit + validación de membresía

**Decisión:** `ProjectService.deleteProject` publica un evento de dominio; un listener `@TransactionalEventListener(AFTER_COMMIT)` archiva/elimina las conversaciones en Mongo. Si la limpieza falla, se registra y la validación de membresía impide el acceso; un job de reconciliación puede limpiar huérfanos en fase 2.

**Por qué:** no hay transacciones distribuidas; la membresía es la barrera de seguridad y la limpieza es best-effort.

**Alternativas:** transacción distribuida (no disponible); borrar Mongo dentro de la transacción JPA (si falla Mongo, se revierte el borrado del proyecto); no limpiar (datos huérfanos).

## Data Model (MongoDB)

### `conversations`

```text
_id, projectId (unique), title, createdBy, createdAt, updatedAt,
lastMessageAt, focusDiagramId, status: active|archived|deleted,
summary: { text, throughMessageId, updatedAt },
unsummarizedCount, lastSummaryAt
```

### `messages`

```text
_id, conversationId, projectId, diagramId?,
authorType: user|assistant|system|tool,
authorUserId?, authorName, authorRole?,
content, contentType,
clientMessageId?, replyToId?,
citations: [{ messageId, snippet, authorName, createdAt }],
actions: [{ action, entity, id, result }],
provider?, model?,
aiStatus: none|generating|completed|failed,
createdAt, editedAt?, deletedAt?
```

### `summaries`

```text
_id, conversationId, fromMessageId, toMessageId, text, keyFacts[],
model, createdAt
```

### `message_embeddings` (fase 2)

```text
_id, conversationId, messageId, embedding[], createdAt
```

### Índices

- `conversations`: único `{ projectId: 1 }`; `{ status: 1, lastMessageAt: -1 }`.
- `messages`: único `{ conversationId: 1, clientMessageId: 1 }` (parcial, solo cuando `clientMessageId` existe); `{ conversationId: 1, createdAt: -1, _id: -1 }`; `{ conversationId: 1, aiStatus: 1 }`; índice de texto sobre `content`.
- `summaries`: `{ conversationId: 1, toMessageId: 1 }`.
- `message_embeddings`: índice vectorial en Atlas (fase 2).

La paginación usa cursor compuesto por `createdAt` + `_id` para mantener orden estable ante empates.

## Backend Architecture

### Dominio

- `GroupConversation`: agregado con `projectId`, estado, resumen y foco de diagrama.
- `ConversationMessage`: mensaje inmutable con autor, contenido, estado de IA, citas y acciones.
- `ConversationSummary`: resumen de un rango de mensajes.
- `ChatAuthor`, `ChatCitation`, `ChatMessageStatus`.
- Puertos: `GroupChatRepository`, `ChatSummaryRepository`, `ChatContextRetriever`, `ChatEventPublisher`.

### Aplicación

- `GroupChatService`: obtiene/crea la conversación, valida membresía, persiste mensajes con idempotencia, pagina, construye el contexto, dispara la respuesta de IA y archiva/elimina.
- `ChatContextBuilder`: arma el prompt y los mensajes del LLM con autoría, resumen, recientes y recuperados; aplica presupuesto de tokens.
- `ChatSummaryScheduler`: job periódico idempotente.
- `ChatCitationParser`: extrae y valida `[msg:<id>]`.
- `ChatCleanupListener`: escucha el borrado de proyecto after-commit.

### Infraestructura

- `MongoGroupChatRepository` + `ConversationDocument`, `MessageDocument`, `SummaryDocument` + mappers.
- `MongoChatContextRetriever` con búsqueda textual; fase 2: Atlas Vector Search.
- `ChatEventPublisher` con `SimpMessagingTemplate`.
- `GroupChatController` + request/response DTOs.
- `ChatWebSocketHandler` para `typing`/presencia; `ChatSubscriptionInterceptor` para autorizar `SUBSCRIBE`.
- `ChatMongoIndexInitializer` para crear índices cuando la inicialización esté habilitada; en tests/H2 se desactiva para no requerir MongoDB.

### Integración con la IA existente

- `AiChatService` gana una variante que acepta la lista de mensajes ya construida, el contexto de tools y un flag `allowMutations`.
- Se preconstruyen dos conjuntos de `ToolCallback`: completo y solo lectura. El grupo de solo lectura contiene `list_elements`, `list_attributes`, `list_methods`, `list_relationships`, `get_diagram_state`.
- La rotación, el enfriamiento y el timeout se reutilizan sin cambios.
- `UmlTools` sigue validando el diagrama y publicando cambios por `DiagramEventPublisher`; el chat grupal no duplica esa lógica.

## API REST

Base: `/api/v1/projects/{projectId}/chat`.

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/` | Obtiene o crea la conversación principal; incluye `conversationId`, resumen, foco y `canMutate`. |
| `GET` | `/messages?before=<cursor>&limit=50` | Historial paginado, más reciente primero. |
| `POST` | `/messages` | Persiste un mensaje `{ clientMessageId, content, diagramId? }`; responde `202` con el mensaje y `aiStatus: generating`. |
| `POST` | `/messages/{messageId}/retry` | Reintenta la respuesta de IA para un mensaje de usuario fallido. |

Reglas de respuesta:

- `400`: mensaje vacío o diagrama inválido.
- `403`: usuario sin membresía.
- `404`: proyecto o diagrama inexistente.
- `503`: MongoDB o proveedor de IA no disponible.
- Un `clientMessageId` repetido devuelve `200` con el mensaje existente y no vuelve a invocar a la IA.

## STOMP

- Destino de suscripción: `/topic/project/{projectId}/chat`.
- Destinos de envío: `/app/chat/typing`, `/app/chat/presence`.
- Eventos:
  - `chat_message_created`: mensaje completo (autor, rol, contenido, estado, citas, acciones, proveedor/modelo).
  - `chat_ai_status`: `{ messageId, status, error? }`.
  - `chat_typing`: `{ userId, name, typing }` (no persistido).
  - `chat_presence`: `{ users: [...] }` (no persistido).
- `SUBSCRIBE` a `/topic/project/{projectId}/chat` exige membresía aceptada; si falla, se rechaza la suscripción.
- Al reconectar, el cliente vuelve a suscribirse y recupera el historial por REST.

## Flujo de un mensaje

1. El cliente genera `clientMessageId` y muestra el mensaje como pendiente.
2. `POST /messages` valida membresía y diagrama; inserta el mensaje de usuario con estado `generating` (índice único evita duplicados).
3. Responde `202` y publica `chat_message_created` + `chat_ai_status(generating)`.
4. En segundo plano, `GroupChatService` construye el contexto y llama a `AiChatService` con las tools permitidas.
5. Persiste el mensaje del asistente con acciones, citas, proveedor/modelo y `replyToId`; actualiza el mensaje de usuario a `completed`.
6. Publica `chat_message_created` del asistente y `chat_ai_status(completed)`.
7. Si falla, actualiza a `failed` y publica `chat_ai_status(failed)`; el cliente puede reintentar.
8. `ChatSummaryScheduler` resume cuando corresponde.

## Contexto y citas

- System prompt: rol de asistente del proyecto, reglas de autoría, prohibición de inventar IDs, instrucción de citar `[msg:<id>]` solo para mensajes recuperados y de no mutar si el usuario es viewer.
- Mensajes de usuario al LLM: `[Nombre (rol)]: contenido`, para que el modelo distinga autores.
- Mensajes del asistente previos: rol `assistant` sin prefijo de autor.
- Resumen: se inserta como mensaje de sistema o bloque de contexto antes de los recientes.
- Recuperación: búsqueda textual sobre `content` filtrada por `conversationId`, excluyendo los últimos `N`; top `K` por relevancia. Si no hay índice textual o no hay resultados, se omite.
- Citas: se parsean los `[msg:<id>]` de la respuesta, se buscan los mensajes en Mongo y se devuelven solo las citas válidas; los IDs inválidos se descartan.
- Presupuesto: máximo de mensajes recientes configurable y truncado por longitud; el resumen se regenera si crece demasiado.

## Frontend (FSD)

- Nueva página `pages/AiModePage.tsx` con ruta `/projects/:id/ai` y query opcional `diagramId`.
- Slice `features/ai-chat/`:
  - `api/groupChatApi.ts`: conversación, mensajes, envío, reintento.
  - `model/useGroupChat.ts`: carga, paginación, envío optimista, reconciliación y errores.
  - `model/useChatSocket.ts`: conexión STOMP, suscripción al tópico del proyecto, eventos y reconexión.
  - `ui/AiConversation.tsx`, `MessageList.tsx`, `MessageBubble.tsx`, `ChatComposer.tsx`, `CitationList.tsx`, `DiagramSelector.tsx`, `ParticipantsPanel.tsx`, `AiStatusBadge.tsx`.
- `App.tsx`: nueva ruta protegida.
- `ProjectDetailPage`: acceso directo al modo IA del proyecto.
- `DiagramEditorPage`: el botón de IA navega al modo IA con `diagramId`; se retira el panel efímero y `useAiChat` como flujo principal.
- Reutiliza `projectsApi.getCollaborators` y `diagramsApi.list`; añade los tipos de chat a `shared/api/client.ts`.
- Sin nueva librería de estado: hooks locales y contexto existente.
- Optimistic UI: mensaje pendiente con `clientMessageId`; se reemplaza al recibir el evento o la respuesta REST. Si falla, se marca como error y se ofrece reintento.
- Citas: al pulsar una cita, si el mensaje no está cargado se piden páginas anteriores hasta encontrarlo; luego se hace scroll y se resalta.
- Viewer: la UI muestra el aviso de solo lectura para mutaciones; el backend es la autoridad.

## Configuración y despliegue

- Dependencia `spring-boot-starter-data-mongodb`.
- `application.yml`:
  - `spring.data.mongodb.uri: ${MONGODB_URI:mongodb://localhost:27017/umlpilot_chat}`
  - `spring.data.mongodb.auto-index-creation: false` (los índices se crean explícitamente).
  - `app.chat.*`: `recent-messages`, `retrieval-limit`, `summary-threshold`, `summary-batch-size`, `index-initialization-enabled`, `timeout`.
- `.env` / `.env.sample`: `MONGODB_URI` y variables de chat; sin variantes adicionales de `.env.sample`.
- Docker:
  - `umlpilot-mongo` con imagen `mongo:7`, volumen `umlpilot_mongo_data`, healthcheck `mongosh --eval "db.adminCommand('ping')"`, red `umlpilot-net`.
  - Producción: sin puertos publicados; `MONGODB_URI=mongodb://umlpilot-mongo:27017/umlpilot_chat` en el `.env` de despliegue.
  - Desarrollo local del backend: `MONGODB_URI=mongodb://localhost:27017/umlpilot_chat`; el compose de desarrollo puede usar el nombre del servicio.
  - `umlpilot-api` depende de Mongo con `condition: service_healthy`.
- No se publica Mongo en producción. El arranque del backend no requiere MongoDB para los tests existentes: el cliente se crea de forma perezosa y la creación de índices está desactivada salvo en entornos con Mongo.

## Testing

- Unitarios: `ChatContextBuilder` (autoría, resumen, recientes, recuperación, presupuesto), `GroupChatService` (idempotencia, permisos, estados), `ChatCitationParser`, `ChatSummaryScheduler`.
- Controller/security: `GroupChatControllerTest` y pruebas de 401/403/404/503.
- Mongo: `@DataMongoTest` con Testcontainers `mongo:7` y `@Testcontainers(disabledWithoutDocker = true)`; cubre índices, cursor, unicidad e idempotencia. Los tests unitarios no requieren Docker.
- STOMP: prueba del interceptor de suscripción y, si el entorno lo permite, un `WebSocketStompClient` contra un usuario miembro/no miembro.
- Frontend: `npm run lint` y `npm run build`; pruebas manuales de dos sesiones simultáneas, reconexión, viewer y citas.
- Backend completo: `mvn test` debe seguir verde sin MongoDB local.

## Risks / Trade-offs

- **Consistencia entre PostgreSQL y MongoDB** → validación de membresía en cada operación, limpieza after-commit y job de reconciliación futuro. La conversación puede quedar huérfana si falla la limpieza, pero nunca accesible.
- **MongoDB no disponible** → el chat responde 503; el resto de la aplicación sigue funcionando. El arranque no debe fallar por Mongo.
- **Coste/latencia del contexto** → resumen + últimos N + top K; límites de tokens y timeout; el resumen se ejecuta fuera de la petición del usuario.
- **Prompt injection en mensajes grupales** → el contenido del usuario se trata como datos; el system prompt no incluye secretos; las tools se filtran por rol y el backend valida el diagrama.
- **Duplicados por reintentos** → índice único `(conversationId, clientMessageId)` y respuesta idempotente.
- **Fuga de mensajes por STOMP** → autorización en `SUBSCRIBE` y en el envío; pruebas de no miembro.
- **Viewer con tools de mutación** → dos conjuntos de tools y validación de rol en el servicio; pruebas específicas.
- **Crecimiento del historial** → un documento por mensaje, índices y paginación; sin TTL en el MVP.
- **Tests que requieren Docker** → Testcontainers con `disabledWithoutDocker`; los tests unitarios y de controller no dependen de Mongo.

## Migration Plan

1. Añadir Mongo y configuración sin activar la UI; verificar que el backend arranca sin Mongo y que `mvn test` sigue verde.
2. Implementar persistencia y REST del chat; pruebas de contrato y permisos.
3. Implementar STOMP y contexto de IA; pruebas de dos sesiones.
4. Añadir el modo IA en el frontend y retirar el panel efímero como flujo principal.
5. Añadir resúmenes/recuperación y citas; ajustar umbrales.
6. Desplegar Mongo en dev/prod; crear índices; verificar healthchecks y backup del volumen.
7. Rollback: ocultar la ruta y el botón de modo IA; el backend puede desactivar los endpoints con una propiedad. PostgreSQL no se modifica y los datos de Mongo quedan aislados.

## Open Questions

- ¿Se habilita streaming token a token en una fase 2? El diseño actual ya publica estados y deja el tópico preparado.
- ¿Se adopta Atlas Vector Search o se mantiene la búsqueda textual en producción? Depende del volumen y del proveedor de MongoDB.
- ¿Se admitirán múltiples conversaciones por proyecto (hilos) más adelante? El modelo incluye `conversationId`, pero la UI y el contexto asumen una principal.
- ¿Se añadirán adjuntos y notificaciones de chat? Fuera del MVP.
