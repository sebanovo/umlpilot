# AGENTS.md

## Arquitectura del Proyecto

Este proyecto sigue:
- **Backend**: Arquitectura Hexagonal.
- **Frontend**: Feature-Sliced Design (FSD).

## Referencias a Guías Detalladas

CRITICAL: When you encounter a file reference (e.g., @docs/architecture/backend-hexagonal.md), use your Read tool to load it on a need-to-know basis. They're relevant to the SPECIFIC task at hand.

Instructions:
- Do NOT preemptively load all references - use lazy loading based on actual need
- When loaded, treat content as mandatory instructions that override defaults

- **Backend**: Para reglas y ejemplos de la arquitectura hexagonal: `@docs/architecture/backend-hexagonal.md`
- **Frontend**: Para reglas y ejemplos de FSD y Clean Architecture: `@docs/architecture/frontend-fsd.md`
- **Base de Datos**: Para el esquema y convenciones de PostgreSQL: `@docs/architecture/database.md`

## Reglas Generales

- Java 21 + SpringBoot 3.3
- React 18 + TypeScript
- ...