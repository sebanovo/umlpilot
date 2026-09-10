## Purpose

Permite a los usuarios autenticarse con email y password para obtener tokens de acceso.

## ADDED Requirements

### Requirement: Login con credenciales
El sistema SHALL exponer un endpoint `POST /api/v1/auth/login` que acepte email y password, valide las credenciales contra la base de datos y retorne tokens JWT.

#### Scenario: Login exitoso
- **WHEN** se envía un POST a `/api/v1/auth/login` con email y password correctos
- **THEN** el sistema retorna HTTP 200 con un JSON que contiene `accessToken`, `refreshToken`, `tokenType` ("Bearer") y los datos del usuario (id, email, nombre, rol)

#### Scenario: Email no registrado
- **WHEN** se envía un POST a `/api/v1/auth/login` con un email que no existe
- **THEN** el sistema retorna HTTP 401 con un mensaje de error

#### Scenario: Password incorrecto
- **WHEN** se envía un POST a `/api/v1/auth/login` con un email válido pero password incorrecto
- **THEN** el sistema retorna HTTP 401 con un mensaje de error

#### Scenario: Campos vacíos
- **WHEN** se envía un POST a `/api/v1/auth/login` sin email o sin password
- **THEN** el sistema retorna HTTP 400 con errores de validación

### Requirement: Respuesta de login
La respuesta del login SHALL incluir: `accessToken` (string JWT), `refreshToken` (string JWT), `tokenType` ("Bearer"), y un objeto `user` con `id`, `email`, `firstName`, `lastName`, `role`.

#### Scenario: Estructura de respuesta
- **WHEN** el login es exitoso
- **THEN** la respuesta contiene todos los campos requeridos con los tipos correctos
