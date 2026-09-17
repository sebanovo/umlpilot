## Purpose

Permite al usuario importar un archivo XMI 2.5.1 desde el editor de diagramas, añadiendo su contenido al diagrama actual y comunicando el resultado con notificaciones claras.

## Requirements

### Requirement: Acción de importar XMI en el editor
El frontend SHALL mostrar en el editor de diagramas una acción "Importar XMI" que permita elegir un archivo `.xmi`/`.xml` y lo envíe como `multipart/form-data` al endpoint de importación del diagrama actual, sin pedir confirmación destructiva porque la importación es aditiva.

#### Scenario: Importación y refresco exitosos
- **WHEN** el usuario elige un XMI válido y confirma
- **THEN** el frontend envía el archivo, refresca el lienzo y muestra el resultado con los elementos añadidos

#### Scenario: Indicador de progreso
- **WHEN** la importación está en curso
- **THEN** la acción muestra un estado de carga y no permite lanzar una segunda importación simultánea

#### Scenario: El archivo no reemplaza el diagrama
- **WHEN** el usuario importa sobre un diagrama con contenido
- **THEN** el frontend no pide borrar ni reemplazar nada y el contenido existente permanece visible

### Requirement: Notificaciones del resultado de la importación
El frontend SHALL mostrar un toast de éxito con el resumen de lo importado y los warnings generados, y SHALL mostrar un toast de error con el mensaje descriptivo devuelto por el backend cuando la importación falla.

#### Scenario: Éxito con resumen
- **WHEN** el backend responde 200 con el resumen de la importación
- **THEN** el frontend muestra un toast de éxito con los elementos añadidos

#### Scenario: Éxito con advertencias
- **WHEN** el backend responde 200 con warnings (por ejemplo, nombres renombrados por colisión)
- **THEN** el frontend muestra un toast de advertencia con el detalle de los warnings

#### Scenario: Versión XMI no soportada
- **WHEN** el backend responde 400 indicando que la versión XMI no es 2.5.1
- **THEN** el frontend muestra ese mensaje en un toast de error y no modifica el diagrama

#### Scenario: Tipo de diagrama no soportado
- **WHEN** el backend responde 400 indicando que el tipo de diagrama no está soportado
- **THEN** el frontend muestra ese mensaje en un toast de error y no modifica el diagrama

#### Scenario: Elemento no soportado
- **WHEN** el backend responde 400 indicando un elemento no soportado
- **THEN** el frontend muestra el tipo de elemento no soportado en un toast de error

#### Scenario: Error inesperado
- **WHEN** la importación falla por un error de red o del servidor distinto de los casos anteriores
- **THEN** el frontend muestra un mensaje de error genérico y permite reintentar

### Requirement: Refresco del lienzo tras la importación
El frontend SHALL reconstruir el diagrama tras una importación exitosa, tanto para el usuario que la solicitó como para los colaboradores que reciban el evento `diagram_changed` con `type: "import"`.

#### Scenario: Refresco del solicitante
- **WHEN** la importación termina correctamente
- **THEN** el editor del usuario que importó muestra los elementos importados sin recargar la página

#### Scenario: Refresco de otros colaboradores
- **WHEN** otro usuario tiene el mismo diagrama abierto
- **THEN** su editor refresca y muestra los elementos importados
