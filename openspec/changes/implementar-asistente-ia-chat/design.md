## Context

Ver `proposal.md`. Estado actual relevante:

- Los casos de uso ya existen como servicios de aplicación (`ElementService`, `RelationshipService`) con CRUD de elementos, atributos/métodos/parámetros y relaciones. Las tools deben reutilizarlos, no duplicar lógica.
- La colaboración en tiempo real ya funciona por STOMP: el cliente publica en `/app/diagram/update` y el `WebSocketHandler` difunde por `/topic/diagram/{id}/changes`. El cliente descarta los mensajes cuyo `data.clientId` coincide con el suyo.
- Existe un orquestador de IA propio (`AiOrchestrator` con OpenRouter/Gemini vía `RestClient`) expuesto en `/api/v1/ai/chat`, sin contexto de diagrama ni tools.
- Seguridad: todo `/api/**` exige autenticación JWT; no hay autorización por membresía de proyecto a nivel de controlador.
- Arquitectura hexagonal: el dominio no conoce frameworks; los adaptadores viven en `infrastructure/`.

## Goals / Non-Goals

**Goals:**
- Añadir un asistente conversacional con *function calling* real (Spring AI `@Tool`) que opere sobre el diagrama activo.
- Reutilizar los servicios de aplicación existentes para toda mutación (una sola fuente de verdad y mismas validaciones).
- Propagar los cambios del asistente a todos los colaboradores con el mecanismo WebSocket actual.
- Integrar un panel de chat en el editor sin romper la interacción existente.

**Non-Goals:**
- Persistir conversaciones en base de datos (historial efímero en el cliente).
- Reemplazar el orquestador de IA actual ni la generación de código.
- Confirmación interactiva antes de acciones destructivas (decisión: el asistente ejecuta directo).
- Streaming token a token de la respuesta (respuesta completa por request) en esta versión.

## Decisions

### 1. Spring AI con modelo OpenAI-compatible apuntando a OpenRouter
Se añade Spring AI usando el starter de modelo OpenAI configurado con `base-url=https://openrouter.ai/api/v1`, reutilizando las API keys de OpenRouter ya presentes en `app.ai.openrouter`.
- **Por qué**: `@Tool` y el bucle de tool-calling vienen resueltos; se reutilizan credenciales y la lógica multi-key existente.
- **Alternativas**: (a) bucle manual sobre `AiOrchestrator` — más código, sin `@Tool`; (b) starter de Google GenAI — cambia de proveedor/credenciales (descartado por el usuario); (c) Spring AI Anthropic — requiere otra key.

### 2. Componente de tools con contexto por llamada (`ToolContext`)
Un bean `UmlTools` (en `infrastructure/ai`) expone métodos anotados con `@Tool` (`createElement`, `listElements`, `updateElement`, `deleteElement`, `addAttribute`, `updateAttribute`, `deleteAttribute`, `listAttributes`, `addMethod`, `updateMethod`, `deleteMethod`, `listMethods`, `createRelationship`, `updateRelationship`, `deleteRelationship`, `listRelationships`, y variantes de multiplicidades).
- Cada método recibe `ToolContext` y de ahí obtiene `projectId`, `diagramId` y `userId`, de modo que el modelo no puede dirigir operaciones a otro diagrama.
- Las tools delegan en `ElementService`/`RelationshipService` y devuelven resultados serializables (id, nombre, tipo) o un error de validación legible por el modelo.
- **Por qué `ToolContext`**: evita estado mutable compartido entre conversaciones concurrentes.

### 3. Publicación de eventos en tiempo real desde el servidor
Un `DiagramEventPublisher` (infrastructure/websocket) envuelve `SimpMessagingTemplate` y publica en `/topic/diagram/{diagramId}/changes` con el mismo formato `{userId, type, data}` que ya consume el cliente. Después de cada mutación exitosa, la tool publica el evento correspondiente (`element_created|updated|deleted`, `relationship_created|updated|deleted`).
- Los eventos originados en servidor **no** incluyen `clientId`, por lo que el cliente solicitante no los filtra y también reconstruye el diagrama.
- **Alternativa**: reutilizar `WebSocketHandler`; se descarta porque es un controlador de entrada y publicar desde ahí mezcla responsabilidades.

### 4. Endpoint de chat con contexto de diagrama
`POST /api/v1/projects/{projectId}/diagrams/{diagramId}/ai/chat`, autenticado, que recibe `{message, history[]}` y devuelve `{reply, actions[]}`.
- Obtiene el `userId` con `SecurityUtils.getCurrentUserId()`.
- Construye el `ChatClient` con el *system prompt* (rol de asistente UML, reglas de uso de tools y el `diagramId`), ejecuta el bucle de tools con un máximo de iteraciones configurable y traduce fallos del proveedor a HTTP 503.
- **Por qué un endpoint con contexto** en vez de reutilizar `/api/v1/ai/chat`: las tools necesitan `diagramId`/`userId` y el scope de proyecto para validar y difundir.

### 5. Panel de chat efímero en el editor (frontend)
Nuevo componente `AiChatPanel` + hook `useAiChat` dentro de `features/diagram-editor`, y una función en `shared/api/client.ts` para llamar al endpoint.
- El historial vive en el estado del hook (memoria), no en `localStorage` ni servidor.
- Al recibir la respuesta, muestra el texto y las acciones; el refresco del lienzo llega por WebSocket (sección 3). Se permite además un `rebuildDiagram()` de respaldo si no hay conexión.
- Se integra en `pages/DiagramEditorPage.tsx` como un panel lateral conmutable, sin bloquear el resto del editor.

### 6. Selección de modelo con soporte de tool calling
El modelo se toma de configuración (`app.ai`); el valor por defecto debe ser un modelo de OpenRouter con soporte de herramientas. Si el modelo no soporta tools, el asistente responderá solo texto (degradación, no error).

## Risks / Trade-offs

- **Compatibilidad de versión de Spring AI con Spring Boot 4.1 / Java 25** → fijar una versión/BOM concreta y verificar el arranque; si no hay versión compatible, el plan B es un bucle de tool-calling manual sobre el `AiOrchestrator` existente (mismo contrato de endpoint y tools).
- **Modelo sin soporte de function calling** → documentar el requisito del modelo y permitir configurarlo; degradar a respuesta de texto.
- **Bucles de tools largos o costosos** → límite de iteraciones configurable y *timeout* del proveedor.
- **Prompt injection que dispare borrados** → decisión de "ejecutar directo"; mitigación: el endpoint devuelve la lista de acciones ejecutadas y las tools registran cada mutación, y todas pasan por servicios con validaciones.
- **Tormenta de refrescos en tiempo real** → una tool emite un evento por mutación; el cliente ya debounce/rebuild; aceptable para el volumen esperado.
- **Referencias cruzadas entre diagramas por parte del modelo** → validar en cada tool que el elemento/relación pertenezca al `diagramId` de la conversación.
- **Duplicados de nombre de atributo/método** → el error de dominio se devuelve al modelo como resultado de la tool para que lo comunique o corrija.

## Migration Plan

- Cambio aditivo: nueva dependencia, nueva configuración, nuevos adaptadores y UI. Sin migraciones de base de datos.
- Despliegue: backend y frontend juntos; el endpoint anterior `/api/v1/ai/chat` permanece intacto.
- Rollback: quitar el panel del editor y el endpoint nuevo; no deja datos huérfanos.

## Open Questions

- Versión exacta de Spring AI (BOM) compatible con Spring Boot 4.1.1 y Java 25.
- Modelo de OpenRouter por defecto con soporte de function calling.
- Formato exacto de `ToolContext` y del `ChatClient` en la versión de Spring AI elegida.
