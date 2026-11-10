# Chat grupal de IA

El chat de IA es **grupal, persistente y colaborativo** por proyecto. PostgreSQL sigue siendo la
fuente de verdad de usuarios, proyectos, diagramas, colaboradores y permisos; MongoDB almacena
solo el dominio conversacional.

## Responsabilidades

| Almacén | Datos |
|---|---|
| PostgreSQL | usuarios, proyectos, diagramas, colaboradores, roles, permisos |
| MongoDB | conversaciones, mensajes, resúmenes del chat |

Un usuario solo puede leer/escribir si es el creador del proyecto o un colaborador con invitación
aceptada. Los `viewer` pueden conversar, pero la IA se ejecuta con tools de solo lectura. Solo
`owner`/`editor` pueden mutar el diagrama desde el chat.

## Colecciones

### `conversations`

```text
_id, projectId (único), title, createdBy, createdAt, updatedAt,
lastMessageAt, focusDiagramId, status: ACTIVE|ARCHIVED|DELETED,
summaryText, summaryThroughMessageId, summaryUpdatedAt, unsummarizedCount
```

### `messages`

```text
_id, conversationId, projectId, diagramId?,
authorType: USER|ASSISTANT|SYSTEM|TOOL,
authorUserId?, authorName, authorRole?,
content, clientMessageId?, replyToId?,
citations[{messageId, snippet, authorName, createdAt}],
actions[{action, entity, id, result}],
provider?, model?, aiStatus: NONE|GENERATING|COMPLETED|FAILED,
createdAt, editedAt?, deletedAt?
```

### `summaries`

```text
_id, conversationId, fromMessageId, toMessageId, text, model, createdAt
```

### Índices

- `conversations`: único `{ projectId: 1 }`; `{ status: 1, lastMessageAt: -1 }`.
- `messages`: único parcial `{ conversationId: 1, clientMessageId: 1 }`;
  `{ conversationId: 1, createdAt: -1, _id: -1 }`; `{ conversationId: 1, aiStatus: 1 }`;
  índice de texto sobre `content`.
- `summaries`: `{ conversationId: 1, createdAt: -1 }`.

Los índices se crean al arrancar si `CHAT_MONGO_INDEX_INIT=true` (por defecto). En el perfil H2
se desactiva para que los tests no necesiten MongoDB.

## API REST

Base: `/api/v1/projects/{projectId}/chat`.

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/` | Obtiene o crea la conversación principal del proyecto. |
| `GET` | `/messages?before=<cursor>&limit=50` | Historial paginado, más reciente primero. |
| `POST` | `/messages` | Envía `{ clientMessageId, content, diagramId? }`; responde `202` (o `200` si es duplicado). |
| `POST` | `/messages/{messageId}/retry` | Reintenta la respuesta de IA de un mensaje fallido. |
| `POST` | `/transcribe` | Convierte audio a texto con Groq/Whisper y lo devuelve al compositor sin enviar el mensaje. |

El envío persiste el mensaje y devuelve `generating`; la respuesta de la IA llega por STOMP.
Los errores usan `400` (petición inválida), `403` (sin membresía), `404` (proyecto/mensaje) y
`503` (MongoDB no disponible).

## STOMP

- Suscripción: `/topic/project/{projectId}/chat`.
- Envío de actividad: `/app/chat/typing` y `/app/chat/presence`.
- Eventos: `chat_message_created`, `chat_ai_status`, `chat_typing`, `chat_presence`.
- La suscripción valida la membresía en PostgreSQL; un usuario sin acceso no recibe eventos.
- Tras una reconexión, el cliente vuelve a suscribirse y recupera el historial por REST.

## Menciones y activación de la IA

- La IA solo responde cuando el mensaje menciona `@asistente` (o `@ai`/`@ia`, sin distinguir mayúsculas).
- Un mensaje que solo menciona a una persona se guarda y se difunde como chat grupal, sin invocar al modelo.
- El compositor ofrece autocompletado al escribir `@`: `@Asistente IA` y los integrantes del proyecto. El botón `@` inserta la mención sin perder el foco del cuadro.
- El botón de micrófono graba audio, lo envía a Groq/Whisper y coloca la transcripción en el cuadro de texto; no envía el mensaje automáticamente.
- Las menciones se resaltan en las burbujas; la interfaz recuerda que la IA necesita la mención.
- La conversación usa burbujas propias/ajenas, separadores de fecha, scroll condicionado al final, botón “Ir al final” y compositor autoajustable.

## App móvil

La app Flutter incluye el mismo chat grupal por proyecto:

- Ruta `/projects/:projectId/chat`, accesible desde el detalle del proyecto.
- Historial y envío por REST; mensajes, estado de IA, escritura y presencia por STOMP.
- Menciones con `@` mediante selector de `@Asistente IA` e integrantes.
- Dictado por voz con Groq/Whisper: graba audio, lo transcribe y coloca el texto en el compositor sin enviar el mensaje.
- Selector de diagrama de contexto y panel de participantes.
- La app sigue siendo de solo lectura para el diagrama; el chat no edita directamente el modelo.
- El historial se carga por bloques y se completa al reconectar el WebSocket.

## Contexto de la IA

1. System prompt con proyecto, diagrama seleccionado y reglas de autoría.
2. Resumen acumulado más reciente.
3. Últimos `CHAT_RECENT_MESSAGES` mensajes.
4. Hasta `CHAT_RETRIEVAL_LIMIT` mensajes antiguos recuperados por búsqueda textual.
5. Instrucción de citar con `[msg:<id>]`; el backend valida las citas antes de devolverlas.

`ChatSummaryScheduler` resume cada `CHAT_SUMMARY_INTERVAL_MS` cuando una conversación supera
`CHAT_SUMMARY_THRESHOLD` mensajes sin resumir, conservando los mensajes recientes.

## Configuración

```env
MONGODB_URI=mongodb://localhost:27017/umlpilot_chat
MONGO_PORT=27017
CHAT_RECENT_MESSAGES=40
CHAT_RETRIEVAL_LIMIT=5
CHAT_SUMMARY_THRESHOLD=60
CHAT_SUMMARY_BATCH_SIZE=40
CHAT_SUMMARY_INTERVAL_MS=60000
CHAT_MONGO_INDEX_INIT=true
CHAT_TIMEOUT_SECONDS=120
```

- Desarrollo local: `MONGODB_URI` apunta a `localhost`.
- Despliegue con `docker-compose.prod.yml`: el `.env` del servidor debe usar
  `MONGODB_URI=mongodb://umlpilot-mongo:27017/umlpilot_chat`.
- El servicio `umlpilot-mongo` usa `mongo:7`, volumen persistente y healthcheck; en producción
  no publica puertos.

## Operación

- Si MongoDB no responde, las rutas del chat devuelven `503`; el resto de la aplicación sigue
  funcionando.
- Al eliminar un proyecto se publica un evento after-commit y se archiva su conversación. Si la
  limpieza falla, la validación de membresía impide el acceso y puede reintentarse.
- Los tests unitarios no requieren MongoDB. Las pruebas de integración del adaptador usan
  Testcontainers y se omiten si Docker no está disponible para Testcontainers.
