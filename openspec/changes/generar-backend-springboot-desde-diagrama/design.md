## Context

Ver `proposal.md - Why`. Estado y restricciones que condicionan el diseño:

- **UmlPilot (este proyecto)** sigue Arquitectura Hexagonal (`docs/architecture/backend-hexagonal.md`): el dominio no conoce Spring/JPA y los adaptadores viven en `infrastructure`. La generación es una capacidad *de* UmlPilot, pero **el código que produce es un proyecto Spring Boot de 4 capas clásicas**, ajeno a esa arquitectura.
- El modelo UML ya se persiste y se lee con `ElementService` (elementos, atributos, métodos, parámetros, literales) y `RelationshipService` (relaciones y multiplicidades con extremos `source`/`target`). No existe hoy un read-model que componga el diagrama completo; `UmlTools.getDiagramState` lo hace de forma ad-hoc para la IA.
- Los controladores usan `@RequestMapping` con la ruta `…/projects/{projectId}/diagrams/{diagramId}/…`, validan existencia del diagrama con `DiagramService` y traducen `DomainException` a 400. La autenticación es JWT vía `JwtAuthenticationFilter`; `SecurityConfig` protege `/api/**`.
- La exportación XMI es **exclusivamente de frontend** (`lib/xmi.ts` + descarga con `Blob` en `DiagramEditorPage`); no hay precedente de descarga binaria desde el backend.
- Tipos de relación existentes: `association`, `association_class`, `generalization`, `realization`, `dependency`, `aggregation`, `composition`, `template_binding` (`types/uml.ts`, `UmlEdges.tsx`). Tipos de elemento: `class`, `interface`, `enumeration`, `note`.
- No hay motor de plantillas ni `zip` en el `pom.xml`; `springdoc` está configurado en `application.yml` pero no como dependencia.

## Goals / Non-Goals

**Goals:**
- Endpoint que devuelva un `.zip` **streameado** con un proyecto Spring Boot Maven compilable (4 capas + DTOs).
- Generación **en memoria** con **plantillas StringTemplate (ST4)**, sanitización de identificadores y validación previa con errores 400 claros.
- Reutilizar los casos de uso existentes para leer el diagrama; no tocar el modelo de dominio ni la base de datos.
- Acción de descarga en el editor reutilizable para futuras exportaciones.

**Non-Goals:**
- Soporte de herencia/realización/dependencia/`template_binding`/agregación/composición (se rechazan explícitamente).
- Gradle, Kotlin, otros frameworks o multi-módulo; el proyecto generado es Maven + Java.
- Persistir o versionar los `.zip` generados, ni round-trip (importar el backend de vuelta).
- Relaciones bidireccionales con `mappedBy`, herencia JPA o mapeo de enumeraciones/literales UML a `enum` Java.
- Cambiar `DiagramBuildService`/`xmi.ts` (solo se comparte el criterio de multiplicidades).

## Decisions

### 1. Ubicación en la arquitectura hexagonal
- Nuevo caso de uso `application/service/SpringBootGenerationService` que orquesta: leer snapshot → validar → construir modelo de plantillas → delegar el rendimiento.
- Nuevo read-model `application/dto/DiagramSnapshot` (records: elemento + atributos + métodos/parámetros; relación + extremos + multiplicidades) producido por `application/service/DiagramSnapshotService` usando `ElementService`/`RelationshipService`.
- Puerto de salida `application/port/out/BackendProjectRenderer` (interfaz) implementado por `infrastructure/template/StringTemplateProjectRenderer`. *Alternativa considerada:* generar dentro del controlador o en el dominio (rechazada: rompe la regla de dependencia y mezcla infraestructura). Se introduce `application/port/out` siguiendo la guía de arquitectura del proyecto, aunque hoy solo existan `domain/repository`.
- Adaptador de entrada `infrastructure/adapter/in/web/BackendGenerationController`.

### 2. Motor de plantillas
- **StringTemplate 4** (`org.antlr:ST4`), plantillas `.st` en `src/main/resources/templates/springboot/`. *Alternativas:* Mustache (logic-less: obligaría a precalcular muchas vistas y duplicar lógica) y concatenación en Java (rechazada por requisito explícito). ST4 permite condicionales/iteración para los casos variables (id presente o no, tipo de relación, imports).
- El renderer recibe el modelo ya validado y devuelve `Map<String, String>` (ruta relativa → contenido). No accede a la base de datos.

### 3. Empaquetado y streaming sin disco
- Se valida y **renderiza todo en memoria** primero (bounded por el tope de clases), de modo que un error de validación aún pueda responder 400 antes de abrir el stream.
- El controlador devuelve `StreamingResponseBody` **directamente** (no envuelto en `ResponseEntity`) y escribe las entradas con `ZipOutputStream` sobre el `OutputStream` de la respuesta, sin construir un `byte[]` gigante ni archivos temporales. `Content-Type` (`application/zip`) y `Content-Disposition` se fijan sobre el `HttpServletResponse` antes de devolver el cuerpo; los errores (400/404) se resuelven con `@ExceptionHandler`.
- *Decisión revisada:* la primera versión usaba `ResponseEntity<StreamingResponseBody>`; Spring MVC no resuelve el cuerpo cuando va envuelto en `ResponseEntity` y cae en los conversores de mensajes (`HttpMessageNotWritableException: No converter for [...$$Lambda...]`). Devolver el `StreamingResponseBody` plano activa el `StreamingResponseBodyReturnValueHandler`.
- *Seguridad y despacho ASYNC:* Spring Boot registra la cadena de seguridad para los tipos de despacho `ASYNC` y `ERROR`. Al streamear, el contenedor hace un despacho ASYNC sobre el que `SecurityContextHolder` está vacío (la app es stateless), y `AuthorizationFilter` volvía a denegar (`AuthorizationDeniedException`) con la respuesta ya comprometida. `SecurityConfig` ahora hace `dispatcherTypeMatchers(ASYNC, ERROR).permitAll()` (y `/error` también es público), porque el despacho ASYNC es una continuación interna de una petición ya autorizada.
- *Riesgo asumido:* un fallo tras iniciar el stream ya no puede cambiar el código HTTP; se mitiga renderizando todo antes.

### 4. Validación (rechazo con 400)
- Solo `association` y `association_class`. Cualquier otro tipo (incluida la “propagación”, interpretada como `dependency`/`template_binding`, y `generalization`/`realization`/`aggregation`/`composition`) produce 400 con la lista de relaciones ofensoras (tipo + nombres de los elementos).
- Elementos: `class`, `interface`, `enumeration`, `table` y cualquier tipo desconocido → entidad; `note` → ignorado. La clase asociada de `association_class` → entidad.
- Diagrama sin entidades (vacío o solo notas) → 400.
- Tope de entidades configurable (`app.code-generation.max-classes`, por defecto 100, alineado con `DiagramBuildService.MAX_ELEMENTS`) → 400 si se excede.
- Diagrama inexistente/sin acceso → 404 (se valida vía `DiagramService`).

### 5. Modelo intermedio de generación
Una clase `BackendModel` (records internos, en `application/generation` o `application/dto`) con: `basePackage` fijo `com.generated`, entidades (nombre Java, tabla, campos escalares, referencias), tipos de relación resueltos y metadatos para DTOs. El modelo resuelve nombres/tipos **antes** de renderizar, de modo que las plantillas son tontas.

### 6. Mapeo de tipos UML → Java
- `String`→`String`, `int`→`Integer`, `long`→`Long`, `double`→`Double`, `boolean`→`Boolean`, `Date`→`java.util.Date`, `LocalDate`→`java.time.LocalDate`, `LocalDateTime`→`java.time.LocalDateTime`, `BigDecimal`→`java.math.BigDecimal`, `UUID`→`java.util.UUID`; comparación sin distinguir mayúsculas y tolerando sufijos; **tipo no reconocido → `String`**.
- Se usan **wrappers** (`Integer`/`Long`/`Double`/`Boolean`) en lugar de primitivos para permitir multiplicidad `0..1` y nulos de columna. *Alternativa:* primitivos literales (rechazada por imposibilidad de representar ausencia).
- Clave primaria: si la entidad no declara un atributo `id`, se añade `Long id` con `@Id` y `@GeneratedValue(strategy = IDENTITY)`; si lo declara, se respeta su tipo como `@Id`.
- `@Table(name = …)`: `snake_case` plural con pluralización simple (`-y`→`-ies`, terminaciones sibilantes→`-es`, resto→`-s`). *Limitación:* heurística en inglés; documentada en el `README` generado.

### 7. Mapeo de relaciones → JPA (unidireccional)
- La multiplicidad de un extremo es el número de instancias de ese extremo enlazables con **una** del otro; por tanto la referencia viva en el lado “muchos” apunta al lado “uno”.
- Reglas: extremo muchos + extremo uno → `@ManyToOne` en la entidad del lado muchos; ambos muchos → `@ManyToMany` en el origen con `@JoinTable`; ambos uno → `@OneToOne` con `@JoinColumn`; multiplicidad ausente → se asume `1`.
- Se generan **solo unidireccionales** para evitar `mappedBy` y la recursión infinita en la serialización JSON. *Alternativas:* bidireccional (rechazada por complejidad y riesgo de bucles), tabla intermedia explícita (la añade JPA automáticamente en `@ManyToMany`).
- `association_class`: la clase asociada se genera como entidad independiente; la asociación se trata como `association` normal entre origen y destino.

### 8. DTOs, servicio y controlador generados
- `<Entidad>Request`/`<Entidad>Response` como `record`. El `Request` expone los atributos escalares y las referencias a otras entidades como `<relacion>Id` (o `List<Long>` para colecciones); el `Response` añade el `id`. Nunca se expone la entidad JPA.
- `<Entidad>Service`: `findAll`, `findById`, `create`, `update`, `delete`; lanza `ResourceNotFoundException` si no existe.
- `<Entidad>Controller`: `@RestController` en `/api/v1/{recurso}` (recurso = nombre de tabla) con `POST` (201), `GET` (200), `GET/{id}` (200), `PUT/{id}` (200), `DELETE/{id}` (204) y `@Valid` en los cuerpos.
- `config/OpenApiConfig` (`@OpenAPIDefinition`/bean `OpenAPI`), `GeneratedApplication` y `GlobalExceptionHandler` (`@RestControllerAdvice`) que traduce `ResourceNotFoundException`→404 y `MethodArgumentNotValidException`/`ConstraintViolationException`→400.
- *Alternativa:* usar `ValidationException` de Jakarta (no existe como excepción lanzable por Bean Validation; se usan las de Spring/`ConstraintViolation`).

### 9. Frontend
- `shared/api/client.ts`: helper `authDownload` que hace `fetch` con el token, y en respuestas no-OK parsea el JSON `{error}` para lanzar un `Error` legible.
- `DiagramEditorPage`: botón “Generar backend” junto a “Exportar XMI”, con estado de carga que deshabilita la acción y muestra errores; al éxito desencadena la descarga del `.zip` con el `Content-Disposition` recibido.

### 10. Configuración
- `app.code-generation.max-classes` (env `CODE_GENERATION_MAX_CLASSES`, default 100) y `app.code-generation.base-package` (default `com.generated`).

## Risks / Trade-offs

- **Pluralización/`snake_case` heurísticos** → Mitigación: función simple y determinista, cubierta por tests unitarios y documentada en el `README` del proyecto generado.
- **Fallo tras comenzar el stream** deja la descarga corrupta sin poder cambiarse el HTTP → Mitigación: validar y renderizar todo antes de abrir el `ZipOutputStream`.
- **`StreamingResponseBody` se ejecuta en otro hilo** (sin `SecurityContext`) → Mitigación: resolver usuario/validar acceso de forma síncrona antes de devolver la respuesta; el render no necesita el contexto.
- **`@ManyToMany`/`@OneToOne` sin dueño explícito** puede producir esquemas inesperados → Mitigación: `@JoinTable`/`@JoinColumn` explícitos y solo unidireccional.
- **Dependencia ST4/ANTLR** añade transitivas nuevas → Mitigación: versión fijada; el proyecto no usa ANTLR hoy, sin conflicto esperado.
- **“El código generado compila”** es un criterio fuerte y costoso de verificar → Mitigación: tests de las plantillas y del manifiesto, más una prueba de compilación con un diagrama mínimo de 3 clases (JDK Compiler API o `mvn -q compile` sobre un fixture generado) marcada como la verificación clave.
- **Diagramas grandes** consumen memoria al renderizar todo antes de streamear → Mitigación: tope de clases (100) y contenidos de tamaño acotado.
- **Pérdida de información** por ignorar notas y rechazar tipos no soportados → Mitigación: mensajes de error explícitos; se documenta como alcance actual.

## Migration Plan

1. Añadir `org.antlr:ST4` al `pom.xml` y `app.code-generation.*` a `application.yml` (con defaults, sin variables obligatorias).
2. Desplegar backend con el endpoint nuevo (aditivo; ningún contrato existente cambia).
3. Desplegar frontend con el botón (aditivo; se oculta si la generación falla).
4. Rollback: quitar el botón y/o el endpoint; no hay migraciones de base de datos ni datos persistidos que revertir.
