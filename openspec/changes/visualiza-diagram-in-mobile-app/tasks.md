## 1. Preparación del proyecto

- [ ] 1.1 Actualizar `pubspec.yaml` con `dio`, `go_router`, `flutter_secure_storage`, `stomp_dart_client`, `json_annotation`, `json_serializable`, `build_runner`, `file_picker`, `share_plus`, `path_provider`, `intl`, `record`, `mocktail` y verificar con `flutter pub get`
- [ ] 1.2 Actualizar `.env` y `.env.sample`: mantener `API_BASE_URL`/`APP_NAME` y eliminar `OLLAMA_URL`/`OLLAMA_MODEL`; verificar que `flutter analyze` no reporta referencias a variables eliminadas
- [ ] 1.3 Configurar Android (`INTERNET`, cleartext solo para desarrollo) y verificar `flutter build apk --debug`
- [ ] 1.4 Eliminar `lib/features/chat/**` y cualquier referencia a Ollama en código, README y dependencias; verificar con `rg -i ollama umlpilot_mobile` sin resultados
- [ ] 1.5 Crear la estructura de carpetas de `design.md` (`core`, `features`, `injection`, `routes`, `theme`) y verificar que compila con `flutter analyze`

## 2. Núcleo y design system

- [ ] 2.1 Implementar `ApiConstants`/`AppConstants` leyendo `API_BASE_URL` y `APP_NAME`; verificar con test de valores leídos y faltantes
- [ ] 2.2 Implementar `exceptions.dart` y `failures.dart`; verificar tests de mapeo servidor/red/sesión
- [ ] 2.3 Implementar `Dio` con interceptores de auth, refresh single-flight y traducción de errores; verificar tests con adapter simulado (401 refresca una vez, error `{error}`)
- [ ] 2.4 Implementar almacenamiento seguro de tokens y usuario; verificar test de guardar/restaurar/limpiar
- [ ] 2.5 Implementar tokens de tema (`AppColors`, `AppTypography`, `AppSpacing`, `AppRadius`, `AppShadows`) y `ThemeData`; verificar widget test de colores y tipografía
- [ ] 2.6 Crear widgets compartidos (botón, input, card, loading, empty, error, confirm, snackbar); verificar widget tests de estados y accesibilidad táctil
- [ ] 2.7 Registrar dependencias en `get_it`; verificar que `configureDependencies` resuelve repositorios y use cases
- [ ] 2.8 Configurar `go_router` con rutas públicas/protegidas y `redirect`; verificar widget test sin sesión → login, con sesión → proyectos, `/share/:token` pública
- [ ] 2.9 Implementar el shell de navegación autenticado; verificar widget test de cambio entre proyectos y salir

## 3. Autenticación

- [ ] 3.1 Implementar DTOs, data source y repositorio de auth (`login`, `register`, `refresh`); verificar test de serialización y mapeo
- [ ] 3.2 Implementar use cases de login/registro/logout/sesión; verificar tests unitarios
- [ ] 3.3 Implementar `AuthViewModel` y el estado de sesión; verificar tests de éxito, error y restauración
- [ ] 3.4 Implementar la pantalla de login con validación local; verificar widget test de campos y error
- [ ] 3.5 Implementar la pantalla de registro con validación; verificar widget test de alta correcta
- [ ] 3.6 Implementar splash de restauración y logout; verificar widget test de ambos caminos
- [ ] 3.7 Integrar el guard de rutas y la ruta de retorno tras login; verificar test de redirección

## 4. Proyectos

- [ ] 4.1 Implementar modelo/DTO, data source, repositorio y use cases de proyectos; verificar tests de mapeo y llamadas
- [ ] 4.2 Implementar `ProjectsViewModel` (lista y creación); verificar tests de carga, vacío, error y creación
- [ ] 4.3 Implementar la pantalla de proyectos y el formulario de creación; verificar widget tests
- [ ] 4.4 Implementar el detalle de proyecto (diagramas, colaboradores, rol actual); verificar widget test
- [ ] 4.5 Implementar editar y eliminar proyecto con permisos; verificar widget tests owner/editor/viewer

## 5. Colaboradores y notificaciones

- [ ] 5.1 Implementar datos/dominio de colaboradores (listar, invitar, cambiar rol, eliminar); verificar tests unitarios
- [ ] 5.2 Implementar búsqueda de usuarios con debounce e invitación por email; verificar widget test de sugerencias y error
- [ ] 5.3 Implementar la lista de colaboradores con estado pendiente, cambio de rol y eliminación con confirmación; verificar widget tests
- [ ] 5.4 Implementar datos/dominio de notificaciones (lista, contador, marcar, responder); verificar tests unitarios
- [ ] 5.5 Implementar la bandeja con badge, polling/enfoque y acciones aceptar/rechazar; verificar widget tests

## 6. Diagramas

- [ ] 6.1 Implementar modelo/DTO, repositorio y use cases de diagramas; verificar test de mapeo
- [ ] 6.2 Implementar la lista, creación y eliminación de diagramas; verificar widget tests
- [ ] 6.3 Implementar bloquear/desbloquear y el aviso de bloqueo; verificar tests de servicio y widget

## 7. Editor UML

- [ ] 7.1 Implementar modelos y repositorios de elementos, atributos, métodos, parámetros, literales, relaciones y multiplicidades; verificar tests de mapeo
- [ ] 7.2 Implementar el `DiagramEditorViewModel` (carga, permisos, refresco); verificar tests de carga, error y viewer solo lectura
- [ ] 7.3 Implementar el viewport del canvas (zoom, pan, fit, cuadrícula); verificar widget test de gestos
- [ ] 7.4 Renderizar clase/interfaz/enumeración/nota con atributos y métodos; verificar widget test de notación
- [ ] 7.5 Pintar relaciones por tipo con flechas y multiplicidades; verificar widget/golden test
- [ ] 7.6 Implementar selección, arrastre y snap con persistencia debounced; verificar test de posición guardada
- [ ] 7.7 Implementar caja de herramientas y creación de elementos; verificar widget test de alta
- [ ] 7.8 Implementar creación de relaciones con taps origen→destino y tipo; verificar widget test
- [ ] 7.9 Implementar hoja de propiedades (nombre, visibilidad, atributos, métodos, parámetros, literales, eliminar); verificar widget tests
- [ ] 7.10 Implementar hoja de relación (nombre, multiplicidades, eliminar); verificar widget tests
- [ ] 7.11 Implementar "Eliminar todo" con confirmación; verificar test de servicio y widget
- [ ] 7.12 Modo solo lectura para viewer (sin toolbox ni acciones); verificar widget test

## 8. Tiempo real

- [ ] 8.1 Implementar el cliente STOMP (conexión con token, join/leave, reconexión); verificar test con cliente falso
- [ ] 8.2 Mostrar presencia y refrescar el canvas ante cambios remotos; verificar widget/unit tests
- [ ] 8.3 Publicar cambios locales y cursores (si aplica) sin bloquear la UI; verificar test de publicaciones

## 9. Compartir

- [ ] 9.1 Implementar datos/dominio de compartir (estado, activar/desactivar, rol, vista pública, unirse); verificar tests unitarios
- [ ] 9.2 Implementar el diálogo de compartir con rol, copiar y compartir nativo; verificar widget tests
- [ ] 9.3 Implementar la vista previa pública `/share/:token` y el CTA de unirse/login; verificar widget tests
- [ ] 9.4 Implementar la unión al proyecto y la navegación/rol resultante; verificar test de integración de unión

## 10. Asistente IA

- [ ] 10.1 Implementar datos/dominio del asistente (chat con historial, transcribir, desde texto/imagen); verificar tests de payloads
- [ ] 10.2 Implementar la UI del asistente (mensajes, carga, errores, limpiar); verificar widget tests
- [ ] 10.3 Refrescar el canvas cuando la respuesta incluye acciones; verificar test de acciones
- [ ] 10.4 Implementar construcción desde texto e imagen (archivo/cámara); verificar widget tests
- [ ] 10.5 Implementar dictado por voz con `record` y manejo de permisos; verificar test de flujo y denegación

## 11. XMI y generación

- [ ] 11.1 Implementar importación XMI (selector, límite 10 MB, advertencias, refresco); verificar test de resultado con advertencias
- [ ] 11.2 Portar la exportación XMI del cliente y compartir/guardar el archivo; verificar test del XML generado
- [ ] 11.3 Implementar descarga del backend ZIP y compartirlo; verificar test de descarga simulada

## 12. Verificación final

- [ ] 12.1 Ejecutar `flutter analyze` y corregir todos los avisos
- [ ] 12.2 Ejecutar `flutter test` y dejar la suite en verde
- [ ] 12.3 Ejecutar un test de integración login → proyecto → diagrama → compartir
- [ ] 12.4 Compilar `flutter build apk --debug` y verificar el arranque en emulador
- [ ] 12.5 Actualizar `umlpilot_mobile/README.md` con configuración `.env`, ejecución y pruebas; verificar los pasos documentados
