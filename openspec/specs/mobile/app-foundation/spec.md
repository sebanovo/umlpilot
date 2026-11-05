## Purpose

Define el arranque de la app móvil, su configuración por entorno, la inyección de dependencias, el ruteo, el tema basado en el design system, el cliente HTTP y el manejo uniforme de sesión y errores.

## Requirements

### Requirement: Configuración por archivo de entorno
La app SHALL leer `API_BASE_URL` y `APP_NAME` desde el archivo `.env` empaquetado como asset. La app MUST NOT requerir ni usar `OLLAMA_URL` ni `OLLAMA_MODEL`.

#### Scenario: Arranque con configuración válida
- **WHEN** la app inicia y `.env` define `API_BASE_URL`
- **THEN** todas las llamadas al backend usan esa URL base y el título muestra `APP_NAME`

#### Scenario: Falta la URL del backend
- **WHEN** `.env` no define `API_BASE_URL`
- **THEN** la app muestra un error de configuración claro y no intenta llamadas de red

### Requirement: Cliente HTTP autenticado
La app SHALL enviar el access token como `Bearer` en las peticiones autenticadas, serializar JSON, aplicar timeouts y convertir errores del backend (`{"error": "..."}`) en mensajes legibles.

#### Scenario: Token expirado
- **WHEN** una petición autenticada responde 401
- **THEN** la app refresca el token una sola vez y reintenta la petición original

#### Scenario: Refresh fallido
- **WHEN** el refresh responde con error
- **THEN** la sesión se cierra y se redirige al login conservando la ruta de retorno

#### Scenario: Error del backend
- **WHEN** el backend responde 4xx/5xx con un campo `error`
- **THEN** la UI muestra ese mensaje como error de la operación

### Requirement: Sesión persistente
La app SHALL almacenar los tokens y el usuario de forma segura en el dispositivo y restaurar la sesión al volver a abrir la aplicación.

#### Scenario: Restauración de sesión
- **WHEN** el usuario abre la app con tokens válidos guardados
- **THEN** entra directamente al listado de proyectos sin volver a autenticarse

#### Scenario: Cierre de sesión
- **WHEN** el usuario cierra sesión
- **THEN** se borran los tokens almacenados y se navega al login

### Requirement: Ruteo y rutas protegidas
La app SHALL exponer rutas públicas (login, registro y vista previa de enlace compartido) y rutas protegidas (proyectos, detalle de proyecto y editor). Las rutas protegidas MUST exigir sesión.

#### Scenario: Acceso sin sesión
- **WHEN** un usuario sin sesión intenta abrir una ruta protegida
- **THEN** se le redirige al login y, tras autenticarse, vuelve a la ruta solicitada

#### Scenario: Enlace compartido público
- **WHEN** un usuario sin sesión abre `/share/{token}`
- **THEN** puede ver la vista previa del diagrama sin autenticarse

### Requirement: Tema basado en el design system
La app SHALL aplicar los tokens de color, tipografía, espaciado, radios y sombras del design system mediante un tema centralizado, con contraste accesible y áreas táctiles mínimas de 44x44 px.

#### Scenario: Consistencia visual
- **WHEN** se navega entre pantallas
- **THEN** colores, tipografía, espaciados y componentes mantienen los mismos tokens

#### Scenario: Accesibilidad táctil
- **WHEN** el usuario interactúa con botones e iconos
- **THEN** cada objetivo táctil cumple el tamaño mínimo y expone una etiqueta semántica

### Requirement: Estados de interfaz uniformes
Cada pantalla que carga datos SHALL mostrar estados de carga, vacío y error con opción de reintentar, sin dejar la UI en blanco ni silenciar fallos.

#### Scenario: Carga fallida
- **WHEN** una pantalla no puede cargar sus datos
- **THEN** muestra el mensaje de error y una acción de reintento

#### Scenario: Lista vacía
- **WHEN** una lista no tiene elementos
- **THEN** muestra un estado vacío con la acción principal disponible

### Requirement: Manejo de conectividad
La app SHALL informar cuando no hay conexión o el servidor no responde, y permitir reintentar la operación.

#### Scenario: Sin red
- **WHEN** una operación de red falla por conectividad
- **THEN** se muestra un mensaje de conexión y no se pierde el estado de la pantalla
