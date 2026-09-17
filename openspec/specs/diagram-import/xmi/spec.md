## Purpose

Permite importar un diagrama de clases exportado en XMI 2.5.1 (Enterprise Architect, Visual Paradigm, MagicDraw o el propio UmlPilot) y **añadir** sus elementos al diagrama actual, validando el archivo de forma estricta antes de parsearlo.

## Requirements

### Requirement: Endpoint de importación XMI
El sistema SHALL exponer `POST /api/v1/projects/{projectId}/diagrams/{diagramId}/import/xmi` que reciba el archivo XMI como `multipart/form-data` (campo `file`) y devuelva un `ImportXmiResult` con el resumen de lo importado y los warnings generados.

#### Scenario: Importación correcta
- **WHEN** un usuario autenticado con acceso al diagrama sube un XMI 2.5.1 de clases válido
- **THEN** el sistema responde HTTP 200 con el resumen (clases, atributos, métodos, parámetros, literales y relaciones añadidas) y la lista de warnings

#### Scenario: Petición sin archivo
- **WHEN** la petición no incluye el campo `file` o el archivo está vacío
- **THEN** el sistema responde HTTP 400 con un mensaje que indica que el archivo XMI es requerido

#### Scenario: Diagrama inexistente
- **WHEN** el `diagramId` no existe o no pertenece al proyecto indicado
- **THEN** el sistema responde HTTP 404 con un mensaje de error legible

#### Scenario: Petición sin autenticación
- **WHEN** la petición no incluye un token válido
- **THEN** el sistema responde HTTP 401 con un cuerpo JSON de error y no modifica el diagrama

### Requirement: Validación de versión XMI
El sistema SHALL aceptar únicamente XMI 2.5.1: SHALL verificar el atributo `xmi:version` y el namespace `http://www.omg.org/spec/XMI/20131001` en la raíz del documento, y SHALL rechazar con HTTP 400 cualquier otra versión o cualquier documento XML no parseable.

#### Scenario: Versión no soportada
- **WHEN** el documento declara una versión XMI distinta de 2.5.1 (por ejemplo 2.1 o 2.4)
- **THEN** el sistema responde HTTP 400 con el mensaje `"Versión XMI no soportada. Se requiere XMI 2.5.1. Versión detectada: {version}"`

#### Scenario: Namespace XMI incorrecto
- **WHEN** el documento declara `xmi:version="2.5.1"` pero el namespace `xmi` no es el de XMI 2.5.1
- **THEN** el sistema responde HTTP 400 con el mensaje de versión no soportada

#### Scenario: XML inválido
- **WHEN** el archivo no es XML bien formado
- **THEN** el sistema responde HTTP 400 con un mensaje que indica que el archivo XMI no es válido

### Requirement: Validación de tipo de diagrama
El sistema SHALL aceptar únicamente diagramas de clases y SHALL rechazar con HTTP 400 los archivos cuyo modelo contenga elementos de otros tipos de diagrama (casos de uso, secuencia, actividad, estados, componentes o despliegue).

#### Scenario: Diagrama de clases
- **WHEN** el modelo contiene solo elementos de clase, interfaz, enumeración, nota y las relaciones permitidas
- **THEN** el sistema continúa con la importación

#### Scenario: Diagrama de otro tipo
- **WHEN** el modelo contiene elementos de otros tipos de diagrama (por ejemplo `uml:Actor`, `uml:UseCase`, `uml:Lifeline`, `uml:Activity`, `uml:StateMachine`, `uml:Interaction`)
- **THEN** el sistema responde HTTP 400 con el mensaje `"Tipo de diagrama no soportado. Solo se aceptan diagramas de clases."`

### Requirement: Validación de elementos y relaciones permitidos
El sistema SHALL tratar como contenido del diagrama únicamente los hijos `packagedElement` del modelo y sus paquetes, y SHALL ignorar (sin error) las construcciones ajenas al diagrama que las herramientas incluyen en el modelo: envoltorios propios (`primitivetypes`, `profiles`, `diagrams`, `profileApplication`, `xmi:Extension`), *diagram interchange* (`umldi:*`, `dc:*`) y definiciones de perfil (`uml:Profile`, `uml:Stereotype`, `uml:Extension`, `uml:PrimitiveType`). Los elementos permitidos son `uml:Class`, `uml:Interface`, `uml:Enumeration`, `uml:Comment` y `uml:DataType` (tratado como clase); las relaciones permitidas son `uml:Association` (incluidas `aggregation="composite"` y `aggregation="shared"`), `uml:Generalization`, `uml:AssociationClass` y `uml:Realization`. Los elementos de otros diagramas no soportados SHALL rechazarse con HTTP 400.

#### Scenario: XMI de Enterprise Architect con perfil propio
- **WHEN** el archivo incluye `profileApplication`, `primitivetypes`, `profiles`, `diagrams` o `xmi:Extension`
- **THEN** el sistema los ignora y importa solo las clases y relaciones del diagrama, sin errores ni warnings

#### Scenario: Elemento de otro diagrama no soportado
- **WHEN** el modelo contiene como contenido del diagrama un elemento no soportado (por ejemplo `uml:Component`, `uml:Artifact`, `uml:Node` o `uml:Device`)
- **THEN** el sistema responde HTTP 400 con el mensaje `"Elemento no soportado: {tipo}. Solo se permiten Clase, Interfaz, Enum, Nota y las relaciones Asociación, Generalización, Composición, Agregación, Clase asociación y Realización."`

#### Scenario: DataType como clase
- **WHEN** el modelo contiene un `uml:DataType`
- **THEN** el sistema lo importa como una clase con sus atributos

#### Scenario: Extensiones propietarias ignoradas
- **WHEN** el archivo incluye una `xmi:Extension` con datos de la herramienta de origen o elementos `umldi:*`
- **THEN** el sistema no los valida como elementos del modelo ni los importa

### Requirement: Importación aditiva y no destructiva
La importación SHALL ser aditiva: el sistema SHALL conservar todos los elementos y relaciones existentes del diagrama, SHALL generar identificadores UUID nuevos para lo importado (sin reutilizar los `xmi:id` del archivo) y, ante una colisión de nombres con un elemento existente, SHALL renombrar el importado con un sufijo numérico y registrar un warning.

#### Scenario: Diagrama con contenido previo
- **WHEN** se importa sobre un diagrama que ya tiene elementos y relaciones
- **THEN** los elementos y relaciones existentes permanecen intactos y los importados se añaden

#### Scenario: Colisión de nombres
- **WHEN** el archivo contiene una clase llamada igual que una existente en el diagrama
- **THEN** el sistema importa la clase con un sufijo numérico (por ejemplo `Cliente_1`) y añade un warning que lo indica

### Requirement: Mapeo de elementos UML a UmlPilot
El sistema SHALL mapear los elementos UML a los tipos de UmlPilot según la tabla: `uml:Class` → clase, `uml:Interface` → interfaz, `uml:Enumeration` → enumeración, `uml:Comment` → nota, `uml:DataType` → clase, `uml:Property` → atributo, `uml:Operation` → método, `uml:Parameter` → parámetro, `uml:EnumerationLiteral` → literal. El sistema SHALL importar únicamente los elementos **presentes en el diagrama** (con forma y `dc:bounds` en el *diagram interchange*) y SHALL omitir con un warning los elementos que solo existen en el modelo, conservando la posición y el tamaño declarados. Si el archivo no incluye ninguna información de diagrama, SHALL importar todos los elementos y colocarlos en una cuadrícula por debajo del contenido existente.

#### Scenario: Clase con atributos, métodos y parámetros
- **WHEN** el archivo contiene una clase con `uml:Property`, `uml:Operation` y `uml:Parameter`
- **THEN** el sistema crea el elemento y sus atributos, métodos y parámetros correspondientes

#### Scenario: Posiciones preservadas
- **WHEN** el archivo incluye `dc:bounds` para un elemento
- **THEN** el elemento importado conserva la posición y el tamaño del archivo

#### Scenario: Elemento del modelo que no está en el diagrama
- **WHEN** el archivo trae formas con `dc:bounds` y existe un elemento del modelo sin forma en el diagrama
- **THEN** el sistema no lo importa y añade un warning indicando que se omitió porque no está en el diagrama

#### Scenario: Archivo sin información de diagrama
- **WHEN** el archivo no incluye ninguna forma con `dc:bounds`
- **THEN** el sistema importa todos los elementos y los coloca en una cuadrícula debajo del contenido existente

#### Scenario: Nota importada
- **WHEN** el archivo contiene un `uml:Comment`
- **THEN** el sistema crea un elemento de tipo nota con el cuerpo del comentario como texto

#### Scenario: Atributo o parámetro sin tipo
- **WHEN** un atributo o un parámetro no declara tipo (no tiene `<type>`/`type`)
- **THEN** el sistema lo importa con el tipo por defecto de la aplicación (`String`), para no violar la restricción `NOT NULL` de la base de datos

### Requirement: Mapeo de relaciones UML a UmlPilot
El sistema SHALL mapear las relaciones según la tabla: `uml:Association` → asociación, `uml:Association` con `aggregation="composite"` → composición, `uml:Association` con `aggregation="shared"` → agregación, `uml:Generalization` → generalización, `uml:AssociationClass` → clase asociación con su clase asociada, y `uml:Realization` → realización. El sistema SHALL preservar las multiplicidades de los extremos (`lowerValue`/`upperValue`) como texto (`"0..*"`, `"1"`, etc.) y la visibilidad.

#### Scenario: Asociación con multiplicidades
- **WHEN** el archivo contiene una asociación con `lowerValue`/`upperValue` en sus extremos
- **THEN** la relación importada conserva esas multiplicidades como texto

#### Scenario: Agregación y composición
- **WHEN** el archivo contiene una asociación con `aggregation="shared"` o `aggregation="composite"`
- **THEN** el sistema crea una relación de agregación o composición respectivamente

#### Scenario: Clase asociación
- **WHEN** el archivo contiene un `uml:AssociationClass`
- **THEN** el sistema crea la clase asociada y la relación de tipo clase asociación

#### Scenario: Visibilidad preservada
- **WHEN** un atributo o método declara visibilidad (`public`, `private`, `protected`, `package`)
- **THEN** el elemento importado conserva esa visibilidad

### Requirement: Límites de importación
El sistema SHALL limitar la importación a un máximo de 200 elementos y los archivos a 10 MB, y SHALL rechazar con HTTP 400 cuando se exceda cualquiera de los dos límites.

#### Scenario: Archivo demasiado grande
- **WHEN** el archivo supera los 10 MB
- **THEN** el sistema responde HTTP 400 indicando el límite de tamaño

#### Scenario: Demasiados elementos
- **WHEN** el archivo contiene más de 200 elementos del modelo
- **THEN** el sistema responde HTTP 400 indicando el límite de elementos

### Requirement: Refresco en tiempo real de la importación
Al terminar la importación, el sistema SHALL publicar **un solo** evento `diagram_changed` en el canal de cambios del diagrama, incluyendo `type: "import"` en los datos, para que todos los colaboradores refresquen el diagrama.

#### Scenario: Evento único tras importar
- **WHEN** la importación finaliza correctamente
- **THEN** el sistema publica un único evento `diagram_changed` con `type: "import"`

#### Scenario: Colaboradores refrescan
- **WHEN** otro usuario tiene el mismo diagrama abierto
- **THEN** su editor reconstruye el diagrama y muestra los elementos importados sin recargar la página
