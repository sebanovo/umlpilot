## Purpose

Genera, valida y refresca tokens JWT para mantener sesiones de usuario autenticadas de forma stateless.

## ADDED Requirements

### Requirement: Generación de access token
El sistema SHALL generar un access token JWT al autenticar un usuario correctamente. El token SHALL contener el email, el rol y la fecha de expiración. El token SHALL estar firmado con el secret configurado en `app.security.jwt.secret`.

#### Scenario: Token generado exitosamente
- **WHEN** se procesan credenciales válidas
- **THEN** el sistema retorna un access token JWT con expiración de 15 minutos (configurable)

### Requirement: Generación de refresh token
El sistema SHALL generar un refresh token JWT junto con el access token. El refresh token SHALL tener una expiración mayor al access token (configurable, default 7 días).

#### Scenario: Refresh token incluido en respuesta
- **WHEN** se autentica un usuario exitosamente
- **THEN** la respuesta incluye tanto access token como refresh token

### Requirement: Validación de token
El sistema SHALL validar que un token JWT recibido esté firmado correctamente, no haya expirado y contenga claims válidos.

#### Scenario: Token válido
- **WHEN** se envía un request con un token JWT válido en el header `Authorization: Bearer <token>`
- **THEN** el sistema permite el acceso al recurso

#### Scenario: Token expirado
- **WHEN** se envía un token JWT cuya fecha de expiración ya pasó
- **THEN** el sistema rechaza el request con código HTTP 401

#### Scenario: Token con firma inválida
- **WHEN** se envía un token JWT con firma incorrecta
- **THEN** el sistema rechaza el request con código HTTP 401

### Requirement: Refresh de access token
El sistema SHALL permitir intercambiar un refresh token válido por un nuevo access token.

#### Scenario: Refresh exitoso
- **WHEN** se envía un refresh token válido
- **THEN** el sistema retorna un nuevo access token

#### Scenario: Refresh con token expirado
- **WHEN** se envía un refresh token expirado
- **THEN** el sistema rechaza el request con código HTTP 401
