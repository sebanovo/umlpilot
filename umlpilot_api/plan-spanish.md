# Proyecto: Collaborative UML Modeler with AI Assistance

## 1. Descripción General
Desarrollo de una aplicación web de modelado UML colaborativo, similar a **Enterprise Architect**, pero con capacidades avanzadas de **Inteligencia Artificial** y **colaboración en tiempo real**. La herramienta permitirá diseñar software a nivel conceptual y lógico, generar código backend completo (SpringBoot) y ofrecer una experiencia de desarrollo asistida por IA.

## 2. Objetivos del Sistema
- Proveer un entorno de diseño UML basado en la especificación **UML 2.5.1**.
- Facilitar el diseño colaborativo entre múltiples usuarios en tiempo real.
- Incorporar **asistentes de IA** para:
  - Generación automática de código backend (SpringBoot).
  - Normalización, mapeo y optimización de modelos de datos.
  - Edición y creación de diagramas mediante comandos de voz y texto.
- Ofrecer una **aplicación móvil** (Flutter) para consumo del backend generado, con capacidad de operar sin conexión a internet (IA local en el dispositivo).

## 3. Innovaciones Clave
1. **IA Generativa**: Generación de código backend completo a partir de diagramas de clases.
2. **IA Asistida**: Asistente virtual para guiar al usuario en el uso de los endpoints generados.
3. **IA Local (Offline)**: Capacidad de ejecutar modelos de IA en el dispositivo móvil sin necesidad de conexión a internet.
4. **Diseño Colaborativo en Tiempo Real**: Edición simultánea de diagramas por múltiples usuarios.
5. **IA Mediante Voz**: Comandos de voz para editar diagramas y controlar la interfaz.

## 4. Alcance Funcional

### 4.1. Modelado UML
- **Diagramas soportados**:
  - Diagrama de clases (conceptual y lógico).
  - Relaciones: herencia, asociación, agregación, composición, dependencia.
  - Multiplicidades y restricciones.
- **Interacción**:
  - Edición manual mediante interfaz gráfica.
  - Edición asistida por IA (texto y voz).
  - Actualización automática de la vista (sin recarga de página).

### 4.2. Generación de Código Backend
- **Tecnología**: SpringBoot.
- **Salida**: Archivo `.zip` con el proyecto completo listo para ejecutar.
- **Incluye**:
  - Capas: Controller, Service, Repository.
  - Entidades JPA mapeadas desde el diagrama de clases.
  - Endpoints REST básicos (CRUD).
  - Documentación Swagger/OpenAPI.z

### 4.3. Asistente IA para Backend
- Guía interactiva para entender y probar los endpoints generados.
- Sugerencias de pruebas con **Postman**.
- Explicación de la lógica de negocio implementada.

### 4.4. Importación/Exportación
- **Exportar** modelos a formato **XMI** (compatible con Enterprise Architect).
- **Importar** modelos desde XMI (futuro).

### 4.5. Aplicación Móvil (Flutter)
- **Frontend dinámico**: Se adapta al backend generado.
- **Funcionamiento offline**: IA local para procesamiento de datos sin internet.
- **Consumo de API**: Conexión al backend generado (SpringBoot).

## 5. Stack Tecnológico

| Componente               | Tecnología                        |
| ------------------------ | --------------------------------- |
| Backend Principal        | A definir (Node.js, Python, Java) |
| Frontend Web             | A definir (React, Angular, Vue)   |
| Base de Datos            | PostgreSQL                        |
| Backend Generado         | SpringBoot                        |
| Frontend Móvil           | Flutter                           |
| Comunicación Tiempo Real | WebSockets / Socket.io            |
| IA                       | Modelos locales y en la nube      |

## 6. Requerimientos No Funcionales
- **Rendimiento**: Actualización en tiempo real con latencia < 500ms.
- **Escalabilidad**: Soporte para múltiples usuarios simultáneos.
- **Usabilidad**: Interfaz intuitiva con soporte para comandos de voz.
- **Seguridad**: Autenticación y autorización de usuarios.
- **Offline**: Funcionalidad básica en la app móvil sin conexión.

## 7. Entregables

### 7.1. Documentación
- Manual de usuario.
- Guía de instalación y despliegue.
- Diagramas de arquitectura y base de datos.
- Especificación de la API generada.

### 7.2. Software
- Código fuente del sistema principal.
- Código fuente del backend generado (ejemplo).
- Código fuente de la aplicación móvil.
- Scripts de base de datos.

### 7.3. Demostración
- Video demostrativo (máx. 5 min).
- Presentación de las innovaciones implementadas.
## 9. Consideraciones Finales
- La herramienta debe ser autónoma y funcional en el entorno del usuario.
- El código generado debe ser limpio, documentado y seguir buenas prácticas.
- La IA debe ser un asistente útil, no un reemplazo del diseñador.

---
**Fecha de Entrega:** 23/10/2026  
**Modalidad:** Grupal (equipos de hasta 4 integrantes)  
**Forma de Entrega:** Repositorio GitHub con todo el código y documentación.


## 10.BASE DE DATOS
```sql
-- ============================================================
-- 1. TABLAS PRINCIPALES
-- ============================================================

-- Tabla: usuario
CREATE TABLE usuario (
    id_usuario VARCHAR(36) PRIMARY KEY,
    nombre_usuario VARCHAR(50) UNIQUE NOT NULL,
    correo VARCHAR(100) UNIQUE NOT NULL,
    contrasena_hash VARCHAR(255) NOT NULL,
    nombre_completo VARCHAR(100),
    avatar_url VARCHAR(255),
    idioma_preferido VARCHAR(10),
    tema VARCHAR(20) DEFAULT 'claro',
    esta_activo BOOLEAN DEFAULT TRUE,
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ultimo_acceso TIMESTAMP
);

-- Tabla: proyecto
CREATE TABLE proyecto (
    id_proyecto VARCHAR(36) PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT,
    id_creador VARCHAR(36) NOT NULL,
    estado VARCHAR(20) DEFAULT 'activo', -- activo, archivado, eliminado
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_creador) REFERENCES usuario(id_usuario)
);

-- Tabla: colaborador (proyecto_colaborador)
CREATE TABLE colaborador (
    id_proyecto VARCHAR(36) NOT NULL,
    id_usuario VARCHAR(36) NOT NULL,
    rol VARCHAR(20) NOT NULL, -- propietario, editor, visualizador
    permisos JSON,
    fecha_invitacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_aceptacion TIMESTAMP,
    estado_invitacion VARCHAR(20) DEFAULT 'pendiente', -- pendiente, aceptada, rechazada
    PRIMARY KEY (id_proyecto, id_usuario),
    FOREIGN KEY (id_proyecto) REFERENCES proyecto(id_proyecto),
    FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)
);

-- Tabla: sesion_usuario
CREATE TABLE sesion_usuario (
    id_sesion VARCHAR(36) PRIMARY KEY,
    id_usuario VARCHAR(36) NOT NULL,
    token VARCHAR(255) UNIQUE NOT NULL,
    ip_origen VARCHAR(45),
    user_agent TEXT,
    fecha_inicio TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_expiracion TIMESTAMP,
    esta_activa BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)
);

-- ============================================================
-- 2. ELEMENTOS UML (ESTRUCTURA DE ARBOL)
-- ============================================================

-- Tabla: elemento (clase base abstracta)
CREATE TABLE elemento (
    id_elemento VARCHAR(36) PRIMARY KEY,
    id_diagrama VARCHAR(36),
    id_padre VARCHAR(36), -- Relacion recursiva para estructura de arbol
    id_creador VARCHAR(36) NOT NULL,
    nombre VARCHAR(100),
    visibilidad VARCHAR(10), -- public, private, protected, package
    estereotipo VARCHAR(50),
    tipo_elemento VARCHAR(30) NOT NULL, -- paquete, clase, interfaz, enumeracion, data_type, primitive_type, note
    posicion_x INTEGER DEFAULT 0,
    posicion_y INTEGER DEFAULT 0,
    ancho INTEGER DEFAULT 100,
    alto INTEGER DEFAULT 80,
    propiedades JSON,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_diagrama) REFERENCES diagrama(id_diagrama),
    FOREIGN KEY (id_padre) REFERENCES elemento(id_elemento),
    FOREIGN KEY (id_creador) REFERENCES usuario(id_usuario)
);

-- Tabla: paquete (hereda de elemento)
CREATE TABLE paquete (
    id_elemento VARCHAR(36) PRIMARY KEY,
    namespace VARCHAR(255),
    descripcion TEXT,
    FOREIGN KEY (id_elemento) REFERENCES elemento(id_elemento) ON DELETE CASCADE
);

-- Tabla: clase (hereda de elemento)
CREATE TABLE clase (
    id_elemento VARCHAR(36) PRIMARY KEY,
    es_abstracta BOOLEAN DEFAULT FALSE,
    es_final BOOLEAN DEFAULT FALSE,
    nombre_base VARCHAR(100),
    es_activa BOOLEAN DEFAULT FALSE,
    es_plantilla BOOLEAN DEFAULT FALSE,
    parametros_plantilla JSON,
    FOREIGN KEY (id_elemento) REFERENCES elemento(id_elemento) ON DELETE CASCADE
);

-- Tabla: interfaz (hereda de elemento)
CREATE TABLE interfaz (
    id_elemento VARCHAR(36) PRIMARY KEY,
    version VARCHAR(20),
    FOREIGN KEY (id_elemento) REFERENCES elemento(id_elemento) ON DELETE CASCADE
);

-- Tabla: enumeracion (hereda de elemento)
CREATE TABLE enumeracion (
    id_elemento VARCHAR(36) PRIMARY KEY,
    tipo_base VARCHAR(50),
    FOREIGN KEY (id_elemento) REFERENCES elemento(id_elemento) ON DELETE CASCADE
);

-- Tabla: data_type (hereda de elemento)
CREATE TABLE data_type (
    id_elemento VARCHAR(36) PRIMARY KEY,
    tipo_dato_primitivo VARCHAR(50),
    es_inmutable BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (id_elemento) REFERENCES elemento(id_elemento) ON DELETE CASCADE
);

-- Tabla: primitive_type (hereda de elemento)
CREATE TABLE primitive_type (
    id_elemento VARCHAR(36) PRIMARY KEY,
    tipo VARCHAR(50),
    tamanio INTEGER,
    rango_min VARCHAR(50),
    rango_max VARCHAR(50),
    FOREIGN KEY (id_elemento) REFERENCES elemento(id_elemento) ON DELETE CASCADE
);

-- Tabla: note (hereda de elemento)
CREATE TABLE note (
    id_elemento VARCHAR(36) PRIMARY KEY,
    texto TEXT,
    color_fondo VARCHAR(20),
    color_texto VARCHAR(20),
    border_style VARCHAR(20),
    FOREIGN KEY (id_elemento) REFERENCES elemento(id_elemento) ON DELETE CASCADE
);

-- ============================================================
-- 3. ESTRUCTURA INTERNA DE ELEMENTOS
-- ============================================================

-- Tabla: atributo
CREATE TABLE atributo (
    id_atributo VARCHAR(36) PRIMARY KEY,
    id_elemento VARCHAR(36) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    tipo_dato VARCHAR(50) NOT NULL,
    visibilidad VARCHAR(10) DEFAULT 'private', -- public, private, protected, package
    valor_por_defecto VARCHAR(255),
    es_estatico BOOLEAN DEFAULT FALSE,
    es_final BOOLEAN DEFAULT FALSE,
    es_transitorio BOOLEAN DEFAULT FALSE,
    es_volatile BOOLEAN DEFAULT FALSE,
    multiplicidad VARCHAR(20),
    orden INTEGER DEFAULT 0,
    propiedades JSON,
    FOREIGN KEY (id_elemento) REFERENCES elemento(id_elemento) ON DELETE CASCADE
);

-- Tabla: metodo
CREATE TABLE metodo (
    id_metodo VARCHAR(36) PRIMARY KEY,
    id_elemento VARCHAR(36) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    tipo_retorno VARCHAR(50) NOT NULL,
    visibilidad VARCHAR(10) DEFAULT 'public',
    es_estatico BOOLEAN DEFAULT FALSE,
    es_abstracto BOOLEAN DEFAULT FALSE,
    es_final BOOLEAN DEFAULT FALSE,
    es_constructor BOOLEAN DEFAULT FALSE,
    es_sincronizado BOOLEAN DEFAULT FALSE,
    es_native BOOLEAN DEFAULT FALSE,
    cuerpo TEXT,
    orden INTEGER DEFAULT 0,
    propiedades JSON,
    FOREIGN KEY (id_elemento) REFERENCES elemento(id_elemento) ON DELETE CASCADE
);

-- Tabla: parametro
CREATE TABLE parametro (
    id_parametro VARCHAR(36) PRIMARY KEY,
    id_metodo VARCHAR(36) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    tipo_dato VARCHAR(50) NOT NULL,
    orden INTEGER DEFAULT 0,
    es_varargs BOOLEAN DEFAULT FALSE,
    es_final BOOLEAN DEFAULT FALSE,
    valor_por_defecto VARCHAR(255),
    FOREIGN KEY (id_metodo) REFERENCES metodo(id_metodo) ON DELETE CASCADE
);

-- Tabla: literal (para enumeraciones)
CREATE TABLE literal (
    id_literal VARCHAR(36) PRIMARY KEY,
    id_elemento VARCHAR(36) NOT NULL, -- referencia a enumeracion
    nombre VARCHAR(100) NOT NULL,
    valor VARCHAR(255),
    orden INTEGER DEFAULT 0,
    FOREIGN KEY (id_elemento) REFERENCES elemento(id_elemento) ON DELETE CASCADE
);

-- ============================================================
-- 4. RELACIONES UML
-- ============================================================

-- Tabla: relacion
CREATE TABLE relacion (
    id_relacion VARCHAR(36) PRIMARY KEY,
    id_diagrama VARCHAR(36) NOT NULL,
    id_elemento_origen VARCHAR(36) NOT NULL,
    id_elemento_destino VARCHAR(36) NOT NULL,
    tipo_relacion VARCHAR(30) NOT NULL, -- asociacion, herencia, dependencia, agregacion, composicion, realizacion, template_binding
    nombre VARCHAR(100),
    direccion VARCHAR(10), -- bidireccional, unidireccional
    es_vinculacion_plantilla BOOLEAN DEFAULT FALSE,
    clase_plantilla VARCHAR(100),
    propiedades JSON,
    FOREIGN KEY (id_diagrama) REFERENCES diagrama(id_diagrama),
    FOREIGN KEY (id_elemento_origen) REFERENCES elemento(id_elemento),
    FOREIGN KEY (id_elemento_destino) REFERENCES elemento(id_elemento)
);

-- Tabla: multiplicidad
CREATE TABLE multiplicidad (
    id_multiplicidad VARCHAR(36) PRIMARY KEY,
    id_relacion VARCHAR(36) NOT NULL,
    extremo VARCHAR(10) NOT NULL, -- origen, destino
    min INTEGER DEFAULT 0,
    max INTEGER DEFAULT 1,
    es_ordenada BOOLEAN DEFAULT FALSE,
    es_unica BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (id_relacion) REFERENCES relacion(id_relacion) ON DELETE CASCADE
);

-- Tabla: vinculacion_plantilla (para Template Binding)
CREATE TABLE vinculacion_plantilla (
    id_vinculacion VARCHAR(36) PRIMARY KEY,
    id_relacion VARCHAR(36) NOT NULL,
    parametro_formal VARCHAR(100) NOT NULL,
    argumento_real VARCHAR(100) NOT NULL,
    posicion INTEGER DEFAULT 0,
    FOREIGN KEY (id_relacion) REFERENCES relacion(id_relacion) ON DELETE CASCADE
);

-- Tabla: asociacion_clase (para Association Class)
CREATE TABLE asociacion_clase (
    id_asociacion_clase VARCHAR(36) PRIMARY KEY,
    id_relacion VARCHAR(36) NOT NULL,
    id_clase_asociacion VARCHAR(36) NOT NULL,
    nombre VARCHAR(100),
    FOREIGN KEY (id_relacion) REFERENCES relacion(id_relacion) ON DELETE CASCADE,
    FOREIGN KEY (id_clase_asociacion) REFERENCES elemento(id_elemento)
);

-- ============================================================
-- 5. DIAGRAMAS
-- ============================================================

-- Tabla: diagrama
CREATE TABLE diagrama (
    id_diagrama VARCHAR(36) PRIMARY KEY,
    id_proyecto VARCHAR(36) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    tipo VARCHAR(30) NOT NULL, -- clase, secuencia, caso_uso, actividad, estado, etc.
    descripcion TEXT,
    canvas_data JSON,
    version INTEGER DEFAULT 1,
    esta_bloqueado BOOLEAN DEFAULT FALSE,
    id_usuario_bloqueo VARCHAR(36),
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_proyecto) REFERENCES proyecto(id_proyecto),
    FOREIGN KEY (id_usuario_bloqueo) REFERENCES usuario(id_usuario)
);

-- ============================================================
-- 6. HISTORIAL Y AUDITORIA
-- ============================================================

-- Tabla: historial_cambio
CREATE TABLE historial_cambio (
    id_cambio VARCHAR(36) PRIMARY KEY,
    id_diagrama VARCHAR(36) NOT NULL,
    id_usuario VARCHAR(36) NOT NULL,
    accion VARCHAR(30) NOT NULL, -- crear, editar, eliminar, mover
    id_objeto_afectado VARCHAR(36),
    tipo_objeto VARCHAR(20), -- elemento, atributo, metodo, relacion
    datos_antes JSON,
    datos_despues JSON,
    fecha_cambio TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_diagrama) REFERENCES diagrama(id_diagrama),
    FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)
);

-- ============================================================
-- 7. INTELIGENCIA ARTIFICIAL (IA)
-- ============================================================

-- Tabla: comando_ia
CREATE TABLE comando_ia (
    id_comando VARCHAR(36) PRIMARY KEY,
    id_usuario VARCHAR(36) NOT NULL,
    id_diagrama VARCHAR(36) NOT NULL,
    tipo_comando VARCHAR(20), -- texto, voz
    comando_original TEXT,
    comando_interpretado JSON,
    accion_ejecutada JSON,
    exito BOOLEAN DEFAULT FALSE,
    mensaje_error TEXT,
    tiempo_procesamiento INTEGER,
    fecha_ejecucion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario),
    FOREIGN KEY (id_diagrama) REFERENCES diagrama(id_diagrama)
);

-- Tabla: conversacion_ia
CREATE TABLE conversacion_ia (
    id_conversacion VARCHAR(36) PRIMARY KEY,
    id_usuario VARCHAR(36) NOT NULL,
    id_diagrama VARCHAR(36) NOT NULL,
    tipo VARCHAR(20), -- asistente, generacion
    fecha_inicio TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_fin TIMESTAMP,
    FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario),
    FOREIGN KEY (id_diagrama) REFERENCES diagrama(id_diagrama)
);

-- Tabla: mensaje_ia
CREATE TABLE mensaje_ia (
    id_mensaje VARCHAR(36) PRIMARY KEY,
    id_conversacion VARCHAR(36) NOT NULL,
    remitente VARCHAR(10) NOT NULL, -- usuario, ia
    mensaje TEXT,
    metadata JSON,
    fecha_envio TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_conversacion) REFERENCES conversacion_ia(id_conversacion) ON DELETE CASCADE
);

-- ============================================================
-- 8. GENERACION DE CODIGO
-- ============================================================

-- Tabla: generacion_codigo
CREATE TABLE generacion_codigo (
    id_generacion VARCHAR(36) PRIMARY KEY,
    id_diagrama VARCHAR(36) NOT NULL,
    id_usuario VARCHAR(36) NOT NULL,
    lenguaje VARCHAR(20) NOT NULL, -- springboot
    configuracion JSON,
    archivo_zip_url VARCHAR(255),
    tamanio_zip BIGINT,
    estado VARCHAR(20) DEFAULT 'generando', -- generando, completado, error
    mensaje_error TEXT,
    fecha_solicitud TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_completado TIMESTAMP,
    FOREIGN KEY (id_diagrama) REFERENCES diagrama(id_diagrama),
    FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)
);

-- ============================================================
-- 9. EXPORTACION E IMPORTACION XMI
-- ============================================================

-- Tabla: exportacion_xmi
CREATE TABLE exportacion_xmi (
    id_exportacion VARCHAR(36) PRIMARY KEY,
    id_diagrama VARCHAR(36) NOT NULL,
    id_usuario VARCHAR(36) NOT NULL,
    version_xmi VARCHAR(20),
    archivo_url VARCHAR(255),
    tamanio BIGINT,
    fecha_exportacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_diagrama) REFERENCES diagrama(id_diagrama),
    FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)
);

-- Tabla: importacion_xmi
CREATE TABLE importacion_xmi (
    id_importacion VARCHAR(36) PRIMARY KEY,
    id_proyecto VARCHAR(36) NOT NULL,
    id_usuario VARCHAR(36) NOT NULL,
    nombre_archivo VARCHAR(255),
    tamanio BIGINT,
    estado VARCHAR(20) DEFAULT 'procesando', -- procesando, completado, error
    mensaje_error TEXT,
    fecha_importacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_proyecto) REFERENCES proyecto(id_proyecto),
    FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)
);

-- ============================================================
-- 10. INDICES PARA OPTIMIZACION
-- ============================================================

-- Indices para busquedas frecuentes
CREATE INDEX idx_elemento_diagrama ON elemento(id_diagrama);
CREATE INDEX idx_elemento_padre ON elemento(id_padre);
CREATE INDEX idx_elemento_tipo ON elemento(tipo_elemento);
CREATE INDEX idx_atributo_elemento ON atributo(id_elemento);
CREATE INDEX idx_metodo_elemento ON metodo(id_elemento);
CREATE INDEX idx_parametro_metodo ON parametro(id_metodo);
CREATE INDEX idx_literal_elemento ON literal(id_elemento);
CREATE INDEX idx_relacion_diagrama ON relacion(id_diagrama);
CREATE INDEX idx_relacion_origen ON relacion(id_elemento_origen);
CREATE INDEX idx_relacion_destino ON relacion(id_elemento_destino);
CREATE INDEX idx_multiplicidad_relacion ON multiplicidad(id_relacion);
CREATE INDEX idx_vinculacion_relacion ON vinculacion_plantilla(id_relacion);
CREATE INDEX idx_diagrama_proyecto ON diagrama(id_proyecto);
CREATE INDEX idx_historial_diagrama ON historial_cambio(id_diagrama);
CREATE INDEX idx_historial_usuario ON historial_cambio(id_usuario);
CREATE INDEX idx_comando_usuario ON comando_ia(id_usuario);
CREATE INDEX idx_comando_diagrama ON comando_ia(id_diagrama);
CREATE INDEX idx_conversacion_usuario ON conversacion_ia(id_usuario);
CREATE INDEX idx_mensaje_conversacion ON mensaje_ia(id_conversacion);
CREATE INDEX idx_generacion_diagrama ON generacion_codigo(id_diagrama);
CREATE INDEX idx_exportacion_diagrama ON exportacion_xmi(id_diagrama);
CREATE INDEX idx_importacion_proyecto ON importacion_xmi(id_proyecto);

-- ============================================================
-- 11. VISTAS UTILES
-- ============================================================

-- Vista: elementos_completos (con todos los detalles)
CREATE VIEW vista_elementos_completos AS
SELECT 
    e.id_elemento,
    e.nombre,
    e.tipo_elemento,
    e.visibilidad,
    e.estereotipo,
    e.propiedades,
    e.id_diagrama,
    d.nombre AS nombre_diagrama,
    d.tipo AS tipo_diagrama,
    e.id_padre,
    p.nombre AS nombre_padre,
    e.id_creador,
    u.nombre_usuario AS creador,
    e.fecha_creacion,
    e.fecha_actualizacion,
    -- Campos especificos segun tipo
    CASE 
        WHEN e.tipo_elemento = 'clase' THEN c.es_abstracta
        ELSE NULL
    END AS es_abstracta,
    CASE 
        WHEN e.tipo_elemento = 'clase' THEN c.es_final
        ELSE NULL
    END AS es_final,
    CASE 
        WHEN e.tipo_elemento = 'interfaz' THEN i.version
        ELSE NULL
    END AS version_interfaz,
    CASE 
        WHEN e.tipo_elemento = 'enumeracion' THEN en.tipo_base
        ELSE NULL
    END AS tipo_base_enumeracion,
    CASE 
        WHEN e.tipo_elemento = 'data_type' THEN dt.es_inmutable
        ELSE NULL
    END AS es_inmutable,
    CASE 
        WHEN e.tipo_elemento = 'primitive_type' THEN pt.tipo
        ELSE NULL
    END AS tipo_primitivo,
    CASE 
        WHEN e.tipo_elemento = 'note' THEN n.texto
        ELSE NULL
    END AS texto_nota
FROM elemento e
LEFT JOIN diagrama d ON e.id_diagrama = d.id_diagrama
LEFT JOIN elemento p ON e.id_padre = p.id_elemento
LEFT JOIN usuario u ON e.id_creador = u.id_usuario
LEFT JOIN clase c ON e.id_elemento = c.id_elemento AND e.tipo_elemento = 'clase'
LEFT JOIN interfaz i ON e.id_elemento = i.id_elemento AND e.tipo_elemento = 'interfaz'
LEFT JOIN enumeracion en ON e.id_elemento = en.id_elemento AND e.tipo_elemento = 'enumeracion'
LEFT JOIN data_type dt ON e.id_elemento = dt.id_elemento AND e.tipo_elemento = 'data_type'
LEFT JOIN primitive_type pt ON e.id_elemento = pt.id_elemento AND e.tipo_elemento = 'primitive_type'
LEFT JOIN note n ON e.id_elemento = n.id_elemento AND e.tipo_elemento = 'note';

-- Vista: proyecto_con_colaboradores
CREATE VIEW vista_proyecto_colaboradores AS
SELECT 
    p.id_proyecto,
    p.nombre AS nombre_proyecto,
    p.descripcion,
    p.estado,
    p.id_creador,
    c_usr.nombre_usuario AS nombre_creador,
    COUNT(DISTINCT col.id_usuario) AS total_colaboradores,
    json_agg(
        json_build_object(
            'id_usuario', col.id_usuario,
            'nombre_usuario', u.nombre_usuario,
            'rol', col.rol,
            'estado_invitacion', col.estado_invitacion
        )
    ) AS colaboradores
FROM proyecto p
LEFT JOIN usuario c_usr ON p.id_creador = c_usr.id_usuario
LEFT JOIN colaborador col ON p.id_proyecto = col.id_proyecto
LEFT JOIN usuario u ON col.id_usuario = u.id_usuario
GROUP BY p.id_proyecto, p.nombre, p.descripcion, p.estado, p.id_creador, c_usr.nombre_usuario;

-- Vista: diagrama_con_elementos
CREATE VIEW vista_diagrama_elementos AS
SELECT 
    d.id_diagrama,
    d.nombre AS nombre_diagrama,
    d.tipo AS tipo_diagrama,
    d.id_proyecto,
    pr.nombre AS nombre_proyecto,
    COUNT(DISTINCT e.id_elemento) AS total_elementos,
    COUNT(DISTINCT r.id_relacion) AS total_relaciones
FROM diagrama d
LEFT JOIN proyecto pr ON d.id_proyecto = pr.id_proyecto
LEFT JOIN elemento e ON d.id_diagrama = e.id_diagrama
LEFT JOIN relacion r ON d.id_diagrama = r.id_diagrama
GROUP BY d.id_diagrama, d.nombre, d.tipo, d.id_proyecto, pr.nombre;
```