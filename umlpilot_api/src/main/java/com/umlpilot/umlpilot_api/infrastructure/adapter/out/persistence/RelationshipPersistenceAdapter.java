package com.umlpilot.umlpilot_api.infrastructure.adapter.out.persistence;

import com.umlpilot.umlpilot_api.domain.model.*;
import com.umlpilot.umlpilot_api.domain.repository.RelationshipRepository;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;

@Component
public class RelationshipPersistenceAdapter implements RelationshipRepository {
    private final RelationshipJpaRepository jpa;
    public RelationshipPersistenceAdapter(RelationshipJpaRepository jpa) { this.jpa = jpa; }

    @Override public Relationship save(Relationship rel) {
        Optional<RelationshipJpaEntity> existing = jpa.findById(rel.getId().value());
        RelationshipJpaEntity e = existing.orElse(new RelationshipJpaEntity());
        e.setDiagramId(rel.getDiagramId().value());
        e.setSourceElementId(rel.getSourceElementId().value());
        e.setTargetElementId(rel.getTargetElementId().value());
        e.setRelationshipType(rel.getRelationshipType());
        e.setName(rel.getName());
        e.setDirection(rel.getDirection());
        e.setAssociationClassElementId(rel.getAssociationClassElementId());
        e.setSourceHandle(rel.getSourceHandle());
        e.setTargetHandle(rel.getTargetHandle());
        RelationshipJpaEntity saved = jpa.save(e);
        return toDomain(saved);
    }

    @Override public Optional<Relationship> findById(RelationshipId id) { return jpa.findById(id.value()).map(this::toDomain); }

    @Override public List<Relationship> findByDiagramId(String diagramId) { return jpa.findByDiagramId(diagramId).stream().map(this::toDomain).toList(); }

    @Override public void delete(RelationshipId id) { jpa.deleteById(id.value()); }

    private Relationship toDomain(RelationshipJpaEntity e) {
        return Relationship.reconstitute(RelationshipId.from(e.getId()), DiagramId.from(e.getDiagramId()), ElementId.from(e.getSourceElementId()), ElementId.from(e.getTargetElementId()), e.getRelationshipType(), e.getName(), e.getDirection(), e.getAssociationClassElementId(), e.getSourceHandle(), e.getTargetHandle(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
