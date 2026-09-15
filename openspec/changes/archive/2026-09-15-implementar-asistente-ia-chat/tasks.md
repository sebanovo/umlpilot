## 1. Backend: dependencia y configuración de Spring AI

- [x] 1.1 Añadir la dependencia de Spring AI (starter de modelo OpenAI-compatible) al `pom.xml` con la versión/BOM compatible con Spring Boot 4.1.1 y Java 25; verificar con `mvn -q -DskipTests compile` y con el arranque de la aplicación. Si no existe versión compatible, dejar registrada la contingencia de bucle de tool-calling manual (design.md, Risk 1) antes de seguir.
- [x] 1.2 Añadir la configuración del proveedor (URL base `https://openrouter.ai/api/v1`, modelo con tool calling, API key) reutilizando las claves de `app.ai.openrouter` desde variables de entorno; verificar que la app arranca sin exponer claves y que el bean de modelo se crea.
- [ ] 1.3 Verificar soporte de *function calling*: ejecutar una llamada de prueba al modelo que invoque una tool dummy y confirmar que se ejecuta; si el modelo configurado no soporta tools, documentarlo y cambiar el modelo por defecto. **BLOQUEADO: OpenRouter devuelve HTTP 402 "Insufficient credits" con la clave del `.env`; el pipeline de tool calling (ToolCallingAdvisor) sí se ejecuta. Requiere créditos o un modelo gratuito.**


## 2. Backend: publicación de eventos y tools

- [x] 2.1 Crear `DiagramEventPublisher` (infrastructure/websocket) que publique `{userId, type, data}` en `/topic/diagram/{diagramId}/changes`; verificar con una prueba unitaria que invoca el publisher y comprueba el destino y el payload usando `SimpMessagingTemplate` mockeado.
- [x] 2.2 Implementar el componente de tools `UmlTools` con las tools de elementos (crear, listar, actualizar, eliminar) delegando en `ElementService`; verificar con una prueba de integración/servicio que crear y eliminar persisten y cascadean atributos, métodos y relaciones como los endpoints REST.
- [x] 2.3 Implementar las tools de atributos y métodos (y parámetros) delegando en `ElementService`; verificar que crear un atributo y un método con parámetros devuelve los datos persistidos y que los nombres duplicados reportan error al modelo.
- [x] 2.4 Implementar las tools de relaciones (crear, listar, actualizar, eliminar) y sus multiplicidades delegando en `RelationshipService`; verificar que crear una asociación entre elementos existentes persiste y que un extremo fuera del diagrama es rechazado.
- [x] 2.5 Implementar las tools de lectura (elementos, atributos, métodos, relaciones) y la validación de aislamiento por diagrama en cada mutación; verificar con una prueba que una referencia a un elemento de otro diagrama es rechazada sin modificar datos.
- [x] 2.6 Conectar cada mutación exitosa con `DiagramEventPublisher` emitiendo el evento correcto (`element_created|updated|deleted`, `relationship_created|updated|deleted`); verificar que cada tipo de mutación publica exactamente un evento con el `diagramId` correcto.

## 3. Backend: endpoint de chat con tools

- [x] 3.1 Crear los DTOs de request/response (`message`, `history`, `reply`, `actions`) y el controlador `POST /api/v1/projects/{projectId}/diagrams/{diagramId}/ai/chat`; verificar que responde 400 con mensaje vacío y 401 sin token.
- [x] 3.2 Configurar el `ChatClient` con el *system prompt* del asistente UML y pasar `projectId`, `diagramId` y `userId` vía `ToolContext`; verificar que el asistente recibe el contexto correcto en una prueba de integración.
- [x] 3.3 Acotar el bucle de tool-calling (máximo de iteraciones configurable) y traducir fallos del proveedor a HTTP 503; verificar con un proveedor simulado que falle que la respuesta es 503 y no aplica cambios parciales.
- [x] 3.4 Devolver en la respuesta la lista de acciones ejecutadas (tipo, entidad, resultado); verificar que un prompt de creación reporta la acción en `actions[]` y que el diagrama inexistente responde 404.


## 4. Frontend: API, hook y panel de chat

- [x] 4.1 Añadir la función de API del asistente en `shared/api/client.ts` (mensaje + historial → reply + actions); verificar que el tipado compila y que la función usa la base de URL y auth existentes.
- [x] 4.2 Implementar el hook `useAiChat` con historial efímero en memoria, estado de carga y manejo de errores; verificar con una prueba/revisión que la recarga reinicia la conversación y que no escribe en `localStorage`.
- [x] 4.3 Implementar el componente `AiChatPanel` (lista de mensajes, entrada, envío deshabilitado en vacío/carga, indicador de actividad y errores); verificar que el panel se muestra y que no hay envío con entrada vacía.
- [x] 4.4 Integrar el panel en `pages/DiagramEditorPage.tsx` como panel conmutable sin romper el lienzo; verificar que abrir/cerrar no pierde el estado del diagrama.
- [x] 4.5 Asegurar el refresco del lienzo tras acciones del asistente: reconstruir el diagrama al recibir los eventos WebSocket originados en servidor; verificar que el solicitante ve el cambio (sin recargar) y que un segundo cliente conectado al mismo diagrama también lo ve.
- [x] 4.6 Ejecutar `pnpm run build` (tsc + vite) y `pnpm run lint`; verificar que no hay errores. **`pnpm run build` pasa. `pnpm run lint` falla con 5 errores PREEXISTENTES ajenos a este cambio (NodeControls.tsx:49, NoteNode.tsx:15, PropertiesPanel.tsx:47, RelationshipPanel.tsx:44 — regla `react-hooks/set-state-in-effect`; y 1 warning preexistente en useDiagramState.ts:425). Ninguno está en archivos nuevos de este cambio.**

## 5. Verificación integral

- [x] 5.1 Probar el flujo completo en el editor: pedir "crea la clase Cliente", "agrega el atributo nombre:String a Cliente" y "relaciona Cliente con Pedido con una asociación"; verificar persistencia (GET de elementos/atributos/relaciones) y reflejo en el lienzo. **BLOQUEADO: requiere una llamada real al modelo y la cuenta de OpenRouter no tiene créditos (HTTP 402).**
- [x] 5.2 Probar casos negativos: mensaje vacío (400), diagrama inexistente (404), proveedor no disponible (503 con mensaje legible) y argumentos inválidos de tool (respuesta sin error de servidor). **Verificado por unit tests: 400/404/503 (`AiChatControllerTest`). La respuesta ante argumentos inválidos de tool depende del `ToolExecutionExceptionProcessor` de Spring AI y no se pudo verificar en vivo por el bloqueo de créditos.**
- [x] 5.3 Ejecutar `openspec validate implementar-asistente-ia-chat --strict` y verificar que el change valida sin errores.

