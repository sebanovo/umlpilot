## Why

UmlPilot ya permite modelar diagramas de clases UML colaborativos, pero el modelo no produce artefactos ejecutables: el usuario debe traducir a mano su diagrama a código. Generar un backend Spring Boot compilable a partir del diagrama cierra el ciclo del modelado y convierte el editor en una herramienta de productividad, reutilizando el modelo UML ya persistido.

## What Changes

- Nuevo endpoint que, dado un `diagramId`, devuelve como **stream** un `.zip` con un proyecto Spring Boot completo (Maven) listo para compilar y ejecutar.
- El proyecto generado sigue el patrón **clásico de 4 capas + DTOs** (Model → Repository → Service → Controller + DTO), **independiente de la arquitectura hexagonal de UmlPilot**.
- Mapeo UML → Java: entidades JPA (`@Entity`/`@Table`), relaciones JPA (`@OneToMany`/`@ManyToOne`/`@OneToOne`/`@ManyToMany`), tipos UML → tipos Java, métodos como métodos de clase (sin endpoint), CRUD REST por entidad.
- El `.zip` incluye `pom.xml`, `application.yml` (H2 en archivo con ruta de datos configurable, por defecto `./data`), `application-postgres.yml` (perfil secundario con PostgreSQL), `README.md`, `.gitignore`, `OpenApiConfig` (Swagger UI en `/swagger-ui.html`) y health check vía Actuator.
- **Validación previa**: solo se admiten relaciones `association` y `association_class`; herencia/generalización, realización, dependencia, `template_binding`, agregación y composición se **rechazan con HTTP 400** listando las relaciones no soportadas.
- Tratamiento de elementos: `class`, `interface`, `enumeration` y `table` (u otro tipo desconocido) se generan como entidad JPA; las `note` se ignoran.
- Generación **en memoria** (sin archivos temporales) con **plantillas StringTemplate (ST4)**, sanitización de identificadores Java y tope máximo de clases.
- Nueva acción en el editor ("Generar backend") junto a "Exportar XMI" que solicita el `.zip` y dispara la descarga en el navegador.

## Capabilities

### New Capabilities
- `code-generation/springboot`: Endpoint que valida el diagrama y genera en memoria un proyecto Spring Boot (4 capas + DTOs) empaquetado como `.zip` descargable.
- `frontend/code-generation`: Acción en el editor de diagramas que solicita la generación y descarga el `.zip` del backend Spring Boot.

### Modified Capabilities
<!-- Ninguna: la capacidad es nueva y no cambia requisitos de specs existentes. -->

## Impact

- **Backend (`umlpilot_api`)**:
  - UmlPilot mantiene **PostgreSQL** como base de datos por defecto y añade **H2** (archivo, ruta configurable `H2_DATA_PATH`, por defecto `./data`) en el perfil secundario `h2` (`application-h2.yml`), para poder arrancar también sin base de datos externa.
  - Nuevo adaptador de entrada: `CodeGenerationController` (`GET /api/v1/projects/{projectId}/diagrams/{diagramId}/generate/backend`).
  - Nuevo caso de uso en `application`: servicio de generación + modelo de entrada (instantánea del diagrama) + validador de relaciones.
  - Nueva infraestructura de plantillas: recursos ST4 en `src/main/resources/templates/springboot/**`.
  - Reutiliza `ElementService` y `RelationshipService` para leer el diagrama (elementos, atributos, métodos, parámetros, relaciones y multiplicidades).
  - Dependencia nueva: `org.antlr:ST4` en `pom.xml`. Sin cambios de base de datos.
- **Frontend (`umlpilot_web`)**:
  - `shared/api/client.ts`: helper de descarga autenticada (`authDownload`) y llamada al endpoint de generación.
  - `pages/DiagramEditorPage.tsx` (y/o `Toolbar`): botón "Generar backend" con estado de carga y manejo de errores.
- **Contratos**: nuevo endpoint documentado; respuestas `200 application/zip`, `400` (diagrama inválido/vacío/límite excedido), `404` (diagrama inexistente) y `401` sin token válido.
- **No afecta** la arquitectura hexagonal de UmlPilot ni los endpoints existentes.
