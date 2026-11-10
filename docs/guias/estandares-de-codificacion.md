# Anexo A. Estándares de codificación de UMLPilot

## A.1. Objetivo y alcance

Este anexo documenta las convenciones de codificación, organización y calidad adoptadas en UMLPilot. Su objetivo es que el código sea consistente, legible, mantenible y seguro, independientemente de quién lo escriba.

El estándar aplica a:

- **Backend:** Java 25, Spring Boot 4.1.1 y arquitectura hexagonal.
- **Frontend web:** React 19, TypeScript 6, Vite 8, Tailwind CSS 4 y Feature-Sliced Design (FSD).
- **App móvil:** Flutter/Dart, visualizador de solo lectura.
- **Persistencia:** PostgreSQL como fuente de verdad y MongoDB para el chat.
- **Integración con IA:** proveedores OpenAI-compatible con rotación y tools UML.

Las reglas descritas son obligatorias para el código nuevo y se aplican de forma progresiva al código existente.

---

## A.2. Convenciones generales

| Aspecto | Convención |
|---|---|
| Codificación de archivos | UTF-8 sin BOM |
| Idioma de identificadores | Inglés técnico (`GroupChatService`, `sendMessage`) |
| Idioma de documentación y UI | Español |
| Fin de línea | LF; se evita mezclar CRLF/LF |
| Indentación | 4 espacios en Java; 2 espacios en TypeScript/React; 2 espacios en Dart |
| Tabulaciones | Prohibidas |
| Longitud de línea | Máximo recomendado: 120 caracteres |
| Llaves | Estilo K&R (llave de apertura en la misma línea) |
| Código muerto | No se deja código comentado ni imports sin usar |
| Comentarios | Explican el “por qué”, no el “qué evidente” |
| Secretos | Nunca en código ni en el repositorio; se usan variables de entorno |

### A.2.1. Nombres

| Elemento | Convención | Ejemplo |
|---|---|---|
| Clases Java | `PascalCase` | `GroupChatService` |
| Interfaces Java | `PascalCase` | `ChatEventPublisher` |
| Métodos Java | `camelCase` | `sendMessage` |
| Variables Java | `camelCase` | `clientMessageId` |
| Constantes Java | `UPPER_SNAKE_CASE` | `MAX_MESSAGE_CHARS` |
| Enums Java | `UPPER_SNAKE_CASE` | `GENERATING` |
| Paquetes Java | minúsculas | `com.umlpilot.umlpilot_api.application.service` |
| Componentes React | `PascalCase.tsx` | `ChatComposer.tsx` |
| Hooks React | `camelCase` con `use` | `useGroupChat.ts` |
| Archivos TypeScript | `camelCase.ts` | `groupChatApi.ts` |
| Carpetas FSD | `kebab-case` | `features/ai-chat` |
| Clases Dart | `PascalCase` | `DiagramViewerPage` |
| Archivos Dart | `lower_snake_case.dart` | `diagram_viewer_page.dart` |
| Tablas PostgreSQL | `snake_case` | `collaborators`, `diagram_elements` |
| Campos MongoDB | `camelCase` | `conversationId`, `clientMessageId` |

### A.2.2. Principios de diseño

- **Responsabilidad única:** una clase, componente o función debe tener una razón clara para cambiar.
- **Bajo acoplamiento:** las dependencias apuntan hacia el dominio, nunca al revés.
- **Alta cohesión:** lo relacionado vive junto; lo independiente se separa.
- **Explícito sobre implícito:** nombres claros, DTOs explícitos y mappers visibles.
- **Fallar rápido:** validar entradas al inicio y devolver errores legibles.
- **No repetir lógica de negocio:** reutilizar servicios y puertos existentes.

---

## A.3. Backend: Java y Spring Boot

### A.3.1. Arquitectura hexagonal

El backend sigue la regla de dependencia de la arquitectura hexagonal:

```text
domain ← application ← infrastructure
```

| Capa | Responsabilidad | Prohibido |
|---|---|---|
| `domain` | Modelo, reglas de negocio, puertos de repositorio | Spring, JPA, Mongo, HTTP |
| `application` | Casos de uso, DTOs, orquestación, transacciones | Depender de adaptadores concretos |
| `infrastructure` | Controladores REST, JPA, Mongo, WebSocket, configuración | Contener reglas de negocio |

Reglas concretas:

- El dominio no importa `org.springframework.*`, `jakarta.persistence.*` ni `org.springframework.data.mongodb.*`.
- Las entidades JPA viven en `infrastructure/adapter/out/persistence`, separadas del dominio.
- Los documentos MongoDB viven en `infrastructure/adapter/out/persistence/mongo`.
- Los repositorios se declaran como puertos en `domain/repository` o `application/port/out` y se implementan en infraestructura.
- `@Transactional` se usa en servicios de aplicación, nunca en el dominio.
- Los controladores no contienen lógica de negocio: delegan en servicios de aplicación.
- Los DTOs de API viven en infraestructura; los comandos/resultados de aplicación en `application/dto`.

Referencia: `docs/architecture/backend-hexagonal.md`.

### A.3.2. Estructura de paquetes

```text
com.umlpilot.umlpilot_api
├── domain/
│   ├── model/
│   ├── repository/
│   └── exception/
├── application/
│   ├── dto/
│   ├── port/out/
│   └── service/
└── infrastructure/
    ├── adapter/in/web/
    ├── adapter/out/persistence/
    ├── ai/
    ├── config/
    ├── security/
    └── websocket/
```

### A.3.3. Formato Java

- 4 espacios por nivel.
- Una clase pública por archivo.
- Imports explícitos; no se usan `*` salvo en casos justificados.
- Se eliminan imports no usados.
- Métodos cortos y con una sola responsabilidad.
- Se prefieren `record` para DTOs y value objects inmutables.
- Se usan `List.of()`, `Map.of()` y `Optional` de forma consistente.
- Se evita `null` en retornos de colecciones; se devuelve lista vacía.
- Se usan `Instant` para fechas del chat y `LocalDateTime` solo donde el dominio existente lo requiere.

### A.3.4. Manejo de errores

- Los errores de negocio se representan con `DomainException`.
- Los controladores traducen excepciones a códigos HTTP claros:
  - `400` para entradas inválidas.
  - `401` para falta de autenticación.
  - `403` para falta de permisos.
  - `404` para recursos inexistentes.
  - `503` para dependencias no disponibles (MongoDB, proveedor IA).
- No se exponen stack traces, claves API ni detalles internos al cliente.
- Las tools de IA devuelven errores de validación al modelo, sin romper la conversación.

### A.3.5. Persistencia

- PostgreSQL es la fuente de verdad de usuarios, proyectos, diagramas, colaboradores y permisos.
- MongoDB almacena exclusivamente conversaciones, mensajes, resúmenes y metadatos del chat.
- No se realizan transacciones cruzadas PostgreSQL↔MongoDB; la consistencia es eventual y la membresía se valida siempre en PostgreSQL.
- Los documentos MongoDB usan un documento por mensaje; no se usan arreglos crecientes dentro de la conversación.
- Los índices se crean de forma explícita en `ChatMongoIndexInitializer`.
- Se respetan las convenciones de `docs/architecture/database.md`.

### A.3.6. Configuración y secretos

- La configuración vive en `application.yml` y se parametriza con variables de entorno.
- Las claves de IA, JWT y base de datos se leen desde `.env`/variables de entorno.
- No se commitea `.env` con secretos reales.
- No se hardcodean URLs de proveedores ni modelos; se usan propiedades como `AI_PROVIDER_ORDER`.
- Los valores por defecto deben ser seguros para desarrollo local.

### A.3.7. Logging

- Se usa SLF4J.
- No se registran contraseñas, tokens, claves API ni contenido sensible completo.
- Los errores recuperables se registran con contexto suficiente para diagnosticar.
- En el chat, los fallos del proveedor IA se registran sin perder el mensaje del usuario.

### A.3.8. Pruebas backend

- JUnit 5 + Mockito para pruebas unitarias.
- Pruebas de controlador para códigos HTTP y validaciones.
- Pruebas de integración con Testcontainers para MongoDB, omitidas automáticamente si Docker no está disponible.
- Nombres de test descriptivos en camelCase: `rejectsBlankUserMessage`, `viewerSendsWithoutMutationTools`.
- Comando de verificación: `mvn test`.
- Compilación rápida: `mvn -q -DskipTests compile`.

---

## A.4. Frontend web: React, TypeScript y FSD

### A.4.1. Organización FSD

El frontend se organiza por capas y slices:

```text
src/
├── pages/
├── features/
├── shared/
│   ├── api/
│   ├── lib/
│   └── ui/
└── types/
```

Reglas:

- Las dependencias fluyen hacia abajo: `pages → features → shared`.
- Cada feature agrupa su API, hooks y componentes.
- Los componentes reutilizables sin lógica de negocio viven en `shared/ui`.
- Las llamadas HTTP se centralizan en `shared/api/client.ts`.
- La lógica de WebSocket se encapsula en hooks (`useWebSocket`, `useChatSocket`).
- No se crean utilidades globales para lógica de un solo feature.

Referencia: `docs/architecture/frontend-fsd.md`.

### A.4.2. Formato TypeScript/React

- 2 espacios por nivel.
- Se usan comillas simples.
- Se evita `any`; se prefieren tipos explícitos e interfaces.
- Los componentes se declaran como funciones con nombre.
- Un componente por archivo; archivo con el mismo nombre del componente.
- Los hooks empiezan con `use` y se encuentran en `model/` o `hooks/`.
- Se prefieren `const` para valores que no se reasignan.
- Se evita lógica compleja dentro del JSX; se extrae a funciones o hooks.
- Se usan clases de Tailwind de forma consistente y con el tema oscuro del proyecto.
- Los formularios validan antes de enviar y deshabilitan acciones mientras hay una petición en curso.

### A.4.3. Estado y datos

- El estado local se maneja con `useState`/`useReducer`.
- El estado compartido se maneja con el contexto existente (`AuthProvider`, `ToastProvider`).
- No se agrega una librería de estado global sin justificarlo.
- Las llamadas al backend usan rutas relativas `/api` y el cliente centralizado.
- Los errores de red se muestran con mensajes legibles y opción de reintento cuando aplica.
- Los mensajes optimistas usan `clientMessageId` para reconciliarse con la respuesta del servidor.

### A.4.4. Accesibilidad y UX

- Todos los botones tienen texto o `title`/`aria-label`.
- Los formularios tienen labels asociados.
- Se respeta la navegación por teclado en el compositor del chat.
- Los estados de carga, error, vacío y desconectado son visibles.
- Se evita bloquear toda la interfaz por una petición; solo se bloquea la acción correspondiente.

### A.4.5. Calidad frontend

- ESLint con TypeScript, React Hooks y React Refresh.
- Comando de lint: `npm run lint`.
- Compilación TypeScript + Vite: `npm run build`.
- No se commitea código con errores de lint o build.
- Se permite `warning` de dependencias de hooks solo si está justificado; los errores no.

---

## A.5. App móvil: Flutter/Dart

### A.5.1. Convenciones

- Se sigue *Effective Dart*.
- Archivos en `lower_snake_case.dart`.
- Clases y widgets en `PascalCase`.
- Variables, métodos y parámetros en `camelCase`.
- Constantes en `lowerCamelCase` o `static const` según el contexto.
- Widgets pequeños y con una responsabilidad.
- La app es de solo lectura: no edita diagramas, no usa IA, no importa/exporta XMI y no genera código.

### A.5.2. Organización

- `lib/` se organiza por features: autenticación, proyectos, diagramas, visor, compartir.
- La lógica de API y WebSocket se separa de los widgets.
- Los modelos de datos se mantienen inmutables cuando sea posible.
- Se reutilizan componentes de visualización del diagrama en lugar de duplicarlos.

### A.5.3. Calidad móvil

- `flutter analyze` debe terminar sin errores.
- `flutter test` debe pasar en la suite existente.
- Las actualizaciones en tiempo real deben liberar suscripciones al salir de la vista.
- No se realizan operaciones de escritura desde la app móvil.

---

## A.6. Base de datos

### A.6.1. PostgreSQL

- Nombres de tablas y columnas en `snake_case`.
- Claves primarias UUID o String según el modelo existente.
- Fechas de auditoría con `created_at`/`updated_at` cuando aplique.
- No se elimina información en cascada sin revisar el impacto.
- Los permisos y la membresía se validan en la capa de aplicación.

### A.6.2. MongoDB

- Colecciones en plural: `conversations`, `messages`, `summaries`.
- Campos en `camelCase`.
- Un documento por mensaje.
- Índices únicos para `projectId` y para `(conversationId, clientMessageId)`.
- Índice de cursor para `(conversationId, createdAt, _id)`.
- Índice de texto sobre el contenido de los mensajes.
- No se usan transacciones cruzadas con PostgreSQL.

---

## A.7. Integración con IA

- Los proveedores se definen por configuración; no se hardcodean claves ni modelos.
- La rotación usa `AI_PROVIDER_ORDER` y respeta cooldowns por cuota/error.
- El chat grupal solo invoca a la IA cuando el mensaje menciona `@asistente` (o `@ai`/`@ia`).
- Las tools de mutación solo están disponibles para `owner`/`editor`; los `viewer` usan tools de solo lectura.
- Las respuestas del asistente incluyen proveedor y modelo usados, sin exponer credenciales.
- Las citas de mensajes antiguos se validan contra IDs reales.
- El contexto se compone de resumen, mensajes recientes y recuperación de mensajes antiguos; no se envía todo el historial.

---

## A.8. Seguridad

- Autenticación JWT en REST y WebSocket.
- Contraseñas con BCrypt.
- Validación de membresía en cada operación de proyecto, diagrama y chat.
- Validación de roles antes de mutar contenido.
- No se confía en el `projectId` enviado por el cliente sin comprobar pertenencia.
- Las suscripciones STOMP se autorizan en `SUBSCRIBE`.
- No se registran secretos ni tokens.
- Las entradas se validan y se escapan cuando se muestran.
- Los archivos subidos se limitan por tamaño y tipo.

---

## A.9. Git y revisión de código

### A.9.1. Ramas

```text
feature/<descripcion-corta>
fix/<descripcion-corta>
docs/<descripcion-corta>
```

### A.9.2. Commits

Se usa Conventional Commits:

```text
feat: agregar chat grupal persistente
fix: corregir autorizacion de suscripcion STOMP
docs: documentar estandares de codificacion
test: cubrir idempotencia de mensajes
refactor: mover mapeo de conversaciones a infraestructura
chore: actualizar dependencias
```

### A.9.3. Revisión

Antes de integrar:

- El código compila.
- `mvn test`, `npm run lint` y `npm run build` pasan.
- No se suben secretos.
- La arquitectura hexagonal/FSD se respeta.
- Se actualiza la documentación o specs si cambia el comportamiento.

---

## A.10. Herramientas y comandos de verificación

| Ámbito | Herramienta | Comando |
|---|---|---|
| Backend | Maven + JUnit 5 + Mockito | `mvn test` |
| Backend (compilación) | Maven | `mvn -q -DskipTests compile` |
| Frontend | ESLint | `npm run lint` |
| Frontend | TypeScript + Vite | `npm run build` |
| Móvil | Flutter Analyzer | `flutter analyze` |
| Móvil | Flutter Test | `flutter test` |
| OpenSpec | OpenSpec CLI | `openspec validate --specs` |

---

## A.11. Checklist de cumplimiento

- [ ] El código nuevo respeta la arquitectura hexagonal o FSD según corresponda.
- [ ] Los nombres siguen las convenciones de la sección A.2.1.
- [ ] No hay secretos ni credenciales en el repositorio.
- [ ] Los errores se manejan con códigos HTTP y mensajes claros.
- [ ] Las pruebas relevantes fueron agregadas o actualizadas.
- [ ] `mvn test`, `npm run lint` y `npm run build` pasan.
- [ ] `flutter analyze` y `flutter test` pasan cuando se modifica la app móvil.
- [ ] La documentación y las specs reflejan el comportamiento implementado.
- [ ] El commit sigue Conventional Commits.

---

## A.12. Referencias

- `docs/architecture/backend-hexagonal.md`
- `docs/architecture/frontend-fsd.md`
- `docs/architecture/database.md`
- `docs/architecture/group-chat.md`
- Google Java Style Guide
- Effective Dart
- Airbnb JavaScript/TypeScript Style Guide
- Conventional Commits 1.0.0
