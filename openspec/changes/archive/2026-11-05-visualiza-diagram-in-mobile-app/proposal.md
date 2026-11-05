## Why

UMLPilot hoy solo es usable desde la web. El proyecto móvil existe, pero únicamente tiene un chat local con Ollama y no puede autenticarse, ver proyectos ni abrir diagramas. Para que un usuario pueda consultar sus diagramas desde el teléfono, la app Flutter debe ser un cliente real del mismo backend (REST + STOMP) con visualización en tiempo real y modo solo lectura.

## What Changes

- Reestructurar `umlpilot_mobile` en capas (`core/`, `features/`, `injection/`, `routes/`, `theme/`) siguiendo MVVM + Clean Architecture.
- Autenticación real: login, registro, sesión persistente y refresh de JWT.
- Proyectos: listar, crear, ver detalle, editar y eliminar; acceso solo por membresía aceptada.
- Colaboradores: invitar por email con búsqueda, cambiar rol (owner), eliminar y aceptar/rechazar invitaciones.
- Bandeja de notificaciones con contador de no leídas, marcar leídas y responder invitaciones.
- Diagramas: listar, crear y eliminar; abrirlos en un **visor de solo lectura** con zoom/pan y ajuste a contenido.
- Renderizado UML completo en móvil: clases, interfaces, enumeraciones, notas, relaciones con multiplicidades, clase de asociación y relaciones recursivas. El canvas se dimensiona dinámicamente para no recortar elementos.
- Colaboración en tiempo real por STOMP: presencia, cambios remotos y refresco del canvas.
- Compartir: enlace público con rol configurable (lector/editor), vista previa sin cuenta, unirse al proyecto e invitación por email.
- **BREAKING**: eliminar el chat directo con Ollama, su dependencia y las variables `OLLAMA_URL` / `OLLAMA_MODEL`.
- Configuración por `.env` con `API_BASE_URL`, `APP_NAME` y `WEB_BASE_URL` (para enlaces compartidos).

## Capabilities

### New Capabilities
- `mobile/app-foundation`: arranque, configuración `.env`, inyección de dependencias, ruteo, tema y design system, cliente HTTP, sesión y manejo de errores.
- `mobile/auth`: login, registro, logout, refresh de sesión y rutas protegidas.
- `mobile/projects`: proyectos, colaboradores/roles y bandeja de notificaciones.
- `mobile/diagram-viewer`: gestión de diagramas y visor UML de solo lectura con renderizado completo y colaboración en tiempo real.
- `mobile/sharing`: enlace público con rol, vista previa, unirse al proyecto e invitación por correo.

### Modified Capabilities
<!-- Ninguna: el backend y sus contratos REST/STOMP no cambian. -->

## Impact

- `umlpilot_mobile/lib/**` (reescritura por capas), `pubspec.yaml`, `.env` / `.env.sample`, `android/app/src/main/AndroidManifest.xml`.
- Consume los endpoints existentes de `umlpilot_api`: auth, projects, collaborators, notifications, users, diagrams, elements (solo lectura), relationships (solo lectura), compartir; y el canal STOMP `/ws`.
- Dependencias: `dio`, `go_router`, `provider`, `get_it`, `flutter_secure_storage`, `stomp_dart_client`, `share_plus`, `intl`.
- Sin cambios de backend previstos.
- Fuera de alcance: edición del diagrama (elementos, relaciones, propiedades, IA, importación/exportación XMI, generación de backend). Esas funciones siguen disponibles en la web.
