# `docs/architecture/backend-hexagonal.md`

```markdown
# Backend - SpringBoot + Arquitectura Hexagonal

## Principio Fundamental

La Regla de Dependencia es sagrada: las dependencias solo apuntan hacia adentro.
El dominio NUNCA conoce Spring, JPA, ni ninguna infraestructura.

## Stack

- Java 21
- SpringBoot 3.3
- Spring Data JPA (Hibernate)
- PostgreSQL 16
- Flyway (migraciones)
- ArchUnit (validación de arquitectura)

## Estructura de Paquetes

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

## Reglas Críticas

1. **Domain NO importa Spring**: Cero anotaciones `@Service`, `@Component`, `@Entity` en el dominio
2. **Separación de entidades**: Las entidades JPA viven en infrastructure, NO en domain. Usa mappers para convertir entre ambas representaciones
3. **Repositorios como puertos**: El dominio define la interfaz (`UserRepository`), la infraestructura la implementa (`UserPersistenceAdapter`)
4. **@Transactional solo en application**: Los servicios de aplicación manejan transacciones, nunca el dominio
5. **DTOs separados del dominio**: Los contratos de API (Request/Response) viven en infrastructure, los Commands/Results en application

## Reglas de Persistencia (Base de Datos)

- Usa `@Column(name = "...", nullable = false)` explícitamente, nunca dejes el nombre inferido
- Los Value Objects del dominio (ej: `DiagramId`) se mapean como `UUID` en la entidad JPA
- Los campos de auditoría se gestionan con `@PrePersist` y `@PreUpdate` en la entidad JPA
- Nunca uses `@Data` de Lombok en entidades JPA (genera equals/hashCode incorrectos)
- Migraciones Flyway en `backend/src/main/resources/db/migration/` con formato `V1__nombre.sql`

| Entidad Dominio | Entidad JPA | Tabla |
|----------------|-------------|-------|
| `Diagram` | `DiagramJpaEntity` | `diagrams` |
| `UmlClass` | `UmlClassJpaEntity` | `uml_classes` |
| `UmlRelation` | `UmlRelationJpaEntity` | `uml_relations` |
| `User` | `UserJpaEntity` | `users` |

---

## Ejemplos de Código

### Domain - Entidad con lógica de negocio pura

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

    public DiagramId getId() { return id; }
    public String getName() { return name; }
    public List<UmlClass> getClasses() { return List.copyOf(classes); }
    public List<UmlRelation> getRelations() { return List.copyOf(relations); }
}
```

### Domain - Value Object

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

### Domain - Puerto de Salida (Repository Interface)

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

### Application - Puerto de Entrada (Use Case Interface)

```java
// application/port/in/CreateDiagramUseCase.java
package com.umlmodeler.application.port.in;

import com.umlmodeler.application.dto.CreateDiagramCommand;
import com.umlmodeler.application.dto.DiagramResult;

public interface CreateDiagramUseCase {
    DiagramResult createDiagram(CreateDiagramCommand command);
}
```

### Application - DTOs

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

### Application - Implementación del Caso de Uso

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

### Infrastructure - Entidad JPA (Separada del Dominio)

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

    protected DiagramJpaEntity() {}
    
    public DiagramJpaEntity(UUID id, String name, String ownerId, 
                            LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.ownerId = ownerId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getOwnerId() { return ownerId; }
    public List<UmlClassJpaEntity> getClasses() { return classes; }
}
```

### Infrastructure - Adaptador de Persistencia (Implementa el Puerto)

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

### Infrastructure - Controlador REST (Adaptador de Entrada)

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