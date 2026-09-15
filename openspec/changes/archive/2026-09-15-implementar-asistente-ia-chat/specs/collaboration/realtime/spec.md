## ADDED Requirements

### Requirement: Sincronización de cambios originados por el asistente
El sistema SHALL transmitir a todos los usuarios de la sala los cambios de elementos y relaciones aplicados por el asistente de IA, usando los mismos eventos y el mismo canal que los cambios originados por un usuario.

#### Scenario: Elemento modificado por el asistente
- **WHEN** una tool del asistente crea, actualiza o elimina un elemento
- **THEN** el evento `element_created`, `element_updated` o `element_deleted` se publica en el canal de cambios del diagrama

#### Scenario: Relación modificada por el asistente
- **WHEN** una tool del asistente crea, actualiza o elimina una relación
- **THEN** el evento `relationship_created`, `relationship_updated` o `relationship_deleted` se publica en el canal de cambios del diagrama

#### Scenario: Refresco de colaboradores
- **WHEN** un usuario distinto recibe un evento originado por el asistente
- **THEN** su editor reconstruye el diagrama y muestra el cambio en tiempo real

#### Scenario: Refresco del solicitante
- **WHEN** el usuario que solicitó la acción del asistente recibe el evento resultante
- **THEN** su editor refleja el cambio sin recargar la página
