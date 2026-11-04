package com.umlpilot.umlpilot_api.infrastructure.adapter.out.persistence;

import com.umlpilot.umlpilot_api.domain.model.DiagramShareLink;
import com.umlpilot.umlpilot_api.domain.model.DiagramShareLinkId;
import com.umlpilot.umlpilot_api.domain.repository.DiagramShareLinkRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class DiagramShareLinkPersistenceAdapter implements DiagramShareLinkRepository {

    private final DiagramShareLinkJpaRepository jpa;

    public DiagramShareLinkPersistenceAdapter(DiagramShareLinkJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public DiagramShareLink save(DiagramShareLink link) {
        DiagramShareLinkJpaEntity existing = jpa.findById(link.getId().value()).orElse(null);
        DiagramShareLinkJpaEntity entity = existing != null
                ? existing
                : new DiagramShareLinkJpaEntity(link.getId().value(), link.getProjectId(), link.getDiagramId(),
                        link.getToken(), link.getCreatedBy(), link.getCreatedAt(), link.getLinkRole(), link.isActive());
        entity.setLinkRole(link.getLinkRole());
        entity.setActive(link.isActive());
        return toDomain(jpa.save(entity));
    }

    @Override
    public Optional<DiagramShareLink> findByDiagramId(String diagramId) {
        return jpa.findByDiagramId(diagramId).map(this::toDomain);
    }

    @Override
    public Optional<DiagramShareLink> findActiveByToken(String token) {
        return jpa.findByTokenAndActiveTrue(token).map(this::toDomain);
    }

    private DiagramShareLink toDomain(DiagramShareLinkJpaEntity entity) {
        return DiagramShareLink.reconstitute(
                DiagramShareLinkId.from(entity.getId()),
                entity.getProjectId(),
                entity.getDiagramId(),
                entity.getToken(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getLinkRole(),
                entity.isActive()
        );
    }
}
