## Purpose

Permite al usuario generar y descargar, desde el editor de diagramas, el proyecto de backend Spring Boot derivado del diagrama de clases actual.

## Requirements

### Requirement: Acción de generación en el editor
El frontend SHALL mostrar en el editor de diagramas una acción "Generar backend" que solicite al backend la generación del proyecto Spring Boot del diagrama actual y, cuando la generación tenga éxito, descargue en el navegador el archivo `.zip` recibido.

#### Scenario: Generación y descarga exitosa
- **WHEN** el usuario pulsa "Generar backend" en un diagrama válido
- **THEN** el frontend solicita la generación y descarga el `.zip` con el nombre de archivo indicado por el servidor

#### Scenario: Indicador de progreso
- **WHEN** el usuario pulsa "Generar backend" y la generación está en curso
- **THEN** la acción muestra un estado de carga y no permite lanzar una segunda generación simultánea

### Requirement: Manejo de errores de generación
El frontend SHALL mostrar un mensaje de error legible cuando la generación falla, incluyendo el detalle devuelto por el backend para los casos de diagrama inválido.

#### Scenario: Diagrama con relaciones no soportadas
- **WHEN** el backend responde con un error que indica relaciones no soportadas
- **THEN** el frontend muestra el mensaje con las relaciones no soportadas y no descarga ningún archivo

#### Scenario: Diagrama sin entidades
- **WHEN** el backend responde que el diagrama no tiene entidades generables
- **THEN** el frontend muestra un mensaje que indica que el diagrama está vacío

#### Scenario: Sesión no válida
- **WHEN** el backend responde con un error de autenticación
- **THEN** el frontend muestra un mensaje de sesión no válida y no descarga ningún archivo

#### Scenario: Error inesperado
- **WHEN** la petición de generación falla por un error de red o del servidor distinto de los casos anteriores
- **THEN** el frontend muestra un mensaje de error genérico y permite reintentar
