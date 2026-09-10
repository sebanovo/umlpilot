package com.umlpilot.umlpilot_api.infrastructure.adapter.out.persistence;

import com.umlpilot.umlpilot_api.domain.model.*;
import com.umlpilot.umlpilot_api.domain.repository.DiagramRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class DiagramPersistenceAdapter implements DiagramRepository {

    private final DiagramJpaRepository jpaRepository;

    public DiagramPersistenceAdapter(DiagramJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Diagram save(Diagram diagram) {
        DiagramJpaEntity entity = new DiagramJpaEntity(
                diagram.getId().value(), diagram.getProjectId().value(), diagram.getName(), diagram.getType(),
                diagram.getDescription(), diagram.getCanvasData(), diagram.getVersion(),
                diagram.getIsLocked(), diagram.getLockedByUserId(),
                diagram.getCreatedAt(), diagram.getUpdatedAt()
        );
        DiagramJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Diagram> findById(DiagramId id) {
        return jpaRepository.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<Diagram> findByProjectId(String projectId) {
        return jpaRepository.findByProjectId(projectId).stream().map(this::toDomain).toList();
    }

    @Override
    public void delete(DiagramId id) {
        jpaRepository.deleteById(id.value());
    }

    private Diagram toDomain(DiagramJpaEntity entity) {
        return Diagram.reconstitute(
                DiagramId.from(entity.getId()), ProjectId.from(entity.getProjectId()),
                entity.getName(), entity.getType(), entity.getDescription(),
                entity.getCanvasData(), entity.getVersion(), entity.getIsLocked(),
                entity.getLockedByUserId(), entity.getCreatedAt(), entity.getUpdatedAt()
        );
    }
}
