# ai-assistant/chat Specification

## Purpose

Asistente conversacional que interpreta instrucciones en lenguaje natural y ejecuta operaciones sobre el modelo UML de un diagrama (elementos, atributos/métodos y relaciones) mediante *function calling*, reutilizando los casos de uso existentes y el contexto de acceso del diagrama.

## Requirements

### Requirement: Endpoint de chat con contexto de diagrama
El sistema SHALL exponer `POST /api/v1/projects/{projectId}/diagrams/{diagramId}/ai/chat` que reciba el mensaje del usuario junto con el historial de la conversación y retorne la respuesta del asistente más la lista de acciones ejecutadas (tipo, entidad y resultado).

#### Scenario: Consulta con respuesta y acciones
- **WHEN** un usuario autenticado con acceso al diagrama envía un mensaje
- **THEN** el sistema responde HTTP 200 con el texto del asistente y un arreglo de acciones ejecutadas

#### Scenario: Mensaje vacío
- **WHEN** el mensaje es nulo, vacío o solo espacios
- **THEN** el sistema responde HTTP 400

#### Scenario: Petición sin autenticación
- **WHEN** la petición no incluye un token válido
- **THEN** el sistema responde HTTP 401 y no ejecuta ninguna acción

#### Scenario: Diagrama inexistente
- **WHEN** el `diagramId` no existe
- **THEN** el sistema responde HTTP 404

### Requirement: Tools de elementos UML
El sistema SHALL exponer al modelo tools para crear, listar, actualizar y eliminar elementos UML (class, interface, enumeration, dataType, note), invocando los casos de uso existentes.

#### Scenario: Crear clase desde lenguaje natural
- **WHEN** el usuario pide "crea una clase Cliente"
- **THEN** el asistente invoca la tool de creación y el elemento aparece persistido en el diagrama

#### Scenario: Eliminar elemento
- **WHEN** el usuario pide eliminar un elemento existente
- **THEN** el asistente elimina el elemento y sus hijos (atributos, métodos y relaciones asociadas)

#### Scenario: Actualizar elemento
- **WHEN** el usuario pide renombrar o mover un elemento
- **THEN** el asistente actualiza nombre, visibilidad o posición del elemento

### Requirement: Tools de atributos y métodos
El sistema SHALL exponer al modelo tools para crear, listar, actualizar y eliminar atributos y métodos (y sus parámetros) de un elemento.

#### Scenario: Agregar atributo
- **WHEN** el usuario pide "agrega el atributo nombre:String a Cliente"
- **THEN** el asistente crea el atributo con nombre, tipo y visibilidad en el elemento indicado

#### Scenario: Agregar método con parámetros
- **WHEN** el usuario describe un método con parámetros
- **THEN** el asistente crea el método y sus parámetros en el elemento indicado

#### Scenario: Eliminar atributo o método
- **WHEN** el usuario pide eliminar un atributo o método existente
- **THEN** el asistente elimina la entidad (y los parámetros del método si aplica)

### Requirement: Tools de relaciones UML
El sistema SHALL exponer al modelo tools para crear, listar, actualizar y eliminar relaciones (association, aggregation, composition, generalization, realization, dependency, association_class, template_binding), incluyendo nombre, dirección y multiplicidades.

#### Scenario: Crear asociación
- **WHEN** el usuario pide "relaciona Cliente con Pedido"
- **THEN** el asistente crea la relación con el tipo indicado entre los elementos existentes

#### Scenario: Extremo inexistente
- **WHEN** la relación referencia un elemento que no pertenece al diagrama
- **THEN** la tool reporta el error y no se crea la relación

### Requirement: Lectura del estado actual del diagrama
El sistema SHALL exponer al modelo tools de lectura que devuelvan los elementos, atributos, métodos y relaciones del diagrama, para responder preguntas y resolver referencias antes de mutar.

#### Scenario: Pregunta sobre el modelo
- **WHEN** el usuario pregunta cuántas clases tiene el diagrama
- **THEN** el asistente usa la tool de listado y responde con datos reales del diagrama

#### Scenario: Resolución de nombres
- **WHEN** el usuario menciona elementos por nombre en lugar de id
- **THEN** el asistente los resuelve consultando el estado del diagrama antes de operar

### Requirement: Aislamiento por diagrama
El sistema SHALL restringir toda tool al diagrama de la conversación; ninguna operación puede afectar elementos de otro diagrama.

#### Scenario: Referencia fuera del diagrama
- **WHEN** una tool recibe un elemento que no pertenece al diagrama de la conversación
- **THEN** la operación se rechaza y el asistente informa el error, sin modificar datos de otros diagramas

### Requirement: Manejo de errores y bucle de herramientas acotado
El sistema SHALL acotar el número de iteraciones del bucle de *tool calling* y SHALL manejar los fallos del proveedor y de las tools sin dejar la conversación en un estado inconsistente.

#### Scenario: Proveedor de IA no disponible
- **WHEN** el proveedor LLM falla o está agotado
- **THEN** el sistema responde HTTP 503 con un mensaje de error y no aplica cambios parciales

#### Scenario: Argumentos inválidos de una tool
- **WHEN** el modelo invoca una tool con argumentos inválidos
- **THEN** la tool devuelve un error de validación al modelo y el bucle continúa o finaliza con una respuesta, sin lanzar error al cliente

#### Scenario: Límite de iteraciones alcanzado
- **WHEN** el bucle de tools alcanza el máximo configurado de iteraciones
- **THEN** el sistema responde con el mejor resultado parcial disponible

### Requirement: Configuración del proveedor
El sistema SHALL obtener la URL base, el modelo y las credenciales del proveedor LLM desde la configuración de la aplicación, sin exponerlas en las respuestas.

#### Scenario: Configuración por entorno
- **WHEN** se define la configuración del proveedor (URL base y modelo OpenAI-compatible)
- **THEN** el asistente usa esa configuración y no incluye claves ni detalles internos en la respuesta
