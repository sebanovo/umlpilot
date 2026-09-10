# AGENTS.md para Collaborative UML Modeler with AI

A continuación tienes el archivo `AGENTS.md` completo, listo para copiar en la raíz de tu proyecto. Define la arquitectura hexagonal para el backend SpringBoot y Feature-Sliced Design (FSD) con principios de Clean Architecture para el frontend React/TypeScript.

## Estructura Recomendada del Proyecto

```
collaborative-uml-modeler/
├── AGENTS.md                 # Este archivo
├── openspec/                 # Specs gestionados por OpenSpec
├── backend/                  # SpringBoot + Arquitectura Hexagonal
└── frontend/                 # React + TypeScript + FSD
```

---

## Contenido del AGENTS.md

```markdown
# AGENTS.md - Collaborative UML Modeler with AI

## Arquitectura del Proyecto

Este proyecto implementa una aplicación de modelado UML colaborativo con asistencia de IA.

- **Backend**: SpringBoot con Arquitectura Hexagonal (Ports & Adapters)
- **Frontend**: React + TypeScript con Feature-Sliced Design (FSD) y principios de Clean Architecture

---

## BACKEND - SpringBoot + Arquitectura Hexagonal

### Principio Fundamental

La Regla de Dependencia es sagrada: las dependencias solo apuntan hacia adentro.
El dominio NUNCA conoce Spring, JPA, ni ninguna infraestructura.

### Estructura de Paquetes (Hexagonal)

```
backend/src/main/java/com/umlmodeler/
├── domain/                          # Núcleo puro - SIN dependencias de framework
│   ├── model/                       # Entidades, Value Objects
│   │   ├── User.java
│   │   ├── Diagram.java
│   │   └── UserId.java
│   ├── repository/                  # Puertos de salida (interfaces)
│   │   └── UserRepository.java
│   ├── exception/
│   │   └── DomainException.java
│   └── service/                     # Servicios de dominio (lógica pura)
│       └── DiagramValidationService.java
│
├── application/                     # Casos de uso - orquesta dominio
│   ├── port/
│   │   ├── in/                      # Puertos de entrada (interfaces de casos de uso)
│   │   │   ├── CreateUserUseCase.java
│   │   │   └── GenerateCodeUseCase.java
│   │   └── out/                     # Puertos de salida adicionales
│   │       └── CodeGeneratorPort.java
│   ├── service/                     # Implementación de casos de uso
│   │   ├── CreateUserService.java
│   │   └── GenerateCodeService.java
│   └── dto/                         # DTOs de aplicación (Command/Query/Result)
│       ├── CreateUserCommand.java
│       └── UserResult.java
│
└── infrastructure/                  # Adaptadores - conoce el mundo exterior
    ├── adapter/
    │   ├── in/
    │   │   └── web/                 # Adaptador REST
    │   │       ├── UserController.java
    │   │       ├── UserRequest.java
    │   │       └── UserResponse.java
    │   └── out/
    │       └── persistence/         # Adaptador JPA
    │           ├── UserJpaEntity.java
    │           ├── UserJpaRepository.java
    │           ├── UserPersistenceAdapter.java
    │           └── UserMapper.java
    └── config/
        └── BeanConfiguration.java
```

### Reglas Críticas del Backend

1. **Domain NO importa Spring**: Cero anotaciones `@Service`, `@Component`, `@Entity` en el dominio
2. **Separación de entidades**: Las entidades JPA viven en infrastructure, NO en domain. Usa mappers para convertir entre ambas representaciones
3. **Repositorios como puertos**: El dominio define la interfaz (`UserRepository`), la infraestructura la implementa (`UserPersistenceAdapter`)
4. **@Transactional solo en application**: Los servicios de aplicación manejan transacciones, nunca el dominio
5. **DTOs separados del dominio**: Los contratos de API (Request/Response) viven en infrastructure, los Commands/Results en application

### Ejemplos de Código Backend

#### Domain - Entidad con lógica de negocio pura

```java
// domain/model/Diagram.java
package com.umlmodeler.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Diagram {
    private final DiagramId id;
    private String name;
    private final List<UmlClass> classes;
    private final List<UmlRelation> relations;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Diagram(DiagramId id, String name) {
        this.id = id;
        this.name = name;
        this.classes = new ArrayList<>();
        this.relations = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public static Diagram create(String name) {
        if (name == null || name.isBlank()) {
            throw new DomainException("Diagram name cannot be empty");
        }
        return new Diagram(DiagramId.generate(), name);
    }

    public void addClass(UmlClass umlClass) {
        if (classes.stream().anyMatch(c -> c.getName().equals(umlClass.getName()))) {
            throw new DomainException("A class with this name already exists");
        }
        this.classes.add(umlClass);
        this.updatedAt = LocalDateTime.now();
    }

    public void addRelation(UmlRelation relation) {
        boolean sourceExists = classes.stream()
            .anyMatch(c -> c.getId().equals(relation.getSourceId()));
        boolean targetExists = classes.stream()
            .anyMatch(c -> c.getId().equals(relation.getTargetId()));
        
        if (!sourceExists || !targetExists) {
            throw new DomainException("Both classes must exist before adding a relation");
        }
        this.relations.add(relation);
        this.updatedAt = LocalDateTime.now();
    }

    // Getters
    public DiagramId getId() { return id; }
    public String getName() { return name; }
    public List<UmlClass> getClasses() { return List.copyOf(classes); }
    public List<UmlRelation> getRelations() { return List.copyOf(relations); }
}
```

#### Domain - Value Object

```java
// domain/model/DiagramId.java
package com.umlmodeler.domain.model;

import java.util.Objects;
import java.util.UUID;

public record DiagramId(UUID value) {
    public DiagramId {
        Objects.requireNonNull(value, "DiagramId value cannot be null");
    }

    public static DiagramId generate() {
        return new DiagramId(UUID.randomUUID());
    }

    public static DiagramId from(String value) {
        return new DiagramId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
```

#### Domain - Puerto de Salida (Repository Interface)

```java
// domain/repository/DiagramRepository.java
package com.umlmodeler.domain.repository;

import com.umlmodeler.domain.model.Diagram;
import com.umlmodeler.domain.model.DiagramId;
import java.util.Optional;
import java.util.List;

public interface DiagramRepository {
    Diagram save(Diagram diagram);
    Optional<Diagram> findById(DiagramId id);
    List<Diagram> findByOwnerId(String ownerId);
    void delete(DiagramId id);
}
```

#### Application - Puerto de Entrada (Use Case Interface)

```java
// application/port/in/CreateDiagramUseCase.java
package com.umlmodeler.application.port.in;

import com.umlmodeler.application.dto.CreateDiagramCommand;
import com.umlmodeler.application.dto.DiagramResult;

public interface CreateDiagramUseCase {
    DiagramResult createDiagram(CreateDiagramCommand command);
}
```

#### Application - DTOs

```java
// application/dto/CreateDiagramCommand.java
package com.umlmodeler.application.dto;

public record CreateDiagramCommand(
    String name,
    String ownerId
) {}
```

```java
// application/dto/DiagramResult.java
package com.umlmodeler.application.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DiagramResult(
    String id,
    String name,
    String ownerId,
    List<ClassResult> classes,
    List<RelationResult> relations,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public record ClassResult(String id, String name, List<AttributeResult> attributes) {}
    public record AttributeResult(String name, String type, String visibility) {}
    public record RelationResult(String id, String type, String sourceId, String targetId) {}
}
```

#### Application - Implementación del Caso de Uso

```java
// application/service/CreateDiagramService.java
package com.umlmodeler.application.service;

import com.umlmodeler.application.dto.CreateDiagramCommand;
import com.umlmodeler.application.dto.DiagramResult;
import com.umlmodeler.application.port.in.CreateDiagramUseCase;
import com.umlmodeler.domain.model.Diagram;
import com.umlmodeler.domain.repository.DiagramRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class CreateDiagramService implements CreateDiagramUseCase {
    
    private final DiagramRepository diagramRepository;
    private final DiagramMapper diagramMapper;

    public CreateDiagramService(DiagramRepository diagramRepository, DiagramMapper diagramMapper) {
        this.diagramRepository = diagramRepository;
        this.diagramMapper = diagramMapper;
    }

    @Override
    public DiagramResult createDiagram(CreateDiagramCommand command) {
        Diagram diagram = Diagram.create(command.name());
        Diagram savedDiagram = diagramRepository.save(diagram);
        return diagramMapper.toResult(savedDiagram);
    }
}
```

#### Infrastructure - Entidad JPA (Separada del Dominio)

```java
// infrastructure/adapter/out/persistence/DiagramJpaEntity.java
package com.umlmodeler.infrastructure.adapter.out.persistence;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "diagrams")
public class DiagramJpaEntity {
    
    @Id
    private UUID id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(name = "owner_id", nullable = false)
    private String ownerId;
    
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "diagram_id")
    private List<UmlClassJpaEntity> classes = new ArrayList<>();
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructores, getters, setters...
    protected DiagramJpaEntity() {}
    
    public DiagramJpaEntity(UUID id, String name, String ownerId, 
                            LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.ownerId = ownerId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    // Getters y setters
    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getOwnerId() { return ownerId; }
    public List<UmlClassJpaEntity> getClasses() { return classes; }
    // ...
}
```

#### Infrastructure - Adaptador de Persistencia (Implementa el Puerto)

```java
// infrastructure/adapter/out/persistence/DiagramPersistenceAdapter.java
package com.umlmodeler.infrastructure.adapter.out.persistence;

import com.umlmodeler.domain.model.Diagram;
import com.umlmodeler.domain.model.DiagramId;
import com.umlmodeler.domain.repository.DiagramRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class DiagramPersistenceAdapter implements DiagramRepository {
    
    private final DiagramJpaRepository jpaRepository;
    private final DiagramPersistenceMapper mapper;

    public DiagramPersistenceAdapter(DiagramJpaRepository jpaRepository, 
                                     DiagramPersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Diagram save(Diagram diagram) {
        DiagramJpaEntity entity = mapper.toJpaEntity(diagram);
        DiagramJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Diagram> findById(DiagramId id) {
        return jpaRepository.findById(id.value())
            .map(mapper::toDomain);
    }

    @Override
    public List<Diagram> findByOwnerId(String ownerId) {
        return jpaRepository.findByOwnerId(ownerId).stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public void delete(DiagramId id) {
        jpaRepository.deleteById(id.value());
    }
}
```

#### Infrastructure - Controlador REST (Adaptador de Entrada)

```java
// infrastructure/adapter/in/web/DiagramController.java
package com.umlmodeler.infrastructure.adapter.in.web;

import com.umlmodeler.application.dto.CreateDiagramCommand;
import com.umlmodeler.application.dto.DiagramResult;
import com.umlmodeler.application.port.in.CreateDiagramUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/diagrams")
public class DiagramController {
    
    private final CreateDiagramUseCase createDiagramUseCase;
    private final WebMapper webMapper;

    public DiagramController(CreateDiagramUseCase createDiagramUseCase, 
                             WebMapper webMapper) {
        this.createDiagramUseCase = createDiagramUseCase;
        this.webMapper = webMapper;
    }

    @PostMapping
    public ResponseEntity<DiagramResponse> createDiagram(
            @RequestBody CreateDiagramRequest request) {
        
        CreateDiagramCommand command = webMapper.toCommand(request);
        DiagramResult result = createDiagramUseCase.createDiagram(command);
        DiagramResponse response = webMapper.toResponse(result);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
```

---

## FRONTEND - React + TypeScript + Feature-Sliced Design

### Principios FSD + Clean Architecture

FSD organiza el código por **capas de responsabilidad** y **slices de negocio**, mientras que Clean Architecture define la **dirección de las dependencias** (hacia adentro). La combinación permite:

- **Alta cohesión** dentro de cada slice
- **Bajo acoplamiento** entre slices
- **Fronteras explícitas** con APIs públicas (index.ts)
- **Testabilidad** mediante aislamiento

### Estructura de Carpetas (FSD)

```
frontend/src/
├── app/                    # Composition root, providers, routing
│   ├── providers/
│   │   ├── QueryProvider.tsx
│   │   └── AuthProvider.tsx
│   ├── router/
│   │   └── index.tsx
│   └── App.tsx
│
├── pages/                  # Route-level pages (composición)
│   ├── diagram-editor/
│   │   └── index.tsx
│   └── dashboard/
│       └── index.tsx
│
├── widgets/                # Composite UI blocks (secciones de página)
│   ├── uml-canvas/
│   │   ├── index.ts
│   │   ├── ui/
│   │   │   └── UmlCanvas.tsx
│   │   └── model/
│   │       └── useCanvasState.ts
│   └── class-palette/
│       └── index.ts
│
├── features/               # User-facing capabilities (casos de uso)
│   ├── create-diagram/
│   │   ├── index.ts        # API pública del slice
│   │   ├── ui/
│   │   │   └── CreateDiagramForm.tsx
│   │   └── model/
│   │       ├── useCreateDiagram.ts
│   │       └── types.ts
│   ├── add-class/
│   │   ├── index.ts
│   │   ├── ui/
│   │   └── model/
│   └── generate-code/
│       ├── index.ts
│       ├── ui/
│       └── model/
│
├── entities/               # Domain entities (modelos de negocio)
│   ├── diagram/
│   │   ├── index.ts        # API pública
│   │   ├── model/
│   │   │   ├── types.ts    # Tipos del dominio
│   │   │   ├── diagram.ts  # Lógica de dominio pura
│   │   │   └── useDiagram.ts
│   │   └── ui/
│   │       └── DiagramCard.tsx
│   ├── uml-class/
│   │   ├── index.ts
│   │   ├── model/
│   │   └── ui/
│   └── user/
│       ├── index.ts
│       ├── model/
│       └── ui/
│
└── shared/                 # Código reutilizable sin lógica de negocio
    ├── api/
    │   ├── client.ts       # Cliente HTTP base
    │   └── endpoints.ts
    ├── ui/
    │   ├── Button/
    │   └── Modal/
    ├── lib/
    │   ├── useDebounce.ts
    │   └── formatDate.ts
    └── config/
        └── constants.ts
```

### Regla de Dependencias (Dependency Cone)

Las importaciones solo pueden fluir **hacia abajo**:

```
app → pages → widgets → features → entities → shared
```

| Capa | Puede importar de |
|------|-------------------|
| `app` | pages, widgets, features, entities, shared |
| `pages` | widgets, features, entities, shared |
| `widgets` | features, entities, shared |
| `features` | entities, shared |
| `entities` | shared |
| `shared` | Solo paquetes npm |

### API Pública por Slice

Cada slice expone una API pública a través de `index.ts`. Los consumidores **NUNCA** importan archivos internos directamente:

```typescript
// ✅ CORRECTO - Importa desde el índice del slice
import { CreateDiagramForm } from '@/features/create-diagram';
import { useDiagram } from '@/entities/diagram';
import { Button } from '@/shared/ui/Button';

// ❌ INCORRECTO - Importa archivos internos directamente
import { CreateDiagramForm } from '@/features/create-diagram/ui/CreateDiagramForm';
import { useDiagram } from '@/entities/diagram/model/useDiagram';
```

### Mapeo Clean Architecture → FSD

| Concepto Clean Architecture | Capa FSD |
|----------------------------|----------|
| Entidades de dominio | `entities/` |
| Casos de uso | `features/` |
| Adaptadores de interfaz | `features/` y `shared/` |
| Frameworks y drivers | `app/`, `pages/`, `shared/` |

### Ejemplos de Código Frontend

#### Entities - Tipos del Dominio

```typescript
// entities/diagram/model/types.ts
export interface Diagram {
  id: string;
  name: string;
  ownerId: string;
  classes: UmlClass[];
  relations: UmlRelation[];
  createdAt: Date;
  updatedAt: Date;
}

export interface UmlClass {
  id: string;
  name: string;
  attributes: UmlAttribute[];
  methods: UmlMethod[];
}

export interface UmlAttribute {
  name: string;
  type: string;
  visibility: 'public' | 'private' | 'protected';
}

export interface UmlRelation {
  id: string;
  type: 'inheritance' | 'association' | 'aggregation' | 'composition' | 'dependency';
  sourceId: string;
  targetId: string;
  multiplicity?: string;
}
```

#### Entities - Lógica de Dominio Pura

```typescript
// entities/diagram/model/diagram.ts
import type { Diagram, UmlClass, UmlRelation } from './types';

export function createEmptyDiagram(name: string, ownerId: string): Diagram {
  return {
    id: crypto.randomUUID(),
    name,
    ownerId,
    classes: [],
    relations: [],
    createdAt: new Date(),
    updatedAt: new Date(),
  };
}

export function addClassToDiagram(diagram: Diagram, umlClass: UmlClass): Diagram {
  if (diagram.classes.some(c => c.name === umlClass.name)) {
    throw new Error(`A class named "${umlClass.name}" already exists`);
  }
  
  return {
    ...diagram,
    classes: [...diagram.classes, umlClass],
    updatedAt: new Date(),
  };
}

export function addRelationToDiagram(diagram: Diagram, relation: UmlRelation): Diagram {
  const sourceExists = diagram.classes.some(c => c.id === relation.sourceId);
  const targetExists = diagram.classes.some(c => c.id === relation.targetId);
  
  if (!sourceExists || !targetExists) {
    throw new Error('Both classes must exist before adding a relation');
  }
  
  return {
    ...diagram,
    relations: [...diagram.relations, relation],
    updatedAt: new Date(),
  };
}
```

#### Entities - API Pública

```typescript
// entities/diagram/index.ts
export { DiagramCard } from './ui/DiagramCard';
export { useDiagram } from './model/useDiagram';
export { 
  createEmptyDiagram, 
  addClassToDiagram, 
  addRelationToDiagram 
} from './model/diagram';
export type { Diagram, UmlClass, UmlRelation } from './model/types';
```

#### Entities - Hook de Dominio

```typescript
// entities/diagram/model/useDiagram.ts
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/shared/api/client';
import type { Diagram } from './types';

export function useDiagram(diagramId: string) {
  return useQuery({
    queryKey: ['diagram', diagramId],
    queryFn: () => apiClient.get<Diagram>(`/diagrams/${diagramId}`),
    staleTime: 30_000,
  });
}

export function useUpdateDiagram() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: (diagram: Diagram) => 
      apiClient.put<Diagram>(`/diagrams/${diagram.id}`, diagram),
    onSuccess: (updatedDiagram) => {
      queryClient.setQueryData(['diagram', updatedDiagram.id], updatedDiagram);
    },
  });
}
```

#### Features - Hook de Caso de Uso

```typescript
// features/create-diagram/model/useCreateDiagram.ts
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/shared/api/client';
import { createEmptyDiagram } from '@/entities/diagram';
import type { Diagram } from '@/entities/diagram';

interface CreateDiagramInput {
  name: string;
  ownerId: string;
}

export function useCreateDiagram() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async (input: CreateDiagramInput): Promise<Diagram> => {
      const newDiagram = createEmptyDiagram(input.name, input.ownerId);
      return apiClient.post<Diagram>('/diagrams', newDiagram);
    },
    onSuccess: (diagram) => {
      queryClient.invalidateQueries({ queryKey: ['diagrams'] });
      queryClient.setQueryData(['diagram', diagram.id], diagram);
    },
  });
}
```

#### Features - Componente de UI

```typescript
// features/create-diagram/ui/CreateDiagramForm.tsx
import { useState } from 'react';
import { useCreateDiagram } from '../model/useCreateDiagram';
import { Button, Input } from '@/shared/ui';

interface CreateDiagramFormProps {
  ownerId: string;
  onSuccess?: (diagramId: string) => void;
}

export function CreateDiagramForm({ ownerId, onSuccess }: CreateDiagramFormProps) {
  const [name, setName] = useState('');
  const createDiagram = useCreateDiagram();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!name.trim()) return;
    
    const diagram = await createDiagram.mutateAsync({ name, ownerId });
    setName('');
    onSuccess?.(diagram.id);
  };

  return (
    <form onSubmit={handleSubmit}>
      <Input
        value={name}
        onChange={(e) => setName(e.target.value)}
        placeholder="Nombre del diagrama"
      />
      <Button type="submit" disabled={createDiagram.isPending}>
        {createDiagram.isPending ? 'Creando...' : 'Crear Diagrama'}
      </Button>
    </form>
  );
}
```

#### Features - API Pública

```typescript
// features/create-diagram/index.ts
export { CreateDiagramForm } from './ui/CreateDiagramForm';
export { useCreateDiagram } from './model/useCreateDiagram';
```

#### Widgets - Composición de Features y Entities

```typescript
// widgets/uml-canvas/ui/UmlCanvas.tsx
import { useDiagram } from '@/entities/diagram';
import { AddClassButton } from '@/features/add-class';
import { GenerateCodeButton } from '@/features/generate-code';

interface UmlCanvasProps {
  diagramId: string;
}

export function UmlCanvas({ diagramId }: UmlCanvasProps) {
  const { data: diagram, isLoading } = useDiagram(diagramId);

  if (isLoading) return <div>Cargando diagrama...</div>;
  if (!diagram) return <div>Diagrama no encontrado</div>;

  return (
    <div className="uml-canvas">
      <div className="canvas-toolbar">
        <AddClassButton diagramId={diagramId} />
        <GenerateCodeButton diagramId={diagramId} />
      </div>
      
      <svg className="canvas-area">
        {diagram.classes.map((umlClass) => (
          <ClassNode key={umlClass.id} umlClass={umlClass} />
        ))}
        {diagram.relations.map((relation) => (
          <RelationLine key={relation.id} relation={relation} />
        ))}
      </svg>
    </div>
  );
}
```

#### Shared - Cliente HTTP

```typescript
// shared/api/client.ts
const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';

class ApiClient {
  private async request<T>(
    method: string,
    path: string,
    body?: unknown
  ): Promise<T> {
    const response = await fetch(`${BASE_URL}${path}`, {
      method,
      headers: {
        'Content-Type': 'application/json',
        ...this.getAuthHeaders(),
      },
      body: body ? JSON.stringify(body) : undefined,
    });

    if (!response.ok) {
      throw new ApiError(response.status, await response.text());
    }

    if (response.status === 204) {
      return undefined as T;
    }

    return response.json();
  }

  private getAuthHeaders(): Record<string, string> {
    const token = localStorage.getItem('auth_token');
    return token ? { Authorization: `Bearer ${token}` } : {};
  }

  get<T>(path: string): Promise<T> {
    return this.request<T>('GET', path);
  }

  post<T>(path: string, body: unknown): Promise<T> {
    return this.request<T>('POST', path, body);
  }

  put<T>(path: string, body: unknown): Promise<T> {
    return this.request<T>('PUT', path, body);
  }

  delete(path: string): Promise<void> {
    return this.request<void>('DELETE', path);
  }
}

export class ApiError extends Error {
  constructor(public status: number, message: string) {
    super(message);
    this.name = 'ApiError';
  }
}

export const apiClient = new ApiClient();
```

#### Shared - Configuración de Path Aliases

```json
// tsconfig.json (fragmento)
{
  "compilerOptions": {
    "baseUrl": ".",
    "paths": {
      "@/*": ["src/*"],
      "@app/*": ["src/app/*"],
      "@pages/*": ["src/pages/*"],
      "@widgets/*": ["src/widgets/*"],
      "@features/*": ["src/features/*"],
      "@entities/*": ["src/entities/*"],
      "@shared/*": ["src/shared/*"]
    }
  }
}
```

---

## Validación Automática de Arquitectura

### Backend - ArchUnit Test

Añade este test para verificar automáticamente que las reglas de dependencia se cumplen:

```java
// src/test/java/com/umlmodeler/ArchitectureTest.java
package com.umlmodeler;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@AnalyzeClasses(
    packages = "com.umlmodeler",
    importOptions = ImportOption.DoNotIncludeTests.class
)
public class ArchitectureTest {

    @ArchTest
    static final ArchRule hexagonalArchitecture = layeredArchitecture()
        .consideringAllDependencies()
        .layer("Domain").definedBy("..domain..")
        .layer("Application").definedBy("..application..")
        .layer("Infrastructure").definedBy("..infrastructure..")
        
        .whereLayer("Domain").mayNotAccessAnyLayer()
        .whereLayer("Application").mayOnlyAccessLayers("Domain")
        .whereLayer("Infrastructure").mayOnlyAccessLayers("Application", "Domain");

    @ArchTest
    static final ArchRule domainHasNoSpring = noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat()
        .resideInAnyPackage("org.springframework..", "jakarta.persistence..");

    @ArchTest
    static final ArchRule domainHasNoInfrastructure = noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat()
        .resideInAPackage("..infrastructure..");
}
```

### Frontend - ESLint Boundaries

Configura `eslint-plugin-boundaries` para hacer cumplir las reglas FSD:

```javascript
// .eslintrc.js (fragmento)
module.exports = {
  plugins: ['boundaries'],
  settings: {
    'boundaries/elements': [
      { type: 'app', pattern: 'src/app/*' },
      { type: 'pages', pattern: 'src/pages/*' },
      { type: 'widgets', pattern: 'src/widgets/*' },
      { type: 'features', pattern: 'src/features/*' },
      { type: 'entities', pattern: 'src/entities/*' },
      { type: 'shared', pattern: 'src/shared/*' },
    ],
  },
  rules: {
    'boundaries/element-types': [
      'error',
      {
        default: 'disallow',
        rules: [
          { from: 'app', allow: ['pages', 'widgets', 'features', 'entities', 'shared'] },
          { from: 'pages', allow: ['widgets', 'features', 'entities', 'shared'] },
          { from: 'widgets', allow: ['features', 'entities', 'shared'] },
          { from: 'features', allow: ['entities', 'shared'] },
          { from: 'entities', allow: ['shared'] },
          { from: 'shared', allow: [] },
        ],
      },
    ],
  },
};
```

---

## Comandos OpenSpec Recomendados

Al proponer cambios con OpenSpec, referencia siempre este archivo:

```
/opsx:propose añadir-crud-diagramas

Implementa un CRUD básico de diagramas UML siguiendo estrictamente:
- Backend: Arquitectura Hexagonal definida en AGENTS.md (sección BACKEND)
- Frontend: FSD con Clean Architecture definida en AGENTS.md (sección FRONTEND)

El caso de uso "CreateDiagram" debe ir en application/port/in y su implementación en application/service.
La entidad Diagram en domain/model con lógica de negocio pura.
El repositorio (interfaz) en domain/repository, implementación en infrastructure/adapter/out/persistence.
En el frontend, el slice "create-diagram" en features/, la entidad "diagram" en entities/.
```

---

## Cómo Usarlo

1. **Crea el archivo**: Copia todo el contenido del bloque `markdown` anterior en un archivo llamado `AGENTS.md` en la raíz de tu proyecto.

2. **Ajusta las rutas**: Si tu estructura de carpetas es diferente (por ejemplo, `backend/` vs `server/`), edita las rutas en el archivo.

3. **Ejecuta OpenCode**: Cuando OpenCode se inicie en este directorio, leerá automáticamente el `AGENTS.md` y usará estas reglas como contexto para todas las sugerencias de código.

4. **Combínalo con OpenSpec**: Cuando propongas un cambio con `/opsx:propose`, los specs generados respetarán automáticamente esta arquitectura.

5. **Valida con las herramientas**: Añade el `ArchitectureTest.java` para el backend y la configuración de ESLint para el frontend. Así podrás verificar automáticamente que las reglas se cumplen en cada build.