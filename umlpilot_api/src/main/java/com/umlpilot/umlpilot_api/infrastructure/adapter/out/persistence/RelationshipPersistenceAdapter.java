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
        e.setIsTemplateBinding(rel.getIsTemplateBinding());
        e.setTemplateClass(rel.getTemplateClass());
        RelationshipJpaEntity saved = jpa.save(e);
        return Relationship.reconstitute(RelationshipId.from(saved.getId()), DiagramId.from(saved.getDiagramId()), ElementId.from(saved.getSourceElementId()), ElementId.from(saved.getTargetElementId()), saved.getRelationshipType(), saved.getName(), saved.getDirection(), saved.getIsTemplateBinding(), saved.getTemplateClass(), saved.getCreatedAt(), saved.getUpdatedAt());
    }
    @Override public Optional<Relationship> findById(RelationshipId id) { return jpa.findById(id.value()).map(e -> Relationship.reconstitute(RelationshipId.from(e.getId()), DiagramId.from(e.getDiagramId()), ElementId.from(e.getSourceElementId()), ElementId.from(e.getTargetElementId()), e.getRelationshipType(), e.getName(), e.getDirection(), e.getIsTemplateBinding(), e.getTemplateClass(), e.getCreatedAt(), e.getUpdatedAt())); }
    @Override public List<Relationship> findByDiagramId(String diagramId) { return jpa.findByDiagramId(diagramId).stream().map(e -> Relationship.reconstitute(RelationshipId.from(e.getId()), DiagramId.from(e.getDiagramId()), ElementId.from(e.getSourceElementId()), ElementId.from(e.getTargetElementId()), e.getRelationshipType(), e.getName(), e.getDirection(), e.getIsTemplateBinding(), e.getTemplateClass(), e.getCreatedAt(), e.getUpdatedAt())).toList(); }
    @Override public void delete(RelationshipId id) { jpa.deleteById(id.value()); }
}
