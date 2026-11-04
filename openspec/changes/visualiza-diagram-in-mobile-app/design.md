## Context

Ver `proposal.md` - Why. El punto de partida es `umlpilot_mobile`: hoy solo contiene un `main.dart`, un tema oscuro y un chat que llama directo a Ollama. No hay auth, proyectos, diagramas ni cliente del backend. El backend (`umlpilot_api`) ya expone todo lo necesario por REST y STOMP, y la web es la referencia funcional.

Restricciones:
- Seguir `docs/architecture/mobile-mvvm.md` (MVVM + capas Clean: datos → dominio → presentación, flujo unidireccional).
- Seguir `docs/design-system.md` (tokens de color, tipografía, espaciado, radios, sombras) y la estética oscura del editor web.
- Configuración por `umlpilot_mobile/.env` (`API_BASE_URL`, `APP_NAME`). Sin Ollama.
- El proyecto Android ya existe; no hay carpeta `ios/`.

## Goals / Non-Goals

**Goals:**
- Cliente Flutter con paridad funcional con la web sobre el mismo backend.
- Capas desacopladas y testeables: entidades y contratos en `domain`, implementaciones en `data`, estado en `presentation` (ViewModels).
- Editor UML táctil usable: seleccionar, mover, crear elementos/relaciones, editar propiedades.
- Reutilizar contratos existentes; cero cambios de backend en este cambio.

**Non-Goals:**
- Inferencia local o cualquier uso de Ollama.
- Push notifications nativas o modo offline con base de datos local.
- Configuración/build de iOS (el código será cross-platform, pero el scaffolding iOS queda para otro cambio).
- Target Flutter web; el objetivo de este cambio es Android (mobile). El almacenamiento seguro se diseña para móvil.
- Edición colaborativa CRDT; se mantiene el modelo de la web (última escritura gana + refresco completo).

## Decisions

### D1. Estructura por capas y features
`lib/core`, `lib/features/<feature>/{data,domain,presentation}`, `lib/injection`, `lib/routes`, `lib/theme`, `lib/main.dart`.
Cada feature sigue el flujo `View → ViewModel → UseCase → Repository (interfaz) → DataSource → API`.

Alternativas: una sola carpeta `screens/` (descartada: no escala ni permite testear dominio); Clean Architecture completa con `entities/` global (descartada: el doc recomienda empezar simple y crecer).

### D2. Estado con `provider` + `ChangeNotifier`
Se mantiene `provider` (ya en `pubspec`) y se usa un ViewModel por pantalla/flujo. El ViewModel expone estado inmutable (`loading`, `error`, listas) y métodos de intención; la Vista solo consume.

Alternativas: BLoC/Cubit (más boilerplate para un equipo pequeño; el doc lo sugiere para apps medianas, pero `provider` ya está y es suficiente); Riverpod (no está en el proyecto).

### D3. DI con `get_it`
Registro de `Dio`, almacenamiento seguro, data sources, repositorios y use cases en `injection/injection.dart`, y `ChangeNotifierProvider` a nivel de ruta/pantalla para ViewModels.

Alternativa: service locator manual o `provider` para todo (descartada: mezcla responsabilidades).

### D4. HTTP con `dio` e interceptores
- `AuthInterceptor`: agrega `Authorization: Bearer`.
- `RefreshInterceptor`: ante 401 intenta refresh **una sola vez** (single-flight con cola) y reintenta; si falla, limpia sesión y emite `SessionExpired`.
- `ErrorInterceptor`: mapea `{ "error": ... }` a `ServerException`.
- Timeouts explícitos.

Alternativas: `http` (ya presente) no trae interceptores ni descarga con progreso, necesarios para ZIP/XMI.

### D5. Sesión en `flutter_secure_storage`
Access token, refresh token y usuario serializado. Restauración en arranque antes de decidir la ruta inicial.

Alternativa: `shared_preferences` (descartada por seguridad). En Flutter web `flutter_secure_storage` usa WebCrypto con limitaciones: si se ejecuta en web, degradar a memoria + `localStorage` documentado.

### D6. Ruteo con `go_router`
Rutas: `/login`, `/register`, `/projects`, `/projects/:id`, `/projects/:id/diagrams/:diagramId`, `/share/:token`.
`redirect` global: sin sesión, rutas protegidas → `/login?from=...`; con sesión, `/login|/register` → `/projects`.
`/share/:token` es pública (preview) y su acción "Unirme" requiere sesión.

Alternativa: `Navigator 2.0` manual (descartada por complejidad).

### D7. Modelos `json_serializable` + mappers
DTOs con `json_serializable` (build_runner en dev) y mappers explícitos DTO ↔ entidad de dominio. Los ViewModels nunca ven JSON.

Alternativa: `freezed` (más dependencias; no está en el proyecto).

### D8. Canvas UML propio
Composición: `InteractiveViewer` (pan/zoom) → `Stack` con `Positioned` para nodos → `CustomPaint` debajo para aristas/flechas/multiplicidades.
- Coordenadas del servidor (`positionX`, `positionY`) en el espacio del canvas; conversión pantalla→canvas con la transformación de `InteractiveViewer`.
- Nodos: widgets con ancho mínimo fijo, alto automático; selección por tap; drag con `GestureDetector` y snap a 15 px.
- Relaciones: se crean con taps (origen → destino) en lugar de arrastrar handles (ergonomía táctil); se dibujan con `CustomPaint` y estilos por tipo (asociación, herencia, realización, agregación, composición, clase de asociación).
- Propiedades en `showModalBottomSheet` para no robar ancho al canvas en móvil.

Alternativas: paquetes de grafos (poca personalización UML, mantenimiento incierto); `flame` (motor de juegos, sobredimensionado).

### D9. Tiempo real con `stomp_dart_client`
Conexión a `{wsBase}/ws` con `Authorization`; `joinDiagram`/`leaveDiagram`, suscripción a `/topic/diagram/{id}/presence` y `/changes`; los cambios locales se publican en `/app/diagram/update`.
Ante un cambio remoto se hace `rebuild` desde REST (como la web) en lugar de aplicar parches locales; reconexión con backoff.

Alternativa: websockets crudos (fuera de contrato con el backend).

### D10. Design system en Flutter
`AppColors`, `AppTypography`, `AppSpacing`, `AppRadius`, `AppShadows` traducen los tokens de `docs/design-system.md`.
Tema: `ThemeData` con `blue-600` como primario y semánticos (`green-500`, `red-500`, `yellow-500`). El workspace/editor usa fondo oscuro (`gray-950`, nodos `gray-900`) para igualar la web; login/registro pueden usar superficies claras del design system.
Componentes compartidos en `core/widgets`: `AppButton`, `AppTextField`, `AppCard`, `EmptyState`, `ErrorState`, `LoadingState`, `ConfirmDialog`, `AppSnackBar`.

### D11. Entorno
`flutter_dotenv` carga `.env`. `ApiConstants` lee `API_BASE_URL` y `APP_NAME`; `WsConstants` deriva `ws/wss` desde `API_BASE_URL` + `/ws`. Se eliminan `OLLAMA_URL`, `OLLAMA_MODEL`, `lib/features/chat/**` y cualquier referencia a Ollama en `pubspec`, README y `.env`.

### D12. Archivos, exportación y voz
- XMI import: `file_picker` (`.xmi`, `.xml`) → multipart a `/import/xmi`.
- XMI export: generación local del XML (misma lógica que la web) → `share_plus`/guardado.
- Backend ZIP: `dio.download` a `path_provider.temp` → `share_plus`.
- Imagen para IA: `file_picker` (galería/cámara) → multipart.
- Voz: `record` → archivo temporal (`.m4a`/`.wav`) → multipart a `/ai/transcribe`.

### D13. Errores y permisos
`exceptions.dart` (data) → `failures.dart` (dominio) → estado del ViewModel. 401 con refresh fallido → logout; 403 → mensaje de permisos y, en el editor, cambio a solo lectura o salida. Nunca se muestra un error crudo de red.

### D14. Pruebas
- Unitarias: ViewModels y use cases con repositorios falsos (`mocktail`).
- Widget: login, listas, diálogos y estados de error/vacío.
- Integración: login → crear proyecto → abrir/editar diagrama → compartir (con backend de prueba o `DioAdapter` mock).
- `flutter analyze` y `flutter test` como puertas de calidad.

### Contrato de API consumido
| Área | Endpoints |
|------|-----------|
| Auth | `POST /api/v1/auth/login`, `/register`, `/refresh` |
| Proyectos | `GET/POST /api/v1/projects`, `GET/PUT/DELETE /api/v1/projects/{id}` |
| Colaboradores | `GET/POST /projects/{id}/collaborators`, `PUT /{userId}`, `DELETE /{userId}` |
| Notificaciones | `GET /notifications`, `/unread-count`, `PUT /{id}/read`, `/read-all`, `POST /{id}/respond` |
| Usuarios | `GET /api/v1/users/search?q=` |
| Diagramas | `GET/POST /projects/{id}/diagrams`, `GET/PUT/DELETE /{diagramId}`, lock/unlock |
| Elementos | `/elements` + atributos, métodos, parámetros, literales |
| Relaciones | `/relationships` + multiplicidades |
| IA | `/ai/chat`, `/ai/transcribe`, `/ai/diagram-from-text`, `/ai/diagram-from-image` |
| XMI / ZIP | `POST /import/xmi`, `GET /generate/backend` |
| Compartir | `GET/POST/DELETE /diagrams/{id}/share`, `GET /public/diagrams/{token}`, `POST /public/diagrams/{token}/join` |

## Risks / Trade-offs

- **Editor visual complejo** → se implementa por fases (render → interacción → propiedades → tiempo real) con pruebas de widget y un modo de solo lectura temprano para validar el layout.
- **Rendimiento con muchos nodos** → `CustomPaint` para aristas, `RepaintBoundary` por nodo, evitar reconstrucciones globales; refresco completo solo ante cambios remotos.
- **Conflictos de colaboración** → última escritura gana y refresco completo; igual que la web. Sin CRDT en este cambio.
- **Refresh token concurrente** → refresco single-flight y cola de peticiones pendientes.
- **`flutter_secure_storage` en web** → limitaciones conocidas; se documenta y se usa memoria como fallback si el target web se mantiene.
- **Cleartext HTTP en Android** → solo desarrollo (`10.0.2.2`/`localhost`); producción debe usar HTTPS. Se configura `usesCleartextTraffic` para dev y se documenta.
- **Sin carpeta `ios/`** → se implementa Android-first con código cross-platform; el scaffolding iOS queda como seguimiento.

## Migration Plan

1. Actualizar `pubspec.yaml` (dependencias nuevas) y `.env`/`.env.sample` (quitar Ollama; mantener `API_BASE_URL`/`APP_NAME`).
2. Crear la base (`core`, `theme`, `routes`, `injection`) y eliminar `features/chat` y sus referencias a Ollama.
3. Implementar auth y el shell de navegación (login → proyectos).
4. Implementar proyectos, colaboradores, notificaciones y compartir.
5. Implementar diagramas y el editor por fases.
6. Añadir tiempo real, XMI, generación y asistente IA.
7. Ejecutar `flutter analyze`, `flutter test` y build Android.

Rollback: revertir el commit del cambio; `API_BASE_URL` y `APP_NAME` ya existen, por lo que el `.env` sigue siendo válido. El backend no se modifica.


