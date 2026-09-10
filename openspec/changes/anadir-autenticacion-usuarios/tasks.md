## 1. Backend - Dependencias y configuración JWT

- [x] 1.1 Agregar dependencia `io.jsonwebtoken:jjwt-api:0.12.6`, `jjwt-impl:0.12.6` y `jjwt-jackson:0.12.6` (scope runtime) al `pom.xml` y verificar que `mvnw compile` compila sin errores
- [x] 1.2 Crear clase `JwtService` en `service/` que genere access tokens, refresh tokens, valide tokens y extraiga claims. Verificar con un test unitario que genera y valida un token correctamente

## 2. Backend - DTOs y servicio de autenticación

- [x] 2.1 Crear DTOs: `LoginRequest` (email, password), `RegisterRequest` (email, password, firstName, lastName), `AuthResponse` (accessToken, refreshToken, tokenType, user). Verificar que compilan
- [x] 2.2 Crear servicio `AuthService` con métodos `login()`, `register()` y `refreshToken()`. Verificar que `mvnw compile` compila sin errores

## 3. Backend - Filtro y configuración de seguridad

- [x] 3.1 Crear `JwtAuthenticationFilter` que extienda `OncePerRequestFilter`, lea el header `Authorization`, valide el token y establezca el `SecurityContext`. Verificar que compila
- [x] 3.2 Crear `AuthenticationController` con endpoints `POST /api/v1/auth/login`, `POST /api/v1/auth/register` y `POST /api/v1/auth/refresh`. Verificar con curl que login retorna tokens
- [x] 3.3 Actualizar `SecurityConfig` para registrar el filtro JWT, proteger rutasexcepto `/api/v1/auth/**`, `/health`, `/actuator/**` y swagger. Verificar que un request sin token a un endpoint protegido retorna 401

## 4. Frontend - Estructura y dependencias

- [x] 4.1 Instalar `react-router-dom` en `umlpilot_web` y configurar rutas básicas en `App.tsx` con `/login`, `/register` y `/`. Verificar que `npm run dev` inicia sin errores
- [x] 4.2 Crear páginas: `LoginPage.tsx`, `RegisterPage.tsx` y `HomePage.tsx` con formularios básicos. Verificar que las páginas renderizan correctamente

## 5. Frontend - Estado de autenticación

- [x] 5.1 Crear `AuthContext` con provider que maneje token, login, register, logout y persistencia en localStorage. Verificar que compila con `npm run build`
- [x] 5.2 Crear interceptor HTTP que adjunte `Authorization: Bearer <token>` en peticiones a `/api` y maneje 401 con refresh automático. Verificar que una petición autenticada llega al backend con el header

## 6. Frontend - Integración

- [x] 6.1 Conectar `LoginPage` y `RegisterPage` con `AuthContext`, implementar redirección post-login y protección de rutas. Verificar login completo: credenciales → token → redirección a home
- [x] 6.2 Agregar botón de "Cerrar sesión" en `HomePage` que limpie tokens y redirija a `/login`. Verificar que cerrar sesión invalida la sesión
