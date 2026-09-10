package com.umlpilot.umlpilot_api.infrastructure.adapter.out.persistence;

import com.umlpilot.umlpilot_api.domain.model.*;
import com.umlpilot.umlpilot_api.domain.repository.LiteralRepository;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;

@Component
public class LiteralPersistenceAdapter implements LiteralRepository {
    private final LiteralJpaRepository jpa;
    public LiteralPersistenceAdapter(LiteralJpaRepository jpa) { this.jpa = jpa; }

    @Override public Literal save(Literal literal) {
        Optional<LiteralJpaEntity> existing = jpa.findById(literal.getId().value());
        LiteralJpaEntity e = existing.orElse(new LiteralJpaEntity());
        e.setElementId(literal.getElementId().value());
        e.setName(literal.getName());
        e.setValue(literal.getValue());
        e.setOrderIndex(literal.getOrderIndex());
        LiteralJpaEntity saved = jpa.save(e);
        return Literal.reconstitute(LiteralId.from(saved.getId()), ElementId.from(saved.getElementId()), saved.getName(), saved.getValue(), saved.getOrderIndex());
    }
    @Override public Optional<Literal> findById(LiteralId id) { return jpa.findById(id.value()).map(e -> Literal.reconstitute(LiteralId.from(e.getId()), ElementId.from(e.getElementId()), e.getName(), e.getValue(), e.getOrderIndex())); }
    @Override public List<Literal> findByElementId(String elementId) { return jpa.findByElementId(elementId).stream().map(e -> Literal.reconstitute(LiteralId.from(e.getId()), ElementId.from(e.getElementId()), e.getName(), e.getValue(), e.getOrderIndex())).toList(); }
    @Override public void delete(LiteralId id) { jpa.deleteById(id.value()); }
}
