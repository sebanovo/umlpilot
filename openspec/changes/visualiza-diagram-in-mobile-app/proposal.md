## Why

UMLPilot hoy solo es usable desde la web. El proyecto móvil existe, pero únicamente tiene un chat local con Ollama y no puede autenticarse, ver proyectos ni abrir diagramas. Para que un usuario pueda revisar y editar sus diagramas desde el teléfono, la app Flutter debe ser un cliente real del mismo backend (REST + STOMP) y respetar las mismas reglas de acceso.

## What Changes

- Reestructurar `umlpilot_mobile` en capas (`core/`, `features/`, `injection/`, `routes/`, `theme/`) siguiendo MVVM + Clean Architecture.
- Autenticación real: login, registro, sesión persistente y refresh de JWT.
- Proyectos: listar, crear, ver detalle, editar y eliminar; acceso solo por membresía aceptada.
- Colaboradores: invitar por email con búsqueda, cambiar rol (owner), eliminar y aceptar/rechazar invitaciones.
- Bandeja de notificaciones con contador de no leídas, marcar leídas y responder invitaciones.
- Diagramas: listar, crear, eliminar, bloquear y desbloquear.
- Editor de diagramas móvil: canvas con zoom/pan, elementos (clase/interfaz/enumeración/nota), relaciones con multiplicidades, panel de propiedades (atributos, métodos, parámetros, literales), selección y eliminación.
- Colaboración en tiempo real por STOMP: presencia, cambios remotos y refresco del canvas.
- Compartir: enlace público con rol configurable (lector/editor), vista previa sin cuenta, unirse al proyecto e invitación por email.
- Asistente IA usando los endpoints del backend (chat, transcripción y diagrama desde texto/imagen).
- **BREAKING**: eliminar el chat directo con Ollama, la dependencia de su API y las variables `OLLAMA_URL` / `OLLAMA_MODEL`.
- Importación XMI, exportación XMI (generada en el cliente) y descarga del backend generado.
- Design system: tokens de color, tipografía, espaciado, radios y sombras mapeados a `ThemeData` y componentes reutilizables.
- Configuración por `.env` con `API_BASE_URL` y `APP_NAME`.

## Capabilities

### New Capabilities
- `mobile/app-foundation`: arranque, configuración `.env`, inyección de dependencias, ruteo, tema y design system, cliente HTTP, sesión y manejo de errores.
- `mobile/auth`: login, registro, logout, refresh de sesión y rutas protegidas.
- `mobile/projects`: proyectos, colaboradores/roles y bandeja de notificaciones.
- `mobile/diagram-editor`: lista/CRUD de diagramas, canvas editable, propiedades, bloqueo, colaboración en tiempo real, XMI y generación de backend.
- `mobile/sharing`: enlace público con rol, vista previa, unirse al proyecto e invitación por correo.
- `mobile/ai-assistant`: chat y construcción de diagramas desde texto/imagen usando el backend.

### Modified Capabilities
<!-- Ninguna: el backend y sus contratos REST/STOMP no cambian. -->

## Impact

- `umlpilot_mobile/lib/**` (reescritura por capas), `pubspec.yaml`, `.env` / `.env.sample`, `android/app/src/main/AndroidManifest.xml`.
- Consume los endpoints existentes de `umlpilot_api`: auth, projects, collaborators, notifications, users, diagrams, elements, relationships, AI, import XMI, generación de backend y compartir; y el canal STOMP `/ws`.
- Dependencias previstas: `dio`, `go_router`, `provider`, `get_it`, `flutter_secure_storage`, `stomp_dart_client`, `json_annotation`/`json_serializable`, `file_picker`, `share_plus`, `path_provider`, `intl` y `record` (dictado).
- Sin cambios de backend previstos. Si falta un contrato, se resolverá en un cambio separado.
- Riesgo principal: la paridad del editor visual en Flutter; se implementará por fases con pruebas.
