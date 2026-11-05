## Purpose

Permite que el usuario inicie sesión, se registre, mantenga su sesión con refresh de token y cierre sesión desde la app móvil contra el backend existente.

## Requirements

### Requirement: Inicio de sesión
La app SHALL autenticar al usuario con email y contraseña contra `POST /api/v1/auth/login` y guardar `accessToken`, `refreshToken` y `user`.

#### Scenario: Login correcto
- **WHEN** el usuario envía credenciales válidas
- **THEN** se guarda la sesión y se navega a proyectos (o a la ruta de retorno)

#### Scenario: Credenciales inválidas
- **WHEN** el backend rechaza las credenciales
- **THEN** se muestra el mensaje de error y no se crea sesión

### Requirement: Registro de usuario
La app SHALL registrar usuarios con email, contraseña, nombre y apellido contra `POST /api/v1/auth/register`, iniciando sesión automáticamente al completar.

#### Scenario: Registro correcto
- **WHEN** el usuario completa el formulario con datos válidos
- **THEN** se crea la cuenta, se guarda la sesión y se navega a proyectos

#### Scenario: Email ya registrado
- **WHEN** el backend informa que el email existe
- **THEN** se muestra ese mensaje sin crear sesión

### Requirement: Validación del formulario
La app SHALL validar localmente campos requeridos, formato de email y longitud mínima de contraseña antes de enviar.

#### Scenario: Datos incompletos
- **WHEN** el usuario intenta enviar el formulario con campos vacíos o email inválido
- **THEN** se muestran errores por campo y no se llama al backend

### Requirement: Refresh transparente de sesión
La app SHALL renovar el access token con el refresh token cuando expire, sin interrumpir la operación en curso.

#### Scenario: Access token expirado
- **WHEN** una petición falla por token expirado
- **THEN** se renueva la sesión y la operación continúa sin que el usuario vuelva a escribir credenciales

#### Scenario: Refresh inválido
- **WHEN** el refresh token también expiró o es inválido
- **THEN** se cierra la sesión y se muestra el login

### Requirement: Cierre de sesión
La app SHALL permitir cerrar sesión desde cualquier pantalla autenticada, limpiando los datos de sesión locales.

#### Scenario: Logout
- **WHEN** el usuario pulsa "Salir"
- **THEN** se borran los tokens y se navega al login

### Requirement: Restauración de sesión
La app SHALL restaurar la sesión guardada al iniciar y decidir la pantalla inicial según su estado.

#### Scenario: Splash con sesión
- **WHEN** la app inicia con tokens guardados
- **THEN** muestra la carga inicial y entra a proyectos

#### Scenario: Splash sin sesión
- **WHEN** la app inicia sin tokens
- **THEN** muestra el login
