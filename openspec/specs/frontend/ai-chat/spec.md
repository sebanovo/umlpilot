# frontend/ai-chat Specification

## Purpose

Integra un panel de chat del asistente de IA dentro del editor de diagramas, permitiendo conversar con el modelo y ver reflejadas en el lienzo las acciones que el asistente ejecuta sobre el diagrama.

## Requirements

### Requirement: Panel de chat en el editor
El sistema SHALL mostrar un panel de chat dentro de la página del editor de diagramas, con una lista de mensajes, un campo de entrada y una acción de envío.

#### Scenario: Abrir el panel
- **WHEN** el usuario abre el editor de un diagrama
- **THEN** puede mostrar u ocultar el panel de chat sin perder el estado del lienzo

#### Scenario: Enviar un mensaje
- **WHEN** el usuario escribe un mensaje y envía
- **THEN** el mensaje del usuario se agrega a la conversación y se envía al endpoint del asistente con el contexto del diagrama

#### Scenario: Entero vacío no enviado
- **WHEN** el campo de entrada está vacío
- **THEN** el botón de envío está deshabilitado y no se envía ninguna petición

### Requirement: Contexto del diagrama en la conversación
El sistema SHALL enviar cada mensaje al endpoint del asistente incluyendo el proyecto, el diagrama y el historial de la conversación de la sesión.

#### Scenario: Continuidad de la conversación
- **WHEN** el usuario envía un segundo mensaje que depende del primero
- **THEN** el asistente recibe el historial previo y responde en contexto

### Requirement: Historial efímero
El historial de la conversación SHALL permanecer solo en memoria del cliente durante la sesión y no persistirse en el servidor ni en almacenamiento del navegador.

#### Scenario: Recarga de la página
- **WHEN** el usuario recarga el editor
- **THEN** la conversación comienza vacía

### Requirement: Indicador de actividad y errores
El sistema SHALL indicar mientras el asistente procesa una petición y SHALL mostrar un mensaje de error legible cuando la petición falla.

#### Scenario: Petición en curso
- **WHEN** el usuario envía un mensaje y el asistente aún responde
- **THEN** el panel muestra un indicador de actividad y bloquea el envío de otro mensaje

#### Scenario: Error del asistente
- **WHEN** el endpoint responde con error (por ejemplo, proveedor no disponible)
- **THEN** el panel muestra un mensaje de error y permite reintentar

### Requirement: Reflejo de las acciones en el lienzo
El sistema SHALL actualizar el lienzo para reflejar las mutaciones ejecutadas por el asistente, sin recargar la página.

#### Scenario: Acción del asistente aplicada
- **WHEN** el asistente crea, edita o elimina un elemento o relación
- **THEN** el editor reconstruye el diagrama y muestra el cambio

#### Scenario: Cambio recibido por otros colaboradores
- **WHEN** otro usuario tiene el mismo diagrama abierto
- **THEN** su lienzo también refleja el cambio por el canal de tiempo real
