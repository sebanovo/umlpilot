## Purpose

Define el visor móvil de diagramas: listar y gestionar diagramas del proyecto, renderizar el UML en modo solo lectura con zoom/pan, inspeccionar elementos y relaciones, y mantenerse actualizado en tiempo real. La edición del diagrama queda fuera del alcance de la app móvil.

## ADDED Requirements

### Requirement: Listar diagramas del proyecto
La app SHALL listar los diagramas del proyecto con nombre, tipo y estado de bloqueo.

#### Scenario: Diagramas listados
- **WHEN** el usuario abre un proyecto accesible
- **THEN** ve sus diagramas y puede abrir cada uno en modo lectura

#### Scenario: Sin diagramas
- **WHEN** el proyecto no tiene diagramas
- **THEN** se muestra un estado vacío con la acción de crear el primero

### Requirement: Crear y eliminar diagramas
La app SHALL permitir crear diagramas con nombre y tipo, y eliminarlos con confirmación a owner/editor. La gestión del diagrama no habilita su edición en la app.

#### Scenario: Crear diagrama
- **WHEN** un owner/editor envía un nombre y tipo válidos
- **THEN** el diagrama aparece en el listado

#### Scenario: Eliminar diagrama
- **WHEN** un owner/editor confirma la eliminación
- **THEN** el diagrama desaparece del listado

#### Scenario: Viewer intenta crear o eliminar
- **WHEN** un viewer intenta crear o eliminar un diagrama
- **THEN** la acción no está disponible y el backend la rechaza

### Requirement: Abrir el visor sin edición
La app SHALL abrir el diagrama en modo solo lectura para cualquier miembro con acceso, sin herramientas ni acciones que muten el diagrama.

#### Scenario: Acceso de miembro
- **WHEN** un miembro abre un diagrama
- **THEN** ve el canvas y la insignia "Solo lectura", sin caja de herramientas ni paneles de edición

#### Scenario: Acceso revocado
- **WHEN** un usuario sin acceso intenta abrir el diagrama
- **THEN** se muestra que no puede abrirlo y no se carga su contenido

### Requirement: Renderizar el canvas UML completo
La app SHALL representar clases, interfaces, enumeraciones y notas con sus atributos y métodos, y todas las relaciones con su notación, nombre y multiplicidades: asociación, generalización, realización, agregación, composición y clase de asociación.

#### Scenario: Diagrama con contenido
- **WHEN** el diagrama tiene elementos y relaciones
- **THEN** se dibujan en sus posiciones con la notación UML correspondiente

#### Scenario: Clase de asociación
- **WHEN** una relación tiene una clase de asociación
- **THEN** se dibuja una línea discontinua que conecta la relación con su clase

#### Scenario: Relación recursiva
- **WHEN** una relación une un elemento consigo mismo
- **THEN** se dibuja un bucle visible junto al elemento

#### Scenario: Tipos de relación
- **WHEN** el diagrama contiene generalización, realización, agregación o composición
- **THEN** cada una se dibuja con su marcador correspondiente (triángulo vacío, línea discontinua, rombo vacío o lleno)

### Requirement: Límites del canvas
La app SHALL calcular el área del canvas a partir de la posición y tamaño de los elementos, con margen suficiente, para que ningún elemento, etiqueta o bucle quede recortado, incluso con posiciones negativas.

#### Scenario: Elementos fuera del origen
- **WHEN** hay elementos en posiciones negativas o lejanas
- **THEN** el canvas se expande para mostrarlos y pueden alcanzarse con pan/zoom

### Requirement: Navegar el canvas
La app SHALL permitir hacer zoom, desplazar el lienzo y ajustar la vista al contenido.

#### Scenario: Gestos de navegación
- **WHEN** el usuario pellizca o arrastra el lienzo
- **THEN** la vista cambia de escala o posición sin modificar el diagrama

#### Scenario: Ajustar a contenido
- **WHEN** el usuario pulsa "Ajustar vista"
- **THEN** todo el contenido del diagrama queda visible

### Requirement: Inspeccionar detalles en solo lectura
La app SHALL permitir seleccionar un elemento o una relación y consultar sus detalles (nombre, tipo, visibilidad, atributos, métodos, parámetros, literales o multiplicidades) sin modificarlos.

#### Scenario: Detalle de elemento
- **WHEN** el usuario selecciona un elemento y abre "Detalles"
- **THEN** ve su información completa en solo lectura

#### Scenario: Detalle de relación
- **WHEN** el usuario selecciona una relación
- **THEN** ve su tipo, extremos y multiplicidades

### Requirement: Colaboración en tiempo real
La app SHALL conectarse al canal STOMP autenticado, unirse a la sala del diagrama y reflejar presencia y cambios remotos; MUST NOT unirse si el usuario no tiene acceso.

#### Scenario: Cambio remoto
- **WHEN** otro colaborador modifica el diagrama
- **THEN** el canvas se refresca con el estado actualizado

#### Scenario: Presencia
- **WHEN** hay colaboradores en el diagrama
- **THEN** se muestran los usuarios presentes

#### Scenario: Sin acceso al WebSocket
- **WHEN** un usuario sin membresía intenta unirse a la sala
- **THEN** no recibe presencia ni cambios

#### Scenario: Cierre del visor
- **WHEN** el usuario sale del diagrama
- **THEN** la conexión y las suscripciones se cierran sin emitir eventos después del cierre

### Requirement: Errores de permisos y red
La app SHALL mostrar mensajes claros ante 403, 404 y fallos de red, sin dejar el visor en un estado inconsistente.

#### Scenario: Permiso revocado
- **WHEN** el usuario pierde el acceso al diagrama
- **THEN** se muestra el error y se ofrece volver a proyectos
