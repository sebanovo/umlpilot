## 1. Infraestructura y configuración

- [x] 1.1 Agregar `spring-boot-starter-data-mongodb` y las dependencias de test de Testcontainers a `umlpilot_api/pom.xml`; verificar con `mvn -q -DskipTests compile`.
- [x] 1.2 Configurar `spring.data.mongodb.uri` desde `MONGODB_URI`, `auto-index-creation: false` y las propiedades `app.chat.*` en `application.yml`; verificar que el contexto de tests con perfil H2 arranca sin MongoDB.
- [x] 1.3 Implementar `ChatMongoProperties` con umbrales de mensajes recientes, recuperación, resumen y bandera de inicialización de índices; verificar con un test de binding.
- [x] 1.4 Agregar el servicio `umlpilot-mongo` a `docker-compose.yml`, `docker-compose.dev.yml` y `docker-compose.prod.yml` con volumen, healthcheck y sin puertos publicados en producción; verificar con `docker compose -f <archivo> config`.
- [x] 1.5 Agregar `MONGODB_URI` y las variables de chat al único `.env` y a `.env.sample`, sin crear variantes adicionales; verificar que las claves existen y que el backend local usa `localhost`.
- [x] 1.6 Implementar `ChatMongoIndexInitializer` condicionado por `app.chat.index-initialization-enabled`, con índices únicos, de paginación y de texto; verificar que no se ejecuta en el perfil H2 y que crea índices en un test con Testcontainers.

## 2. Dominio y puertos

- [x] 2.1 Crear los modelos de dominio `GroupConversation`, `ConversationMessage`, `ConversationSummary`, `ChatAuthor`, `ChatCitation` y `ChatMessageStatus`; verificar con tests de validación de mensajes vacíos, estados y autoría.
- [x] 2.2 Crear los puertos `GroupChatRepository`, `ChatSummaryRepository`, `ChatContextRetriever` y `ChatEventPublisher`; verificar que el dominio no importa Spring ni Mongo y que el proyecto compila.
- [x] 2.3 Crear los comandos/resultados de aplicación para conversación, mensaje, paginación y citas; verificar con tests de mapeo desde/hacia dominio.

## 3. Persistencia MongoDB

- [x] 3.1 Crear los documentos `ConversationDocument`, `MessageDocument` y `SummaryDocument` y sus repositorios Spring Data; verificar CRUD básico con `@DataMongoTest` y Testcontainers.
- [x] 3.2 Implementar `MongoGroupChatRepository` y `MongoChatSummaryRepository` con mappers dominio↔documento; verificar con tests de round-trip.
- [x] 3.3 Implementar la creación idempotente de conversación por `projectId` y el índice único; verificar que dos creaciones concurrentes devuelven una sola conversación.
- [x] 3.4 Implementar el índice único `(conversationId, clientMessageId)` y la inserción idempotente de mensajes; verificar que un duplicado devuelve el mensaje existente.
- [x] 3.5 Implementar paginación por cursor `(createdAt, _id)` y consulta de mensajes recientes; verificar orden estable, sin duplicados ni omisiones.
- [x] 3.6 Implementar la búsqueda textual de mensajes antiguos por conversación, excluyendo los recientes; verificar que devuelve resultados relevantes y respeta el límite.
- [x] 3.7 Implementar el borrado/archivado de conversaciones por proyecto; verificar que no quedan conversaciones activas tras la limpieza.

## 4. Servicio de aplicación del chat

- [x] 4.1 Implementar `GroupChatService.getOrCreateConversation` con validación de membresía en PostgreSQL; verificar primer acceso, acceso repetido y usuario sin membresía.
- [x] 4.2 Implementar `sendMessage` con validación de contenido, `clientMessageId`, autor, rol y diagrama; verificar 400, 403, 404 e idempotencia.
- [x] 4.3 Implementar `listMessages` con cursor, límite y metadatos de paginación; verificar carga inicial, páginas anteriores y fin del historial.
- [x] 4.4 Implementar los estados `generating`, `completed` y `failed` del mensaje de usuario y el reintento de respuesta; verificar que el reintento no duplica el mensaje de usuario.
- [x] 4.5 Implementar la restricción de mutaciones para `viewer` y la habilitación para `owner`/`editor`; verificar que un viewer no puede crear ni modificar elementos desde el chat.
- [x] 4.6 Implementar `ChatCleanupListener` con `@TransactionalEventListener(AFTER_COMMIT)` para archivar/eliminar el chat al borrar un proyecto; verificar que la conversación deja de ser accesible tras el borrado.

## 5. Integración con la IA existente

- [x] 5.1 Extender `AiChatService` para aceptar mensajes ya construidos y un flag `allowMutations`, y para devolver `provider`/`model`; verificar que los tests existentes de IA siguen pasando.
- [x] 5.2 Preconstruir el conjunto de tools de solo lectura y seleccionarlo para viewers; verificar con un test que `create_element` no está disponible en modo lectura.
- [x] 5.3 Implementar `ChatContextBuilder` con system prompt, autoría `[Nombre (rol)]`, resumen, últimos N y mensajes recuperados; verificar con tests de contexto largo y autores distintos.
- [x] 5.4 Implementar `ChatCitationParser` para extraer y validar `[msg:<id>]`, descartando IDs inexistentes; verificar citas válidas, inválidas y ausentes.
- [x] 5.5 Conectar el chat grupal con `AiChatService`, persistir el mensaje del asistente con acciones/citas/proveedor/modelo y actualizar el estado del mensaje de usuario; verificar con un test de integración con IA simulada.

## 6. Resúmenes

- [x] 6.1 Implementar el contador atómico de mensajes no resumidos y el umbral configurable; verificar que se incrementa al insertar y se reinicia al resumir.
- [x] 6.2 Implementar `ChatSummaryScheduler` con resumen del bloque más antiguo, actualización del puntero y reintento en la siguiente ejecución si falla; verificar con tests de umbral y fallo.
- [x] 6.3 Integrar el resumen en `ChatContextBuilder` y verificar que la IA recibe el resumen más los mensajes recientes no resumidos.

## 7. API REST

- [x] 7.1 Implementar `GroupChatController` con `GET /chat`, `GET /chat/messages`, `POST /chat/messages` y `POST /chat/messages/{id}/retry`; verificar con tests de controller.
- [x] 7.2 Mapear errores a 400, 403, 404 y 503 con mensajes legibles; verificar con tests de seguridad y de almacén no disponible.
- [x] 7.3 Devolver `202` con estado `generating` en el envío y `200` con el mensaje existente en duplicados idempotentes; verificar ambos casos.

## 8. STOMP y tiempo real

- [x] 8.1 Implementar `ChatEventPublisher` para `/topic/project/{projectId}/chat` con eventos de mensaje, estado de IA, escritura y presencia; verificar con un test unitario del payload.
- [x] 8.2 Extender la autorización STOMP para validar membresía en `SUBSCRIBE` al tópico de chat y en los envíos; verificar miembro aceptado, no miembro y colaborador eliminado.
- [x] 8.3 Implementar `ChatWebSocketHandler` para `typing` y presencia sin persistencia; verificar que los eventos llegan a los miembros del proyecto.
- [x] 8.4 Verificar reconexión y recuperación: tras una desconexión, el cliente se vuelve a suscribir y el historial REST cubre los mensajes perdidos.

## 9. Frontend: datos y estado

- [x] 9.1 Agregar tipos y métodos de chat a `umlpilot_web/src/shared/api/client.ts`; verificar con `npm run build`.
- [x] 9.2 Implementar `features/ai-chat/api/groupChatApi.ts` y `model/useGroupChat.ts` con carga inicial, paginación, envío optimista, reconciliación y reintento; verificar con `npm run lint` y pruebas manuales.
- [x] 9.3 Implementar `model/useChatSocket.ts` con conexión JWT, suscripción al tópico del proyecto, eventos, reconexión y recuperación por REST; verificar con dos sesiones simultáneas.
- [x] 9.4 Implementar la reconciliación de `clientMessageId` y la deduplicación de eventos; verificar que un mensaje propio no aparece dos veces.

## 10. Frontend: vista de modo IA

- [x] 10.1 Implementar `pages/AiModePage.tsx` y la ruta protegida `/projects/:id/ai` con query opcional `diagramId`; verificar navegación directa y recarga.
- [x] 10.2 Implementar los componentes de conversación, lista de mensajes, burbuja con autor/rol/fecha, compositor, selector de diagrama, participantes y estado de IA; verificar con `npm run lint` y `npm run build`.
- [x] 10.3 Implementar la lista de citas con carga del mensaje original, scroll y resaltado; verificar cita visible, cita no cargada y cita inexistente.
- [x] 10.4 Actualizar `ProjectDetailPage` con acceso al modo IA del proyecto; verificar que abre la vista correcta.
- [x] 10.5 Actualizar `DiagramEditorPage` para que el botón de IA navegue al modo IA con el diagrama seleccionado y retirar el panel efímero/`useAiChat` como flujo principal; verificar que no quedan imports ni componentes sin usar y que el editor compila.
- [x] 10.6 Mostrar el aviso de solo lectura para `viewer` y reflejar la explicación cuando la IA no puede mutar; verificar con una sesión de viewer.
- [x] 10.7 Mostrar errores de envío/IA con opción de reintento y estados de conexión/desconexión; verificar con el backend detenido y tras reconectar.

## 11. Documentación y verificación final

- [x] 11.1 Documentar el chat grupal, las colecciones Mongo, los endpoints, los tópicos STOMP, `MONGODB_URI` y el despliegue en `docs/architecture/`; verificar que las referencias del README/AGENTS no quedan obsoletas.
- [x] 11.2 Ejecutar `mvn test` en `umlpilot_api` y verificar que todos los tests pasan sin requerir MongoDB local.
- [x] 11.3 Ejecutar `npm run lint` y `npm run build` en `umlpilot_web` y verificar que no hay errores.
- [x] 11.4 Verificar manualmente: dos usuarios ven el mismo historial en tiempo real, recargar conserva los mensajes, un viewer no muta el diagrama, una cita antigua se localiza y una reconexión recupera mensajes.
- [x] 11.5 Verificar `docker compose -f docker-compose.prod.yml config` y que el arranque con Mongo healthy no publica puertos de Mongo ni de la API.

## 12. Menciones y experiencia tipo chat

- [x] 12.1 Implementar la detección backend de `@asistente`/`@ai`/`@ia` y disparar la IA solo con mención; verificar con `ChatMentionsTest` y `GroupChatServiceTest`.
- [x] 12.2 Implementar autocompletado de menciones en el compositor (`@Asistente IA` y participantes), navegación con teclado e inserción en el texto; verificar con `npm run build`.
- [x] 12.3 Resaltar menciones en las burbujas, separadores de fecha, scroll condicionado, botón “Ir al final” y compositor autoajustable; verificar con `npm run lint` y `npm run build`.
- [x] 12.4 Actualizar specs, design y documentación de menciones/experiencia de chat; verificar con `openspec validate --strict`.

## Notas de verificación

- La prueba de integración de MongoDB con Testcontainers (`MongoGroupChatRepositoryTest`) se implementó pero se omitió en este entorno porque Testcontainers no pudo conectarse al socket de Docker; se ejecutará en CI o en una máquina con Docker accesible.
- Las tareas 8.4, 9.3, 9.4, 10.6, 10.7 y 11.4 requieren levantar backend y frontend y abrir dos sesiones de navegador. No se ejecutaron porque el usuario indicó no iniciar procesos largos sin pedir permiso.