# `docs/architecture/frontend-fsd.md`

```markdown
# Frontend - React + TypeScript + Feature-Sliced Design

## Principios FSD + Clean Architecture

FSD organiza el código por **capas de responsabilidad** y **slices de negocio**, mientras que Clean Architecture define la **dirección de las dependencias** (hacia adentro). La combinación permite:

- **Alta cohesión** dentro de cada slice
- **Bajo acoplamiento** entre slices
- **Fronteras explícitas** con APIs públicas (index.ts)
- **Testabilidad** mediante aislamiento

## Stack

- React 18
- TypeScript 5
- Vite
- TanStack Query (React Query) para estado del servidor
- Zustand o Context API para estado global del cliente
- React Router para routing

## Estructura de Carpetas

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

## Regla de Dependencias (Dependency Cone)

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

## API Pública por Slice

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

## Mapeo Clean Architecture → FSD

| Concepto Clean Architecture | Capa FSD |
|----------------------------|----------|
| Entidades de dominio | `entities/` |
| Casos de uso | `features/` |
| Adaptadores de interfaz | `features/` y `shared/` |
| Frameworks y drivers | `app/`, `pages/`, `shared/` |

## Convenciones de Nomenclatura

- **Archivos de componentes**: `PascalCase.tsx` (ej: `CreateDiagramForm.tsx`)
- **Archivos de hooks**: `camelCase.ts` con prefijo `use` (ej: `useCreateDiagram.ts`)
- **Archivos de utilidades**: `camelCase.ts` (ej: `formatDate.ts`)
- **Tipos**: `types.ts` dentro de cada slice
- **Lógica de dominio pura**: nombre descriptivo en `camelCase` (ej: `diagram.ts`)
- **Carpetas de slice**: `kebab-case` (ej: `create-diagram/`)
- **Subcarpetas estándar**: `ui/`, `model/`, `api/`, `lib/`

---

## Ejemplos de Código

### Entities - Tipos del Dominio

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

### Entities - Lógica de Dominio Pura

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

### Entities - API Pública

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

### Entities - Hook de Dominio

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

### Features - Hook de Caso de Uso

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

### Features - Componente de UI

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

### Features - API Pública

```typescript
// features/create-diagram/index.ts
export { CreateDiagramForm } from './ui/CreateDiagramForm';
export { useCreateDiagram } from './model/useCreateDiagram';
```

### Widgets - Composición de Features y Entities

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

### Shared - Cliente HTTP

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

### Shared - Configuración de Path Aliases

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