## Why

Hoy el asistente de IA del editor mantiene una conversación efímera y por usuario: el historial vive en memoria del navegador, se reenvía en cada petición y no se comparte entre colaboradores. Eso impide que un equipo construya conocimiento conjunto sobre el proyecto y que la IA recuerde decisiones tomadas en conversaciones anteriores.

## What Changes

- Nuevo chat de IA **grupal y persistente por proyecto**, almacenado en MongoDB y visible para todos los integrantes aceptados del proyecto.
- La IA recibe el contexto compartido de todos los integrantes, con autor y rol de cada mensaje, y puede citar mensajes/conversaciones antiguas mediante resúmenes, recuperación y enlaces a mensajes.
- Los mensajes se difunden en tiempo real por STOMP a todos los miembros del proyecto, con autor, rol, fecha y estado de generación de la IA.
- Nuevo **modo IA** como vista dedicada en la web (no solo un panel flotante del editor): conversación central, historial paginado, selector de diagrama, participantes, acciones de la IA y referencias a mensajes antiguos.
- El botón de IA del editor pasa a abrir el modo IA; se retira el panel efímero como flujo principal.
- Permisos: todos los miembros aceptados pueden leer y escribir en el chat; solo `owner`/`editor` pueden ejecutar acciones de IA que mutan el diagrama. Un `viewer` usa la IA en modo lectura.
- MongoDB se incorpora como almacén exclusivo del chat. PostgreSQL sigue siendo la fuente de verdad de usuarios, proyectos, diagramas, colaboradores y permisos; no se migran diagramas a NoSQL.
- Nuevos endpoints REST, tópicos STOMP, colecciones e índices de Mongo, configuración `MONGODB_URI` y servicio `umlpilot-mongo` en los entornos Docker.
- El borrado de un proyecto o la eliminación de un colaborador revocan el acceso al chat; el borrado del proyecto archiva/limpia sus conversaciones.
- **BREAKING**: el panel de chat efímero del editor deja de ser el flujo principal; la conversación persistente no se reinicia al recargar y es compartida por todo el proyecto.

## Capabilities

### New Capabilities

- `ai-assistant/group-chat`: conversaciones grupales persistentes por proyecto en MongoDB, mensajes compartidos y en tiempo real, contexto de todos los autores, resúmenes/recuperación de conversaciones antiguas, citas, paginación, idempotencia y permisos de lectura/escritura/acciones de IA.

### Modified Capabilities

- `frontend/ai-chat`: el asistente deja de ser un panel efímero y pasa a un modo IA dedicado con chat grupal persistente, historial paginado, selector de diagrama, referencias a mensajes antiguos y estados de generación.
- `collaboration/realtime`: se añaden tópicos y eventos de chat por proyecto, validación de membresía al suscribirse/enviar y notificaciones de mensajes y actividad de la IA.
- `projects/access-control`: se añaden las reglas de acceso al chat grupal y la restricción de acciones de IA que mutan el diagrama a `owner`/`editor`.

## Impact

- **Backend**: nuevo módulo de chat en arquitectura hexagonal (dominio, puertos, servicio de aplicación, adaptador Mongo, controlador REST, publicador STOMP), integración con `AiChatService`/`UmlTools` y con `ProjectAccessService`; extensión de `WebSocketAuthInterceptor` para autorizar suscripciones de chat.
- **Frontend**: nueva página de modo IA, slice FSD `features/ai-chat`, cliente REST, hook STOMP de chat, componentes de historial/mensajes/citas/selector de diagrama; cambios en `App.tsx`, `ProjectDetailPage` y `DiagramEditorPage`.
- **Infraestructura**: dependencia `spring-boot-starter-data-mongodb`, `MONGODB_URI` en `.env`, servicios Mongo en `docker-compose.yml`/`docker-compose.dev.yml`/`docker-compose.prod.yml`, volumen y healthcheck; sin puertos publicados en producción.
- **Datos**: nuevas colecciones `conversations`, `messages`, `summaries`, `project_facts` y, en fase posterior, `message_embeddings`; PostgreSQL no cambia su esquema.
- **Pruebas**: unitarias de contexto, permisos, idempotencia y resúmenes; controller/security tests; adaptador Mongo con Testcontainers; pruebas de humo de STOMP; lint/build del frontend.
- **Documentación**: arquitectura del chat, configuración de Mongo y guía de operación.
