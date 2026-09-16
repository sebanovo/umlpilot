## Purpose

Genera, a partir de un diagrama de clases UML existente, un proyecto Spring Boot completo y compilable (patrón de 4 capas + DTOs) empaquetado en un archivo `.zip` descargable, sin depender de la arquitectura interna de UmlPilot.

## ADDED Requirements

### Requirement: Endpoint de generación del backend
El sistema SHALL exponer `GET /api/v1/projects/{projectId}/diagrams/{diagramId}/generate/backend` que devuelva el proyecto Spring Boot generado como un archivo `.zip` descargable, transmitido como stream con `Content-Type: application/zip` y una cabecera `Content-Disposition` con el nombre del archivo.

#### Scenario: Generación exitosa
- **WHEN** un usuario autenticado con acceso al diagrama solicita la generación de un diagrama válido
- **THEN** el sistema responde HTTP 200 con un `.zip` que contiene el proyecto y una cabecera `Content-Disposition` de tipo attachment

#### Scenario: Petición sin autenticación
- **WHEN** la petición no incluye un token válido
- **THEN** el sistema responde HTTP 401 y no genera ningún archivo

#### Scenario: Diagrama inexistente
- **WHEN** el `diagramId` no existe o no pertenece al proyecto indicado
- **THEN** el sistema responde HTTP 404 con un mensaje de error legible

#### Scenario: Diagrama sin entidades
- **WHEN** el diagrama no contiene ningún elemento convertible en entidad
- **THEN** el sistema responde HTTP 400 con un mensaje que indica que el diagrama está vacío

### Requirement: Validación de relaciones soportadas
El sistema SHALL admitir únicamente relaciones de tipo asociación (`association`) y clase de asociación (`association_class`). Si el diagrama contiene herencia/generalización, realización, dependencia, `template_binding`, agregación o composición, el sistema SHALL rechazar la generación con HTTP 400 e indicar las relaciones no soportadas encontradas.

#### Scenario: Diagrama con relación no soportada
- **WHEN** el diagrama contiene al menos una relación de un tipo no admitido (por ejemplo, herencia, agregación o dependencia)
- **THEN** el sistema responde HTTP 400 e incluye en el mensaje los tipos y los elementos involucrados de las relaciones no soportadas

#### Scenario: Diagrama solo con asociaciones
- **WHEN** todas las relaciones del diagrama son `association` o `association_class`
- **THEN** el sistema procede con la generación sin errores de validación

### Requirement: Tratamiento de elementos del diagrama
El sistema SHALL tratar los elementos de tipo clase, interfaz, enumeración y tabla (o cualquier tipo no reconocido) como entidades JPA, SHALL tratar la clase asociada de una relación `association_class` como una entidad, y SHALL ignorar los elementos de tipo nota. El sistema SHALL rechazar con HTTP 400 la generación cuando, tras aplicar estas reglas, no queden entidades.

#### Scenario: Interfaz y enumeración como entidades
- **WHEN** el diagrama contiene una interfaz o una enumeración
- **THEN** el código generado las representa como entidades JPA igual que una clase

#### Scenario: Nota ignorada
- **WHEN** el diagrama contiene elementos de tipo nota
- **THEN** las notas no generan código ni entidades

#### Scenario: Clase de asociación como entidad
- **WHEN** una relación es de tipo `association_class` con una clase asociada
- **THEN** el sistema genera esa clase asociada como una entidad con sus atributos

### Requirement: Límite de tamaño del diagrama
El sistema SHALL imponer un tope máximo de entidades generables y SHALL rechazar con HTTP 400 la generación cuando el diagrama lo exceda, indicando el límite.

#### Scenario: Diagrama dentro del límite
- **WHEN** el número de entidades a generar es menor o igual al tope configurado
- **THEN** el sistema genera el proyecto normalmente

#### Scenario: Diagrama por encima del límite
- **WHEN** el número de entidades supera el tope máximo
- **THEN** el sistema responde HTTP 400 con un mensaje que indica el límite superado

### Requirement: Estructura del proyecto generado
El proyecto `.zip` SHALL contener `pom.xml`, `README.md`, `.gitignore`, `src/main/resources/application.yml` y, bajo `src/main/java`, la clase de arranque `GeneratedApplication` y los paquetes `model`, `repository`, `service`, `controller`, `dto` y `config`. El `pom.xml` SHALL declarar dependencias de Spring Boot Web, Spring Data JPA, PostgreSQL, Lombok, SpringDoc OpenAPI y Validation. La configuración `application.yml` SHALL permitir parametrizar el datasource.

#### Scenario: Contenido y estructura del `.zip`
- **WHEN** se inspecciona el `.zip` generado
- **THEN** contiene la clase de arranque, un paquete por capa, el `pom.xml`, el `application.yml`, el `README.md`, el `.gitignore` y la clase de configuración de OpenAPI

#### Scenario: Swagger UI disponible
- **WHEN** se ejecuta el backend generado
- **THEN** la documentación OpenAPI está accesible en `/swagger-ui.html`

#### Scenario: Health check disponible
- **WHEN** se ejecuta el backend generado
- **THEN** el endpoint de health check responde en `/actuator/health`

#### Scenario: El proyecto generado compila
- **WHEN** se compila el proyecto generado con sus dependencias declaradas
- **THEN** la compilación finaliza sin errores, con los imports necesarios incluidos

### Requirement: Mapeo de clases y atributos a código
El sistema SHALL generar cada entidad como una clase JPA anotada con `@Entity` y `@Table` con nombre de tabla en `snake_case` plural, SHALL generar cada atributo como un campo con su tipo Java mapeado y su columna anotada con `@Column`, y SHALL generar los métodos UML como métodos de la clase sin crear endpoints. Cuando una entidad no declare un atributo `id`, el sistema SHALL añadir una clave primaria `Long` autogenerada.

#### Scenario: Clase con atributos
- **WHEN** una clase tiene atributos con tipos UML
- **THEN** la entidad generada incluye esos atributos como campos con su tipo Java correspondiente y su columna

#### Scenario: Clave primaria autogenerada
- **WHEN** una clase no define un atributo `id`
- **THEN** la entidad generada incluye una clave primaria `Long` autogenerada

#### Scenario: Tipo UML no reconocido
- **WHEN** el tipo de un atributo no se corresponde con ningún tipo Java soportado
- **THEN** el sistema genera el campo con el tipo `String`

#### Scenario: Método UML
- **WHEN** una clase define un método
- **THEN** la entidad generada incluye ese método como método de la clase y no se expone como endpoint REST

### Requirement: Mapeo de relaciones a JPA
El sistema SHALL generar anotaciones JPA a partir de las relaciones de asociación según sus multiplicidades: uno-a-muchos con `@OneToMany`/`@ManyToOne`, uno-a-uno con `@OneToOne` y muchos-a-muchos con `@ManyToMany`. Una multiplicidad de rango abierto o múltiple SHALL generar una colección y una multiplicidad de exactamente uno SHALL generar una referencia simple obligatoria.

#### Scenario: Relación uno-a-muchos
- **WHEN** una asociación tiene una multiplicidad múltiple en un extremo
- **THEN** el lado correspondiente se genera como una colección

#### Scenario: Relación de uno
- **WHEN** una asociación tiene multiplicidad `1` en un extremo
- **THEN** el lado correspondiente se genera como una referencia simple obligatoria

#### Scenario: Relación muchos-a-muchos
- **WHEN** una asociación es múltiple en ambos extremos
- **THEN** el sistema genera la relación con `@ManyToMany`

### Requirement: CRUD REST generado por entidad
El sistema SHALL generar, para cada entidad, endpoints REST de creación, listado, consulta por identificador, actualización y eliminación bajo `/api/v1/{entidad}`, usando DTOs de petición y respuesta separados de la entidad JPA, validación de entrada declarativa, un manejador global de excepciones y los códigos HTTP correctos (201, 200, 204 y 404).

#### Scenario: Creación de recurso
- **WHEN** se invoca el endpoint de creación con datos válidos
- **THEN** el sistema responde HTTP 201 con el recurso en formato de DTO de respuesta

#### Scenario: Listado y consulta
- **WHEN** se invocan los endpoints de listado y consulta por identificador
- **THEN** el sistema responde HTTP 200 con los DTOs de respuesta correspondientes

#### Scenario: Actualización y eliminación
- **WHEN** se invocan los endpoints de actualización y eliminación de un recurso existente
- **THEN** el sistema responde HTTP 200 y HTTP 204 respectivamente

#### Scenario: Recurso inexistente
- **WHEN** se consulta, actualiza o elimina un recurso con un identificador inexistente
- **THEN** el sistema responde HTTP 404 con un mensaje de error legible

#### Scenario: Entrada inválida
- **WHEN** el cuerpo de una petición de creación o actualización incumple las reglas de validación
- **THEN** el sistema responde HTTP 400 con los errores de validación

#### Scenario: La entidad JPA no se expone
- **WHEN** se inspeccionan los contratos de los endpoints generados
- **THEN** las peticiones y respuestas usan DTOs y nunca la entidad JPA directamente

### Requirement: Generación en memoria y sanitización
El sistema SHALL generar el proyecto completamente en memoria, sin escribir archivos temporales en disco, y SHALL sanitizar los nombres de clases y paquetes para producir identificadores Java válidos.

#### Scenario: Sin archivos temporales
- **WHEN** finaliza una generación, exitosa o fallida
- **THEN** no queda ningún archivo temporal en el sistema de archivos

#### Scenario: Nombre inválido sanitizado
- **WHEN** el nombre de una clase contiene caracteres o espacios no válidos para Java
- **THEN** el sistema genera un identificador Java válido y el proyecto compila

#### Scenario: Fallo de generación
- **WHEN** la generación falla a mitad del proceso
- **THEN** el sistema responde con un error y no deja archivos ni recursos parciales en disco
