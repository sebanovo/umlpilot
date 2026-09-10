package com.umlpilot.umlpilot_api.infrastructure.adapter.out.persistence;

import com.umlpilot.umlpilot_api.domain.model.*;
import com.umlpilot.umlpilot_api.domain.repository.AttributeRepository;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;

@Component
public class AttributePersistenceAdapter implements AttributeRepository {
    private final AttributeJpaRepository jpa;
    public AttributePersistenceAdapter(AttributeJpaRepository jpa) { this.jpa = jpa; }

    @Override public Attribute save(Attribute attr) {
        Optional<AttributeJpaEntity> existing = jpa.findById(attr.getId().value());
        AttributeJpaEntity e = existing.orElse(new AttributeJpaEntity());
        e.setElementId(attr.getElementId().value());
        e.setName(attr.getName());
        e.setDataType(attr.getDataType());
        e.setVisibility(attr.getVisibility());
        e.setDefaultValue(attr.getDefaultValue());
        e.setIsStatic(attr.getIsStatic());
        e.setIsFinal(attr.getIsFinal());
        e.setIsTransient(attr.getIsTransient());
        e.setIsVolatile(attr.getIsVolatile());
        e.setMultiplicity(attr.getMultiplicity());
        e.setOrderIndex(attr.getOrderIndex());
        AttributeJpaEntity saved = jpa.save(e);
        return Attribute.reconstitute(AttributeId.from(saved.getId()), ElementId.from(saved.getElementId()), saved.getName(), saved.getDataType(), saved.getVisibility(), saved.getDefaultValue(), saved.getIsStatic(), saved.getIsFinal(), saved.getIsTransient(), saved.getIsVolatile(), saved.getMultiplicity(), saved.getOrderIndex());
    }
    @Override public Optional<Attribute> findById(AttributeId id) { return jpa.findById(id.value()).map(e -> Attribute.reconstitute(AttributeId.from(e.getId()), ElementId.from(e.getElementId()), e.getName(), e.getDataType(), e.getVisibility(), e.getDefaultValue(), e.getIsStatic(), e.getIsFinal(), e.getIsTransient(), e.getIsVolatile(), e.getMultiplicity(), e.getOrderIndex())); }
    @Override public List<Attribute> findByElementId(String elementId) { return jpa.findByElementId(elementId).stream().map(e -> Attribute.reconstitute(AttributeId.from(e.getId()), ElementId.from(e.getElementId()), e.getName(), e.getDataType(), e.getVisibility(), e.getDefaultValue(), e.getIsStatic(), e.getIsFinal(), e.getIsTransient(), e.getIsVolatile(), e.getMultiplicity(), e.getOrderIndex())).toList(); }
    @Override public void delete(AttributeId id) { jpa.deleteById(id.value()); }
}
