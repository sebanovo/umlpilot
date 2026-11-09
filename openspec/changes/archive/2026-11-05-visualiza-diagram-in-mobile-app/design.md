## Context

Ver `proposal.md` - Why. El punto de partida es `umlpilot_mobile`: solo contiene un `main.dart`, un tema oscuro y un chat que llama directo a Ollama. No hay auth, proyectos ni cliente del backend. El backend (`umlpilot_api`) ya expone REST y STOMP; la web es la referencia funcional. Esta entrega móvil es **solo lectura de diagramas**: no se implementa edición, IA, XMI ni generación de backend.

Restricciones:
- Seguir `docs/architecture/mobile-mvvm.md` (MVVM + capas Clean).
- Seguir `docs/design-system.md` y la estética oscura del editor web.
- Configuración por `.env` (`API_BASE_URL`, `APP_NAME`, `WEB_BASE_URL`). Sin Ollama.
- Objetivo Android/web de Flutter; el código es cross-platform.

## Goals / Non-Goals

**Goals:**
- Cliente Flutter con autenticación, proyectos, colaboradores, notificaciones y compartir.
- Visor UML de solo lectura con renderizado fiel (tipos de relación, clases de asociación, recursivas, multiplicidades).
- Canvas con límites dinámicos (sin recortes) y zoom/pan/fit.
- Actualización en tiempo real por STOMP (presencia y cambios remotos).
- Capas desacopladas y testeables; sin cambios de backend.

**Non-Goals:**
- Edición del diagrama desde el móvil (elementos, relaciones, propiedades, bloqueo).
- Asistente IA, importación/exportación XMI y generación de backend en el móvil.
- Push notifications nativas y modo offline con base de datos local.
- Scaffolding/build de iOS.
- CRDT: se mantiene el modelo de la web (última escritura gana + refresco completo).

## Decisions

### D1. Estructura por capas y features
`lib/core`, `lib/features/<feature>/{data,domain,presentation}`, `lib/injection`, `lib/routes`, `lib/theme`, `lib/main.dart`.
Features: `auth`, `projects`, `collaborators`, `notifications`, `diagrams`, `diagram-editor` (modelos y repositorios de UML), `diagram-viewer` (UI de solo lectura), `sharing`.
Flujo: `View → ViewModel → UseCase → Repository (interfaz) → DataSource → API`.

Alternativa: una carpeta `screens/` (descartada: no escala); Clean Architecture completa con `entities/` global (descartada: el doc recomienda empezar simple).

### D2. Estado con `provider` + `ChangeNotifier`
Un ViewModel por pantalla/flujo. Expone estado inmutable (`loading`, `error`, listas) y métodos de intención; la vista solo consume.

Alternativas: BLoC/Cubit (más boilerplate; el proyecto ya usa `provider`), Riverpod (no está en el proyecto).

### D3. DI con `get_it`
Registro de `Dio`, almacenamiento seguro, data sources, repositorios y ViewModels en `injection/injection.dart`. `AuthViewModel` y `NotificationsViewModel` son singletons; los ViewModels de pantalla son factories.

### D4. HTTP con `dio`
Cliente central `ApiClient` con `Authorization: Bearer`, refresh **single-flight** ante 401, reintento único y traducción de errores a `ServerException`/`NetworkException`/`UnauthorizedException`/`ForbiddenException`.

Alternativa: `http` no trae interceptores ni descarga con progreso.

### D5. Sesión en `flutter_secure_storage`
Access token, refresh token y usuario serializado. Restauración al arrancar (en `main` y en el splash) para que los enlaces directos conozcan el estado de autenticación.

### D6. Ruteo con `go_router`
Rutas: `/splash`, `/login`, `/register`, `/projects`, `/projects/:projectId`, `/projects/:projectId/diagrams/:diagramId`, `/share/:token`.
`redirect` global: sin sesión, rutas protegidas → `/login?from=...`; autenticado en login/register/splash → `from` o `/projects`; `/share/:token` es pública.
Si la sesión no se restauró, se envía a `/splash`.

### D7. Modelos manuales
Entidades con `fromJson` explícito. Se evita codegen (`json_serializable`) para reducir dependencias y tiempo de build; el contrato se mantiene igual.

### D8. Canvas de solo lectura con límites dinámicos
Composición: `InteractiveViewer` (pan/zoom) → `GridPaper` (cuadrícula) → `Stack` con `Clip.none`; nodos como widgets `Positioned` y aristas en `CustomPaint`.
- **Límites dinámicos**: se calcula el rectángulo que contiene todos los nodos (con su tamaño estimado), se le añade un margen de 260 px y se traslada el origen. Así se soportan posiciones negativas y no se recortan elementos, etiquetas ni bucles (el problema del lienzo fijo de 4000 px).
- **Tamaño de nodo determinista**: `nodeSizeFor` estima alto/ancho (cabecera + filas de 18 px + divisores) y el nodo se fuerza a ese tamaño con `Positioned(width, height)`, de modo que las aristas y marcadores coinciden con el borde real.
- **Aristas**: línea entre los puntos de intersección con los rectángulos; marcador por tipo:
  - generalización/realización: triángulo hueco; realización con línea discontinua.
  - agregación: rombo hueco; composición: rombo lleno.
  - asociación/clase de asociación: flecha llena.
  - clase de asociación: línea discontinua desde el punto medio de la asociación hasta el nodo de la clase, con punto en el ancla.
  - recursiva: bucle Bézier a la derecha del nodo, dentro del margen del canvas.
  - multiplicidades y nombre con `TextPainter`.
- **Interacción**: tap en nodo para seleccionar; tap en arista (distancia a segmento < 18 px) para seleccionar; detalles en `showModalBottomSheet` de solo lectura; "Ajustar vista" calcula la matriz con el contenido.

Alternativas: paquete de grafos (poca personalización UML); `flame` (sobredimensionado). Un canvas fijo grande (descartado: recorta y consume memoria).

### D9. Tiempo real con `stomp_dart_client`
Conexión a `{wsBase}/ws` con `Authorization`; `joinDiagram`/`leaveDiagram`, suscripción a presencia y cambios. Ante un cambio remoto se hace un refresco REST con debounce (400 ms). El servicio expone streams y un flag `_disposed`: tras `dispose()` ignora callbacks y no escribe en streams cerrados (evita `Cannot add new events after calling close`).

### D10. Design system en Flutter
`AppColors`, `AppTypography`, `AppSpacing`, `AppRadius`, `AppShadows` traducen los tokens de `docs/design-system.md`. Tema oscuro (`gray-950`/`gray-900`, primario `blue-600`) para igualar el workspace web. Componentes compartidos: `AppButton`, `AppTextField`, `AppCard`, `LoadingState`, `EmptyState`, `ErrorState`, `showConfirmDialog`, `showAppSnackBar`.

### D11. Entorno
`flutter_dotenv` carga `.env`. `ApiConstants` lee `API_BASE_URL`, `APP_NAME` y `WEB_BASE_URL`; `wsUrl` deriva `ws/wss` desde la API. Se eliminan Ollama, `features/chat` y sus variables.

### D12. Compartir
`SharingRepository` gestiona estado/rol del enlace, activar/desactivar, vista pública y unión. La hoja de compartir muestra el rol, permite copiar/compartir la URL web (`WEB_BASE_URL + /share/token`) e invitar por email. La vista `/share/:token` es pública y de solo lectura; "Unirme" requiere sesión.

### D13. Errores y permisos
Excepciones de datos → fallos de dominio → estado del ViewModel. 401 con refresh fallido → logout; 403 → mensaje de permisos y salida del visor. Nunca se muestra un error crudo de red.

### D14. Pruebas
Unitarias de ViewModels y sesión con repositorios falsos; widget tests de estados compartidos. `flutter analyze` y `flutter test` como puertas de calidad. La ejecución y compilación manual del target queda a cargo del usuario.

### Contrato de API consumido
| Área | Endpoints |
|------|-----------|
| Auth | `POST /api/v1/auth/login`, `/register`, `/refresh` |
| Proyectos | `GET/POST /api/v1/projects`, `GET/PUT/DELETE /api/v1/projects/{id}` |
| Colaboradores | `GET/POST /projects/{id}/collaborators`, `PUT /{userId}`, `DELETE /{userId}` |
| Notificaciones | `GET /notifications`, `/unread-count`, `PUT /{id}/read`, `/read-all`, `POST /{id}/respond` |
| Usuarios | `GET /api/v1/users/search?q=` |
| Diagramas | `GET/POST /projects/{id}/diagrams`, `GET/DELETE /{diagramId}`, lock/unlock |
| Elementos (lectura) | `GET /elements`, `/{id}/attributes`, `/{id}/methods`, `/{id}/literals` |
| Relaciones (lectura) | `GET /relationships`, `/{id}/multiplicities` |
| Compartir | `GET/POST/DELETE /diagrams/{id}/share`, `GET /public/diagrams/{token}`, `POST /public/diagrams/{token}/join` |

## Risks / Trade-offs

- **Renderizado de aristas** → tamaños deterministas y margen dinámico; se valida visualmente en web/Android.
- **Rendimiento con muchos nodos** → `CustomPaint` para aristas y `RepaintBoundary` cuando haga falta; el lienzo solo crece con el contenido.
- **STOMP en web** → WebSocket sin cabeceras personalizadas; la autenticación viaja en los headers STOMP (`Authorization`). El backend ya valida la membresía.
- **Cierre del cliente** → flag `_disposed` y comprobaciones en todos los callbacks antes de emitir en streams.
- **Refresh concurrente** → single-flight con cola de reintento.
- **Sin carpeta `ios/`** → Android/web primero; el código es cross-platform.

## Migration Plan

1. Dependencias y `.env` (sin Ollama; añadir `WEB_BASE_URL`).
2. Base (`core`, `theme`, `routes`, `injection`) y eliminación de `features/chat`.
3. Auth y shell de navegación.
4. Proyectos, colaboradores, notificaciones y compartir.
5. Diagramas y visor de solo lectura con tiempo real.
6. `flutter analyze` y `flutter test`; ejecución manual del usuario.

Rollback: revertir el commit del cambio; `API_BASE_URL`/`APP_NAME` siguen siendo válidos. El backend no se modifica.
