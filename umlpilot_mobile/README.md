# umlpilot_mobile

Cliente Flutter de UMLPilot para **visualizar proyectos y diagramas UML en tiempo real**. La app reutiliza el mismo backend que la web (`umlpilot_api`) mediante REST y STOMP.

## Alcance

- Login, registro, sesión persistente y refresh de JWT.
- Proyectos: listar, crear, detalle, editar y eliminar.
- Colaboradores: invitar por email, cambiar rol (owner) y eliminar.
- Notificaciones: bandeja con no leídas y aceptar/rechazar invitaciones.
- Compartir: enlace público con rol, vista previa, unirse e invitar por email.
- Diagramas: listar, crear, eliminar y **abrir en un visor de solo lectura** con zoom/pan y actualización en tiempo real.

**Fuera de alcance móvil:** edición del diagrama, asistente IA, importación/exportación XMI y generación de backend. Esas funciones permanecen en la web.

## Requisitos

- Flutter 3.41+ / Dart 3.11+.
- Backend UMLPilot corriendo (`umlpilot_api`).

## Configuración

Crea tu `.env` a partir de `.env.sample`:

```bash
cp .env.sample .env
```

Variables:

| Variable | Descripción |
|----------|-------------|
| `API_BASE_URL` | URL del backend. Web/escritorio: `http://localhost:8080`; emulador Android: `http://10.0.2.2:8080`. |
| `APP_NAME` | Nombre mostrado en la app. |
| `WEB_BASE_URL` | Origen de la web, usado para construir los enlaces compartidos (`/share/{token}`). |

> No se usa Ollama. Si vienes de la versión anterior, elimina `OLLAMA_URL` y `OLLAMA_MODEL` de tu `.env`.

## Ejecutar

```bash
flutter pub get
flutter run -d chrome     # web
flutter run               # Android
```

Para Android con backend en el equipo host, usa `http://10.0.2.2:8080` en `API_BASE_URL`.

## Arquitectura

MVVM + capas Clean (ver `docs/architecture/mobile-mvvm.md`):

```
lib/
├── core/        # constantes, errores, red, sesión, tiempo real, widgets
├── features/    # auth, projects, collaborators, notifications, diagrams, diagram-editor, diagram-viewer, sharing
├── injection/   # get_it
├── routes/      # go_router
├── theme/       # design system (docs/design-system.md)
└── main.dart
```

Flujo: `View → ViewModel (ChangeNotifier) → UseCase → Repository → DataSource → API`.

## Calidad

```bash
flutter analyze
flutter test
```

La compilación/ejecución manual en web o Android queda a cargo del desarrollador.
