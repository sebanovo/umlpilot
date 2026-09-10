## Purpose

Proporciona un editor visual interactivo para diagramas de clases UML con canvas, toolbar y panel de propiedades.

## ADDED Requirements

### Requirement: Canvas interactivo
El frontend SHALL renderizar un canvas donde los elementos UML se muestran como cajas interactivas que se pueden arrastrar, redimensionar y seleccionar.

#### Scenario: Elemento arrastrado
- **WHEN** el usuario arrastra un elemento UML en el canvas
- **THEN** la posición del elemento se actualiza en tiempo real y se sincroniza con el backend vía WebSocket

#### Scenario: Elemento seleccionado
- **WHEN** el usuario hace clic en un elemento
- **THEN** el elemento se resalta y el panel de propiedades muestra sus datos

### Requirement: Toolbar de elementos
El frontend SHALL mostrar una toolbar con opciones para crear nuevos elementos: Clase, Interfaz, Enumeración, Nota, y tipos de relación.

#### Scenario: Crear clase desde toolbar
- **WHEN** el usuario hace clic en "Clase" en la toolbar y luego hace clic en el canvas
- **THEN** se crea una nueva clase en la posición del clic con nombre por defecto "ClassName"

#### Scenario: Crear relación
- **WHEN** el usuario selecciona un tipo de relación en la toolbar, hace clic en elemento origen y luego en destino
- **THEN** se crea una relación entre los dos elementos

### Requirement: Panel de propiedades
El frontend SHALL mostrar un panel lateral derecho con las propiedades del elemento seleccionado — nombre, visibilidad, estereotipo, atributos, métodos.

#### Scenario: Editar nombre de clase
- **WHEN** el usuario selecciona una clase y cambia el nombre en el panel
- **THEN** el nombre se actualiza en el canvas y se sincroniza con el backend

#### Scenario: Añadir atributo desde panel
- **WHEN** el usuario hace clic en "Añadir atributo" en el panel de propiedades
- **THEN** se muestra un formulario para crear un atributo y se guarda en el backend

### Requirement: Renderizado UML
El frontend SHALL renderizar elementos UML según especificación:
- **Clase**: Caja con 3 secciones (nombre, atributos, métodos), separadores, visibilidad (±#~)
- **Interfaz**: Caja con «interface» estereotipo
- **Enumeración**: Caja con «enumeration» estereotipo y lista de literales
- **Relaciones**: Líneas con estilos según tipo (sólida, punteada, flecha, diamante)

#### Scenario: Clase renderizada
- **WHEN** existe una clase con 2 atributos y 1 método
- **THEN** el canvas muestra una caja con nombre arriba, atributos en medio, métodos abajo

### Requirement: Zoom y pan
El frontend SHALL soportar zoom con scroll del mouse y pan con arrastre del fondo del canvas.

### Requirement: Grid y snap
El frontend SHALL mostrar una cuadrícula de fondo y alinear elementos al grid al soltarlos.
