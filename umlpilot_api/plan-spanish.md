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

