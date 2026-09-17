# Backend Spring Boot generado por UmlPilot

Proyecto Maven generado automáticamente a partir de un diagrama de clases UML.
Sigue el patrón clásico de 4 capas (Model → Repository → Service → Controller) más DTOs.

## Requisitos

- Java 21
- Maven 3.9+

## Base de datos

Por defecto usa **H2 en archivo**, con el fichero en la ruta raíz `/data` (configurable con `H2_DATA_PATH`):

| Variable        | Valor por defecto |
|-----------------|-------------------|
| `H2_DATA_PATH`  | `/data`           |
| `H2_USER`       | `sa`              |
| `H2_PASSWORD`   | (vacío)           |
| `SERVER_PORT`   | `8080`            |

Para usar **PostgreSQL** activa el perfil `postgres` (`src/main/resources/application-postgres.yml`):

```bash
SPRING_PROFILES_ACTIVE=postgres mvn spring-boot:run
```

| Variable      | Valor por defecto |
|---------------|-------------------|
| `DB_HOST`     | `localhost`       |
| `DB_PORT`     | `5432`            |
| `DB_NAME`     | `generated_db`    |
| `DB_USER`     | `postgres`        |
| `DB_PASSWORD` | `postgres`        |

## Ejecución

```bash
mvn spring-boot:run
```

## Documentación y salud

- Swagger UI: http://localhost:8080/swagger-ui.html
- Health check: http://localhost:8080/actuator/health

## Entidades generadas

$model.entityList; separator="\n"$
## Notas

- Las entidades JPA nunca se exponen en la API: cada operación usa DTOs de petición y respuesta.
- Los nombres de tabla se derivan del nombre de la clase en `snake_case` plural (heurística).
