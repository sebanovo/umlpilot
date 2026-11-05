## 1. Preparación del proyecto

- [x] 1.1 Actualizar `pubspec.yaml` con `dio`, `go_router`, `flutter_secure_storage`, `stomp_dart_client`, `share_plus`, `intl` y `mocktail`; verificado con `flutter pub get`
- [x] 1.2 Actualizar `.env` y `.env.sample` con `API_BASE_URL`, `APP_NAME` y `WEB_BASE_URL`; eliminadas `OLLAMA_URL` y `OLLAMA_MODEL`
- [x] 1.3 Configurar Android (`INTERNET` y cleartext para desarrollo)
- [x] 1.4 Eliminar `lib/features/chat/**` y las referencias a Ollama en código, README y dependencias
- [x] 1.5 Crear la estructura por capas (`core`, `features`, `injection`, `routes`, `theme`)

## 2. Núcleo y design system

- [x] 2.1 Implementar `ApiConstants`/`AppConstants` leyendo `.env`, con test de valores y derivación `ws/wss`
- [x] 2.2 Implementar `exceptions.dart` y `failures.dart`
- [x] 2.3 Implementar `ApiClient` (Bearer, refresh single-flight, mapeo de errores)
- [x] 2.4 Implementar almacenamiento seguro de sesión y `SessionController`, con tests
- [x] 2.5 Implementar tokens de tema (`AppColors`, `AppTypography`, `AppSpacing`, `AppRadius`, `AppShadows`) y `ThemeData`
- [x] 2.6 Crear widgets compartidos (botón, input, card, loading, empty, error, confirm, snackbar), con tests de estados
- [x] 2.7 Registrar dependencias en `get_it`
- [x] 2.8 Configurar `go_router` con rutas públicas/protegidas y `redirect`
- [x] 2.9 Implementar splash y pantallas de navegación autenticada

## 3. Autenticación

- [x] 3.1 Implementar DTOs, data source y repositorio de auth (`login`, `register`, `refresh`)
- [x] 3.2 Implementar use cases de login/registro/logout/sesión
- [x] 3.3 Implementar `AuthViewModel`, con tests de éxito, error y restauración
- [x] 3.4 Implementar la pantalla de login con validación local
- [x] 3.5 Implementar la pantalla de registro con validación
- [x] 3.6 Implementar restauración de sesión y logout
- [x] 3.7 Integrar el guard de rutas y la ruta de retorno tras login

## 4. Proyectos

- [x] 4.1 Implementar modelo/DTO, data source, repositorio y use cases de proyectos
- [x] 4.2 Implementar `ProjectsViewModel`, con tests de carga, error y creación
- [x] 4.3 Implementar la pantalla de proyectos y el formulario de creación
- [x] 4.4 Implementar el detalle de proyecto con diagramas, colaboradores y rol actual
- [x] 4.5 Implementar editar y eliminar proyecto respetando permisos

## 5. Colaboradores y notificaciones

- [x] 5.1 Implementar datos/dominio de colaboradores (listar, invitar, cambiar rol, eliminar)
- [x] 5.2 Implementar búsqueda de usuarios con debounce e invitación por email
- [x] 5.3 Implementar la lista de colaboradores con estado pendiente, cambio de rol y eliminación
- [x] 5.4 Implementar datos/dominio de notificaciones (lista, contador, marcar, responder)
- [x] 5.5 Implementar la bandeja con badge, polling y acciones aceptar/rechazar, con tests

## 6. Diagramas

- [x] 6.1 Implementar modelo/DTO, repositorio y use cases de diagramas
- [x] 6.2 Implementar la lista, creación y eliminación de diagramas
- [x] 6.3 Implementar bloquear/desbloquear y el aviso de bloqueo

## 7. Visor de diagramas (solo lectura)

- [x] 7.1 Implementar modelos y repositorios de UML (elementos, atributos, métodos, parámetros, literales, relaciones y multiplicidades)
- [x] 7.2 Implementar `DiagramViewerViewModel` (carga, permisos y refresco)
- [x] 7.3 Implementar el canvas con límites dinámicos, zoom, pan y ajuste a contenido
- [x] 7.4 Renderizar clase/interfaz/enumeración/nota con atributos, métodos y literales
- [x] 7.5 Pintar todos los tipos de relación con marcadores y multiplicidades; conectar la clase de asociación y dibujar relaciones recursivas
- [x] 7.6 Implementar selección de nodos y aristas con hoja de detalles de solo lectura
- [x] 7.7 Conectar STOMP con autenticación, presencia y cierre seguro del cliente (sin eventos tras `close`)
- [x] 7.8 Refrescar el canvas ante cambios remotos
- [x] 7.9 Mantener el visor siempre en solo lectura (sin herramientas ni acciones de mutación)

## 8. Compartir

- [x] 8.1 Implementar datos/dominio de compartir (estado, activar/desactivar, rol, vista pública, unirse)
- [x] 8.2 Implementar la hoja de compartir con rol, copiar y compartir nativo
- [x] 8.3 Implementar la vista previa pública `/share/:token`
- [x] 8.4 Implementar la unión al proyecto y la navegación al visor

## 9. Verificación

- [x] 9.1 Ejecutar `flutter analyze` y dejar la salida sin issues
- [x] 9.2 Ejecutar `flutter test` (16 pruebas en verde para núcleo, auth, proyectos, notificaciones y widgets)
- [x] 9.3 Actualizar `umlpilot_mobile/README.md` con configuración, ejecución y alcance
- [ ] 9.4 Prueba de integración automática login → proyecto → diagrama → compartir (pendiente)
- [ ] 9.5 Prueba manual en web/Android del renderizado UML (a cargo del usuario)
- [ ] 9.6 Prueba de adapter HTTP simulada para `ApiClient` (pendiente)
