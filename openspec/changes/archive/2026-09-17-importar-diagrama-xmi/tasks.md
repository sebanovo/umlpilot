## 1. Modelo intermedio y parser XMI

- [x] 1.1 Crear `application/dto/ImportedDiagram` (records de elemento con atributos/métodos/parámetros/literales, relación con extremos y multiplicidades, y posiciones) y el puerto `application/port/out/XmiDocumentParser`, verificando que compila y que los records construyen un diagrama de ejemplo en un test
- [x] 1.2 Implementar `infrastructure/xmi/DomXmiParser` con JAXP/DOM en modo seguro (`FEATURE_SECURE_PROCESSING`, `disallow-doctype-decl`, sin entidades externas) y validar el XML: verificar con tests que un XML mal formado y un intento de XXE (`<!DOCTYPE ... SYSTEM>`) fallan con `DomainException`
- [x] 1.3 Validar versión y namespace XMI 2.5.1 en el parser, verificando con tests el mensaje exacto `"Versión XMI no soportada. Se requiere XMI 2.5.1. Versión detectada: {version}"` para versiones 2.1/2.4 y para namespace incorrecto
- [x] 1.4 Procesar solo los hijos `packagedElement` como contenido del diagrama (clase/interfaz/enumeración/nota/`DataType` y las relaciones permitidas) e **ignorar** envoltorios y perfiles de la herramienta (`profileApplication`, `primitivetypes`, `profiles`, `diagrams`, `xmi:Extension`, `Profile`, `Stereotype`, `Extension`, `PrimitiveType`), verificando con tests el mensaje `"Tipo de diagrama no soportado..."` para `uml:Actor`/`uml:UseCase`/`uml:Lifeline`, el mensaje `"Elemento no soportado: {tipo}..."` para `uml:Component`/`uml:Artifact`, que `uml:DataType` se importa como clase y que un XMI de EA con perfil propio se importa sin warnings
- [x] 1.5 Mapear elementos, atributos (`uml:Property`), métodos (`uml:Operation`), parámetros (`uml:Parameter`) y literales (`uml:EnumerationLiteral`) con su visibilidad, leer posiciones/tamaños de `dc:bounds` e importar solo los elementos dibujados en el diagrama (omitiendo con warning los que solo existen en el modelo cuando el archivo trae formas), verificando con tests que un XMI de varias clases con atributos, métodos, parámetros y bounds produce el modelo intermedio esperado
- [x] 1.6 Mapear relaciones (`association`, `aggregation="shared"`, `aggregation="composite"`, `generalization`, `association_class` y `realization`) preservando multiplicidades (`lowerValue`/`upperValue` → texto) y verificando con tests cada tipo y el caso de referencia colgante (se omite con warning)

## 2. Caso de uso de importación

- [x] 2.1 Implementar `application/service/XmiImportService` (`@Transactional`) que persiste el `ImportedDiagram` reutilizando `ElementService`/`RelationshipService`, y verificar con un test que un diagrama con contenido previo no pierde elementos ni relaciones (importación aditiva)
- [x] 2.2 Implementar el renombrado por colisión (sin distinguir mayúsculas, también dentro del mismo lote con sufijo `_1`, `_2`) y los warnings asociados, verificando con un test que `Cliente` existente + importado produce `Cliente_1` y un warning
- [x] 2.3 Aplicar posiciones: conservar `dc:bounds` cuando existe y, si falta, colocar en cuadrícula por debajo del contenido existente, verificando con tests ambos caminos
- [x] 2.4 Aplicar el tope de 200 elementos rechazando con error descriptivo, verificado con un test que un XMI de 201 elementos lanza `DomainException`
- [x] 2.5 Emitir **un único** evento tras la importación con `type: "import"` (reutilizando `DiagramEventPublisher`), verificando con un test que el publicador recibe exactamente una llamada

## 3. Endpoint de importación

- [x] 3.1 Exponer `POST /api/v1/projects/{projectId}/diagrams/{diagramId}/import/xmi` (`multipart/form-data`, campo `file`) devolviendo el `ImportXmiResult`, y verificar con tests de MockMvc los casos 200, 400 (sin archivo, validación), 404 (diagrama inexistente) y 401 (sin token)
- [x] 3.2 Manejar `MaxUploadSizeExceededException` devolviendo 400 con el límite de 10 MB, verificando con un test que el handler responde el mensaje de tamaño
- [x] 3.3 Definir el DTO de respuesta `ImportXmiResult` (conteos de elementos/atributos/métodos/parámetros/literales/relaciones + warnings) y verificar con un test el JSON devuelto

## 4. Frontend

- [x] 4.1 Añadir `xmiImportApi.import(projectId, diagramId, file)` en `shared/api/client.ts` usando `authUpload` (multipart), verificando que compila y que un error del backend se propaga como `Error` con su mensaje
- [x] 4.2 Añadir el botón "Importar XMI" en la barra del editor `DiagramEditorPage` con `<input type="file" accept=".xmi,.xml">`, estado de carga que bloquea doble importación y reinicio del input
- [x] 4.3 Mostrar los toasts de resultado (éxito con resumen, advertencia con warnings, error descriptivo)
- [x] 4.4 Refrescar el lienzo tras la importación (rebuild para el solicitante y manejo del evento `diagram_changed` para los colaboradores)
- [x] 4.5 Verificar que el frontend compila y tipa: `pnpm run build` y `npx tsc -b` sin errores

## 5. Verificación integral

- [x] 5.1 Probar el round-trip: un XMI con la forma del exportador de UmlPilot se parsea completo (`XmiRoundTripTest`) y el caso de uso lo añade sin tocar el contenido existente (`XmiImportServiceTest`); además se verificó el parseo del XMI real de EA reportado por el usuario (3 clases + 1 asociación, 0 warnings)
- [x] 5.2 Verificar el rechazo 400 con cada mensaje (versión, tipo de diagrama, elemento no soportado, XML inválido, límites) y que el diagrama no se modifica tras un rechazo
- [x] 5.3 Ejecutar la batería de tests del backend (`mvn test` de las clases afectadas) y confirmar que no hay regresiones
