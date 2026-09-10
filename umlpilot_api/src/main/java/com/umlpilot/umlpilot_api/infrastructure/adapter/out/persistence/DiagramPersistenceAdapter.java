package com.umlpilot.umlpilot_api.infrastructure.adapter.out.persistence;

import com.umlpilot.umlpilot_api.domain.model.*;
import com.umlpilot.umlpilot_api.domain.repository.DiagramRepository;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;

@Component
public class DiagramPersistenceAdapter implements DiagramRepository {
    private final DiagramJpaRepository jpa;
    public DiagramPersistenceAdapter(DiagramJpaRepository jpa) { this.jpa = jpa; }

    @Override public Diagram save(Diagram diagram) {
        Optional<DiagramJpaEntity> existing = jpa.findById(diagram.getId().value());
        DiagramJpaEntity entity = existing.orElse(new DiagramJpaEntity());
        entity.setProjectId(diagram.getProjectId().value());
        entity.setName(diagram.getName());
        entity.setType(diagram.getType());
        entity.setDescription(diagram.getDescription());
        entity.setCanvasData(diagram.getCanvasData());
        entity.setVersion(diagram.getVersion());
        entity.setIsLocked(diagram.getIsLocked());
        entity.setLockedByUserId(diagram.getLockedByUserId());
        return toDomain(jpa.save(entity));
    }
    @Override public Optional<Diagram> findById(DiagramId id) { return jpa.findById(id.value()).map(this::toDomain); }
    @Override public List<Diagram> findByProjectId(String projectId) { return jpa.findByProjectId(projectId).stream().map(this::toDomain).toList(); }
    @Override public void delete(DiagramId id) { jpa.deleteById(id.value()); }
    private Diagram toDomain(DiagramJpaEntity e) {
        return Diagram.reconstitute(DiagramId.from(e.getId()), ProjectId.from(e.getProjectId()), e.getName(), e.getType(), e.getDescription(), e.getCanvasData(), e.getVersion(), e.getIsLocked(), e.getLockedByUserId(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
