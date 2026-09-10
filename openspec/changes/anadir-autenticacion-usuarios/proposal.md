## Why

La aplicación necesita autenticación de usuarios para proteger endpoints, identificar quién realiza cada acción y habilitar roles (SUPERADMIN, ADMIN, USER). Actualmente todos los endpoints están abiertos (`permitAll`), no hay login, registro ni manejo de sesiones. Sin autenticación no se puede avanzar con funcionalidades colaborativas ni con control de acceso.

## What Changes

- **Backend**: Se añade librería JWT (`jjwt`), servicio de generación/validación de tokens, filtro de autenticación, endpoints de login, registro y refresh token, y se reconfigura SecurityFilterChain para proteger rutas.
- **Frontend**: Se añaden páginas de login y registro, proveedor de autenticación, interceptor HTTP para adjuntar token, y lógica de rutas protegidas.
- **BREAKING**: Los endpoints actualmente públicos pasarán a requerir token JWT excepto login, registro, health y actuator.

## Capabilities

### New Capabilities

- `auth/jwt`: Generación, validación y refresh de tokens JWT. Incluye el servicio de token, el filtro de seguridad y la configuración de expiración.
- `auth/login`: Endpoint `POST /api/v1/auth/login` que valida credenciales y retorna access/refresh tokens.
- `auth/register`: Endpoint `POST /api/v1/auth/register` que crea un usuario nuevo con rol USER.
- `frontend/auth`: Páginas de login y registro, proveedor de contexto de autenticación, interceptor HTTP y almacenamiento de tokens.

### Modified Capabilities

<!-- Ninguna capability existente cambia requisitos -->

## Impact

- **Backend**: `pom.xml` (nueva dependencia `jjwt`), `SecurityConfig.java` (filtro JWT, rutas protegidas), nuevos archivos en `service/`, `controller/`, `model/` (DTOs).
- **Frontend**: `package.json` (nueva dependencia si se usa librería HTTP), nuevos archivos en `src/` (pages, features, shared).
- **Base de datos**: Sin cambios — la tabla `users` ya existe con los campos necesarios.
- **API**: Nuevos endpoints `/api/v1/auth/*`. Los endpoints existentes cambian de públicos a protegidos.
