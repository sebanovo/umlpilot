## Purpose

Define el flujo de diagramas y el editor móvil: listado y CRUD, canvas UML interactivo, edición de elementos y relaciones, bloqueo, colaboración en tiempo real, importación/exportación XMI y generación de backend.

## ADDED Requirements

### Requirement: Listar diagramas del proyecto
La app SHALL listar los diagramas del proyecto con nombre, tipo y estado de bloqueo.

#### Scenario: Diagramas listados
- **WHEN** el usuario abre un proyecto accesible
- **THEN** ve sus diagramas y puede abrir cada uno

#### Scenario: Sin diagramas
- **WHEN** el proyecto no tiene diagramas
- **THEN** se muestra un estado vacío con la acción de crear el primero

### Requirement: Crear y eliminar diagramas
La app SHALL permitir crear diagramas con nombre y tipo, y eliminarlos con confirmación a owner/editor.

#### Scenario: Crear diagrama
- **WHEN** un owner/editor envía un nombre y tipo válidos
- **THEN** el diagrama aparece en el listado

#### Scenario: Eliminar diagrama
- **WHEN** un owner/editor confirma la eliminación
- **THEN** el diagrama desaparece del listado

#### Scenario: Viewer intenta crear o eliminar
- **WHEN** un viewer intenta crear o eliminar un diagrama
- **THEN** la acción no está disponible y el backend la rechaza

### Requirement: Abrir el editor con permisos
La app SHALL abrir el editor para owner/editor en modo edición y para viewer en modo solo lectura.

#### Scenario: Acceso de viewer
- **WHEN** un viewer abre un diagrama
- **THEN** ve el canvas y una insignia de solo lectura, sin herramientas de mutación

#### Scenario: Acceso revocado
- **WHEN** un usuario sin acceso intenta abrir el diagrama
- **THEN** se muestra que no puede abrirlo y no se carga su contenido

### Requirement: Renderizar el canvas UML
La app SHALL representar clases, interfaces, enumeraciones y notas con sus atributos y métodos, y las relaciones con su tipo, nombre y multiplicidades, sobre una cuadrícula.

#### Scenario: Diagrama con contenido
- **WHEN** el diagrama tiene elementos y relaciones
- **THEN** se dibujan en sus posiciones con la notación UML correspondiente

### Requirement: Navegar el canvas
La app SHALL permitir hacer zoom, desplazar el lienzo y ajustar la vista al contenido.

#### Scenario: Gestos de navegación
- **WHEN** el usuario pellizca o arrastra el lienzo
- **THEN** la vista cambia de escala o posición sin modificar el diagrama

### Requirement: Añadir y mover elementos
La app SHALL permitir añadir elementos desde la caja de herramientas y moverlos con ajuste a la cuadrícula.

#### Scenario: Añadir clase
- **WHEN** el usuario elige "Clase" y toca el lienzo
- **THEN** se crea el elemento en esa posición y se sincroniza con el backend

#### Scenario: Mover elemento
- **WHEN** el usuario arrastra un elemento
- **THEN** la nueva posición se guarda y se refleja en el resto de colaboradores

### Requirement: Editar propiedades del elemento
La app SHALL permitir renombrar, cambiar visibilidad, gestionar atributos, métodos con parámetros y literales de enumeraciones, y eliminar el elemento.

#### Scenario: Añadir atributo
- **WHEN** el usuario agrega un atributo a un elemento seleccionado
- **THEN** el atributo aparece en el elemento y se persiste

#### Scenario: Añadir método con parámetros
- **WHEN** el usuario agrega un método y sus parámetros
- **THEN** se muestran en la notación del elemento y se persisten

#### Scenario: Eliminar elemento
- **WHEN** el usuario confirma eliminar un elemento
- **THEN** se elimina junto con sus relaciones dependientes

### Requirement: Gestionar relaciones
La app SHALL permitir crear relaciones seleccionando origen y destino, elegir el tipo, editar nombre y multiplicidades, y eliminarlas.

#### Scenario: Crear relación
- **WHEN** el usuario selecciona origen, destino y tipo
- **THEN** la relación se dibuja y se persiste

#### Scenario: Editar multiplicidad
- **WHEN** el usuario cambia las multiplicidades de una relación
- **THEN** las etiquetas se actualizan y se persisten

#### Scenario: Eliminar relación
- **WHEN** el usuario confirma eliminar una relación
- **THEN** la arista desaparece del canvas y del backend

### Requirement: Bloquear y desbloquear diagrama
La app SHALL permitir bloquear y desbloquear el diagrama, e impedir la edición cuando está bloqueado por otro usuario.

#### Scenario: Diagrama bloqueado por otro
- **WHEN** un usuario intenta editar un diagrama bloqueado por otra persona
- **THEN** se muestra un aviso y no se aplican cambios

### Requirement: Vaciar el diagrama
La app SHALL permitir eliminar todos los elementos y relaciones del diagrama con confirmación explícita.

#### Scenario: Eliminar todo
- **WHEN** el usuario confirma "Eliminar todo"
- **THEN** el diagrama queda vacío y se notifica al resto de colaboradores

### Requirement: Colaboración en tiempo real
La app SHALL conectarse al canal STOMP autenticado, unirse a la sala del diagrama y reflejar presencia y cambios remotos; MUST NOT unirse si el usuario no tiene acceso.

#### Scenario: Cambio remoto
- **WHEN** otro colaborador modifica el diagrama
- **THEN** el canvas se refresca con el estado actualizado

#### Scenario: Presencia
- **WHEN** hay colaboradores en el diagrama
- **THEN** se muestran los usuarios presentes y se avisa cuando entran o salen

#### Scenario: Sin acceso al WebSocket
- **WHEN** un usuario sin membresía intenta unirse a la sala
- **THEN** no recibe presencia ni cambios

### Requirement: Importar y exportar XMI
La app SHALL importar archivos XMI (máx. 10 MB) mostrando advertencias y refrescando el canvas, y exportar el diagrama como XMI 2.5.1 para compartirlo o guardarlo.

#### Scenario: Importación con advertencias
- **WHEN** el XMI contiene elementos renombrados o ignorados
- **THEN** se muestran las advertencias y el resumen de lo importado

#### Scenario: Exportación
- **WHEN** el usuario exporta el diagrama
- **THEN** se genera un archivo XMI descargable o compartible

### Requirement: Generar backend
La app SHALL solicitar la generación del backend Spring Boot del diagrama y permitir descargar o compartir el ZIP resultante.

#### Scenario: Generación correcta
- **WHEN** el usuario inicia la generación
- **THEN** recibe el archivo ZIP y puede guardarlo o compartirlo

#### Scenario: Generación rechazada
- **WHEN** el diagrama no cumple las reglas de generación
- **THEN** se muestra el mensaje de error del backend

### Requirement: Errores de permisos y red
La app SHALL mostrar mensajes claros ante 403, 404 y fallos de red, sin dejar el editor en un estado inconsistente.

#### Scenario: Permiso revocado en caliente
- **WHEN** una operación de edición es rechazada por permisos
- **THEN** se muestra el error y el editor pasa a solo lectura o sale del diagrama
