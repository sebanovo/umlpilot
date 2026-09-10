## Context

El backend Spring Boot tiene User entity, SecurityConfig con `permitAll`, y configuración JWT en `application.yml` que nadie consume. El frontend es un scaffold Vite+React sin rutas ni estado. No hay librería JWT en `pom.xml`. La arquitectura definida en AGENTS.md es Hexagonal pero el código actual usa un layout plano (`model/`, `service/`, `controller/`). Esta implementación seguirá el layout plano existente para no bloquiar la migración hexagonal futura.

## Goals / Non-Goals

**Goals:**
- Login y registro funcionales con JWT
- Filtro de seguridad que valide tokens en endpoints protegidos
- Frontend con páginas de login/registro y manejo de sesión
- Tokens access (15min) y refresh (7 días) con renovación automática

**Non-Goals:**
- Migrar a arquitectura hexagonal (se hará en un cambio separado)
- OAuth2 / social login
- Recuperación de contraseña por email
- Rotación de refresh tokens
- Blacklist de tokens revocados

## Decisions

### 1. Librería JWT: `io.jsonwebtoken:jjwt` (0.12.6)
**Por qué**: La librería más usada en ecosistemas Spring, API limpia, soporte completo para JWS/JWE, buena documentación.
**Alternativa descartada**: `nimbus-jose-jwt` — más verbosa, orientada a JOSE completo (JWE/JWS/JWT), sobreingeniería para este caso.

### 2. Filtro: `JwtAuthenticationFilter` extiende `OncePerRequestFilter`
**Por qué**: Ejecuta una vez por request, lee el header `Authorization`, valida el token y establece el `SecurityContext`. Se registra antes del filtro de autorización.
**Alternativa descartada**: `@PreAuthorize` en cada endpoint — no filtra requests sin token, requiere anotación manual en cada controlador.

### 3. Almacenamiento de tokens en frontend: `localStorage`
**Por qué**: Persiste entre refreshes de página, simple de implementar, suficiente para una app interna.
**Alternativa descartada**: `httpOnly cookies` — protege contra XSS pero complica el setup con CORS y no es estándar en SPAs con API REST.

### 4. DTOs de auth: registros Java separados (no hereda de entity)
**Por qué**: Separa el contrato de API del modelo de persistencia. LoginRequest no necesita campos de User como `createdAt` o `role`.
**Archivos**: `LoginRequest`, `RegisterRequest`, `AuthResponse`.

### 5. Endpoints bajo prefijo `/api/v1/auth/`
**Por qué**: Consistente con la convención REST del proyecto, separa auth de otros endpoints, facilita reglas de CORS y proxy.

## Risks / Trade-offs

- **[JWT stateless → no revocación]** → Mitigación: refresh tokens de corta duración. Un token comprometido expira en 15min. La blacklist se puede añadir después si es necesario.
- **[localStorage vulnerable a XSS]** → Mitigación: CSP headers en producción, sanitización de inputs. Para una app interna es aceptable.
- **[Secret hardcoded como fallback]** → Mitigación: el `.env` siempre define `JWT_SECRET`. El fallback es solo para que la app no crashee si falta la variable.
- **[Layout plano viola AGENTS.md]** → Mitigación: la migración hexagonal es un cambio futuro. Ahora priorizar funcionalidad.
