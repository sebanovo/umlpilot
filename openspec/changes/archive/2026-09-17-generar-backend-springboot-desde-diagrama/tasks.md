## 1. Configuración y dependencias

- [x] 1.1 Añadir `org.antlr:ST4` (versión fijada) a `umlpilot_api/pom.xml` y verificar con `mvn -q -DskipTests compile` que resuelve y compila
- [x] 1.2 Añadir `app.code-generation.max-classes` (env `CODE_GENERATION_MAX_CLASSES`, default 100) y `app.code-generation.base-package` (default `com.generated`) en `application.yml` con un `@ConfigurationProperties`, y verificar el binding con un test que arranque el contexto de propiedades
- [x] 1.3 Crear el árbol de recursos `umlpilot_api/src/main/resources/templates/springboot/` (`project/`, `java/`, `resources/`) y verificar que las plantillas se copian al classpath (`mvn -q process-resources`)

## 2. Read-model del diagrama y validación

- [x] 2.1 Crear `application/dto/DiagramSnapshot` (records de elemento con atributos/métodos/parámetros y de relación con extremos y multiplicidades) y verificar con un test de construcción
- [x] 2.2 Implementar `DiagramSnapshotService` que componga la instantánea reutilizando `ElementService` y `RelationshipService`, y verificar con un test que un diagrama con 2 elementos, atributos, métodos y 1 relación se lee completo
- [x] 2.3 Implementar el validador de relaciones (solo `association`/`association_class`) que lance error 400 listando tipo y elementos de las relaciones no soportadas, y verificar tests para herencia, realización, dependencia, `template_binding`, agregación y composición
- [x] 2.4 Implementar el tratamiento de elementos (`class`/`interface`/`enumeration`/`table`/desconocido → entidad, `note` → ignorado, `association_class` → entidad) y las validaciones de diagrama sin entidades y de tope máximo, verificando cada caso con un test que espere 400 o el conteo correcto

## 3. Modelo de generación y mapeos

- [x] 3.1 Implementar utilidades de nombres (`sanitizeIdentifier`, PascalCase/camelCase, `snake_case` plural, palabras reservadas Java) y verificar con tests de casos inválidos (espacios, acentos, guiones, `class`, `y`/`-ies`)
- [x] 3.2 Implementar el mapeo de tipos UML → Java con `String` como fallback y verificar con tests que cubran los tipos soportados y uno desconocido
- [x] 3.3 Implementar la resolución de relaciones a JPA (`@ManyToOne` en el lado muchos, `@ManyToMany` ambos muchos, `@OneToOne` ambos uno, multiplicidad ausente = `1`, unidireccional) y verificar con tests para 1-N, N-M y 1-1
- [x] 3.4 Construir el `BackendModel` (entidades con tabla, `id` autogenerado `Long` cuando falta, campos, referencias, DTOs y rutas de endpoint) y verificar con un test que un diagrama de 3 clases produce 3 entidades, 3 repos, 3 servicios, 3 controllers y 6 DTOs, con `id` autogenerado en las que no lo declaran

## 4. Plantillas StringTemplate y renderer

- [x] 4.1 Crear la plantilla del `pom.xml` generado (Web, Data JPA, PostgreSQL, Lombok, SpringDoc, Validation) y verificar que el XML renderizado es válido y contiene las dependencias requeridas
- [x] 4.2 Crear las plantillas de `application.yml` (H2 en archivo con ruta de datos configurable `H2_DATA_PATH`, por defecto `./data`), `application-postgres.yml` (perfil secundario con PostgreSQL), `README.md`, `.gitignore`, `GeneratedApplication` y `OpenApiConfig` y verificar el contenido renderizado esperado
- [x] 4.3 Crear las plantillas de las 4 capas + DTOs (entidad JPA, repositorio, servicio CRUD, controller REST con `/api/v1/{recurso}`, `Request`/`Response`, `ResourceNotFoundException` y `GlobalExceptionHandler`) y verificar con tests de renderizado que no exponen la entidad JPA y que los códigos 201/200/204/404 están presentes
- [x] 4.4 Implementar `StringTemplateProjectRenderer` (implementa `BackendProjectRenderer`, carga plantillas del classpath y devuelve `Map<ruta, contenido>`) y verificar con un test que las rutas coinciden con la estructura definida
- [x] 4.5 Verificar la compilación del proyecto generado: generar un fixture de 3 clases con relaciones, escribir el `.zip` en un directorio temporal de test y compilarlo, comprobando que `mvn -q compile` (o el JDK Compiler API) finaliza sin errores

## 5. Caso de uso, streaming y endpoint

- [x] 5.1 Implementar `SpringBootGenerationService` (leer snapshot → validar → construir modelo → renderizar → empaquetar en `ZipOutputStream`) y verificar con un test que el zip contiene todas las entradas esperadas y que una validación fallida no produce zip
- [x] 5.2 Exponer `GET /api/v1/projects/{projectId}/diagrams/{diagramId}/generate/backend` devolviendo `StreamingResponseBody` con `Content-Type: application/zip` y `Content-Disposition`, y verificar con `MockMvc` los casos 200 (zip streameado), 400 (relaciones no soportadas / diagrama vacío / límite) y 404 (diagrama inexistente)
- [x] 5.3 Verificar que la generación es en memoria y no deja archivos temporales, con un test que compare el contenido del directorio temporal antes y después de una generación exitosa y de una fallida
- [x] 5.4 Verificar que la petición sin token responde 401 (con cuerpo JSON) sin generar, que el despacho ASYNC del streaming no se re-autoriza (requiere permitir `DispatcherType.ASYNC/ERROR`), y que el acceso se valida con `DiagramService` (proyecto/diagrama ajeno → 404)

## 6. Frontend

- [x] 6.1 Añadir en `shared/api/client.ts` el helper `authDownload` (fetch autenticado que parsea `{error}` en respuestas no-OK) y la llamada `generateBackend(projectId, diagramId)`, verificando el manejo de error con un test o prueba manual
- [x] 6.2 Añadir el botón "Generar backend" en `DiagramEditorPage` junto a "Exportar XMI", con estado de carga que bloquea la acción y descarga del `.zip` al éxito
- [x] 6.3 Mostrar los errores legibles del backend (relaciones no soportadas, diagrama vacío, sesión no válida, error genérico)
- [x] 6.4 Verificar que el frontend compila y tipa: `pnpm run build` y `npx tsc -b` sin errores

## 7. Verificación integral

- [x] 7.3 Verificar el rechazo con 400 y mensaje que liste las relaciones no soportadas usando un diagrama con herencia, y confirmar que no se descarga archivo
- [x] 7.4 Ejecutar la batería de tests del backend (`mvn test` de las clases afectadas) y confirmar que no hay regresiones
- [ ] 7.1 Prueba end-to-end: con un diagrama de 3 clases y asociaciones, descargar el `.zip` desde el editor, descomprimirlo, comprobar la estructura (4 capas + DTOs), compilarlo y ejecutarlo
- [ ] 7.2 Verificar en el backend generado que Swagger UI responde en `/swagger-ui.html`, que `/actuator/health` responde y que el CRUD de una entidad funciona (crear 201, listar 200, obtener 200, actualizar 200, eliminar 204, inexistente 404)
