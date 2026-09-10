## Purpose

Permite a nuevos usuarios crear una cuenta con rol USER predeterminado.

## ADDED Requirements

### Requirement: Registro de usuario
El sistema SHALL exponer un endpoint `POST /api/v1/auth/register` que acepte email, password, firstName y lastName, cree un usuario con rol USER y retorne tokens JWT.

#### Scenario: Registro exitoso
- **WHEN** se envía un POST a `/api/v1/auth/register` con email, password, firstName y lastName válidos
- **THEN** el sistema crea el usuario con rol USER, retorna HTTP 201 con `accessToken`, `refreshToken`, `tokenType` y datos del usuario

#### Scenario: Email ya registrado
- **WHEN** se envía un POST a `/api/v1/auth/register` con un email que ya existe
- **THEN** el sistema retorna HTTP 409 con un mensaje indicando que el email ya está en uso

#### Scenario: Campos inválidos
- **WHEN** se envía un POST a `/api/v1/auth/register` con campos vacíos o formato de email inválido
- **THEN** el sistema retorna HTTP 400 con errores de validación por campo

#### Scenario: Password débil
- **WHEN** se envía un POST a `/api/v1/auth/register` con un password menor a 8 caracteres
- **THEN** el sistema retorna HTTP 400 indicando que el password no cumple requisitos mínimos

### Requirement: Password hasheado
El sistema SHALL guardar el password usando BCrypt (hash unidireccional). El password original NUNCA SHALL almacenarse en texto plano.

#### Scenario: Password almacenado
- **WHEN** se registra un usuario exitosamente
- **THEN** el password en la base de datos es un hash BCrypt, no el texto plano
