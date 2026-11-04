## Purpose

Integra el asistente de IA del backend en el editor móvil para conversar, dictar por voz y construir diagramas desde texto o imagen, sin ejecutar modelos locales ni usar Ollama.

## ADDED Requirements

### Requirement: Abrir el asistente desde el editor
La app SHALL ofrecer un acceso al asistente dentro del editor para owner/editor, mostrando la conversación en un panel o pantalla dedicada.

#### Scenario: Acceso al asistente
- **WHEN** un owner/editor abre el asistente
- **THEN** ve la conversación vacía y el campo de mensaje

### Requirement: Conversación con contexto del diagrama
La app SHALL enviar el mensaje y el historial de la sesión al endpoint de chat del diagrama y mostrar la respuesta del asistente.

#### Scenario: Mensaje enviado
- **WHEN** el usuario envía una consulta
- **THEN** se agrega el mensaje, se muestra el indicador de actividad y luego la respuesta

#### Scenario: Error del proveedor
- **WHEN** el backend devuelve un error del asistente
- **THEN** se muestra el mensaje de error sin perder la conversación

### Requirement: Reflejar acciones del asistente
Cuando el asistente ejecuta acciones que modifican el diagrama, la app SHALL refrescar el canvas.

#### Scenario: Acción de escritura
- **WHEN** la respuesta incluye acciones distintas de lectura
- **THEN** el canvas se recarga y muestra los cambios

### Requirement: Construir desde texto
La app SHALL permitir construir el diagrama completo a partir de una descripción de texto.

#### Scenario: Construcción desde texto
- **WHEN** el usuario envía una descripción
- **THEN** el asistente responde y el canvas refleja el diagrama construido

### Requirement: Construir desde imagen
La app SHALL permitir seleccionar o capturar una imagen y enviarla para construir el diagrama, mostrando el progreso y los errores.

#### Scenario: Construcción desde imagen
- **WHEN** el usuario elige una imagen válida
- **THEN** se envía al backend y el canvas se actualiza con el resultado

### Requirement: Dictado por voz
La app SHALL permitir grabar audio, transcribirlo mediante el backend y colocar el texto en el campo de mensaje.

#### Scenario: Dictado exitoso
- **WHEN** el usuario graba y detiene la grabación
- **THEN** el texto transcrito aparece en el campo de entrada

#### Scenario: Permiso de micrófono denegado
- **WHEN** el usuario no concede el permiso de micrófono
- **THEN** se muestra un aviso y no se inicia la grabación

### Requirement: Historial efímero
La conversación MUST vivir solo en memoria durante la sesión del editor y no persistirse en el servidor ni en almacenamiento local.

#### Scenario: Cerrar el asistente
- **WHEN** el usuario cierra el asistente y lo vuelve a abrir en la misma sesión
- **THEN** la conversación se conserva; al salir del editor se descarta

### Requirement: Sin Ollama ni inferencia local
La app MUST NOT llamar a un servidor Ollama ni cargar modelos locales; todo el asistente SHALL usar los endpoints del backend de UMLPilot.

#### Scenario: Arranque sin Ollama
- **WHEN** la app se ejecuta sin Ollama instalado
- **THEN** el asistente funciona igualmente mediante el backend

### Requirement: Permisos del asistente
El asistente que puede modificar el diagrama SHALL estar disponible solo para owner/editor; un lector no puede invocarlo.

#### Scenario: Viewer intenta usar el asistente
- **WHEN** un viewer abre un diagrama
- **THEN** la acción del asistente no está disponible
