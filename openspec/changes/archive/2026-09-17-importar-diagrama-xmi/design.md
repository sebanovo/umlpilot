## Context

Ver `proposal.md - Why`. Estado y restricciones que condicionan el diseño:

- **La exportación XMI ya existe y es de frontend** (`umlpilot_web/src/features/diagram-editor/lib/xmi.ts`): usa los namespaces `xmi=http://www.omg.org/spec/XMI/20131001`, `uml=http://www.omg.org/spec/UML/20131001`, `umldi` y `dc`, emite `umldi:Diagram` con `<bounds xmi:type="dc:bounds">` y una `xmi:Extension` de Enterprise Architect. El importador debe ser el inverso: leer el modelo `uml:*` y posiciones de `dc:bounds`, **ignorando** `umldi:*` y las extensiones propietarias.
- **Existe un precedente de alta masiva aditiva**: `DiagramBuildService` (transaccional, aditivo, cuadrícula, `MAX_ELEMENTS/MULTIPLICITY` parsing) y `DiagramEventPublisher.diagramChanged(...)` → evento `diagram_changed`, que `useDiagramState` ya usa para refrescar todo el diagrama.
- **Precedente de subida de archivos**: `AiChatController` recibe `MultipartFile`; el frontend usa `authUpload` en `shared/api/client.ts`. `application.yml` ya limita `multipart` a 10 MB.
- **No hay librería XML en el `pom.xml`**: se usa JAXP/DOM del JDK.
- Modelo de dominio y casos de uso reutilizables: `ElementService`/`RelationshipService` (elementos, atributos, métodos, parámetros, literales, relaciones y multiplicidades con `max = -1` = `*`). Sin cambios de esquema de base de datos.
- Convención hexagonal del proyecto: el dominio no conoce infraestructura; el renderizado de plantillas se resolvió con un puerto en `application/port/out` (precedente `BackendProjectRenderer`).

## Goals / Non-Goals

**Goals:**
- Importar XMI 2.5.1 de clases de forma **aditiva**, preservando posiciones cuando el archivo las traiga.
- Validación estricta y errores 400 **descriptivos** (versión, tipo de diagrama, elemento no soportado, XML inválido, límites).
- Un único evento de refresco para todos los colaboradores.
- UI con acción de importar y notificaciones (toasts) de éxito/warnings/error.

**Non-Goals:**
- Importar diagramas de secuencia, casos de uso, actividad, estados, componentes o despliegue.
- Mapear estereotipos, plantillas (`template_binding`), dependencias ni `uml:DataType`.
- Reutilizar los `xmi:id` del archivo como identificadores.
- Importar varios archivos a la vez o hacer merge diferencial (se refresca el diagrama completo).
- Modificar el exportador XMI existente.

## Decisions

### 1. Ubicación en la arquitectura hexagonal
- Puerto de salida `application/port/out/XmiDocumentParser` con un modelo intermedio `application/dto/ImportedDiagram` (records: elementos con atributos/métodos/parámetros/literales, relaciones con extremos y multiplicidades, posiciones). Implementación `infrastructure/xmi/DomXmiParser`. *Alternativa rechazada:* parsear dentro del servicio de aplicación o del controlador (mezcla infraestructura y hace el parser difícil de testear aislado).
- Caso de uso `application/service/XmiImportService` (`@Transactional`) que reutiliza `ElementService`/`RelationshipService` para persistir, igual que `DiagramBuildService`.
- Adaptador de entrada `infrastructure/adapter/in/web/DiagramImportController` (`POST .../import/xmi`, `multipart/form-data`), con el patrón de errores del proyecto: `DomainException` → 400, diagrama inexistente → 404.

### 2. Parseo XML seguro (JAXP/DOM)
- `DocumentBuilderFactory` con `FEATURE_SECURE_PROCESSING`, `disallow-doctype-decl=true` y entidades externas deshabilitadas (evita **XXE**), `setExpandEntityReferences(false)`, `setXIncludeAware(false)`.
- Validación en este orden: (a) XML bien formado → si falla, 400 "archivo XMI no válido"; (b) `xmi:version` + namespace XMI 2.5.1 → si no, 400 con la versión detectada; (c) solo se rechazan los elementos de diagrama no soportados (otros tipos de diagrama y `Component`/`Artifact`/`Node`/…) con 400.
- *Decisión revisada:* la allow-list estricta ("cualquier otro elemento → error") rompía los XMI reales de Enterprise Architect, que incluyen envoltorios propios (`primitivetypes`, `profiles`, `diagrams`, `profileApplication`, `xmi:Extension`) y definiciones de perfil (`uml:Profile`, `uml:Stereotype`, `uml:Extension`, `uml:PrimitiveType`). Ahora solo se considera contenido del diagrama a los hijos `packagedElement` del modelo/paquetes, y **todo lo demás se ignora** (no se importa ni se rechaza); `uml:DataType` se importa como clase para permitir el round-trip de los XMI de UmlPilot. El rechazo queda acotado a elementos de otros diagramas.

### 3. Modelo intermedio y resolución de referencias
- El parser resuelve los `xmi:id` del archivo a **claves locales** y arma `ImportedDiagram`: elementos con `localKey`, tipo, nombre, visibilidad, posición/tamaño; relaciones con `sourceKey`/`targetKey` y multiplicidades ya normalizadas a `min`/`max` (con `-1` = `*`).
- Relaciones con referencias colgantes (extremos que apuntan a `xmi:id` no presentes o no permitidos) se **omiten con warning** en lugar de abortar. *Alternativa:* 400; se descarta para no romper archivos grandes con basura parcial.
- `uml:Association` se resuelve por sus `memberEnd`/`ownedEnd` y el `type xmi:idref` de cada extremo; `aggregation` en el extremo decide composición/agregación.

### 4. Importación aditiva y renombrado
- Nunca se borra ni modifica nada existente. Los UUID los genera UmlPilot (`ElementService.createElement`), ignorando los `xmi:id`.
- Colisión de nombres: se compara **sin distinguir mayúsculas** contra los nombres ya existentes y contra los importados en el mismo lote; si hay colisión se añade sufijo numérico (`Cliente_1`, `Cliente_2`) y se agrega un warning por cada renombrado. *Alternativa:* rechazar la importación (peor UX) o permitir duplicados (confuso).
- Posiciones: si el elemento tiene `dc:bounds` se conserva `x`/`y`/`width`/`height`.
- *Decisión revisada:* se importa **solo lo que está dibujado en el diagrama**. Si el archivo trae formas con `dc:bounds` (todo XMI con *diagram interchange*), los elementos del modelo sin forma (p. ej. `Class1` en un export de EA: existe en el paquete pero no se dibujó) se omiten con un warning, porque el usuario espera importar el diagrama, no todo el modelo. Si el archivo no trae ninguna información de diagrama, se importan todos y se colocan en cuadrícula **por debajo del contenido existente** (mismo criterio que `DiagramBuildService.computeStartY`).
- Asociación-clase: se crea la clase asociada como elemento y la relación `association_class` enlazándola (`associationClassElementId`).

### 5. Evento de refresco
- Se reutiliza `DiagramEventPublisher`. Se añade un parámetro de origen a `diagramChanged` para que el payload incluya `type: "import"` (`data.type`), manteniendo el evento `diagram_changed` que ya maneja `useDiagramState`. *Alternativa rechazada:* crear un evento nuevo `diagram_updated` (duplicaría el manejo en el frontend sin aportar nada).
- Se emite **un único** evento al finalizar la transacción, no uno por elemento.

### 6. Límites y errores
- Tope de **200 elementos** y **10 MB** por archivo. El tamaño lo corta `spring.servlet.multipart`; si se excede, un `@ExceptionHandler(MaxUploadSizeExceededException.class)` en el controlador devuelve 400 con el límite. El tope de elementos lo valida el caso de uso antes de persistir.
- Todos los mensajes de validación son los del requerimiento (versión detectada, tipo de diagrama, elemento no soportado).
- *Campos `NOT NULL`*: `attribute.data_type`, `parameter.data_type` y `method.return_type` no admiten nulos. Al importar se aplican los defaults de la UI: tipo `String` para atributos/parámetros sin tipo y `void` para métodos sin retorno, evitando `DataIntegrityViolationException`.

### 7. Frontend
- `shared/api/client.ts`: `xmiImportApi.import(projectId, diagramId, file)` con `authUpload` (multipart, campo `file`).
- `DiagramEditorPage`: botón "Importar XMI" junto a "Exportar XMI" con `<input type="file" accept=".xmi,.xml" hidden>`, estado de carga que bloquea doble importación y descarte del `input` para poder reimportar el mismo archivo.
- Resultado: `toast` de éxito con el resumen (`n` clases, atributos, relaciones…), `toast` de advertencia si hay warnings, y `toast` de error con el mensaje del backend. Tras el éxito se refresca el lienzo (y `diagram_changed` refresca a los demás).

## Risks / Trade-offs

- **XXE / XML malicioso** → *Mitigación:* parseo con *secure processing*, sin DTD ni entidades externas, y límite de 10 MB.
- **Cuerpo de nota larga**: el `name` del elemento es un `varchar` (255 por defecto) y las notas usan ese campo → *Mitigación:* truncar a 255 caracteres y añadir un warning cuando se trunque. *Limitación documentada.*
- **Construcciones ajenas al diagrama** (envoltorios y perfiles de EA, `uml:PrimitiveType`, estereotipos) → *Mitigación:* se ignoran; solo se rechazan elementos de diagrama no soportados. Riesgo residual: un elemento de diagrama desconocido se ignora silenciosamente en vez de avisar; los tipos conocidos de otros diagramas sí se rechazan.
- **Referencias colgantes o ids desconocidos** → *Mitigación:* omitir la relación con warning (no abortar).
- **Solapamiento visual** si el archivo trae posiciones parciales mezcladas con cuadrícula → *Trade-off aceptado:* se prioriza no destruir el layout existente.
- **Transacción larga** (hasta 200 elementos con hijos) → *Mitigación:* una sola transacción acotada por el tope de 200.

## Migration Plan

1. Desplegar el backend con el endpoint y el parser (aditivo; ningún contrato existente cambia).
2. Desplegar el frontend con el botón y los toasts.
3. Rollback: retirar el botón y/o el endpoint; no hay migraciones de base de datos ni datos persistidos que revertir.
