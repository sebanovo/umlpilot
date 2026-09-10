package com.umlpilot.umlpilot_api.infrastructure.adapter.out.persistence;

import com.umlpilot.umlpilot_api.domain.model.*;
import com.umlpilot.umlpilot_api.domain.repository.MultiplicityRepository;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;

@Component
public class MultiplicityPersistenceAdapter implements MultiplicityRepository {
    private final MultiplicityJpaRepository jpa;
    public MultiplicityPersistenceAdapter(MultiplicityJpaRepository jpa) { this.jpa = jpa; }

    @Override public Multiplicity save(Multiplicity mult) {
        Optional<MultiplicityJpaEntity> existing = jpa.findById(mult.getId().value());
        MultiplicityJpaEntity e = existing.orElse(new MultiplicityJpaEntity());
        e.setRelationshipId(mult.getRelationshipId().value());
        e.setEnd(mult.getEnd());
        e.setMin(mult.getMin());
        e.setMax(mult.getMax());
        e.setIsOrdered(mult.getIsOrdered());
        e.setIsUnique(mult.getIsUnique());
        MultiplicityJpaEntity saved = jpa.save(e);
        return Multiplicity.reconstitute(MultiplicityId.from(saved.getId()), RelationshipId.from(saved.getRelationshipId()), saved.getEnd(), saved.getMin(), saved.getMax(), saved.getIsOrdered(), saved.getIsUnique());
    }
    @Override public Optional<Multiplicity> findById(MultiplicityId id) { return jpa.findById(id.value()).map(e -> Multiplicity.reconstitute(MultiplicityId.from(e.getId()), RelationshipId.from(e.getRelationshipId()), e.getEnd(), e.getMin(), e.getMax(), e.getIsOrdered(), e.getIsUnique())); }
    @Override public List<Multiplicity> findByRelationshipId(String relationshipId) { return jpa.findByRelationshipId(relationshipId).stream().map(e -> Multiplicity.reconstitute(MultiplicityId.from(e.getId()), RelationshipId.from(e.getRelationshipId()), e.getEnd(), e.getMin(), e.getMax(), e.getIsOrdered(), e.getIsUnique())).toList(); }
    @Override public void delete(MultiplicityId id) { jpa.deleteById(id.value()); }
}
