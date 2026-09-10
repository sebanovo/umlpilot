## Purpose

Proporciona la interfaz de usuario para autenticación (login y registro) y gestiona el estado de sesión del usuario en el frontend.

## ADDED Requirements

### Requirement: Página de login
El frontend SHALL exponer una página de login en `/login` con campos de email y password, y un botón para enviar.

#### Scenario: Login exitoso
- **WHEN** el usuario ingresa credenciales válidas y hace clic en "Iniciar sesión"
- **THEN** el sistema guarda el token JWT, redirige al usuario a `/` y muestra su nombre

#### Scenario: Login fallido
- **WHEN** el usuario ingresa credenciales inválidas
- **THEN** se muestra un mensaje de error debajo del formulario

### Requirement: Página de registro
El frontend SHALL exponer una página de registro en `/register` con campos de email, password, firstName y lastName.

#### Scenario: Registro exitoso
- **WHEN** el usuario completa el formulario con datos válidos y hace clic en "Registrarse"
- **THEN** el sistema crea la cuenta, guarda el token JWT y redirige a `/`

#### Scenario: Email ya registrado
- **WHEN** el usuario intenta registrarse con un email existente
- **THEN** se muestra un mensaje de error indicando que el email ya está en uso

### Requirement: Almacenamiento de tokens
El frontend SHALL almacenar el access token y refresh token en localStorage. El refresh token SHALL usarse para obtener un nuevo access token cuando este expire.

#### Scenario: Token almacenado
- **WHEN** el usuario se autentica exitosamente
- **THEN** ambos tokens se guardan en localStorage

#### Scenario: Sesión persistente
- **WHEN** el usuario recarga la página y existe un token válido en localStorage
- **THEN** el sistema mantiene al usuario autenticado

### Requirement: Interceptor HTTP
El frontend SHALL adjuntar el header `Authorization: Bearer <token>` en todas las peticiones HTTP a la API excepto login y register.

#### Scenario: Petición autenticada
- **WHEN** el frontend realiza una petición a un endpoint protegido
- **THEN** el header `Authorization` se adjunta automáticamente con el access token

#### Scenario: Token expirado
- **WHEN** la API retorna HTTP 401
- **THEN** el frontend intenta renovar el token con el refresh token; si falla, cierra sesión y redirige a `/login`

### Requirement: Cierre de sesión
El frontend SHALL proporcionar un botón o menú para cerrar sesión que limpie los tokens y redirija a `/login`.

#### Scenario: Logout
- **WHEN** el usuario hace clic en "Cerrar sesión"
- **THEN** se eliminan los tokens de localStorage y se redirige a `/login`

### Requirement: Rutas protegidas
El frontend SHALL proteger rutas que requieren autenticación. Si el usuario no está autenticado y intenta acceder a una ruta protegida, SHALL redirigir a `/login`.

#### Scenario: Acceso no autenticado
- **WHEN** un usuario no autenticado accede a `/` u otra ruta protegida
- **THEN** se redirige a `/login`
