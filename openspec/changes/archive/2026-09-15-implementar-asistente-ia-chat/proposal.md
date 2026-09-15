## Why

Hoy el editor solo permite manipular el diagrama de forma manual (formularios, arrastrar, clic). No existe un asistente conversacional que traduzca intención en acciones sobre el modelo. Añadir un chatbot con *function calling* reduce la fricción para crear/editar diagramas grandes y aprovecha la infraestructura de IA y de tiempo real que el proyecto ya tiene.

## What Changes

- **Backend — Spring AI + `@Tool`**: se añade Spring AI (modelo OpenAI-compatible apuntando a OpenRouter) y se exponen los casos de uso de UML como *tools* (`@Tool`) invocables por el modelo: elementos, atributos/métodos y relaciones (CRUD + lectura del estado actual).
- **Backend — endpoint de chat con contexto de diagrama**: nuevo `POST /api/v1/projects/{projectId}/diagrams/{diagramId}/ai/chat` que ejecuta el bucle de *tool calling* y devuelve la respuesta + las acciones aplicadas.
- **Backend — sincronización en tiempo real**: cada mutación hecha por una tool publica el evento correspondiente (`element_created`, `element_updated`, `element_deleted`, `relationship_created`, `relationship_updated`, `relationship_deleted`) en `/topic/diagram/{diagramId}/changes`, reutilizando el mecanismo WebSocket existente.
- **Frontend — chat en el editor**: panel de chat en `DiagramEditorPage` que envía mensajes al endpoint, muestra la conversación (efímera, en memoria) y refleja las acciones del asistente en el lienzo.
- **Frontend — refresco tras acciones de IA**: al recibir los eventos de la tool, el editor reconstruye el diagrama (mismo camino que los cambios remotos actuales).
- No se elimina el orquestador propio (`/api/v1/ai/chat`): se mantiene para la generación de código existente. El asistente nuevo es un camino aparte.

## Capabilities

### New Capabilities
- `ai-assistant/chat`: asistente conversacional con function calling que lee y modifica elementos UML, atributos/métodos y relaciones a través de las tools, con contexto de diagrama y control de acceso.
- `frontend/ai-chat`: integración del panel de chat del asistente dentro del editor de diagramas y su reflejo en el lienzo.

### Modified Capabilities
- `collaboration/realtime`: la sincronización de cambios debe cubrir también las mutaciones originadas por el asistente (servidor), no solo las de un usuario en el cliente.

## Impact

- **Backend** (`umlpilot_api`): `pom.xml` (Spring AI), configuración `application.yml`/`AiProperties` (base-url y modelo OpenAI-compatible de OpenRouter), nuevos adaptadores `infrastructure/ai/**` y un controlador de chat con contexto, reutilizando `ElementService` y `RelationshipService`.
- **Frontend** (`umlpilot_web`): nuevo slice en `features/diagram-editor` (panel de chat + hook), API en `shared/api/client.ts`, integración en `pages/DiagramEditorPage.tsx`.
- **Dependencias**: nueva dependencia Spring AI compatible con Spring Boot 4.1 / Java 25.
- **Sin cambios incompatibles** en las APIs REST existentes.
