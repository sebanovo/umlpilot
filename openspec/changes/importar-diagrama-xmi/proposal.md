## Why

UmlPilot ya **exporta** diagramas a XMI 2.5.1, pero no puede **reimportarlos**: un modelo hecho en Enterprise Architect, Visual Paradigm o MagicDraw (o incluso un XMI exportado por el propio UmlPilot) hay que redibujarlo a mano. Importar XMI cierra el ciclo de interoperabilidad y ahorra horas de modelado manual.

## What Changes

- Nuevo endpoint multipart `POST /api/v1/projects/{projectId}/diagrams/{diagramId}/import/xmi` que valida, parsea y **añade** los elementos del archivo al diagrama actual.
- **Validaciones estrictas** (400 descriptivo antes de parsear): versión XMI 2.5.1 (atributo `xmi:version` + namespace `http://www.omg.org/spec/XMI/20131001`), solo diagramas de clases, y allow-list de elementos y relaciones permitidos.
- **Importación aditiva y nunca destructiva**: no borra ni modifica lo existente; genera UUID nuevos (ignora los `xmi:id`); ante colisión de nombres renombra con sufijo numérico (`Cliente_1`) y lo reporta como warning.
- **Posiciones preservadas** desde `dc:bounds` del XMI cuando existen; si faltan, se coloca en cuadrícula debajo del contenido actual.
- Mapeo UML → UmlPilot: clases/interfaces/enums/notas, atributos, métodos, parámetros, literales, y relaciones (asociación, agregación, composición, generalización, clase asociación y realización), preservando multiplicidades (`lowerValue`/`upperValue`) y visibilidad.
- **Un solo evento WebSocket** `diagram_changed` (payload `type: "import"`) para que todos los colaboradores refresquen el diagrama.
- Resultado `ImportXmiResult` con el resumen de lo añadido y los warnings.
- Frontend: botón **"Importar XMI"** en la barra del editor, selección de archivo y **toasts** de éxito/error.
- Límites: **200 elementos** por importación y **10 MB** por archivo.

## Capabilities

### New Capabilities
- `diagram-import/xmi`: endpoint de importación XMI 2.5.1, validaciones estrictas, mapeo UML→UmlPilot, importación aditiva con renombrado y warnings, y refresco en tiempo real de los colaboradores.
- `frontend/xmi-import`: acción de importar XMI desde el editor, subida del archivo y notificaciones (toasts) de éxito y de error descriptivo.

### Modified Capabilities
<!-- Ninguna: el evento `diagram_changed` ya existe y se reutiliza; el refresco se documenta dentro de `diagram-import/xmi`. -->

## Impact

- **Backend (`umlpilot_api`)**:
  - Nuevo adaptador de entrada (multipart) en `infrastructure/adapter/in/web`.
  - Nuevo caso de uso en `application` (validación + parseo + persistencia aditiva, `@Transactional`) y DTOs de resultado/warnings.
  - Parser XMI basado en el XML del JDK (JAXP/DOM) con *secure processing* (sin DTD ni entidades externas, para evitar XXE).
  - Reutiliza `ElementService`/`RelationshipService` (mismos casos de uso que usan las tools de IA y el build con IA); sin cambios de esquema de base de datos.
  - Publica `diagram_changed` con `DiagramEventPublisher`.
- **Frontend (`umlpilot_web`)**:
  - `shared/api/client.ts`: llamada de importación con `authUpload` (multipart).
  - Barra del editor (`DiagramEditorPage`): botón "Importar XMI" + `<input type="file" accept=".xmi,.xml">`.
  - `useToast` para éxito/errores descriptivos; refresco del lienzo al recibir `diagram_changed`.
- **Contratos**: `200` con `ImportXmiResult`; `400` (versión XMI no soportada, tipo de diagrama no soportado, elemento no soportado, XML inválido, topes excedidos); `404` (diagrama inexistente); `401` sin token.
